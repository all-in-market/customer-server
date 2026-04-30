package com.example.allinmarket.seller.payout.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.payout.entity.Payout;
import com.example.allinmarket.domain.payout.enums.PayoutStatus;
import com.example.allinmarket.domain.payout.repository.PayoutRepository;
import com.example.allinmarket.domain.settlement.entity.Settlement;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SellerPayoutService {
    private final PayoutRepository payoutRepository;
    private final SettlementRepository settlementRepository;
    private final SellerPayoutProcessor sellerPayoutProcessor;

    @Transactional
    public void createPayout() {
        int page = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, 100);

            List<Settlement> settlements = settlementRepository.findWithoutPayout(SettlementStatus.COMPLETED, pageable);

            if (settlements.isEmpty()) {
                log.info("{}", ErrorEnum.SETTLEMENT_NOT_FOUND.getMessage());
                break;
            }

            for (Settlement settlement : settlements) {

                try {
                    Payout payout = Payout.of(
                            settlement.getSeller(),
                            settlement.getId(),
                            settlement.getAmount(),
                            settlement.getFee(),
                            PayoutStatus.PENDING,
                            generatePayoutKey(settlement.getId())
                    );

                    payoutRepository.save(payout);

                    // 정산 상태 지급 대기로 변경
                    settlement.markPayoutReady();

                    log.info("정산 지급 생성 성공 : payoutId = {}", payout.getId());

                } catch (DataIntegrityViolationException e) {
                    if (isDuplicate(e)) {
                        log.info("중복 지급 스킵: settlementId = {}", settlement.getId());
                        continue;
                    }

                    throw e;

                } catch (Exception e) {
                    // 개별 실패가 전체 루프를 멈추지 않도록 예외 처리
                    log.error("지급 데이터 생성 실패: settlementId = {}, error = {}", settlement.getId(), e.getMessage(), e);
                }
            }

            page++;
        }
    }

    @Transactional
    public void processPayout() {
        int page = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, 100);

            List<Payout> payouts = payoutRepository.findForUpdate(PayoutStatus.PENDING, 5, pageable);

            if (payouts.isEmpty()) {
                log.info("{}", ErrorEnum.PAYOUT_NOT_FOUND.getMessage());
                break;
            }

            for (Payout payout : payouts) {

                try {
                    sellerPayoutProcessor.processSinglePayout(payout);

                    log.info("지급 성공: payoutId = {}, amount = {}", payout.getId(), payout.getAmount());

                } catch (BaseException e) {
                    // 검증 실패 시 payout 실패 처리
                    payout.fail();

                    log.error("검증 실패 -> 즉시 실패 처리: payoutId = {}", payout.getId(), e);

                } catch (Exception e) {
                    // 타임아웃 등 예외 발생 시 PROCESSING 유지 및 재시도
                    log.error("지급 처리 중 예외 발생: payoutId = {}, error = {}", payout.getId(), e.getMessage(), e);
                    payout.increaseRetryCount();

                    if (payout.getRetryCount() >= 5) {
                        payout.fail();
                        log.error("최대 재시도 초과 -> 실패 처리: payoutId = {}", payout.getId(), e);
                    }
                }
            }

            if (payouts.size() < 100) break;

            page++;
        }
    }

    private String generatePayoutKey(Long settlementId) {
        return "PAYOUT_" + settlementId;
    }

    private boolean isDuplicate(DataIntegrityViolationException e) {
        Throwable cause = e;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException) {
                String constraintName = ((ConstraintViolationException) cause).getConstraintName();
                return "uk_payout_settlement_id".equalsIgnoreCase(constraintName);
            }
            cause = cause.getCause();
        }
        return false;
    }
}

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
    private final SellerPayoutFailHandler sellerPayoutFailHandler;

    @Transactional
    public void createPayout() {
        Long lastId = 0L;

        while (true) {
            List<Settlement> settlements = settlementRepository.findWithoutPayout(SettlementStatus.COMPLETED, lastId, PageRequest.of(0, 100));

            if (settlements.isEmpty()) {
                log.info("{}", ErrorEnum.SETTLEMENT_NOT_FOUND.getMessage());
                break;
            }

            for (Settlement settlement : settlements) {
                lastId = settlement.getId();

                try {
                    sellerPayoutProcessor.createSinglePayout(settlement.getId());

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
        }
    }

    // 내부에서 독립 Transactional로 처리되어 외부는 루프 컨트롤만 하기 위해 Transactional 제거
    public void processPayout() {
        Long lastId = 0L;

        while (true) {
            List<Payout> payouts = payoutRepository.findBatch(PayoutStatus.PENDING, 5, lastId, PageRequest.of(0, 100));

            if (payouts.isEmpty()) {
                log.info("{}", ErrorEnum.PAYOUT_NOT_FOUND.getMessage());
                break;
            }

            for (Payout payout : payouts) {

                lastId = payout.getId();

                try {
                    sellerPayoutProcessor.processSinglePayout(payout.getId());

                } catch (BaseException e) {
                    // 검증 실패 시 payout 실패 처리
                    if (isValidationError(e)) {
                        sellerPayoutFailHandler.handlePayoutFail(payout.getId(), true);
                        log.error("검증 실패 -> 즉시 실패 처리: payoutId = {}", payout.getId(), e);
                    } else {
                        // 기타 BaseException (상태 변경 오류 등)
                        log.error("지급 처리 중 비즈니스 예외 발생: payoutId = {}, errorEnum = {}", payout.getId(), e.getErrorEnum(), e);
                        sellerPayoutFailHandler.handlePayoutFail(payout.getId(), false);
                    }

                } catch (Exception e) {
                    // 타임아웃 등 예외 발생 시 PROCESSING 유지 및 재시도
                    log.error("지급 처리 중 예외 발생: payoutId = {}, error = {}", payout.getId(), e.getMessage(), e);
                    sellerPayoutFailHandler.handlePayoutFail(payout.getId(), false);
                }
            }

            if (payouts.size() < 100) break;
        }
    }

    private boolean isValidationError(BaseException e) {
        return e.getErrorEnum() == ErrorEnum.PAYOUT_MISMATCH
                || e.getErrorEnum() == ErrorEnum.PAYOUT_AMOUNT_INVALID
                || e.getErrorEnum() == ErrorEnum.PAYOUT_AMOUNT_MISMATCH;
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

package com.example.allinmarket.seller.payout.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.banking.BankingGateway;
import com.example.allinmarket.domain.banking.dto.BankingResponse;
import com.example.allinmarket.domain.payout.entity.Payout;
import com.example.allinmarket.domain.settlement.entity.Settlement;
import com.example.allinmarket.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerPayoutProcessor {
    private final BankingGateway bankingGateway;
    private final SettlementRepository settlementRepository;

    // 독립 트랜잭션으로 분리
    // 각 지급 건이 성공/실패하더라도 다른 지급 건이나 전체 리스트에 영향 X
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSinglePayout(Payout payout) {
        payout.markProcessing();

        // 뱅킹 API 호출
        BankingResponse response = bankingGateway.getBanking(
                payout.getPayoutKey(),
                payout.getSeller().getBankCode(),
                payout.getSeller().getBankAccount(),
                payout.getAmount()
        );

        // 상태 변경 (이 시점에 DB 커밋)
        if (response.isSuccess()) {
            // 성공 응답에 대해 검증
            validateResponse(payout, response);

            payout.success();

            Settlement settlement = settlementRepository.findById(payout.getSettlementId())
                    .orElseThrow(() -> new BaseException(ErrorEnum.SETTLEMENT_NOT_FOUND));

            settlement.markPayoutDone();

        } else {

            payout.fail();

        }
    }

    private void validateResponse(Payout payout, BankingResponse response) {
        // 멱등성 키 일치 여부
        if (!payout.getPayoutKey().equals(response.getPayoutKey())) {
            throw new BaseException(ErrorEnum.PAYOUT_MISMATCH);
        }

        if (response.getAmount() == null) {
            throw new BaseException(ErrorEnum.PAYOUT_AMOUNT_INVALID);
        }

        if (payout.getAmount().compareTo(response.getAmount()) != 0) {
            throw new BaseException(ErrorEnum.PAYOUT_AMOUNT_MISMATCH);
        }
    }
}

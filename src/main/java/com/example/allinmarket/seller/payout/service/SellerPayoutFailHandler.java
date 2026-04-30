package com.example.allinmarket.seller.payout.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.payout.entity.Payout;
import com.example.allinmarket.domain.payout.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SellerPayoutFailHandler {

    private final PayoutRepository payoutRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePayoutFail(Long payoutId, boolean isValidation) {
        Payout payout = payoutRepository.findByIdForUpdate(payoutId).orElseThrow(
                () -> new BaseException(ErrorEnum.PAYOUT_NOT_FOUND)
        );

        if (isValidation) {
            payout.fail();
            return;
        }

        payout.increaseRetryCount();

        if (payout.getRetryCount() >= 5) {
            payout.fail();
        }
    }
}

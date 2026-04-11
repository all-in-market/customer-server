package com.example.allinmarket.buyer.me.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.me.dto.response.BuyerDetailResponse;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerMeService {

    private final BuyerRepository buyerRepository;

    /**
     * 구매자 내 정보 조회
     */
    public BuyerDetailResponse getMe(Long currentUserId) {
        Buyer buyer = buyerRepository.findByIdAndDeletedAtIsNull(currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        return BuyerDetailResponse.from(buyer);
    }
}

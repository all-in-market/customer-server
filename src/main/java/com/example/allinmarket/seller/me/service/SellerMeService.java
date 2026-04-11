package com.example.allinmarket.seller.me.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.me.dto.SellerDetailResponse;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerMeService {

    private final SellerRepository sellerRepository;

    // 내 정보 조회
    public SellerDetailResponse getMyProfile (Long userId) {

        // 존재 && deletedAt != null 확인
        Seller me = sellerRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND));

        return SellerDetailResponse.from(me);
    }
}

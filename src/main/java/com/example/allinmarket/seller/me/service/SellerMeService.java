package com.example.allinmarket.seller.me.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.me.dto.request.SellerUpdateRequest;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerMeService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;

    // 내 정보 조회
    public SellerDetailResponse getMyProfile (Long userId) {

        Seller me = sellerRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        return SellerDetailResponse.from(me);
    }

    @Transactional
    public SellerDetailResponse updateMyProfile (Long userId, SellerUpdateRequest request) {

        Seller me = sellerRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        me.updateMyProfile(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone(),
                request.storeName(),
                request.bizNumber(),
                request.bankAccount()
        );

        return SellerDetailResponse.from(me);
    }
}

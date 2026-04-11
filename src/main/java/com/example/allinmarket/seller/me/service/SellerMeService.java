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

        if (request.email() != null && !request.email().isBlank()) {
            me.updateEmail(request.email());
        }

        if (request.password() != null && !request.password().isBlank()) {
            me.updatePassword(passwordEncoder.encode(request.password()));
        }

        if (request.name() != null && !request.name().isBlank()) {
            me.updateName(request.name());
        }

        if (request.phone() != null && !request.phone().isBlank()) {
            me.updatePhone(request.phone());
        }

        if (request.storeName() != null && !request.storeName().isBlank()) {
            me.updateStoreName(request.storeName());
        }

        if (request.bizNumber() != null && !request.bizNumber().isBlank()) {
            me.updateBizNumber(request.bizNumber());
        }

        if (request.bankAccount() != null && !request.bankAccount().isBlank()) {
            me.updateBankAccount(request.bankAccount());
        }

        return SellerDetailResponse.from(me);
    }
}

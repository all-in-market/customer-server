package com.example.allinmarket.seller.me.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.me.dto.request.SellerUpdateRequest;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.repository.SellerRepository;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

        // 중복 이메일 검사
        if (sellerRepository.existsByEmail(request.email())) {
            throw new BaseException(ErrorEnum.EMAIL_ALREADY_EXISTS);
        }

        if (StringUtils.hasText(request.email())) {
            me.updateEmail(request.email());
        }

        if (StringUtils.hasText(request.password())) {
            me.updatePassword(passwordEncoder.encode(request.password()));
        }

        if (StringUtils.hasText(request.name())) {
            me.updateName(request.name());
        }

        if (StringUtils.hasText(request.phone())) {
            me.updatePhone(request.phone());
        }

        if (StringUtils.hasText(request.storeName())) {
            me.updateStoreName(request.storeName());
        }

        if (StringUtils.hasText(request.bizNumber())) {
            me.updateBizNumber(request.bizNumber());
        }

        if (StringUtils.hasText(request.bankAccount())) {
            me.updateBankAccount(request.bankAccount());
        }

        return SellerDetailResponse.from(me);
    }
}

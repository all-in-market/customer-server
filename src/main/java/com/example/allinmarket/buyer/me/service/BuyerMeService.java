package com.example.allinmarket.buyer.me.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.me.dto.request.BuyerUpdateRequest;
import com.example.allinmarket.buyer.me.dto.response.BuyerDetailResponse;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerMeService {

    private final BuyerRepository buyerRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 구매자 내 정보 조회
     */
    public BuyerDetailResponse getMyProfile(Long currentUserId) {
        Buyer buyer = buyerRepository.findByIdAndDeletedAtIsNull(currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        return BuyerDetailResponse.from(buyer);
    }

    /**
     * 구매자 내 정보 수정
     */
    @Transactional
    public BuyerDetailResponse updateMyProfile(Long currentUserId, BuyerUpdateRequest request) {
        Buyer buyer = buyerRepository.findByIdAndDeletedAtIsNull(currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        updateEmail(buyer, request.email());
        updatePassword(buyer, request.password());

        if (StringUtils.hasText(request.name())) {
            buyer.updateName(request.name());
        }

        if (StringUtils.hasText(request.phone())) {
            buyer.updatePhone(request.phone());
        }

        return BuyerDetailResponse.from(buyer);
    }

    private void updateEmail(Buyer buyer, String email) {
        if (StringUtils.hasText(email) && !email.equals(buyer.getEmail())) {

            boolean existence = buyerRepository.existsByEmail(email);

            if (existence) {
                throw new BaseException(ErrorEnum.EMAIL_ALREADY_EXISTS);
            }

            buyer.updateEmail(email);
        }
    }

    private void updatePassword(Buyer buyer, String password) {
        if (StringUtils.hasText(password)) {
            buyer.updatePassword(passwordEncoder.encode(password));
        }
    }


}

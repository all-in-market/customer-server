package com.example.allinmarket.buyer.auth.service;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.auth.dto.response.BuyerLoginResponse;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BuyerAuthService {
    private final BuyerRepository buyerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public BuyerAuthResponse signup(BuyerSignupRequest request) {
        boolean existence = buyerRepository.existsByEmail(request.email());

        if (existence) {
            throw new BaseException(ErrorEnum.EMAIL_ALREADY_EXISTS);
        }

        Buyer buyer = Buyer.of(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone()
        );

        Buyer savedBuyer = buyerRepository.save(buyer);

        return BuyerAuthResponse.from(savedBuyer);
    }

    public BuyerLoginResponse login(BuyerLoginRequest request) {
        Buyer buyer = buyerRepository.findByEmail(request.email()).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        if (buyer.getDeletedAt() != null) {
            throw new BaseException(ErrorEnum.BUYER_ALREADY_DELETED);
        }

        if (!passwordEncoder.matches(request.password(), buyer.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_MISMATCH);
        }

        String token = jwtProvider.generateToken(buyer.getId(), buyer.getRole());

        return new BuyerLoginResponse(token);
    }
}

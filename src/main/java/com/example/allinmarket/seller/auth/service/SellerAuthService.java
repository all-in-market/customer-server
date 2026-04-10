package com.example.allinmarket.seller.auth.service;

import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.seller.auth.dto.SellerCreateRequest;
import com.example.allinmarket.seller.auth.dto.SellerCreateResponse;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SellerAuthService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public SellerCreateResponse signup(SellerCreateRequest request) {
        Seller seller = Seller.of(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone(),
                request.storeName(),
                request.bizNumber(),
                request.bankAccount()
        );
        sellerRepository.save(seller);
        return SellerCreateResponse.from(seller);
    }


}

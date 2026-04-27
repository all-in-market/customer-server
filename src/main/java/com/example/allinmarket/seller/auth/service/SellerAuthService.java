package com.example.allinmarket.seller.auth.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.auth.dto.request.SellerCreateRequest;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.auth.dto.response.SellerCreateResponse;
import com.example.allinmarket.seller.auth.dto.response.SellerLoginResponse;
import com.example.allinmarket.seller.auth.dto.response.SellerLoginResult;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SellerAuthService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final SellerDashboardRepository sellerDashboardRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public SellerCreateResponse signup(SellerCreateRequest request) {

        if (sellerRepository.existsByEmail(request.email())) {
            throw new BaseException(ErrorEnum.EMAIL_ALREADY_EXISTS);
        }

        Seller seller = Seller.of(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.phone(),
                request.storeName(),
                request.bizNumber(),
                request.bankAccount()
        );
        Seller savedSeller = sellerRepository.save(seller);

        SellerDashboard dashboard = SellerDashboard.of(savedSeller, LocalDate.now(), 0, 0, 0, null, null);
        sellerDashboardRepository.save(dashboard);

        return SellerCreateResponse.from(seller);
    }

    public SellerLoginResult login(SellerLoginRequest request) {
        Seller seller = sellerRepository.findByEmailAndDeletedAtIsNull(request.email()).orElseThrow(
                () -> new BaseException(ErrorEnum.SELLER_NOT_FOUND)
        );

        if (seller.getStatus().equals(SellerStatus.PENDING)) {
            throw new BaseException(ErrorEnum.FORBIDDEN);
        }

        if (seller.getDeletedAt() != null) {
            throw new BaseException(ErrorEnum.SELLER_ALREADY_DELETED);
        }

        if (!passwordEncoder.matches(request.password(), seller.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_MISMATCH);
        }

        String accessToken = jwtProvider.generateToken(seller.getId(), seller.getRole());
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set("refresh:" + refreshToken, seller.getId(), 7, TimeUnit.DAYS);

        return new SellerLoginResult(new SellerLoginResponse(accessToken), refreshToken);
    }

    public SellerLoginResult refresh(String refreshToken) {
        Long userId = (Long) redisTemplate.opsForValue().get("refresh:" + refreshToken);
        if (userId == null) {
            throw new BaseException(ErrorEnum.TOKEN_EXPIRED);
        }

        redisTemplate.delete("refresh:" + refreshToken);
        String newRefreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("refresh:" + newRefreshToken, userId, 7, TimeUnit.DAYS);

        String newAccessToken = jwtProvider.generateToken(userId, UserRole.SELLER);
        return new SellerLoginResult(new SellerLoginResponse(newAccessToken), newRefreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        long remaining = jwtProvider.getRemainingExpiration(accessToken);
        redisTemplate.opsForValue()
                .set("blacklist:" + accessToken, "logout", remaining, TimeUnit.MILLISECONDS);

        redisTemplate.delete("refresh:" + refreshToken);
    }
}

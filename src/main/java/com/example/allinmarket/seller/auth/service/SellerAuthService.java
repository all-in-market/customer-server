package com.example.allinmarket.seller.auth.service;

import com.example.allinmarket.common.auth.consts.AuthConsts;
import com.example.allinmarket.common.auth.dto.LoginResponse;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.auth.dto.request.SellerCreateRequest;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.auth.dto.response.SellerCreateResponse;
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

    // USER_NOT_FOUND와 PASSWORD_MISMATCH 두 경로의 응답 시간을 통계적으로 일치시키기 위한 더미 해시값
    private static final String DUMMY_HASH =
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

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
                request.bankCode(),
                request.bankAccount()
        );
        Seller savedSeller = sellerRepository.save(seller);

        SellerDashboard dashboard = SellerDashboard.of(savedSeller, LocalDate.now(), 0, 0, 0, null, null);
        sellerDashboardRepository.save(dashboard);

        return SellerCreateResponse.from(seller);
    }

    public LoginResult login(SellerLoginRequest request) {
        Seller seller = sellerRepository.findByEmail(request.email()).orElseGet(() -> {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            log.warn("로그인 실패: {}", ErrorEnum.SELLER_NOT_FOUND);
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        });

        if (seller.getDeletedAt() != null) {
            log.warn("로그인 실패: {}", ErrorEnum.SELLER_ALREADY_DELETED);
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        if (seller.getStatus() != SellerStatus.APPROVED) {
            log.warn("로그인 실패: {}", ErrorEnum.FORBIDDEN);
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(request.password(), seller.getPassword())) {
            log.warn("로그인 실패: {}", ErrorEnum.PASSWORD_MISMATCH);
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        String accessToken = jwtProvider.generateToken(seller.getId(), seller.getRole());
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                AuthConsts.refreshKey(UserRole.SELLER, refreshToken), seller.getId(),
                AuthConsts.REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForSet().add(AuthConsts.userRefreshesKey(UserRole.SELLER, seller.getId()), refreshToken);
        redisTemplate.expire(
                AuthConsts.userRefreshesKey(UserRole.SELLER, seller.getId()),
                AuthConsts.REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);

        return new LoginResult(new LoginResponse(accessToken), refreshToken);
    }

    public LoginResult refresh(String refreshToken) {
        Long userId = (Long) redisTemplate.opsForValue()
                .getAndDelete(AuthConsts.refreshKey(UserRole.SELLER, refreshToken));
        if (userId == null) {
            // 구 키(refresh:{token})에는 role 구분자가 없어 크로스-롤 발급을 막을 수 없다.
            // 값을 신뢰하지 않고 지우기만 한 뒤 재로그인을 유도한다. TTL(7일) 경과 후 이 블록을 제거한다.
            redisTemplate.opsForValue().getAndDelete(AuthConsts.legacyRefreshKey(refreshToken));
            throw new BaseException(ErrorEnum.TOKEN_EXPIRED);
        }

        Seller seller = sellerRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.SELLER_NOT_FOUND)
        );
        if (seller.getDeletedAt() != null) {
            throw new BaseException(ErrorEnum.SELLER_ALREADY_DELETED);
        }

        String newRefreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                AuthConsts.refreshKey(UserRole.SELLER, newRefreshToken), userId,
                AuthConsts.REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForSet().remove(AuthConsts.userRefreshesKey(UserRole.SELLER, userId), refreshToken);
        redisTemplate.opsForSet().add(AuthConsts.userRefreshesKey(UserRole.SELLER, userId), newRefreshToken);
        redisTemplate.expire(
                AuthConsts.userRefreshesKey(UserRole.SELLER, userId),
                AuthConsts.REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);

        String newAccessToken = jwtProvider.generateToken(userId, UserRole.SELLER);
        return new LoginResult(new LoginResponse(newAccessToken), newRefreshToken);
    }

    public void logout(String accessToken) {
        if (jwtProvider.getRole(accessToken) != UserRole.SELLER) {
            throw new BaseException(ErrorEnum.FORBIDDEN);
        }

        Long userId = jwtProvider.getUserId(accessToken);
        var tokens = redisTemplate.opsForSet().members(AuthConsts.userRefreshesKey(UserRole.SELLER, userId));
        if (tokens != null && !tokens.isEmpty()) {
            tokens.forEach(token ->
                    redisTemplate.delete(AuthConsts.refreshKey(UserRole.SELLER, (String) token)));
            redisTemplate.delete(AuthConsts.userRefreshesKey(UserRole.SELLER, userId));
        }
        long remaining = jwtProvider.getRemainingExpiration(accessToken);
        if (remaining > 0) {
            redisTemplate.opsForValue()
                    .set(AuthConsts.BLACKLIST_KEY_PREFIX + accessToken, "logout", remaining, TimeUnit.MILLISECONDS);
        }
    }
}

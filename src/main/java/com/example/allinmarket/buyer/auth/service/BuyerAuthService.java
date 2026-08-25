package com.example.allinmarket.buyer.auth.service;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.auth.consts.AuthConsts;
import com.example.allinmarket.common.auth.dto.LoginResponse;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BuyerAuthService {
    private final BuyerRepository buyerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CartRepository cartRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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

        Cart cart = Cart.of(buyer);

        Cart savedCart = cartRepository.save(cart);

        return BuyerAuthResponse.from(savedBuyer);
    }

    public LoginResult login(BuyerLoginRequest request) {
        Buyer buyer = buyerRepository.findByEmail(request.email()).orElseThrow(
                () -> {
                    log.warn("로그인 실패: {}", ErrorEnum.BUYER_NOT_FOUND.getMessage());
                    return new BaseException(ErrorEnum.LOGIN_FAILED);
                }
        );

        if (buyer.getDeletedAt() != null) {
            log.warn("로그인 실패: {}", ErrorEnum.BUYER_ALREADY_DELETED);
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(request.password(), buyer.getPassword())) {
            log.warn("로그인 실패: {}", ErrorEnum.PASSWORD_MISMATCH);
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        String accessToken = jwtProvider.generateToken(buyer.getId(), buyer.getRole());
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                AuthConsts.refreshKey(UserRole.BUYER, refreshToken), buyer.getId(),
                AuthConsts.REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForSet().add(AuthConsts.userRefreshesKey(UserRole.BUYER, buyer.getId()), refreshToken);
        redisTemplate.expire(
                AuthConsts.userRefreshesKey(UserRole.BUYER, buyer.getId()),
                AuthConsts.REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);

        LoginResponse response = new LoginResponse(accessToken);
        return new LoginResult(response, refreshToken);
    }

    public LoginResult refresh(String refreshToken) {
        Long userId = (Long) redisTemplate.opsForValue()
                .getAndDelete(AuthConsts.refreshKey(UserRole.BUYER, refreshToken));
        if (userId == null) {
            // 구 키(refresh:{token})에는 role 구분자가 없어 크로스-롤 발급을 막을 수 없다.
            // 값을 신뢰하지 않고 지우기만 한 뒤 재로그인을 유도한다. TTL(7일) 경과 후 이 블록을 제거한다.
            redisTemplate.opsForValue().getAndDelete(AuthConsts.legacyRefreshKey(refreshToken));
            throw new BaseException(ErrorEnum.TOKEN_EXPIRED);
        }

        Buyer buyer = buyerRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );
        if (buyer.getDeletedAt() != null) {
            throw new BaseException(ErrorEnum.BUYER_ALREADY_DELETED);
        }

        String newRefreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                AuthConsts.refreshKey(UserRole.BUYER, newRefreshToken), userId,
                AuthConsts.REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForSet().remove(AuthConsts.userRefreshesKey(UserRole.BUYER, userId), refreshToken);
        redisTemplate.opsForSet().add(AuthConsts.userRefreshesKey(UserRole.BUYER, userId), newRefreshToken);
        redisTemplate.expire(
                AuthConsts.userRefreshesKey(UserRole.BUYER, userId),
                AuthConsts.REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);

        String newAccessToken = jwtProvider.generateToken(userId, UserRole.BUYER);
        return new LoginResult(new LoginResponse(newAccessToken), newRefreshToken);
    }

    public void logout(String accessToken) {
        if (jwtProvider.getRole(accessToken) != UserRole.BUYER) {
            throw new BaseException(ErrorEnum.FORBIDDEN);
        }

        Long userId = jwtProvider.getUserId(accessToken);
        var tokens = redisTemplate.opsForSet().members(AuthConsts.userRefreshesKey(UserRole.BUYER, userId));
        if (tokens != null && !tokens.isEmpty()) {
            tokens.forEach(token ->
                    redisTemplate.delete(AuthConsts.refreshKey(UserRole.BUYER, (String) token)));
            redisTemplate.delete(AuthConsts.userRefreshesKey(UserRole.BUYER, userId));
        }
        long remaining = jwtProvider.getRemainingExpiration(accessToken);
        if (remaining > 0) {
            redisTemplate.opsForValue()
                    .set(AuthConsts.BLACKLIST_KEY_PREFIX + accessToken, "logout", remaining, TimeUnit.MILLISECONDS);
        }
    }
}

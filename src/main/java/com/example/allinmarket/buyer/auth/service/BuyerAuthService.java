package com.example.allinmarket.buyer.auth.service;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.auth.dto.response.BuyerLoginResponse;
import com.example.allinmarket.buyer.auth.dto.response.LoginResult;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
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
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        if (buyer.getDeletedAt() != null) {
            throw new BaseException(ErrorEnum.BUYER_ALREADY_DELETED);
        }

        if (!passwordEncoder.matches(request.password(), buyer.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_MISMATCH);
        }

        String accessToken = jwtProvider.generateToken(buyer.getId(), buyer.getRole());
        String refreshToken = UUID.randomUUID().toString();

        // Refresh 토큰 유효기간 일주일로 설정
        redisTemplate.opsForValue().set("refresh:" + refreshToken, buyer.getId(), 7, TimeUnit.DAYS);

        BuyerLoginResponse response = new BuyerLoginResponse(accessToken);
        return new LoginResult(response, refreshToken);
    }

    public LoginResult refresh(String refreshToken) {
        Long userId = (Long) redisTemplate.opsForValue().get("refresh:" + refreshToken);
        if (userId == null) {
            throw new BaseException(ErrorEnum.TOKEN_EXPIRED);
        }

        redisTemplate.delete("refresh:" + refreshToken);
        String newRefreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("refresh:" + newRefreshToken, userId, 7, TimeUnit.DAYS);

        String newAccessToken = jwtProvider.generateToken(userId, UserRole.BUYER);
        return new LoginResult(new BuyerLoginResponse(newAccessToken), newRefreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        long remaining = jwtProvider.getRemainingExpiration(accessToken);
        redisTemplate.opsForValue()
                .set("blacklist:" + accessToken, "logout", remaining, TimeUnit.MILLISECONDS);

        redisTemplate.delete("refresh:" + refreshToken);
    }
}

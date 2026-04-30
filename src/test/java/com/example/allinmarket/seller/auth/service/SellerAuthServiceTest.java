package com.example.allinmarket.seller.auth.service;

import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellerAuthServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private SellerDashboardRepository sellerDashboardRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private SellerAuthService sellerAuthService;

    @Test
    void 로그인_성공_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

        Seller seller = mock(Seller.class);
        given(seller.getStatus()).willReturn(SellerStatus.APPROVED);
        given(seller.getDeletedAt()).willReturn(null);
        given(seller.getPassword()).willReturn("encodedPassword");
        given(seller.getId()).willReturn(1L);
        given(seller.getRole()).willReturn(UserRole.SELLER);

        given(sellerRepository.findByEmail("seller@test.com")).willReturn(Optional.of(seller));
        given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
        given(jwtProvider.generateToken(1L, UserRole.SELLER)).willReturn("jwt.token.here");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        LoginResult result = sellerAuthService.login(request);

        assertThat(result.response().accessToken()).isEqualTo("jwt.token.here");
        assertThat(result.refreshToken()).isNotNull();
    }

    @Test
    void 로그인_이메일_없는_판매자_예외_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("notfound@test.com", "password123");

        given(sellerRepository.findByEmail("notfound@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
    }

    @Test
    void 로그인_승인_대기_판매자_예외_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

        Seller seller = mock(Seller.class);
        given(seller.getStatus()).willReturn(SellerStatus.PENDING);

        given(sellerRepository.findByEmail("seller@test.com")).willReturn(Optional.of(seller));

        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
    }

    @Test
    void 로그인_탈퇴한_판매자_예외_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "password123");

        Seller seller = mock(Seller.class);
        given(seller.getDeletedAt()).willReturn(LocalDateTime.now());

        given(sellerRepository.findByEmail("seller@test.com")).willReturn(Optional.of(seller));

        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
    }

    @Test
    void 로그인_비밀번호_불일치_예외_테스트() {
        SellerLoginRequest request = new SellerLoginRequest("seller@test.com", "wrongPassword");

        Seller seller = mock(Seller.class);
        given(seller.getStatus()).willReturn(SellerStatus.APPROVED);
        given(seller.getDeletedAt()).willReturn(null);
        given(seller.getPassword()).willReturn("encodedPassword");

        given(sellerRepository.findByEmail("seller@test.com")).willReturn(Optional.of(seller));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        assertThatThrownBy(() -> sellerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.LOGIN_FAILED.getMessage());
    }

    @Test
    void 토큰_재발급_성공_테스트() {
        // given
        Seller seller = mock(Seller.class);
        given(seller.getDeletedAt()).willReturn(null);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(valueOperations.getAndDelete("refresh:old-refresh-token")).willReturn(1L);
        given(sellerRepository.findById(1L)).willReturn(Optional.of(seller));
        given(jwtProvider.generateToken(1L, UserRole.SELLER)).willReturn("new-accessToken");

        LoginResult result = sellerAuthService.refresh("old-refresh-token");

        assertThat(result.response().accessToken()).isEqualTo("new-accessToken");
        assertThat(result.refreshToken()).isNotNull();
        assertThat(result.refreshToken()).isNotEqualTo("old-refresh-token");
        verify(valueOperations).set(anyString(), eq(1L), eq(7L), eq(TimeUnit.DAYS));
    }

    @Test
    void 토큰_재발급_실패_만료된_토큰_테스트() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.getAndDelete("refresh:expired-token")).willReturn(null);

        assertThatThrownBy(() -> sellerAuthService.refresh("expired-token"))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.TOKEN_EXPIRED.getMessage());
    }
}

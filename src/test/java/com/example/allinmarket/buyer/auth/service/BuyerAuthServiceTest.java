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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BuyerAuthServiceTest {
    @Mock
    private BuyerRepository buyerRepository;

    @InjectMocks
    private BuyerAuthService buyerAuthService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Test
    void 회원_가입_성공_테스트() {
        // given
        BuyerSignupRequest request = new BuyerSignupRequest(
                "테스트@테스트.com",
                "12345678",
                "테스트",
                "010-1234-1234"
        );

        given(buyerRepository.existsByEmail("테스트@테스트.com")).willReturn(false);
        given(passwordEncoder.encode("12345678")).willReturn("암호화");

        Buyer savedBuyer = Buyer.of(
                request.email(),
                "암호화",
                request.name(),
                request.phone()
        );

        given(buyerRepository.save(any(Buyer.class))).willReturn(savedBuyer);

        Cart savedCart = Cart.of(savedBuyer);

        given(cartRepository.save(any(Cart.class))).willReturn(savedCart);

        // when
        BuyerAuthResponse response = buyerAuthService.signup(request);

        // then
        assertThat(response.email()).isEqualTo("테스트@테스트.com");
        assertThat(response.name()).isEqualTo("테스트");
    }

    @Test
    void 회원_가입_실패_테스트() {
        // given
        BuyerSignupRequest request = new BuyerSignupRequest(
                "테스트@테스트.com",
                "12345678",
                "테스트",
                "010-1234-1234"
        );

        given(buyerRepository.existsByEmail("테스트@테스트.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> buyerAuthService.signup(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.EMAIL_ALREADY_EXISTS.getMessage());
    }

    @Test
    void 로그인_성공_테스트() {
        // given
        BuyerLoginRequest request = new BuyerLoginRequest(
                "테스트@테스트.com",
                "12345678"
        );

        Buyer buyer = Buyer.of(
                "테스트@테스트.com",
                "비밀번호암호화",
                "테스트",
                "010-1234-1234"
        );

        ReflectionTestUtils.setField(buyer, "id", 1L);
        ReflectionTestUtils.setField(buyer, "role", UserRole.BUYER);

        given(buyerRepository.findByEmail("테스트@테스트.com")).willReturn(Optional.of(buyer));
        given(passwordEncoder.matches("12345678", "비밀번호암호화")).willReturn(true);
        given(jwtProvider.generateToken(buyer.getId(), buyer.getRole())).willReturn("test-accessToken");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        LoginResult result = buyerAuthService.login(request);

        // then
        assertThat(result.response().accessToken()).isEqualTo("test-accessToken");
        assertThat(result.refreshToken()).isNotNull();
    }

    @Test
    void 로그인_실패_이메일_없음_테스트() {
        // given
        BuyerLoginRequest request = new BuyerLoginRequest(
                "테스트@테스트.com",
                "12345678"
        );

        given(buyerRepository.findByEmail("테스트@테스트.com"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> buyerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.BUYER_NOT_FOUND.getMessage());
    }

    @Test
    void 로그인_실패_탈퇴회원_테스트() {
        // given
        BuyerLoginRequest request = new BuyerLoginRequest(
                "테스트@테스트.com",
                "12345678"
        );

        Buyer buyer = Buyer.of(
                "테스트@테스트.com",
                "비밀번호암호화",
                "테스트",
                "010-1234-1234"
        );
        buyer.delete();

        given(buyerRepository.findByEmail("테스트@테스트.com")).willReturn(Optional.of(buyer));

        // when & then
        assertThatThrownBy(() -> buyerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.BUYER_ALREADY_DELETED.getMessage());
    }

    @Test
    void 로그인_실패_비밀번호_불일치_테스트() {
        // given
        BuyerLoginRequest request = new BuyerLoginRequest(
                "테스트@테스트.com",
                "틀린비밀번호"
        );

        Buyer buyer = Buyer.of(
                "테스트@테스트.com",
                "비밀번호암호화",
                "테스트",
                "010-1234-1234"
        );

        given(buyerRepository.findByEmail("테스트@테스트.com")).willReturn(Optional.of(buyer));
        given(passwordEncoder.matches("틀린비밀번호", "비밀번호암호화")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> buyerAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.PASSWORD_MISMATCH.getMessage());
    }

    @Test
    void 토큰_재발급_성공_테스트() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:old-refresh-token")).willReturn(1L);
        given(jwtProvider.generateToken(1L, UserRole.BUYER)).willReturn("new-accessToken");

        // when
        LoginResult result = buyerAuthService.refresh("old-refresh-token");

        // then
        assertThat(result.response().accessToken()).isEqualTo("new-accessToken");
        assertThat(result.refreshToken()).isNotNull();
        assertThat(result.refreshToken()).isNotEqualTo("old-refresh-token");
        verify(redisTemplate).delete("refresh:old-refresh-token");
        verify(valueOperations).set(anyString(), eq(1L), eq(7L), eq(TimeUnit.DAYS));
    }

    @Test
    void 토큰_재발급_실패_만료된_토큰_테스트() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("refresh:expired-token")).willReturn(null);

        // when & then
        assertThatThrownBy(() -> buyerAuthService.refresh("expired-token"))
                .isInstanceOf(BaseException.class)
                .hasMessage(ErrorEnum.TOKEN_EXPIRED.getMessage());
    }
}

package com.example.allinmarket.buyer.auth.service;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.auth.dto.response.BuyerLoginResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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

        // when
        BuyerLoginResponse response = buyerAuthService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("test-accessToken");
    }

    @Test
    void 로그인_실패_테스트() {
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
}

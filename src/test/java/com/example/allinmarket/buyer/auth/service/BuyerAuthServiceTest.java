package com.example.allinmarket.buyer.auth.service;

import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
}

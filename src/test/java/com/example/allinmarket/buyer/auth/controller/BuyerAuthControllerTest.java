package com.example.allinmarket.buyer.auth.controller;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.auth.dto.response.BuyerLoginResponse;
import com.example.allinmarket.buyer.auth.dto.response.LoginResult;
import com.example.allinmarket.buyer.auth.service.BuyerAuthService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.security.JwtAuthenticationFilter;
import com.example.allinmarket.common.security.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@WebMvcTest(BuyerAuthController.class)
@AutoConfigureRestTestClient
public class BuyerAuthControllerTest {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private BuyerAuthService buyerAuthService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void 회원_가입_성공_테스트() {
        // given
        BuyerSignupRequest request = new BuyerSignupRequest(
                "테스트@테스트.com",
                "12345678",
                "테스트",
                "010-1234-1234"
        );

        BuyerAuthResponse response = new BuyerAuthResponse(
                "테스트@테스트.com",
                "테스트",
                "010-1234-1234"
        );

        when(buyerAuthService.signup(request)).thenReturn(response);

        // when & then
        restTestClient.post().uri("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo(SuccessEnum.REGISTER_SUCCESS.getMessage())
                .jsonPath("$.data.email").isEqualTo("테스트@테스트.com")
                .jsonPath("$.data.name").isEqualTo("테스트")
                .jsonPath("$.data.phone").isEqualTo("010-1234-1234");
    }

    @Test
    void 회원_가입_실패_테스트() {
        // given
        BuyerSignupRequest request = new BuyerSignupRequest(
                "이메일형식오류",
                "12345678",
                "테스트",
                "전화번호형식오류"
        );

        // when & then
        restTestClient.post()
                .uri("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").exists();
    }

    @Test
    void 로그인_성공_테스트() {
        // given
        BuyerLoginRequest request = new BuyerLoginRequest(
                "테스트@테스트.com",
                "12345678"
        );

        LoginResult loginResult = new LoginResult(
                new BuyerLoginResponse("test-accessToken"),
                "test-refreshToken"
        );

        given(buyerAuthService.login(any(BuyerLoginRequest.class))).willReturn(loginResult);

        // when & then
        restTestClient.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", ".*refreshToken=test-refreshToken.*")
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.LOGIN_SUCCESS.getMessage())
                .jsonPath("$.data.accessToken").isEqualTo("test-accessToken");
    }

    @Test
    void 로그인_실패_테스트() {
        // given
        BuyerLoginRequest request = new BuyerLoginRequest(
                "테스트@테스트.com",
                ""
        );

        // when & then
        restTestClient.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").exists();
    }
}

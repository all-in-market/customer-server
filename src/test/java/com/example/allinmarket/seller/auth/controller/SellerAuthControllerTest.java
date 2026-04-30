package com.example.allinmarket.seller.auth.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtAuthenticationFilter;
import com.example.allinmarket.common.security.LoginRateLimitFilter;
import com.example.allinmarket.seller.auth.dto.request.SellerCreateRequest;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.auth.dto.response.SellerCreateResponse;
import com.example.allinmarket.seller.auth.dto.response.SellerLoginResponse;
import com.example.allinmarket.seller.auth.dto.response.SellerLoginResult;
import com.example.allinmarket.seller.auth.service.SellerAuthService;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.common.enums.UserRole;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(SellerAuthController.class)
@AutoConfigureRestTestClient
public class SellerAuthControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private LoginRateLimitFilter loginRateLimitFilter;

    @MockitoBean
    private SellerAuthService sellerAuthService;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(loginRateLimitFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void 판매자_회원가입_성공_테스트() {
        SellerCreateResponse response = new SellerCreateResponse(
                1L,
                "seller@test.com",
                "홍길동",
                "010-1234-5678",
                "홍길동상점",
                "123-45-67890",
                SellerStatus.PENDING,
                UserRole.SELLER
        );

        when(sellerAuthService.signup(any(SellerCreateRequest.class))).thenReturn(response);

        restTestClient.post().uri("/seller/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "seller@test.com",
                            "password": "password123",
                            "name": "홍길동",
                            "phone": "010-1234-5678",
                            "storeName": "홍길동상점",
                            "bizNumber": "123-45-67890",
                            "bankCode": "KOOKMIN",
                            "bankAccount": "123-456-7890"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.data.email").isEqualTo("seller@test.com")
                .jsonPath("$.data.status").isEqualTo("PENDING")
                .jsonPath("$.data.role").isEqualTo("SELLER");
    }

    @Test
    void 판매자_회원가입_이메일_형식_오류_테스트() {
        restTestClient.post().uri("/seller/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "not-an-email",
                            "password": "password123",
                            "name": "홍길동",
                            "phone": "010-1234-5678",
                            "storeName": "홍길동상점",
                            "bizNumber": "123-45-67890",
                            "bankCode": "KOOKMIN",
                            "bankAccount": "123-456-7890"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400);

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_회원가입_비밀번호_길이_오류_테스트() {
        restTestClient.post().uri("/seller/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "seller@test.com",
                            "password": "short",
                            "name": "홍길동",
                            "phone": "010-1234-5678",
                            "storeName": "홍길동상점",
                            "bizNumber": "123-45-67890",
                            "bankCode": "KOOKMIN",
                            "bankAccount": "123-456-7890"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("비밀번호는 8자 이상 20자 이하여야 합니다.");

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_회원가입_이름_공백_오류_테스트() {
        restTestClient.post().uri("/seller/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "seller@test.com",
                            "password": "password123",
                            "name": "   ",
                            "phone": "010-1234-5678",
                            "storeName": "홍길동상점",
                            "bizNumber": "123-45-67890",
                            "bankCode": "KOOKMIN",
                            "bankAccount": "123-456-7890"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400);

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_로그인_성공_테스트() {
        SellerLoginResult loginResult = new SellerLoginResult(
                new SellerLoginResponse("jwt.token.here"),
                "test-refresh-token"
        );

        when(sellerAuthService.login(any(SellerLoginRequest.class))).thenReturn(loginResult);

        restTestClient.post().uri("/seller/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "seller@test.com",
                            "password": "password123"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", ".*refreshToken=test-refresh-token.*")
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo(SuccessEnum.LOGIN_SUCCESS.getMessage())
                .jsonPath("$.data.accessToken").isEqualTo("jwt.token.here");
    }

    @Test
    void 판매자_로그인_이메일_형식_오류_테스트() {
        restTestClient.post().uri("/seller/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "not-an-email",
                            "password": "password123"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400);

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_로그인_비밀번호_길이_오류_테스트() {
        restTestClient.post().uri("/seller/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "seller@test.com",
                            "password": "short"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("비밀번호는 8자 이상 20자 이하여야 합니다.");

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_로그인_판매자_없음_예외_테스트() {
        when(sellerAuthService.login(any(SellerLoginRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        restTestClient.post().uri("/seller/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "seller@test.com",
                            "password": "password123"
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo(ErrorEnum.SELLER_NOT_FOUND.getMessage());
    }

    @Test
    void 판매자_로그인_비밀번호_불일치_예외_테스트() {
        when(sellerAuthService.login(any(SellerLoginRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.PASSWORD_MISMATCH));

        restTestClient.post().uri("/seller/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "seller@test.com",
                            "password": "wrongPassword"
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo(ErrorEnum.PASSWORD_MISMATCH.getMessage());
    }

    @Test
    void 판매자_토큰_재발급_성공_테스트() {
        SellerLoginResult loginResult = new SellerLoginResult(
                new SellerLoginResponse("new-accessToken"),
                "new-refresh-token"
        );

        given(sellerAuthService.refresh("valid-refresh-token")).willReturn(loginResult);

        restTestClient.post().uri("/seller/auth/refresh")
                .cookie("refreshToken", "valid-refresh-token")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", ".*refreshToken=new-refresh-token.*")
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isEqualTo(SuccessEnum.TOKEN_REFRESHED.getMessage())
                .jsonPath("$.data.accessToken").isEqualTo("new-accessToken");
    }

    @Test
    void 판매자_토큰_재발급_실패_만료된_토큰_테스트() {
        given(sellerAuthService.refresh(anyString()))
                .willThrow(new BaseException(ErrorEnum.TOKEN_EXPIRED));

        restTestClient.post().uri("/seller/auth/refresh")
                .cookie("refreshToken", "expired-token")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(ErrorEnum.TOKEN_EXPIRED.getStatus())
                .jsonPath("$.message").isEqualTo(ErrorEnum.TOKEN_EXPIRED.getMessage());
    }
}

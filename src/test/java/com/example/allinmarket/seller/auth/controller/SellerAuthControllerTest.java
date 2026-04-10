package com.example.allinmarket.seller.auth.controller;

import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.seller.auth.dto.request.SellerCreateRequest;
import com.example.allinmarket.seller.auth.dto.response.SellerCreateResponse;
import com.example.allinmarket.seller.auth.service.SellerAuthService;
import com.example.allinmarket.seller.enums.SellerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.auth.dto.response.SellerLoginResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(SellerAuthController.class)
@AutoConfigureRestTestClient
public class SellerAuthControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SellerAuthService sellerAuthService;

    @Test
    void 판매자_회원가입_성공_테스트() {
        // given
        SellerCreateResponse response = new SellerCreateResponse(
                1L,
                "seller@test.com",
                "홍길동",
                "010-1234-5678",
                "홍길동상점",
                "123-45-67890",
                "110-123-456789",
                SellerStatus.PENDING,
                UserRole.SELLER
        );

        when(sellerAuthService.signup(any(SellerCreateRequest.class))).thenReturn(response);

        // when & then
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
                            "bankAccount": "110-123-456789"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo("데이터 생성에 성공하였습니다.")
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.email").isEqualTo("seller@test.com")
                .jsonPath("$.data.name").isEqualTo("홍길동")
                .jsonPath("$.data.phone").isEqualTo("010-1234-5678")
                .jsonPath("$.data.storeName").isEqualTo("홍길동상점")
                .jsonPath("$.data.bizNumber").isEqualTo("123-45-67890")
                .jsonPath("$.data.bankAccount").isEqualTo("110-123-456789")
                .jsonPath("$.data.status").isEqualTo("PENDING")
                .jsonPath("$.data.role").isEqualTo("SELLER");
    }

    @Test
    void 판매자_회원가입_이메일_형식_오류_테스트() {
        // when & then
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
                            "bankAccount": "110-123-456789"
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
        // when & then
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
                            "bankAccount": "110-123-456789"
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
        // when & then
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
                            "bankAccount": "110-123-456789"
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
        // given
        SellerLoginResponse response = new SellerLoginResponse("jwt.token.here");

        when(sellerAuthService.login(any(SellerLoginRequest.class))).thenReturn(response);

        // when & then
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
                .expectHeader().valueEquals("Authorization", "Bearer jwt.token.here")
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.accessToken").isEqualTo("jwt.token.here");
    }

    @Test
    void 판매자_로그인_이메일_형식_오류_테스트() {
        // when & then
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
        // when & then
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
        // given
        when(sellerAuthService.login(any(SellerLoginRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        // when & then
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
        // given
        when(sellerAuthService.login(any(SellerLoginRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.PASSWORD_MISMATCH));

        // when & then
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
}

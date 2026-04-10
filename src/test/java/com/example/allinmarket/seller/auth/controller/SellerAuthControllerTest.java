package com.example.allinmarket.seller.auth.controller;

import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.seller.auth.dto.SellerCreateRequest;
import com.example.allinmarket.seller.auth.dto.SellerCreateResponse;
import com.example.allinmarket.seller.auth.service.SellerAuthService;
import com.example.allinmarket.seller.enums.SellerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
                .expectStatus().isOk()
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
}

package com.example.allinmarket.seller.me.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.me.dto.request.SellerUpdateRequest;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.me.service.SellerMeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebMvcTest(SellerMeController.class)
@AutoConfigureRestTestClient
public class SellerMeControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SellerMeService sellerMeService;

    // SecurityUtils가 (Long) authentication.getPrincipal()로 캐스팅하므로
    // principal을 반드시 Long 타입으로 설정해야 합니다.
    private void setAuthContext(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 판매자_내_정보_조회_성공_테스트() {
        // given
        setAuthContext(1L);

        SellerDetailResponse response = new SellerDetailResponse(
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

        when(sellerMeService.getMyProfile(1L)).thenReturn(response);

        // when & then
        restTestClient.get().uri("/sellers/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("데이터 조회에 성공하였습니다.")
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
    void 판매자_내_정보_조회_미인증_예외_테스트() {
        // given - SecurityContext 비어있는 상태 (인증 없음)
        SecurityContextHolder.clearContext();

        // when & then
        restTestClient.get().uri("/sellers/me")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void 판매자_내_정보_조회_존재하지_않는_판매자_예외_테스트() {
        // given
        setAuthContext(999L);

        when(sellerMeService.getMyProfile(999L))
                .thenThrow(new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        // when & then
        restTestClient.get().uri("/sellers/me")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo(ErrorEnum.SELLER_NOT_FOUND.getMessage());
    }

    @Test
    void 판매자_내_정보_수정_성공_테스트() {
        // given
        setAuthContext(1L);

        SellerDetailResponse response = new SellerDetailResponse(
                1L,
                "updated@test.com",
                "김철수",
                "010-9999-8888",
                "김철수상점",
                "987-65-43210",
                "220-999-123456",
                SellerStatus.PENDING,
                UserRole.SELLER
        );

        when(sellerMeService.updateMyProfile(eq(1L), any(SellerUpdateRequest.class))).thenReturn(response);

        // when & then
        restTestClient.put().uri("/sellers/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "updated@test.com",
                            "password": "newpassword1",
                            "name": "김철수",
                            "phone": "010-9999-8888",
                            "storeName": "김철수상점",
                            "bizNumber": "987-65-43210",
                            "bankAccount": "220-999-123456"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("데이터 수정에 성공하였습니다.")
                .jsonPath("$.data.email").isEqualTo("updated@test.com")
                .jsonPath("$.data.name").isEqualTo("김철수")
                .jsonPath("$.data.storeName").isEqualTo("김철수상점");
    }

    @Test
    void 판매자_내_정보_수정_미인증_예외_테스트() {
        // given - SecurityContext 비어있는 상태
        SecurityContextHolder.clearContext();

        // when & then
        restTestClient.put().uri("/sellers/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "updated@test.com",
                            "password": "newpassword1",
                            "name": "김철수",
                            "phone": "010-9999-8888",
                            "storeName": "김철수상점",
                            "bizNumber": "987-65-43210",
                            "bankAccount": "220-999-123456"
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void 판매자_내_정보_수정_존재하지_않는_판매자_예외_테스트() {
        // given
        setAuthContext(999L);

        when(sellerMeService.updateMyProfile(eq(999L), any(SellerUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        // when & then
        restTestClient.put().uri("/sellers/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "updated@test.com",
                            "password": "newpassword1",
                            "name": "김철수",
                            "phone": "010-9999-8888",
                            "storeName": "김철수상점",
                            "bizNumber": "987-65-43210",
                            "bankAccount": "220-999-123456"
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
    void 판매자_내_정보_수정_비밀번호_길이_미달_예외_테스트() {
        // given - password 7자 (min=8)
        setAuthContext(1L);

        // when & then
        restTestClient.put().uri("/sellers/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "updated@test.com",
                            "password": "pass123",
                            "name": "김철수",
                            "phone": "010-9999-8888",
                            "storeName": "김철수상점",
                            "bizNumber": "987-65-43210",
                            "bankAccount": "220-999-123456"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("비밀번호는 8자 이상 20자 이하여야 합니다.");

        verifyNoInteractions(sellerMeService);
    }

    @Test
    void 판매자_내_정보_수정_이메일_형식_예외_테스트() {
        // given - 이메일 형식 오류
        setAuthContext(1L);

        // when & then
        restTestClient.put().uri("/sellers/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "email": "invalid-email",
                            "password": "newpassword1",
                            "name": "김철수",
                            "phone": "010-9999-8888",
                            "storeName": "김철수상점",
                            "bizNumber": "987-65-43210",
                            "bankAccount": "220-999-123456"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400);

        verifyNoInteractions(sellerMeService);
    }
}

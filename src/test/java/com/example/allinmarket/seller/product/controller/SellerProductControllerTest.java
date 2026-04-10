package com.example.allinmarket.seller.product.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.product.service.SellerProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(SellerProductController.class)
@AutoConfigureRestTestClient
public class SellerProductControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SellerProductService sellerProductService;

    @Test
    @WithMockUser
    void 판매자_상품_등록_성공_테스트() {
        // given
        ProductDetailResponse response = new ProductDetailResponse(
                1L,
                1L,
                "테스트 상품",
                BigDecimal.valueOf(10000),
                50,
                ProductStatus.ON_SALE,
                "상품 설명"
        );

        when(sellerProductService.create(any(SellerProductCreateRequest.class)))
                .thenReturn(response);

        String requestBody = """
            {
                "categoryId": 1,
                "name": "테스트 상품",
                "price": 10000,
                "stock": 50,
                "description": "상품 설명"
            }
            """;

        // when & then
        restTestClient.post().uri("/seller/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo("데이터 생성에 성공하였습니다.")
                .jsonPath("$.data.name").isEqualTo("테스트 상품")
                .jsonPath("$.data.price").isEqualTo(10000)
                .jsonPath("$.data.stock").isEqualTo(50)
                .jsonPath("$.data.status").isEqualTo("ON_SALE")
                .jsonPath("$.data.description").isEqualTo("상품 설명");
    }

    @Test
    @WithMockUser
    void 판매자_상품_등록_500에러_실패_테스트() {
        // given
        when(sellerProductService.create(any(SellerProductCreateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        String requestBody = """
            {
                "categoryId": 1,
                "name": "테스트 상품",
                "price": 10000,
                "stock": 50,
                "description": "상품 설명"
            }
            """;

        // when & then
        restTestClient.post().uri("/seller/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @WithMockUser
    void 판매자_상품_등록_유효성검사_실패_테스트() {
        // given - name 누락된 잘못된 요청
        String invalidRequestBody = """
            {
                "categoryId": 1,
                "price": 10000,
                "stock": 50,
                "description": "상품 설명"
            }
            """;

        // when & then
        restTestClient.post().uri("/seller/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequestBody)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}
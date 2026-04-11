package com.example.allinmarket.buyer.product.controller;

import com.example.allinmarket.buyer.product.service.BuyerProductService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@WebMvcTest(BuyerProductController.class)
@AutoConfigureRestTestClient
public class BuyerProductControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private BuyerProductService buyerProductService;

    @Test
    @WithMockUser
    void 구매자_상품_목록_조회_성공_테스트() {
        // given
        ProductDetailResponse response = new ProductDetailResponse(
                1L,
                null,
                null,
                "테스트",
                BigDecimal.valueOf(10000),
                50,
                ProductStatus.ON_SALE,
                "설명"
        );

        when(buyerProductService.findAllProducts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        // when & then
        restTestClient.get().uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("데이터 조회에 성공하였습니다.")
                .jsonPath("$.data.content[0].name").isEqualTo("테스트")
                .jsonPath("$.data.content[0].price").isEqualTo(10000)
                .jsonPath("$.data.content[0].stock").isEqualTo(50)
                .jsonPath("$.data.content[0].status").isEqualTo("ON_SALE")
                .jsonPath("$.data.content[0].description").isEqualTo("설명");
    }

    @Test
    @WithMockUser
    void 구매자_상품_목록_조회_500에러_실패_테스트() {
        // given
        when(buyerProductService.findAllProducts(any(Pageable.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        // when & then
        restTestClient.get().uri("/products")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @WithMockUser
    void 상품_상세_조회_성공_테스트() {
        // given
        ProductDetailResponse response = new ProductDetailResponse(
                1L,
                null,
                null,
                "상품 테스트",
                BigDecimal.valueOf(12000),
                30,
                ProductStatus.ON_SALE,
                "상품 설명"
        );

        given(buyerProductService.findOneProduct(any())).willReturn(response);

        // when & then
        restTestClient.get().uri("/products/{productId}", response.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.READ_SUCCESS.getMessage())
                .jsonPath("$.data.name").isEqualTo("상품 테스트")
                .jsonPath("$.data.price").isEqualTo(12000)
                .jsonPath("$.data.stock").isEqualTo(30)
                .jsonPath("$.data.status").isEqualTo("ON_SALE")
                .jsonPath("$.data.description").isEqualTo("상품 설명");
    }

    @Test
    @WithMockUser
    void 상품_상세_조회_실패_테스트() {
        // given
        given(buyerProductService.findOneProduct(any())).willThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        // when & then
        restTestClient.get().uri("/products/1")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo(ErrorEnum.PRODUCT_NOT_FOUND.getMessage())
                .jsonPath("$.data").isEmpty();
    }
}

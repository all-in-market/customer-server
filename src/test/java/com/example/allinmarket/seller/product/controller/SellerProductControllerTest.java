package com.example.allinmarket.seller.product.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductStockUpdateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
import com.example.allinmarket.seller.product.service.SellerProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

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
    void 판매자_상품_등록_성공_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        ProductDetailResponse response = new ProductDetailResponse(
                1L,
                1L,
                1L,
                "테스트 상품",
                BigDecimal.valueOf(10000),
                50,
                ProductStatus.ON_SALE,
                "상품 설명"
        );

        when(sellerProductService.create(any(Long.class), any(SellerProductCreateRequest.class)))
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
    void 판매자_상품_등록_500에러_실패_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(sellerProductService.create(any(Long.class), any(SellerProductCreateRequest.class)))
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
    void 판매자_상품_등록_유효성검사_실패_테스트() {
        // given - name 누락된 잘못된 요청
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

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

    @Test
    void 판매자_상품_수정_성공_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        ProductDetailResponse response = new ProductDetailResponse(
                1L,
                1L,
                1L,
                "수정된 상품",
                BigDecimal.valueOf(20000),
                50,
                ProductStatus.ON_SALE,
                "수정된 설명"
        );

        when(sellerProductService.update(any(Long.class), any(Long.class), any(SellerProductUpdateRequest.class)))
                .thenReturn(response);

        String requestBody = """
        {
            "categoryId": 1,
            "name": "수정된 상품",
            "price": 20000,
            "status": "ON_SALE",
            "description": "수정된 설명"
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.name").isEqualTo("수정된 상품")
                .jsonPath("$.data.price").isEqualTo(20000)
                .jsonPath("$.data.status").isEqualTo("ON_SALE")
                .jsonPath("$.data.description").isEqualTo("수정된 설명");
    }

    @Test
    void 판매자_상품_수정_일부필드만_성공_테스트() {
        // given - name만 수정
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        ProductDetailResponse response = new ProductDetailResponse(
                1L,
                1L,
                1L,
                "수정된 상품",
                BigDecimal.valueOf(20000),
                50,
                ProductStatus.ON_SALE,
                "수정된 설명"
        );

        when(sellerProductService.update(any(Long.class), any(Long.class), any(SellerProductUpdateRequest.class)))
                .thenReturn(response);

        String requestBody = """
        {
            "name": "수정된 상품"
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("수정된 상품");
    }

    @Test
    @WithMockUser
    void 판매자_상품_수정_유효성검사_상품명_초과_실패_테스트() {
        // given - name 200자 초과
        String longName = "a".repeat(201);
        String requestBody = String.format("""
        {
            "name": "%s"
        }
        """, longName);

        // when & then
        restTestClient.put().uri("/seller/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @WithMockUser
    void 판매자_상품_수정_유효성검사_잘못된_status_실패_테스트() {
        // given - 존재하지 않는 status 값
        String requestBody = """
        {
            "status": "INVALID_STATUS"
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @WithMockUser
    void 판매자_상품_수정_500에러_실패_테스트() {
        // given
        when(sellerProductService.update(any(Long.class), any(Long.class), any(SellerProductUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        String requestBody = """
        {
            "name": "수정된 상품"
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void 판매자_상품_삭제_성공_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        ProductDetailResponse response = new ProductDetailResponse(
                1L, 1L, 1L, "테스트 상품",
                BigDecimal.valueOf(10000), 50,
                ProductStatus.ON_SALE, "상품 설명"
        );

        when(sellerProductService.delete(any(Long.class), any(Long.class)))
                .thenReturn(response);

        // when & then
        restTestClient.delete().uri("/seller/products/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.name").isEqualTo("테스트 상품");
    }

    @Test
    void 판매자_상품_삭제_상품없음_실패_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(sellerProductService.delete(any(Long.class), any(Long.class)))
                .thenThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        // when & then
        restTestClient.delete().uri("/seller/products/999")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void 판매자_상품_삭제_권한없음_실패_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(sellerProductService.delete(any(Long.class), any(Long.class)))
                .thenThrow(new BaseException(ErrorEnum.FORBIDDEN));

        // when & then
        restTestClient.delete().uri("/seller/products/1")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser
    void 판매자_상품_삭제_500에러_실패_테스트() {
        // given
        when(sellerProductService.delete(any(Long.class), any(Long.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        // when & then
        restTestClient.delete().uri("/seller/products/1")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void 판매자_상품_재고수정_성공_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        ProductDetailResponse response = new ProductDetailResponse(
                1L, 1L, 1L, "테스트 상품",
                BigDecimal.valueOf(10000), 100,
                ProductStatus.ON_SALE, "상품 설명"
        );

        when(sellerProductService.stockUpdate(any(Long.class), any(Long.class), any(SellerProductStockUpdateRequest.class)))
                .thenReturn(response);

        String requestBody = """
        {
            "stock": 100
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.stock").isEqualTo(100);
    }

    @Test
    void 판매자_상품_재고수정_상품없음_실패_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(sellerProductService.stockUpdate(any(Long.class), any(Long.class), any(SellerProductStockUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        String requestBody = """
        {
            "stock": 100
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/999/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void 판매자_상품_재고수정_권한없음_실패_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(sellerProductService.stockUpdate(any(Long.class), any(Long.class), any(SellerProductStockUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.FORBIDDEN));

        String requestBody = """
        {
            "stock": 100
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser
    void 판매자_상품_재고수정_유효성검사_음수_실패_테스트() {
        // given - 음수 재고
        String requestBody = """
        {
            "stock": -1
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @WithMockUser
    void 판매자_상품_재고수정_500에러_실패_테스트() {
        // given
        when(sellerProductService.stockUpdate(any(Long.class), any(Long.class), any(SellerProductStockUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        String requestBody = """
        {
            "stock": 100
        }
        """;

        // when & then
        restTestClient.put().uri("/seller/products/1/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
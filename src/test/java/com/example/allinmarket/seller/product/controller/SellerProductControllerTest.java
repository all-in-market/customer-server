package com.example.allinmarket.seller.product.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductStockUpdateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
import com.example.allinmarket.seller.product.service.SellerProductService;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerProductController.class)
public class SellerProductControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private SellerProductService sellerProductService;

    @BeforeEach
    void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void 판매자_상품_등록_성공_테스트() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                1L, 1L, 1L, "테스트 상품", BigDecimal.valueOf(10000),
                50, ProductStatus.ON_SALE, "상품 설명"
        );
        when(sellerProductService.create(any(Long.class), any(SellerProductCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/seller/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "categoryId": 1,
                                    "name": "테스트 상품",
                                    "price": 10000,
                                    "stock": 50,
                                    "description": "상품 설명"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("데이터 생성에 성공하였습니다."))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"))
                .andExpect(jsonPath("$.data.price").value(10000))
                .andExpect(jsonPath("$.data.stock").value(50))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.description").value("상품 설명"));
    }

    @Test
    void 판매자_상품_등록_500에러_실패_테스트() throws Exception {
        when(sellerProductService.create(any(Long.class), any(SellerProductCreateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        mockMvc.perform(post("/seller/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "categoryId": 1,
                                    "name": "테스트 상품",
                                    "price": 10000,
                                    "stock": 50,
                                    "description": "상품 설명"
                                }
                                """))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void 판매자_상품_등록_유효성검사_실패_테스트() throws Exception {
        mockMvc.perform(post("/seller/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "categoryId": 1,
                                    "price": 10000,
                                    "stock": 50,
                                    "description": "상품 설명"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 판매자_상품_수정_성공_테스트() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                1L, 1L, 1L, "수정된 상품", BigDecimal.valueOf(20000),
                50, ProductStatus.ON_SALE, "수정된 설명"
        );
        when(sellerProductService.update(any(Long.class), any(Long.class), any(SellerProductUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/seller/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "categoryId": 1,
                                    "name": "수정된 상품",
                                    "price": 20000,
                                    "status": "ON_SALE",
                                    "description": "수정된 설명"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("수정된 상품"))
                .andExpect(jsonPath("$.data.price").value(20000))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andExpect(jsonPath("$.data.description").value("수정된 설명"));
    }

    @Test
    void 판매자_상품_수정_일부필드만_성공_테스트() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                1L, 1L, 1L, "수정된 상품", BigDecimal.valueOf(20000),
                50, ProductStatus.ON_SALE, "수정된 설명"
        );
        when(sellerProductService.update(any(Long.class), any(Long.class), any(SellerProductUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/seller/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "수정된 상품"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수정된 상품"));
    }

    @Test
    @WithMockUser
    void 판매자_상품_수정_유효성검사_상품명_초과_실패_테스트() throws Exception {
        mockMvc.perform(put("/seller/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + "a".repeat(201) + "\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    void 판매자_상품_수정_유효성검사_잘못된_status_실패_테스트() throws Exception {
        mockMvc.perform(put("/seller/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "status": "INVALID_STATUS"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    void 판매자_상품_수정_500에러_실패_테스트() throws Exception {
        when(sellerProductService.update(any(Long.class), any(Long.class), any(SellerProductUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        mockMvc.perform(put("/seller/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "수정된 상품"
                                }
                                """))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void 판매자_상품_삭제_성공_테스트() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                1L, 1L, 1L, "테스트 상품", BigDecimal.valueOf(10000),
                50, ProductStatus.ON_SALE, "상품 설명"
        );
        when(sellerProductService.delete(any(Long.class), any(Long.class))).thenReturn(response);

        mockMvc.perform(delete("/seller/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.name").value("테스트 상품"));
    }

    @Test
    void 판매자_상품_삭제_상품없음_실패_테스트() throws Exception {
        when(sellerProductService.delete(any(Long.class), any(Long.class)))
                .thenThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        mockMvc.perform(delete("/seller/products/999"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 판매자_상품_삭제_권한없음_실패_테스트() throws Exception {
        when(sellerProductService.delete(any(Long.class), any(Long.class)))
                .thenThrow(new BaseException(ErrorEnum.FORBIDDEN));

        mockMvc.perform(delete("/seller/products/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void 판매자_상품_삭제_500에러_실패_테스트() throws Exception {
        when(sellerProductService.delete(any(Long.class), any(Long.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        mockMvc.perform(delete("/seller/products/1"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void 판매자_상품_재고수정_성공_테스트() throws Exception {
        ProductDetailResponse response = new ProductDetailResponse(
                1L, 1L, 1L, "테스트 상품", BigDecimal.valueOf(10000),
                100, ProductStatus.ON_SALE, "상품 설명"
        );
        when(sellerProductService.stockUpdate(any(Long.class), any(Long.class), any(SellerProductStockUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/seller/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "stock": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.stock").value(100));
    }

    @Test
    void 판매자_상품_재고수정_상품없음_실패_테스트() throws Exception {
        when(sellerProductService.stockUpdate(any(Long.class), any(Long.class), any(SellerProductStockUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        mockMvc.perform(put("/seller/products/999/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "stock": 100
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 판매자_상품_재고수정_권한없음_실패_테스트() throws Exception {
        when(sellerProductService.stockUpdate(any(Long.class), any(Long.class), any(SellerProductStockUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.FORBIDDEN));

        mockMvc.perform(put("/seller/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "stock": 100
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void 판매자_상품_재고수정_유효성검사_음수_실패_테스트() throws Exception {
        mockMvc.perform(put("/seller/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "stock": -1
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser
    void 판매자_상품_재고수정_500에러_실패_테스트() throws Exception {
        when(sellerProductService.stockUpdate(any(Long.class), any(Long.class), any(SellerProductStockUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        mockMvc.perform(put("/seller/products/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "stock": 100
                                }
                                """))
                .andExpect(status().is5xxServerError());
    }
}

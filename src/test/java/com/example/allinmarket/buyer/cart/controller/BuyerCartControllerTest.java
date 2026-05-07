package com.example.allinmarket.buyer.cart.controller;

import com.example.allinmarket.buyer.cart.dto.request.AddProductToCartRequest;
import com.example.allinmarket.buyer.cart.dto.request.UpdateCartItemQuantityRequest;
import com.example.allinmarket.buyer.cart.dto.response.CartDetailResponse;
import com.example.allinmarket.buyer.cart.service.BuyerCartService;
import com.example.allinmarket.buyer.cartitem.dto.CartItemDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerCartController.class)
public class BuyerCartControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerCartService buyerCartService;

    @BeforeEach
    void setAuth() {
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser
    void 장바구니_조회_성공_테스트() throws Exception {
        CartDetailResponse response = new CartDetailResponse(
                1L, 1L,
                new PageResponse<>(
                        List.of(new CartItemDetailResponse(1L, 1L, 1L, "노트북", BigDecimal.valueOf(1200000), 1)),
                        1, 1, 1, 10, true
                )
        );
        given(buyerCartService.getCart(eq(1L), any(Pageable.class))).willReturn(response);

        mockMvc.perform(get("/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.buyerId").value(1))
                .andExpect(jsonPath("$.data.items.content[0].cartId").value(1))
                .andExpect(jsonPath("$.data.items.content[0].productName").value("노트북"))
                .andExpect(jsonPath("$.data.items.content[0].productPrice").value(1200000))
                .andExpect(jsonPath("$.data.items.content[0].quantity").value(1))
                .andDo(document("buyer/cart/get",
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("장바구니 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.items.content[].id").description("장바구니 아이템 ID"),
                                fieldWithPath("data.items.content[].cartId").description("장바구니 ID"),
                                fieldWithPath("data.items.content[].productId").description("상품 ID"),
                                fieldWithPath("data.items.content[].productName").description("상품명"),
                                fieldWithPath("data.items.content[].productPrice").description("상품 가격"),
                                fieldWithPath("data.items.content[].quantity").description("수량"),
                                fieldWithPath("data.items.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("data.items.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.items.totalElements").description("전체 항목 수"),
                                fieldWithPath("data.items.size").description("페이지 크기"),
                                fieldWithPath("data.items.isLast").description("마지막 페이지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 장비구니_조회_실패_테스트() throws Exception {
        given(buyerCartService.getCart(eq(1L), any(Pageable.class)))
                .willThrow(new BaseException(ErrorEnum.UNAUTHORIZED));

        mockMvc.perform(get("/carts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(ErrorEnum.UNAUTHORIZED.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @WithMockUser
    void 장바구니_상품_추가_성공_테스트() throws Exception {
        AddProductToCartRequest request = new AddProductToCartRequest(1L, 2);
        CartDetailResponse response = new CartDetailResponse(
                1L, 1L,
                new PageResponse<>(
                        List.of(new CartItemDetailResponse(1L, 1L, 1L, "노트북", BigDecimal.valueOf(1200000), 1)),
                        1, 1, 1, 10, true
                )
        );
        given(buyerCartService.addProductToCart(eq(1L), eq(request), any(Pageable.class))).willReturn(response);

        mockMvc.perform(post("/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.buyerId").value(1))
                .andExpect(jsonPath("$.data.items.content[0].cartId").value(1))
                .andExpect(jsonPath("$.data.items.content[0].quantity").value(1))
                .andDo(document("buyer/cart/add-item",
                        requestFields(
                                fieldWithPath("productId").description("장바구니에 추가할 상품 ID"),
                                fieldWithPath("quantity").description("추가할 수량")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("장바구니 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.items.content[].id").description("장바구니 아이템 ID"),
                                fieldWithPath("data.items.content[].cartId").description("장바구니 ID"),
                                fieldWithPath("data.items.content[].productId").description("상품 ID"),
                                fieldWithPath("data.items.content[].productName").description("상품명"),
                                fieldWithPath("data.items.content[].productPrice").description("상품 가격"),
                                fieldWithPath("data.items.content[].quantity").description("수량"),
                                fieldWithPath("data.items.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("data.items.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.items.totalElements").description("전체 항목 수"),
                                fieldWithPath("data.items.size").description("페이지 크기"),
                                fieldWithPath("data.items.isLast").description("마지막 페이지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 장바구니_상품_추가_실패_테스트() throws Exception {
        AddProductToCartRequest request = new AddProductToCartRequest(1L, 5);
        given(buyerCartService.addProductToCart(eq(1L), eq(request), any(Pageable.class)))
                .willThrow(new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK));

        mockMvc.perform(post("/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.PRODUCT_OUT_OF_STOCK.getMessage()));
    }

    @Test
    @WithMockUser
    void 장바구니_상품_수량_변경_성공_테스트() throws Exception {
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5);
        CartDetailResponse response = new CartDetailResponse(
                1L, 1L,
                new PageResponse<>(
                        List.of(new CartItemDetailResponse(1L, 1L, 1L, "노트북", BigDecimal.valueOf(1200000), 5)),
                        1, 1, 1, 10, true
                )
        );
        given(buyerCartService.updateCartItemQuantity(eq(1L), eq(1L), eq(request), any(Pageable.class)))
                .willReturn(response);

        mockMvc.perform(put("/carts/items/{productId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.buyerId").value(1))
                .andExpect(jsonPath("$.data.items.content[0].cartId").value(1))
                .andExpect(jsonPath("$.data.items.content[0].quantity").value(5))
                .andDo(document("buyer/cart/update-item",
                        pathParameters(
                                parameterWithName("productId").description("수량을 변경할 상품 ID")
                        ),
                        requestFields(
                                fieldWithPath("quantity").description("변경할 수량")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("장바구니 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.items.content[].id").description("장바구니 아이템 ID"),
                                fieldWithPath("data.items.content[].cartId").description("장바구니 ID"),
                                fieldWithPath("data.items.content[].productId").description("상품 ID"),
                                fieldWithPath("data.items.content[].productName").description("상품명"),
                                fieldWithPath("data.items.content[].productPrice").description("상품 가격"),
                                fieldWithPath("data.items.content[].quantity").description("변경된 수량"),
                                fieldWithPath("data.items.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("data.items.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.items.totalElements").description("전체 항목 수"),
                                fieldWithPath("data.items.size").description("페이지 크기"),
                                fieldWithPath("data.items.isLast").description("마지막 페이지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 장바구니_수량_변경_실패_테스트() throws Exception {
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5);
        given(buyerCartService.updateCartItemQuantity(eq(1L), eq(1L), eq(request), any(Pageable.class)))
                .willThrow(new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK));

        mockMvc.perform(put("/carts/items/{productId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.PRODUCT_OUT_OF_STOCK.getMessage()));
    }

    @Test
    @WithMockUser
    void 장바구니_상품_삭제_성공_테스트() throws Exception {
        CartDetailResponse response = new CartDetailResponse(
                1L, 1L,
                new PageResponse<>(List.of(), 1, 1, 1, 10, true)
        );
        given(buyerCartService.removeCartItem(anyLong(), eq(1L), any(Pageable.class)))
                .willReturn(response);

        mockMvc.perform(delete("/carts/items/{productId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.DELETE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.buyerId").value(1))
                .andExpect(jsonPath("$.data.items.content").isEmpty())
                .andDo(document("buyer/cart/remove-item",
                        pathParameters(
                                parameterWithName("productId").description("삭제할 상품 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("장바구니 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.items.content").description("장바구니 아이템 목록 (삭제 후 빈 배열)"),
                                fieldWithPath("data.items.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("data.items.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.items.totalElements").description("전체 항목 수"),
                                fieldWithPath("data.items.size").description("페이지 크기"),
                                fieldWithPath("data.items.isLast").description("마지막 페이지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 장바구니_상품_삭제_실패_테스트() throws Exception {
        given(buyerCartService.removeCartItem(anyLong(), eq(1L), any(Pageable.class)))
                .willThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        mockMvc.perform(delete("/carts/items/{productId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.PRODUCT_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.PRODUCT_NOT_FOUND.getMessage()));
    }
}

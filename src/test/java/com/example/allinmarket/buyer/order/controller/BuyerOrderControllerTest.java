package com.example.allinmarket.buyer.order.controller;

import com.example.allinmarket.buyer.order.dto.request.OrderCreateRequest;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.buyer.order.dto.response.OrderWithOrderItemDetailResponse;
import com.example.allinmarket.buyer.order.facade.BuyerPaymentOrderFacade;
import com.example.allinmarket.buyer.order.service.BuyerOrderService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.orderitem.dto.OrderItemDetailResponse;
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

@WebMvcTest(BuyerOrderController.class)
class BuyerOrderControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerOrderService buyerOrderService;

    @MockitoBean
    private BuyerPaymentOrderFacade buyerPaymentOrderFacade;

    @BeforeEach
    void setAuth() {
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser
    void 주문_생성_성공_테스트() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(List.of(1L, 2L), 1L);
        OrderDetailResponse response = new OrderDetailResponse(
                1L,
                1L,
                BigDecimal.valueOf(30000),
                OrderStatus.CREATED,
                null,
                "홍길동",
                "서울시 강남구 테헤란로 123"
        );

        given(buyerPaymentOrderFacade.createOrder(eq(1L), eq(request))).willReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.buyerId").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(30000))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.recipient").value("홍길동"))
                .andExpect(jsonPath("$.data.address").value("서울시 강남구 테헤란로 123"))
                .andDo(document("buyer/order/create",
                        requestFields(
                                fieldWithPath("cartItemIds").description("주문할 장바구니 상품 ID 목록"),
                                fieldWithPath("addressId").description("배송지 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.orderId").description("주문 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.totalAmount").description("주문 총 금액"),
                                fieldWithPath("data.status").description("주문 상태"),
                                fieldWithPath("data.trackingNumber").optional().description("운송장 번호"),
                                fieldWithPath("data.recipient").description("수령인"),
                                fieldWithPath("data.address").description("배송지 주소"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 주문_생성_검증_실패_테스트() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(List.of(), null);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    void 주문_생성_실패_테스트() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(List.of(1L), 1L);

        given(buyerPaymentOrderFacade.createOrder(eq(1L), eq(request)))
                .willThrow(new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.PRODUCT_OUT_OF_STOCK.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @WithMockUser
    void 주문_목록_조회_성공_테스트() throws Exception {
        OrderDetailResponse order = new OrderDetailResponse(
                1L,
                1L,
                BigDecimal.valueOf(30000),
                OrderStatus.CREATED,
                null,
                "홍길동",
                "서울시 강남구 테헤란로 123"
        );
        PageResponse<OrderDetailResponse> response = new PageResponse<>(List.of(order), 1, 1, 1, 10, true);

        given(buyerOrderService.findAllOrders(eq(1L), any(Pageable.class), eq(OrderStatus.CREATED)))
                .willReturn(response);

        mockMvc.perform(get("/orders")
                        .param("status", "CREATED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content[0].orderId").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("CREATED"))
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.isLast").value(true))
                .andDo(document("buyer/order/list",
                        queryParameters(
                                parameterWithName("status").optional().description("주문 상태 필터 (CREATED, PAID, SHIPPED, DELIVERED, REFUNDED, FAILED)"),
                                parameterWithName("page").optional().description("페이지 번호 (0부터 시작, 기본값: 0)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본값: 10)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.content[].orderId").description("주문 ID"),
                                fieldWithPath("data.content[].buyerId").description("구매자 ID"),
                                fieldWithPath("data.content[].totalAmount").description("주문 총 금액"),
                                fieldWithPath("data.content[].status").description("주문 상태"),
                                fieldWithPath("data.content[].trackingNumber").optional().description("운송장 번호"),
                                fieldWithPath("data.content[].recipient").description("수령인"),
                                fieldWithPath("data.content[].address").description("배송지 주소"),
                                fieldWithPath("data.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("data.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.totalElements").description("전체 항목 수"),
                                fieldWithPath("data.size").description("페이지 크기"),
                                fieldWithPath("data.isLast").description("마지막 페이지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 주문_단건_조회_성공_테스트() throws Exception {
        OrderItemDetailResponse item = new OrderItemDetailResponse(
                1L,
                1L,
                1L,
                1L,
                "노트북",
                BigDecimal.valueOf(30000),
                1
        );
        OrderWithOrderItemDetailResponse response = new OrderWithOrderItemDetailResponse(
                1L,
                1L,
                BigDecimal.valueOf(30000),
                OrderStatus.CREATED,
                null,
                "홍길동",
                "서울시 강남구 테헤란로 123",
                List.of(item)
        );

        given(buyerOrderService.findOrder(eq(1L), eq(1L))).willReturn(response);

        mockMvc.perform(get("/orders/{orderId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.items[0].productName").value("노트북"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(1))
                .andDo(document("buyer/order/detail",
                        pathParameters(
                                parameterWithName("orderId").description("조회할 주문 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.orderId").description("주문 ID"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.totalAmount").description("주문 총 금액"),
                                fieldWithPath("data.status").description("주문 상태"),
                                fieldWithPath("data.trackingNumber").optional().description("운송장 번호"),
                                fieldWithPath("data.recipient").description("수령인"),
                                fieldWithPath("data.address").description("배송지 주소"),
                                fieldWithPath("data.items[].id").description("주문 상품 ID"),
                                fieldWithPath("data.items[].orderId").description("주문 ID"),
                                fieldWithPath("data.items[].productId").description("상품 ID"),
                                fieldWithPath("data.items[].sellerId").description("판매자 ID"),
                                fieldWithPath("data.items[].productName").description("상품명"),
                                fieldWithPath("data.items[].unitPrice").description("상품 단가"),
                                fieldWithPath("data.items[].quantity").description("주문 수량"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 주문_단건_조회_실패_테스트() throws Exception {
        given(buyerOrderService.findOrder(eq(1L), eq(1L)))
                .willThrow(new BaseException(ErrorEnum.ORDER_NOT_FOUND));

        mockMvc.perform(get("/orders/{orderId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.ORDER_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.ORDER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}

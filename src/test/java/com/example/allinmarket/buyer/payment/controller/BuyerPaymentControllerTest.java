package com.example.allinmarket.buyer.payment.controller;

import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.payment.facade.BuyerPaymentFacade;
import com.example.allinmarket.buyer.payment.service.BuyerPaymentService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerPaymentController.class)
class BuyerPaymentControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerPaymentFacade buyerPaymentFacade;

    @MockitoBean
    private BuyerPaymentService buyerPaymentService;

    @BeforeEach
    void setAuth() {
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser
    void 결제_처리_성공_테스트() throws Exception {
        PaymentCreateRequest request = new PaymentCreateRequest(1L, MethodEnum.MOCK);
        PaymentDetailResponse response = new PaymentDetailResponse(
                1L,
                1L,
                "merchant-uid-001",
                "imp-uid-001",
                BigDecimal.valueOf(30000),
                MethodEnum.MOCK,
                PaymentStatus.SUCCESS,
                LocalDateTime.of(2025, 4, 10, 12, 30)
        );

        given(buyerPaymentFacade.processPayment(eq(1L), eq(request))).willReturn(response);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.paymentId").value(1))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.merchantUid").value("merchant-uid-001"))
                .andExpect(jsonPath("$.data.impUid").value("imp-uid-001"))
                .andExpect(jsonPath("$.data.amount").value(30000))
                .andExpect(jsonPath("$.data.method").value("MOCK"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paidAt").value("2025-04-10T12:30:00"))
                .andDo(document("buyer/payment/create",
                        requestFields(
                                fieldWithPath("orderId").description("결제할 주문 ID"),
                                fieldWithPath("method").description("결제 수단 (MOCK, TOSS, KAKAO)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.paymentId").description("결제 ID"),
                                fieldWithPath("data.orderId").description("주문 ID"),
                                fieldWithPath("data.merchantUid").description("주문/결제 고유 번호"),
                                fieldWithPath("data.impUid").optional().description("PG사 결제 고유 번호"),
                                fieldWithPath("data.amount").description("결제 금액"),
                                fieldWithPath("data.method").description("결제 수단"),
                                fieldWithPath("data.status").description("결제 상태"),
                                fieldWithPath("data.paidAt").optional().description("결제 완료 시각"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 결제_처리_검증_실패_테스트() throws Exception {
        PaymentCreateRequest request = new PaymentCreateRequest(null, null);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser
    void 결제_처리_실패_테스트() throws Exception {
        PaymentCreateRequest request = new PaymentCreateRequest(1L, MethodEnum.MOCK);

        given(buyerPaymentFacade.processPayment(eq(1L), eq(request)))
                .willThrow(new BaseException(ErrorEnum.PAYMENT_AMOUNT_MISMATCH));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.PAYMENT_AMOUNT_MISMATCH.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.PAYMENT_AMOUNT_MISMATCH.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    @WithMockUser
    void 결제_목록_조회_성공_테스트() throws Exception {
        PaymentDetailResponse payment = new PaymentDetailResponse(
                1L,
                1L,
                "merchant-uid-001",
                "imp-uid-001",
                BigDecimal.valueOf(30000),
                MethodEnum.MOCK,
                PaymentStatus.SUCCESS,
                LocalDateTime.of(2025, 4, 10, 12, 30)
        );
        PageResponse<PaymentDetailResponse> response = new PageResponse<>(List.of(payment), 1, 1, 1, 10, true);

        given(buyerPaymentService.getPayments(eq(1L), any(Pageable.class))).willReturn(response);

        mockMvc.perform(get("/payments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content[0].paymentId").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.isLast").value(true))
                .andDo(document("buyer/payment/list",
                        queryParameters(
                                parameterWithName("page").optional().description("페이지 번호 (0부터 시작, 기본값: 0)"),
                                parameterWithName("size").optional().description("페이지 크기 (기본값: 10)")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.content[].paymentId").description("결제 ID"),
                                fieldWithPath("data.content[].orderId").description("주문 ID"),
                                fieldWithPath("data.content[].merchantUid").description("주문/결제 고유 번호"),
                                fieldWithPath("data.content[].impUid").optional().description("PG사 결제 고유 번호"),
                                fieldWithPath("data.content[].amount").description("결제 금액"),
                                fieldWithPath("data.content[].method").description("결제 수단"),
                                fieldWithPath("data.content[].status").description("결제 상태"),
                                fieldWithPath("data.content[].paidAt").optional().description("결제 완료 시각"),
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
    void 결제_단건_조회_성공_테스트() throws Exception {
        PaymentDetailResponse response = new PaymentDetailResponse(
                1L,
                1L,
                "merchant-uid-001",
                "imp-uid-001",
                BigDecimal.valueOf(30000),
                MethodEnum.MOCK,
                PaymentStatus.SUCCESS,
                LocalDateTime.of(2025, 4, 10, 12, 30)
        );

        given(buyerPaymentService.findPayment(eq(1L), eq(1L))).willReturn(response);

        mockMvc.perform(get("/payments/{paymentId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.paymentId").value(1))
                .andExpect(jsonPath("$.data.orderId").value(1))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andDo(document("buyer/payment/detail",
                        pathParameters(
                                parameterWithName("paymentId").description("조회할 결제 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.paymentId").description("결제 ID"),
                                fieldWithPath("data.orderId").description("주문 ID"),
                                fieldWithPath("data.merchantUid").description("주문/결제 고유 번호"),
                                fieldWithPath("data.impUid").optional().description("PG사 결제 고유 번호"),
                                fieldWithPath("data.amount").description("결제 금액"),
                                fieldWithPath("data.method").description("결제 수단"),
                                fieldWithPath("data.status").description("결제 상태"),
                                fieldWithPath("data.paidAt").optional().description("결제 완료 시각"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 결제_단건_조회_실패_테스트() throws Exception {
        given(buyerPaymentService.findPayment(eq(1L), eq(1L)))
                .willThrow(new BaseException(ErrorEnum.PAYMENT_NOT_FOUND));

        mockMvc.perform(get("/payments/{paymentId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.PAYMENT_NOT_FOUND.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.PAYMENT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}

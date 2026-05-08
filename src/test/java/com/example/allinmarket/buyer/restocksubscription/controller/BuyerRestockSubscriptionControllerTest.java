package com.example.allinmarket.buyer.restocksubscription.controller;

import com.example.allinmarket.buyer.restocksubscription.service.BuyerRestockSubscriptionService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionDetailResponse;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionRequest;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerRestockSubscriptionController.class)
class BuyerRestockSubscriptionControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerRestockSubscriptionService buyerRestockSubscriptionService;

    @BeforeEach
    void setAuth() {
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser
    void 재입고_알림_신청_성공_테스트() throws Exception {
        RestockSubscriptionRequest request = new RestockSubscriptionRequest(100L);
        RestockSubscriptionDetailResponse response =
                new RestockSubscriptionDetailResponse(100L, SubscriptionStatusEnum.ACTIVE, "재입고 알림이 신청되었습니다.");

        given(buyerRestockSubscriptionService.subscribe(eq(1L), any()))
                .willReturn(response);

        mockMvc.perform(post("/restock-subscriptions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.CREATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.CREATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.productId").value(100))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.message").value("재입고 알림이 신청되었습니다."))
                .andDo(document("buyer/restock-subscription/subscribe",
                        requestFields(
                                fieldWithPath("productId").description("상품 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.productId").description("상품 ID"),
                                fieldWithPath("data.status").description("구독 상태 (ACTIVE 등)"),
                                fieldWithPath("data.message").description("구독 메시지"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 내_재입고_알림_목록조회_성공_테스트() throws Exception {
        RestockSubscriptionDetailResponse dto =
                new RestockSubscriptionDetailResponse(100L, SubscriptionStatusEnum.ACTIVE, "재입고 알림이 신청되었습니다.");
        PageResponse<RestockSubscriptionDetailResponse> pageResponse =
                new PageResponse<>(List.of(dto), 1, 1, 1, 10, true);

        given(buyerRestockSubscriptionService.getSubscriptions(eq(1L), any()))
                .willReturn(pageResponse);

        mockMvc.perform(get("/restock-subscriptions/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content[0].productId").value(100))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
                .andDo(document("buyer/restock-subscription/list",
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.content[].productId").description("상품 ID"),
                                fieldWithPath("data.content[].status").description("구독 상태"),
                                fieldWithPath("data.content[].message").description("구독 메시지"),
                                fieldWithPath("data.currentPage").description("현재 페이지 번호"),
                                fieldWithPath("data.totalPages").description("전체 페이지 수"),
                                fieldWithPath("data.totalElements").description("전체 알림 수"),
                                fieldWithPath("data.size").description("페이지 크기"),
                                fieldWithPath("data.isLast").description("마지막 페이지 여부"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 내_재입고_알림_단건조회_성공_테스트() throws Exception {
        RestockSubscriptionDetailResponse response =
                new RestockSubscriptionDetailResponse(100L, SubscriptionStatusEnum.ACTIVE, "재입고 알림이 신청되었습니다.");

        given(buyerRestockSubscriptionService.getSingleSubscription(eq(1L), eq(100L)))
                .willReturn(response);

        mockMvc.perform(get("/restock-subscriptions/me/{productId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.productId").value(100))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andDo(document("buyer/restock-subscription/detail",
                        pathParameters(
                                parameterWithName("productId").description("상품 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.productId").description("상품 ID"),
                                fieldWithPath("data.status").description("구독 상태"),
                                fieldWithPath("data.message").description("구독 메시지"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 재입고_알림_구독취소_성공_테스트() throws Exception {
        mockMvc.perform(delete("/restock-subscriptions/{productId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.DELETE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.DELETE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andDo(document("buyer/restock-subscription/unsubscribe",
                        pathParameters(
                                parameterWithName("productId").description("상품 ID")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("응답 데이터 (없음)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }
}

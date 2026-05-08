package com.example.allinmarket.buyer.restocknotification.controller;

import com.example.allinmarket.buyer.restocknotification.service.RestockNotificationService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationDetailResponse;
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
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RestockNotificationController.class)
class RestockNotificationControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private RestockNotificationService restockNotificationService;

    @BeforeEach
    void setAuth() {
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser
    void 안읽은_알림_전체조회_성공_테스트() throws Exception {
        RestockNotificationDetailResponse dto = new RestockNotificationDetailResponse(1L, 100L);
        PageResponse<RestockNotificationDetailResponse> pageResponse =
                new PageResponse<>(List.of(dto), 1, 1, 1, 10, true);

        given(restockNotificationService.getNotifications(eq(1L), any()))
                .willReturn(pageResponse);

        mockMvc.perform(get("/restock-notifications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.READ_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content[0].buyerId").value(1))
                .andExpect(jsonPath("$.data.content[0].productId").value(100))
                .andDo(document("buyer/restock-notification/list",
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.content[].buyerId").description("구매자 ID"),
                                fieldWithPath("data.content[].productId").description("상품 ID"),
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
    void 전체_읽음처리_성공_테스트() throws Exception {
        mockMvc.perform(put("/restock-notifications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andDo(document("buyer/restock-notification/readAll",
                        relaxedResponseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("응답 데이터 (없음)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 개별_읽음처리_성공_테스트() throws Exception {
        mockMvc.perform(put("/restock-notifications/{productId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.UPDATE_SUCCESS.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andDo(document("buyer/restock-notification/readOne",
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
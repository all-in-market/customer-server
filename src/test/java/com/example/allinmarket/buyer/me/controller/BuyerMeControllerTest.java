package com.example.allinmarket.buyer.me.controller;

import com.example.allinmarket.buyer.me.dto.request.BuyerUpdateRequest;
import com.example.allinmarket.buyer.me.dto.response.BuyerDetailResponse;
import com.example.allinmarket.buyer.me.service.BuyerMeService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerMeController.class)
public class BuyerMeControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerMeService buyerMeService;

    // ────────────────────────────────────────────────────
    // SecurityContext 헬퍼
    // ────────────────────────────────────────────────────

    private void setAuthContext(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ────────────────────────────────────────────────────
    // 공통 픽스처
    // ────────────────────────────────────────────────────

    private BuyerDetailResponse sampleResponse() {
        // BuyerDetailResponse(Long buyerId, String email, String name, String phone, UserRole role)
        return new BuyerDetailResponse(
                1L,
                "테스트@테스트.com",
                "테스트유저",
                "010-1234-5678",
                UserRole.BUYER
        );
    }

    // ────────────────────────────────────────────────────
    // 내 정보 조회
    // ────────────────────────────────────────────────────

    @Test
    void 내_정보_조회_성공_테스트() throws Exception {
        setAuthContext(1L);

        given(buyerMeService.getMyProfile(anyLong()))
                .willReturn(sampleResponse());

        mockMvc.perform(get("/buyers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.buyerId").value(1L))
                .andExpect(jsonPath("$.data.email").value("테스트@테스트.com"))
                .andExpect(jsonPath("$.data.name").value("테스트유저"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.role").value("BUYER"))
                .andDo(document("buyer/me/get",
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.email").description("이메일 주소"),
                                fieldWithPath("data.name").description("이름"),
                                fieldWithPath("data.phone").description("전화번호"),
                                fieldWithPath("data.role").description("사용자 권한 (BUYER)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 내_정보_조회_실패_존재하지_않는_구매자_테스트() throws Exception {
        setAuthContext(999L);  // ← 인증은 통과, 서비스에서 404 발생

        given(buyerMeService.getMyProfile(anyLong()))
                .willThrow(new BaseException(ErrorEnum.BUYER_NOT_FOUND));

        mockMvc.perform(get("/buyers/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.BUYER_NOT_FOUND.getStatus()))   // 404
                .andExpect(jsonPath("$.message").value(ErrorEnum.BUYER_NOT_FOUND.getMessage())); // "존재하지 않는 사용자입니다."
    }

    // ────────────────────────────────────────────────────
    // 내 정보 수정
    // ────────────────────────────────────────────────────

    @Test
    void 내_정보_수정_성공_테스트() throws Exception {
        setAuthContext(1L);

        BuyerUpdateRequest request = new BuyerUpdateRequest(
                "new@test.com", "newPassword1!", "새이름", "010-9999-8888"
        );
        BuyerDetailResponse updated = new BuyerDetailResponse(
                1L, "new@test.com", "새이름", "010-9999-8888", UserRole.BUYER
        );

        given(buyerMeService.updateMyProfile(anyLong(), any(BuyerUpdateRequest.class)))
                .willReturn(updated);

        mockMvc.perform(put("/buyers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.buyerId").value(1L))
                .andExpect(jsonPath("$.data.email").value("new@test.com"))
                .andExpect(jsonPath("$.data.name").value("새이름"))
                .andExpect(jsonPath("$.data.phone").value("010-9999-8888"))
                .andExpect(jsonPath("$.data.role").value("BUYER"))
                .andDo(document("buyer/me/update",
                        requestFields(
                                fieldWithPath("email").description("변경할 이메일 주소 (변경 시에만 입력)").optional(),
                                fieldWithPath("password").description("변경할 비밀번호 (8자 이상, 변경 시에만 입력)").optional(),
                                fieldWithPath("name").description("변경할 이름 (변경 시에만 입력)").optional(),
                                fieldWithPath("phone").description("변경할 전화번호 (변경 시에만 입력)").optional()
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.buyerId").description("구매자 ID"),
                                fieldWithPath("data.email").description("이메일 주소"),
                                fieldWithPath("data.name").description("이름"),
                                fieldWithPath("data.phone").description("전화번호"),
                                fieldWithPath("data.role").description("사용자 권한 (BUYER)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 내_정보_수정_실패_이미_사용중인_이메일_테스트() throws Exception {
        setAuthContext(1L);  // ← 인증은 통과, 서비스에서 400 발생

        BuyerUpdateRequest request = new BuyerUpdateRequest(
                "duplicate@test.com", null, null, null
        );

        given(buyerMeService.updateMyProfile(anyLong(), any(BuyerUpdateRequest.class)))
                .willThrow(new BaseException(ErrorEnum.EMAIL_ALREADY_EXISTS));

        mockMvc.perform(put("/buyers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.EMAIL_ALREADY_EXISTS.getStatus()))    // 400
                .andExpect(jsonPath("$.message").value(ErrorEnum.EMAIL_ALREADY_EXISTS.getMessage())); // "이미 사용 중인 이메일입니다."
    }

    @Test
    void 내_정보_수정_실패_유효성_오류_테스트() throws Exception {
        setAuthContext(1L);

        // @Valid 위반: 이메일 형식 오류
        BuyerUpdateRequest request = new BuyerUpdateRequest(
                "이메일형식오류", null, null, null
        );

        mockMvc.perform(put("/buyers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }
}
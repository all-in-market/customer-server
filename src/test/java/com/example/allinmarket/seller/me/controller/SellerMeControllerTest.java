package com.example.allinmarket.seller.me.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.me.dto.request.SellerUpdateRequest;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.me.service.SellerMeService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerMeController.class)
public class SellerMeControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private SellerMeService sellerMeService;

    private void setAuthContext(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 판매자_내_정보_조회_성공_테스트() throws Exception {
        setAuthContext(1L);

        SellerDetailResponse response = new SellerDetailResponse(
                1L, "seller@test.com", "홍길동", "010-1234-5678",
                "홍길동상점", "123-45-67890", SellerStatus.PENDING, UserRole.SELLER
        );
        when(sellerMeService.getMyProfile(1L)).thenReturn(response);

        mockMvc.perform(get("/sellers/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("데이터 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("seller@test.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.storeName").value("홍길동상점"))
                .andExpect(jsonPath("$.data.bizNumber").value("123-45-67890"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.role").value("SELLER"))
                .andDo(document("seller/me/get",
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("판매자 ID"),
                                fieldWithPath("data.email").description("이메일 주소"),
                                fieldWithPath("data.name").description("이름"),
                                fieldWithPath("data.phone").description("전화번호"),
                                fieldWithPath("data.storeName").description("상점명"),
                                fieldWithPath("data.bizNumber").description("사업자등록번호"),
                                fieldWithPath("data.status").description("판매자 상태 (PENDING: 승인 대기, APPROVED: 승인)"),
                                fieldWithPath("data.role").description("사용자 권한 (SELLER)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 판매자_내_정보_조회_미인증_예외_테스트() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/sellers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 판매자_내_정보_조회_존재하지_않는_판매자_예외_테스트() throws Exception {
        setAuthContext(999L);
        when(sellerMeService.getMyProfile(999L))
                .thenThrow(new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        mockMvc.perform(get("/sellers/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorEnum.SELLER_NOT_FOUND.getMessage()));
    }

    @Test
    void 판매자_내_정보_수정_성공_테스트() throws Exception {
        setAuthContext(1L);

        SellerDetailResponse response = new SellerDetailResponse(
                1L, "updated@test.com", "김철수", "010-9999-8888",
                "김철수상점", "987-65-43210", SellerStatus.PENDING, UserRole.SELLER
        );
        when(sellerMeService.updateMyProfile(eq(1L), any(SellerUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/sellers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "updated@test.com",
                                    "password": "newpassword1",
                                    "name": "김철수",
                                    "phone": "010-9999-8888",
                                    "storeName": "김철수상점",
                                    "bizNumber": "987-65-43210",
                                    "bankAccount": "220-999-123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("데이터 수정에 성공하였습니다."))
                .andExpect(jsonPath("$.data.email").value("updated@test.com"))
                .andExpect(jsonPath("$.data.name").value("김철수"))
                .andExpect(jsonPath("$.data.storeName").value("김철수상점"))
                .andDo(document("seller/me/update",
                        requestFields(
                                fieldWithPath("email").optional().description("변경할 이메일 주소"),
                                fieldWithPath("password").optional().description("변경할 비밀번호 (8~20자)"),
                                fieldWithPath("name").optional().description("변경할 이름"),
                                fieldWithPath("phone").optional().description("변경할 전화번호"),
                                fieldWithPath("storeName").optional().description("변경할 상점명"),
                                fieldWithPath("bizNumber").optional().description("변경할 사업자등록번호"),
                                fieldWithPath("bankAccount").optional().description("변경할 계좌번호")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("판매자 ID"),
                                fieldWithPath("data.email").description("이메일 주소"),
                                fieldWithPath("data.name").description("이름"),
                                fieldWithPath("data.phone").description("전화번호"),
                                fieldWithPath("data.storeName").description("상점명"),
                                fieldWithPath("data.bizNumber").description("사업자등록번호"),
                                fieldWithPath("data.status").description("판매자 상태"),
                                fieldWithPath("data.role").description("사용자 권한"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 판매자_내_정보_수정_미인증_예외_테스트() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(put("/sellers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "updated@test.com",
                                    "password": "newpassword1",
                                    "name": "김철수",
                                    "phone": "010-9999-8888",
                                    "storeName": "김철수상점",
                                    "bizNumber": "987-65-43210",
                                    "bankAccount": "220-999-123456"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 판매자_내_정보_수정_존재하지_않는_판매자_예외_테스트() throws Exception {
        setAuthContext(999L);
        when(sellerMeService.updateMyProfile(eq(999L), any(SellerUpdateRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        mockMvc.perform(put("/sellers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "updated@test.com",
                                    "password": "newpassword1",
                                    "name": "김철수",
                                    "phone": "010-9999-8888",
                                    "storeName": "김철수상점",
                                    "bizNumber": "987-65-43210",
                                    "bankAccount": "220-999-123456"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorEnum.SELLER_NOT_FOUND.getMessage()));
    }

    @Test
    void 판매자_내_정보_수정_비밀번호_길이_미달_예외_테스트() throws Exception {
        setAuthContext(1L);

        mockMvc.perform(put("/sellers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "updated@test.com",
                                    "password": "pass123",
                                    "name": "김철수",
                                    "phone": "010-9999-8888",
                                    "storeName": "김철수상점",
                                    "bizNumber": "987-65-43210",
                                    "bankAccount": "220-999-123456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상 20자 이하여야 합니다."));

        verifyNoInteractions(sellerMeService);
    }

    @Test
    void 판매자_내_정보_수정_이메일_형식_예외_테스트() throws Exception {
        setAuthContext(1L);

        mockMvc.perform(put("/sellers/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "invalid-email",
                                    "password": "newpassword1",
                                    "name": "김철수",
                                    "phone": "010-9999-8888",
                                    "storeName": "김철수상점",
                                    "bizNumber": "987-65-43210",
                                    "bankAccount": "220-999-123456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(sellerMeService);
    }
}

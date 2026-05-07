package com.example.allinmarket.seller.auth.controller;

import com.example.allinmarket.common.auth.dto.LoginResponse;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.auth.dto.request.SellerCreateRequest;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.auth.dto.response.SellerCreateResponse;
import com.example.allinmarket.seller.auth.service.SellerAuthService;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.support.RestDocsControllerTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.cookies.CookieDocumentation.cookieWithName;
import static org.springframework.restdocs.cookies.CookieDocumentation.requestCookies;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerAuthController.class)
public class SellerAuthControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private SellerAuthService sellerAuthService;

    @Test
    void 판매자_회원가입_성공_테스트() throws Exception {
        SellerCreateResponse response = new SellerCreateResponse(
                1L, "seller@test.com", "홍길동", "010-1234-5678",
                "홍길동상점", "123-45-67890", SellerStatus.PENDING, UserRole.SELLER
        );
        when(sellerAuthService.signup(any(SellerCreateRequest.class))).thenReturn(response);

        String requestBody = """
                {
                    "email": "seller@test.com",
                    "password": "password123",
                    "name": "홍길동",
                    "phone": "010-1234-5678",
                    "storeName": "홍길동상점",
                    "bizNumber": "123-45-67890",
                    "bankCode": "KOOKMIN",
                    "bankAccount": "123-456-7890"
                }
                """;

        mockMvc.perform(post("/seller/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.email").value("seller@test.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.role").value("SELLER"))
                .andDo(document("seller/auth/signup",
                        requestFields(
                                fieldWithPath("email").description("이메일 주소 (최대 100자)"),
                                fieldWithPath("password").description("비밀번호 (8~20자)"),
                                fieldWithPath("name").description("이름 (최대 50자)"),
                                fieldWithPath("phone").description("전화번호 (최대 20자)"),
                                fieldWithPath("storeName").description("상점명 (최대 100자)"),
                                fieldWithPath("bizNumber").description("사업자등록번호 (최대 20자)"),
                                fieldWithPath("bankCode").description("은행 코드 (최대 30자)"),
                                fieldWithPath("bankAccount").description("계좌번호 (최대 50자)")
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
                                fieldWithPath("data.status").description("판매자 상태 (PENDING: 승인 대기)"),
                                fieldWithPath("data.role").description("사용자 권한 (SELLER)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 판매자_회원가입_이메일_형식_오류_테스트() throws Exception {
        mockMvc.perform(post("/seller/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": "password123",
                                    "name": "홍길동",
                                    "phone": "010-1234-5678",
                                    "storeName": "홍길동상점",
                                    "bizNumber": "123-45-67890",
                                    "bankCode": "KOOKMIN",
                                    "bankAccount": "123-456-7890"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_회원가입_비밀번호_길이_오류_테스트() throws Exception {
        mockMvc.perform(post("/seller/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "seller@test.com",
                                    "password": "short",
                                    "name": "홍길동",
                                    "phone": "010-1234-5678",
                                    "storeName": "홍길동상점",
                                    "bizNumber": "123-45-67890",
                                    "bankCode": "KOOKMIN",
                                    "bankAccount": "123-456-7890"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상 20자 이하여야 합니다."));

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_회원가입_이름_공백_오류_테스트() throws Exception {
        mockMvc.perform(post("/seller/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "seller@test.com",
                                    "password": "password123",
                                    "name": "   ",
                                    "phone": "010-1234-5678",
                                    "storeName": "홍길동상점",
                                    "bizNumber": "123-45-67890",
                                    "bankCode": "KOOKMIN",
                                    "bankAccount": "123-456-7890"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_로그인_성공_테스트() throws Exception {
        LoginResult loginResult = new LoginResult(
                new LoginResponse("jwt.token.here"), "test-refresh-token"
        );
        when(sellerAuthService.login(any(SellerLoginRequest.class))).thenReturn(loginResult);

        String requestBody = """
                {
                    "email": "seller@test.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/seller/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=test-refresh-token")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(SuccessEnum.LOGIN_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.accessToken").value("jwt.token.here"))
                .andDo(document("seller/auth/login",
                        requestFields(
                                fieldWithPath("email").description("이메일 주소"),
                                fieldWithPath("password").description("비밀번호")
                        ),
                        responseHeaders(
                                headerWithName("Set-Cookie").description("리프레시 토큰 쿠키 (HttpOnly, Secure)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.accessToken").description("액세스 토큰 (Bearer)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 판매자_로그인_이메일_형식_오류_테스트() throws Exception {
        mockMvc.perform(post("/seller/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_로그인_비밀번호_길이_오류_테스트() throws Exception {
        mockMvc.perform(post("/seller/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "seller@test.com",
                                    "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상 20자 이하여야 합니다."));

        verifyNoInteractions(sellerAuthService);
    }

    @Test
    void 판매자_로그인_판매자_없음_예외_테스트() throws Exception {
        when(sellerAuthService.login(any(SellerLoginRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.SELLER_NOT_FOUND));

        mockMvc.perform(post("/seller/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "seller@test.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorEnum.SELLER_NOT_FOUND.getMessage()));
    }

    @Test
    void 판매자_로그인_비밀번호_불일치_예외_테스트() throws Exception {
        when(sellerAuthService.login(any(SellerLoginRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.PASSWORD_MISMATCH));

        mockMvc.perform(post("/seller/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "seller@test.com",
                                    "password": "wrongPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(ErrorEnum.PASSWORD_MISMATCH.getMessage()));
    }

    @Test
    void 판매자_토큰_재발급_성공_테스트() throws Exception {
        LoginResult loginResult = new LoginResult(
                new LoginResponse("new-accessToken"), "new-refresh-token"
        );
        given(sellerAuthService.refresh("valid-refresh-token")).willReturn(loginResult);

        mockMvc.perform(post("/seller/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=new-refresh-token")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(SuccessEnum.TOKEN_REFRESHED.getMessage()))
                .andExpect(jsonPath("$.data.accessToken").value("new-accessToken"))
                .andDo(document("seller/auth/refresh",
                        requestCookies(
                                cookieWithName("refreshToken").description("리프레시 토큰")
                        ),
                        responseHeaders(
                                headerWithName("Set-Cookie").description("갱신된 리프레시 토큰 쿠키 (HttpOnly, Secure)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.accessToken").description("새로 발급된 액세스 토큰 (Bearer)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 판매자_토큰_재발급_실패_만료된_토큰_테스트() throws Exception {
        given(sellerAuthService.refresh(anyString()))
                .willThrow(new BaseException(ErrorEnum.TOKEN_EXPIRED));

        mockMvc.perform(post("/seller/auth/refresh")
                        .cookie(new Cookie("refreshToken", "expired-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.TOKEN_EXPIRED.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.TOKEN_EXPIRED.getMessage()));
    }
}

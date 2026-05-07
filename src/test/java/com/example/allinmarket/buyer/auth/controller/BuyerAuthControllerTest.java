package com.example.allinmarket.buyer.auth.controller;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.auth.service.BuyerAuthService;
import com.example.allinmarket.common.auth.dto.LoginResponse;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BuyerAuthController.class)
public class BuyerAuthControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private BuyerAuthService buyerAuthService;

    @Test
    void 회원_가입_성공_테스트() throws Exception {
        BuyerSignupRequest request = new BuyerSignupRequest(
                "테스트@테스트.com", "12345678", "테스트", "010-1234-1234"
        );
        BuyerAuthResponse response = new BuyerAuthResponse(
                "테스트@테스트.com", "테스트", "010-1234-1234"
        );
        when(buyerAuthService.signup(request)).thenReturn(response);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value(SuccessEnum.REGISTER_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.email").value("테스트@테스트.com"))
                .andExpect(jsonPath("$.data.name").value("테스트"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-1234"));
    }

    @Test
    void 회원_가입_실패_테스트() throws Exception {
        BuyerSignupRequest request = new BuyerSignupRequest(
                "이메일형식오류", "12345678", "테스트", "전화번호형식오류"
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void 로그인_성공_테스트() throws Exception {
        BuyerLoginRequest request = new BuyerLoginRequest(
                "테스트@테스트.com", "12345678"
        );
        LoginResult loginResult = new LoginResult(
                new LoginResponse("test-accessToken"), "test-refreshToken"
        );
        given(buyerAuthService.login(any(BuyerLoginRequest.class))).willReturn(loginResult);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=test-refreshToken")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessEnum.LOGIN_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.accessToken").value("test-accessToken"));
    }

    @Test
    void 로그인_실패_테스트() throws Exception {
        BuyerLoginRequest request = new BuyerLoginRequest("테스트@테스트.com", "");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void 토큰_재발급_성공_테스트() throws Exception {
        LoginResult loginResult = new LoginResult(
                new LoginResponse("new-accessToken"), "new-refresh-token"
        );
        given(buyerAuthService.refresh("valid-refresh-token")).willReturn(loginResult);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=new-refresh-token")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(SuccessEnum.TOKEN_REFRESHED.getStatus()))
                .andExpect(jsonPath("$.message").value(SuccessEnum.TOKEN_REFRESHED.getMessage()))
                .andExpect(jsonPath("$.data.accessToken").value("new-accessToken"));
    }

    @Test
    void 토큰_재발급_실패_만료된_토큰_테스트() throws Exception {
        given(buyerAuthService.refresh(anyString()))
                .willThrow(new BaseException(ErrorEnum.TOKEN_EXPIRED));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "expired-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(ErrorEnum.TOKEN_EXPIRED.getStatus()))
                .andExpect(jsonPath("$.message").value(ErrorEnum.TOKEN_EXPIRED.getMessage()));
    }
}

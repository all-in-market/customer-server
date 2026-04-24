package com.example.allinmarket.buyer.auth.controller;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.auth.dto.response.BuyerLoginResponse;
import com.example.allinmarket.buyer.auth.dto.response.LoginResult;
import com.example.allinmarket.buyer.auth.service.BuyerAuthService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class BuyerAuthController {
    private final BuyerAuthService buyerAuthService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<BuyerAuthResponse>> signup(@Valid @RequestBody BuyerSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(SuccessEnum.REGISTER_SUCCESS, buyerAuthService.signup(request))
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<BuyerLoginResponse>> login(@Valid @RequestBody BuyerLoginRequest request) {
        LoginResult result = buyerAuthService.login(request);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(14))
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(SuccessEnum.LOGIN_SUCCESS, result.response()));
    }

//    @PostMapping("/logout")
//    public ResponseEntity<ApiResponse<Void>> logout() {
//        buyerAuthService.logout(SecurityUtils.getCurrentUserId());
//        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.LOGOUT_SUCCESS, null));
//    }
//
//    // TODO: Access Token 재발급
//    @PostMapping("/refresh")

}

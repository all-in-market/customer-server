package com.example.allinmarket.buyer.auth.controller;

import com.example.allinmarket.buyer.auth.dto.request.BuyerLoginRequest;
import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.auth.service.BuyerAuthService;
import com.example.allinmarket.common.auth.dto.LoginResponse;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody BuyerLoginRequest request) {
        LoginResult result = buyerAuthService.login(request);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true) // JS에서 document.cookie로 접근 불가 -> XSS 공격으로 토큰 탈취 방지
                .secure(true) // HTTPS 연결에서만 쿠키를 전송. 배포 환경에서는 true로 설정 필요
                .path("/auth") // 이 경로로 요청할 때만 쿠키가 자동 포함
                .maxAge(Duration.ofDays(7)) // 브라우저가 쿠키를 보관하는 기간. Redis TTL과 맞춰두는 것이 일반적
                .sameSite("Strict") // 다른 도메인에서 온 요청에는 쿠키를 포함하지 않음. CSRF 공격 방어.
                .build(); // 위 설정을 조합해 ResponseCookie 객체를 생성
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(SuccessEnum.LOGIN_SUCCESS, result.response()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue("refreshToken") String refreshToken) {
        LoginResult result = buyerAuthService.refresh(refreshToken);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/auth")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(SuccessEnum.TOKEN_REFRESHED, result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @CookieValue(value = "refreshToken") String refreshToken
    ) {
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail(ErrorEnum.UNAUTHORIZED));
        }
        String accessToken = authHeader.substring(7);
        buyerAuthService.logout(accessToken, refreshToken);

        ResponseCookie expired = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .maxAge(0)
                .path("/auth")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .body(ApiResponse.success(SuccessEnum.LOGOUT_SUCCESS, null));
    }
}

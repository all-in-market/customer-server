package com.example.allinmarket.seller.auth.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.seller.auth.dto.request.SellerCreateRequest;
import com.example.allinmarket.seller.auth.dto.request.SellerLoginRequest;
import com.example.allinmarket.seller.auth.dto.response.SellerCreateResponse;
import com.example.allinmarket.seller.auth.dto.response.SellerLoginResponse;
import com.example.allinmarket.seller.auth.service.SellerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seller/auth")
@RequiredArgsConstructor
public class SellerAuthController {

    private final SellerAuthService sellerAuthService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SellerCreateResponse>> signup(
            @Valid @RequestBody SellerCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(SuccessEnum.CREATE_SUCCESS, sellerAuthService.signup(request))
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<SellerLoginResponse>> login(
            @Valid @RequestBody SellerLoginRequest request
    ) {
        SellerLoginResponse response = sellerAuthService.login(request);
        String token = response.accessToken();
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + token)
                .body(ApiResponse.success(SuccessEnum.LOGIN_SUCCESS, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.LOGOUT_SUCCESS, null));
    }

}

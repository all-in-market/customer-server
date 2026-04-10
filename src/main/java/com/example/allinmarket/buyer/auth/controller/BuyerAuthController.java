package com.example.allinmarket.buyer.auth.controller;

import com.example.allinmarket.buyer.auth.dto.request.BuyerSignupRequest;
import com.example.allinmarket.buyer.auth.dto.response.BuyerAuthResponse;
import com.example.allinmarket.buyer.auth.service.BuyerAuthService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}

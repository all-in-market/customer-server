package com.example.allinmarket.seller.me.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.seller.me.dto.request.SellerUpdateRequest;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.me.service.SellerMeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sellers/me")
public class SellerMeController {

    private final SellerMeService sellerMeService;

    @GetMapping
    public ResponseEntity<ApiResponse<SellerDetailResponse>> getMyProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        SellerDetailResponse response = sellerMeService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.READ_SUCCESS,
                response
        ));
    }


    @PutMapping
    public ResponseEntity<ApiResponse<SellerDetailResponse>> updateMyProfile(
            @Valid @RequestBody SellerUpdateRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        SellerDetailResponse response = sellerMeService.updateMyProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.UPDATE_SUCCESS,
                response
        ));
    }
}

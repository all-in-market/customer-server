package com.example.allinmarket.seller.me.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.seller.me.dto.request.SellerUpdateRequest;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.me.dto.response.SellerUpdateResponse;
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
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.READ_SUCCESS,
                sellerMeService.getMyProfile(userId)
        ));
    }


    @PutMapping
    public ResponseEntity<ApiResponse<SellerUpdateResponse>> updateMyProfile(
            @Valid @RequestBody SellerUpdateRequest request
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.UPDATE_SUCCESS,
                sellerMeService.updateMyProfile(userId, request)
        ));
    }
}

package com.example.allinmarket.buyer.me.controller;

import com.example.allinmarket.buyer.me.dto.request.BuyerUpdateRequest;
import com.example.allinmarket.buyer.me.dto.response.BuyerDetailResponse;
import com.example.allinmarket.buyer.me.service.BuyerMeService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/buyers/me")
public class BuyerMeController {

    private final BuyerMeService buyerMeService;

    /**
     * 구매자 내 정보 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<BuyerDetailResponse>> getMyProfile() {
        BuyerDetailResponse result = buyerMeService.getMyProfile(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        result
                )
        );
    }

    /**
     * 구매자 내 정보 수정
     */
    @PutMapping
    public ResponseEntity<ApiResponse<BuyerDetailResponse>> updateMyProfile(
            @RequestBody @Valid BuyerUpdateRequest request
    ) {
        BuyerDetailResponse result = buyerMeService.updateMyProfile(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        result
                )
        );
    }
}

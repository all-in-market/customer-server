package com.example.allinmarket.buyer.me.controller;

import com.example.allinmarket.buyer.me.dto.response.BuyerDetailResponse;
import com.example.allinmarket.buyer.me.service.BuyerMeService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BuyerMeController {

    private final BuyerMeService buyerMeService;

    /**
     * 구매자 내 정보 조회
     */
    @GetMapping("/buyer/me")
    public ResponseEntity<ApiResponse<BuyerDetailResponse>> getMe() {
        BuyerDetailResponse result = buyerMeService.getMe(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        result
                )
        );
    }
}

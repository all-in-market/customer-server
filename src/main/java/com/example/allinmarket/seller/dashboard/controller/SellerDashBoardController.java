package com.example.allinmarket.seller.dashboard.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.seller.dashboard.dto.response.SellerDashboardResponse;
import com.example.allinmarket.seller.dashboard.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/dashboard")
public class SellerDashBoardController {

    private final SellerDashboardService sellerDashBoardService;

    @GetMapping
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getSellerDashboard() {
        Long sellerId = SecurityUtils.getCurrentUserId();
        SellerDashboardResponse response = sellerDashBoardService.getSellerDashboard(sellerId);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.READ_SUCCESS,
                response
        ));
    }
}

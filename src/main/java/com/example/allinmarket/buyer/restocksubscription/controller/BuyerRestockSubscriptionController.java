package com.example.allinmarket.buyer.restocksubscription.controller;

import com.example.allinmarket.buyer.restocksubscription.service.BuyerRestockSubscriptionService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionDetailResponse;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restock-subscriptions")
@RequiredArgsConstructor
public class BuyerRestockSubscriptionController {

    private final BuyerRestockSubscriptionService buyerRestockSubscriptionService;

    // 재입고 알림 신청 (생성)
    @PostMapping
    public ResponseEntity<ApiResponse<RestockSubscriptionDetailResponse>> subscribe(
            @Valid @RequestBody RestockSubscriptionRequest request) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        RestockSubscriptionDetailResponse result = buyerRestockSubscriptionService.subscribe(buyerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(SuccessEnum.CREATE_SUCCESS, result));
    }

    // 내 재입고 알림 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RestockSubscriptionDetailResponse>>> getSubscriptions(Pageable pageable) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        PageResponse<RestockSubscriptionDetailResponse> result = buyerRestockSubscriptionService.getSubscriptions(buyerId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.READ_SUCCESS, result)
        );
    }
    // 재입고 알림 구독 취소
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(@PathVariable Long productId) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        buyerRestockSubscriptionService.unsubscribe(buyerId, productId);
        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.DELETE_SUCCESS, null)
        );
    }
}

package com.example.allinmarket.buyer.restocksubscription.controller;

import com.example.allinmarket.buyer.restocksubscription.service.BuyerRestockSubscriptionService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionDetailResponse;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // TODO: 내 재입고 알림 목록 조회
    // TODO: 내 재입고 알림 단건 조회

    // TODO: 재입고 알림 취소
}

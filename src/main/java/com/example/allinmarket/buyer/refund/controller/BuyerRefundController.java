package com.example.allinmarket.buyer.refund.controller;

import com.example.allinmarket.buyer.refund.dto.request.RefundCreateRequest;
import com.example.allinmarket.buyer.refund.dto.response.RefundDetailResponse;
import com.example.allinmarket.buyer.refund.service.BuyerRefundService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BuyerRefundController {

    private final BuyerRefundService buyerRefundService;

    @PostMapping("/orders/{orderId}/refunds")
    public ResponseEntity<ApiResponse<RefundDetailResponse>> createRefund(
            @PathVariable Long orderId,
            @RequestBody @Valid RefundCreateRequest request
    ) {
        RefundDetailResponse result = buyerRefundService.createRefundByOrder(SecurityUtils.getCurrentUserId(), orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        result
                )
        );
    }

    @GetMapping("/refunds/{refundId}")
    public ResponseEntity<ApiResponse<RefundDetailResponse>> getRefund(
            @PathVariable Long refundId
    ){
        RefundDetailResponse result = buyerRefundService.getRefund(SecurityUtils.getCurrentUserId(), refundId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        result
                ));
    }
}

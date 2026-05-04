package com.example.allinmarket.buyer.restocknotification.controller;

import com.example.allinmarket.buyer.restocknotification.service.RestockNotificationService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restock-notifications")
public class RestockNotificationController {

    private final RestockNotificationService restockNotificationService;

    // 안읽은 알림 전체 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<RestockNotificationDetailResponse>>> getNotifications(Pageable pageable) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        PageResponse<RestockNotificationDetailResponse> result = restockNotificationService.getNotifications(buyerId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.READ_SUCCESS, result)
        );
    }

    // 전체 읽음 처리
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> readAllNotifications() {
        Long buyerId = SecurityUtils.getCurrentUserId();
        restockNotificationService.readAllNotifications(buyerId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, null)
        );
    }

    // 개별 읽음 처리
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> readNotification(@PathVariable Long productId) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        restockNotificationService.readNotification(buyerId, productId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, null)
        );
    }
}

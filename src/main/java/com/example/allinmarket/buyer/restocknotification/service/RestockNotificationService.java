package com.example.allinmarket.buyer.restocknotification.service;

import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationDetailResponse;
import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import com.example.allinmarket.domain.restocknotification.repository.RestockNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestockNotificationService {

    private final BuyerRepository buyerRepository;
    private final RestockNotificationRepository restockNotificationRepository;

    public PageResponse<RestockNotificationDetailResponse> getNotifications(Long buyerId, Pageable pageable) {
        buyerRepository.findByIdAndDeletedAtIsNull(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        return PageResponse.register(
                restockNotificationRepository.findByUserId(buyerId, pageable)
                        .map(RestockNotificationDetailResponse::of)
        );
    }

    @Transactional
    public void readNotification(Long buyerId, Long productId) {
        buyerRepository.findByIdAndDeletedAtIsNull(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        RestockNotification notification = restockNotificationRepository.findByUserIdAndProductId(buyerId, productId).orElseThrow(
                () -> new BaseException(ErrorEnum.NOTIFICATION_NOT_FOUND)
        );

        notification.read();
    }
}

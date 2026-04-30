package com.example.allinmarket.buyer.restocksubscription.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionDetailResponse;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionRequest;
import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.repository.RestockSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerRestockSubscriptionService {

    private final RestockSubscriptionRepository restockSubscriptionRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;

    // 재입고 알림 추가
    @Transactional
    public RestockSubscriptionDetailResponse subscribe(Long buyerId, RestockSubscriptionRequest request) {
        // 구매자 검증
        buyerRepository.findByIdAndDeletedAtIsNull(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        // 상품 검증
        productRepository.findByIdAndDeletedAtIsNull(request.productId()).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        // 재입고 알림 신청 내역 추가
        RestockSubscription restockSubscription = RestockSubscription.of(buyerId, request.productId());
        restockSubscriptionRepository.save(restockSubscription);

        return new RestockSubscriptionDetailResponse(
                restockSubscription.getProductId(),
                restockSubscription.getStatus(),
                "재입고 알림이 신청되었습니다.");
    }

}

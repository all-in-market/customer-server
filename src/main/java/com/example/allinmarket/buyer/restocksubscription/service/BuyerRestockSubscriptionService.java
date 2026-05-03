package com.example.allinmarket.buyer.restocksubscription.service;

import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionDetailResponse;
import com.example.allinmarket.domain.restocksubscription.dto.RestockSubscriptionRequest;
import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import com.example.allinmarket.domain.restocksubscription.repository.RestockSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerRestockSubscriptionService {

    private final RestockSubscriptionRepository restockSubscriptionRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;

    @Transactional
    public RestockSubscriptionDetailResponse subscribe(Long buyerId, RestockSubscriptionRequest request) {
        buyerRepository.findByIdAndDeletedAtIsNull(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        productRepository.findByIdAndDeletedAtIsNull(request.productId()).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );

        // SENT/EXPIRED 상태인 기존 구독이 있으면 재활성화, 없으면 신규 생성
        RestockSubscription subscription = restockSubscriptionRepository
                .findByUserIdAndProductId(buyerId, request.productId())
                .map(existing -> { existing.activate(); return existing; })
                .orElseGet(() -> restockSubscriptionRepository.save(RestockSubscription.of(buyerId, request.productId())));

        return new RestockSubscriptionDetailResponse(
                subscription.getProductId(),
                subscription.getStatus(),
                "재입고 알림이 신청되었습니다.");
    }

    @Transactional
    public void unsubscribe(Long buyerId, Long productId) {
        buyerRepository.findByIdAndDeletedAtIsNull(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        RestockSubscription restock = restockSubscriptionRepository
                .findByUserIdAndProductId(buyerId, productId)
                .orElseThrow(() -> new BaseException(ErrorEnum.RESTOCK_SUBSCRIPTION_NOT_FOUND));

        restockSubscriptionRepository.delete(restock);
    }

    public PageResponse<RestockSubscriptionDetailResponse> getSubscriptions(Long buyerId, @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        buyerRepository.findByIdAndDeletedAtIsNull(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        return PageResponse.register(
                restockSubscriptionRepository.findByUserIdAndStatus(buyerId, SubscriptionStatusEnum.ACTIVE, pageable)
                        .map(RestockSubscriptionDetailResponse::from)
        );
    }

}

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerRestockSubscriptionServiceTest {

    @Mock
    private RestockSubscriptionRepository restockSubscriptionRepository;
    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BuyerRestockSubscriptionService buyerRestockSubscriptionService;

    private static final Long BUYER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    private RestockSubscription makeSubscription(SubscriptionStatusEnum status) {
        RestockSubscription sub = RestockSubscription.of(BUYER_ID, PRODUCT_ID);
        ReflectionTestUtils.setField(sub, "id", 1L);
        ReflectionTestUtils.setField(sub, "status", status);
        return sub;
    }

    @Nested
    @DisplayName("재입고 알림 신청")
    class SubscribeTest {

        @Test
        @DisplayName("기존 구독이 없으면 신규 생성한다")
        void subscribe_new() {
            // given
            RestockSubscriptionRequest request = new RestockSubscriptionRequest(PRODUCT_ID);
            RestockSubscription saved = makeSubscription(SubscriptionStatusEnum.ACTIVE);

            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(com.example.allinmarket.buyer.entity.Buyer.class)));
            given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID)).willReturn(Optional.of(mock(com.example.allinmarket.domain.product.entity.Product.class)));
            given(restockSubscriptionRepository.findByUserIdAndProductId(BUYER_ID, PRODUCT_ID)).willReturn(Optional.empty());
            given(restockSubscriptionRepository.save(any(RestockSubscription.class))).willReturn(saved);

            // when
            RestockSubscriptionDetailResponse result = buyerRestockSubscriptionService.subscribe(BUYER_ID, request);

            // then
            assertThat(result.productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.status()).isEqualTo(SubscriptionStatusEnum.ACTIVE);
            verify(restockSubscriptionRepository).save(any(RestockSubscription.class));
        }

        @Test
        @DisplayName("SENT 상태 구독이 있으면 재활성화한다")
        void subscribe_reactivate_sent() {
            // given
            RestockSubscriptionRequest request = new RestockSubscriptionRequest(PRODUCT_ID);
            RestockSubscription sentSub = makeSubscription(SubscriptionStatusEnum.SENT);

            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(com.example.allinmarket.buyer.entity.Buyer.class)));
            given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID)).willReturn(Optional.of(mock(com.example.allinmarket.domain.product.entity.Product.class)));
            given(restockSubscriptionRepository.findByUserIdAndProductId(BUYER_ID, PRODUCT_ID)).willReturn(Optional.of(sentSub));

            // when
            RestockSubscriptionDetailResponse result = buyerRestockSubscriptionService.subscribe(BUYER_ID, request);

            // then
            assertThat(result.status()).isEqualTo(SubscriptionStatusEnum.ACTIVE);
            verify(restockSubscriptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("구매자가 존재하지 않으면 예외를 던진다")
        void subscribe_buyerNotFound() {
            // given
            RestockSubscriptionRequest request = new RestockSubscriptionRequest(PRODUCT_ID);
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buyerRestockSubscriptionService.subscribe(BUYER_ID, request))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.BUYER_NOT_FOUND);
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 예외를 던진다")
        void subscribe_productNotFound() {
            // given
            RestockSubscriptionRequest request = new RestockSubscriptionRequest(PRODUCT_ID);
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(com.example.allinmarket.buyer.entity.Buyer.class)));
            given(productRepository.findByIdAndDeletedAtIsNull(PRODUCT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buyerRestockSubscriptionService.subscribe(BUYER_ID, request))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PRODUCT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("재입고 알림 구독 취소")
    class UnsubscribeTest {

        @Test
        @DisplayName("구독을 hard delete한다")
        void unsubscribe_success() {
            // given
            RestockSubscription sub = makeSubscription(SubscriptionStatusEnum.ACTIVE);

            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(com.example.allinmarket.buyer.entity.Buyer.class)));
            given(restockSubscriptionRepository.findByUserIdAndProductId(BUYER_ID, PRODUCT_ID)).willReturn(Optional.of(sub));

            // when
            buyerRestockSubscriptionService.unsubscribe(BUYER_ID, PRODUCT_ID);

            // then
            verify(restockSubscriptionRepository).delete(sub);
        }

        @Test
        @DisplayName("구독이 없으면 예외를 던진다")
        void unsubscribe_notFound() {
            // given
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(com.example.allinmarket.buyer.entity.Buyer.class)));
            given(restockSubscriptionRepository.findByUserIdAndProductId(BUYER_ID, PRODUCT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buyerRestockSubscriptionService.unsubscribe(BUYER_ID, PRODUCT_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.RESTOCK_SUBSCRIPTION_NOT_FOUND);

            verify(restockSubscriptionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("구매자가 존재하지 않으면 예외를 던진다")
        void unsubscribe_buyerNotFound() {
            // given
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buyerRestockSubscriptionService.unsubscribe(BUYER_ID, PRODUCT_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.BUYER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("재입고 알림 목록 조회")
    class GetSubscriptionsTest {

        @Test
        @DisplayName("ACTIVE 상태 구독 목록을 반환한다")
        void getSubscriptions_success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            RestockSubscription sub = makeSubscription(SubscriptionStatusEnum.ACTIVE);
            Page<RestockSubscription> page = new PageImpl<>(List.of(sub), pageable, 1);

            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(com.example.allinmarket.buyer.entity.Buyer.class)));
            given(restockSubscriptionRepository.findByUserIdAndStatus(BUYER_ID, SubscriptionStatusEnum.ACTIVE, pageable)).willReturn(page);

            // when
            PageResponse<RestockSubscriptionDetailResponse> result = buyerRestockSubscriptionService.getSubscriptions(BUYER_ID, pageable);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("구독이 없으면 빈 목록을 반환한다")
        void getSubscriptions_empty() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(com.example.allinmarket.buyer.entity.Buyer.class)));
            given(restockSubscriptionRepository.findByUserIdAndStatus(BUYER_ID, SubscriptionStatusEnum.ACTIVE, pageable)).willReturn(Page.empty(pageable));

            // when
            PageResponse<RestockSubscriptionDetailResponse> result = buyerRestockSubscriptionService.getSubscriptions(BUYER_ID, pageable);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("구매자가 존재하지 않으면 예외를 던진다")
        void getSubscriptions_buyerNotFound() {
            // given
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buyerRestockSubscriptionService.getSubscriptions(BUYER_ID, PageRequest.of(0, 10)))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.BUYER_NOT_FOUND);
        }
    }
}

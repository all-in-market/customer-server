package com.example.allinmarket.buyer.restocknotification.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationDetailResponse;
import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import com.example.allinmarket.domain.restocknotification.repository.RestockNotificationRepository;
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
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class RestockNotificationServiceTest {

    @Mock
    private RestockNotificationRepository restockNotificationRepository;
    @Mock
    private BuyerRepository buyerRepository;

    @InjectMocks
    private RestockNotificationService restockNotificationService;

    private static final Long BUYER_ID = 1L;
    private static final Long PRODUCT_ID = 100L;

    private RestockNotification makeNotification(Long productId) {
        RestockNotification n = RestockNotification.of(BUYER_ID, productId);
        ReflectionTestUtils.setField(n, "id", 1L);
        return n;
    }

    @Nested
    @DisplayName("재입고 알림 목록 조회")
    class GetNotificationsTest {

        @Test
        @DisplayName("읽지 않은 알림 목록을 페이지로 반환한다")
        void getNotifications_success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            RestockNotification n = makeNotification(PRODUCT_ID);
            Page<RestockNotification> page = new PageImpl<>(List.of(n), pageable, 1);

            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(Buyer.class)));
            given(restockNotificationRepository.findByUserId(BUYER_ID, pageable)).willReturn(page);

            // when
            PageResponse<RestockNotificationDetailResponse> result = restockNotificationService.getNotifications(BUYER_ID, pageable);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).productId()).isEqualTo(PRODUCT_ID);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("알림이 없으면 빈 목록을 반환한다")
        void getNotifications_empty() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(Buyer.class)));
            given(restockNotificationRepository.findByUserId(BUYER_ID, pageable)).willReturn(Page.empty(pageable));

            // when
            PageResponse<RestockNotificationDetailResponse> result = restockNotificationService.getNotifications(BUYER_ID, pageable);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("구매자가 존재하지 않으면 예외를 던진다")
        void getNotifications_buyerNotFound() {
            // given
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> restockNotificationService.getNotifications(BUYER_ID, PageRequest.of(0, 10)))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.BUYER_NOT_FOUND);

            verify(restockNotificationRepository, never()).findByUserId(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("재입고 알림 읽음 처리")
    class ReadNotificationTest {

        @Test
        @DisplayName("알림을 읽음 상태로 변경한다")
        void readNotification_success() {
            // given
            RestockNotification notification = makeNotification(PRODUCT_ID);

            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(Buyer.class)));
            given(restockNotificationRepository.findByUserIdAndProductId(BUYER_ID, PRODUCT_ID)).willReturn(Optional.of(notification));

            // when
            restockNotificationService.readNotification(BUYER_ID, PRODUCT_ID);

            // then
            assertThat(notification.isRead()).isTrue();
        }

        @Test
        @DisplayName("알림이 존재하지 않으면 예외를 던진다")
        void readNotification_notFound() {
            // given
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(Buyer.class)));
            given(restockNotificationRepository.findByUserIdAndProductId(BUYER_ID, PRODUCT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> restockNotificationService.readNotification(BUYER_ID, PRODUCT_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.NOTIFICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("구매자가 존재하지 않으면 예외를 던진다")
        void readNotification_buyerNotFound() {
            // given
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> restockNotificationService.readNotification(BUYER_ID, PRODUCT_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.BUYER_NOT_FOUND);

            verify(restockNotificationRepository, never()).findByUserIdAndProductId(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("전체 알림 읽음 처리")
    class ReadAllNotificationsTest {

        @Test
        @DisplayName("구매자의 모든 읽지 않은 알림을 읽음 처리한다")
        void readAllNotifications_success() {
            // given
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.of(mock(Buyer.class)));

            // when
            restockNotificationService.readAllNotifications(BUYER_ID);

            // then
            verify(restockNotificationRepository).markAllAsReadByUserId(BUYER_ID);
        }

        @Test
        @DisplayName("구매자가 존재하지 않으면 예외를 던진다")
        void readAllNotifications_buyerNotFound() {
            // given
            given(buyerRepository.findByIdAndDeletedAtIsNull(BUYER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> restockNotificationService.readAllNotifications(BUYER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.BUYER_NOT_FOUND);

            verify(restockNotificationRepository, never()).markAllAsReadByUserId(anyLong());
        }
    }
}

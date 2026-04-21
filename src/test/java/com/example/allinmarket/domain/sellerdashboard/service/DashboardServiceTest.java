package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.example.allinmarket.seller.consts.sellerConsts.COMMISSION_RATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private SellerDashboardRepository sellerDashboardRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private Seller createSeller(Long id) {
        Seller seller = Seller.of(
                "seller" + id + "@test.com",
                "encodedPassword",
                "판매자" + id,
                "010-1234-567" + id,
                "테스트 스토어" + id,
                "123-45-6789" + id,
                "123-456789-12-345"
        );
        ReflectionTestUtils.setField(seller, "id", id);
        return seller;
    }

    private OrderItem createOrderItem(Seller seller, BigDecimal unitPrice, int quantity) {
        Order order = mock(Order.class);
        OrderItem item = OrderItem.of(order, mock(), seller, "상품명", unitPrice, quantity);
        return item;
    }

    @Nested
    @DisplayName("판매자 대시보드 업데이트")
    class UpdateSellerDashboard {

        @Test
        @DisplayName("오늘 대시보드가 없으면 새로 생성 후 저장")
        void updateSellerDashboard_createNew() {
            // given
            Long orderId = 1L;
            Seller seller = createSeller(1L);

            // 아이템 2개: 10000 * 2 + 5000 * 1 = 25000, 수량 합계 3
            List<OrderItem> items = List.of(
                    createOrderItem(seller, new BigDecimal("10000"), 2),
                    createOrderItem(seller, new BigDecimal("5000"), 1)
            );

            given(orderItemRepository.findAllByOrderIdWithSeller(orderId)).willReturn(items);

            // when
            dashboardService.updateSellerDashboard(orderId);

            // then - 원자적 UPDATE 쿼리 호출 검증
            verify(sellerDashboardRepository).addOrder(seller.getId(), new BigDecimal("25000"), 3, COMMISSION_RATE);
        }

        @Test
        @DisplayName("오늘 대시보드가 이미 있으면 기존 데이터에 누적")
        void updateSellerDashboard_updateExisting() {
            // given
            Long orderId = 1L;
            Seller seller = createSeller(1L);

            List<OrderItem> items = List.of(
                    createOrderItem(seller, new BigDecimal("10000"), 1)
            );

            given(orderItemRepository.findAllByOrderIdWithSeller(orderId)).willReturn(items);

            // when
            dashboardService.updateSellerDashboard(orderId);

            // then - DB 레벨 원자적 누적(+1 order, +1 product, +10000 sales)
            verify(sellerDashboardRepository).addOrder(seller.getId(), new BigDecimal("10000"), 1, COMMISSION_RATE);
        }

        @Test
        @DisplayName("한 주문에 여러 판매자가 있으면 판매자별로 각각 업데이트")
        void updateSellerDashboard_multiSeller() {
            // given
            Long orderId = 1L;
            Seller sellerA = createSeller(1L);
            Seller sellerB = createSeller(2L);

            List<OrderItem> items = List.of(
                    createOrderItem(sellerA, new BigDecimal("10000"), 1),
                    createOrderItem(sellerB, new BigDecimal("20000"), 2)
            );

            given(orderItemRepository.findAllByOrderIdWithSeller(orderId)).willReturn(items);

            // when
            dashboardService.updateSellerDashboard(orderId);

            // then - 판매자별로 각각 addOrder 호출
            verify(sellerDashboardRepository).addOrder(sellerA.getId(), new BigDecimal("10000"), 1, COMMISSION_RATE);
            verify(sellerDashboardRepository).addOrder(sellerB.getId(), new BigDecimal("40000"), 2, COMMISSION_RATE);
        }

        @Test
        @DisplayName("주문 아이템이 없으면 대시보드 업데이트 없음")
        void updateSellerDashboard_emptyItems() {
            // given
            Long orderId = 1L;
            given(orderItemRepository.findAllByOrderIdWithSeller(orderId)).willReturn(List.of());

            // when
            dashboardService.updateSellerDashboard(orderId);

            // then
            verify(sellerDashboardRepository, never()).findBySellerId(any());
            verify(sellerDashboardRepository, never()).save(any());
        }
    }
}

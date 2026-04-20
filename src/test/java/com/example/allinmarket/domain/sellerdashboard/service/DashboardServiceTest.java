package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
            given(sellerDashboardRepository.findBySellerIdAndStatDate(seller.getId(), LocalDate.now()))
                    .willReturn(Optional.empty());
            given(sellerDashboardRepository.save(any(SellerDashboard.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            dashboardService.updateSellerDashboard(orderId);

            // then
            ArgumentCaptor<SellerDashboard> captor = ArgumentCaptor.forClass(SellerDashboard.class);
            verify(sellerDashboardRepository).save(captor.capture());

            SellerDashboard saved = captor.getValue();
            assertThat(saved.getTotalOrders()).isEqualTo(1);
            assertThat(saved.getTotalProductsSold()).isEqualTo(3);
            assertThat(saved.getTotalSales()).isEqualByComparingTo("25000");
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

            SellerDashboard existing = SellerDashboard.of(
                    seller, LocalDate.now(), 2, 5, 0,
                    new BigDecimal("50000"), BigDecimal.ZERO
            );

            given(orderItemRepository.findAllByOrderIdWithSeller(orderId)).willReturn(items);
            given(sellerDashboardRepository.findBySellerIdAndStatDate(seller.getId(), LocalDate.now()))
                    .willReturn(Optional.of(existing));
            given(sellerDashboardRepository.save(any(SellerDashboard.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            dashboardService.updateSellerDashboard(orderId);

            // then
            // 기존 totalOrders=2 → 3, totalProductsSold=5 → 6, totalSales=50000 → 60000
            assertThat(existing.getTotalOrders()).isEqualTo(3);
            assertThat(existing.getTotalProductsSold()).isEqualTo(6);
            assertThat(existing.getTotalSales()).isEqualByComparingTo("60000");
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
            given(sellerDashboardRepository.findBySellerIdAndStatDate(sellerA.getId(), LocalDate.now()))
                    .willReturn(Optional.empty());
            given(sellerDashboardRepository.findBySellerIdAndStatDate(sellerB.getId(), LocalDate.now()))
                    .willReturn(Optional.empty());
            given(sellerDashboardRepository.save(any(SellerDashboard.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            // when
            dashboardService.updateSellerDashboard(orderId);

            // then - 판매자 수만큼 save 호출
            ArgumentCaptor<SellerDashboard> captor = ArgumentCaptor.forClass(SellerDashboard.class);
            verify(sellerDashboardRepository, times(2)).save(captor.capture());

            List<SellerDashboard> saved = captor.getAllValues();
            BigDecimal totalSalesA = saved.stream()
                    .filter(d -> d.getSeller().equals(sellerA))
                    .findFirst().map(SellerDashboard::getTotalSales).orElseThrow();
            BigDecimal totalSalesB = saved.stream()
                    .filter(d -> d.getSeller().equals(sellerB))
                    .findFirst().map(SellerDashboard::getTotalSales).orElseThrow();

            assertThat(totalSalesA).isEqualByComparingTo("10000");
            assertThat(totalSalesB).isEqualByComparingTo("40000");
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
            verify(sellerDashboardRepository, never()).findBySellerIdAndStatDate(any(), any());
            verify(sellerDashboardRepository, never()).save(any());
        }
    }
}

package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockReleaseServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private StockReleaseService stockReleaseService;

    private static final Long ORDER_ID = 1L;

    @Nested
    @DisplayName("재고 복구 및 주문 실패 처리")
    class ReleaseStockAndFailOrderTest {

        @Test
        @DisplayName("주문이 존재하지 않으면 예외를 던진다")
        void releaseStockAndFailOrder_whenOrderNotFound_thenThrowOrderNotFound() {
            // given
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockReleaseService.releaseStockAndFailOrder(ORDER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.ORDER_NOT_FOUND);

            verifyNoInteractions(orderItemRepository);
        }

        @Test
        @DisplayName("주문 상태가 CREATED가 아니면 아무 처리 없이 조용히 반환한다")
        void releaseStockAndFailOrder_whenOrderStatusNotCreated_thenReturnSilently() {
            // given
            Order order = createOrder(ORDER_ID, OrderStatus.PAID);
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

            // when
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            // then
            verifyNoInteractions(orderItemRepository);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("주문 상태가 CREATED면 재고를 복구하고 주문을 FAILED로 전이한다")
        void releaseStockAndFailOrder_whenOrderStatusCreated_thenReleaseStockAndFailOrder() {
            // given
            Order order = createOrder(ORDER_ID, OrderStatus.CREATED);
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

            Product product1 = createProduct(10L, 5);
            Product product2 = createProduct(20L, 0);

            OrderItem orderItem1 = createOrderItem(product1, 3);
            OrderItem orderItem2 = createOrderItem(product2, 7);

            given(orderItemRepository.findAllByOrderIdWithProduct(ORDER_ID))
                    .willReturn(List.of(orderItem1, orderItem2));

            // when
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            // then
            assertThat(product1.getStock()).isEqualTo(8);
            assertThat(product2.getStock()).isEqualTo(7);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);

            verify(orderItemRepository).findAllByOrderIdWithProduct(ORDER_ID);
        }
    }

    private Buyer createBuyer(Long id) {
        Buyer buyer = Buyer.of("test@test.com", "1234", "홍길동", "010-0000-0000");
        ReflectionTestUtils.setField(buyer, "id", id);
        return buyer;
    }

    private Order createOrder(Long id, OrderStatus status) {
        Buyer buyer = createBuyer(1L);
        Order order = Order.of(buyer, BigDecimal.TEN, null, "홍길동", "010-0000-0000", "서울시 강남구");
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "status", status);
        return order;
    }

    private Product createProduct(Long id, int stock) {
        Seller seller = mock(Seller.class);
        Category category = mock(Category.class);
        Product product = Product.of(seller, category, "상품", BigDecimal.TEN, stock, "설명");
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private OrderItem createOrderItem(Product product, int quantity) {
        Seller seller = mock(Seller.class);
        OrderItem orderItem = OrderItem.of(null, product, seller, "상품", BigDecimal.TEN, quantity);
        return orderItem;
    }
}

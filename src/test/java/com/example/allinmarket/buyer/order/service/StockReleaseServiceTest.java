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
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StockReleaseServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockReleaseService stockReleaseService;

    private static final Long ORDER_ID = 1L;

    @Nested
    @DisplayName("재고 복구 및 주문 실패 처리")
    class ReleaseStockAndFailOrderTest {

        @Test
        @DisplayName("주문이 존재하지 않으면 예외를 던지고 상품/주문상품 저장소와 상호작용하지 않는다")
        void releaseStockAndFailOrder_whenOrderNotFound_thenThrowOrderNotFound() {
            // given
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> stockReleaseService.releaseStockAndFailOrder(ORDER_ID))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.ORDER_NOT_FOUND);

            verifyNoInteractions(orderItemRepository);
            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("주문 상태가 CREATED가 아니면 재고 복구 없이 조용히 반환한다 (멱등 가드)")
        void releaseStockAndFailOrder_whenOrderStatusNotCreated_thenReturnSilentlyWithoutStockChange() {
            // given
            Order order = createOrder(ORDER_ID, OrderStatus.PAID);
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

            // when
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            // then
            verifyNoInteractions(orderItemRepository);
            verifyNoInteractions(productRepository);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("이미 FAILED로 종료된 주문도 재고 복구 없이 조용히 반환한다 (멱등 가드)")
        void releaseStockAndFailOrder_whenOrderAlreadyFailed_thenReturnSilentlyWithoutStockChange() {
            // given
            Order order = createOrder(ORDER_ID, OrderStatus.FAILED);
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

            // when
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            // then
            verifyNoInteractions(orderItemRepository);
            verifyNoInteractions(productRepository);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        }

        @Test
        @DisplayName("주문 상태가 CREATED면 각 주문상품 수량만큼 정확한 인자로 재고를 복구하고 주문을 FAILED로 전이한다")
        void releaseStockAndFailOrder_whenOrderStatusCreated_thenIncreaseStockWithExactArgsAndFailOrder() {
            // given
            Order order = createOrder(ORDER_ID, OrderStatus.CREATED);
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

            Product product1 = createProduct(10L, 5);
            Product product2 = createProduct(20L, 0);

            OrderItem orderItem1 = createOrderItem(product1, 3);
            OrderItem orderItem2 = createOrderItem(product2, 5);

            given(orderItemRepository.findAllByOrderIdWithProduct(ORDER_ID))
                    .willReturn(List.of(orderItem1, orderItem2));
            given(productRepository.increaseStock(10L, 3)).willReturn(1);
            given(productRepository.increaseStock(20L, 5)).willReturn(1);

            // when
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            // then
            verify(productRepository).increaseStock(eq(10L), eq(3));
            verify(productRepository).increaseStock(eq(20L), eq(5));
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);

            verify(orderItemRepository).findAllByOrderIdWithProduct(ORDER_ID);
        }

        @Test
        @DisplayName("order.fail()이 increaseStock 호출보다 먼저 수행된다")
        void releaseStockAndFailOrder_whenOrderStatusCreated_thenFailOrderBeforeIncreaseStock() {
            // given
            Order order = mock(Order.class);
            given(order.getStatus()).willReturn(OrderStatus.CREATED);
            given(order.getId()).willReturn(ORDER_ID);
            doNothing().when(order).fail();

            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

            Product product = createProduct(10L, 5);
            OrderItem orderItem = createOrderItem(product, 3);

            given(orderItemRepository.findAllByOrderIdWithProduct(ORDER_ID))
                    .willReturn(List.of(orderItem));
            given(productRepository.increaseStock(10L, 3)).willReturn(1);

            // when
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            // then
            InOrder inOrder = inOrder(order, productRepository);
            inOrder.verify(order).fail();
            inOrder.verify(productRepository).increaseStock(anyLong(), anyInt());
        }

        @Test
        @DisplayName("주문상품이 없으면 재고 복구 없이 주문만 FAILED로 전이한다")
        void releaseStockAndFailOrder_whenNoOrderItems_thenOnlyFailOrder() {
            // given
            Order order = createOrder(ORDER_ID, OrderStatus.CREATED);
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
            given(orderItemRepository.findAllByOrderIdWithProduct(ORDER_ID))
                    .willReturn(Collections.emptyList());

            // when
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("increaseStock이 갱신된 행이 없다고 반환해도 예외 없이 나머지 아이템 복구를 계속한다")
        void releaseStockAndFailOrder_whenIncreaseStockUpdatesNoRows_thenContinueWithoutException() {
            // given
            Order order = createOrder(ORDER_ID, OrderStatus.CREATED);
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));

            Product product1 = createProduct(10L, 5);
            Product product2 = createProduct(20L, 0);

            OrderItem orderItem1 = createOrderItem(product1, 3);
            OrderItem orderItem2 = createOrderItem(product2, 5);

            given(orderItemRepository.findAllByOrderIdWithProduct(ORDER_ID))
                    .willReturn(List.of(orderItem1, orderItem2));
            given(productRepository.increaseStock(10L, 3)).willReturn(0);
            given(productRepository.increaseStock(20L, 5)).willReturn(1);

            // when & then
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            verify(productRepository, times(1)).increaseStock(eq(10L), eq(3));
            verify(productRepository, times(1)).increaseStock(eq(20L), eq(5));
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        }

        @Test
        @DisplayName("비관적 락 조회 메서드(findByIdForUpdate)를 사용한다")
        void releaseStockAndFailOrder_whenCalled_thenUsePessimisticLockLookup() {
            // given
            Order order = createOrder(ORDER_ID, OrderStatus.CREATED);
            given(orderRepository.findByIdForUpdate(ORDER_ID)).willReturn(Optional.of(order));
            given(orderItemRepository.findAllByOrderIdWithProduct(ORDER_ID))
                    .willReturn(Collections.emptyList());

            // when
            stockReleaseService.releaseStockAndFailOrder(ORDER_ID);

            // then
            verify(orderRepository).findByIdForUpdate(ORDER_ID);
            verify(orderRepository, never()).findById(ORDER_ID);
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
        return OrderItem.of(null, product, seller, "상품", BigDecimal.TEN, quantity);
    }
}

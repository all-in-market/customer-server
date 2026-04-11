package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.order.dto.request.OrderCreateRequest;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.address.entity.Address;
import com.example.allinmarket.domain.address.repository.AddressRepository;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.cartitem.repository.CartItemRepository;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private BuyerRepository buyerRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderValidator orderValidator;

    @InjectMocks
    private BuyerOrderService buyerOrderService;

    @Nested
    @DisplayName("주문 생성")
    class CreateOrderTest {
        private Long buyerId;
        private Long addressId;
        private Long cartItemId1;
        private Long cartItemId2;

        @BeforeEach
        void setUp() {
            buyerId = 1L;
            addressId = 10L;
            cartItemId1 = 100L;
            cartItemId2 = 101L;
        }

        @Test
        @DisplayName("정상 주문 생성 성공")
        void createOrder_success() {
            // given
            OrderCreateRequest request = new OrderCreateRequest(List.of(cartItemId1, cartItemId2), addressId);

            Buyer buyer = mock(Buyer.class);
            Address address = mock(Address.class);

            Product product1 = mock(Product.class);
            Product product2 = mock(Product.class);

            CartItem cartItem1 = mock(CartItem.class);
            CartItem cartItem2 = mock(CartItem.class);

            given(buyerRepository.findById(buyerId)).willReturn(Optional.of(buyer));
            given(addressRepository.findByIdAndBuyerId(addressId, buyerId)).willReturn(Optional.of(address));
            given(cartItemRepository.findAllByIdsWithCartAndProduct(request.cartItemIds()))
                    .willReturn(List.of(cartItem1, cartItem2));

            given(cartItem1.getProduct()).willReturn(product1);
            given(cartItem2.getProduct()).willReturn(product2);

            given(product1.getId()).willReturn(1000L);
            given(product2.getId()).willReturn(2000L);

            given(product1.getPrice()).willReturn(BigDecimal.valueOf(1000));
            given(product2.getPrice()).willReturn(BigDecimal.valueOf(2000));

            given(cartItem1.getQuantity()).willReturn(2);
            given(cartItem2.getQuantity()).willReturn(3);

            given(productRepository.findAllByIdInWithSellerWithLock(List.of(1000L, 2000L)))
                    .willReturn(List.of(product1, product2));

            given(address.getRecipient()).willReturn("홍길동");
            given(address.getPhone()).willReturn("010-1111-2222");
            given(address.getDetail()).willReturn("서울시 강남구");

            given(orderRepository.save(any(Order.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            OrderDetailResponse result = buyerOrderService.createOrder(buyerId, request);

            // then
            assertThat(result).isNotNull();

            verify(orderValidator).validateCartItemsNotEmpty(anyList());
            verify(orderValidator).validateCartItemsOwnedByBuyer(anyList(), eq(buyerId));
            verify(orderValidator).validateProductSellable(anyMap(), anyList());

            verify(orderRepository).save(any(Order.class));
            verify(orderItemRepository).saveAll(anyList());
            verify(cartItemRepository).deleteAll(List.of(cartItem1, cartItem2));

            verify(product1).decreaseStock(2);
            verify(product2).decreaseStock(3);
        }
    }

    @Nested
    @DisplayName("주문 목록 조회")
    class FindAllOrdersTest {

        @Test
        @DisplayName("status가 null이면 구매자의 전체 주문을 조회한다")
        void findAllOrders_withNullStatus() {
            // given
            Long buyerId = 1L;
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

            Order order1 = mock(Order.class);
            Order order2 = mock(Order.class);

            stubOrder(order1, 100L, 1L, OrderStatus.CREATED);
            stubOrder(order2, 101L, 1L, OrderStatus.PAID);

            Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), pageable, 2);

            given(orderRepository.findByBuyerId(buyerId, pageable)).willReturn(orderPage);

            // when
            PageResponse<OrderDetailResponse> result = buyerOrderService.findAllOrders(buyerId, pageable, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(2);

            verify(orderRepository).findByBuyerId(buyerId, pageable);
            verify(orderRepository, never()).findByBuyerIdAndStatus(anyLong(), any(OrderStatus.class), any(Pageable.class));
        }

        @Test
        @DisplayName("status가 CREATED이면 구매자의 CREATED 주문만 조회한다")
        void findAllOrders_withCreatedStatus() {
            // given
            Long buyerId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Order order1 = mock(Order.class);
            Order order2 = mock(Order.class);

            stubOrder(order1, 200L, 1L, OrderStatus.CREATED);
            stubOrder(order2, 201L, 1L, OrderStatus.CREATED);

            Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), pageable, 2);

            given(orderRepository.findByBuyerIdAndStatus(buyerId, OrderStatus.CREATED, pageable))
                    .willReturn(orderPage);

            // when
            PageResponse<OrderDetailResponse> result =
                    buyerOrderService.findAllOrders(buyerId, pageable, OrderStatus.CREATED);

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(2);

            verify(orderRepository).findByBuyerIdAndStatus(buyerId, OrderStatus.CREATED, pageable);
            verify(orderRepository, never()).findByBuyerId(anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("status가 null이고 조회 결과가 없으면 빈 페이지를 반환한다")
        void findAllOrders_withNullStatusAndEmptyResult() {
            // given
            Long buyerId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            given(orderRepository.findByBuyerId(buyerId, pageable))
                    .willReturn(Page.empty(pageable));

            // when
            PageResponse<OrderDetailResponse> result = buyerOrderService.findAllOrders(buyerId, pageable, null);

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).isEmpty();

            verify(orderRepository).findByBuyerId(buyerId, pageable);
            verify(orderRepository, never()).findByBuyerIdAndStatus(anyLong(), any(OrderStatus.class), any(Pageable.class));
        }

        private void stubOrder(
                Order order,
                Long orderId,
                Long buyerId,
                OrderStatus status
        ) {
            Buyer buyer = mock(Buyer.class);

            // 필수
            given(order.getId()).willReturn(orderId);
            given(order.getBuyer()).willReturn(buyer);
            given(buyer.getId()).willReturn(buyerId);

            given(order.getTotalAmount()).willReturn(BigDecimal.valueOf(10000));
            given(order.getStatus()).willReturn(status);
            given(order.getTrackingNumber()).willReturn("TRACK-123");
            given(order.getRecipient()).willReturn("홍길동");
            given(order.getAddress()).willReturn("서울시 강남구");
        }
    }
}
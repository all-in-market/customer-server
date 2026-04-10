package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.order.dto.request.OrderCreateRequest;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.address.entity.Address;
import com.example.allinmarket.domain.address.repository.AddressRepository;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.cartitem.repository.CartItemRepository;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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
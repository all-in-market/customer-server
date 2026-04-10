package com.example.allinmarket.buyer.order.service;

import static org.junit.jupiter.api.Assertions.*;

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
import org.junit.jupiter.api.Nested;
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
        Cart cart = mock(Cart.class);

        Product product1 = mock(Product.class);
        Product product2 = mock(Product.class);

        CartItem cartItem1 = mock(CartItem.class);
        CartItem cartItem2 = mock(CartItem.class);

        Order savedOrder = mock(Order.class);

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(buyer));
        given(addressRepository.findByIdAndBuyerId(addressId, buyerId)).willReturn(Optional.of(address));
        given(cartItemRepository.findAllByIdsWithCartAndProduct(request.cartItemIds()))
                .willReturn(List.of(cartItem1, cartItem2));

        given(cartItem1.getCart()).willReturn(cart);
        given(cartItem2.getCart()).willReturn(cart);
        given(cart.getBuyer()).willReturn(buyer);

        given(cartItem1.getProduct()).willReturn(product1);
        given(cartItem2.getProduct()).willReturn(product2);

        given(product1.getId()).willReturn(1000L);
        given(product2.getId()).willReturn(2000L);

        given(product1.getStatus()).willReturn(ProductStatus.ON_SALE);
        given(product2.getStatus()).willReturn(ProductStatus.ON_SALE);

        given(product1.getStock()).willReturn(10);
        given(product2.getStock()).willReturn(20);

        given(product1.getPrice()).willReturn(BigDecimal.valueOf(1000));
        given(product2.getPrice()).willReturn(BigDecimal.valueOf(2000));

        given(cartItem1.getQuantity()).willReturn(2);
        given(cartItem2.getQuantity()).willReturn(3);

        given(productRepository.findAllByIdInWithSellerWithLock(List.of(1000L, 2000L)))
                .willReturn(List.of(product1, product2));

        given(address.getRecipient()).willReturn("홍길동");
        given(address.getPhone()).willReturn("010-1111-2222");
        given(address.getDetail()).willReturn("서울시 강남구");

        given(orderRepository.save(any(Order.class))).willReturn(savedOrder);

        // when
        OrderDetailResponse result = buyerOrderService.createOrder(buyerId, request);

        // then
        assertThat(result).isNotNull();

        then(orderValidator).should().validateCartItemsNotEmpty(anyList());
        then(orderValidator).should().validateCartItemsOwnedByBuyer(anyList(), eq(buyerId));
        then(orderValidator).should().validateProductSellable(anyMap(), anyList());

        then(orderRepository).should().save(any(Order.class));
        then(orderItemRepository).should().saveAll(anyList());
        then(cartItemRepository).should().deleteAll(List.of(cartItem1, cartItem2));

        then(product1).should().decreaseStock(2);
        then(product2).should().decreaseStock(3);
    }

    @Test
    @DisplayName("buyer가 없으면 BUYER_NOT_FOUND 예외")
    void createOrder_fail_buyerNotFound() {
        // given
        OrderCreateRequest request = new OrderCreateRequest(List.of(cartItemId1), addressId);
        given(buyerRepository.findById(buyerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> buyerOrderService.createOrder(buyerId, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.BUYER_NOT_FOUND);

        then(addressRepository).shouldHaveNoInteractions();
        then(cartItemRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("address가 없거나 본인 주소가 아니면 ADDRESS_NOT_FOUND 예외")
    void createOrder_fail_addressNotFound() {
        // given
        OrderCreateRequest request = new OrderCreateRequest(List.of(cartItemId1), addressId);
        Buyer buyer = mock(Buyer.class);

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(buyer));
        given(addressRepository.findByIdAndBuyerId(addressId, buyerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> buyerOrderService.createOrder(buyerId, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.ADDRESS_NOT_FOUND);

        then(cartItemRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("cartItems가 비어 있으면 예외")
    void createOrder_fail_cartItemsEmpty() {
        // given
        OrderCreateRequest request = new OrderCreateRequest(List.of(cartItemId1), addressId);
        Buyer buyer = mock(Buyer.class);
        Address address = mock(Address.class);

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(buyer));
        given(addressRepository.findByIdAndBuyerId(addressId, buyerId)).willReturn(Optional.of(address));
        given(cartItemRepository.findAllByIdsWithCartAndProduct(request.cartItemIds())).willReturn(List.of());

        willThrow(new BaseException(ErrorEnum.CART_ITEMS_EMPTY))
                .given(orderValidator).validateCartItemsNotEmpty(anyList());

        // when & then
        assertThatThrownBy(() -> buyerOrderService.createOrder(buyerId, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.CART_ITEMS_EMPTY);

        then(orderRepository).shouldHaveNoInteractions();
        then(orderItemRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("cartItem 소유자가 다르면 INVALID_CART_ITEM_OWNER 예외")
    void createOrder_fail_invalidCartItemOwner() {
        // given
        OrderCreateRequest request = new OrderCreateRequest(List.of(cartItemId1), addressId);
        Buyer buyer = mock(Buyer.class);
        Address address = mock(Address.class);
        CartItem cartItem = mock(CartItem.class);

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(buyer));
        given(addressRepository.findByIdAndBuyerId(addressId, buyerId)).willReturn(Optional.of(address));
        given(cartItemRepository.findAllByIdsWithCartAndProduct(request.cartItemIds())).willReturn(List.of(cartItem));

        willDoNothing().given(orderValidator).validateCartItemsNotEmpty(anyList());
        willThrow(new BaseException(ErrorEnum.INVALID_CART_ITEM_OWNER))
                .given(orderValidator).validateCartItemsOwnedByBuyer(anyList(), eq(buyerId));

        // when & then
        assertThatThrownBy(() -> buyerOrderService.createOrder(buyerId, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.INVALID_CART_ITEM_OWNER);

        then(productRepository).shouldHaveNoInteractions();
        then(orderRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("상품이 판매중이 아니면 PRODUCT_NOT_AVAILABLE 예외")
    void createOrder_fail_productNotAvailable() {
        // given
        OrderCreateRequest request = new OrderCreateRequest(List.of(cartItemId1), addressId);

        Buyer buyer = mock(Buyer.class);
        Address address = mock(Address.class);
        CartItem cartItem = mock(CartItem.class);
        Product product = mock(Product.class);

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(buyer));
        given(addressRepository.findByIdAndBuyerId(addressId, buyerId)).willReturn(Optional.of(address));
        given(cartItemRepository.findAllByIdsWithCartAndProduct(request.cartItemIds()))
                .willReturn(List.of(cartItem));

        given(cartItem.getProduct()).willReturn(product);
        given(product.getId()).willReturn(1000L);
        given(productRepository.findAllByIdInWithSellerWithLock(List.of(1000L)))
                .willReturn(List.of(product));

        willDoNothing().given(orderValidator).validateCartItemsNotEmpty(anyList());
        willDoNothing().given(orderValidator).validateCartItemsOwnedByBuyer(anyList(), eq(buyerId));
        willThrow(new BaseException(ErrorEnum.PRODUCT_NOT_AVAILABLE))
                .given(orderValidator).validateProductSellable(anyMap(), anyList());

        // when & then
        assertThatThrownBy(() -> buyerOrderService.createOrder(buyerId, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorEnum")
                .isEqualTo(ErrorEnum.PRODUCT_NOT_AVAILABLE);

        then(orderRepository).shouldHaveNoInteractions();
        then(orderItemRepository).shouldHaveNoInteractions();
    }
}
package com.example.allinmarket.buyer.order.facade;

import com.example.allinmarket.buyer.cart.service.BuyerCartService;
import com.example.allinmarket.buyer.consts.BuyerConsts;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.order.dto.request.OrderCreateRequest;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.buyer.order.service.BuyerOrderService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerPaymentOrderFacadeTest {

    @Mock
    private BuyerOrderService buyerOrderService;

    @Mock
    private BuyerCartService buyerCartService;

    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private BuyerPaymentOrderFacade buyerPaymentOrderFacade;

    private static final Long BUYER_ID = 1L;

    @Nested
    @DisplayName("주문 생성")
    class CreateOrderTest {

        @Test
        @DisplayName("요청한 cartItemId 개수와 실제 조회된 cartItem 개수가 다르면 예외를 던진다")
        void createOrder_whenCartItemCountMismatch_thenThrowInvalidOrderCartItems() {
            // given
            OrderCreateRequest request = new OrderCreateRequest(List.of(1L, 2L, 3L), 10L);

            CartItem cartItem = createCartItem(1L, createProduct(100L, 10));

            given(buyerCartService.getCartItemsByIdsAndBuyerId(request.cartItemIds(), BUYER_ID))
                    .willReturn(List.of(cartItem));

            // when & then
            assertThatThrownBy(() -> buyerPaymentOrderFacade.createOrder(BUYER_ID, request))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_ORDER_CART_ITEMS);

            verifyNoInteractions(redissonClient);
            verifyNoInteractions(buyerOrderService);
        }

        @Test
        @DisplayName("상품 ID 오름차순으로 락을 획득한다")
        void createOrder_whenMultipleProducts_thenAcquireLocksInAscendingProductIdOrder() {
            // given
            Product productHigh = createProduct(300L, 10);
            Product productLow = createProduct(100L, 10);
            Product productMid = createProduct(200L, 10);

            // 뒤섞인 순서로 카트 아이템 구성
            CartItem cartItemHigh = createCartItem(1L, productHigh);
            CartItem cartItemLow = createCartItem(2L, productLow);
            CartItem cartItemMid = createCartItem(3L, productMid);

            List<CartItem> cartItems = List.of(cartItemHigh, cartItemLow, cartItemMid);
            OrderCreateRequest request = new OrderCreateRequest(List.of(1L, 2L, 3L), 10L);

            given(buyerCartService.getCartItemsByIdsAndBuyerId(request.cartItemIds(), BUYER_ID))
                    .willReturn(cartItems);

            RLock lockLow = mock(RLock.class);
            RLock lockMid = mock(RLock.class);
            RLock lockHigh = mock(RLock.class);

            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 100L)).willReturn(lockLow);
            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 200L)).willReturn(lockMid);
            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 300L)).willReturn(lockHigh);

            try {
                given(lockLow.tryLock(3, TimeUnit.SECONDS)).willReturn(true);
                given(lockMid.tryLock(3, TimeUnit.SECONDS)).willReturn(true);
                given(lockHigh.tryLock(3, TimeUnit.SECONDS)).willReturn(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            given(lockLow.isHeldByCurrentThread()).willReturn(true);
            given(lockMid.isHeldByCurrentThread()).willReturn(true);
            given(lockHigh.isHeldByCurrentThread()).willReturn(true);

            OrderDetailResponse response = createOrderDetailResponse();
            given(buyerOrderService.createOrder(BUYER_ID, cartItems, request.addressId())).willReturn(response);

            // when
            OrderDetailResponse result = buyerPaymentOrderFacade.createOrder(BUYER_ID, request);

            // then
            InOrder inOrder = inOrder(redissonClient);
            inOrder.verify(redissonClient).getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 100L);
            inOrder.verify(redissonClient).getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 200L);
            inOrder.verify(redissonClient).getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 300L);

            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("락 획득에 실패하면 예외를 던지고 이미 획득한 락은 해제한다")
        void createOrder_whenTryLockFails_thenThrowRedisLockConflictAndReleaseAcquiredLocks() throws InterruptedException {
            // given
            Product productLow = createProduct(100L, 10);
            Product productHigh = createProduct(200L, 10);

            CartItem cartItemLow = createCartItem(1L, productLow);
            CartItem cartItemHigh = createCartItem(2L, productHigh);

            List<CartItem> cartItems = List.of(cartItemLow, cartItemHigh);
            OrderCreateRequest request = new OrderCreateRequest(List.of(1L, 2L), 10L);

            given(buyerCartService.getCartItemsByIdsAndBuyerId(request.cartItemIds(), BUYER_ID))
                    .willReturn(cartItems);

            RLock lockLow = mock(RLock.class);
            RLock lockHigh = mock(RLock.class);

            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 100L)).willReturn(lockLow);
            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 200L)).willReturn(lockHigh);

            given(lockLow.tryLock(3, TimeUnit.SECONDS)).willReturn(true);
            given(lockHigh.tryLock(3, TimeUnit.SECONDS)).willReturn(false);

            given(lockLow.isHeldByCurrentThread()).willReturn(true);

            // when & then
            assertThatThrownBy(() -> buyerPaymentOrderFacade.createOrder(BUYER_ID, request))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.REDIS_LOCK_CONFLICT);

            verify(lockLow).unlock();
            verify(lockHigh, never()).unlock();
            verifyNoInteractions(buyerOrderService);
        }

        @Test
        @DisplayName("락 획득 중 인터럽트가 발생하면 예외를 던진다")
        void createOrder_whenTryLockInterrupted_thenThrowRedisLockInterrupted() throws InterruptedException {
            // given
            Product product = createProduct(100L, 10);
            CartItem cartItem = createCartItem(1L, product);

            List<CartItem> cartItems = List.of(cartItem);
            OrderCreateRequest request = new OrderCreateRequest(List.of(1L), 10L);

            given(buyerCartService.getCartItemsByIdsAndBuyerId(request.cartItemIds(), BUYER_ID))
                    .willReturn(cartItems);

            RLock lock = mock(RLock.class);
            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 100L)).willReturn(lock);
            given(lock.tryLock(3, TimeUnit.SECONDS)).willThrow(new InterruptedException());

            // when & then
            assertThatThrownBy(() -> buyerPaymentOrderFacade.createOrder(BUYER_ID, request))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.REDIS_LOCK_INTERRUPTED);

            verifyNoInteractions(buyerOrderService);

            // 인터럽트 상태 복구 확인 및 정리
            assertThat(Thread.interrupted()).isTrue();
        }

        @Test
        @DisplayName("정상 경로에서는 획득한 락을 역순으로 해제하고 buyerOrderService에 위임한다")
        void createOrder_whenSuccess_thenReleaseLocksInReverseOrderAndDelegateToOrderService() throws InterruptedException {
            // given
            Product productLow = createProduct(100L, 10);
            Product productHigh = createProduct(200L, 10);

            CartItem cartItemLow = createCartItem(1L, productLow);
            CartItem cartItemHigh = createCartItem(2L, productHigh);

            List<CartItem> cartItems = List.of(cartItemLow, cartItemHigh);
            OrderCreateRequest request = new OrderCreateRequest(List.of(1L, 2L), 10L);

            given(buyerCartService.getCartItemsByIdsAndBuyerId(request.cartItemIds(), BUYER_ID))
                    .willReturn(cartItems);

            RLock lockLow = mock(RLock.class);
            RLock lockHigh = mock(RLock.class);

            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 100L)).willReturn(lockLow);
            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 200L)).willReturn(lockHigh);

            given(lockLow.tryLock(3, TimeUnit.SECONDS)).willReturn(true);
            given(lockHigh.tryLock(3, TimeUnit.SECONDS)).willReturn(true);

            given(lockLow.isHeldByCurrentThread()).willReturn(true);
            given(lockHigh.isHeldByCurrentThread()).willReturn(true);

            OrderDetailResponse response = createOrderDetailResponse();
            given(buyerOrderService.createOrder(BUYER_ID, cartItems, request.addressId())).willReturn(response);

            // when
            OrderDetailResponse result = buyerPaymentOrderFacade.createOrder(BUYER_ID, request);

            // then
            InOrder inOrder = inOrder(lockHigh, lockLow);
            inOrder.verify(lockHigh).unlock();
            inOrder.verify(lockLow).unlock();

            verify(buyerOrderService).createOrder(BUYER_ID, cartItems, request.addressId());
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("isHeldByCurrentThread가 false인 락은 해제하지 않는다")
        void createOrder_whenLockNotHeldByCurrentThread_thenDoesNotUnlock() throws InterruptedException {
            // given
            Product product = createProduct(100L, 10);
            CartItem cartItem = createCartItem(1L, product);

            List<CartItem> cartItems = List.of(cartItem);
            OrderCreateRequest request = new OrderCreateRequest(List.of(1L), 10L);

            given(buyerCartService.getCartItemsByIdsAndBuyerId(request.cartItemIds(), BUYER_ID))
                    .willReturn(cartItems);

            RLock lock = mock(RLock.class);
            given(redissonClient.getLock(BuyerConsts.PRODUCT_LOCK_PREFIX + 100L)).willReturn(lock);
            given(lock.tryLock(3, TimeUnit.SECONDS)).willReturn(true);
            given(lock.isHeldByCurrentThread()).willReturn(false);

            OrderDetailResponse response = createOrderDetailResponse();
            given(buyerOrderService.createOrder(BUYER_ID, cartItems, request.addressId())).willReturn(response);

            // when
            buyerPaymentOrderFacade.createOrder(BUYER_ID, request);

            // then
            verify(lock, never()).unlock();
        }
    }

    private Buyer createBuyer(Long id) {
        Buyer buyer = Buyer.of("test@test.com", "1234", "홍길동", "010-0000-0000");
        ReflectionTestUtils.setField(buyer, "id", id);
        return buyer;
    }

    private Product createProduct(Long id, int stock) {
        Seller seller = mock(Seller.class);
        Category category = mock(Category.class);
        Product product = Product.of(seller, category, "상품", BigDecimal.TEN, stock, "설명");
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "status", ProductStatus.ON_SALE);
        return product;
    }

    private CartItem createCartItem(Long id, Product product) {
        Buyer buyer = createBuyer(BUYER_ID);
        Cart cart = Cart.of(buyer);
        CartItem cartItem = CartItem.of(cart, product);
        ReflectionTestUtils.setField(cartItem, "id", id);
        return cartItem;
    }

    private OrderDetailResponse createOrderDetailResponse() {
        return new OrderDetailResponse(
                1L,
                BUYER_ID,
                BigDecimal.TEN,
                OrderStatus.CREATED,
                null,
                "홍길동",
                "서울시 강남구"
        );
    }
}

package com.example.allinmarket.buyer.order.facade;

import com.example.allinmarket.buyer.cart.service.BuyerCartService;
import com.example.allinmarket.buyer.consts.BuyerConsts;
import com.example.allinmarket.buyer.order.dto.request.OrderCreateRequest;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.buyer.order.service.BuyerOrderService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BuyerPaymentOrderFacade {

    private final BuyerOrderService buyerOrderService;
    private final BuyerCartService  buyerCartService;
    private final RedissonClient redissonClient;

    public OrderDetailResponse createOrder(Long buyerId, OrderCreateRequest request){

        List<CartItem> cartItems = buyerCartService.getCartItemsByIdsAndBuyerId(request.cartItemIds(), buyerId);

        if (cartItems.size() != request.cartItemIds().size()) {
            throw new BaseException(ErrorEnum.INVALID_ORDER_CART_ITEMS);
        }

        List<Long> productIds = cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        List<RLock> locks = new ArrayList<>();

        try {
            for (Long productId : productIds) {
                String key = BuyerConsts.PRODUCT_LOCK_PREFIX + productId;
                RLock lock = redissonClient.getLock(key);

                boolean acquired = lock.tryLock(3, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new BaseException(ErrorEnum.REDIS_LOCK_CONFLICT);
                }

                locks.add(lock);
            }

            return buyerOrderService.createOrder(buyerId, cartItems, request.addressId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException(ErrorEnum.REDIS_LOCK_INTERRUPTED);
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                RLock lock = locks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}

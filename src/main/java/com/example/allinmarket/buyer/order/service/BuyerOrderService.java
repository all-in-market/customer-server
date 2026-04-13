package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.order.dto.response.OrderWithOrderItemDetailResponse;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.address.entity.Address;
import com.example.allinmarket.domain.address.repository.AddressRepository;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.cartitem.repository.CartItemRepository;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.orderitem.dto.OrderItemDetailResponse;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.buyer.order.dto.request.OrderCreateRequest;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerOrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final BuyerRepository buyerRepository;
    private final AddressRepository addressRepository;
    private final OrderItemRepository orderItemRepository;

    private final OrderValidator orderValidator;

    /**
     * 주문 생성
     */
    @Transactional
    public OrderDetailResponse createOrder(Long buyerId, OrderCreateRequest request) {

        Buyer buyer = buyerRepository.findById(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        Address address = addressRepository.findByIdAndBuyerId(request.addressId(), buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.ADDRESS_NOT_FOUND)
        );

        List<CartItem> cartItems = cartItemRepository.findAllByIdsWithCartAndProduct(request.cartItemIds());

        orderValidator.validateCartItemsNotEmpty(cartItems);
        orderValidator.validateCartItemsOwnedByBuyer(cartItems, buyerId);

        List<Product> products = findAndLockProducts(cartItems);

        // n+1 쿼리 문제 해결을 위해 생성
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        orderValidator.validateProductSellable(productMap, cartItems);

        BigDecimal totalAmount = calculateTotalAmount(productMap, cartItems);

        Order order = orderRepository.save(
                Order.of(
                        buyer,
                        totalAmount,
                        null,
                        address.getRecipient(),
                        address.getPhone(),
                        address.getDetail()
                )
        );

        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    Product product = productMap.get(cartItem.getProduct().getId());
                    return OrderItem.of(
                            order,
                            product,
                            product.getSeller(),
                            product.getName(),
                            product.getPrice(),
                            cartItem.getQuantity()
                    );
                })
                .toList();

        orderItemRepository.saveAll(orderItems);

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());
            product.decreaseStock(cartItem.getQuantity());
        }

        cartItemRepository.deleteAll(cartItems);

        return OrderDetailResponse.from(order);
    }

    /**
     * 주문 내역 전체 조회
     */
    public PageResponse<OrderDetailResponse> findAllOrders(Long buyerId, Pageable pageable, OrderStatus status) {

        if (status == null) {
            return PageResponse.register(
                    orderRepository.findByBuyerId(buyerId, pageable)
                            .map(OrderDetailResponse::from)
            );
        }

        return PageResponse.register(
                orderRepository.findByBuyerIdAndStatus(buyerId, status, pageable)
                        .map(OrderDetailResponse::from)
        );
    }

    /**
     * 주문 내역 단건 조회
     */
    public OrderWithOrderItemDetailResponse findOrder(Long orderId, Long buyerId) {
        Order order = orderRepository.findByIdAndBuyerId(orderId, buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.ORDER_NOT_FOUND)
        );

        List<OrderItem> orderItems = orderItemRepository.findAllByBuyerIdAndOrderId(buyerId, orderId);

        List<OrderItemDetailResponse> orderItemDetailResponseList = orderItems.stream()
                .map(OrderItemDetailResponse::from)
                .toList();

        return OrderWithOrderItemDetailResponse.from(order, orderItemDetailResponseList);
    }


    /**
     * 주문 생성 시, 총 주문 금액 계산
     */
    private BigDecimal calculateTotalAmount(Map<Long, Product> productMap, List<CartItem> cartItems) {

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());

            BigDecimal productTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            totalAmount = totalAmount.add(productTotal);
        }

        return totalAmount;
    }

    /**
     * cartItem에 있는 담긴 상품을 락을 걸고 조회
     */
    private List<Product> findAndLockProducts(List<CartItem> cartItems) {
        List<Long> productIds = cartItems.stream()
                .map(cartItem -> cartItem.getProduct().getId())
                .distinct()
                .sorted()
                .toList();

        List<Product> products = productRepository.findAllByIdInWithSellerWithLock(productIds);

        if(products.size() != productIds.size()) {
            throw new BaseException(ErrorEnum.INVALID_ORDER_PRODUCT);
        }

        return products;
    }

}

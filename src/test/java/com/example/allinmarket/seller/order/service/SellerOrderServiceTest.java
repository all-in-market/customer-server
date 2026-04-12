package com.example.allinmarket.seller.order.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.order.dto.response.SellerOrderGetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SellerOrderServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private SellerOrderService sellerOrderService;

    private Order createOrderMock(Long orderId, BigDecimal totalAmount) {
        Buyer buyer = mock(Buyer.class);
        given(buyer.getId()).willReturn(1L);

        Order order = mock(Order.class);
        given(order.getId()).willReturn(orderId);
        given(order.getBuyer()).willReturn(buyer);
        given(order.getTotalAmount()).willReturn(totalAmount);
        given(order.getStatus()).willReturn(OrderStatus.PAID);
        given(order.getTrackingNumber()).willReturn("TRACK123");
        given(order.getRecipient()).willReturn("홍길동");
        given(order.getPhone()).willReturn("010-1234-5678");
        given(order.getAddress()).willReturn("서울시 강남구");
        return order;
    }

    private OrderItem createOrderItemMock(Order order, Long itemId, String productName, BigDecimal price, int quantity) {
        Product product = mock(Product.class);
        given(product.getId()).willReturn(itemId);

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(1L);

        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getOrder()).willReturn(order);
        given(orderItem.getId()).willReturn(itemId);
        given(orderItem.getProduct()).willReturn(product);
        given(orderItem.getSeller()).willReturn(seller);
        given(orderItem.getProductName()).willReturn(productName);
        given(orderItem.getUnitPrice()).willReturn(price);
        given(orderItem.getQuantity()).willReturn(quantity);
        return orderItem;
    }

    @Test
    void 판매자_주문목록_조회_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Order order = createOrderMock(1L, BigDecimal.valueOf(30000));
        OrderItem orderItem = createOrderItemMock(order, 1L, "테스트 상품", BigDecimal.valueOf(30000), 1);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of(orderItem));

        // when
        PageResponse<SellerOrderGetResponse> result = sellerOrderService.findAll(sellerId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(1L, result.content().get(0).id());
    }

    @Test
    void 판매자_주문목록_조회_같은주문_상품여러개_그룹핑_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Order order = createOrderMock(1L, BigDecimal.valueOf(50000));
        OrderItem item1 = createOrderItemMock(order, 1L, "신발", BigDecimal.valueOf(30000), 1);
        OrderItem item2 = createOrderItemMock(order, 2L, "양말", BigDecimal.valueOf(20000), 2);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of(item1, item2));

        // when
        PageResponse<SellerOrderGetResponse> result = sellerOrderService.findAll(sellerId, pageable);

        // then
        assertEquals(1, result.content().size());
        assertEquals(2, result.content().get(0).items().size());
    }

    @Test
    void 판매자_주문목록_조회_빈목록_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of());

        // when
        PageResponse<SellerOrderGetResponse> result = sellerOrderService.findAll(sellerId, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
        assertTrue(result.isLast());
    }

    @Test
    void 판매자_주문목록_조회_페이징_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 2);

        Order order1 = createOrderMock(1L, BigDecimal.valueOf(10000));
        Order order2 = createOrderMock(2L, BigDecimal.valueOf(20000));
        Order order3 = createOrderMock(3L, BigDecimal.valueOf(30000));

        OrderItem item1 = createOrderItemMock(order1, 1L, "상품1", BigDecimal.valueOf(10000), 1);
        OrderItem item2 = createOrderItemMock(order2, 2L, "상품2", BigDecimal.valueOf(20000), 1);
        OrderItem item3 = createOrderItemMock(order3, 3L, "상품3", BigDecimal.valueOf(30000), 1);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of(item1, item2, item3));

        // when
        PageResponse<SellerOrderGetResponse> result = sellerOrderService.findAll(sellerId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(2, result.content().size());
        assertEquals(2, result.totalPages());
        assertFalse(result.isLast());
    }

    @Test
    void 판매자_주문목록_조회_두번째_페이지_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(1, 2);

        Order order1 = createOrderMock(1L, BigDecimal.valueOf(10000));
        Order order2 = createOrderMock(2L, BigDecimal.valueOf(20000));
        Order order3 = createOrderMock(3L, BigDecimal.valueOf(30000));

        OrderItem item1 = createOrderItemMock(order1, 1L, "상품1", BigDecimal.valueOf(10000), 1);
        OrderItem item2 = createOrderItemMock(order2, 2L, "상품2", BigDecimal.valueOf(20000), 1);
        OrderItem item3 = createOrderItemMock(order3, 3L, "상품3", BigDecimal.valueOf(30000), 1);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of(item1, item2, item3));

        // when
        PageResponse<SellerOrderGetResponse> result = sellerOrderService.findAll(sellerId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(2, result.currentPage());
        assertTrue(result.isLast());
    }
}
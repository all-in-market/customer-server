package com.example.allinmarket.seller.order.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
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

    // 목록 조회용 - Order 정보만 필요
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
        given(order.getAddress()).willReturn("서울시 강남구");
        return order;
    }

    // 목록 조회용 - OrderItem은 getOrder()만 필요
    private OrderItem createOrderItemForList(Order order) {
        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getOrder()).willReturn(order);
        return orderItem;
    }

    // 상세 조회용 - OrderItem 전체 정보 필요
    private OrderItem createOrderItemForDetail(Order order, Long itemId, String productName, BigDecimal price, int quantity) {
        Product product = mock(Product.class);
        given(product.getId()).willReturn(itemId);

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(1L);

        Order orderWithPhone = order;
        given(orderWithPhone.getPhone()).willReturn("010-1234-5678");

        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getOrder()).willReturn(orderWithPhone);
        given(orderItem.getId()).willReturn(itemId);
        given(orderItem.getProduct()).willReturn(product);
        given(orderItem.getSeller()).willReturn(seller);
        given(orderItem.getProductName()).willReturn(productName);
        given(orderItem.getUnitPrice()).willReturn(price);
        given(orderItem.getQuantity()).willReturn(quantity);
        return orderItem;
    }

    // ==================== 목록 조회 ====================

    @Test
    void 판매자_주문목록_조회_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Order order = createOrderMock(1L, BigDecimal.valueOf(30000));
        OrderItem orderItem = createOrderItemForList(order);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of(orderItem));

        // when
        PageResponse<OrderDetailResponse> result = sellerOrderService.findAll(sellerId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(1L, result.content().get(0).orderId());
    }

    @Test
    void 판매자_주문목록_조회_같은주문_상품여러개_그룹핑_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Order order = createOrderMock(1L, BigDecimal.valueOf(50000));
        OrderItem item1 = createOrderItemForList(order);
        OrderItem item2 = createOrderItemForList(order);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of(item1, item2));

        // when
        PageResponse<OrderDetailResponse> result = sellerOrderService.findAll(sellerId, pageable);

        // then
        assertEquals(1, result.content().size()); // 주문 1건으로 묶임
        assertEquals(1, result.totalElements());
    }

    @Test
    void 판매자_주문목록_조회_빈목록_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of());

        // when
        PageResponse<OrderDetailResponse> result = sellerOrderService.findAll(sellerId, pageable);

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

        OrderItem item1 = createOrderItemForList(order1);
        OrderItem item2 = createOrderItemForList(order2);
        OrderItem item3 = createOrderItemForList(order3);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of(item1, item2, item3));

        // when
        PageResponse<OrderDetailResponse> result = sellerOrderService.findAll(sellerId, pageable);

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

        OrderItem item1 = createOrderItemForList(order1);
        OrderItem item2 = createOrderItemForList(order2);
        OrderItem item3 = createOrderItemForList(order3);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(List.of(item1, item2, item3));

        // when
        PageResponse<OrderDetailResponse> result = sellerOrderService.findAll(sellerId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(2, result.currentPage());
        assertTrue(result.isLast());
    }

    // ==================== 상세 조회 ====================

    @Test
    void 판매자_주문상세_조회_성공_테스트() {
        // given
        Long sellerId = 1L;
        Long orderId = 1L;

        Order order = createOrderMock(orderId, BigDecimal.valueOf(50000));
        OrderItem item1 = createOrderItemForDetail(order, 1L, "신발", BigDecimal.valueOf(30000), 1);
        OrderItem item2 = createOrderItemForDetail(order, 2L, "양말", BigDecimal.valueOf(20000), 1);

        given(orderItemRepository.findAllBySellerIdAndOrderId(sellerId, orderId))
                .willReturn(List.of(item1, item2));

        // when
        SellerOrderGetResponse result = sellerOrderService.findOne(sellerId, orderId);

        // then
        assertNotNull(result);
        assertEquals(orderId, result.id());
        assertEquals(2, result.items().size());
        assertEquals("신발", result.items().get(0).productName());
        assertEquals("양말", result.items().get(1).productName());
    }

    @Test
    void 판매자_주문상세_조회_주문없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long orderId = 999L;

        given(orderItemRepository.findAllBySellerIdAndOrderId(sellerId, orderId))
                .willReturn(List.of());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> sellerOrderService.findOne(sellerId, orderId)
        );
        assertEquals(ErrorEnum.ORDER_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 판매자_주문상세_조회_다른판매자_주문_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long otherSellerOrderId = 99L;

        given(orderItemRepository.findAllBySellerIdAndOrderId(sellerId, otherSellerOrderId))
                .willReturn(List.of());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> sellerOrderService.findOne(sellerId, otherSellerOrderId)
        );
        assertEquals(ErrorEnum.ORDER_NOT_FOUND, exception.getErrorEnum());
    }
}
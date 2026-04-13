package com.example.allinmarket.seller.orderitem.service;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.orderitem.dto.OrderItemDetailResponse;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SellerOrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private SellerOrderItemService sellerOrderItemService;

    private OrderItem createOrderItemMock(Long itemId, Long orderId, String productName, BigDecimal price, int quantity) {
        Order order = mock(Order.class);
        given(order.getId()).willReturn(orderId);

        Product product = mock(Product.class);
        given(product.getId()).willReturn(itemId);

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(1L);

        OrderItem orderItem = mock(OrderItem.class);
        given(orderItem.getId()).willReturn(itemId);
        given(orderItem.getOrder()).willReturn(order);
        given(orderItem.getProduct()).willReturn(product);
        given(orderItem.getSeller()).willReturn(seller);
        given(orderItem.getProductName()).willReturn(productName);
        given(orderItem.getUnitPrice()).willReturn(price);
        given(orderItem.getQuantity()).willReturn(quantity);
        return orderItem;
    }

    @Test
    void 판매자_주문상품목록_조회_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        OrderItem orderItem = createOrderItemMock(1L, 10L, "신발", BigDecimal.valueOf(30000), 1);
        Page<OrderItem> page = new PageImpl<>(List.of(orderItem), pageable, 1);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(page);

        // when
        PageResponse<OrderItemDetailResponse> result = sellerOrderItemService.findAll(sellerId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(1L, result.content().get(0).id());
        assertEquals(10L, result.content().get(0).orderId());
        assertEquals("신발", result.content().get(0).productName());
    }

    @Test
    void 판매자_주문상품목록_조회_여러상품_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        OrderItem item1 = createOrderItemMock(1L, 10L, "신발", BigDecimal.valueOf(30000), 1);
        OrderItem item2 = createOrderItemMock(2L, 10L, "양말", BigDecimal.valueOf(20000), 2);
        OrderItem item3 = createOrderItemMock(3L, 11L, "모자", BigDecimal.valueOf(15000), 1);
        Page<OrderItem> page = new PageImpl<>(List.of(item1, item2, item3), pageable, 3);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(page);

        // when
        PageResponse<OrderItemDetailResponse> result = sellerOrderItemService.findAll(sellerId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(3, result.content().size());
    }

    @Test
    void 판매자_주문상품목록_조회_빈목록_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<OrderItem> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(emptyPage);

        // when
        PageResponse<OrderItemDetailResponse> result = sellerOrderItemService.findAll(sellerId, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
        assertTrue(result.isLast());
    }

    @Test
    void 판매자_주문상품목록_조회_페이징_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 2);

        OrderItem item1 = createOrderItemMock(1L, 10L, "상품1", BigDecimal.valueOf(10000), 1);
        OrderItem item2 = createOrderItemMock(2L, 10L, "상품2", BigDecimal.valueOf(20000), 1);
        Page<OrderItem> page = new PageImpl<>(List.of(item1, item2), pageable, 3);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(page);

        // when
        PageResponse<OrderItemDetailResponse> result = sellerOrderItemService.findAll(sellerId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(2, result.content().size());
        assertEquals(2, result.totalPages());
        assertFalse(result.isLast());
    }

    @Test
    void 판매자_주문상품목록_조회_두번째_페이지_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(1, 2);

        OrderItem item3 = createOrderItemMock(3L, 11L, "상품3", BigDecimal.valueOf(30000), 1);
        Page<OrderItem> page = new PageImpl<>(List.of(item3), pageable, 3);

        given(orderItemRepository.findAllBySellerId(sellerId, pageable)).willReturn(page);

        // when
        PageResponse<OrderItemDetailResponse> result = sellerOrderItemService.findAll(sellerId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(2, result.currentPage());
        assertTrue(result.isLast());
    }
}
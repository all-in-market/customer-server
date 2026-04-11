package com.example.allinmarket.buyer.order.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.buyer.order.dto.request.OrderCreateRequest;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.buyer.order.service.BuyerOrderService;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class BuyerOrderController {

    private BuyerOrderService buyerOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDetailResponse>> createOrder(
            @RequestBody @Valid OrderCreateRequest request
            ) {
        OrderDetailResponse result = buyerOrderService.createOrder(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        result
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderDetailResponse>>> findAllOrders(
            Pageable pageable,
            @RequestParam(required = false) OrderStatus status

    ) {
        PageResponse<OrderDetailResponse> result = buyerOrderService.findAllOrders(SecurityUtils.getCurrentUserId(), pageable, status);
        return ResponseEntity.ok(
                ApiResponse.success(
                SuccessEnum.READ_SUCCESS,
                result
        ));
    }

}

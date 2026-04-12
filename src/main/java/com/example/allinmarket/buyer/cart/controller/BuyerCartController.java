package com.example.allinmarket.buyer.cart.controller;

import com.example.allinmarket.buyer.cart.dto.request.AddProductToCartRequest;
import com.example.allinmarket.buyer.cart.dto.response.CartDetailResponse;
import com.example.allinmarket.buyer.cart.service.BuyerCartService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
public class BuyerCartController {
    private final BuyerCartService buyerCartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDetailResponse>> addProductToCart(AddProductToCartRequest request, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        buyerCartService.addProductToCart(SecurityUtils.getCurrentUserId(), request, pageable)
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartDetailResponse>> getCart(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.READ_SUCCESS, buyerCartService.getCart(SecurityUtils.getCurrentUserId(), pageable))
        );
    }
}

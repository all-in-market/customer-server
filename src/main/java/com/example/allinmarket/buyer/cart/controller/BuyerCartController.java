package com.example.allinmarket.buyer.cart.controller;

import com.example.allinmarket.buyer.cart.dto.CartDetailResponse;
import com.example.allinmarket.buyer.cart.service.BuyerCartService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
public class BuyerCartController {
    private final BuyerCartService buyerCartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartDetailResponse>> getCart(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.READ_SUCCESS, buyerCartService.getCart(SecurityUtils.getCurrentUserId(), pageable))
        );
    }
}

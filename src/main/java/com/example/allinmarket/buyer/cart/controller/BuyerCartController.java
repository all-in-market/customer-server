package com.example.allinmarket.buyer.cart.controller;

import com.example.allinmarket.buyer.cart.dto.request.AddProductToCartRequest;
import com.example.allinmarket.buyer.cart.dto.request.UpdateCartItemQuantityRequest;
import com.example.allinmarket.buyer.cart.dto.response.CartDetailResponse;
import com.example.allinmarket.buyer.cart.service.BuyerCartService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/carts")
public class BuyerCartController {
    private final BuyerCartService buyerCartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDetailResponse>> addProductToCart(
            @Valid @RequestBody AddProductToCartRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        buyerCartService.addProductToCart(SecurityUtils.getCurrentUserId(), request, pageable)
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartDetailResponse>> getCart(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.READ_SUCCESS, buyerCartService.getCart(SecurityUtils.getCurrentUserId(), pageable))
        );
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartDetailResponse>> updateCartItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.UPDATE_SUCCESS,
                buyerCartService.updateCartItemQuantity(SecurityUtils.getCurrentUserId(), productId, request, pageable)
                )
        );
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<CartDetailResponse>> removeCartItem(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.DELETE_SUCCESS,
                buyerCartService.removeCartItem(SecurityUtils.getCurrentUserId(), productId, pageable)
                )
        );
    }
}

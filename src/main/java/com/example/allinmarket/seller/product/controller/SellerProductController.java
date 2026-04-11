package com.example.allinmarket.seller.product.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
import com.example.allinmarket.seller.product.service.SellerProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/products")
public class SellerProductController {
    private final SellerProductService sellerProductService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> create(@Valid @RequestBody SellerProductCreateRequest request) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessEnum.CREATE_SUCCESS, sellerProductService.create(sellerId, request)));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> update(@PathVariable(name = "productId") Long productId, @Valid @RequestBody SellerProductUpdateRequest request) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, sellerProductService.update(sellerId, productId, request)));
    }

//    @DeleteMapping("/{productId}")
//    public ResponseEntity<ApiResponse<ProductDetailResponse>> delete(@PathVariable(name = "productId") Long productId) {
//        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.DELETE_SUCCESS, sellerProductService.delete(productId)));
//    }
//
//    @PutMapping("/{productId}/stock")
//    public ResponseEntity<ApiResponse<ProductDetailResponse>> stockUpdate(@PathVariable(name = "productId") Long productId, @Valid @RequestBody SellerProductStockUpdateRequest request) {
//        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, sellerProductService.stockUpdate(productId, request)));
//    }
}

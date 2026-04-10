package com.example.allinmarket.seller.product.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.seller.product.service.SellerProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seller/products")
public class SellerProductController {
    private final SellerProductService sellerProductService;

    @PostMapping
    public ResponseEntity<ApiResponse<SellerProductResponse>> create(@Valid @RequestBody SellerProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(SuccessEnum.CREATE_SUCCESS, sellerProductService.create(request)));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> update(@PathVariable(name = "productId") Long productId, @Valid @RequestBody SellerProductUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, sellerProductService.update(productId, request)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> delete(@PathVariable(name = "productId") Long productId) {
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.DELETE_SUCCESS, sellerProductService.delete(productId)));
    }

    @PutMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<SellerProductResponse>> stockUpdate(@PathVariable(name = "productId") Long productId, @Valid @RequestBody SellerProductStockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, sellerProductService.stockUpdate(productId, request)));
    }
}

package com.example.allinmarket.seller.product.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductStockUpdateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
import com.example.allinmarket.seller.product.service.SellerProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductDetailResponse>>> findAll(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.READ_SUCCESS, sellerProductService.findAll(sellerId, pageable)));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> update(@PathVariable(name = "productId") Long productId, @Valid @RequestBody SellerProductUpdateRequest request) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, sellerProductService.update(sellerId, productId, request)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> delete(@PathVariable(name = "productId") Long productId) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.DELETE_SUCCESS, sellerProductService.delete(sellerId, productId)));
    }

    @PutMapping("/{productId}/stock")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> stockUpdate(@PathVariable(name = "productId") Long productId, @Valid @RequestBody SellerProductStockUpdateRequest request) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, sellerProductService.stockUpdate(sellerId, productId, request)));
    }
}

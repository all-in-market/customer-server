package com.example.allinmarket.buyer.product.controller;

import com.example.allinmarket.buyer.product.service.BuyerProductService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class BuyerProductController {
    private final BuyerProductService buyerProductService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductDetailResponse>>> findAllProduct(Pageable pageable) {
        Page<ProductDetailResponse> productPage = buyerProductService.findAllProducts(pageable);

        PageResponse<ProductDetailResponse> pageResponse = PageResponse.register(productPage);

        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.READ_SUCCESS, pageResponse));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> findOneProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.READ_SUCCESS, buyerProductService.findOneProduct(productId)));
    }
}

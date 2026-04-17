package com.example.allinmarket.buyer.category.controller;

import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.buyer.category.service.BuyerCategoryService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class BuyerCategoryController {
    private final BuyerCategoryService buyerCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDetailResponse>>> findAllCategory() {

        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.READ_SUCCESS, buyerCategoryService.findAllCategory()));
    }
}

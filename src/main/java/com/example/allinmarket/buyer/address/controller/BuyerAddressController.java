package com.example.allinmarket.buyer.address.controller;

import com.example.allinmarket.buyer.address.dto.request.AddressCreateRequest;
import com.example.allinmarket.buyer.address.dto.response.AddressDetailResponse;
import com.example.allinmarket.buyer.address.service.BuyerAddressService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/addresses")
public class BuyerAddressController {

    private final BuyerAddressService buyerAddressService;

    @GetMapping
    public ResponseEntity<ApiResponse<AddressDetailResponse>> createAddress(
            @RequestBody @Valid AddressCreateRequest request
    ) {
        AddressDetailResponse result = buyerAddressService.createAddress(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        result
                ));
    }
}

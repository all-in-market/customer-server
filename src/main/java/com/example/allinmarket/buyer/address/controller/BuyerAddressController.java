package com.example.allinmarket.buyer.address.controller;

import com.example.allinmarket.buyer.address.dto.request.AddressCreateRequest;
import com.example.allinmarket.buyer.address.dto.request.AddressUpdateRequest;
import com.example.allinmarket.buyer.address.dto.response.AddressDetailResponse;
import com.example.allinmarket.buyer.address.service.BuyerAddressService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/addresses")
public class BuyerAddressController {

    private final BuyerAddressService buyerAddressService;

    /**
     * 구매자 배송지 추가
     */
    @PostMapping
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

    /**
     * 구매자 본인 배송지 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressDetailResponse>>> getAllAddresses() {
        List<AddressDetailResponse> result = buyerAddressService.getAllAddresses(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        result
                ));
    }

    /**
     * 배송지 수정
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressDetailResponse>> updateAddress(
            @RequestBody @Valid AddressUpdateRequest request,
            @PathVariable Long addressId
    ) {
        AddressDetailResponse result = buyerAddressService.updateAddress(SecurityUtils.getCurrentUserId(), addressId, request);
        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.UPDATE_SUCCESS,
                        result
                ));
    }

    /**
     *  배송지 삭제
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<ApiResponse<AddressDetailResponse>> removeAddress(
            @PathVariable Long addressId
    ) {
        AddressDetailResponse result = buyerAddressService.removeAddress(SecurityUtils.getCurrentUserId(), addressId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.DELETE_SUCCESS,
                        result
                ));
    }

}

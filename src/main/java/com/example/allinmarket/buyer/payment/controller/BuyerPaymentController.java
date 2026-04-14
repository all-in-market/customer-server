package com.example.allinmarket.buyer.payment.controller;

import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.payment.facade.BuyerPaymentFacade;
import com.example.allinmarket.buyer.payment.service.BuyerPaymentService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class BuyerPaymentController {

    private final BuyerPaymentFacade buyerPaymentFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> processPayment(
            @RequestBody @Valid PaymentCreateRequest request
    ) {
        PaymentDetailResponse result = buyerPaymentFacade.processPayment(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        result
                )
        );
    }
}

package com.example.allinmarket.buyer.payment.controller;

import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.payment.facade.BuyerPaymentFacade;
import com.example.allinmarket.buyer.payment.service.BuyerPaymentService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
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
@RequestMapping("/payments")
public class BuyerPaymentController {

    private final BuyerPaymentFacade buyerPaymentFacade;
    private final BuyerPaymentService buyerPaymentService;

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

    /**
     * 결제 단건 상세 조회
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> findPayment(
            @PathVariable Long paymentId
    ) {
        PaymentDetailResponse result = buyerPaymentService.findPayment(SecurityUtils.getCurrentUserId(), paymentId);
        return ResponseEntity.ok(
                ApiResponse.success(
                        SuccessEnum.READ_SUCCESS,
                        result
                ));
    }
}

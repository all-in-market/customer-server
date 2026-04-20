package com.example.allinmarket.buyer.payment.service;

import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.refund.service.BuyerRefundService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.transactionhistory.service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BuyerPaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final BuyerRefundService buyerRefundService;
    private final TransactionHistoryService transactionHistoryService;
    private final PaymentStateService paymentStateService;

    /**
     * 결제 생성 및 DB 저장
     */
    @Transactional
    public PaymentDetailResponse createPayment(Long currentUserId, PaymentCreateRequest request) {
        Order order = orderRepository.findByIdAndBuyerIdWithLock(request.orderId(), currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.ORDER_NOT_FOUND)
        );

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BaseException(ErrorEnum.ORDER_NOT_PAYABLE);
        }

        boolean exists = paymentRepository.existsByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING);

        if (exists) {
            throw new BaseException(ErrorEnum.PAYMENT_ALREADY_EXISTS);
        }

        String impUid = createPaymentId(request.orderId());

        Payment payment = Payment.of(
                order,
                impUid,
                order.getTotalAmount(),
                request.method()
        );

        paymentRepository.save(payment);
        log.info("결제 생성 성공: paymentId = {}", payment.getId());

        // transaction_histories 이력 추가
        try {
            transactionHistoryService.savePaymentHistory(payment);
            log.info("결제 생성 이력 저장 성공: paymentId = {}", payment.getId());
        } catch (Exception e) {
            log.error("결제 생성 이력 저장 실패: paymentId = {}, reason = {}", payment.getId(), e.getMessage());
        }

        return PaymentDetailResponse.from(payment);
    }

    /**
     * 결제 확인 및 상태 업데이트
     */
    @Transactional
    public PaymentDetailResponse confirmPayment(Long currentUserId, String paymentId, PortOnePaymentResponse payment) {

        Payment dbPayment = paymentRepository.findByImpUidWithOrder(paymentId).orElseThrow(
                () -> new BaseException(ErrorEnum.PAYMENT_NOT_FOUND)
        );

        validatePaymentOwner(currentUserId, dbPayment);
        validatePaymentIdMatch(paymentId, payment);

        // 멱등성 검사
        PaymentDetailResponse idempotentSuccessResponse = handleAlreadySucceededPayment(dbPayment);
        if (idempotentSuccessResponse != null) {
            return idempotentSuccessResponse;
        }

        validatePaymentNonProcessableStatus(dbPayment);

        /**
         * PG 응답을 통해 실제 결제가 성공했는지 검사
         * PG 결과 검증 실패 -> REQUIRES_NEW로 fail + 이력 저장 후 예외 전파
         */
        if (!payment.isPaid()) {
            paymentStateService.failAndSaveHistory(dbPayment);
            throw new BaseException(ErrorEnum.PAYMENT_NOT_COMPLETED);
        }

        /**
         * 포트원 결제 금액이 유효한지 확인
         */
        if (payment.getTotalAmount() == null) {
            paymentStateService.failAndSaveHistory(dbPayment);
            throw new BaseException(ErrorEnum.PAYMENT_AMOUNT_INVALID);
        }
        /**
         * 주문 금액과 실제 결제 금액이 일치하는지 확인
         * 금액 불일치 -> REQUIRES_NEW로 fail + 이력 저장 + 환불 생성 후 예외 전파
         */
        if (dbPayment.getAmount().compareTo(payment.getTotalAmount()) != 0) {
            paymentStateService.failAndSaveHistory(dbPayment);
            buyerRefundService.createRefundForAmountMismatch(currentUserId, dbPayment, payment);
            throw new BaseException(ErrorEnum.PAYMENT_AMOUNT_MISMATCH);
        }

        dbPayment.success(LocalDateTime.now());
        dbPayment.getOrder().paid();

        // flush를 commit 전에 발생하도록 하여 OptimisticLockingFailureException이 메서드 안에서 발생
        paymentRepository.saveAndFlush(dbPayment);

        // todo: seller_dashboard 업데이트

        log.info("결제 승인 성공: paymentId = {}", dbPayment.getId());

        try {
            transactionHistoryService.savePaymentHistory(dbPayment);
            log.info("결제 승인 이력 저장 성공: paymentId = {}", dbPayment.getId());
        } catch (Exception e) {
            log.error("결제 승인 이력 저장 실패: paymentId = {}, reason = {}", dbPayment.getId(), e.getMessage());
        }

        return PaymentDetailResponse.from(dbPayment);
    }

    /**
     * 결제 목록 페이지로 조회
     */
    public PageResponse<PaymentDetailResponse> getPayments(Long buyerId, Pageable pageable) {
        return PageResponse.register(
                paymentRepository.findAllByOrderBuyerId(buyerId, pageable)
                        .map(PaymentDetailResponse::from)
        );
    }

    /**
     * 결제 단건 조회
     */
    public PaymentDetailResponse findPayment(Long currentUserId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndOrderBuyerId(paymentId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.PAYMENT_NOT_FOUND)
        );

        return PaymentDetailResponse.from(payment);
    }

    /**
     * 결제 확인 요청을 보낸 주체가 해당 결제의 주인이 맞는지 검증
     */
    private void validatePaymentOwner(Long currentUserId, Payment dbPayment) {
        if (!dbPayment.getOrder().getBuyer().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.PAYMENT_FORBIDDEN);
        }
    }

    /**
     * 요청한 결제 ID와 PG 응답의 결제 ID가 일치하는지 검증
     */
    private void validatePaymentIdMatch(String paymentId, PortOnePaymentResponse payment) {
        if (!paymentId.equals(payment.getPaymentId())) {
            throw new BaseException(ErrorEnum.PAYMENT_MISMATCH);
        }
    }

    /**
     * 이미 성공 처리된 결제인지 멱등성 검사 (동일 결제 중복 처리 방지)
     * 이미 성공한 결제인 경우 예외 대신 성공 응답메세지 전송
     */
    private PaymentDetailResponse handleAlreadySucceededPayment(Payment dbPayment) {
        if (dbPayment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentDetailResponse.from(dbPayment);
        }

        return null;
    }

    /**
     * 결제 진행이 불가능한 상태인지 멱등성 검사 (동일 결제 중복 처리 방지)
     */
    private void validatePaymentNonProcessableStatus(Payment dbPayment) {
        if (dbPayment.getStatus() == PaymentStatus.FAILED) {
            throw new BaseException(ErrorEnum.PAYMENT_ALREADY_FAILED);
        }

        if (dbPayment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BaseException(ErrorEnum.PAYMENT_ALREADY_REFUNDED);
        }
    }

    /**
     * 결제ID 생성
     */
    private String createPaymentId(Long orderId) {
        return "payment_" + orderId + "_" + UUID.randomUUID();
    }
}

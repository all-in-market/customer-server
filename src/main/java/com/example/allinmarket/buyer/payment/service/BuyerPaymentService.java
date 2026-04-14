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
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerPaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    private final BuyerRefundService buyerRefundService;

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

        boolean exists = paymentRepository.existsByOrderIdAndStatus(order.getId(), TransactionStatus.PENDING);

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

        // todo: transaction_histories 업데이트

        return PaymentDetailResponse.from(payment);
    }

    /**
     * 결제 확인 및 상태 업데이트
     */
    @Transactional
    public PaymentDetailResponse confirmPayment(Long currentUserId, String paymentId, PortOnePaymentResponse payment) {

        // 비관적 락 적용
        Payment dbPayment = paymentRepository.findByImpUidWithOrderForUpdate(paymentId).orElseThrow(
                () -> new BaseException(ErrorEnum.PAYMENT_NOT_FOUND)
        );

        validatePaymentOwner(currentUserId, dbPayment);
        validatePaymentIdMatch(paymentId, payment);

        PaymentDetailResponse idempotentSuccessResponse = handleAlreadySucceededPayment(dbPayment);

        if (idempotentSuccessResponse != null) {
            return idempotentSuccessResponse;
        }

        validatePaymentNonProcessableStatus(dbPayment);

        validatePaymentResult(payment, dbPayment);
        validatePaymentAmount(currentUserId, payment, dbPayment);

        dbPayment.success(LocalDateTime.now());
        dbPayment.getOrder().paid();

        // todo: seller_dashboard 업데이트
        // todo: transaction_histories 업데이트

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
        if (dbPayment.getStatus() == TransactionStatus.SUCCESS) {
            return PaymentDetailResponse.from(dbPayment);
        }

        return null;
    }

    /**
     * 결제 진행이 불가능한 상태인지 멱등성 검사 (동일 결제 중복 처리 방지)
     */
    private void validatePaymentNonProcessableStatus(Payment dbPayment) {
        if (dbPayment.getStatus() == TransactionStatus.FAILED) {
            throw new BaseException(ErrorEnum.PAYMENT_ALREADY_FAILED);
        }

        if (dbPayment.getStatus() == TransactionStatus.REFUNDED) {
            throw new BaseException(ErrorEnum.PAYMENT_ALREADY_REFUNDED);
        }
    }

    /**
     * PG 응답을 통해 실제 결제가 성공했는지 검사
     */
    private void validatePaymentResult(PortOnePaymentResponse payment, Payment dbPayment) {

        if (!payment.isPaid()) {
            dbPayment.fail();

            // todo: transaction_histories 업데이트

            throw new BaseException(ErrorEnum.PAYMENT_NOT_COMPLETED);
        }
    }

    /**
     * 주문 금액과 실제 결제 금액이 일치하는지 확인
     * 상이할 경우 결제를 실패 처리하고 환불 대상으로 남김
     */
    private void validatePaymentAmount(Long currentUerId, PortOnePaymentResponse payment, Payment dbPayment) {

        // 주문 금액과 실결제 금액이 다를 때
        if (payment.getTotalAmount() == null) {
            dbPayment.fail();

            // todo: transaction_histories 업데이트

            throw new BaseException(ErrorEnum.PAYMENT_AMOUNT_INVALID);
        }

        if (dbPayment.getAmount().compareTo(payment.getTotalAmount()) != 0) {

            // 환불 로직 발생 시 먼저 fail 처리 후
            // 관리자 서버에서 환불이 진행되면 refunded 처리
            dbPayment.fail();

            buyerRefundService.createRefundForAmountMismatch(currentUerId, dbPayment, payment);
            // todo: transaction_histories 업데이트

            throw new BaseException(ErrorEnum.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    /**
     * 결제ID 생성
     */
    private String createPaymentId(Long orderId) {
        return "payment_" + orderId + "_" + UUID.randomUUID();
    }
}

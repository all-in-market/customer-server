package com.example.allinmarket.buyer.refund.service;

import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.refund.dto.request.RefundCreateRequest;
import com.example.allinmarket.buyer.refund.dto.response.RefundDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerRefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public RefundDetailResponse createRefundByOrder(Long currentUserId, Long orderId, RefundCreateRequest request) {

        Order order = orderRepository.findByIdAndBuyerIdWithBuyer(orderId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.ORDER_NOT_FOUND)
        );

        // 환불 가능한 상태의 주문인지 검증
        validateOrderRefundable(order);

        // 해당 주문과 연결된 결제 중 환불 대상 결제 조회
        Payment payment = paymentRepository.findByOrderIdAndStatusForUpdate(orderId, PaymentStatus.SUCCESS).orElseThrow(
                () -> new BaseException(ErrorEnum.PAYMENT_NOT_FOUND)
        );

        // 중복환불 신청에 대한 멱등성 검사 (이미 진행중인 환불이 있는지)
        Refund existingRefund = handleExistingRefund(payment);

        // failed 상태의 환불이 있을 경우 dto 반환
        if (existingRefund != null) {
            existingRefund.updateReason(request.reason());
            existingRefund.updateDescription(request.description());
            return RefundDetailResponse.from(existingRefund);
        }

        // 환불 객체 생성 및 저장
        Refund refund = Refund.of(
                order.getBuyer(),
                payment,
                request.reason(),
                request.description()
        );

        refundRepository.save(refund);

        return RefundDetailResponse.from(refund);
    }

    /**
     * 주문 금액 != 실결제 금액 시, 환불 처리
     */
    @Transactional
    public void createRefundForAmountMismatch(Long currentUserId, Payment payment, PortOnePaymentResponse paymentResponse) {

        // 전달받은 payment의 소유자와 현재 사용자가 일치하는지 검증
        if (!payment.getOrder().getBuyer().getId().equals(currentUserId)) {
            throw new BaseException(ErrorEnum.REFUND_FORBIDDEN);
        }

        // 연결된 주문의 상태가 환불 생성 가능한 상태인지 검증
        Order order = payment.getOrder();
        validateOrderRefundableForAmountMismatch(order);

        // 전달받은 payment가 환불 생성 가능한 결제 상태인지 검증
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.FAILED) {
            throw new BaseException(ErrorEnum.PAYMENT_NOT_REFUNDABLE);
        }

        if (paymentResponse == null || paymentResponse.getTotalAmount() == null) {
            throw new BaseException(ErrorEnum.PAYMENT_AMOUNT_INVALID);
        }

        if(payment.getAmount().compareTo(paymentResponse.getTotalAmount()) == 0) {
            throw new BaseException(ErrorEnum.REFUND_AMOUNT_MISMATCH_NOT_FOUND);
        }

        // 해당 payment 기준으로 이미 진행 중이거나 완료된 환불이 있는지 멱등성 검사
        Refund existingRefund = handleExistingRefund(payment);

        if (existingRefund != null) {
            existingRefund.updateReason(ReasonEnum.PAYMENT_AMOUNT_MISMATCH);
            existingRefund.updateDescription(ReasonEnum.PAYMENT_AMOUNT_MISMATCH.getReason());
            return;
        }

        // 환불 객체 생성 및 저장
        Refund refund = Refund.of(
                order.getBuyer(),
                payment,
                ReasonEnum.PAYMENT_AMOUNT_MISMATCH,
                ReasonEnum.PAYMENT_AMOUNT_MISMATCH.getReason()
        );

        refundRepository.save(refund);

        // todo: refund_histories 또는 transaction_histories 업데이트
    }

    public PageResponse<RefundDetailResponse> getRefunds(Long buyerId, Pageable pageable) {
        return PageResponse.register(
                refundRepository.findAllByBuyerId(buyerId, pageable)
                        .map(RefundDetailResponse::from)
        );
    }

    /**
     * 환불 단건 조회
     */
    public RefundDetailResponse getRefund(Long currentUserId, Long refundId) {
        Refund refund = refundRepository.findByIdAndBuyerId(refundId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
        );

        return RefundDetailResponse.from(refund);
    }


    /**
     * 주문이 환불 가능한 상태인지 검증
     */
    private void validateOrderRefundable(Order order) {
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.DELIVERED) {
            throw new BaseException(ErrorEnum.ORDER_NOT_REFUNDABLE);
        }
    }

    /**
     * 주문 금액과 실결제 금액 불일치 환불은 pending 상태일때 검증 통과
     */
    private void validateOrderRefundableForAmountMismatch(Order order) {
        if (order.getStatus() != OrderStatus.CREATED
                && order.getStatus() != OrderStatus.PAID
                && order.getStatus() != OrderStatus.DELIVERED) {
            throw new BaseException(ErrorEnum.ORDER_NOT_REFUNDABLE);
        }
    }

    /**
     * 해당 결제에 대해 이미 환불이 존재하는지 확인
     * 존재할 경우에 해당 환불이 failed 상태이면 이를 pending으로 변경 후 바로 응답 반환
     * 그 외 상태(PENDING, SUCCESS, DENIED)는 중복 환불로 간주하여 예외 발생
     */
    private Refund handleExistingRefund(Payment payment) {
        Optional<Refund> refundOptional = refundRepository.findByPayment(payment);

        if (refundOptional.isEmpty()) {
            return null;
        }

        Refund existingRefund = refundOptional.get();

        switch (existingRefund.getStatus()) {
            case PENDING, SUCCESS, DENIED -> throw new BaseException(ErrorEnum.REFUND_ALREADY_EXISTS);
            case FAILED -> {
                existingRefund.pending();
                return existingRefund;
            }
        }

        return null;
    }
}

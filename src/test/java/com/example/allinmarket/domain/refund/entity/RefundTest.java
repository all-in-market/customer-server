package com.example.allinmarket.domain.refund.entity;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.refund.dto.request.RefundCreateRequest;
import com.example.allinmarket.buyer.refund.dto.response.RefundDetailResponse;
import com.example.allinmarket.buyer.refund.service.BuyerRefundService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.outbox.service.HistoryOutboxService;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundTest {

    @InjectMocks
    private BuyerRefundService buyerRefundService;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private HistoryOutboxService historyOutBoxService;

    private Buyer createBuyer(Long id) {
        Buyer buyer = Buyer.of(
                "test@test.com",
                "encodedPassword",
                "홍길동",
                "010-1111-2222"
        );
        setField(buyer, "id", id);
        return buyer;
    }

    private Order createOrder(Long id, Buyer buyer, BigDecimal totalAmount) {
        Order order = Order.of(
                buyer,
                totalAmount,
                null,
                "홍길동",
                "010-1111-2222",
                "서울시 강남구"
        );
        setField(order, "id", id);
        return order;
    }

    private Payment createPayment(Order order, String impUid, BigDecimal amount, MethodEnum method) {
        return Payment.of(order, impUid, amount, method);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    @Nested
    @DisplayName("주문 기준 환불 생성")
    class CreateRefundByOrderTest {

        @Test
        @DisplayName("환불 생성 성공")
        void createRefundByOrder_success() {
            // given
            Long currentUserId = 1L;
            Long orderId = 10L;

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(orderId, buyer, new BigDecimal("15000"));
            setField(order, "status", OrderStatus.PAID);

            Payment payment = createPayment(order, "payment_10_abc", new BigDecimal("15000"), MethodEnum.MOCK);
            payment.success(LocalDateTime.now());

            RefundCreateRequest request =
                    new RefundCreateRequest(ReasonEnum.CHANGE_OF_MIND, "단순 변심");

            given(orderRepository.findByIdAndBuyerIdWithBuyer(orderId, currentUserId))
                    .willReturn(Optional.of(order));
            given(paymentRepository.findByOrderIdAndStatusForUpdate(orderId, PaymentStatus.SUCCESS))
                    .willReturn(Optional.of(payment));
            given(refundRepository.findByPayment(payment))
                    .willReturn(Optional.empty());

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);

            // when
            RefundDetailResponse response =
                    buyerRefundService.createRefundByOrder(currentUserId, orderId, request);

            // then
            verify(refundRepository).save(captor.capture());
            Refund savedRefund = captor.getValue();

            assertThat(savedRefund.getBuyer()).isSameAs(buyer);
            assertThat(savedRefund.getPayment()).isSameAs(payment);
            assertThat(savedRefund.getReason()).isEqualTo(ReasonEnum.CHANGE_OF_MIND);
            assertThat(savedRefund.getDescription()).isEqualTo("단순 변심");
            assertThat(savedRefund.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(savedRefund.getProcessedAt()).isNull();

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("주문 상태가 환불 불가면 예외 발생")
        void createRefundByOrder_fail_orderNotRefundable() {
            // given
            Long currentUserId = 1L;
            Long orderId = 10L;

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(orderId, buyer, new BigDecimal("15000"));
            // CREATED 상태 유지

            RefundCreateRequest request =
                    new RefundCreateRequest(ReasonEnum.CHANGE_OF_MIND, "단순 변심");

            given(orderRepository.findByIdAndBuyerIdWithBuyer(orderId, currentUserId))
                    .willReturn(Optional.of(order));

            // when
            BaseException ex = assertThrows(
                    BaseException.class,
                    () -> buyerRefundService.createRefundByOrder(currentUserId, orderId, request)
            );

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.ORDER_NOT_REFUNDABLE);
            verify(paymentRepository, never()).findByOrderIdAndStatusForUpdate(anyLong(), any());
        }

        @Test
        @DisplayName("기존 failed 환불이 있으면 pending으로 복구하고 사유를 수정한다")
        void createRefundByOrder_failedRefund_reuse() {
            // given
            Long currentUserId = 1L;
            Long orderId = 10L;

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(orderId, buyer, new BigDecimal("15000"));
            setField(order, "status", OrderStatus.PAID);

            Payment payment = createPayment(order, "payment_10_abc", new BigDecimal("15000"), MethodEnum.MOCK);
            payment.success(LocalDateTime.now());

            Refund existingRefund = Refund.of(
                    buyer,
                    payment,
                    ReasonEnum.CHANGE_OF_MIND,
                    "기존 설명"
            );
            setField(existingRefund, "status", RefundStatus.FAILED);

            RefundCreateRequest request =
                    new RefundCreateRequest(ReasonEnum.DAMAGED, "파손");

            given(orderRepository.findByIdAndBuyerIdWithBuyer(orderId, currentUserId))
                    .willReturn(Optional.of(order));
            given(paymentRepository.findByOrderIdAndStatusForUpdate(orderId, PaymentStatus.SUCCESS))
                    .willReturn(Optional.of(payment));
            given(refundRepository.findByPayment(payment))
                    .willReturn(Optional.of(existingRefund));

            // when
            RefundDetailResponse response =
                    buyerRefundService.createRefundByOrder(currentUserId, orderId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(existingRefund.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(existingRefund.getReason()).isEqualTo(ReasonEnum.DAMAGED);
            assertThat(existingRefund.getDescription()).isEqualTo("파손");
            verify(refundRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("금액 불일치 환불 생성")
    class CreateRefundForAmountMismatchTest {

        @Test
        @DisplayName("금액 불일치 환불 생성 성공")
        void createRefundForAmountMismatch_success() {
            // given
            Long currentUserId = 1L;

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            // 금액 불일치 환불은 CREATED도 허용
            setField(order, "status", OrderStatus.CREATED);

            Payment payment = createPayment(order, "payment_10_abc", new BigDecimal("15000"), MethodEnum.MOCK);
            payment.fail();

            PortOnePaymentResponse paymentResponse = mock(PortOnePaymentResponse.class);
            given(paymentResponse.getTotalAmount()).willReturn(new BigDecimal("10000"));

            given(refundRepository.findByPayment(payment))
                    .willReturn(Optional.empty());

            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);

            // when
            buyerRefundService.createRefundForAmountMismatch(currentUserId, payment, paymentResponse);

            // then
            verify(refundRepository).save(captor.capture());
            Refund savedRefund = captor.getValue();

            assertThat(savedRefund.getBuyer()).isSameAs(buyer);
            assertThat(savedRefund.getPayment()).isSameAs(payment);
            assertThat(savedRefund.getReason()).isEqualTo(ReasonEnum.PAYMENT_AMOUNT_MISMATCH);
            assertThat(savedRefund.getDescription())
                    .isEqualTo(ReasonEnum.PAYMENT_AMOUNT_MISMATCH.getReason());
            assertThat(savedRefund.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(savedRefund.getProcessedAt()).isNull();
        }

        @Test
        @DisplayName("응답 금액이 null이면 예외 발생")
        void createRefundForAmountMismatch_fail_amountInvalid() {
            // given
            Long currentUserId = 1L;

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            setField(order, "status", OrderStatus.CREATED);

            Payment payment = createPayment(order, "payment_10_abc", new BigDecimal("15000"), MethodEnum.MOCK);
            payment.fail();

            PortOnePaymentResponse paymentResponse = mock(PortOnePaymentResponse.class);
            given(paymentResponse.getTotalAmount()).willReturn(null);

            // when
            BaseException ex = assertThrows(
                    BaseException.class,
                    () -> buyerRefundService.createRefundForAmountMismatch(currentUserId, payment, paymentResponse)
            );

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_AMOUNT_INVALID);
            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("실결제 금액이 같으면 금액 불일치 환불 예외 발생")
        void createRefundForAmountMismatch_fail_amountMismatchNotFound() {
            // given
            Long currentUserId = 1L;

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            setField(order, "status", OrderStatus.CREATED);

            Payment payment = createPayment(order, "payment_10_abc", new BigDecimal("15000"), MethodEnum.MOCK);
            payment.fail();

            PortOnePaymentResponse paymentResponse = mock(PortOnePaymentResponse.class);
            given(paymentResponse.getTotalAmount()).willReturn(new BigDecimal("15000"));

            // when
            BaseException ex = assertThrows(
                    BaseException.class,
                    () -> buyerRefundService.createRefundForAmountMismatch(currentUserId, payment, paymentResponse)
            );

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.REFUND_AMOUNT_MISMATCH_NOT_FOUND);
            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("기존 failed 환불이 있으면 pending으로 복구하고 사유와 설명을 수정한다")
        void createRefundForAmountMismatch_failedRefund_reuse() {
            // given
            Long currentUserId = 1L;

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            setField(order, "status", OrderStatus.CREATED);

            Payment payment = createPayment(order, "payment_10_abc", new BigDecimal("15000"), MethodEnum.MOCK);
            payment.fail();

            PortOnePaymentResponse paymentResponse = mock(PortOnePaymentResponse.class);
            given(paymentResponse.getTotalAmount()).willReturn(new BigDecimal("10000"));

            Refund existingRefund = Refund.of(
                    buyer,
                    payment,
                    ReasonEnum.CHANGE_OF_MIND,
                    "기존 설명"
            );
            setField(existingRefund, "status", RefundStatus.FAILED);

            given(refundRepository.findByPayment(payment))
                    .willReturn(Optional.of(existingRefund));

            // when
            buyerRefundService.createRefundForAmountMismatch(currentUserId, payment, paymentResponse);

            // then
            assertThat(existingRefund.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(existingRefund.getReason()).isEqualTo(ReasonEnum.PAYMENT_AMOUNT_MISMATCH);
            assertThat(existingRefund.getDescription())
                    .isEqualTo(ReasonEnum.PAYMENT_AMOUNT_MISMATCH.getReason());
            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("결제 소유자가 다르면 예외 발생")
        void createRefundForAmountMismatch_fail_forbidden() {
            // given
            Long currentUserId = 1L;

            Buyer buyer = createBuyer(999L);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            setField(order, "status", OrderStatus.CREATED);

            Payment payment = createPayment(order, "payment_10_abc", new BigDecimal("15000"), MethodEnum.MOCK);
            payment.fail();

            PortOnePaymentResponse paymentResponse = mock(PortOnePaymentResponse.class);

            // when
            BaseException ex = assertThrows(
                    BaseException.class,
                    () -> buyerRefundService.createRefundForAmountMismatch(currentUserId, payment, paymentResponse)
            );

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.REFUND_FORBIDDEN);
            verify(refundRepository, never()).save(any());
        }
    }
}
package com.example.allinmarket.buyer.payment.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.order.service.StockReleaseService;
import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.refund.service.BuyerRefundService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.sellerdashboard.service.DashboardService;
import com.example.allinmarket.domain.transactionhistory.service.TransactionHistoryService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerPaymentServiceTest {

    @InjectMocks
    private BuyerPaymentService buyerPaymentService;

    private PaymentRetryService paymentRetryService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private BuyerRefundService buyerRefundService;

    @Mock
    private PaymentStateService paymentStateService;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private TransactionHistoryService transactionHistoryService;

    @Mock
    private StockReleaseService stockReleaseService;

    @BeforeEach
    void setUp() {
        paymentRetryService = new PaymentRetryService(buyerPaymentService, paymentRepository, paymentStateService);
    }

    private Buyer createBuyer(Long id) {
        Buyer buyer = Buyer.of("test@test.com", "encodedPassword", "홍길동", "010-1111-2222");
        setField(buyer, "id", id);
        return buyer;
    }

    private Order createOrder(Long id, Buyer buyer, BigDecimal totalAmount) {
        Order order = Order.of(buyer, totalAmount, null, "홍길동", "010-1111-2222", "서울시 강남구");
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
    @DisplayName("결제 생성")
    class CreatePayment {
        @Test
        @DisplayName("결제 생성 성공 - 실제 저장되는 Payment 값 검증")
        void createPayment_success() {
            // given
            Long currentUserId = 1L;
            Long orderId = 10L;

            Buyer buyer = Buyer.of("test@test.com", "encodedPassword", "홍길동", "010-1111-2222");
            setField(buyer, "id", currentUserId);

            Order order = Order.of(buyer, new BigDecimal("15000"), null, "홍길동", "010-1111-2222", "서울시 강남구");
            setField(order, "id", orderId);

            PaymentCreateRequest request = new PaymentCreateRequest(orderId, MethodEnum.MOCK);

            given(orderRepository.findByIdAndBuyerIdWithLock(orderId, currentUserId)).willReturn(Optional.of(order));
            given(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PENDING)).willReturn(false);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);

            // save가 들어오면 그대로 반환
            given(paymentRepository.save(any(Payment.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            PaymentDetailResponse response = buyerPaymentService.createPayment(currentUserId, request);

            // then
            verify(paymentRepository).save(captor.capture());
            Payment savedPayment = captor.getValue();

            assertThat(savedPayment).isNotNull();
            assertThat(savedPayment.getOrder()).isSameAs(order);
            assertThat(savedPayment.getAmount()).isEqualByComparingTo("15000");
            assertThat(savedPayment.getMethod()).isEqualTo(MethodEnum.MOCK);
            assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(savedPayment.getImpUid()).startsWith("payment_" + orderId + "_");

            assertThat(response).isNotNull();
            assertThat(response.impUid()).isEqualTo(savedPayment.getImpUid());
            assertThat(response.amount()).isEqualByComparingTo("15000");
            assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(response.method()).isEqualTo(MethodEnum.MOCK);
        }

        @Test
        @DisplayName("결제 생성 실패 - 주문 상태가 CREATED가 아니면 예외 발생")
        void createPayment_fail_orderNotPayable() {
            // given
            Long currentUserId = 1L;
            Long orderId = 10L;

            Buyer buyer = Buyer.of("test@test.com", "encodedPassword", "홍길동", "010-1111-2222");
            setField(buyer, "id", currentUserId);

            Order order = Order.of(buyer, new BigDecimal("15000"), null, "홍길동", "010-1111-2222", "서울시 강남구");
            setField(order, "id", orderId);
            setField(order, "status", OrderStatus.PAID);

            PaymentCreateRequest request = new PaymentCreateRequest(orderId, MethodEnum.MOCK);

            given(orderRepository.findByIdAndBuyerIdWithLock(orderId, currentUserId)).willReturn(Optional.of(order));

            // when
            BaseException ex = assertThrows(BaseException.class, () -> buyerPaymentService.createPayment(currentUserId, request));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.ORDER_NOT_PAYABLE);
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("결제 생성 실패 - 이미 PENDING 결제가 존재하면 예외 발생")
        void createPayment_fail_paymentAlreadyExists() {
            // given
            Long currentUserId = 1L;
            Long orderId = 10L;

            Buyer buyer = Buyer.of("test@test.com", "encodedPassword", "홍길동", "010-1111-2222");
            setField(buyer, "id", currentUserId);

            Order order = Order.of(buyer, new BigDecimal("15000"), null, "홍길동", "010-1111-2222", "서울시 강남구");
            setField(order, "id", orderId);

            PaymentCreateRequest request = new PaymentCreateRequest(orderId, MethodEnum.MOCK);

            given(orderRepository.findByIdAndBuyerIdWithLock(orderId, currentUserId)).willReturn(Optional.of(order));
            given(paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.PENDING)).willReturn(true);

            // when
            BaseException ex = assertThrows(BaseException.class, () -> buyerPaymentService.createPayment(currentUserId, request));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_ALREADY_EXISTS);
            verify(paymentRepository, never()).save(any(Payment.class));
        }


    }

    @Nested
    @DisplayName("결제 확인")
    class ConfirmPaymentTest {

        @Test
        @DisplayName("결제 확인 성공")
        void confirmPayment_success() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));

            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(pgResponse.getPaymentId()).willReturn(paymentId);
            given(pgResponse.isPaid()).willReturn(true);
            given(pgResponse.getTotalAmount()).willReturn(new BigDecimal("15000"));

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));

            // when
            PaymentDetailResponse response = paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse);

            // then
            assertThat(response).isNotNull();
            assertThat(dbPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            verify(buyerRefundService, never()).createRefundForAmountMismatch(anyLong(), any(Payment.class), any(PortOnePaymentResponse.class));
            verify(paymentRepository).saveAndFlush(dbPayment);
        }

        @Test
        @DisplayName("이미 성공한 결제면 멱등하게 성공 응답")
        void confirmPayment_alreadySuccess_idempotent() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            // 이미 성공 상태로 변경
            dbPayment.success(java.time.LocalDateTime.now());
            order.paid();

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(pgResponse.getPaymentId()).willReturn(paymentId);

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));

            // when
            PaymentDetailResponse response = paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse);

            // then
            assertThat(response).isNotNull();
            assertThat(dbPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            verify(buyerRefundService, never()).createRefundForAmountMismatch(anyLong(), any(Payment.class), any(PortOnePaymentResponse.class));
        }

        @Test
        @DisplayName("결제가 존재하지 않으면 예외 발생")
        void confirmPayment_fail_paymentNotFound() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";
            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.empty());

            // when
            BaseException ex = assertThrows(BaseException.class, () -> paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("결제 소유자가 아니면 예외 발생")
        void confirmPayment_fail_forbidden() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(999L); // 다른 사용자
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));

            // when
            BaseException ex = assertThrows(BaseException.class, () -> paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_FORBIDDEN);
        }

        @Test
        @DisplayName("요청 paymentId와 PG paymentId가 다르면 예외 발생")
        void confirmPayment_fail_paymentIdMismatch() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(pgResponse.getPaymentId()).willReturn("different_payment_id");

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));

            // when
            BaseException ex = assertThrows(BaseException.class, () -> paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_MISMATCH);
        }

        @Test
        @DisplayName("이미 FAILED 상태인 결제면 예외 발생")
        void confirmPayment_fail_alreadyFailed() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            dbPayment.fail();

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(pgResponse.getPaymentId()).willReturn(paymentId);

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));

            // when
            BaseException ex = assertThrows(BaseException.class, () -> paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_ALREADY_FAILED);
        }

        @Test
        @DisplayName("이미 REFUNDED 상태인 결제면 예외 발생")
        void confirmPayment_fail_alreadyRefunded() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            setField(dbPayment, "status", PaymentStatus.REFUNDED);

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(pgResponse.getPaymentId()).willReturn(paymentId);

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));

            // when
            BaseException ex = assertThrows(BaseException.class, () -> paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_ALREADY_REFUNDED);
        }

        @Test
        @DisplayName("PG 결제가 완료되지 않았으면 FAILED 처리 후 예외 발생")
        void confirmPayment_fail_notCompleted() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(pgResponse.getPaymentId()).willReturn(paymentId);
            given(pgResponse.isPaid()).willReturn(false);

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));
            doAnswer(inv -> {
                inv.getArgument(0, Payment.class).fail();
                return null;
            }).when(paymentStateService).failAndSaveHistory(any(Payment.class));

            doNothing().when(stockReleaseService).releaseStockAndFailOrder(order.getId());

            // when
            BaseException ex = assertThrows(BaseException.class, () -> paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_NOT_COMPLETED);
            assertThat(dbPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(stockReleaseService).releaseStockAndFailOrder(order.getId());
        }

        @Test
        @DisplayName("실제 결제 금액이 null이면 FAILED 처리 후 예외 발생")
        void confirmPayment_fail_amountInvalid() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(pgResponse.getPaymentId()).willReturn(paymentId);
            given(pgResponse.isPaid()).willReturn(true);
            given(pgResponse.getTotalAmount()).willReturn(null);

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));
            doAnswer(inv -> {
                inv.getArgument(0, Payment.class).fail();
                return null;
            }).when(paymentStateService).failAndSaveHistory(any(Payment.class));

            doNothing().when(stockReleaseService).releaseStockAndFailOrder(order.getId());

            // when
            BaseException ex = assertThrows(BaseException.class, () -> paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_AMOUNT_INVALID);
            assertThat(dbPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(stockReleaseService).releaseStockAndFailOrder(order.getId());
            verify(buyerRefundService, never()).createRefundForAmountMismatch(anyLong(), any(Payment.class), any(PortOnePaymentResponse.class));
        }

        @Test
        @DisplayName("결제 금액 불일치면 FAILED 처리 후 환불 생성")
        void confirmPayment_fail_amountMismatch() {
            // given
            Long currentUserId = 1L;
            String paymentId = "payment_10_abc";

            Buyer buyer = createBuyer(currentUserId);
            Order order = createOrder(10L, buyer, new BigDecimal("15000"));
            Payment dbPayment = createPayment(order, paymentId, new BigDecimal("15000"), MethodEnum.MOCK);

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            given(pgResponse.getPaymentId()).willReturn(paymentId);
            given(pgResponse.isPaid()).willReturn(true);
            given(pgResponse.getTotalAmount()).willReturn(new BigDecimal("10000"));

            given(paymentRepository.findByImpUidWithOrder(paymentId)).willReturn(Optional.of(dbPayment));
            doAnswer(inv -> {
                inv.getArgument(0, Payment.class).fail();
                return null;
            }).when(paymentStateService).failAndSaveHistory(any(Payment.class));

            doNothing().when(stockReleaseService).releaseStockAndFailOrder(order.getId());

            // when
            BaseException ex = assertThrows(BaseException.class, () -> paymentRetryService.retryConfirmPayment(currentUserId, paymentId, pgResponse));

            // then
            assertThat(ex.getErrorEnum()).isEqualTo(ErrorEnum.PAYMENT_AMOUNT_MISMATCH);
            assertThat(dbPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);

            verify(buyerRefundService).createRefundForAmountMismatch(currentUserId, dbPayment, pgResponse);
            verify(stockReleaseService).releaseStockAndFailOrder(order.getId());
        }
    }
}
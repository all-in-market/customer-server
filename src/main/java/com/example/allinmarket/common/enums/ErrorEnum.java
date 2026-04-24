package com.example.allinmarket.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorEnum {

    // Common
    INVALID_INPUT(400, "잘못된 입력값입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다."),
    LOCK_ACQUISITION_FAILED(500, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    REDIS_UNAVAILABLE(503, "Redis 서버에 연결할 수 없습니다."),
    INVALID_ARGUMENT(400, "요청값이 올바르지 않습니다"),
    DATA_CONFLICT(409, "요청이 현재 데이터 상태와 충돌합니다."),

    // Order
    ORDER_NOT_FOUND(404, "존재하지 않는 주문입니다."),
    ORDER_ALREADY_COMPLETED(400, "이미 결제 완료된 주문입니다."),
    ORDER_NOT_CANCELLABLE(400, "취소할 수 없는 주문 상태입니다."),
    ORDER_NOT_PAYABLE(400, "해당 주문 상태에서는 결제가 불가능합니다."),
    ORDER_NOT_REFUNDABLE(409, "해당 주문은 환불할 수 없는 상태입니다."),

    // Cart
    CART_ITEMS_EMPTY(404, "주문할 장바구니 상품이 없습니다."),
    INVALID_CART_ITEM_OWNER(403, "다른 사용자의 장바구니 상품이 포함되어 있습니다."),
    CART_NOT_FOUND(404, "장바구니가 존재하지 않습니다."),
    CART_ITEMS_NOT_FOUND(404, "장바구니에 해당 상품이 존재하지 않습니다."),
    INVALID_ORDER_CART_ITEMS(404, "요청한 장바구니 상품이 유효하지 않습니다."),

    // Product
    PRODUCT_NOT_FOUND(404, "존재하지 않는 상품입니다."),
    INVALID_ORDER_PRODUCT(404, "주문 대상 상품이 유효하지 않습니다."),
    PRODUCT_OUT_OF_STOCK(409, "해당 상품의 재고가 부족합니다."),
    PRODUCT_NOT_AVAILABLE(409, "현재 판매 중인 상품이 아닙니다."),

    // Payment
    PAYMENT_ALREADY_EXISTS(400, "이미 결제된 주문입니다."),
    PAYMENT_NOT_COMPLETED(400, "결제가 완료되지 않았습니다."),
    PAYMENT_AMOUNT_MISMATCH(400, "결제 금액이 주문 금액과 일치하지 않습니다."),
    PAYMENT_MISMATCH(400, "결제 정보가 유효하지 않습니다."),
    PAYMENT_AMOUNT_INVALID(400, "결제 금액이 올바르지 않습니다."),
    PAYMENT_NOT_REFUNDABLE(400, "환불이 불가능한 상태의 결제입니다."),
    PAYMENT_FORBIDDEN(403, "해당 결제에 대한 접근 권한이 없습니다."),
    PAYMENT_ALREADY_PROCESSED(409, "이미 처리된 결제입니다."),
    PAYMENT_ALREADY_FAILED(409, "이미 실패 처리된 결제입니다."),
    PAYMENT_ALREADY_REFUNDED(409, "이미 환불 처리된 결제입니다."),
    PAYMENT_FAILED(500, "결제 처리 중 오류가 발생했습니다."),
    PAYMENT_NOT_FOUND(404, "결제 내역이 없습니다."),

    // Refund
    REFUND_ALREADY_EXISTS(409, "이미 환불이 진행 중이거나 처리된 결제입니다."),
    REFUND_FORBIDDEN(403, "해당 결제에 대한 환불 신청 권한이 없습니다."),
    REFUND_AMOUNT_MISMATCH_NOT_FOUND(400, "결제 금액이 주문 금액과 일치하여 금액 불일치 환불을 생성할 수 없습니다."),
    REFUND_NOT_FOUND(404, "존재하지 않는 환불내역 입니다."),
    REFUND_FAILED(500, "환불 처리가 실패했습니다."),

    // Address
    ADDRESS_NOT_FOUND(404, "존재하지 않는 주소입니다."),

    // User
    USER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),
    USER_ALREADY_DELETED(400, "이미 탈퇴한 사용자입니다."),
  
    // BUYER
    BUYER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),
    BUYER_ALREADY_DELETED(400, "이미 탈퇴한 사용자입니다."),
    EMAIL_ALREADY_EXISTS(400, "이미 사용 중인 이메일입니다."),
    PASSWORD_MISMATCH(401, "비밀번호가 올바르지 않습니다."),

    // Seller
    SELLER_NOT_FOUND(404, "존재하지 않는 판매자입니다."),
    SELLER_ALREADY_DELETED(400, "이미 탈퇴한 판매자입니다."),

    // Token
    TOKEN_EXPIRED(401, "만료된 토큰입니다."),
    TOKEN_INVALID(401, "유효하지 않은 토큰입니다."),

    // Category
    CATEGORY_NOT_FOUND(404, "존재하지 않는 카테고리입니다."),

    // Dashboard
    DASHBOARD_NOT_FOUND(404, "대시보드가 존재하지 않습니다."),

    // Redis
    REDIS_LOCK_CONFLICT(409, "현재 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),
    REDIS_LOCK_INTERRUPTED(409, "요청 처리 중 문제가 발생했습니다. 잠시 후 다시 시도해주세요."),
  
    // Statistics
    STATISTICS_NOT_FOUND(404, "존재하지 않는 통계 데이터입니다.");

    private final int status;
    private final String message;
}
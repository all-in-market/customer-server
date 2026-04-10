package com.example.allinmarket.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

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

    // Order
    ORDER_NOT_FOUND(404, "존재하지 않는 주문입니다."),
    ORDER_ALREADY_COMPLETED(400, "이미 결제 완료된 주문입니다."),
    ORDER_NOT_CANCELLABLE(400, "취소할 수 없는 주문 상태입니다."),

    //Payment
    PAYMENT_ALREADY_EXISTS(400, "이미 결제된 주문입니다."),
    PAYMENT_FAILED(500, "결제 처리 중 오류가 발생했습니다."),
    PAYMENT_NOT_FOUND(404, "결제 내역이 없습니다."),

    // User
    USER_NOT_FOUND(404, "존재하지 않는 사용자입니다."),
    USER_ALREADY_DELETED(400, "이미 탈퇴한 사용자입니다."),
    EMAIL_ALREADY_EXISTS(400, "이미 사용 중인 이메일입니다."),
    PASSWORD_MISMATCH(401, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // Seller
    SELLER_NOT_FOUND(404, "존재하지 않는 판매자입니다."),
    SELLER_ALREADY_DELETED(400, "이미 탈퇴한 판매자입니다."),


    // Token
    TOKEN_EXPIRED(401, "만료된 토큰입니다."),
    TOKEN_INVALID(401, "유효하지 않은 토큰입니다.");

    private final int status;
    private final String message;
}
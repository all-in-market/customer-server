package com.example.allinmarket.common.response;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

@JsonPropertyOrder({"success", "status", "message", "data", "timestamp"})
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        LocalDateTime timestamp,
        T data
) {
    public static <T> ApiResponse<T> success(SuccessEnum successEnum, T data) {
        return new ApiResponse<>(true, successEnum.getStatus(), successEnum.getMessage(), LocalDateTime.now(), data);
    }

    public static ApiResponse<Void> fail(ErrorEnum errorEnum) {
        return new ApiResponse<>(false, errorEnum.getStatus(), errorEnum.getMessage(), LocalDateTime.now(), null);
    }

    public static ApiResponse<Void> fail(ErrorEnum errorEnum, String message) {
        return new ApiResponse<>(false, errorEnum.getStatus(), message, LocalDateTime.now(), null);
    }
}
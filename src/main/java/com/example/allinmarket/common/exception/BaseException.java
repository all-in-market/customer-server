package com.example.allinmarket.common.exception;

import com.example.allinmarket.common.enums.ErrorEnum;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorEnum ErrorEnum;

    public BaseException(ErrorEnum ErrorEnum) {
        super(ErrorEnum.getMessage());
        this.ErrorEnum = ErrorEnum;
    }
}
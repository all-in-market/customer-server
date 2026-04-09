package com.example.allinmarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessEnum {

    // Auth
    REGISTER_SUCCESS(201, "회원가입에 성공하였습니다."),
    LOGIN_SUCCESS(200,  "로그인에 성공하였습니다."),
    LOGOUT_SUCCESS(200,  "로그아웃에 성공하였습니다."),

    // 성공한 경우 별도로 구분하지 않음
    CREATE_SUCCESS(201,  "데이터 생성에 성공하였습니다."),
    READ_SUCCESS(200,  "데이터 조회에 성공하였습니다."),
    UPDATE_SUCCESS(200,  "데이터 수정에 성공하였습니다."),
    DELETE_SUCCESS(200,  "데이터 삭제에 성공하였습니다.");

    private final int status;
    private final String message;
}
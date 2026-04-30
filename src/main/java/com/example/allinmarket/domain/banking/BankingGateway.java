package com.example.allinmarket.domain.banking;

import com.example.allinmarket.domain.banking.dto.BankingResponse;

import java.math.BigDecimal;

public interface BankingGateway {

     // 판매자 계좌로 정산 지급(이체)을 요청
     // payoutKey 멱등성 보장을 위한 키
     // bankCode 은행 코드 (Seller 엔티티 확장)
     // accountNumber 계좌 번호
     // amount 이체 금액
     // return 이체 성공 여부

    BankingResponse getBanking(String payoutKey, String bankCode, String accountNumber, BigDecimal amount);
}


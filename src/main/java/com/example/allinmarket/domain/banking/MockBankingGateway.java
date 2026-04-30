package com.example.allinmarket.domain.banking;

import com.example.allinmarket.domain.banking.dto.BankingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class MockBankingGateway implements BankingGateway {

    @Override
    public BankingResponse getBanking(String payoutKey, String bankCode, String accountNumber, BigDecimal amount) {
        log.info("[Mock Banking] 이체 요청 시작 - Key: {}, Account: {}, Amount: {}", payoutKey, accountNumber, amount);

        // 실제 API 연동 시에는 여기서 RestTemplate/WebClient를 사용

        // 성공 응답 시뮬레이션
        return new BankingResponse(true, "MOCK_TX_" + UUID.randomUUID(), payoutKey, amount, null);
    }
}
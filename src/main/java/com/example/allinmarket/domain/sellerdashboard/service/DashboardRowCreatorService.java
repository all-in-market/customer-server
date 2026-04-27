package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardRowCreatorService {
    private final SellerDashboardRepository sellerDashboardRepository;

    // seller + statDate 기준 dashboard 생성
    // update가 실패하는 문제를 방지하기 위해 dashboard row를 사전에 생성하는 역할
    // 존재하면 duplicate 예외를 무시하고 계속 진행
    // REQUIRES_NEW로 독립 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDashboardIfNotExists(Seller seller, LocalDate statDate) {
        try {
            SellerDashboard dashboard = SellerDashboard.of(
                    seller,
                    statDate,
                    0,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );

            sellerDashboardRepository.saveAndFlush(dashboard);

        } catch (DataIntegrityViolationException e) {

            if (isDuplicateKey(e)) {

                log.warn("대시보드 row 동시 생성 충돌. sellerId = {}, statDate = {}", seller.getId(), statDate);

                return;
            }

            throw e;
        }
    }

    private boolean isDuplicateKey(DataIntegrityViolationException e) {
        return e.getMessage() != null && e.getMessage().contains(
                "uk_seller_stat_date"
        );
    }
}

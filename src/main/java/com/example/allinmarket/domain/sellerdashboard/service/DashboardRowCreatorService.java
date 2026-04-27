package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
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

    // seller + statDate 기준 dashboard row가 존재하지 않을 경우 생성
    // update가 실패하는 문제를 방지하기 위해 dashboard row를 사전에 생성하는 역할
    // REQUIRES_NEW로 독립 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDashboardIfNotExists(Seller seller, LocalDate statDate) {
        sellerDashboardRepository.findBySellerIdAndStatDate(seller.getId(), statDate).orElseGet(
                () -> {
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

                        return sellerDashboardRepository.save(dashboard);

                    } catch (DataIntegrityViolationException e) {
                        log.warn("대시보드 row가 이미 존재합니다. sellerId = {}, statDate = {}", seller.getId(), statDate);

                        return sellerDashboardRepository.findBySellerIdAndStatDate(seller.getId(), statDate).orElseThrow(
                                () -> new BaseException(ErrorEnum.DASHBOARD_UPDATE_FAILED)
                        );
                    }
                }
        );
    }
}

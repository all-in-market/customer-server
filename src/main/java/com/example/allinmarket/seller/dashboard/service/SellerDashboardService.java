package com.example.allinmarket.seller.dashboard.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.dashboard.dto.response.SellerDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {

    private final SellerDashboardRepository sellerDashboardRepository;

    public SellerDashboardResponse getSellerDashboard(Long sellerId) {

        SellerDashboard sellerDashboard = sellerDashboardRepository.findBySellerIdAndStatDate(sellerId, LocalDate.now())
                .orElseThrow(() -> new BaseException(ErrorEnum.DASHBOARD_NOT_FOUND));

        return SellerDashboardResponse.from(sellerDashboard);
    }
}

package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Component
@RequiredArgsConstructor
public class DashboardScheduler {
    private final SellerDashboardRepository sellerDashboardRepository;
    private final SellerRepository sellerRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDashboards() {
        List<SellerDashboard> sellerDashboards = sellerDashboardRepository.findAll();

        for (SellerDashboard sellerDashboard : sellerDashboards) {
            sellerDashboard.reset();
        }
    }
}

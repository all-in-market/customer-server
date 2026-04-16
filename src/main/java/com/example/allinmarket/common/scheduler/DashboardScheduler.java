package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DashboardScheduler {
    private final SellerDashboardRepository sellerDashboardRepository;
    private final SellerRepository sellerRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDashboards() {
        List<Seller> sellers = sellerRepository.findAll();

        for (Seller seller : sellers) {
            SellerDashboard dashboard = sellerDashboardRepository.findBySellerId(seller.getId())
                    .orElseGet(() -> SellerDashboard.of(
                            seller,
                            LocalDate.now(),
                            0,
                            0,
                            0,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    ));
            dashboard.reset();
        }
    }
}

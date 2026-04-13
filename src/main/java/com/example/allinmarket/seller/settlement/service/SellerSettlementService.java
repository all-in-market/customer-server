package com.example.allinmarket.seller.settlement.service;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.settlement.dto.response.SettlementDetailResponse;
import com.example.allinmarket.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSettlementService {

    private final SettlementRepository settlementRepository;

    public PageResponse<SettlementDetailResponse> findAll(Long sellerId, Pageable pageable) {
        return PageResponse.register(
                settlementRepository.findAllBySellerId(sellerId, pageable)
                        .map(SettlementDetailResponse::from)
        );
    }
}

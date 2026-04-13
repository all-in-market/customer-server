package com.example.allinmarket.seller.settlement.controller;

import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.settlement.dto.response.SettlementDetailResponse;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.seller.settlement.service.SellerSettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(SellerSettlementController.class)
@AutoConfigureRestTestClient
class SellerSettlementControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SellerSettlementService sellerSettlementService;

    @Test
    void 판매자_정산내역_조회_성공_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        SettlementDetailResponse response = new SettlementDetailResponse(
                1L, 1L,
                BigDecimal.valueOf(50000), BigDecimal.valueOf(1000),
                SettlementStatus.COMPLETED, SettlementType.MID,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15),
                null
        );

        PageResponse<SettlementDetailResponse> pageResponse = new PageResponse<>(
                List.of(response), 1, 1, 1L, 10, true
        );

        when(sellerSettlementService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/seller/settlements")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.content[0].id").isEqualTo(1)
                .jsonPath("$.data.content[0].amount").isEqualTo(50000)
                .jsonPath("$.data.content[0].status").isEqualTo("COMPLETED")
                .jsonPath("$.data.totalElements").isEqualTo(1)
                .jsonPath("$.data.currentPage").isEqualTo(1);
    }

    @Test
    void 판매자_정산내역_조회_빈목록_성공_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        PageResponse<SettlementDetailResponse> emptyPage = new PageResponse<>(
                List.of(), 1, 0, 0L, 10, true
        );

        when(sellerSettlementService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when & then
        restTestClient.get().uri("/seller/settlements")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content").isEmpty()
                .jsonPath("$.data.totalElements").isEqualTo(0);
    }

    @Test
    void 판매자_정산내역_조회_페이징_파라미터_성공_테스트() {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        PageResponse<SettlementDetailResponse> pageResponse = new PageResponse<>(
                List.of(), 2, 5, 42L, 10, false
        );

        when(sellerSettlementService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/seller/settlements?page=1&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.currentPage").isEqualTo(2)
                .jsonPath("$.data.totalPages").isEqualTo(5)
                .jsonPath("$.data.totalElements").isEqualTo(42)
                .jsonPath("$.data.isLast").isEqualTo(false);
    }
}
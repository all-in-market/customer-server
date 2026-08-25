package com.example.allinmarket.seller.dashboard.service;

import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardRowCreatorServiceTest {

    @Mock
    private SellerDashboardRepository sellerDashboardRepository;

    @InjectMocks
    private DashboardRowCreatorService dashboardRowCreatorService;

    @Nested
    @DisplayName("대시보드 row 생성")
    class CreateDashboardIfNotExists {

        @Test
        @DisplayName("정상 생성 시 seller와 statDate, 0 초기값으로 저장한다")
        void createDashboardIfNotExists_success_savesWithZeroInitialValues() {
            // given
            Seller seller = createSeller(1L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);

            // when
            dashboardRowCreatorService.createDashboardIfNotExists(seller, statDate);

            // then
            ArgumentCaptor<SellerDashboard> captor = ArgumentCaptor.forClass(SellerDashboard.class);
            verify(sellerDashboardRepository).saveAndFlush(captor.capture());

            SellerDashboard saved = captor.getValue();
            assertThat(saved.getSeller()).isEqualTo(seller);
            assertThat(saved.getStatDate()).isEqualTo(statDate);
            assertThat(saved.getTotalOrders()).isZero();
            assertThat(saved.getTotalProductsSold()).isZero();
            assertThat(saved.getTotalRefunds()).isZero();
            assertThat(saved.getTotalSales()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getRefundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(saved.getSettlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("uk_seller_stat_date 제약 위반은 무시하고 정상 종료한다")
        void createDashboardIfNotExists_duplicateKeyViolation_isIgnored() {
            // given
            Seller seller = createSeller(1L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);

            ConstraintViolationException cve = new ConstraintViolationException(
                    "duplicate key", new SQLException("duplicate"), "uk_seller_stat_date"
            );
            DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key violation", cve);

            given(sellerDashboardRepository.saveAndFlush(any())).willThrow(exception);

            // when & then
            assertThatCode(() -> dashboardRowCreatorService.createDashboardIfNotExists(seller, statDate))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("cause 체인이 여러 단계로 중첩되어도 uk_seller_stat_date 위반을 찾아내면 무시한다")
        void createDashboardIfNotExists_deeplyNestedDuplicateKeyViolation_isIgnored() {
            // given
            Seller seller = createSeller(1L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);

            ConstraintViolationException cve = new ConstraintViolationException(
                    "duplicate key", new SQLException("duplicate"), "uk_seller_stat_date"
            );
            RuntimeException middleCause = new RuntimeException("wrapped", cve);
            DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate key violation", middleCause);

            given(sellerDashboardRepository.saveAndFlush(any())).willThrow(exception);

            // when & then
            assertThatCode(() -> dashboardRowCreatorService.createDashboardIfNotExists(seller, statDate))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("다른 제약 조건 위반은 예외를 전파한다")
        void createDashboardIfNotExists_otherConstraintViolation_propagatesException() {
            // given
            Seller seller = createSeller(1L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);

            ConstraintViolationException cve = new ConstraintViolationException(
                    "not null violation", new SQLException("not null"), "some_other_constraint"
            );
            DataIntegrityViolationException exception = new DataIntegrityViolationException("other violation", cve);

            given(sellerDashboardRepository.saveAndFlush(any())).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> dashboardRowCreatorService.createDashboardIfNotExists(seller, statDate))
                    .isSameAs(exception);
        }

        @Test
        @DisplayName("cause 체인에 ConstraintViolationException이 없으면 예외를 전파한다")
        void createDashboardIfNotExists_noConstraintViolationInCauseChain_propagatesException() {
            // given
            Seller seller = createSeller(1L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);

            DataIntegrityViolationException exception = new DataIntegrityViolationException(
                    "unexpected failure", new RuntimeException("unrelated cause")
            );

            given(sellerDashboardRepository.saveAndFlush(any())).willThrow(exception);

            // when & then
            assertThatThrownBy(() -> dashboardRowCreatorService.createDashboardIfNotExists(seller, statDate))
                    .isSameAs(exception);
        }
    }

    private Seller createSeller(Long id) {
        Seller seller = Seller.of(
                "seller" + id + "@test.com",
                "encodedPassword",
                "판매자" + id,
                "010-1234-567" + id,
                "테스트 스토어" + id,
                "123-45-6789" + id,
                "KOOKMIN",
                "123-456789-12-345"
        );
        ReflectionTestUtils.setField(seller, "id", id);
        return seller;
    }
}

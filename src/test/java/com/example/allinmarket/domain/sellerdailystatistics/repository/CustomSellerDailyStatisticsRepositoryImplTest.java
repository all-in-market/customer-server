package com.example.allinmarket.domain.sellerdailystatistics.repository;

import com.example.allinmarket.common.config.JpaAuditingConfig;
import com.example.allinmarket.common.config.QuerydslConfig;
import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import com.example.allinmarket.seller.dailystatistics.dto.DailyStatisticsResponse;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class})
class CustomSellerDailyStatisticsRepositoryImplTest {

    @Autowired
    private SellerDailyStatisticsRepository repository;

    @Autowired
    private EntityManager em;

    private Seller seller1;
    private Seller seller2;

    @BeforeEach
    void setUp() {
        seller1 = Seller.of("s1@test.com", "pw", "판매자1", "010-1111-1111", "스토어1", "111-11-11111", "acc1");
        seller2 = Seller.of("s2@test.com", "pw", "판매자2", "010-2222-2222", "스토어2", "222-22-22222", "acc2");
        em.persist(seller1);
        em.persist(seller2);
        em.flush();
    }

    private void saveStats(Seller seller, LocalDate date, int orders, int items, int refunds,
                           BigDecimal sales, BigDecimal refundAmt, BigDecimal netSales) {
        SellerDailyStatistics stats = SellerDailyStatistics.of(seller, date, orders, items, refunds, sales, refundAmt, netSales);
        em.persist(stats);
    }

    private BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }

    @Nested
    @DisplayName("기간 내 데이터 합산")
    class AggregationTests {

        @Test
        @DisplayName("기간 내 여러 날짜의 통계가 합산되어 반환된다")
        void 기간_내_여러날짜_합산_성공() {
            // given
            LocalDate from = LocalDate.of(2025, 4, 1);
            LocalDate to = LocalDate.of(2025, 4, 30);

            saveStats(seller1, LocalDate.of(2025, 4, 10), 2, 5, 0, bd(10000), bd(0), bd(10000));
            saveStats(seller1, LocalDate.of(2025, 4, 20), 3, 8, 1, bd(20000), bd(5000), bd(15000));
            em.flush();

            // when
            DailyStatisticsResponse result = repository.findRangedStatistics(seller1.getId(), from, to);

            // then
            assertNotNull(result);
            assertEquals(seller1.getId(), result.sellerId());
            assertEquals(from, result.from());
            assertEquals(to, result.to());
            assertEquals(5, result.totalOrders());
            assertEquals(13, result.totalItems());
            assertEquals(1, result.totalRefunds());
            assertEquals(0, bd(30000).compareTo(result.totalSales()));
            assertEquals(0, bd(5000).compareTo(result.refundAmount()));
            assertEquals(0, bd(25000).compareTo(result.netSales()));
        }

        @Test
        @DisplayName("from, to 경계 날짜의 통계가 포함된다")
        void 경계_날짜_포함() {
            // given
            LocalDate from = LocalDate.of(2025, 4, 1);
            LocalDate to = LocalDate.of(2025, 4, 30);

            saveStats(seller1, from, 1, 1, 0, bd(1000), bd(0), bd(1000));
            saveStats(seller1, to, 2, 2, 0, bd(2000), bd(0), bd(2000));
            em.flush();

            // when
            DailyStatisticsResponse result = repository.findRangedStatistics(seller1.getId(), from, to);

            // then
            assertNotNull(result);
            assertEquals(3, result.totalOrders());
            assertEquals(0, bd(3000).compareTo(result.totalSales()));
        }

        @Test
        @DisplayName("기간 밖 날짜의 통계는 합산에 포함되지 않는다")
        void 기간_밖_데이터_미포함() {
            // given
            LocalDate from = LocalDate.of(2025, 4, 1);
            LocalDate to = LocalDate.of(2025, 4, 30);

            saveStats(seller1, LocalDate.of(2025, 3, 31), 9, 9, 0, bd(90000), bd(0), bd(90000));
            saveStats(seller1, LocalDate.of(2025, 4, 15), 2, 4, 0, bd(10000), bd(0), bd(10000));
            saveStats(seller1, LocalDate.of(2025, 5, 1),  9, 9, 0, bd(90000), bd(0), bd(90000));
            em.flush();

            // when
            DailyStatisticsResponse result = repository.findRangedStatistics(seller1.getId(), from, to);

            // then
            assertNotNull(result);
            assertEquals(2, result.totalOrders());
            assertEquals(0, bd(10000).compareTo(result.totalSales()));
        }
    }

    @Nested
    @DisplayName("null 조건 처리")
    class NullConditionTests {

        @Test
        @DisplayName("기간 내 데이터가 없으면 null을 반환한다")
        void 데이터_없으면_null_반환() {
            // given
            LocalDate from = LocalDate.of(2025, 4, 1);
            LocalDate to = LocalDate.of(2025, 4, 30);

            // when
            DailyStatisticsResponse result = repository.findRangedStatistics(seller1.getId(), from, to);

            // then
            assertNull(result);
        }

        @Test
        @DisplayName("from이 null이면 to 이전 데이터를 모두 포함한다")
        void from_null이면_to_이전_전체_포함() {
            // given
            LocalDate to = LocalDate.of(2025, 4, 30);

            saveStats(seller1, LocalDate.of(2025, 1, 1),  1, 1, 0, bd(1000), bd(0), bd(1000));
            saveStats(seller1, LocalDate.of(2025, 4, 30), 2, 2, 0, bd(2000), bd(0), bd(2000));
            saveStats(seller1, LocalDate.of(2025, 5, 1),  9, 9, 0, bd(9000), bd(0), bd(9000)); // 범위 밖
            em.flush();

            // when
            DailyStatisticsResponse result = repository.findRangedStatistics(seller1.getId(), null, to);

            // then
            assertNotNull(result);
            assertNull(result.from());
            assertEquals(to, result.to());
            assertEquals(3, result.totalOrders());
        }

        @Test
        @DisplayName("to가 null이면 from 이후 데이터를 모두 포함한다")
        void to_null이면_from_이후_전체_포함() {
            // given
            LocalDate from = LocalDate.of(2025, 4, 1);

            saveStats(seller1, LocalDate.of(2025, 3, 31),  9, 9, 0, bd(9000), bd(0), bd(9000)); // 범위 밖
            saveStats(seller1, LocalDate.of(2025, 4, 1),   1, 1, 0, bd(1000), bd(0), bd(1000));
            saveStats(seller1, LocalDate.of(2025, 12, 31), 2, 2, 0, bd(2000), bd(0), bd(2000));
            em.flush();

            // when
            DailyStatisticsResponse result = repository.findRangedStatistics(seller1.getId(), from, null);

            // then
            assertNotNull(result);
            assertEquals(from, result.from());
            assertNull(result.to());
            assertEquals(3, result.totalOrders());
        }
    }

    @Nested
    @DisplayName("판매자 격리")
    class SellerIsolationTests {

        @Test
        @DisplayName("다른 판매자의 통계는 합산에 포함되지 않는다")
        void 다른_판매자_데이터_미포함() {
            // given
            LocalDate from = LocalDate.of(2025, 4, 1);
            LocalDate to = LocalDate.of(2025, 4, 30);

            saveStats(seller1, LocalDate.of(2025, 4, 10), 2, 5, 0, bd(10000), bd(0), bd(10000));
            saveStats(seller2, LocalDate.of(2025, 4, 10), 99, 99, 0, bd(999999), bd(0), bd(999999));
            em.flush();

            // when
            DailyStatisticsResponse result = repository.findRangedStatistics(seller1.getId(), from, to);

            // then
            assertNotNull(result);
            assertEquals(seller1.getId(), result.sellerId());
            assertEquals(2, result.totalOrders());
            assertEquals(0, bd(10000).compareTo(result.totalSales()));
        }
    }
}

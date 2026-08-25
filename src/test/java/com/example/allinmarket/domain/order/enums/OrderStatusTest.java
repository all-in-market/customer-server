package com.example.allinmarket.domain.order.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Nested
    @DisplayName("주문 상태 전이 매트릭스")
    class CanTransitToTargetStatusTest {

        @ParameterizedTest(name = "{0} → {1} 은 {2}")
        @DisplayName("코드에 정의된 전이 규칙을 그대로 검증한다")
        @CsvSource({
                "CREATED,   CREATED,   false",
                "CREATED,   PAID,      true",
                "CREATED,   SHIPPED,   false",
                "CREATED,   DELIVERED, false",
                "CREATED,   REFUNDED,  false",
                "CREATED,   FAILED,    true",

                "PAID,      CREATED,   false",
                "PAID,      PAID,      false",
                "PAID,      SHIPPED,   true",
                "PAID,      DELIVERED, false",
                "PAID,      REFUNDED,  true",
                "PAID,      FAILED,    false",

                "SHIPPED,   CREATED,   false",
                "SHIPPED,   PAID,      false",
                "SHIPPED,   SHIPPED,   false",
                "SHIPPED,   DELIVERED, true",
                "SHIPPED,   REFUNDED,  false",
                "SHIPPED,   FAILED,    false",

                "DELIVERED, CREATED,   false",
                "DELIVERED, PAID,      false",
                "DELIVERED, SHIPPED,   false",
                "DELIVERED, DELIVERED, false",
                "DELIVERED, REFUNDED,  true",
                "DELIVERED, FAILED,    false",

                "REFUNDED,  CREATED,   false",
                "REFUNDED,  PAID,      false",
                "REFUNDED,  SHIPPED,   false",
                "REFUNDED,  DELIVERED, false",
                "REFUNDED,  REFUNDED,  false",
                "REFUNDED,  FAILED,    false",

                "FAILED,    CREATED,   false",
                "FAILED,    PAID,      false",
                "FAILED,    SHIPPED,   false",
                "FAILED,    DELIVERED, false",
                "FAILED,    REFUNDED,  false",
                "FAILED,    FAILED,    false",
        })
        void canTransitToTargetStatus_matrix(OrderStatus from, OrderStatus to, boolean expected) {
            // when
            boolean result = from.canTransitToTargetStatus(to);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} → null 은 항상 false")
        @DisplayName("목표 상태가 null이면 어떤 상태에서도 전이가 불가능하다")
        @EnumSource(OrderStatus.class)
        void canTransitToTargetStatus_whenTargetIsNull_thenFalse(OrderStatus from) {
            // when
            boolean result = from.canTransitToTargetStatus(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("REFUNDED, FAILED는 종료 상태이므로 어떤 상태로도 전이할 수 없다")
    void canTransitToTargetStatus_terminalStates_alwaysFalse() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThat(OrderStatus.REFUNDED.canTransitToTargetStatus(target)).isFalse();
            assertThat(OrderStatus.FAILED.canTransitToTargetStatus(target)).isFalse();
        }
    }
}

package com.example.allinmarket.domain.product.entity;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ProductTest {

    @Nested
    @DisplayName("상품 생성")
    class OfTest {

        @Test
        @DisplayName("상품을 생성하면 상태는 판매중으로 초기화된다")
        void of_whenCreated_thenStatusIsOnSale() {
            // given
            Seller seller = mock(Seller.class);
            Category category = mock(Category.class);

            // when
            Product product = Product.of(seller, category, "상품", BigDecimal.valueOf(1000), 10, "설명");

            // then
            assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        }
    }

    @Nested
    @DisplayName("재고 차감")
    class DecreaseStockTest {

        @Test
        @DisplayName("재고보다 적은 수량을 차감하면 재고가 줄어든다")
        void decreaseStock_whenAmountLessThanStock_thenDecreaseStock() {
            // given
            Product product = createProduct(10);

            // when
            product.decreaseStock(4);

            // then
            assertThat(product.getStock()).isEqualTo(6);
        }

        @Test
        @DisplayName("재고와 정확히 같은 수량을 차감하면 재고가 0이 된다")
        void decreaseStock_whenAmountEqualsStock_thenStockBecomesZero() {
            // given
            Product product = createProduct(10);

            // when
            product.decreaseStock(10);

            // then
            assertThat(product.getStock()).isZero();
        }

        @Test
        @DisplayName("재고보다 많은 수량을 차감하면 예외가 발생하고 재고는 변하지 않는다")
        void decreaseStock_whenAmountExceedsStock_thenThrowOutOfStock() {
            // given
            Product product = createProduct(10);

            // when & then
            assertThatThrownBy(() -> product.decreaseStock(11))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PRODUCT_OUT_OF_STOCK);

            assertThat(product.getStock()).isEqualTo(10);
        }
    }

    private Product createProduct(int stock) {
        Seller seller = mock(Seller.class);
        Category category = mock(Category.class);
        return Product.of(seller, category, "상품", BigDecimal.valueOf(1000), stock, "설명");
    }
}

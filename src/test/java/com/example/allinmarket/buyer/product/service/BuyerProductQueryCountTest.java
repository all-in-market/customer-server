package com.example.allinmarket.buyer.product.service;

import com.example.allinmarket.common.config.JpaAuditingConfig;
import com.example.allinmarket.common.config.QuerydslConfig;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class BuyerProductQueryCountTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void productListQueryCountIsBoundedWhenMappingToResponse() {
        saveProducts(3);
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<Product> products = productRepository.findAllVisibleProducts(
                PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "createdAt"))
        );
        List<ProductDetailResponse> responses = products
                .map(ProductDetailResponse::from)
                .getContent();

        assertThat(responses).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    private void saveProducts(int count) {
        IntStream.rangeClosed(1, count).forEach(index -> {
            Seller seller = Seller.of(
                    "seller" + index + "@test.com",
                    "password",
                    "판매자" + index,
                    "010-1000-100" + index,
                    "스토어" + index,
                    "biz-number-" + index,
                    "KOOKMIN",
                    "account-" + index
            );
            Category category = Category.of("카테고리" + index, index);
            entityManager.persist(seller);
            entityManager.persist(category);
            entityManager.persist(Product.of(
                    seller,
                    category,
                    "상품" + index,
                    BigDecimal.valueOf(10000 + index),
                    100,
                    "상품 설명" + index
            ));
        });
    }
}

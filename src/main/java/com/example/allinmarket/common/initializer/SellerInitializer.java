package com.example.allinmarket.common.initializer;

import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
@ConditionalOnProperty(name = "app.init.admin.enabled", havingValue = "true", matchIfMissing = true)
public class SellerInitializer implements ApplicationRunner {

    // { 카테고리명, 상품명, 가격, 재고, 설명 }
    private static final List<Object[]> PRODUCTS = List.of(
            new Object[]{"전자제품", "무선 블루투스 이어폰", "29900", 100, "고음질 무선 블루투스 이어폰입니다."},
            new Object[]{"전자제품", "스마트워치", "189000", 50, "심박수·수면 측정 기능이 포함된 스마트워치입니다."},
            new Object[]{"전자제품", "USB-C 고속 충전기", "19900", 200, "65W PD 고속 충전을 지원하는 충전기입니다."},
            new Object[]{"전자제품", "미니 블루투스 스피커", "45000", 80, "방수 기능을 갖춘 포터블 블루투스 스피커입니다."},
            new Object[]{"의류", "기본 면 티셔츠", "15900", 300, "사계절 입기 좋은 순면 반팔 티셔츠입니다."},
            new Object[]{"의류", "슬림핏 청바지", "49000", 150, "신축성 좋은 슬림핏 데님 청바지입니다."},
            new Object[]{"의류", "후드 집업", "39000", 120, "기모 안감의 따뜻한 후드 집업입니다."},
            new Object[]{"식품", "유기농 그래놀라", "12900", 500, "무첨가 유기농 귀리로 만든 그래놀라입니다."},
            new Object[]{"식품", "콜드브루 커피 10입", "18000", 250, "진한 풍미의 콜드브루 커피 파우치 10개입입니다."},
            new Object[]{"식품", "프리미엄 견과류 혼합", "22000", 180, "아몬드·캐슈넛·호두가 포함된 혼합 견과류입니다."}
    );

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Value("${seller.email}")
    private String email;

    @Value("${seller.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (sellerRepository.existsByEmail(email)) {
            return;
        }

        Seller seller = Seller.of(
                email,
                passwordEncoder.encode(password),
                "홍길동",
                "010-1234-1234",
                "all-in-market",
                "biz67899",
                "01-123456-123456"
        );

        seller.updateStatus(SellerStatus.APPROVED);
        sellerRepository.save(seller);

        log.info("판매자 계정이 생성되었습니다: {}", email);

        for (Object[] info : PRODUCTS) {
            String categoryName = (String) info[0];
            String productName = (String) info[1];
            BigDecimal price = new BigDecimal((String) info[2]);
            int stock = (int) info[3];
            String description = (String) info[4];

            Category category = categoryRepository.findByName(categoryName)
                    .orElseGet(() -> {
                        Category c = Category.of(categoryName, 0);
                        categoryRepository.save(c);
                        log.info("카테고리가 생성되었습니다: {}", categoryName);
                        return c;
                    });

            Product product = Product.of(seller, category, productName, price, stock, description);
            productRepository.save(product);
            log.info("상품이 생성되었습니다: {}", productName);
        }
    }
}
package com.example.allinmarket.common.config;

import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.init.admin.enabled", havingValue = "true", matchIfMissing = true)
public class SellerInitializer implements ApplicationRunner {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;

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

        Category category = Category.of(
                "전자제품",
                0
        );

        categoryRepository.save(category);

        log.info("카테고리가 생성되었습니다: {}", category.getName());
    }
}
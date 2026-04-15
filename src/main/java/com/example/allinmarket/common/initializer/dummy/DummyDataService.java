package com.example.allinmarket.common.initializer.dummy;

import com.example.allinmarket.domain.category.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.datafaker.Faker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    private final Faker faker = new Faker(new Locale("ko"));

    // batchUpdate 크기 단위 설정
    private static final int CATEGORY_BATCH_SIZE = 100;

    /**
     * 카테고리 더미 데이터 생성
     */
    public void createDummyCategory(int totalCategoryCount) {

        long start = System.currentTimeMillis();

        List<Object[]> batchCategories = new ArrayList<>(CATEGORY_BATCH_SIZE);

        String sql = """
            INSERT INTO categories
            (name, sort_order, created_at, updated_at)
            VALUES (?, ?, now(), now())
            """;

        for (int i = 0; i < totalCategoryCount; i++) {

            String name = faker.commerce().department() + "_" + i;
            long sortOrder = 1L;

            batchCategories.add(new Object[]{name, sortOrder});

            if(batchCategories.size() == CATEGORY_BATCH_SIZE){
                jdbcTemplate.batchUpdate(sql, batchCategories);
                batchCategories.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} category in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if(!batchCategories.isEmpty()){
            jdbcTemplate.batchUpdate(sql, batchCategories);
        }
    }
}

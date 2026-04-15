package com.example.allinmarket.common.initializer.dummy;

import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.datafaker.Faker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    private final SellerRepository sellerRepository;
    private final CategoryRepository categoryRepository;

    private final Faker faker = new Faker(new Locale("ko"));

    // batchUpdate 크기 단위 설정
    private static final int CATEGORY_BATCH_SIZE = 100;
    private static final int SELLER_BATCH_SIZE = 1000;
    private static final int PRODUCT_BATCH_SIZE = 10000;
    private static final int BUYER_BATCH_SIZE = 1000;
    private static final int CART_BATCH_SIZE = 1000;
    private static final int ADDRESS_BATCH_SIZE = 1000;

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

    /**
     * 판매자 더미 데이터 생성
     */
    public void createDummySeller(int totalSellerCount) {
        long start = System.currentTimeMillis();

        List<Object[]> batchSellers = new ArrayList<>(SELLER_BATCH_SIZE);

        String sql = """
            INSERT INTO sellers
            (email, password, name, phone, store_name, biz_number, bank_account, status, role, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
            """;

        String password = passwordEncoder.encode("12345678");

        for (int i = 0; i < totalSellerCount; i++) {

            String email = "user" + i + "@test.com";
            String name = faker.name().fullName() + "_" + i;
            String phone = faker.phoneNumber().phoneNumber();
            String storeName = faker.funnyName().name() + "_" + i;
            String biz_number = "biz_" + i;
            String bank_account = "bank_" + i;
            String status = SellerStatus.APPROVED.name();
            String role = UserRole.SELLER.name();

            batchSellers.add(new Object[]{email, password, name, phone, storeName, biz_number, bank_account, status, role});

            if(batchSellers.size() == SELLER_BATCH_SIZE){
                jdbcTemplate.batchUpdate(sql, batchSellers);
                batchSellers.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} seller in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if(!batchSellers.isEmpty()){
            jdbcTemplate.batchUpdate(sql, batchSellers);
        }

        long finished = System.currentTimeMillis() - start;

        log.info("sell batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 상품 더미 데이터 생성
     */
    public void createDummyProduct(int totalProductCount) {

        long start = System.currentTimeMillis();

        List<Long> sellerIdList = jdbcTemplate.queryForList("SELECT id FROM sellers", Long.class);
        List<Long> categoryIdList = jdbcTemplate.queryForList("SELECT id FROM categories", Long.class);

        List<Object[]> batchProducts = new ArrayList<>(PRODUCT_BATCH_SIZE);

        String sql = """
            INSERT INTO products
            (seller_id, category_id, name, price, stock, status, description, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())
            """;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < totalProductCount; i++) {

            Long sellerId = sellerIdList.get(random.nextInt(sellerIdList.size()));
            Long categoryId = categoryIdList.get(random.nextInt(categoryIdList.size()));

            String name = faker.commerce().productName() + "_" + i;
            BigDecimal price = BigDecimal.valueOf(random.nextInt(3000, 30000));
            int stock = random.nextInt(50000, 100000);
            String status = ProductStatus.ON_SALE.name();
            String description = "description_" + i;

            batchProducts.add(new Object[]{sellerId, categoryId, name, price, stock, status, description});

            if(batchProducts.size() == PRODUCT_BATCH_SIZE){
                jdbcTemplate.batchUpdate(sql, batchProducts);
                batchProducts.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} product in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if(!batchProducts.isEmpty()){
            jdbcTemplate.batchUpdate(sql, batchProducts);
        }

        long finished = System.currentTimeMillis() - start;
        log.info("product batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 구매자 더미 데이터 생성
     */
    public void createDummyBuyer(int totalBuyerCount) {
        long start = System.currentTimeMillis();

        List<Object[]> batchBuyers = new ArrayList<>(BUYER_BATCH_SIZE);

        String sql = """
        INSERT INTO buyers
        (email, password, name, phone, role, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, now(), now())
        """;

        String password = passwordEncoder.encode("12345678");

        for (int i = 0; i < totalBuyerCount; i++) {

            String email = "user" + i + "@test.com";
            String name = faker.name().fullName() + "_" + i;
            String phone = faker.phoneNumber().phoneNumber();
            String role = UserRole.BUYER.name();

            batchBuyers.add(new Object[]{email, password, name, phone, role});

            if(batchBuyers.size() == BUYER_BATCH_SIZE){
                jdbcTemplate.batchUpdate(sql, batchBuyers);
                batchBuyers.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} buyer in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if(!batchBuyers.isEmpty()){
            jdbcTemplate.batchUpdate(sql, batchBuyers);
        }

        long finished = System.currentTimeMillis() - start;

        log.info("sell batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 카트 데이터 생성
     */
    public void createDummyCart(int totalCartCount) {

        long start = System.currentTimeMillis();

        List<Long> buyerIdList = jdbcTemplate.queryForList("SELECT id FROM buyers", Long.class);

        List<Object[]> batchCarts = new ArrayList<>(CART_BATCH_SIZE);

        String sql = """
            INSERT INTO carts
            (buyer_id, created_at, updated_at)
            VALUES (?, now(), now())
            """;

        for (int i = 0; i < totalCartCount; i++) {

            Long buyerId = buyerIdList.get(i);

            batchCarts.add(new Object[]{buyerId});

            if(batchCarts.size() == CART_BATCH_SIZE){
                jdbcTemplate.batchUpdate(sql, batchCarts);
                batchCarts.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} cart in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if(!batchCarts.isEmpty()){
            jdbcTemplate.batchUpdate(sql, batchCarts);
        }

        long finished = System.currentTimeMillis() - start;
        log.info("cart batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 주소 더미 데이터 생성
     */
    public void createDummyAddress(int totalAddressCount) {
        long start = System.currentTimeMillis();

        List<Object[]> batchAddress = new ArrayList<>(ADDRESS_BATCH_SIZE);

        String sql = """
        INSERT INTO addresses
        (buyer_id, recipient, phone, detail, is_default, created_at, updated_at)
        VALUES (?, ?, ?, ?, false, now(), now())
        """;

        for (int i = 0; i < totalAddressCount; i++) {

            long buyerId = i + 1;
            String recipient = faker.name().fullName() + "_" + i;
            String phone = faker.phoneNumber().phoneNumber();
            String detail = faker.address().fullAddress();

            batchAddress.add(new Object[]{buyerId, recipient, phone, detail});

            if(batchAddress.size() == ADDRESS_BATCH_SIZE){
                jdbcTemplate.batchUpdate(sql, batchAddress);
                batchAddress.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} addresses in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if(!batchAddress.isEmpty()){
            jdbcTemplate.batchUpdate(sql, batchAddress);
        }

        long finished = System.currentTimeMillis() - start;

        log.info("address batchUpdate finished in {} s", finished / 1000.0);
    }
}

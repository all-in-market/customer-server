package com.example.allinmarket.common.initializer.dummy;

import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.enums.SellerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataService {

    // batchUpdate 크기 단위 설정
    private static final int CATEGORY_BATCH_SIZE = 100;
    private static final int SELLER_BATCH_SIZE = 1000;
    private static final int PRODUCT_BATCH_SIZE = 10000;
    private static final int BUYER_BATCH_SIZE = 1000;
    private static final int CART_BATCH_SIZE = 1000;
    private static final int ADDRESS_BATCH_SIZE = 1000;
    private static final int CART_ITEM_BATCH_SIZE = 1000;
    private static final int ORDER_BATCH_SIZE = 1000;
    private static final int ORDER_ITEM_BATCH_SIZE = 1000;
    private static final int DAILY_STATISTICS_BATCH_SIZE = 1000;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final Faker faker = new Faker(new Locale("ko"));

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

            if (batchCategories.size() == CATEGORY_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchCategories);
                batchCategories.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} category in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if (!batchCategories.isEmpty()) {
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
                (email, password, name, phone, store_name, biz_number, bank_code, bank_account, status, role, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """;

        String password = passwordEncoder.encode("1234567890");

        for (int i = 0; i < totalSellerCount; i++) {

            String email = "user" + i + "@test.com";
            String name = faker.name().fullName() + "_" + i;
            String phone = faker.phoneNumber().phoneNumber();
            String storeName = faker.funnyName().name() + "_" + i;
            String biz_number = "biz_" + i;
            String bank_code = "KOOKMIN";
            String bank_account = "bank_" + i;
            String status = SellerStatus.APPROVED.name();
            String role = UserRole.SELLER.name();

            batchSellers.add(new Object[]{email, password, name, phone, storeName, biz_number, bank_code, bank_account, status, role});

            if (batchSellers.size() == SELLER_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchSellers);
                batchSellers.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} seller in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if (!batchSellers.isEmpty()) {
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

            if (batchProducts.size() == PRODUCT_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchProducts);
                batchProducts.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} product in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if (!batchProducts.isEmpty()) {
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

        String password = passwordEncoder.encode("1234567890");

        for (int i = 0; i < totalBuyerCount; i++) {

            String email = "user" + i + "@test.com";
            String name = faker.name().fullName() + "_" + i;
            String phone = faker.phoneNumber().phoneNumber();
            String role = UserRole.BUYER.name();

            batchBuyers.add(new Object[]{email, password, name, phone, role});

            if (batchBuyers.size() == BUYER_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchBuyers);
                batchBuyers.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} buyer in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if (!batchBuyers.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchBuyers);
        }

        long finished = System.currentTimeMillis() - start;

        log.info("sell batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 셀러 대시보드 초기 데이터 생성 (오늘 날짜 기준 seller당 1행)
     * addOrder()가 UPDATE이므로 row가 없으면 0 rows affected → 테스트 현실성 확보
     */
    public void createDummySellerDashboard() {
        long start = System.currentTimeMillis();

        List<Long> sellerIdList = jdbcTemplate.queryForList("SELECT id FROM sellers", Long.class);

        Date today = Date.valueOf(LocalDate.now());
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seller_dashboard WHERE stat_date = ?", Integer.class, today);
        if (existing != null && existing > 0) {
            log.info("seller_dashboard already seeded for today, skipping");
            return;
        }

        String sql = """
                INSERT INTO seller_dashboard
                (seller_id, stat_date, total_orders, total_sales, total_products_sold,
                 total_refunds, refund_amount, fee_amount, settlement_amount, created_at, updated_at)
                VALUES (?, ?, 0, 0.00, 0, 0, 0.00, 0.00, 0.00, now(), now())
                """;

        List<Object[]> batch = new ArrayList<>(1000);

        for (Long sellerId : sellerIdList) {
            batch.add(new Object[]{sellerId, today});

            if (batch.size() == 1000) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }

        log.info("seller_dashboard seed finished in {} s", (System.currentTimeMillis() - start) / 1000.0);
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

            if (batchCarts.size() == CART_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchCarts);
                batchCarts.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} cart in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if (!batchCarts.isEmpty()) {
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

            if (batchAddress.size() == ADDRESS_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchAddress);
                batchAddress.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} addresses in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if (!batchAddress.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchAddress);
        }

        long finished = System.currentTimeMillis() - start;

        log.info("address batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 카트 아이템 더미 데이터 생성
     */
    public void createDummyCartItem(int totalCartItemCount) {
        long start = System.currentTimeMillis();

        List<Object[]> batchCartItem = new ArrayList<>(CART_ITEM_BATCH_SIZE);
        List<Long> buyerIdList = jdbcTemplate.queryForList("SELECT id FROM buyers", Long.class);
        List<Long> productIdList = jdbcTemplate.queryForList("SELECT id FROM products", Long.class);


        String sql = """
                INSERT INTO cart_items
                (cart_id, product_id, quantity, created_at, updated_at)
                VALUES (?, ?, ?, now(), now())
                """;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < totalCartItemCount; i++) {

            Long cartId = buyerIdList.get(i);
            Long productId = productIdList.get(random.nextInt(productIdList.size()));
            int quantity = random.nextInt(1,5);

            batchCartItem.add(new Object[]{cartId, productId, quantity});

            if (batchCartItem.size() == CART_ITEM_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchCartItem);
                batchCartItem.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} cart_items in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if (!batchCartItem.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchCartItem);
        }

        long finished = System.currentTimeMillis() - start;

        log.info("cart_items batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 주문(결제 이전) 더미 데이터 생성
     */
    public void createDummyOrderBeforePayment(int totalOrderCount) {
        long start = System.currentTimeMillis();

        List<Object[]> batchOrder = new ArrayList<>(ORDER_BATCH_SIZE);
        List<Long> buyerIdList = jdbcTemplate.queryForList("SELECT id FROM buyers", Long.class);


        String sql = """
                INSERT INTO orders
                (buyer_id, total_amount, status, tracking_number, recipient, phone, address, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, now(), now())
                """;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < totalOrderCount; i++) {

            Long buyerId = buyerIdList.get(i);
            BigDecimal totalAmount = BigDecimal.valueOf(random.nextInt(100000));
            String status = OrderStatus.CREATED.getStatus();
            String trackingNumber = UUID.randomUUID().toString();
            String recipient = faker.name().fullName() + "_" + i;
            String phone = faker.phoneNumber().phoneNumber();
            String address = faker.address().fullAddress();

            batchOrder.add(new Object[]{buyerId, totalAmount, status, trackingNumber, recipient, phone, address});

            if (batchOrder.size() == ORDER_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchOrder);
                batchOrder.clear();

                long elapsed = System.currentTimeMillis() - start;

                log.info("create {} beforePaymentOrders in {} s", i + 1, elapsed / 1000.0);
            }
        }

        if (!batchOrder.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchOrder);
        }

        long finished = System.currentTimeMillis() - start;

        log.info("beforePaymentOrders batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 주문(결제 완료) 더미 데이터 생성
     */
    public void createDummyOrderWithItems(int totalOrderCount) {

        long start = System.currentTimeMillis();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<Long> buyerIds = jdbcTemplate.queryForList("SELECT id FROM buyers", Long.class);
        List<Map<String, Object>> products =
                jdbcTemplate.queryForList("SELECT id, seller_id, name FROM products");

        String itemSql = """
        INSERT INTO order_items
        (order_id, product_id, seller_id, product_name, unit_price, quantity, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        int createdOrderCount = 0;

        for (int i = 0; i < totalOrderCount; i += ORDER_BATCH_SIZE) {

            int currentBatchSize = Math.min(ORDER_BATCH_SIZE, totalOrderCount - i);

            List<Object[]> orderBatch = new ArrayList<>(currentBatchSize);
            List<List<Object[]>> itemBufferPerOrder = new ArrayList<>(currentBatchSize);

            for (int j = 0; j < currentBatchSize; j++) {

                Long buyerId = buyerIds.get(random.nextInt(buyerIds.size()));
                LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(1, 7));

                int itemCount = random.nextInt(1, 4);

                BigDecimal totalAmount = BigDecimal.ZERO;
                List<Object[]> itemBuffer = new ArrayList<>();

                for (int k = 0; k < itemCount; k++) {

                    Map<String, Object> product = products.get(random.nextInt(products.size()));

                    Long productId = ((Number) product.get("id")).longValue();
                    Long sellerId = ((Number) product.get("seller_id")).longValue();
                    String name = product.get("name").toString();

                    BigDecimal price = BigDecimal.valueOf(random.nextInt(1000, 10000));
                    int quantity = random.nextInt(1, 5);

                    totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));

                    itemBuffer.add(new Object[]{
                            productId, sellerId, name,
                            price, quantity,
                            createdAt, createdAt
                    });
                }

                orderBatch.add(new Object[]{
                        buyerId,
                        totalAmount,
                        OrderStatus.PAID.getStatus(),
                        UUID.randomUUID().toString(),
                        faker.name().fullName(),
                        faker.phoneNumber().phoneNumber(),
                        faker.address().fullAddress(),
                        createdAt,
                        createdAt
                });

                itemBufferPerOrder.add(itemBuffer);
            }

            StringBuilder sqlBuilder = new StringBuilder("""
            INSERT INTO orders
            (buyer_id, total_amount, status, tracking_number, recipient, phone, address, created_at, updated_at)
            VALUES
        """);

            List<Object> params = new ArrayList<>();

            for (int j = 0; j < orderBatch.size(); j++) {

                sqlBuilder.append("(?, ?, ?, ?, ?, ?, ?, ?, ?)");

                if (j < orderBatch.size() - 1) {
                    sqlBuilder.append(", ");
                }

                Object[] order = orderBatch.get(j);
                Collections.addAll(params, order);
            }

            sqlBuilder.append(" RETURNING id");

            List<Long> orderIds = jdbcTemplate.query(
                    sqlBuilder.toString(),
                    params.toArray(),
                    (rs, rowNum) -> rs.getLong(1)
            );

            List<Object[]> itemBatch = new ArrayList<>(ORDER_ITEM_BATCH_SIZE);

            for (int idx = 0; idx < orderIds.size(); idx++) {

                Long orderId = orderIds.get(idx);
                List<Object[]> items = itemBufferPerOrder.get(idx);

                for (Object[] item : items) {

                    itemBatch.add(new Object[]{
                            orderId,
                            item[0], item[1], item[2],
                            item[3], item[4], item[5], item[6]
                    });

                    if (itemBatch.size() == ORDER_ITEM_BATCH_SIZE) {
                        jdbcTemplate.batchUpdate(itemSql, itemBatch);
                        itemBatch.clear();
                    }
                }
            }

            if (!itemBatch.isEmpty()) {
                jdbcTemplate.batchUpdate(itemSql, itemBatch);
            }

            createdOrderCount += currentBatchSize;

            long elapsed = System.currentTimeMillis() - start;
            log.info("create {} orders in {} s", createdOrderCount, elapsed / 1000.0);
        }

        long finished = System.currentTimeMillis() - start;
        log.info("orders batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 결제 완료 더미 데이터 생성
     */
    public void createDummyPayment() {
        long start = System.currentTimeMillis();
        int count = 0;

        List<Object[]> batchPayment = new ArrayList<>(ORDER_BATCH_SIZE);

        List<Map<String, Object>> orderList = jdbcTemplate.queryForList("SELECT id, total_amount, created_at FROM orders WHERE status = 'PAID'"
        );

        String sql = """
                INSERT INTO payments
                (order_id, merchant_uid, amount, method, status, paid_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Map<String, Object> row : orderList) {

            Long orderId = ((Number) row.get("id")).longValue();
            String merchantUid = UUID.randomUUID().toString();
            BigDecimal amount = (BigDecimal) row.get("total_amount");
            String method = MethodEnum.MOCK.name();
            String status = PaymentStatus.SUCCESS.name();
            LocalDateTime orderDate = ((Timestamp) row.get("created_at")).toLocalDateTime();
            LocalDateTime paidAt = orderDate.plusMinutes(random.nextInt(1, 6));

            batchPayment.add(new Object[]{orderId, merchantUid, amount, method, status, paidAt, paidAt, paidAt});

            if (batchPayment.size() == ORDER_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batchPayment);
                batchPayment.clear();

                long elapsed = System.currentTimeMillis() - start;

                count = count + ORDER_BATCH_SIZE;

                log.info("create {} payments in {} s", count, elapsed / 1000.0);

            }
        }

        if (!batchPayment.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchPayment);
        }

        long finished = System.currentTimeMillis() - start;

        log.info("payments batchUpdate finished in {} s", finished / 1000.0);
    }

    /**
     * 일일 통계 더미 데이터 생성
     */
    public void createDummyDailyStatistics() {
        int count = 0;

        long start = System.currentTimeMillis();

        List<Object[]> batchDailyStatistics = new ArrayList<>(DAILY_STATISTICS_BATCH_SIZE);

        String sql = """
            SELECT oi.seller_id,
                   DATE(o.created_at) AS stat_date,
                   COUNT(DISTINCT o.id) AS total_orders,
                   SUM(oi.quantity) AS total_items,
                   SUM(oi.unit_price * oi.quantity) AS total_sales,
                   SUM(CASE WHEN r.status = 'SUCCESS' THEN 1 ELSE 0 END) AS total_refunds,
                   SUM(CASE WHEN r.status = 'SUCCESS' THEN oi.unit_price * oi.quantity ELSE 0 END) AS refund_amount
            FROM orders o
            JOIN order_items oi ON oi.order_id = o.id
            JOIN payments p ON p.order_id = o.id
            LEFT JOIN refunds r ON r.payment_id = p.id
            WHERE o.created_at >= NOW() - INTERVAL '7 days'
            GROUP BY oi.seller_id, DATE(o.created_at);
            """;

        List<Map<String, Object>> stats = jdbcTemplate.queryForList(sql);

        if (stats.isEmpty()) {
            log.warn("statistics 데이터 없음");
            return;
        }

        String insertSql = """
            INSERT INTO seller_daily_statistics
            (seller_id, stat_date, total_orders, total_items, total_sales, total_refunds, refund_amount, net_sales, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())
            """;

        for (Map<String, Object> row : stats) {

            Long sellerId = ((Number) row.get("seller_id")).longValue();
            LocalDate statDate = ((Date) row.get("stat_date")).toLocalDate();
            int totalOrders = ((Number) row.get("total_orders")).intValue();
            int totalItems = row.get("total_items") != null ? ((Number) row.get("total_items")).intValue() : 0;
            BigDecimal totalSales = row.get("total_sales") != null ? (BigDecimal) row.get("total_sales") : BigDecimal.ZERO;
            int totalRefunds = row.get("total_refunds") != null ? ((Number) row.get("total_refunds")).intValue() : 0;
            BigDecimal refundAmount = row.get("refund_amount") != null ? (BigDecimal) row.get("refund_amount") : BigDecimal.ZERO;
            BigDecimal netSales = totalSales.subtract(refundAmount);

            batchDailyStatistics.add(new Object[]{
                    sellerId, statDate, totalOrders, totalItems, totalSales, totalRefunds, refundAmount, netSales
            });

            if (batchDailyStatistics.size() == DAILY_STATISTICS_BATCH_SIZE) {
                jdbcTemplate.batchUpdate(insertSql, batchDailyStatistics);
                batchDailyStatistics.clear();

                long elapsed = System.currentTimeMillis() - start;

                count = count + DAILY_STATISTICS_BATCH_SIZE;

                log.info("create {} dailyStatistics in {} s", batchDailyStatistics.size(), elapsed / 1000.0);
            }
        }

        if (!batchDailyStatistics.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchDailyStatistics);
        }

        long finished = System.currentTimeMillis() - start;
        log.info("dailyStatistics batchUpdate finished in {} s", finished / 1000.0);
    }
}

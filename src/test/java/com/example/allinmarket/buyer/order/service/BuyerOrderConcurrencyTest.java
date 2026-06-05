package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.security.LoginRateLimitFilter;
import com.example.allinmarket.domain.address.entity.Address;
import com.example.allinmarket.domain.address.repository.AddressRepository;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cart.repository.CartRepository;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.cartitem.repository.CartItemRepository;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.repository.SellerRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BuyerOrderConcurrencyTest {

    private static final AtomicReference<CyclicBarrier> stockReadBarrier = new AtomicReference<>();

    @MockitoBean
    RedissonClient redissonClient;

    @MockitoBean
    LoginRateLimitFilter loginRateLimitFilter;

    @Autowired
    BuyerOrderService buyerOrderService;

    @Autowired
    BuyerRepository buyerRepository;

    @Autowired
    SellerRepository sellerRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        addressRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        sellerRepository.deleteAll();
        buyerRepository.deleteAll();
    }

    @Test
    void concurrentOrdersCanOversellSingleRemainingStockWithoutProductLock() throws Exception {
        Seller seller = sellerRepository.save(Seller.of(
                "seller-concurrency@test.com",
                "password",
                "판매자",
                "010-1000-1000",
                "동시성 상점",
                "biz-concurrency",
                "KOOKMIN",
                "1234567890"
        ));
        Category category = categoryRepository.save(Category.of("동시성 카테고리", 1));
        Product product = productRepository.save(Product.of(
                seller,
                category,
                "재고 1개 상품",
                BigDecimal.valueOf(10000),
                1,
                "동시 주문 재현용 상품"
        ));

        BuyerFixture firstBuyer = createBuyerFixture("first", product);
        BuyerFixture secondBuyer = createBuyerFixture("second", product);

        stockReadBarrier.set(new CyclicBarrier(2));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstOrder = executor.submit(createOrderTask(firstBuyer));
            Future<Boolean> secondOrder = executor.submit(createOrderTask(secondBuyer));

            assertThat(firstOrder.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondOrder.get(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            stockReadBarrier.set(null);
            executor.shutdownNow();
        }

        Product reloadedProduct = productRepository.findById(product.getId()).orElseThrow();
        int orderedQuantity = orderItemRepository.findAll().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        assertThat(orderRepository.count()).isEqualTo(2);
        assertThat(orderedQuantity).isEqualTo(2);
        assertThat(reloadedProduct.getStock()).isZero();
    }

    private Callable<Boolean> createOrderTask(BuyerFixture fixture) {
        return () -> {
            buyerOrderService.createOrder(
                    fixture.buyerId(),
                    List.of(fixture.cartItem()),
                    fixture.addressId()
            );
            return true;
        };
    }

    private BuyerFixture createBuyerFixture(String suffix, Product product) {
        Buyer buyer = buyerRepository.save(Buyer.of(
                "buyer-" + suffix + "@test.com",
                "password",
                "구매자 " + suffix,
                "010-2000-" + ("first".equals(suffix) ? "1000" : "2000")
        ));
        Address address = addressRepository.save(Address.of(
                buyer,
                "수령인 " + suffix,
                "010-3000-" + ("first".equals(suffix) ? "1000" : "2000"),
                "서울시 테스트구"
        ));
        Cart cart = cartRepository.save(Cart.of(buyer));
        CartItem cartItem = cartItemRepository.save(CartItem.of(cart, product));

        return new BuyerFixture(buyer.getId(), address.getId(), cartItem);
    }

    private record BuyerFixture(Long buyerId, Long addressId, CartItem cartItem) {
    }

    @TestConfiguration
    static class StockReadProbeConfig {

        @Bean
        StockReadProbeAspect stockReadProbeAspect() {
            return new StockReadProbeAspect();
        }
    }

    @Aspect
    static class StockReadProbeAspect {

        @Around("execution(* com.example.allinmarket.domain.product.repository.ProductRepository.findAllByIdInWithSellerWithLock(..))")
        Object waitUntilBothTransactionsReadStock(ProceedingJoinPoint joinPoint) throws Throwable {
            Object result = joinPoint.proceed();
            CyclicBarrier barrier = stockReadBarrier.get();
            if (barrier != null) {
                barrier.await(3, TimeUnit.SECONDS);
            }
            return result;
        }
    }
}

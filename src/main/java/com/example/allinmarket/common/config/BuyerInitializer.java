package com.example.allinmarket.common.config;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cart.repository.CartRepository;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.core.annotation.Order(2)
@ConditionalOnProperty(name = "app.init.admin.enabled", havingValue = "true", matchIfMissing = true)
public class BuyerInitializer implements ApplicationRunner {

    private static final List<String[]> BUYERS = List.of(
            new String[]{"buyer1@example.com", "buyer1234!", "김구매", "010-1111-2222"},
            new String[]{"buyer2@example.com", "buyer1234!", "이쇼핑", "010-3333-4444"}
    );

    private final BuyerRepository buyerRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${seller.email}")
    private String sellerEmail;

    @Override
    public void run(ApplicationArguments args) {
        Seller seller = sellerRepository.findByEmailAndDeletedAtIsNull(sellerEmail).orElse(null);
        if (seller == null) {
            log.warn("판매자 계정을 찾을 수 없어 구매자 예시 데이터를 생성하지 않습니다.");
            return;
        }

        List<Product> products = productRepository
                .findAllBySellerIdAndDeletedAtIsNull(seller.getId(), PageRequest.of(0, BUYERS.size()))
                .getContent();

        if (products.isEmpty()) {
            log.warn("상품이 없어 구매자 예시 데이터를 생성하지 않습니다.");
            return;
        }

        for (int i = 0; i < BUYERS.size(); i++) {
            String[] info = BUYERS.get(i);
            String email = info[0];
            String password = info[1];
            String name = info[2];
            String phone = info[3];

            if (buyerRepository.existsByEmail(email)) {
                continue;
            }

            Buyer buyer = Buyer.of(email, passwordEncoder.encode(password), name, phone);
            buyerRepository.save(buyer);
            log.info("구매자 계정이 생성되었습니다: {}", email);

            cartRepository.save(Cart.of(buyer));
            log.info("장바구니가 생성되었습니다: buyer={}", email);

            Product product = products.get(i % products.size());

            Order order = Order.of(
                    buyer,
                    product.getPrice().multiply(BigDecimal.valueOf(2)),
                    null,
                    name,
                    phone,
                    "서울특별시 강남구 테헤란로 123"
            );
            orderRepository.save(order);
            log.info("주문이 생성되었습니다: buyer={}, orderId={}", email, order.getId());

            OrderItem orderItem = OrderItem.of(
                    order,
                    product,
                    seller,
                    product.getName(),
                    product.getPrice(),
                    2
            );
            orderItemRepository.save(orderItem);
            log.info("주문 상품이 생성되었습니다: product={}, quantity={}", product.getName(), orderItem.getQuantity());
        }
    }
}

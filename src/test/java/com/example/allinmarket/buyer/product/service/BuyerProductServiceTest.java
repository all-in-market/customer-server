package com.example.allinmarket.buyer.product.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class BuyerProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BuyerProductService buyerProductService;

    @Test
    void 구매자_상품_목록_조회_성공_테스트() {
        // given
        Seller seller = mock(Seller.class);
        Category category = mock(Category.class);

        Product product = Product.of(
                seller,
                category,
                "테스트",
                BigDecimal.valueOf(10000),
                50,
                "설명"
        );

        ReflectionTestUtils.setField(product, "id", 1L);

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(product), pageable, 1);

        given(productRepository.findAllVisibleProducts(pageable)).willReturn(productPage);

        // when
        Page<ProductDetailResponse> responses = buyerProductService.findAllProducts(pageable);

        // then
        assertEquals(1, responses.getTotalElements());
        assertEquals("테스트", responses.getContent().get(0).name());
    }

    @Test
    void 구매자_상품_목록_조회_실패_테스트() {
        //given
        given(productRepository.findAllVisibleProducts(any(Pageable.class))).willThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

        // when & then
        assertThrows(BaseException.class, () -> buyerProductService.findAllProducts(PageRequest.of(0, 10)));
    }

    @Test
    void 상품_상세_조회_성공_테스트() {
        // given
        Seller seller = mock(Seller.class);
        Category category = mock(Category.class);

        Product product = Product.of(
                seller,
                category,
                "테스트",
                BigDecimal.valueOf(10000),
                50,
                "설명"
        );

        ReflectionTestUtils.setField(product, "id", 1L);

        given(productRepository.findVisibleProductById(1L)).willReturn(Optional.of(product));

        // when
        ProductDetailResponse response = buyerProductService.findOneProduct(1L);

        // then
        assertEquals(1L, response.id());
        assertEquals("테스트", response.name());
    }

    @Test
    void 상품_상세_조회_실패_테스트() {
        //given
        given(productRepository.findVisibleProductById(any())).willThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        // when & then
        assertThrows(BaseException.class, () -> buyerProductService.findOneProduct(1L));
    }
}

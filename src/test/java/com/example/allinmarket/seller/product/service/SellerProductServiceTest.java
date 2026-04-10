package com.example.allinmarket.seller.product.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import com.example.allinmarket.domain.product.dto.ProductDetailResponse;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.product.dto.request.SellerProductCreateRequest;
import com.example.allinmarket.seller.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class SellerProductServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SellerProductService sellerProductService;

    @Test
    void 판매자_상품_등록_성공_테스트() {
        // given
        Long sellerId = 1L;

        Seller seller = mock(Seller.class);
        ReflectionTestUtils.setField(seller, "id", sellerId);

        Category category = mock(Category.class);
        given(category.getId()).willReturn(1L);

        Product product = Product.of(
                seller,
                category,
                "테스트 상품",
                BigDecimal.valueOf(10000),
                50,
                "상품 설명"
        );
        ReflectionTestUtils.setField(product, "id", 1L);

        SellerProductCreateRequest request = new SellerProductCreateRequest(
                1L,
                "테스트 상품",
                BigDecimal.valueOf(10000),
                50,
                "상품 설명"
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);

            given(sellerRepository.findById(sellerId)).willReturn(Optional.of(seller));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(product);

            // when
            ProductDetailResponse response = sellerProductService.create(request);

            // then
            assertNotNull(response);
            assertEquals("테스트 상품", response.name());
            assertEquals(BigDecimal.valueOf(10000), response.price());
            assertEquals(50, response.stock());
            assertEquals(ProductStatus.ON_SALE, response.status());
            assertEquals("상품 설명", response.description());
        }
    }

    @Test
    void 판매자_상품_등록_판매자_없음_실패_테스트() {
        // given
        Long sellerId = 1L;

        SellerProductCreateRequest request = new SellerProductCreateRequest(
                null,
                "테스트 상품",
                BigDecimal.valueOf(10000),
                50,
                "상품 설명"
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);

            given(sellerRepository.findById(sellerId)).willReturn(Optional.empty());

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.create(request)
            );

            assertEquals(ErrorEnum.USER_NOT_FOUND, exception.getErrorEnum());
        }
    }

    @Test
    void 판매자_상품_등록_카테고리_없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Seller seller = mock(Seller.class);

        SellerProductCreateRequest request = new SellerProductCreateRequest(
                999L,
                "테스트 상품",
                BigDecimal.valueOf(10000),
                50,
                "상품 설명"
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);

            given(sellerRepository.findById(sellerId)).willReturn(Optional.of(seller));
            given(categoryRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.create(request)
            );

            assertEquals(ErrorEnum.CATEGORY_NOT_FOUND, exception.getErrorEnum());
        }
    }

    @Test
    void 판매자_상품_등록_저장_실패_테스트() {
        // given
        Long sellerId = 1L;

        Seller seller = mock(Seller.class);
        Category category = mock(Category.class);

        SellerProductCreateRequest request = new SellerProductCreateRequest(
                1L,
                "테스트 상품",
                BigDecimal.valueOf(10000),
                50,
                "상품 설명"
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);

            given(sellerRepository.findById(sellerId)).willReturn(Optional.of(seller));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class)))
                    .willThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

            // when & then
            assertThrows(BaseException.class, () -> sellerProductService.create(request));
        }
    }
}
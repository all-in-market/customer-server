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
import com.example.allinmarket.seller.product.dto.request.SellerProductStockUpdateRequest;
import com.example.allinmarket.seller.product.dto.request.SellerProductUpdateRequest;
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

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId)).willReturn(Optional.of(seller));
            given(categoryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class))).willReturn(product);

            // when
            ProductDetailResponse response = sellerProductService.create(sellerId, request);

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

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId)).willReturn(Optional.empty());

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.create(sellerId, request)
            );

            assertEquals(ErrorEnum.SELLER_NOT_FOUND, exception.getErrorEnum());
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

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId)).willReturn(Optional.of(seller));
            given(categoryRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.create(sellerId, request)
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

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId)).willReturn(Optional.of(seller));
            given(categoryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(category));
            given(productRepository.save(any(Product.class)))
                    .willThrow(new BaseException(ErrorEnum.INTERNAL_SERVER_ERROR));

            // when & then
            assertThrows(BaseException.class, () -> sellerProductService.create(sellerId, request));
        }
    }

    @Test
    void 판매자_상품_수정_성공_테스트() {
        // given
        Long sellerId = 1L;
        Long productId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        Category category = mock(Category.class);
        given(category.getId()).willReturn(1L);

        Product product = Product.of(
                seller,
                category,
                "기존 상품",
                BigDecimal.valueOf(10000),
                50,
                "기존 설명"
        );
        ReflectionTestUtils.setField(product, "id", productId);

        SellerProductUpdateRequest request = new SellerProductUpdateRequest(
                1L,
                "수정된 상품",
                BigDecimal.valueOf(20000),
                ProductStatus.ON_SALE,
                "수정된 설명"
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);

            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));
            given(categoryRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(category));

            // when
            ProductDetailResponse response = sellerProductService.update(sellerId, productId, request);

            // then
            assertNotNull(response);
            assertEquals("수정된 상품", response.name());
            assertEquals(BigDecimal.valueOf(20000), response.price());
            assertEquals(ProductStatus.ON_SALE, response.status());
            assertEquals("수정된 설명", response.description());
        }
    }

    @Test
    void 판매자_상품_수정_일부필드만_수정_성공_테스트() {
        // given - name만 수정, 나머지는 null
        Long sellerId = 1L;
        Long productId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        Category category = mock(Category.class);

        Product product = Product.of(
                seller,
                category,
                "기존 상품",
                BigDecimal.valueOf(10000),
                50,
                "기존 설명"
        );
        ReflectionTestUtils.setField(product, "id", productId);

        SellerProductUpdateRequest request = new SellerProductUpdateRequest(
                null, "수정된 상품", null, null, null
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

            // when
            ProductDetailResponse response = sellerProductService.update(sellerId, productId, request);

            // then
            assertEquals("수정된 상품", response.name());
            assertEquals(BigDecimal.valueOf(10000), response.price()); // 기존값 유지
            assertEquals("기존 설명", response.description());         // 기존값 유지
        }
    }

    @Test
    void 판매자_상품_수정_상품없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long productId = 999L;

        SellerProductUpdateRequest request = new SellerProductUpdateRequest(
                null, "수정된 상품", null, null, null
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.update(sellerId, productId, request)
            );
            assertEquals(ErrorEnum.PRODUCT_NOT_FOUND, exception.getErrorEnum());
        }
    }

    @Test
    void 판매자_상품_수정_권한없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long otherSellerId = 2L; // 다른 판매자
        Long productId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(otherSellerId); // 상품 소유자는 2L

        Category category = mock(Category.class);
        Product product = Product.of(seller, category, "기존 상품",
                BigDecimal.valueOf(10000), 50, "기존 설명");
        ReflectionTestUtils.setField(product, "id", productId);

        SellerProductUpdateRequest request = new SellerProductUpdateRequest(
                null, "수정된 상품", null, null, null
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId); // 요청자는 1L
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.update(sellerId, productId, request)
            );
            assertEquals(ErrorEnum.FORBIDDEN, exception.getErrorEnum());
        }
    }

    @Test
    void 판매자_상품_수정_카테고리없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long productId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        Category category = mock(Category.class);
        Product product = Product.of(seller, category, "기존 상품",
                BigDecimal.valueOf(10000), 50, "기존 설명");
        ReflectionTestUtils.setField(product, "id", productId);

        SellerProductUpdateRequest request = new SellerProductUpdateRequest(
                999L, null, null, null, null // 존재하지 않는 categoryId
        );

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));
            given(categoryRepository.findByIdAndDeletedAtIsNull(999L)).willReturn(Optional.empty());

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.update(sellerId, productId, request)
            );
            assertEquals(ErrorEnum.CATEGORY_NOT_FOUND, exception.getErrorEnum());
        }
    }

    @Test
    void 판매자_상품_삭제_성공_테스트() {
        // given
        Long sellerId = 1L;
        Long productId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        Category category = mock(Category.class);
        Product product = Product.of(
                seller, category, "테스트 상품",
                BigDecimal.valueOf(10000), 50, "상품 설명"
        );
        ReflectionTestUtils.setField(product, "id", productId);

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

            // when
            ProductDetailResponse response = sellerProductService.delete(sellerId, productId);

            // then
            assertNotNull(response);
            assertEquals("테스트 상품", response.name());
        }
    }

    @Test
    void 판매자_상품_삭제_상품없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long productId = 999L;

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId);
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.delete(sellerId, productId)
            );
            assertEquals(ErrorEnum.PRODUCT_NOT_FOUND, exception.getErrorEnum());
        }
    }

    @Test
    void 판매자_상품_삭제_권한없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long otherSellerId = 2L;
        Long productId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(otherSellerId); // 상품 소유자는 2L

        Category category = mock(Category.class);
        Product product = Product.of(
                seller, category, "테스트 상품",
                BigDecimal.valueOf(10000), 50, "상품 설명"
        );
        ReflectionTestUtils.setField(product, "id", productId);

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(sellerId); // 요청자는 1L
            given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

            // when & then
            BaseException exception = assertThrows(
                    BaseException.class,
                    () -> sellerProductService.delete(sellerId, productId)
            );
            assertEquals(ErrorEnum.FORBIDDEN, exception.getErrorEnum());
        }
    }

    @Test
    void 판매자_상품_재고수정_성공_테스트() {
        // given
        Long sellerId = 1L;
        Long productId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        Category category = mock(Category.class);
        Product product = Product.of(
                seller, category, "테스트 상품",
                BigDecimal.valueOf(10000), 50, "상품 설명"
        );
        ReflectionTestUtils.setField(product, "id", productId);

        SellerProductStockUpdateRequest request = new SellerProductStockUpdateRequest(100);

        given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

        // when
        ProductDetailResponse response = sellerProductService.stockUpdate(sellerId, productId, request);

        // then
        assertNotNull(response);
        assertEquals(100, response.stock());
    }

    @Test
    void 판매자_상품_재고수정_상품없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long productId = 999L;

        SellerProductStockUpdateRequest request = new SellerProductStockUpdateRequest(100);

        given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> sellerProductService.stockUpdate(sellerId, productId, request)
        );
        assertEquals(ErrorEnum.PRODUCT_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 판매자_상품_재고수정_권한없음_실패_테스트() {
        // given
        Long sellerId = 1L;
        Long otherSellerId = 2L;
        Long productId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(otherSellerId); // 상품 소유자는 2L

        Category category = mock(Category.class);
        Product product = Product.of(
                seller, category, "테스트 상품",
                BigDecimal.valueOf(10000), 50, "상품 설명"
        );
        ReflectionTestUtils.setField(product, "id", productId);

        SellerProductStockUpdateRequest request = new SellerProductStockUpdateRequest(100);

        given(productRepository.findByIdAndDeletedAtIsNull(productId)).willReturn(Optional.of(product));

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> sellerProductService.stockUpdate(sellerId, productId, request)
        );
        assertEquals(ErrorEnum.FORBIDDEN, exception.getErrorEnum());
    }
}
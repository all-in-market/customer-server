package com.example.allinmarket.buyer.me.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.me.dto.request.BuyerUpdateRequest;
import com.example.allinmarket.buyer.me.dto.response.BuyerDetailResponse;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuyerMeServiceTest {

    @Mock
    private BuyerRepository buyerRepository;

    @InjectMocks
    private BuyerMeService buyerMeService;

    @Nested
    @DisplayName("내 정보 수정")
    class UpdateMeTest {

        @Test
        @DisplayName("내 정보 수정 성공")
        void updateMe_success() {
            // given
            Long buyerId = 1L;
            BuyerUpdateRequest request = new BuyerUpdateRequest(
                    null,
                    null,
                    "수정이름",
                    "010-9999-8888"
            );

            Buyer buyer = createBuyer(buyerId, "기존이름", "010-1111-2222");

            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId))
                    .willReturn(Optional.of(buyer));

            // when
            BuyerDetailResponse result = buyerMeService.updateMyProfile(buyerId, request);

            // then
            assertThat(result.name()).isEqualTo("수정이름");
            assertThat(result.phone()).isEqualTo("010-9999-8888");
            assertThat(buyer.getName()).isEqualTo("수정이름");
            assertThat(buyer.getPhone()).isEqualTo("010-9999-8888");

            verify(buyerRepository).findByIdAndDeletedAtIsNull(buyerId);
        }

        @Test
        @DisplayName("구매자가 없으면 예외 발생")
        void updateMe_fail_whenBuyerNotFound() {
            // given
            Long buyerId = 1L;
            BuyerUpdateRequest request = new BuyerUpdateRequest(
                    null,
                    null,
                    "수정이름",
                    "010-9999-8888"
            );

            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> buyerMeService.updateMyProfile(buyerId, request))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorEnum", ErrorEnum.BUYER_NOT_FOUND);

            verify(buyerRepository).findByIdAndDeletedAtIsNull(buyerId);
        }

        @Test
        @DisplayName("이름만 수정되고 전화번호는 수정되지 않는다")
        void updateMyProfile_onlyNameUpdated() {
            // given
            Long buyerId = 1L;

            Buyer buyer = createBuyer(buyerId, "기존이름", "010-1111-2222");

            BuyerUpdateRequest request = new BuyerUpdateRequest(
                    null,
                    null,
                    "홍길동",
                    null
            );

            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId))
                    .willReturn(Optional.of(buyer));

            // when
            BuyerDetailResponse result = buyerMeService.updateMyProfile(buyerId, request);

            // then
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.phone()).isEqualTo("010-1111-2222");
            assertThat(buyer.getName()).isEqualTo("홍길동");
            assertThat(buyer.getPhone()).isEqualTo("010-1111-2222");

            verify(buyerRepository).findByIdAndDeletedAtIsNull(buyerId);
        }

        @Test
        @DisplayName("변경하려는 이메일이 이미 존재하면 예외 발생")
        void updateMyProfile_fail_whenEmailAlreadyExists() {
            // given
            Long buyerId = 1L;
            Buyer buyer = createBuyer(buyerId, "기존이름", "010-1111-2222");

            BuyerUpdateRequest request = new BuyerUpdateRequest(
                    "new@example.com",
                    null,
                    null,
                    null
            );

            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId))
                    .willReturn(Optional.of(buyer));
            given(buyerRepository.existsByEmail("new@example.com"))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> buyerMeService.updateMyProfile(buyerId, request))
                    .isInstanceOf(BaseException.class)
                    .hasFieldOrPropertyWithValue("errorEnum", ErrorEnum.EMAIL_ALREADY_EXISTS);

            verify(buyerRepository).findByIdAndDeletedAtIsNull(buyerId);
            verify(buyerRepository).existsByEmail("new@example.com");
        }

        private Buyer createBuyer(Long id, String name, String phone) {
            Buyer buyer = Buyer.of(
                    "test@example.com",
                    "encodedPassword",
                    name,
                    phone
            );

            setField(buyer, "id", id);
            return buyer;
        }

        private void setField(Object target, String fieldName, Object value) {
            try {
                Field field = target.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
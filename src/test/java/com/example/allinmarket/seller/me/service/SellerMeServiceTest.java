package com.example.allinmarket.seller.me.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.me.dto.request.SellerUpdateRequest;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import com.example.allinmarket.seller.repository.SellerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellerMeServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SellerMeService sellerMeService;

    @Nested
    @DisplayName("내 정보 조회")
    class GetMyProfileTest {

        @Test
        @DisplayName("판매자 본인 정보 조회 성공")
        void getMyProfile_success() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "test@test.com", "홍길동", "010-1111-2222",
                    "가게이름", "123-45-67890", "KAKAO", "1234567890");

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));

            // when
            SellerDetailResponse result = sellerMeService.getMyProfile(sellerId);

            // then
            assertThat(result.id()).isEqualTo(sellerId);
            assertThat(result.email()).isEqualTo("test@test.com");
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.phone()).isEqualTo("010-1111-2222");
            assertThat(result.storeName()).isEqualTo("가게이름");
            assertThat(result.bizNumber()).isEqualTo("123-45-67890");

            verify(sellerRepository).findByIdAndDeletedAtIsNull(sellerId);
        }

        @Test
        @DisplayName("판매자가 없으면 예외 발생")
        void getMyProfile_fail_whenSellerNotFound() {
            // given
            Long sellerId = 1L;

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerMeService.getMyProfile(sellerId))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum").isEqualTo(ErrorEnum.SELLER_NOT_FOUND);

            verify(sellerRepository).findByIdAndDeletedAtIsNull(sellerId);
        }
    }

    @Nested
    @DisplayName("내 정보 수정")
    class UpdateMyProfileTest {

        @Test
        @DisplayName("판매자가 없으면 예외 발생")
        void updateMyProfile_fail_whenSellerNotFound() {
            // given
            Long sellerId = 1L;
            SellerUpdateRequest request = new SellerUpdateRequest(
                    null, null, "수정이름", null, null, null, null, null);

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerMeService.updateMyProfile(sellerId, request))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum").isEqualTo(ErrorEnum.SELLER_NOT_FOUND);

            verify(sellerRepository).findByIdAndDeletedAtIsNull(sellerId);
        }

        @Test
        @DisplayName("이름만 수정되고 나머지 필드는 그대로 유지된다")
        void updateMyProfile_onlyNameUpdated_thenOtherFieldsUnchanged() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "test@test.com", "기존이름", "010-1111-2222",
                    "가게이름", "123-45-67890", "KAKAO", "1234567890");

            SellerUpdateRequest request = new SellerUpdateRequest(
                    null, null, "홍길동", null, null, null, null, null);

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));

            // when
            SellerDetailResponse result = sellerMeService.updateMyProfile(sellerId, request);

            // then
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(seller.getName()).isEqualTo("홍길동");
            assertThat(seller.getEmail()).isEqualTo("test@test.com");
            assertThat(seller.getPhone()).isEqualTo("010-1111-2222");
            assertThat(seller.getStoreName()).isEqualTo("가게이름");
            assertThat(seller.getBizNumber()).isEqualTo("123-45-67890");
            assertThat(seller.getBankCode()).isEqualTo("KAKAO");
            assertThat(seller.getBankAccount()).isEqualTo("1234567890");

            verify(sellerRepository, never()).existsByEmail(org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("이메일이 변경되고 중복이 아니면 정상 반영된다")
        void updateMyProfile_whenEmailChangedAndNotDuplicated_thenUpdated() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "old@test.com", "홍길동", "010-1111-2222",
                    "가게이름", "123-45-67890", "KAKAO", "1234567890");

            SellerUpdateRequest request = new SellerUpdateRequest(
                    "new@test.com", null, null, null, null, null, null, null);

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));
            given(sellerRepository.existsByEmail("new@test.com")).willReturn(false);

            // when
            SellerDetailResponse result = sellerMeService.updateMyProfile(sellerId, request);

            // then
            assertThat(result.email()).isEqualTo("new@test.com");
            assertThat(seller.getEmail()).isEqualTo("new@test.com");

            verify(sellerRepository).existsByEmail("new@test.com");
        }

        @Test
        @DisplayName("이메일이 기존 값과 동일하면 중복 검사를 하지 않는다")
        void updateMyProfile_whenEmailSameAsBefore_thenSkipDuplicateCheck() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "test@test.com", "홍길동", "010-1111-2222",
                    "가게이름", "123-45-67890", "KAKAO", "1234567890");

            SellerUpdateRequest request = new SellerUpdateRequest(
                    "test@test.com", null, null, null, null, null, null, null);

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));

            // when
            SellerDetailResponse result = sellerMeService.updateMyProfile(sellerId, request);

            // then
            assertThat(result.email()).isEqualTo("test@test.com");
            verify(sellerRepository, never()).existsByEmail(org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("변경하려는 이메일이 이미 존재하면 예외 발생")
        void updateMyProfile_fail_whenEmailAlreadyExists() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "old@test.com", "홍길동", "010-1111-2222",
                    "가게이름", "123-45-67890", "KAKAO", "1234567890");

            SellerUpdateRequest request = new SellerUpdateRequest(
                    "dup@test.com", null, null, null, null, null, null, null);

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));
            given(sellerRepository.existsByEmail("dup@test.com")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sellerMeService.updateMyProfile(sellerId, request))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum").isEqualTo(ErrorEnum.EMAIL_ALREADY_EXISTS);

            assertThat(seller.getEmail()).isEqualTo("old@test.com");
            verify(sellerRepository).existsByEmail("dup@test.com");
        }

        @Test
        @DisplayName("비밀번호가 주어지면 인코딩된 값으로 저장된다")
        void updateMyProfile_whenPasswordProvided_thenEncodedAndStored() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "test@test.com", "홍길동", "010-1111-2222",
                    "가게이름", "123-45-67890", "KAKAO", "1234567890");

            SellerUpdateRequest request = new SellerUpdateRequest(
                    null, "newPassword1", null, null, null, null, null, null);

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));
            given(passwordEncoder.encode("newPassword1")).willReturn("encodedPassword");

            // when
            sellerMeService.updateMyProfile(sellerId, request);

            // then
            assertThat(seller.getPassword()).isEqualTo("encodedPassword");
            verify(passwordEncoder).encode("newPassword1");
        }

        @Test
        @DisplayName("모든 필드가 채워지면 전부 반영된다")
        void updateMyProfile_whenAllFieldsProvided_thenAllUpdated() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "old@test.com", "기존이름", "010-1111-2222",
                    "기존가게", "111-11-11111", "KAKAO", "1111111111");

            SellerUpdateRequest request = new SellerUpdateRequest(
                    "new@test.com",
                    "newPassword1",
                    "새이름",
                    "010-9999-8888",
                    "새가게",
                    "222-22-22222",
                    "TOSS",
                    "2222222222"
            );

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));
            given(sellerRepository.existsByEmail("new@test.com")).willReturn(false);
            given(passwordEncoder.encode("newPassword1")).willReturn("encodedPassword");

            // when
            SellerDetailResponse result = sellerMeService.updateMyProfile(sellerId, request);

            // then
            assertThat(result.email()).isEqualTo("new@test.com");
            assertThat(result.name()).isEqualTo("새이름");
            assertThat(result.phone()).isEqualTo("010-9999-8888");
            assertThat(result.storeName()).isEqualTo("새가게");
            assertThat(result.bizNumber()).isEqualTo("222-22-22222");

            assertThat(seller.getEmail()).isEqualTo("new@test.com");
            assertThat(seller.getPassword()).isEqualTo("encodedPassword");
            assertThat(seller.getName()).isEqualTo("새이름");
            assertThat(seller.getPhone()).isEqualTo("010-9999-8888");
            assertThat(seller.getStoreName()).isEqualTo("새가게");
            assertThat(seller.getBizNumber()).isEqualTo("222-22-22222");
            assertThat(seller.getBankCode()).isEqualTo("TOSS");
            assertThat(seller.getBankAccount()).isEqualTo("2222222222");
        }

        @Test
        @DisplayName("유효하지 않은 은행 코드로 수정하면 예외 발생")
        void updateMyProfile_fail_whenBankCodeInvalid() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "test@test.com", "홍길동", "010-1111-2222",
                    "가게이름", "123-45-67890", "KAKAO", "1234567890");

            SellerUpdateRequest request = new SellerUpdateRequest(
                    null, null, null, null, null, null, "INVALID_BANK", null);

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));

            // when & then
            assertThatThrownBy(() -> sellerMeService.updateMyProfile(sellerId, request))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum").isEqualTo(ErrorEnum.BANK_CODE_INVALID);

            assertThat(seller.getBankCode()).isEqualTo("KAKAO");
        }

        @Test
        @DisplayName("빈 문자열/공백 필드는 수정되지 않고 기존 값이 유지된다")
        void updateMyProfile_whenBlankFields_thenValuesUnchanged() {
            // given
            Long sellerId = 1L;
            Seller seller = createSeller(sellerId, "test@test.com", "기존이름", "010-1111-2222",
                    "가게이름", "123-45-67890", "KAKAO", "1234567890");

            SellerUpdateRequest request = new SellerUpdateRequest(
                    "", "   ", "", "", "", "", "", "");

            given(sellerRepository.findByIdAndDeletedAtIsNull(sellerId))
                    .willReturn(Optional.of(seller));

            // when
            SellerDetailResponse result = sellerMeService.updateMyProfile(sellerId, request);

            // then
            assertThat(result.email()).isEqualTo("test@test.com");
            assertThat(result.name()).isEqualTo("기존이름");
            assertThat(seller.getEmail()).isEqualTo("test@test.com");
            assertThat(seller.getName()).isEqualTo("기존이름");
            assertThat(seller.getPhone()).isEqualTo("010-1111-2222");
            assertThat(seller.getStoreName()).isEqualTo("가게이름");
            assertThat(seller.getBizNumber()).isEqualTo("123-45-67890");
            assertThat(seller.getBankCode()).isEqualTo("KAKAO");
            assertThat(seller.getBankAccount()).isEqualTo("1234567890");

            verify(sellerRepository, never()).existsByEmail(org.mockito.ArgumentMatchers.anyString());
            verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
        }
    }

    private Seller createSeller(
            Long id,
            String email,
            String name,
            String phone,
            String storeName,
            String bizNumber,
            String bankCode,
            String bankAccount
    ) {
        Seller seller = Seller.of(
                email,
                "encodedPassword",
                name,
                phone,
                storeName,
                bizNumber,
                bankCode,
                bankAccount
        );
        ReflectionTestUtils.setField(seller, "id", id);
        return seller;
    }
}

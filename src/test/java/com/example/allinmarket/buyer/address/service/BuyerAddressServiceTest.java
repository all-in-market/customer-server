package com.example.allinmarket.buyer.address.service;

import com.example.allinmarket.buyer.address.dto.request.AddressCreateRequest;
import com.example.allinmarket.buyer.address.dto.request.AddressUpdateRequest;
import com.example.allinmarket.buyer.address.dto.response.AddressDetailResponse;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.address.entity.Address;
import com.example.allinmarket.domain.address.repository.AddressRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerAddressServiceTest {

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private BuyerAddressService buyerAddressService;

    @Nested
    @DisplayName("주소 생성")
    class CreateAddressTest{
        @Test
        @DisplayName("기본 주소가 없으면 새 주소는 기본 주소로 저장된다")
        void createAddress_whenNoDefaultAddress_thenSaveAsDefault() {
            // given
            Long buyerId = 1L;
            Buyer buyer = org.mockito.Mockito.mock(Buyer.class);
            AddressCreateRequest request = new AddressCreateRequest(
                    "홍길동",
                    "010-1234-5678",
                    "서울시 강남구 테헤란로 123"
            );

            given(buyerRepository.findById(buyerId)).willReturn(Optional.of(buyer));
            given(buyer.getId()).willReturn(buyerId);
            given(addressRepository.existsByBuyerIdAndIsDefaultTrue(buyerId)).willReturn(false);
            given(addressRepository.save(org.mockito.ArgumentMatchers.any(Address.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            AddressDetailResponse result = buyerAddressService.createAddress(buyerId, request);

            // then
            ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
            verify(addressRepository).save(captor.capture());

            Address savedAddress = captor.getValue();

            assertThat(savedAddress.getBuyer()).isEqualTo(buyer);
            assertThat(savedAddress.getRecipient()).isEqualTo("홍길동");
            assertThat(savedAddress.getPhone()).isEqualTo("010-1234-5678");
            assertThat(savedAddress.getDetail()).isEqualTo("서울시 강남구 테헤란로 123");
            assertThat(savedAddress.isDefault()).isTrue();

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("기본 주소가 이미 있으면 새 주소는 기본 주소가 아니다")
        void createAddress_whenDefaultAddressExists_thenSaveAsNonDefault() {
            // given
            Long buyerId = 1L;
            Buyer buyer = org.mockito.Mockito.mock(Buyer.class);
            AddressCreateRequest request = new AddressCreateRequest(
                    "홍길동",
                    "010-1234-5678",
                    "서울시 강남구 테헤란로 123"
            );

            given(buyerRepository.findById(buyerId)).willReturn(Optional.of(buyer));
            given(buyer.getId()).willReturn(buyerId);
            given(addressRepository.existsByBuyerIdAndIsDefaultTrue(buyerId)).willReturn(true);
            given(addressRepository.save(org.mockito.ArgumentMatchers.any(Address.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            AddressDetailResponse result = buyerAddressService.createAddress(buyerId, request);

            // then
            ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
            verify(addressRepository).save(captor.capture());

            Address savedAddress = captor.getValue();

            assertThat(savedAddress.getBuyer()).isEqualTo(buyer);
            assertThat(savedAddress.getRecipient()).isEqualTo("홍길동");
            assertThat(savedAddress.getPhone()).isEqualTo("010-1234-5678");
            assertThat(savedAddress.getDetail()).isEqualTo("서울시 강남구 테헤란로 123");
            assertThat(savedAddress.isDefault()).isFalse();

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("주소지 목록 조회")
    class GetAllAddressesTest {

        @Test
        @DisplayName("구매자 본인 배송지 목록 조회 성공")
        void getAllAddresses_success() {
            // given
            Long currentUserId = 1L;

            Buyer buyer = createBuyer(currentUserId);

            Address address1 = createAddress(
                    1L, buyer, "홍길동", "010-1111-2222", "서울시 강남구", true
            );
            Address address2 = createAddress(
                    2L, buyer, "김철수", "010-3333-4444", "서울시 서초구", false
            );

            given(addressRepository.findAllByBuyerId(currentUserId))
                    .willReturn(List.of(address1, address2));

            // when
            List<AddressDetailResponse> result = buyerAddressService.getAllAddresses(currentUserId);

            // then
            assertThat(result).hasSize(2);

            AddressDetailResponse res1 = result.get(0);
            assertThat(res1.addressId()).isEqualTo(1L);
            assertThat(res1.buyerId()).isEqualTo(currentUserId);
            assertThat(res1.recipient()).isEqualTo("홍길동");
            assertThat(res1.phone()).isEqualTo("010-1111-2222");
            assertThat(res1.detail()).isEqualTo("서울시 강남구");
            assertThat(res1.isDefault()).isTrue();

            AddressDetailResponse res2 = result.get(1);
            assertThat(res2.addressId()).isEqualTo(2L);
            assertThat(res2.buyerId()).isEqualTo(currentUserId);
            assertThat(res2.recipient()).isEqualTo("김철수");
            assertThat(res2.phone()).isEqualTo("010-3333-4444");
            assertThat(res2.detail()).isEqualTo("서울시 서초구");
            assertThat(res2.isDefault()).isFalse();

            verify(addressRepository).findAllByBuyerId(currentUserId);
        }

        @Test
        @DisplayName("배송지가 없으면 빈 리스트를 반환한다")
        void getAllAddresses_empty() {
            // given
            Long currentUserId = 1L;

            given(addressRepository.findAllByBuyerId(currentUserId))
                    .willReturn(Collections.emptyList());

            // when
            List<AddressDetailResponse> result = buyerAddressService.getAllAddresses(currentUserId);

            // then
            assertThat(result).isEmpty();
            verify(addressRepository).findAllByBuyerId(currentUserId);
        }
    }

    private Buyer createBuyer(Long id) {
        Buyer buyer = Buyer.of(
                "test@test.com",
                "1234",
                "홍길동",
                "010-0000-0000"
        );
        ReflectionTestUtils.setField(buyer, "id", id);
        return buyer;
    }

    @Nested
    @DisplayName("주소지 수정")
    class UpdateAddressTest {
        @Test
        @DisplayName("주소 수정 성공 - 실제 값 변경 검증")
        void updateAddress_success_updateFields_realObject() {
            // given
            Long currentUserId = 1L;
            Long addressId = 10L;

            Buyer buyer = mock(Buyer.class);

            Address address = Address.of(buyer, "기존이름", "010-0000-0000", "기존주소");

            given(addressRepository.findByIdAndBuyerId(addressId, currentUserId))
                    .willReturn(Optional.of(address));

            AddressUpdateRequest request = new AddressUpdateRequest(
                    "홍길동",
                    "010-1234-5678",
                    "서울 강남",
                    null
            );

            // when
            buyerAddressService.updateAddress(currentUserId, addressId, request);

            // then
            assertThat(address.getRecipient()).isEqualTo("홍길동");
            assertThat(address.getPhone()).isEqualTo("010-1234-5678");
            assertThat(address.getDetail()).isEqualTo("서울 강남");
        }

        @Test
        @DisplayName("일반 배송지를 기본 배송지로 변경")
        void updateAddress_makeDefault_realObject() {
            // given
            Long currentUserId = 1L;
            Long addressId = 10L;

            Buyer buyer = mock(Buyer.class);

            // 기본 isDefault 값 false
            Address address = Address.of(buyer, "홍길동", "010", "서울");

            given(addressRepository.findByIdAndBuyerId(addressId, currentUserId))
                    .willReturn(Optional.of(address));

            AddressUpdateRequest request = new AddressUpdateRequest(
                    null, null, null, true
            );

            // when
            buyerAddressService.updateAddress(currentUserId, addressId, request);

            // then
            assertThat(address.isDefault()).isTrue();
            verify(addressRepository).unsetOtherDefaultsByBuyerId(currentUserId, addressId);
        }

        @Test
        @DisplayName("기본 배송지를 일반 배송지로 변경")
        void updateAddress_unsetDefault_realObject() {
            // given
            Long currentUserId = 1L;
            Long addressId = 10L;

            Buyer buyer = mock(Buyer.class);

            Address address = Address.of(buyer, "홍길동", "010", "서울");
            address.makeDefault(); // true로 설정

            given(addressRepository.findByIdAndBuyerId(addressId, currentUserId))
                    .willReturn(Optional.of(address));

            AddressUpdateRequest request = new AddressUpdateRequest(
                    null, null, null, false
            );

            // when
            buyerAddressService.updateAddress(currentUserId, addressId, request);

            // then
            assertThat(address.isDefault()).isFalse();
            verify(addressRepository, never()).unsetOtherDefaultsByBuyerId(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("주소 삭제")
    class DeleteAddressTest {
        @Test
        @DisplayName("주소 삭제 성공")
        void removeAddress_success() {
            // given
            Long userId = 1L;
            Long addressId = 10L;

            Buyer buyer = createBuyer(userId);

            Address address = createAddress(
                    10L, buyer, "홍길동", "010-1111-2222", "서울시 강남구", true
            );

            given(addressRepository.findByIdAndBuyerId(addressId, userId))
                    .willReturn(Optional.of(address));

            // when
            AddressDetailResponse response =
                    buyerAddressService.removeAddress(userId, addressId);

            // then
            verify(addressRepository).delete(address); // 삭제 호출 검증

            assertThat(response).isNotNull();
            assertThat(response.addressId()).isEqualTo(addressId); // DTO 값 검증
        }

        @Test
        @DisplayName("주소가 없으면 예외 발생")
        void removeAddress_fail_notFound() {
            // given
            Long userId = 1L;
            Long addressId = 10L;

            given(addressRepository.findByIdAndBuyerId(addressId, userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    buyerAddressService.removeAddress(userId, addressId)
            )
                    .isInstanceOf(BaseException.class)
                    .hasMessage(ErrorEnum.ADDRESS_NOT_FOUND.getMessage());

            verify(addressRepository, never()).delete(any());
        }
    }

    private Address createAddress(
            Long addressId,
            Buyer buyer,
            String recipient,
            String phone,
            String detail,
            boolean isDefault
    ) {
        Address address = Address.of(buyer, recipient, phone, detail);
        ReflectionTestUtils.setField(address, "id", addressId);

        if (isDefault) {
            address.makeDefault();
        }

        return address;
    }

}
package com.example.allinmarket.buyer.address.service;

import com.example.allinmarket.buyer.address.dto.request.AddressCreateRequest;
import com.example.allinmarket.buyer.address.dto.response.AddressDetailResponse;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
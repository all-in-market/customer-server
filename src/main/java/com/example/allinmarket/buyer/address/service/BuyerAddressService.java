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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerAddressService {

    private final AddressRepository addressRepository;
    private final BuyerRepository buyerRepository;

    /**
     * 주소 생성
     */
    @Transactional
    public AddressDetailResponse createAddress(Long currentUserId, AddressCreateRequest request) {
        Buyer buyer = buyerRepository.findById(currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );

        Address address = Address.of(
                buyer,
                request.recipient(),
                request.phone(),
                request.detail()
        );

        // 등록된 기본 주소지가 있는지 확인
        boolean hasDefault = addressRepository
                .existsByBuyerIdAndIsDefaultTrue(buyer.getId());


        // 기본 주소지가 없을 경우 기본 주소지로 설정
        if (!hasDefault) {
            address.makeDefault();
        }

        addressRepository.save(address);

        return AddressDetailResponse.from(address);
    }

    /**
     * 구매자 본인 배송지 목록 조회
     */
    public List<AddressDetailResponse> getAllAddresses(Long currentUserId) {

        List<Address> addresses = addressRepository.findAllByBuyerId(currentUserId);

        return addresses.stream()
                .map(AddressDetailResponse::from)
                .toList();
    }

    /**
     * 배송지 내용 수정
     */
    @Transactional
    public AddressDetailResponse updateAddress(Long currentUserId, Long addressId, AddressUpdateRequest request) {

        Address address = addressRepository.findByIdAndBuyerId(addressId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.ADDRESS_NOT_FOUND)
        );

        if (StringUtils.hasText(request.recipient())) {
            address.updateRecipient(request.recipient());
        }

        if (StringUtils.hasText(request.phone())) {
            address.updatePhone(request.phone());
        }

        if (StringUtils.hasText(request.detail())) {
            address.updateDetail(request.detail());
        }

        updateDefaultAddress(currentUserId, addressId, address, request.isDefault());

        return AddressDetailResponse.from(address);
    }

    /**
     * 배송지 삭제
     */
    @Transactional
    public AddressDetailResponse removeAddress(Long currentUserId, Long addressId) {
        Address address = addressRepository.findByIdAndBuyerId(addressId, currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.ADDRESS_NOT_FOUND)
        );

        addressRepository.delete(address);

        return AddressDetailResponse.from(address);
    }

    /**
     * 요청된 isDefault 값에 따라 현재 주소지의 기본 주소지 설정을 변경
     */
    private void updateDefaultAddress(Long currentUserId, Long addressId, Address address, Boolean isDefault) {
        if (isDefault == null || isDefault == address.isDefault()) {
            return;
        }

        // default 배송지 -> 일반 배송지
        if (Boolean.FALSE.equals(isDefault)) {
            address.unsetDefault();
            return;
        }

        // 일반 배송지 -> default 배송지
        // 해당 구매자의 다른 배송지를 먼저 일반 배송지로 만든 후 현재 배송지를 default 설정
        addressRepository.unsetOtherDefaultsByBuyerId(currentUserId, addressId);
        address.makeDefault();
    }
}

package com.example.allinmarket.buyer.address.service;

import com.example.allinmarket.buyer.address.dto.request.AddressCreateRequest;
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
}

package com.example.allinmarket.domain.address.repository;

import com.example.allinmarket.domain.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByIdAndBuyerId(Long addressId, Long buyerId);

    boolean existsByBuyerIdAndIsDefaultTrue(Long buyerId);

    List<Address> findAllByBuyerId(Long buyerId);
}

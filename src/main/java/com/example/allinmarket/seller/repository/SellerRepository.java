package com.example.allinmarket.seller.repository;

import com.example.allinmarket.seller.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByEmail(String email);
    boolean existsByIdAndDeletedAtIsNull(Long id);
    Optional<Seller> findByEmail(String email);

    @Query("SELECT s.id FROM Seller s WHERE s.deletedAt IS NULL AND s.status = com.example.allinmarket.seller.enums.SellerStatus.APPROVED")
    List<Long> findAllIds();

    @Query("SELECT s FROM Seller s WHERE s.deletedAt IS NULL AND s.status = com.example.allinmarket.seller.enums.SellerStatus.APPROVED")
    List<Seller> findAllActiveSellers();
}

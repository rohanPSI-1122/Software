// src/main/java/com/example/demoapp/repository/PurchaseRepository.java
package com.example.demoapp.repository;

import com.example.demoapp.model.Purchase;
import com.example.demoapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    List<Purchase> findByUser(User user);
    
    @Query("SELECT p FROM Purchase p WHERE p.user = :user AND p.software.id = :softwareId")
    Optional<Purchase> findByUserAndSoftwareId(@Param("user") User user, @Param("softwareId") Long softwareId);
    
    boolean existsByUserAndSoftwareId(User user, Long softwareId);
    
    @Query("SELECT p FROM Purchase p WHERE p.user = :user ORDER BY p.purchaseDate DESC")
    List<Purchase> findRecentPurchasesByUser(@Param("user") User user);
}

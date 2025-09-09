// src/main/java/com/example/demoapp/repository/UserRepository.java
package com.example.demoapp.repository;

import java.util.List;
import com.example.demoapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    // Add this method to UserRepository if not already present
    List<User> findAllByOrderByIdAsc();
}

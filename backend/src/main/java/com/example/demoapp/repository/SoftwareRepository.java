// src/main/java/com/example/demoapp/repository/SoftwareRepository.java
package com.example.demoapp.repository;

import com.example.demoapp.model.Software;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List; 
 
@Repository
public interface SoftwareRepository extends JpaRepository<Software, Long> {
   // Add this method to SoftwareRepository.java
     List<Software> findByUploadedBy(String uploadedBy);
}

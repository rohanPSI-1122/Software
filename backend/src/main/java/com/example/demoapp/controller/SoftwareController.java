// src/main/java/com/example/demoapp/controller/SoftwareController.java
package com.example.demoapp.controller;

import com.example.demoapp.model.Purchase;
import com.example.demoapp.model.Software;
import com.example.demoapp.model.User;
import com.example.demoapp.repository.PurchaseRepository;
import com.example.demoapp.repository.SoftwareRepository;
import com.example.demoapp.repository.UserRepository;
import com.example.demoapp.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://192.168.1.33:3000", allowCredentials = "true")
@RequestMapping("/api/software")
public class SoftwareController {

    private static final Logger log = LoggerFactory.getLogger(SoftwareController.class);

    @Value("${upload.location:/home/ubuntu/extuploads/}")
    private String uploadLocation;

    private final SoftwareRepository softwareRepository;
    private final UserRepository userRepository;
    private final PurchaseRepository purchaseRepository;
    private final JwtUtil jwtUtil;

    @Autowired
    public SoftwareController(SoftwareRepository softwareRepository,
                             UserRepository userRepository,
                             PurchaseRepository purchaseRepository,
                             JwtUtil jwtUtil) {
        this.softwareRepository = softwareRepository;
        this.userRepository = userRepository;
        this.purchaseRepository = purchaseRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Upload software: title, demo video, ZIP file, price
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadSoftware(
            @RequestParam("title") String title,
            @RequestParam("video") MultipartFile video,
            @RequestParam("zipFile") MultipartFile zipFile,
            @RequestParam("price") Double price,
            HttpServletRequest request) {

        // Get username from token
        String username = getUsernameFromRequest(request);
        if (username == null) {
            log.error("Upload failed: Could not extract username from token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication failed - invalid token");
        }

        // Validate input
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Title is required");
        }
        if (video.isEmpty()) {
            return ResponseEntity.badRequest().body("Demo video is required");
        }
        if (zipFile.isEmpty()) {
            return ResponseEntity.badRequest().body("ZIP file is required");
        }
        if (price == null || price <= 0) {
            return ResponseEntity.badRequest().body("Valid price is required");
        }

        // Ensure upload directory exists and is writable
        File uploadDir = new File(uploadLocation);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                log.error("❌ Failed to create upload directory: " + uploadLocation);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to create upload directory: " + uploadLocation);
            }
            log.info("✅ Created upload directory: " + uploadLocation);
        }

        // Check if directory is writable
        if (!uploadDir.canWrite()) {
            log.error("❌ Upload directory is not writable: " + uploadLocation);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload directory is not writable: " + uploadLocation);
        }

        try {
            // Save video with UUID filename to avoid issues
            String videoExtension = getFileExtension(video.getOriginalFilename());
            String videoFileName = UUID.randomUUID().toString() + "." + videoExtension;
            Path videoPath = Paths.get(uploadLocation, videoFileName);

            // Create parent directories if they don't exist
            Files.createDirectories(videoPath.getParent());

            Files.copy(video.getInputStream(), videoPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ Video saved: " + videoPath);

            // Save ZIP with UUID filename to avoid issues
            String zipExtension = getFileExtension(zipFile.getOriginalFilename());
            String zipFileName = UUID.randomUUID().toString() + "." + zipExtension;
            Path zipPath = Paths.get(uploadLocation, zipFileName);

            // Create parent directories if they don't exist
            Files.createDirectories(zipPath.getParent());

            Files.copy(zipFile.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ ZIP saved: " + zipPath);

            // Save to DB
            Software software = new Software();
            software.setTitle(title.trim());
            software.setVideoUrl("/uploads/" + videoFileName);
            software.setZipUrl("/uploads/" + zipFileName);
            software.setPrice(price);
            software.setUploadedBy(username);

            Software saved = softwareRepository.save(software);
            log.info("✅ Software saved to DB with ID: " + saved.getId());
            return ResponseEntity.ok(saved);

        } catch (IOException e) {
            log.error("❌ File upload failed: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File upload failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error during upload: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Update software (only by owner)
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateSoftware(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "video", required = false) MultipartFile video,
            @RequestParam(value = "zipFile", required = false) MultipartFile zipFile,
            @RequestParam(value = "price", required = false) Double price,
            HttpServletRequest request) {

        // Get username from token
        String username = getUsernameFromRequest(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Software> softwareOpt = softwareRepository.findById(id);
        if (softwareOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Software software = softwareOpt.get();

        // Check if user is owner or admin
        boolean isOwner = software.getUploadedBy() != null && software.getUploadedBy().equals(username);
        boolean isAdmin = isUserAdmin(request);

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to update this software");
        }

        try {
            // Update fields if provided
            if (title != null && !title.trim().isEmpty()) {
                software.setTitle(title.trim());
            }

            if (price != null && price > 0) {
                software.setPrice(price);
            }

            if (video != null && !video.isEmpty()) {
                // Delete old video
                String oldVideoPath = uploadLocation + software.getVideoUrl().replace("/uploads/", "");
                Files.deleteIfExists(Paths.get(oldVideoPath));

                // Save new video with UUID filename
                String videoExtension = getFileExtension(video.getOriginalFilename());
                String videoFileName = UUID.randomUUID().toString() + "." + videoExtension;
                Path videoPath = Paths.get(uploadLocation, videoFileName);
                Files.copy(video.getInputStream(), videoPath, StandardCopyOption.REPLACE_EXISTING);
                software.setVideoUrl("/uploads/" + videoFileName);
            }

            if (zipFile != null && !zipFile.isEmpty()) {
                // Delete old ZIP
                String oldZipPath = uploadLocation + software.getZipUrl().replace("/uploads/", "");
                Files.deleteIfExists(Paths.get(oldZipPath));

                // Save new ZIP with UUID filename
                String zipExtension = getFileExtension(zipFile.getOriginalFilename());
                String zipFileName = UUID.randomUUID().toString() + "." + zipExtension;
                Path zipPath = Paths.get(uploadLocation, zipFileName);
                Files.copy(zipFile.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);
                software.setZipUrl("/uploads/" + zipFileName);
            }

            Software updated = softwareRepository.save(software);
            return ResponseEntity.ok(updated);

        } catch (IOException e) {
            log.error("❌ File update failed: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File update failed: " + e.getMessage());
        }
    }

    /**
     * Delete software (admin only)
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSoftware(@PathVariable Long id, HttpServletRequest request) {
        // Check if user is admin
        if (!isUserAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        Optional<Software> softwareOpt = softwareRepository.findById(id);
        if (softwareOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Software software = softwareOpt.get();

        try {
            // Delete files
            String videoPath = uploadLocation + software.getVideoUrl().replace("/uploads/", "");
            String zipPath = uploadLocation + software.getZipUrl().replace("/uploads/", "");

            Files.deleteIfExists(Paths.get(videoPath));
            Files.deleteIfExists(Paths.get(zipPath));

            // Delete from DB
            softwareRepository.delete(software);

            return ResponseEntity.ok("Software deleted successfully");

        } catch (IOException e) {
            log.error("❌ File deletion failed: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File deletion failed: " + e.getMessage());
        }
    }

    /**
     * List all uploaded software (marketplace)
     */
    @GetMapping("/list")
    public List<Software> getAllSoftware() {
        log.info("📋 Fetching all software from DB");
        return softwareRepository.findAll();
    }

    /**
     * Get software uploaded by current user
     */
    @GetMapping("/my-uploads")
    public ResponseEntity<List<Software>> getMyUploads(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Software> mySoftware = softwareRepository.findByUploadedBy(username);
        log.info("📤 Found {} uploads for user: {}", mySoftware.size(), username);
        return ResponseEntity.ok(mySoftware);
    }

    /**
     * Get software purchased by current user
     */
    @GetMapping("/my-purchases")
    public ResponseEntity<List<Purchase>> getMyPurchases(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Purchase> purchases = purchaseRepository.findByUser(userOpt.get());
        log.info("📥 Found {} purchases for user: {}", purchases.size(), username);
        return ResponseEntity.ok(purchases);
    }

    /**
     * Record a purchase when user buys software
     */
    @PostMapping("/purchase/{softwareId}")
    public ResponseEntity<?> recordPurchase(@PathVariable Long softwareId, HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        Optional<Software> softwareOpt = softwareRepository.findById(softwareId);

        if (userOpt.isEmpty() || softwareOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Software software = softwareOpt.get();

        // Check if already purchased
        if (purchaseRepository.existsByUserAndSoftwareId(user, softwareId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Software already purchased");
        }

        // Record purchase
        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setSoftware(software);
        purchase.setPurchaseDate(LocalDateTime.now());
        purchase.setPricePaid(software.getPrice());

        purchaseRepository.save(purchase);
        log.info("✅ Purchase recorded: User {} bought software {}", username, softwareId);
        return ResponseEntity.ok("Purchase recorded successfully");
    }

    /**
     * Get software by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Software> getSoftwareById(@PathVariable Long id) {
        Optional<Software> softwareOpt = softwareRepository.findById(id);
        return softwareOpt.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Debug endpoint to check software ownership
     */
    @GetMapping("/debug/{id}")
    public ResponseEntity<?> debugSoftware(@PathVariable Long id, HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Software> softwareOpt = softwareRepository.findById(id);
        if (softwareOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Software software = softwareOpt.get();
        boolean isOwner = software.getUploadedBy() != null && software.getUploadedBy().equals(username);
        boolean isAdmin = isUserAdmin(request);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("softwareId", software.getId());
        response.put("softwareTitle", software.getTitle());
        response.put("uploadedBy", software.getUploadedBy());
        response.put("currentUser", username);
        response.put("isOwner", isOwner);
        response.put("isAdmin", isAdmin);
        response.put("canEdit", isOwner || isAdmin);

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    // Helper methods
    private String getUsernameFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                log.error("Error extracting username from token: " + e.getMessage(), e);
                return null;
            }
        }
        log.debug("No valid Authorization header found");
        return null;
    }

    private boolean isUserAdmin(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username != null) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            return userOpt.isPresent() && userOpt.get().isAdmin();
        }
        return false;
    }
}

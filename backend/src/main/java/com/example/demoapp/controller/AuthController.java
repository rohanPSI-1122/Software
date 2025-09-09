// src/main/java/com/example/demoapp/controller/AuthController.java
package com.example.demoapp.controller;

import com.example.demoapp.dto.AuthResponse;
import com.example.demoapp.dto.LoginRequest;
import com.example.demoapp.dto.RegisterRequest;
import com.example.demoapp.dto.PromoteRequest;
import com.example.demoapp.model.User;
import com.example.demoapp.repository.UserRepository;
import com.example.demoapp.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://192.168.1.40:3000", allowCredentials = "true")
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        log.info("📝 Registration request for: {}", request.getUsername());

        // Validate input
        if (request.getUsername() == null || request.getPassword() == null ||
            request.getUsername().trim().isEmpty() || request.getPassword().isEmpty()) {
            log.warn("Invalid registration request: missing username or password");
            return ResponseEntity.badRequest().body("Username and password are required");
        }

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: username '{}' already exists", request.getUsername());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash the password
        user.setAdmin(false); // Default to non-admin
        userRepository.save(user);

        log.info("✅ User registered successfully: {}", request.getUsername());
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("🔐 Login request received for: {}", request.getUsername());

        if (request.getUsername() == null || request.getPassword() == null) {
            log.warn("Login failed: missing username or password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Find user by username
        var userOpt = userRepository.findByUsername(request.getUsername().trim());
        if (userOpt.isEmpty()) {
            log.warn("Login failed: user not found - {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userOpt.get();

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for user - {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getUsername());
        log.info("✅ Login successful for: {}", user.getUsername());

        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.isAdmin()));
    }

    @GetMapping("/check-admin")
    public ResponseEntity<Boolean> checkAdmin(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) {
            log.warn("Admin check failed: No valid authentication");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().isAdmin()) {
            log.info("✅ Admin check passed for user: {}", username);
            return ResponseEntity.ok(true);
        }

        log.info("❌ Admin check failed for user: {}", username);
        return ResponseEntity.ok(false);
    }

    @GetMapping("/debug-user")
    public ResponseEntity<?> debugUser(HttpServletRequest request) {
        String username = getUsernameFromRequest(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getUsername());
        response.put("isAdmin", user.isAdmin());
        response.put("id", user.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all users (admin only) - returns simplified user info without passwords
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpServletRequest request) {
        // Check if user is admin
        if (!isUserAdmin(request)) {
            log.warn("Access denied: Non-admin user attempted to access user list");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }

        try {
            List<User> users = userRepository.findAll();
            
            // Create response without passwords
            List<Map<String, Object>> userResponses = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("isAdmin", user.isAdmin());
                    return userMap;
                })
                .toList();
                
            log.info("Retrieved {} users for admin view", users.size());
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            log.error("Error retrieving users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving users: " + e.getMessage());
        }
    }

    /**
     * Promote/Demote user (admin only) with request body
     */
    @PostMapping("/promote/{userId}")
    public ResponseEntity<?> promoteUser(@PathVariable Long userId, 
                                       @RequestBody PromoteRequest promoteRequest,
                                       HttpServletRequest request) {
        try {
            // Check if the current user is an admin
            String currentUsername = getUsernameFromRequest(request);
            if (currentUsername == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
            }

            Optional<User> currentUserOpt = userRepository.findByUsername(currentUsername);
            if (currentUserOpt.isEmpty() || !currentUserOpt.get().isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only administrators can promote users");
            }

            // Find the target user
            Optional<User> targetUserOpt = userRepository.findById(userId);
            if (targetUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            User targetUser = targetUserOpt.get();
            
            // Prevent self-demotion if trying to remove admin rights from yourself
            if (currentUsername.equals(targetUser.getUsername()) && !promoteRequest.isMakeAdmin()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot remove your own administrator privileges");
            }

            // Update the user's admin status
            targetUser.setAdmin(promoteRequest.isMakeAdmin());
            userRepository.save(targetUser);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User promotion status updated successfully");
            response.put("userId", userId);
            response.put("username", targetUser.getUsername());
            response.put("isAdmin", targetUser.isAdmin());

            log.info("User {} {} admin privileges by {}", 
                    targetUser.getUsername(), 
                    promoteRequest.isMakeAdmin() ? "granted" : "removed", 
                    currentUsername);
                    
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error promoting user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error promoting user: " + e.getMessage());
        }
    }

    /**
     * Simple promote/demote endpoint without request body (admin only)
     */
    @PostMapping("/promote/{userId}/{makeAdmin}")
    public ResponseEntity<?> promoteUserSimple(@PathVariable Long userId,
                                             @PathVariable boolean makeAdmin,
                                             HttpServletRequest request) {
        PromoteRequest promoteRequest = new PromoteRequest(makeAdmin);
        return promoteUser(userId, promoteRequest, request);
    }

    // Helper methods
    private String getUsernameFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                return jwtUtil.getUsernameFromToken(token);
            } catch (Exception e) {
                log.error("Error extracting username from token: {}", e.getMessage());
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

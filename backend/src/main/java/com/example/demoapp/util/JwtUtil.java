package com.example.demoapp.util;

import com.example.demoapp.model.User;
import com.example.demoapp.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;

@Component
public class JwtUtil {

    private final SecretKey key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS512);
    private long expiration = 86400000; // 24 hours

    @Autowired
    private UserRepository userRepository;

    // ✅ Generate JWT token with roles
    public String generateToken(String username) {
        // Get user to check if admin
        boolean isAdmin = false;
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            isAdmin = userOpt.isPresent() && userOpt.get().isAdmin();
        } catch (Exception e) {
            System.out.println("❌ Error checking admin status: " + e.getMessage());
        }
        
        String role = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";
        System.out.println("🔐 Generating token for user: " + username + " with role: " + role);
        
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", role) // Add roles claim
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    // ✅ Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ✅ Extract username from token
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // ✅ Extract roles from token
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // Handle both single role string and list of roles
            Object rolesClaim = claims.get("roles");
            if (rolesClaim instanceof String) {
                return Collections.singletonList((String) rolesClaim);
            } else if (rolesClaim instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) rolesClaim;
                return roles;
            }
            return Collections.singletonList("ROLE_USER");
        } catch (Exception e) {
            System.out.println("❌ Error extracting roles from token: " + e.getMessage());
            return Collections.singletonList("ROLE_USER");
        }
    }
}

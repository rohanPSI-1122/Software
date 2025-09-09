package com.example.demoapp.filter;

import com.example.demoapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component; // ← Required
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component  // ✅ THIS LINE IS CRITICAL
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("🔐 [FILTER] Authorization Header: " + authHeader); // 🔥 Debug log

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);

                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("N/A")
                        .authorities("ROLE_USER")
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("✅ [FILTER] Authenticated as: " + username);
            } else {
                System.out.println("❌ [FILTER] Invalid token");
            }
        } else {
            System.out.println("❌ [FILTER] No Authorization header");
        }

        filterChain.doFilter(request, response);
    }
}

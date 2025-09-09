package com.example.demoapp.filter;

import com.example.demoapp.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("🔐 [FILTER] Authorization Header: " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                List<String> roles = jwtUtil.getRolesFromToken(token);

                // Convert roles to GrantedAuthority objects
                List<GrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Debug: Check what roles we found
                System.out.println("🔐 [FILTER] Roles extracted from token: " + roles);
                System.out.println("🔐 [FILTER] Authorities: " + authorities);

                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("N/A")
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("✅ [FILTER] Authenticated as: " + username + " with roles: " + authorities);
            } else {
                System.out.println("❌ [FILTER] Invalid token");
            }
        } else {
            System.out.println("❌ [FILTER] No Authorization header");
        }

        filterChain.doFilter(request, response);
    }
}

// src/main/java/com/example/demoapp/config/SecurityConfig.java
package com.example.demoapp.config;

import com.example.demoapp.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/software/list").permitAll()
                .requestMatchers("/api/software/upload").authenticated()
                .requestMatchers("/api/software/update/**").authenticated()
                .requestMatchers("/api/software/delete/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/auth/check-admin").authenticated()
                .requestMatchers("/api/software/my-uploads").authenticated()
                .requestMatchers("/api/software/my-purchases").authenticated()
                .requestMatchers("/api/software/purchase/**").authenticated()
                .requestMatchers("/api/auth/users").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/auth/users").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/auth/promote/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/auth/demote/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/payment/create-payment").authenticated()
                .requestMatchers("/api/payment/execute-payment").authenticated()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://192.168.1.40:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

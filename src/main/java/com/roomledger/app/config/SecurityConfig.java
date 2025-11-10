package com.roomledger.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.security.basic.username:admin}")
    private String username;

    @Value("${app.security.basic.password:admin}")
    private String password;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(encoder.encode(password))
                        .roles("USER")
                        .build()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Stateless API: matikan CSRF (webhook diverifikasi via secret header di controller)
                .csrf(AbstractHttpConfigurer::disable)

                // Tidak pakai session
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Aturan akses
                .authorizeHttpRequests(auth -> auth
                        // Webhook + healthcheck publik (sesuaikan path webhook yang kamu pakai)
                        .requestMatchers(
                                "/callbacks/**",          // jika webhook kamu di bawah /callbacks/**
                                "/telegram/webhook",      // atau endpoint webhook versi sebelumnya
                                "/healthcheck"
                        ).permitAll()

                        // (opsional) buka docs kalau ada
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()

                        // lainnya wajib auth
                        .anyRequest().authenticated()
                )

                // Basic auth
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}

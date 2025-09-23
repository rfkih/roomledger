package com.roomledger.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
                // Disable CSRF for all requests except /callbacks/** (webhook)
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/callbacks/**")  // Webhook is exempt from CSRF
                        .disable()  // Disable CSRF for API routes, i.e., /api/** (stateless API)
                )
                // Disable session management for stateless authentication
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define the security rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/callbacks/**", "/healthcheck").permitAll()  // Public routes (webhook, healthcheck)
                        .anyRequest().authenticated()  // All other requests require authentication
                )

                // Enable Basic Authentication
                .httpBasic(Customizer.withDefaults())

                .build();
    }
}



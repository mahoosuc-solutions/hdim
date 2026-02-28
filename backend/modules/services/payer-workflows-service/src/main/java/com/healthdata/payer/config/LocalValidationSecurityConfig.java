package com.healthdata.payer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Local-only security override for Wave-1 contract validation.
 * Never enable this profile outside controlled local validation runs.
 */
@Configuration
@Profile("wave1-local-validation")
public class LocalValidationSecurityConfig {

    @Bean
    SecurityFilterChain localValidationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}

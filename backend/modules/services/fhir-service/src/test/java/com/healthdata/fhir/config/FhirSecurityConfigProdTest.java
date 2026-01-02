package com.healthdata.fhir.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.security.TenantAccessFilter;

@SpringBootTest(
        classes = {FhirSecurityConfig.class, FhirSecurityConfigProdTest.SecurityBeans.class},
        webEnvironment = WebEnvironment.MOCK)
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        WebMvcAutoConfiguration.class
})
@ActiveProfiles("prod")
class FhirSecurityConfigProdTest {

    @TestConfiguration
    static class SecurityBeans {
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return Mockito.mock(JwtAuthenticationFilter.class);
        }

        @Bean
        TenantAccessFilter tenantAccessFilter() {
            return Mockito.mock(TenantAccessFilter.class);
        }
    }

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldProvideProdSecurityChainAndCors() {
        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .contains("http://localhost:4200", "http://localhost:4201", "http://localhost:4202");
        assertThat(securityFilterChain).isNotNull();
    }
}

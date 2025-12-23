package com.healthdata.quality.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.security.TenantAccessFilter;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@DisplayName("Quality Measure Security Config Tests")
class QualityMeasureSecurityConfigTest {

    @Test
    @DisplayName("Should configure allowed CORS origins and methods")
    void shouldConfigureCors() {
        QualityMeasureSecurityConfig config = new QualityMeasureSecurityConfig();
        CorsConfigurationSource source = config.corsConfigurationSource();

        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).contains(
            "http://localhost:4200",
            "http://localhost:4201",
            "http://localhost:4202"
        );
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("Should build test security filter chain")
    void shouldBuildTestSecurityFilterChain() throws Exception {
        QualityMeasureSecurityConfig config = new QualityMeasureSecurityConfig();

        SecurityFilterChain chain = config.testSecurityFilterChain(httpSecurity());

        assertThat(chain).isNotNull();
    }

    @Test
    @DisplayName("Should build production security filter chain without tenant filter")
    void shouldBuildProductionSecurityFilterChainWithoutTenantFilter() throws Exception {
        QualityMeasureSecurityConfig config = new QualityMeasureSecurityConfig();
        JwtAuthenticationFilter jwtAuthenticationFilter = mock(JwtAuthenticationFilter.class);

        SecurityFilterChain chain = config.securityFilterChain(httpSecurity(), jwtAuthenticationFilter);

        assertThat(chain).isNotNull();
    }

    @Test
    @DisplayName("Should build production security filter chain with tenant filter")
    void shouldBuildProductionSecurityFilterChainWithTenantFilter() throws Exception {
        QualityMeasureSecurityConfig config = new QualityMeasureSecurityConfig();
        JwtAuthenticationFilter jwtAuthenticationFilter = mock(JwtAuthenticationFilter.class);
        TenantAccessFilter tenantAccessFilter = mock(TenantAccessFilter.class);
        ReflectionTestUtils.setField(config, "tenantAccessFilter", tenantAccessFilter);

        SecurityFilterChain chain = config.securityFilterChain(httpSecurity(), jwtAuthenticationFilter);

        assertThat(chain).isNotNull();
    }

    private HttpSecurity httpSecurity() throws Exception {
        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        AuthenticationManagerBuilder authBuilder = new AuthenticationManagerBuilder(objectPostProcessor);
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        GenericApplicationContext context = new GenericApplicationContext();
        context.refresh();
        sharedObjects.put(ApplicationContext.class, context);
        HttpSecurity http = new HttpSecurity(objectPostProcessor, authBuilder, sharedObjects);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(auth -> auth.requestMatchers(new AntPathRequestMatcher("/**")).permitAll());
        return http;
    }
}

package com.healthdata.caregap.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Tag;
import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.filter.UserAutoRegistrationFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import com.healthdata.caregap.security.TenantHeaderNormalizationFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@DisplayName("CareGapSecurityConfig")
@Tag("unit")
class CareGapSecurityConfigTest {

    @Test
    @DisplayName("Should configure CORS settings")
    void shouldConfigureCors() {
        CareGapSecurityConfig config = new CareGapSecurityConfig();
        CorsConfigurationSource source = config.corsConfigurationSource();

        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api"));
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).contains("http://localhost:4200");
        assertThat(cors.getAllowedMethods()).contains("GET");
        assertThat(cors.getAllowedHeaders()).contains("*");
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Should build test security filter chain")
    void shouldBuildTestSecurityFilterChain() throws Exception {
        CareGapSecurityConfig config = new CareGapSecurityConfig();

        SecurityFilterChain chain = config.testSecurityFilterChain(httpSecurity());

        assertThat(chain).isNotNull();
    }

    @Test
    @DisplayName("Should build production security filter chain with gateway trust filters")
    void shouldBuildProductionChainWithGatewayTrustFilters() throws Exception {
        CareGapSecurityConfig config = new CareGapSecurityConfig();
        TrustedHeaderAuthFilter trustedHeaderFilter = mock(TrustedHeaderAuthFilter.class);
        UserAutoRegistrationFilter userAutoRegistrationFilter = mock(UserAutoRegistrationFilter.class);
        TenantHeaderNormalizationFilter tenantHeaderNormalizationFilter =
            mock(TenantHeaderNormalizationFilter.class);
        TrustedTenantAccessFilter trustedTenantFilter = mock(TrustedTenantAccessFilter.class);

        SecurityFilterChain chain = config.securityFilterChain(
            httpSecurity(),
            trustedHeaderFilter,
            userAutoRegistrationFilter,
            tenantHeaderNormalizationFilter,
            trustedTenantFilter
        );

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

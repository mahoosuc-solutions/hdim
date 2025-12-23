package com.healthdata.consent.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;

@DisplayName("ConsentSecurityConfig")
class ConsentSecurityConfigTest {

    @Test
    @DisplayName("Should configure CORS settings")
    void shouldConfigureCorsSettings() {
        ConsentSecurityConfig config = new ConsentSecurityConfig();
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/consents");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOrigins()).contains("http://localhost:4200");
        assertThat(cors.getAllowedMethods()).contains("GET");
        assertThat(cors.getAllowedHeaders()).contains("*");
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("Should build SecurityFilterChain")
    void shouldBuildSecurityFilterChain() throws Exception {
        ConsentSecurityConfig config = new ConsentSecurityConfig();
        HttpSecurity http = httpSecurity();

        SecurityFilterChain chain = config.securityFilterChain(http);

        assertThat(chain).isNotNull();
        assertThat(http).isNotNull();
    }

    private HttpSecurity httpSecurity() throws Exception {
        var objectPostProcessor = new org.springframework.security.config.annotation.ObjectPostProcessor<Object>() {
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

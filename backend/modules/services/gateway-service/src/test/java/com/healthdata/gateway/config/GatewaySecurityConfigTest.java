package com.healthdata.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.healthdata.authentication.filter.JwtAuthenticationFilter;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@DisplayName("GatewaySecurityConfig")
class GatewaySecurityConfigTest {

    @Test
    @DisplayName("Should provide password encoder and authentication provider")
    void shouldProvideAuthBeans() {
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        GatewaySecurityConfig config = new GatewaySecurityConfig(userDetailsService);

        PasswordEncoder encoder = config.passwordEncoder();
        assertThat(encoder).isNotNull();

        DaoAuthenticationProvider provider = config.authenticationProvider();
        Object detailsService = ReflectionTestUtils.getField(provider, "userDetailsService");
        assertThat(detailsService).isEqualTo(userDetailsService);
    }

    @Test
    @DisplayName("Should configure CORS settings")
    void shouldConfigureCors() {
        GatewaySecurityConfig config = new GatewaySecurityConfig(mock(UserDetailsService.class));
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
        GatewaySecurityConfig config = new GatewaySecurityConfig(mock(UserDetailsService.class));

        SecurityFilterChain chain = config.testSecurityFilterChain(httpSecurity());

        assertThat(chain).isNotNull();
    }

    @Test
    @DisplayName("Should build production security filter chain")
    void shouldBuildProductionSecurityFilterChain() throws Exception {
        GatewaySecurityConfig config = new GatewaySecurityConfig(mock(UserDetailsService.class));
        JwtAuthenticationFilter filter = mock(JwtAuthenticationFilter.class);

        SecurityFilterChain chain = config.securityFilterChain(httpSecurity(), filter);

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

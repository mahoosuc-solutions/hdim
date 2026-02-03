package com.healthdata.patient.config;

import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.security.TenantAccessFilter;
import com.healthdata.patient.persistence.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag("unit")
@DisplayName("Patient Security Config Tests")
class PatientSecurityConfigTest {

    @SuppressWarnings("unchecked")
    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            SecurityAutoConfiguration.class,
                            WebMvcAutoConfiguration.class
                    ))
                    .withUserConfiguration(PatientSecurityConfig.class)
                    .withBean(JwtAuthenticationFilter.class, () -> mock(JwtAuthenticationFilter.class))
                    .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .withBean(UserRepository.class, () -> mock(UserRepository.class))
                    .withBean("userAutoRegistrationFilterProvider", ObjectProvider.class,
                            () -> mock(ObjectProvider.class));

    @Test
    @DisplayName("Should create test profile security chain and cors config")
    void shouldCreateTestProfileSecurityChain() {
        contextRunner.withPropertyValues("spring.profiles.active=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                    CorsConfigurationSource source =
                            context.getBean("corsConfigurationSource", CorsConfigurationSource.class);
                    CorsConfiguration config = source.getCorsConfiguration(
                            new MockHttpServletRequest("GET", "/patient/_health"));
                    assertThat(config).isNotNull();
                    assertThat(config.getAllowedOrigins()).contains("http://localhost:4200");
                });
    }

    @Test
    @DisplayName("Should create non-test security chain with tenant filter")
    void shouldCreateNonTestSecurityChain() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .withBean(TenantAccessFilter.class, () -> mock(TenantAccessFilter.class))
                .run(context -> {
                    // Non-test profile may create multiple security filter chains (actuator + main)
                    assertThat(context).hasBean("securityFilterChain");
                    assertThat(context.getBeansOfType(SecurityFilterChain.class)).isNotEmpty();
                });
    }
}

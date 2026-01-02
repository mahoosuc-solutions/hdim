package com.healthdata.ehr.dto;

import com.healthdata.ehr.model.EhrVendorType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EhrConnectionConfig validation.
 */
@DisplayName("EHR Connection Config Tests")
class EhrConnectionConfigTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should validate valid config")
    void shouldValidateValidConfig() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .connectionId("conn-1")
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://fhir.epic.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When
        Set<ConstraintViolation<EhrConnectionConfig>> violations = validator.validate(config);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should reject null tenant ID")
    void shouldRejectNullTenantId() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .connectionId("conn-1")
                .tenantId(null)
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://fhir.epic.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When
        Set<ConstraintViolation<EhrConnectionConfig>> violations = validator.validate(config);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Tenant ID"));
    }

    @Test
    @DisplayName("Should reject null vendor type")
    void shouldRejectNullVendorType() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .connectionId("conn-1")
                .tenantId("tenant-1")
                .vendorType(null)
                .baseUrl("https://fhir.epic.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When
        Set<ConstraintViolation<EhrConnectionConfig>> violations = validator.validate(config);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Vendor type"));
    }

    @Test
    @DisplayName("Should use default values for optional fields")
    void shouldUseDefaultValues() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://fhir.epic.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // Then
        assertThat(config.getTimeoutMs()).isEqualTo(30000);
        assertThat(config.getMaxRetries()).isEqualTo(3);
        assertThat(config.getEnableCircuitBreaker()).isTrue();
        assertThat(config.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should build config with all fields")
    void shouldBuildWithAllFields() {
        // Given/When
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .connectionId("conn-1")
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://fhir.epic.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .tokenUrl("https://fhir.epic.com/oauth2/token")
                .scope("system/*.read")
                .timeoutMs(60000)
                .maxRetries(5)
                .enableCircuitBreaker(false)
                .active(true)
                .build();

        // Then
        assertThat(config.getConnectionId()).isEqualTo("conn-1");
        assertThat(config.getTenantId()).isEqualTo("tenant-1");
        assertThat(config.getVendorType()).isEqualTo(EhrVendorType.EPIC);
        assertThat(config.getBaseUrl()).isEqualTo("https://fhir.epic.com");
        assertThat(config.getTokenUrl()).isEqualTo("https://fhir.epic.com/oauth2/token");
        assertThat(config.getScope()).isEqualTo("system/*.read");
        assertThat(config.getTimeoutMs()).isEqualTo(60000);
        assertThat(config.getMaxRetries()).isEqualTo(5);
        assertThat(config.getEnableCircuitBreaker()).isFalse();
    }
}

package com.healthdata.ehr.factory;

import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.connector.impl.EpicFhirConnector;
import com.healthdata.ehr.connector.impl.CernerFhirConnector;
import com.healthdata.ehr.connector.impl.AthenaConnector;
import com.healthdata.ehr.connector.impl.GenericFhirConnector;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.exception.UnsupportedVendorException;
import com.healthdata.ehr.model.EhrVendorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Test suite for EhrConnectorFactory.
 * Validates that the factory creates the correct connector type based on vendor configuration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EHR Connector Factory Tests")
class EhrConnectorFactoryTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    private EhrConnectorFactory factory;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(WebClient.builder().build());
        factory = new EhrConnectorFactory(webClientBuilder);
    }

    @Test
    @DisplayName("Should create Epic FHIR connector for EPIC vendor type")
    void shouldCreateEpicConnector() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://fhir.epic.com/api/FHIR/R4")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When
        EhrConnector connector = factory.createConnector(config);

        // Then
        assertThat(connector).isNotNull();
        assertThat(connector).isInstanceOf(EpicFhirConnector.class);
        assertThat(connector.getConfig()).isEqualTo(config);
    }

    @Test
    @DisplayName("Should create Cerner FHIR connector for CERNER vendor type")
    void shouldCreateCernerConnector() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.CERNER)
                .baseUrl("https://fhir-myrecord.cerner.com/r4")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When
        EhrConnector connector = factory.createConnector(config);

        // Then
        assertThat(connector).isNotNull();
        assertThat(connector).isInstanceOf(CernerFhirConnector.class);
        assertThat(connector.getConfig()).isEqualTo(config);
    }

    @Test
    @DisplayName("Should create Athena connector for ATHENA vendor type")
    void shouldCreateAthenaConnector() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.ATHENA)
                .baseUrl("https://api.athenahealth.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When
        EhrConnector connector = factory.createConnector(config);

        // Then
        assertThat(connector).isNotNull();
        assertThat(connector).isInstanceOf(AthenaConnector.class);
        assertThat(connector.getConfig()).isEqualTo(config);
    }

    @Test
    @DisplayName("Should create Generic FHIR connector for GENERIC vendor type")
    void shouldCreateGenericConnector() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.GENERIC)
                .baseUrl("https://fhir.example.com/api/R4")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When
        EhrConnector connector = factory.createConnector(config);

        // Then
        assertThat(connector).isNotNull();
        assertThat(connector).isInstanceOf(GenericFhirConnector.class);
        assertThat(connector.getConfig()).isEqualTo(config);
    }

    @Test
    @DisplayName("Should throw exception for null configuration")
    void shouldThrowExceptionForNullConfig() {
        // When/Then
        assertThatThrownBy(() -> factory.createConnector(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Connection configuration cannot be null");
    }

    @Test
    @DisplayName("Should throw exception for null vendor type")
    void shouldThrowExceptionForNullVendorType() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .tenantId("tenant-1")
                .vendorType(null)
                .baseUrl("https://fhir.example.com")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When/Then
        assertThatThrownBy(() -> factory.createConnector(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vendor type cannot be null");
    }

    @Test
    @DisplayName("Should create different instances for multiple calls")
    void shouldCreateDifferentInstances() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://fhir.epic.com/api/FHIR/R4")
                .clientId("test-client")
                .clientSecret("test-secret")
                .build();

        // When
        EhrConnector connector1 = factory.createConnector(config);
        EhrConnector connector2 = factory.createConnector(config);

        // Then
        assertThat(connector1).isNotSameAs(connector2);
    }

    @Test
    @DisplayName("Should handle configuration with additional properties")
    void shouldHandleAdditionalProperties() {
        // Given
        EhrConnectionConfig config = EhrConnectionConfig.builder()
                .tenantId("tenant-1")
                .vendorType(EhrVendorType.EPIC)
                .baseUrl("https://fhir.epic.com/api/FHIR/R4")
                .clientId("test-client")
                .clientSecret("test-secret")
                .additionalProperties(java.util.Map.of("key1", "value1"))
                .build();

        // When
        EhrConnector connector = factory.createConnector(config);

        // Then
        assertThat(connector).isNotNull();
        assertThat(connector.getConfig().getAdditionalProperties()).containsEntry("key1", "value1");
    }
}

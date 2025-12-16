package com.healthdata.migration.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;

/**
 * Unit tests for SourceConnectorFactory
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SourceConnectorFactory")
class SourceConnectorFactoryTest {

    @InjectMocks
    private SourceConnectorFactory factory;

    @Nested
    @DisplayName("Connector Creation")
    class ConnectorCreationTests {

        @Test
        @DisplayName("Should create FileSourceConnector for FILE type")
        void shouldCreateFileConnector() {
            // When
            SourceConnector connector = factory.create(SourceType.FILE);

            // Then
            assertThat(connector).isNotNull();
            assertThat(connector).isInstanceOf(FileSourceConnector.class);
            assertThat(connector.getType()).isEqualTo(SourceType.FILE);
        }

        @Test
        @DisplayName("Should create SftpSourceConnector for SFTP type")
        void shouldCreateSftpConnector() {
            // When
            SourceConnector connector = factory.create(SourceType.SFTP);

            // Then
            assertThat(connector).isNotNull();
            assertThat(connector).isInstanceOf(SftpSourceConnector.class);
            assertThat(connector.getType()).isEqualTo(SourceType.SFTP);
        }

        @Test
        @DisplayName("Should create MllpSourceConnector for MLLP type")
        void shouldCreateMllpConnector() {
            // When
            SourceConnector connector = factory.create(SourceType.MLLP);

            // Then
            assertThat(connector).isNotNull();
            assertThat(connector).isInstanceOf(MllpSourceConnector.class);
            assertThat(connector.getType()).isEqualTo(SourceType.MLLP);
        }

        @Test
        @DisplayName("Should create different instances for each call")
        void shouldCreateDifferentInstances() {
            // When
            SourceConnector connector1 = factory.create(SourceType.FILE);
            SourceConnector connector2 = factory.create(SourceType.FILE);

            // Then
            assertThat(connector1).isNotSameAs(connector2);
        }
    }

    @Nested
    @DisplayName("Create and Connect")
    class CreateAndConnectTests {

        @Test
        @DisplayName("Should throw exception when config is null")
        void shouldThrowWhenConfigIsNull() {
            // When/Then
            assertThatThrownBy(() -> factory.createAndConnect(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should not be connected initially after create")
        void shouldNotBeConnectedInitially() {
            // When
            SourceConnector connector = factory.create(SourceType.FILE);

            // Then
            assertThat(connector.isConnected()).isFalse();
        }
    }

    @Nested
    @DisplayName("Test Connection")
    class TestConnectionTests {

        @Test
        @DisplayName("Should return false for invalid file path")
        void shouldReturnFalseForInvalidPath() {
            // Given
            SourceConfig config = SourceConfig.forFile("/non/existent/path", "*.hl7", false);

            // When
            boolean result = factory.testConnection(config);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should handle null config gracefully")
        void shouldHandleNullConfig() {
            // When
            boolean result = factory.testConnection(null);

            // Then - factory handles exceptions and returns false
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Factory Method Integration")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create connectors for all source types")
        void shouldCreateConnectorsForAllTypes() {
            // When/Then
            for (SourceType type : SourceType.values()) {
                SourceConnector connector = factory.create(type);
                assertThat(connector).isNotNull();
                assertThat(connector.getType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("Should create correct connector type based on config")
        void shouldCreateCorrectConnectorFromConfig() {
            // Given
            SourceConfig fileConfig = SourceConfig.forFile("/data/test", "*.hl7", false);
            SourceConfig sftpConfig = SourceConfig.forSftpPassword(
                    "sftp.example.com", 22, "user", "pass", "/remote/path");
            SourceConfig mllpConfig = SourceConfig.forMllp(2575, false);

            // When
            SourceConnector fileConnector = factory.create(fileConfig.getSourceType());
            SourceConnector sftpConnector = factory.create(sftpConfig.getSourceType());
            SourceConnector mllpConnector = factory.create(mllpConfig.getSourceType());

            // Then
            assertThat(fileConnector).isInstanceOf(FileSourceConnector.class);
            assertThat(sftpConnector).isInstanceOf(SftpSourceConnector.class);
            assertThat(mllpConnector).isInstanceOf(MllpSourceConnector.class);
        }
    }
}

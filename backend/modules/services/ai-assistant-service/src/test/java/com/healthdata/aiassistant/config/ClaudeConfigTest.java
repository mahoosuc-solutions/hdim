package com.healthdata.aiassistant.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ClaudeConfig configuration class.
 *
 * Tests TDD principles:
 * - Default configuration values
 * - Query type validation
 * - Configuration validation logic
 */
@DisplayName("ClaudeConfig Tests")
class ClaudeConfigTest {

    private ClaudeConfig config;

    @BeforeEach
    void setUp() {
        config = new ClaudeConfig();
    }

    @Test
    @DisplayName("Should have correct default configuration values")
    void testDefaultConfigurationValues() {
        // Verify default values are set correctly
        assertThat(config.isEnabled()).isFalse();
        assertThat(config.getApiUrl()).isEqualTo("https://api.anthropic.com/v1");
        assertThat(config.getModel()).isEqualTo("claude-3-5-sonnet-20241022");
        assertThat(config.getMaxTokens()).isEqualTo(4096);
        assertThat(config.getTemperature()).isEqualTo(0.3);
        assertThat(config.getTopP()).isEqualTo(0.9);
        assertThat(config.getTimeoutSeconds()).isEqualTo(60);
        assertThat(config.getMaxRetries()).isEqualTo(3);
        assertThat(config.getRateLimitPerMinute()).isEqualTo(60);
        assertThat(config.isCachingEnabled()).isTrue();
        assertThat(config.getCacheTtlSeconds()).isEqualTo(300);
        assertThat(config.isDebugLogging()).isFalse();
    }

    @Test
    @DisplayName("Should have correct default allowed query types")
    void testDefaultAllowedQueryTypes() {
        // Verify default allowed query types
        assertThat(config.getAllowedQueryTypes())
            .hasSize(6)
            .contains(
                "care_gaps",
                "quality_measures",
                "patient_summary",
                "measure_compliance",
                "population_health",
                "care_recommendations"
            );
    }

    @Test
    @DisplayName("Should allow valid query types")
    void testIsQueryTypeAllowed_WithAllowedTypes() {
        // Test each allowed query type
        assertThat(config.isQueryTypeAllowed("care_gaps")).isTrue();
        assertThat(config.isQueryTypeAllowed("quality_measures")).isTrue();
        assertThat(config.isQueryTypeAllowed("patient_summary")).isTrue();
        assertThat(config.isQueryTypeAllowed("measure_compliance")).isTrue();
        assertThat(config.isQueryTypeAllowed("population_health")).isTrue();
        assertThat(config.isQueryTypeAllowed("care_recommendations")).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid query types")
    void testIsQueryTypeAllowed_WithDisallowedTypes() {
        // Test disallowed query types
        assertThat(config.isQueryTypeAllowed("invalid_type")).isFalse();
        assertThat(config.isQueryTypeAllowed("sql_injection")).isFalse();
        assertThat(config.isQueryTypeAllowed("system_command")).isFalse();
        assertThat(config.isQueryTypeAllowed("")).isFalse();
        assertThat(config.isQueryTypeAllowed(null)).isFalse();
    }

    @Test
    @DisplayName("Should not throw exception when disabled and no API key")
    void testValidateConfiguration_WhenDisabled() {
        // Given: Claude is disabled with no API key
        config.setEnabled(false);
        config.setApiKey(null);

        // When/Then: Validation should not throw exception
        config.validateConfiguration();

        // Verify it remains disabled
        assertThat(config.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when enabled but no API key")
    void testValidateConfiguration_WhenEnabledWithoutApiKey() {
        // Given: Claude is enabled with no API key
        config.setEnabled(true);
        config.setApiKey(null);

        // When/Then: Validation should throw IllegalStateException
        assertThatThrownBy(() -> config.validateConfiguration())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Claude API key is required when claude.enabled=true");
    }

    @Test
    @DisplayName("Should throw exception when enabled with blank API key")
    void testValidateConfiguration_WhenEnabledWithBlankApiKey() {
        // Given: Claude is enabled with blank API key
        config.setEnabled(true);
        config.setApiKey("   ");

        // When/Then: Validation should throw IllegalStateException
        assertThatThrownBy(() -> config.validateConfiguration())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Claude API key is required when claude.enabled=true");
    }

    @Test
    @DisplayName("Should not throw exception when enabled with valid API key")
    void testValidateConfiguration_WhenEnabledWithValidApiKey() {
        // Given: Claude is enabled with valid API key
        config.setEnabled(true);
        config.setApiKey("sk-ant-api-key-12345678901234567890");

        // When/Then: Validation should not throw exception
        config.validateConfiguration();

        // Verify configuration is valid
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getApiKey()).isEqualTo("sk-ant-api-key-12345678901234567890");
    }

    @Test
    @DisplayName("Should have non-empty system prompt")
    void testSystemPrompt_IsNotEmpty() {
        // Verify system prompt is configured
        assertThat(config.getSystemPrompt())
            .isNotNull()
            .isNotBlank()
            .contains("clinical AI assistant")
            .contains("HDIM")
            .contains("quality measures")
            .contains("care gaps");
    }

    @Test
    @DisplayName("Should accept custom allowed query types")
    void testCustomAllowedQueryTypes() {
        // Given: Custom allowed query types
        config.getAllowedQueryTypes().clear();
        config.getAllowedQueryTypes().add("custom_type");

        // When/Then: Only custom type should be allowed
        assertThat(config.isQueryTypeAllowed("custom_type")).isTrue();
        assertThat(config.isQueryTypeAllowed("care_gaps")).isFalse();
    }

    @Test
    @DisplayName("Should allow updating configuration values")
    void testConfigurationSetters() {
        // Test all setters work correctly
        config.setEnabled(true);
        config.setApiKey("test-key");
        config.setApiUrl("https://custom-url.com");
        config.setModel("claude-3-opus-20240229");
        config.setMaxTokens(8192);
        config.setTemperature(0.7);
        config.setTopP(0.95);
        config.setTimeoutSeconds(120);
        config.setMaxRetries(5);
        config.setRateLimitPerMinute(100);
        config.setCachingEnabled(false);
        config.setCacheTtlSeconds(600);
        config.setDebugLogging(true);

        // Verify all values are updated
        assertThat(config.isEnabled()).isTrue();
        assertThat(config.getApiKey()).isEqualTo("test-key");
        assertThat(config.getApiUrl()).isEqualTo("https://custom-url.com");
        assertThat(config.getModel()).isEqualTo("claude-3-opus-20240229");
        assertThat(config.getMaxTokens()).isEqualTo(8192);
        assertThat(config.getTemperature()).isEqualTo(0.7);
        assertThat(config.getTopP()).isEqualTo(0.95);
        assertThat(config.getTimeoutSeconds()).isEqualTo(120);
        assertThat(config.getMaxRetries()).isEqualTo(5);
        assertThat(config.getRateLimitPerMinute()).isEqualTo(100);
        assertThat(config.isCachingEnabled()).isFalse();
        assertThat(config.getCacheTtlSeconds()).isEqualTo(600);
        assertThat(config.isDebugLogging()).isTrue();
    }
}

package com.healthdata.notification.infrastructure.providers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FirebasePushProvider.
 *
 * Note: These tests verify the provider's behavior without making actual Firebase API calls.
 * The Firebase SDK initialization is not performed in tests; instead, we test
 * the provider's configuration validation and availability logic.
 */
@ExtendWith(MockitoExtension.class)
class FirebasePushProviderTest {

    @Mock
    private FirebasePushProperties properties;

    private FirebasePushProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FirebasePushProvider(properties);
    }

    @Nested
    @DisplayName("Provider Name")
    class ProviderNameTests {

        @Test
        @DisplayName("should return 'firebase' as provider name")
        void shouldReturnFirebaseAsProviderName() {
            assertThat(provider.getProviderName()).isEqualTo("firebase");
        }
    }

    @Nested
    @DisplayName("Availability Checks")
    class AvailabilityTests {

        @Test
        @DisplayName("should return false when not initialized")
        void shouldReturnFalseWhenNotInitialized() {
            // Provider is not initialized (init() not called with valid credentials)
            lenient().when(properties.isEnabled()).thenReturn(true);

            assertThat(provider.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should return false when disabled")
        void shouldReturnFalseWhenDisabled() {
            // When disabled, isAvailable() should return false
            lenient().when(properties.isEnabled()).thenReturn(false);

            assertThat(provider.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Initialization")
    class InitializationTests {

        @Test
        @DisplayName("should not initialize when credentials JSON is null")
        void shouldNotInitializeWhenCredentialsJsonIsNull() {
            lenient().when(properties.getCredentialsJson()).thenReturn(null);
            lenient().when(properties.isEnabled()).thenReturn(true);

            provider.init();

            assertThat(provider.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should not initialize when credentials JSON is empty")
        void shouldNotInitializeWhenCredentialsJsonIsEmpty() {
            lenient().when(properties.getCredentialsJson()).thenReturn("");
            lenient().when(properties.isEnabled()).thenReturn(true);

            provider.init();

            assertThat(provider.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should not initialize with invalid credentials JSON")
        void shouldNotInitializeWithInvalidCredentialsJson() {
            lenient().when(properties.getCredentialsJson()).thenReturn("invalid-json");
            lenient().when(properties.getProjectId()).thenReturn("test-project");
            lenient().when(properties.isEnabled()).thenReturn(true);

            provider.init();

            assertThat(provider.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Send Push Notification")
    class SendPushTests {

        @Test
        @DisplayName("should throw exception when provider not available")
        void shouldThrowExceptionWhenProviderNotAvailable() {
            // Provider is not initialized, so isAvailable() returns false
            lenient().when(properties.isEnabled()).thenReturn(false);

            assertThatThrownBy(() -> provider.send("device-token", "Title", "Body"))
                .isInstanceOf(FirebasePushProvider.PushProviderException.class)
                .hasMessageContaining("not available");
        }
    }

    @Nested
    @DisplayName("Send with Data")
    class SendWithDataTests {

        @Test
        @DisplayName("should throw exception when provider not available")
        void shouldThrowExceptionWhenProviderNotAvailable() {
            // Provider is not initialized, so isAvailable() returns false
            lenient().when(properties.isEnabled()).thenReturn(false);
            Map<String, String> data = Map.of("key", "value");

            assertThatThrownBy(() -> provider.sendWithData("device-token", "Title", "Body", data))
                .isInstanceOf(FirebasePushProvider.PushProviderException.class)
                .hasMessageContaining("not available");
        }
    }

    @Nested
    @DisplayName("Send to Topic")
    class SendToTopicTests {

        @Test
        @DisplayName("should throw exception when provider not available")
        void shouldThrowExceptionWhenProviderNotAvailable() {
            // Provider is not initialized, so isAvailable() returns false
            lenient().when(properties.isEnabled()).thenReturn(false);

            assertThatThrownBy(() -> provider.sendToTopic("clinical-alerts", "Title", "Body"))
                .isInstanceOf(FirebasePushProvider.PushProviderException.class)
                .hasMessageContaining("not available");
        }
    }

    @Nested
    @DisplayName("Send Silent Notification")
    class SendSilentTests {

        @Test
        @DisplayName("should throw exception when provider not available")
        void shouldThrowExceptionWhenProviderNotAvailable() {
            // Provider is not initialized, so isAvailable() returns false
            lenient().when(properties.isEnabled()).thenReturn(false);
            Map<String, String> data = Map.of("sync", "true");

            assertThatThrownBy(() -> provider.sendSilent("device-token", data))
                .isInstanceOf(FirebasePushProvider.PushProviderException.class)
                .hasMessageContaining("not available");
        }
    }

    @Nested
    @DisplayName("Fallback Methods")
    class FallbackTests {

        @Test
        @DisplayName("sendFallback should throw PushProviderException")
        void sendFallbackShouldThrowException() {
            Throwable cause = new RuntimeException("Connection failed");

            assertThatThrownBy(() -> provider.sendFallback("token", "Title", "Body", cause))
                .isInstanceOf(FirebasePushProvider.PushProviderException.class)
                .hasMessageContaining("temporarily unavailable");
        }

        @Test
        @DisplayName("sendWithDataFallback should throw PushProviderException")
        void sendWithDataFallbackShouldThrowException() {
            Throwable cause = new RuntimeException("Connection failed");
            Map<String, String> data = Map.of("key", "value");

            assertThatThrownBy(() -> provider.sendWithDataFallback("token", "Title", "Body", data, cause))
                .isInstanceOf(FirebasePushProvider.PushProviderException.class)
                .hasMessageContaining("temporarily unavailable");
        }

        @Test
        @DisplayName("sendToTopicFallback should throw PushProviderException")
        void sendToTopicFallbackShouldThrowException() {
            Throwable cause = new RuntimeException("Connection failed");

            assertThatThrownBy(() -> provider.sendToTopicFallback("topic", "Title", "Body", cause))
                .isInstanceOf(FirebasePushProvider.PushProviderException.class)
                .hasMessageContaining("temporarily unavailable");
        }

        @Test
        @DisplayName("sendSilentFallback should throw PushProviderException")
        void sendSilentFallbackShouldThrowException() {
            Throwable cause = new RuntimeException("Connection failed");
            Map<String, String> data = Map.of("sync", "true");

            assertThatThrownBy(() -> provider.sendSilentFallback("token", data, cause))
                .isInstanceOf(FirebasePushProvider.PushProviderException.class)
                .hasMessageContaining("temporarily unavailable");
        }
    }
}

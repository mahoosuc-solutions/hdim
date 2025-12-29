package com.healthdata.notification.infrastructure.providers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TwilioSmsProvider.
 *
 * Note: These tests verify the provider's behavior without making actual Twilio API calls.
 * The Twilio SDK is not mocked at the static level to avoid complexity; instead, we test
 * the provider's initialization and availability logic.
 */
@ExtendWith(MockitoExtension.class)
class TwilioSmsProviderTest {

    @Mock
    private TwilioSmsProperties properties;

    private TwilioSmsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TwilioSmsProvider(properties);
    }

    @Nested
    @DisplayName("Provider Name")
    class ProviderNameTests {

        @Test
        @DisplayName("should return 'twilio' as provider name")
        void shouldReturnTwilioAsProviderName() {
            assertThat(provider.getProviderName()).isEqualTo("twilio");
        }
    }

    @Nested
    @DisplayName("Availability Checks")
    class AvailabilityTests {

        @Test
        @DisplayName("should return false when not initialized")
        void shouldReturnFalseWhenNotInitialized() {
            // Provider is not initialized (init() not called with valid credentials)
            // Even if enabled is true, isAvailable() should return false
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
        @DisplayName("should not initialize when account SID is null")
        void shouldNotInitializeWhenAccountSidIsNull() {
            lenient().when(properties.getAccountSid()).thenReturn(null);
            lenient().when(properties.isEnabled()).thenReturn(true);

            provider.init();

            assertThat(provider.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should not initialize when account SID is empty")
        void shouldNotInitializeWhenAccountSidIsEmpty() {
            lenient().when(properties.getAccountSid()).thenReturn("");
            lenient().when(properties.isEnabled()).thenReturn(true);

            provider.init();

            assertThat(provider.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should not initialize when auth token is null")
        void shouldNotInitializeWhenAuthTokenIsNull() {
            lenient().when(properties.getAccountSid()).thenReturn("AC123");
            lenient().when(properties.getAuthToken()).thenReturn(null);
            lenient().when(properties.isEnabled()).thenReturn(true);

            provider.init();

            assertThat(provider.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should not initialize when auth token is empty")
        void shouldNotInitializeWhenAuthTokenIsEmpty() {
            lenient().when(properties.getAccountSid()).thenReturn("AC123");
            lenient().when(properties.getAuthToken()).thenReturn("");
            lenient().when(properties.isEnabled()).thenReturn(true);

            provider.init();

            assertThat(provider.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Send SMS")
    class SendSmsTests {

        @Test
        @DisplayName("should throw exception when provider not available")
        void shouldThrowExceptionWhenProviderNotAvailable() {
            // Provider is not initialized, so isAvailable() returns false
            lenient().when(properties.isEnabled()).thenReturn(false);

            assertThatThrownBy(() -> provider.send("+15551234567", "Test message"))
                .isInstanceOf(TwilioSmsProvider.SmsProviderException.class)
                .hasMessageContaining("not available");
        }
    }

    @Nested
    @DisplayName("Send with Sender ID")
    class SendWithSenderIdTests {

        @Test
        @DisplayName("should throw exception when provider not available")
        void shouldThrowExceptionWhenProviderNotAvailable() {
            // Provider is not initialized, so isAvailable() returns false
            lenient().when(properties.isEnabled()).thenReturn(false);

            assertThatThrownBy(() -> provider.sendWithSenderId("+15551234567", "Test message", "HealthData"))
                .isInstanceOf(TwilioSmsProvider.SmsProviderException.class)
                .hasMessageContaining("not available");
        }
    }

    @Nested
    @DisplayName("Fallback Methods")
    class FallbackTests {

        @Test
        @DisplayName("sendFallback should throw SmsProviderException")
        void sendFallbackShouldThrowException() {
            Throwable cause = new RuntimeException("Connection failed");

            assertThatThrownBy(() -> provider.sendFallback("+15551234567", "Test", cause))
                .isInstanceOf(TwilioSmsProvider.SmsProviderException.class)
                .hasMessageContaining("temporarily unavailable");
        }

        @Test
        @DisplayName("sendWithSenderIdFallback should throw SmsProviderException")
        void sendWithSenderIdFallbackShouldThrowException() {
            Throwable cause = new RuntimeException("Connection failed");

            assertThatThrownBy(() -> provider.sendWithSenderIdFallback("+15551234567", "Test", "Sender", cause))
                .isInstanceOf(TwilioSmsProvider.SmsProviderException.class)
                .hasMessageContaining("temporarily unavailable");
        }
    }
}

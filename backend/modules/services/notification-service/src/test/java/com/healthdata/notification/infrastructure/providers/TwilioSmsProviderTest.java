package com.healthdata.notification.infrastructure.providers;

import com.twilio.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Twilio SMS Provider
 *
 * Note: These tests focus on validation logic.
 * Integration tests with WireMock will be in separate test class.
 */
@ExtendWith(MockitoExtension.class)
@Tag("integration")  // Requires Twilio SDK initialization with real/mock credentials
class TwilioSmsProviderTest {

    @Mock
    private TwilioSmsProperties properties;

    private TwilioSmsProvider provider;

    @BeforeEach
    void setUp() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getAccountSid()).thenReturn("AC123");
        when(properties.getAuthToken()).thenReturn("secret");
        when(properties.getFromNumber()).thenReturn("+11234567890");
        when(properties.getMaxMessageLength()).thenReturn(1600);

        provider = new TwilioSmsProvider(properties);
    }

    @Test
    void shouldValidateE164PhoneNumber() {
        // Given: Valid E.164 phone number
        String validPhone = "+11234567890";

        // When/Then: No exception thrown
        assertThatCode(() -> provider.send(validPhone, "Test message"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidPhoneNumber_MissingPlus() {
        // Given: Phone number missing +
        String invalidPhone = "11234567890";

        // When/Then: Throws IllegalArgumentException
        assertThatThrownBy(() -> provider.send(invalidPhone, "Test message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E.164 format");
    }

    @Test
    void shouldRejectInvalidPhoneNumber_TooShort() {
        // Given: Phone number too short
        String invalidPhone = "+1234";

        // When/Then: Throws IllegalArgumentException
        assertThatThrownBy(() -> provider.send(invalidPhone, "Test message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E.164 format");
    }

    @Test
    void shouldRejectNullPhoneNumber() {
        // When/Then: Throws IllegalArgumentException
        assertThatThrownBy(() -> provider.send(null, "Test message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number is required");
    }

    @Test
    void shouldRejectBlankMessage() {
        // When/Then: Throws IllegalArgumentException
        assertThatThrownBy(() -> provider.send("+11234567890", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message is required");
    }

    @Test
    void shouldRejectMessageTooLong() {
        // Given: Message exceeding max length
        when(properties.getMaxMessageLength()).thenReturn(160);
        String longMessage = "a".repeat(161);

        // When/Then: Throws IllegalArgumentException
        assertThatThrownBy(() -> provider.send("+11234567890", longMessage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum");
    }

    @Test
    void shouldValidateSenderId_MaxLength11() {
        // Given: Sender ID with 12 characters
        String invalidSenderId = "TooLongName!";

        // When/Then: Throws IllegalArgumentException
        assertThatThrownBy(() -> provider.sendWithSenderId("+11234567890", "Test", invalidSenderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("11 characters or less");
    }

    @Test
    void shouldValidateSenderId_AlphanumericOnly() {
        // Given: Sender ID with special characters
        String invalidSenderId = "Health@Data";

        // When/Then: Throws IllegalArgumentException
        assertThatThrownBy(() -> provider.sendWithSenderId("+11234567890", "Test", invalidSenderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A-Z, a-z, 0-9");
    }

    @Test
    void isAvailable_ShouldReturnTrue_WhenEnabled() {
        // When
        boolean available = provider.isAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void getProviderName_ShouldReturnTwilio() {
        // When
        String name = provider.getProviderName();

        // Then
        assertThat(name).isEqualTo("Twilio");
    }
}

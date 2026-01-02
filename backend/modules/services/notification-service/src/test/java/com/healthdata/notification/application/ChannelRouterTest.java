package com.healthdata.notification.application;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.infrastructure.providers.NotificationProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChannelRouter.
 */
@DisplayName("ChannelRouter Tests")
class ChannelRouterTest {

    @Nested
    @DisplayName("Provider Registration Tests")
    class ProviderRegistrationTests {

        @Test
        @DisplayName("should register providers for supported channels")
        void registerProvidersForSupportedChannels() {
            // Given
            NotificationProvider emailProvider = createMockProvider(Set.of(NotificationChannel.EMAIL));
            NotificationProvider smsProvider = createMockProvider(Set.of(NotificationChannel.SMS));

            // When
            ChannelRouter router = new ChannelRouter(List.of(emailProvider, smsProvider));

            // Then
            assertThat(router.isChannelSupported(NotificationChannel.EMAIL)).isTrue();
            assertThat(router.isChannelSupported(NotificationChannel.SMS)).isTrue();
        }

        @Test
        @DisplayName("should register provider supporting multiple channels")
        void registerProviderSupportingMultipleChannels() {
            // Given
            NotificationProvider multiProvider = createMockProvider(
                Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP)
            );

            // When
            ChannelRouter router = new ChannelRouter(List.of(multiProvider));

            // Then
            assertThat(router.isChannelSupported(NotificationChannel.EMAIL)).isTrue();
            assertThat(router.isChannelSupported(NotificationChannel.IN_APP)).isTrue();
            assertThat(router.getProvider(NotificationChannel.EMAIL)).isSameAs(multiProvider);
            assertThat(router.getProvider(NotificationChannel.IN_APP)).isSameAs(multiProvider);
        }

        @Test
        @DisplayName("should handle empty provider list")
        void handleEmptyProviderList() {
            // When
            ChannelRouter router = new ChannelRouter(List.of());

            // Then
            assertThat(router.isChannelSupported(NotificationChannel.EMAIL)).isFalse();
            assertThat(router.isChannelSupported(NotificationChannel.SMS)).isFalse();
        }
    }

    @Nested
    @DisplayName("Get Provider Tests")
    class GetProviderTests {

        @Test
        @DisplayName("should return correct provider for channel")
        void returnCorrectProviderForChannel() {
            // Given
            NotificationProvider emailProvider = createMockProvider(Set.of(NotificationChannel.EMAIL));
            NotificationProvider smsProvider = createMockProvider(Set.of(NotificationChannel.SMS));
            ChannelRouter router = new ChannelRouter(List.of(emailProvider, smsProvider));

            // When/Then
            assertThat(router.getProvider(NotificationChannel.EMAIL)).isSameAs(emailProvider);
            assertThat(router.getProvider(NotificationChannel.SMS)).isSameAs(smsProvider);
        }

        @Test
        @DisplayName("should throw exception for unsupported channel")
        void throwExceptionForUnsupportedChannel() {
            // Given
            NotificationProvider emailProvider = createMockProvider(Set.of(NotificationChannel.EMAIL));
            ChannelRouter router = new ChannelRouter(List.of(emailProvider));

            // When/Then
            assertThatThrownBy(() -> router.getProvider(NotificationChannel.SMS))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("No provider registered for channel: SMS");
        }

        @Test
        @DisplayName("should throw exception for PUSH channel when not registered")
        void throwExceptionForPushChannelWhenNotRegistered() {
            // Given
            NotificationProvider emailProvider = createMockProvider(Set.of(NotificationChannel.EMAIL));
            ChannelRouter router = new ChannelRouter(List.of(emailProvider));

            // When/Then
            assertThatThrownBy(() -> router.getProvider(NotificationChannel.PUSH))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("PUSH");
        }
    }

    @Nested
    @DisplayName("Channel Support Tests")
    class ChannelSupportTests {

        @Test
        @DisplayName("should return true for supported channels")
        void returnTrueForSupportedChannels() {
            // Given
            NotificationProvider provider = createMockProvider(
                Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.IN_APP)
            );
            ChannelRouter router = new ChannelRouter(List.of(provider));

            // When/Then
            assertThat(router.isChannelSupported(NotificationChannel.EMAIL)).isTrue();
            assertThat(router.isChannelSupported(NotificationChannel.SMS)).isTrue();
            assertThat(router.isChannelSupported(NotificationChannel.IN_APP)).isTrue();
        }

        @Test
        @DisplayName("should return false for unsupported channels")
        void returnFalseForUnsupportedChannels() {
            // Given
            NotificationProvider provider = createMockProvider(Set.of(NotificationChannel.EMAIL));
            ChannelRouter router = new ChannelRouter(List.of(provider));

            // When/Then
            assertThat(router.isChannelSupported(NotificationChannel.SMS)).isFalse();
            assertThat(router.isChannelSupported(NotificationChannel.PUSH)).isFalse();
            assertThat(router.isChannelSupported(NotificationChannel.IN_APP)).isFalse();
        }
    }

    @Nested
    @DisplayName("Provider Override Tests")
    class ProviderOverrideTests {

        @Test
        @DisplayName("should override provider when multiple support same channel")
        void overrideProviderWhenMultipleSupportSameChannel() {
            // Given - when multiple providers support same channel, last one wins
            NotificationProvider provider1 = createMockProvider(Set.of(NotificationChannel.EMAIL));
            NotificationProvider provider2 = createMockProvider(Set.of(NotificationChannel.EMAIL));

            // When
            ChannelRouter router = new ChannelRouter(List.of(provider1, provider2));

            // Then - provider2 should be registered (last one wins due to Map.put)
            assertThat(router.getProvider(NotificationChannel.EMAIL)).isSameAs(provider2);
        }
    }

    private NotificationProvider createMockProvider(Set<NotificationChannel> channels) {
        NotificationProvider provider = mock(NotificationProvider.class);
        when(provider.getSupportedChannels()).thenReturn(channels);
        return provider;
    }
}

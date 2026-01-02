package com.healthdata.agent.llm;

import com.healthdata.agent.llm.config.LLMProviderConfig;
import com.healthdata.agent.llm.providers.ClaudeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LLMProviderFactory Tests")
class LLMProviderFactoryTest {

    @Mock
    private ClaudeProvider claudeProvider;

    @Mock
    private LLMProvider azureProvider;

    @Mock
    private LLMProviderConfig providerConfig;

    private LLMProviderFactory factory;

    @BeforeEach
    void setUp() {
        when(claudeProvider.getName()).thenReturn("claude");
        when(azureProvider.getName()).thenReturn("azure-openai");

        factory = new LLMProviderFactory(List.of(claudeProvider, azureProvider), providerConfig);
    }

    @Nested
    @DisplayName("Provider Retrieval")
    class ProviderRetrievalTests {

        @Test
        @DisplayName("should get provider by name")
        void getProviderByName() {
            LLMProvider provider = factory.getProvider("claude");

            assertThat(provider).isNotNull();
            assertThat(provider.getName()).isEqualTo("claude");
        }

        @Test
        @DisplayName("should throw for unknown provider")
        void getUnknownProvider() {
            assertThatThrownBy(() -> factory.getProvider("unknown"))
                .isInstanceOf(LLMProviderFactory.LLMProviderNotFoundException.class)
                .hasMessageContaining("unknown");
        }

        @Test
        @DisplayName("should list all available providers")
        void listProviders() {
            List<String> providers = factory.listProviders();

            assertThat(providers).containsExactlyInAnyOrder("azure-openai", "claude");
        }
    }

    @Nested
    @DisplayName("Provider If Healthy")
    class ProviderIfHealthyTests {

        @Test
        @DisplayName("should return provider when healthy")
        void getProviderIfHealthyWhenAvailable() {
            when(claudeProvider.isAvailable()).thenReturn(true);

            Optional<LLMProvider> provider = factory.getProviderIfHealthy("claude");

            assertThat(provider).isPresent();
            assertThat(provider.get().getName()).isEqualTo("claude");
        }

        @Test
        @DisplayName("should return empty when provider is unhealthy")
        void getProviderIfHealthyWhenUnavailable() {
            when(claudeProvider.isAvailable()).thenReturn(false);

            Optional<LLMProvider> provider = factory.getProviderIfHealthy("claude");

            assertThat(provider).isEmpty();
        }

        @Test
        @DisplayName("should return empty for unknown provider")
        void getProviderIfHealthyUnknown() {
            Optional<LLMProvider> provider = factory.getProviderIfHealthy("unknown");

            assertThat(provider).isEmpty();
        }
    }

    @Nested
    @DisplayName("Default Provider")
    class DefaultProviderTests {

        @Test
        @DisplayName("should get default provider from config")
        void getDefaultProvider() {
            when(providerConfig.getDefaultProvider()).thenReturn("claude");

            LLMProvider provider = factory.getDefaultProvider();

            assertThat(provider).isNotNull();
            assertThat(provider.getName()).isEqualTo("claude");
        }

        @Test
        @DisplayName("should throw when default provider not found")
        void defaultProviderNotFound() {
            when(providerConfig.getDefaultProvider()).thenReturn("non-existent");

            assertThatThrownBy(() -> factory.getDefaultProvider())
                .isInstanceOf(LLMProviderFactory.LLMProviderNotFoundException.class)
                .hasMessageContaining("non-existent");
        }
    }

    @Nested
    @DisplayName("Provider With Fallback")
    class FallbackTests {

        @Test
        @DisplayName("should return first healthy provider in fallback chain")
        void getProviderWithFallback() {
            when(providerConfig.getFallbackChain()).thenReturn(List.of("claude", "azure-openai"));
            when(claudeProvider.isAvailable()).thenReturn(true);

            LLMProvider provider = factory.getProviderWithFallback();

            assertThat(provider.getName()).isEqualTo("claude");
        }

        @Test
        @DisplayName("should fallback to next provider when first is unavailable")
        void getProviderWithFallbackSkipsUnavailable() {
            when(providerConfig.getFallbackChain()).thenReturn(List.of("claude", "azure-openai"));
            when(claudeProvider.isAvailable()).thenReturn(false);
            when(azureProvider.isAvailable()).thenReturn(true);

            LLMProvider provider = factory.getProviderWithFallback();

            assertThat(provider.getName()).isEqualTo("azure-openai");
        }

        @Test
        @DisplayName("should throw when all providers unavailable")
        void getProviderWithFallbackAllUnavailable() {
            when(providerConfig.getFallbackChain()).thenReturn(List.of("claude", "azure-openai"));
            when(claudeProvider.isAvailable()).thenReturn(false);
            when(azureProvider.isAvailable()).thenReturn(false);

            assertThatThrownBy(() -> factory.getProviderWithFallback())
                .isInstanceOf(LLMProviderFactory.NoAvailableLLMProviderException.class);
        }
    }

    @Nested
    @DisplayName("Health Status")
    class HealthStatusTests {

        @Test
        @DisplayName("should list healthy providers")
        void listHealthyProviders() {
            when(claudeProvider.isAvailable()).thenReturn(true);
            when(azureProvider.isAvailable()).thenReturn(false);

            List<String> healthy = factory.listHealthyProviders();

            assertThat(healthy).containsExactly("claude");
        }

        @Test
        @DisplayName("should return empty when no providers are healthy")
        void listHealthyProvidersEmpty() {
            when(claudeProvider.isAvailable()).thenReturn(false);
            when(azureProvider.isAvailable()).thenReturn(false);

            List<String> healthy = factory.listHealthyProviders();

            assertThat(healthy).isEmpty();
        }
    }
}

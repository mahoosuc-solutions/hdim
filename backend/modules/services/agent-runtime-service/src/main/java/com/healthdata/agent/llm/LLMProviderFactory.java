package com.healthdata.agent.llm;

import com.healthdata.agent.llm.config.LLMProviderConfig;
import com.healthdata.agent.llm.model.LLMRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Factory for managing and selecting LLM providers.
 * Handles provider registration, selection, and fallback logic.
 */
@Slf4j
@Service
public class LLMProviderFactory {

    private final Map<String, LLMProvider> providers = new ConcurrentHashMap<>();
    private final LLMProviderConfig config;

    public LLMProviderFactory(List<LLMProvider> providerList, LLMProviderConfig config) {
        this.config = config;

        // Register all available providers
        providerList.forEach(provider -> {
            providers.put(provider.getName(), provider);
            log.info("Registered LLM provider: {}", provider.getName());
        });

        log.info("Initialized LLM provider factory with {} providers: {}",
            providers.size(), providers.keySet());
    }

    /**
     * Get the default provider.
     */
    public LLMProvider getDefaultProvider() {
        return getProvider(config.getDefaultProvider());
    }

    /**
     * Get a specific provider by name.
     */
    public LLMProvider getProvider(String name) {
        LLMProvider provider = providers.get(name);
        if (provider == null) {
            throw new LLMProviderNotFoundException("Provider not found: " + name);
        }
        return provider;
    }

    /**
     * Get provider if available and healthy.
     */
    public Optional<LLMProvider> getProviderIfHealthy(String name) {
        LLMProvider provider = providers.get(name);
        if (provider != null && provider.isAvailable()) {
            return Optional.of(provider);
        }
        return Optional.empty();
    }

    /**
     * Get provider with fallback chain.
     * Returns the first healthy provider in the fallback chain.
     */
    public LLMProvider getProviderWithFallback() {
        for (String providerName : config.getFallbackChain()) {
            Optional<LLMProvider> provider = getProviderIfHealthy(providerName);
            if (provider.isPresent()) {
                return provider.get();
            }
            log.warn("Provider {} unavailable, trying next in fallback chain", providerName);
        }
        throw new NoAvailableLLMProviderException("All LLM providers are unavailable");
    }

    /**
     * Select optimal provider based on request characteristics.
     */
    public LLMProvider selectOptimalProvider(LLMRequest request) {
        // If request specifies a provider model, try to find matching provider
        if (request.getModel() != null) {
            for (LLMProvider provider : providers.values()) {
                if (provider.getAvailableModels().contains(request.getModel())
                    && provider.isAvailable()) {
                    return provider;
                }
            }
        }

        // Check for large context requirements (prefer providers with larger context)
        if (request.getMaxTokens() > 8000) {
            Optional<LLMProvider> largeContextProvider = getProviderIfHealthy("claude");
            if (largeContextProvider.isPresent()) {
                return largeContextProvider.get();
            }
        }

        // Use default with fallback
        return getProviderWithFallback();
    }

    /**
     * List all registered providers.
     */
    public List<String> listProviders() {
        return providers.keySet().stream().sorted().collect(Collectors.toList());
    }

    /**
     * List all healthy providers.
     */
    public List<String> listHealthyProviders() {
        return providers.entrySet().stream()
            .filter(e -> e.getValue().isAvailable())
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Get health status of all providers.
     */
    public Map<String, LLMProvider.HealthStatus> getHealthStatus() {
        return providers.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().health()
            ));
    }

    /**
     * Exception thrown when a provider is not found.
     */
    public static class LLMProviderNotFoundException extends RuntimeException {
        public LLMProviderNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when no healthy providers are available.
     */
    public static class NoAvailableLLMProviderException extends RuntimeException {
        public NoAvailableLLMProviderException(String message) {
            super(message);
        }
    }
}

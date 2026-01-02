package com.healthdata.agent.llm.providers;

import com.healthdata.agent.llm.LLMProvider;
import com.healthdata.agent.llm.config.LLMProviderConfig;
import com.healthdata.agent.llm.model.LLMRequest;
import com.healthdata.agent.llm.model.LLMResponse;
import com.healthdata.agent.llm.model.LLMStreamChunk;
import com.healthdata.agent.tool.ToolDefinition;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for LLM providers with common functionality.
 */
@Slf4j
public abstract class AbstractLLMProvider implements LLMProvider {

    protected final LLMProviderConfig.ProviderSettings settings;
    protected final MeterRegistry meterRegistry;
    protected final AtomicReference<HealthStatus> lastHealthStatus = new AtomicReference<>(HealthStatus.unhealthy("Not initialized"));

    // Metrics
    protected final Counter requestCounter;
    protected final Counter errorCounter;
    protected final Counter tokenCounter;
    protected final Timer latencyTimer;

    protected AbstractLLMProvider(LLMProviderConfig.ProviderSettings settings, MeterRegistry meterRegistry) {
        this.settings = settings;
        this.meterRegistry = meterRegistry;

        String providerName = getName();
        this.requestCounter = Counter.builder("llm.requests")
            .tag("provider", providerName)
            .description("Number of LLM requests")
            .register(meterRegistry);

        this.errorCounter = Counter.builder("llm.errors")
            .tag("provider", providerName)
            .description("Number of LLM errors")
            .register(meterRegistry);

        this.tokenCounter = Counter.builder("llm.tokens")
            .tag("provider", providerName)
            .description("Number of tokens used")
            .register(meterRegistry);

        this.latencyTimer = Timer.builder("llm.latency")
            .tag("provider", providerName)
            .description("LLM request latency")
            .register(meterRegistry);
    }

    @Override
    @CircuitBreaker(name = "llmProvider", fallbackMethod = "completeFallback")
    @RateLimiter(name = "llmProvider")
    @Retry(name = "llmProvider")
    public LLMResponse complete(LLMRequest request) {
        requestCounter.increment();
        long startTime = System.currentTimeMillis();

        try {
            LLMResponse response = doComplete(request);

            long latencyMs = System.currentTimeMillis() - startTime;
            response.setLatencyMs(latencyMs);
            response.setProvider(getName());

            latencyTimer.record(Duration.ofMillis(latencyMs));
            if (response.getUsage() != null) {
                tokenCounter.increment(response.getUsage().getTotalTokens());
            }

            lastHealthStatus.set(HealthStatus.healthy(latencyMs));
            return response;

        } catch (Exception e) {
            errorCounter.increment();
            lastHealthStatus.set(HealthStatus.unhealthy(e.getMessage()));
            throw e;
        }
    }

    @Override
    @CircuitBreaker(name = "llmProvider")
    @RateLimiter(name = "llmProvider")
    public Flux<LLMStreamChunk> completeStreaming(LLMRequest request) {
        requestCounter.increment();
        long startTime = System.currentTimeMillis();

        return doCompleteStreaming(request)
            .doOnComplete(() -> {
                long latencyMs = System.currentTimeMillis() - startTime;
                latencyTimer.record(Duration.ofMillis(latencyMs));
                lastHealthStatus.set(HealthStatus.healthy(latencyMs));
            })
            .doOnError(e -> {
                errorCounter.increment();
                lastHealthStatus.set(HealthStatus.unhealthy(e.getMessage()));
            });
    }

    @Override
    @CircuitBreaker(name = "llmProvider", fallbackMethod = "completeWithToolsFallback")
    @RateLimiter(name = "llmProvider")
    @Retry(name = "llmProvider")
    public LLMResponse completeWithTools(LLMRequest request, List<ToolDefinition> tools) {
        requestCounter.increment();
        long startTime = System.currentTimeMillis();

        try {
            LLMResponse response = doCompleteWithTools(request, tools);

            long latencyMs = System.currentTimeMillis() - startTime;
            response.setLatencyMs(latencyMs);
            response.setProvider(getName());

            latencyTimer.record(Duration.ofMillis(latencyMs));
            if (response.getUsage() != null) {
                tokenCounter.increment(response.getUsage().getTotalTokens());
            }

            lastHealthStatus.set(HealthStatus.healthy(latencyMs));
            return response;

        } catch (Exception e) {
            errorCounter.increment();
            lastHealthStatus.set(HealthStatus.unhealthy(e.getMessage()));
            throw e;
        }
    }

    @Override
    public HealthStatus health() {
        return lastHealthStatus.get();
    }

    @Override
    public int countTokens(String text) {
        // Simple approximation: ~4 characters per token for English
        // Override in specific providers for more accurate counting
        return (int) Math.ceil(text.length() / 4.0);
    }

    /**
     * Provider-specific completion implementation.
     */
    protected abstract LLMResponse doComplete(LLMRequest request);

    /**
     * Provider-specific streaming implementation.
     */
    protected abstract Flux<LLMStreamChunk> doCompleteStreaming(LLMRequest request);

    /**
     * Provider-specific tool completion implementation.
     */
    protected abstract LLMResponse doCompleteWithTools(LLMRequest request, List<ToolDefinition> tools);

    /**
     * Fallback method for circuit breaker.
     */
    protected LLMResponse completeFallback(LLMRequest request, Exception e) {
        log.error("LLM provider {} failed, circuit breaker triggered: {}", getName(), e.getMessage());
        throw new LLMProviderException("Provider " + getName() + " unavailable: " + e.getMessage(), e);
    }

    /**
     * Fallback method for tool completion.
     */
    protected LLMResponse completeWithToolsFallback(LLMRequest request, List<ToolDefinition> tools, Exception e) {
        log.error("LLM provider {} tool completion failed: {}", getName(), e.getMessage());
        throw new LLMProviderException("Provider " + getName() + " tool completion unavailable: " + e.getMessage(), e);
    }

    /**
     * Exception for LLM provider errors.
     */
    public static class LLMProviderException extends RuntimeException {
        public LLMProviderException(String message) {
            super(message);
        }

        public LLMProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

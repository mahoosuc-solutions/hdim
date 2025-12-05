package com.healthdata.ehr.connector;

import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.model.EhrConnectionStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.reactor.retry.RetryOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Abstract base class for all EHR connectors.
 * Provides common functionality for authentication, retry logic, circuit breaker,
 * and connection management.
 */
@Slf4j
public abstract class AbstractEhrConnector implements EhrConnector {

    protected final EhrConnectionConfig config;
    protected final WebClient webClient;
    protected final CircuitBreaker circuitBreaker;
    protected final Retry retry;

    protected EhrConnectionStatus connectionStatus;
    protected String accessToken;
    protected LocalDateTime tokenExpiryTime;

    protected AbstractEhrConnector(EhrConnectionConfig config, WebClient.Builder webClientBuilder) {
        this.config = config;
        this.webClient = buildWebClient(config, webClientBuilder);
        this.circuitBreaker = buildCircuitBreaker(config);
        this.retry = buildRetry(config);
        this.connectionStatus = initializeConnectionStatus();
    }

    /**
     * Build configured WebClient instance.
     */
    private WebClient buildWebClient(EhrConnectionConfig config, WebClient.Builder builder) {
        return builder
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Build circuit breaker with configuration.
     */
    private CircuitBreaker buildCircuitBreaker(EhrConnectionConfig config) {
        if (!config.getEnableCircuitBreaker()) {
            log.debug("Circuit breaker disabled for connection: {}", config.getConnectionId());
            return null;
        }

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .slowCallRateThreshold(50)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        return registry.circuitBreaker(config.getConnectionId() != null ?
                config.getConnectionId() : "ehr-connector-" + config.getTenantId());
    }

    /**
     * Build retry configuration.
     */
    private Retry buildRetry(EhrConnectionConfig config) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(config.getMaxRetries() != null ? config.getMaxRetries() : 3)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(Exception.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(retryConfig);
        return registry.retry(config.getConnectionId() != null ?
                config.getConnectionId() + "-retry" : "ehr-connector-retry-" + config.getTenantId());
    }

    /**
     * Initialize connection status.
     */
    private EhrConnectionStatus initializeConnectionStatus() {
        return EhrConnectionStatus.builder()
                .connectionId(config.getConnectionId())
                .tenantId(config.getTenantId())
                .vendorType(config.getVendorType())
                .status(EhrConnectionStatus.Status.DISCONNECTED)
                .consecutiveFailures(0)
                .circuitBreakerState(EhrConnectionStatus.CircuitBreakerState.CLOSED)
                .build();
    }

    @Override
    public Mono<Void> initialize(EhrConnectionConfig config) {
        log.info("Initializing EHR connector for tenant: {} vendor: {}",
                config.getTenantId(), config.getVendorType());

        connectionStatus.setStatus(EhrConnectionStatus.Status.CONNECTING);
        connectionStatus.setLastAttempt(LocalDateTime.now());

        return authenticate()
                .doOnSuccess(token -> {
                    log.info("Authentication successful for tenant: {}", config.getTenantId());
                    connectionStatus.setStatus(EhrConnectionStatus.Status.CONNECTED);
                    connectionStatus.setLastSuccessfulConnection(LocalDateTime.now());
                    connectionStatus.setConsecutiveFailures(0);
                })
                .doOnError(error -> {
                    log.error("Authentication failed for tenant: {}", config.getTenantId(), error);
                    connectionStatus.setStatus(EhrConnectionStatus.Status.AUTHENTICATION_FAILED);
                    connectionStatus.setErrorMessage(error.getMessage());
                    connectionStatus.setConsecutiveFailures(
                            connectionStatus.getConsecutiveFailures() + 1);
                })
                .then();
    }

    @Override
    public Mono<EhrConnectionStatus> testConnection() {
        return authenticate()
                .map(token -> {
                    connectionStatus.setStatus(EhrConnectionStatus.Status.CONNECTED);
                    connectionStatus.setLastSuccessfulConnection(LocalDateTime.now());
                    connectionStatus.setConsecutiveFailures(0);
                    return connectionStatus;
                })
                .onErrorResume(error -> {
                    connectionStatus.setStatus(EhrConnectionStatus.Status.ERROR);
                    connectionStatus.setErrorMessage(error.getMessage());
                    connectionStatus.setConsecutiveFailures(
                            connectionStatus.getConsecutiveFailures() + 1);
                    return Mono.just(connectionStatus);
                });
    }

    @Override
    public Mono<Void> disconnect() {
        log.info("Disconnecting EHR connector for tenant: {}", config.getTenantId());
        accessToken = null;
        tokenExpiryTime = null;
        connectionStatus.setStatus(EhrConnectionStatus.Status.DISCONNECTED);
        return Mono.empty();
    }

    @Override
    public Mono<EhrConnectionStatus> getConnectionStatus() {
        if (circuitBreaker != null) {
            CircuitBreaker.State state = circuitBreaker.getState();
            connectionStatus.setCircuitBreakerState(mapCircuitBreakerState(state));
        }
        return Mono.just(connectionStatus);
    }

    @Override
    public EhrConnectionConfig getConfig() {
        return config;
    }

    /**
     * Apply resilience patterns (circuit breaker and retry) to a Mono.
     */
    protected <T> Mono<T> applyResilience(Mono<T> mono) {
        Mono<T> result = mono;

        // Apply retry
        if (retry != null) {
            result = result.transformDeferred(RetryOperator.of(retry));
        }

        // Apply circuit breaker
        if (circuitBreaker != null) {
            result = result.transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
        }

        return result;
    }

    /**
     * Check if access token is valid and not expired.
     */
    protected boolean isTokenValid() {
        return accessToken != null &&
                tokenExpiryTime != null &&
                LocalDateTime.now().isBefore(tokenExpiryTime);
    }

    /**
     * Get valid access token, refreshing if necessary.
     */
    protected Mono<String> getValidAccessToken() {
        if (isTokenValid()) {
            return Mono.just(accessToken);
        }
        return authenticate();
    }

    /**
     * Map Resilience4j circuit breaker state to our model.
     */
    private EhrConnectionStatus.CircuitBreakerState mapCircuitBreakerState(CircuitBreaker.State state) {
        return switch (state) {
            case CLOSED -> EhrConnectionStatus.CircuitBreakerState.CLOSED;
            case OPEN -> EhrConnectionStatus.CircuitBreakerState.OPEN;
            case HALF_OPEN -> EhrConnectionStatus.CircuitBreakerState.HALF_OPEN;
            default -> EhrConnectionStatus.CircuitBreakerState.CLOSED;
        };
    }

    /**
     * Authenticate with the EHR system and obtain an access token.
     * Implementation is vendor-specific.
     *
     * @return Mono containing the access token
     */
    protected abstract Mono<String> authenticate();
}

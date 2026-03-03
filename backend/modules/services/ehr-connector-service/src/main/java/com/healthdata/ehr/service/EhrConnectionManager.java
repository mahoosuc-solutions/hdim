package com.healthdata.ehr.service;

import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.factory.EhrConnectorFactory;
import com.healthdata.ehr.model.EhrConnectionStatus;
import com.healthdata.ehr.persistence.EhrConnectionConfigEntity;
import com.healthdata.ehr.persistence.EhrConnectionConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages active EHR connections for all tenants.
 * Provides connection pooling, lifecycle management, and health monitoring.
 *
 * <p>Connection configurations are persisted to the database so they survive
 * service restarts. On startup, all active configs are loaded and used to
 * reconstruct live connectors.</p>
 */
@Slf4j
@Service
public class EhrConnectionManager {

    private final EhrConnectorFactory connectorFactory;
    private final EhrConnectionConfigRepository configRepository;

    // Map of connectionId -> connector (runtime instances, non-serializable)
    private final Map<String, EhrConnector> activeConnections = new ConcurrentHashMap<>();

    // Map of connectionId -> tenantId for quick tenant lookup
    private final Map<String, String> connectionTenantMap = new ConcurrentHashMap<>();

    public EhrConnectionManager(EhrConnectorFactory connectorFactory,
                                 EhrConnectionConfigRepository configRepository) {
        this.connectorFactory = connectorFactory;
        this.configRepository = configRepository;
    }

    /**
     * On startup, restore all active connections from the database.
     * Connectors are non-serializable, so we reconstruct them from persisted configs.
     */
    @PostConstruct
    void initializeFromDatabase() {
        try {
            List<EhrConnectionConfigEntity> activeConfigs = configRepository.findByActiveTrue();
            log.info("Restoring {} EHR connections from database", activeConfigs.size());
            restoreConnections(activeConfigs);
            log.info("EHR connection initialization complete: {} active connections", activeConnections.size());
        } catch (Exception e) {
            log.error("Failed to restore EHR connections from database — will retry in 5 minutes: {}", e.getMessage());
        }
    }

    /**
     * Periodically retry restoring any active connections that aren't yet loaded.
     * Handles the case where the database was unreachable at startup.
     */
    @Scheduled(fixedDelay = 300_000, initialDelay = 300_000)
    void retryFailedConnections() {
        try {
            List<EhrConnectionConfigEntity> activeConfigs = configRepository.findByActiveTrue();
            List<EhrConnectionConfigEntity> missing = activeConfigs.stream()
                    .filter(entity -> !activeConnections.containsKey(entity.getConnectionId()))
                    .toList();

            if (!missing.isEmpty()) {
                log.info("Retrying {} unrestored EHR connections", missing.size());
                restoreConnections(missing);
            }
        } catch (Exception e) {
            log.warn("Scheduled connection retry failed: {}", e.getMessage());
        }
    }

    private void restoreConnections(List<EhrConnectionConfigEntity> configs) {
        for (EhrConnectionConfigEntity entity : configs) {
            try {
                EhrConnectionConfig config = entity.toConfig();
                EhrConnector connector = connectorFactory.createConnector(config);
                connector.initialize(config).block();

                activeConnections.put(config.getConnectionId(), connector);
                connectionTenantMap.put(config.getConnectionId(), config.getTenantId());

                log.info("Restored connection: {} for tenant: {}", config.getConnectionId(), config.getTenantId());
            } catch (Exception e) {
                log.warn("Failed to restore connection: {} — will retry on next access",
                        entity.getConnectionId(), e);
            }
        }
    }

    /**
     * Register a new EHR connection.
     * Persists the config to DB, then creates the live connector.
     *
     * @param config Connection configuration
     * @return Mono containing the connection ID
     */
    public Mono<String> registerConnection(EhrConnectionConfig config) {
        String connectionId = config.getConnectionId();
        String tenantId = config.getTenantId();

        if (activeConnections.containsKey(connectionId)) {
            return Mono.error(new IllegalArgumentException(
                    "Connection with ID " + connectionId + " already exists"));
        }

        log.info("Registering new EHR connection: {} for tenant: {}", connectionId, tenantId);

        // Persist config to database first
        EhrConnectionConfigEntity entity = EhrConnectionConfigEntity.fromConfig(config);
        configRepository.save(entity);

        return Mono.fromCallable(() -> connectorFactory.createConnector(config))
                .flatMap(connector -> connector.initialize(config)
                        .thenReturn(connector))
                .doOnSuccess(connector -> {
                    activeConnections.put(connectionId, connector);
                    connectionTenantMap.put(connectionId, tenantId);
                    log.info("Successfully registered connection: {} for tenant: {}", connectionId, tenantId);
                })
                .thenReturn(connectionId)
                .doOnError(error -> log.error("Failed to register connection: {}", connectionId, error));
    }

    /**
     * Get an active connection by ID and tenant.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @return EhrConnector instance or null if not found
     */
    public EhrConnector getConnection(String connectionId, String tenantId) {
        // Validate tenant ownership
        String registeredTenantId = connectionTenantMap.get(connectionId);
        if (registeredTenantId == null || !registeredTenantId.equals(tenantId)) {
            log.warn("Connection {} not found or not owned by tenant {}", connectionId, tenantId);
            return null;
        }

        return activeConnections.get(connectionId);
    }

    /**
     * Get connection status.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @return Mono containing connection status
     */
    public Mono<EhrConnectionStatus> getConnectionStatus(String connectionId, String tenantId) {
        EhrConnector connector = getConnection(connectionId, tenantId);
        if (connector == null) {
            return Mono.error(new IllegalArgumentException(
                    "Connection not found: " + connectionId));
        }

        return connector.getConnectionStatus();
    }

    /**
     * Test a connection's health.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @return Mono containing test results
     */
    public Mono<EhrConnectionStatus> testConnection(String connectionId, String tenantId) {
        EhrConnector connector = getConnection(connectionId, tenantId);
        if (connector == null) {
            return Mono.error(new IllegalArgumentException(
                    "Connection not found: " + connectionId));
        }

        log.info("Testing connection: {} for tenant: {}", connectionId, tenantId);
        return connector.testConnection();
    }

    /**
     * Remove a connection.
     *
     * @param connectionId Connection identifier
     * @param tenantId Tenant identifier
     * @return Mono that completes when connection is removed
     */
    public Mono<Void> removeConnection(String connectionId, String tenantId) {
        EhrConnector connector = getConnection(connectionId, tenantId);
        if (connector == null) {
            return Mono.error(new IllegalArgumentException(
                    "Connection not found: " + connectionId));
        }

        log.info("Removing connection: {} for tenant: {}", connectionId, tenantId);

        // Soft-delete in database
        configRepository.findByConnectionIdAndTenantId(connectionId, tenantId)
                .ifPresent(entity -> {
                    entity.setActive(false);
                    configRepository.save(entity);
                });

        return connector.disconnect()
                .doFinally(signal -> {
                    activeConnections.remove(connectionId);
                    connectionTenantMap.remove(connectionId);
                    log.info("Successfully removed connection: {}", connectionId);
                });
    }

    /**
     * Get all connection IDs for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return List of connection IDs
     */
    public List<String> getConnectionsByTenant(String tenantId) {
        return connectionTenantMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(tenantId))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get count of active connections.
     *
     * @return Number of active connections
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }

    /**
     * Remove all connections for a tenant.
     *
     * @param tenantId Tenant identifier
     * @return Mono that completes when all connections are removed
     */
    public Mono<Void> removeAllConnectionsForTenant(String tenantId) {
        List<String> connectionIds = getConnectionsByTenant(tenantId);
        log.info("Removing {} connections for tenant: {}", connectionIds.size(), tenantId);

        return Mono.when(
                connectionIds.stream()
                        .map(id -> removeConnection(id, tenantId))
                        .collect(Collectors.toList())
        );
    }
}

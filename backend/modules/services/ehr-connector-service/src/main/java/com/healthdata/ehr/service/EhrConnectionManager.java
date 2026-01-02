package com.healthdata.ehr.service;

import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.factory.EhrConnectorFactory;
import com.healthdata.ehr.model.EhrConnectionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages active EHR connections for all tenants.
 * Provides connection pooling, lifecycle management, and health monitoring.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EhrConnectionManager {

    private final EhrConnectorFactory connectorFactory;

    // Map of connectionId -> connector
    private final Map<String, EhrConnector> activeConnections = new ConcurrentHashMap<>();

    // Map of connectionId -> tenantId for quick tenant lookup
    private final Map<String, String> connectionTenantMap = new ConcurrentHashMap<>();

    /**
     * Register a new EHR connection.
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

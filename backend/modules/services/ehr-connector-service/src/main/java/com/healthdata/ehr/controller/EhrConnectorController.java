package com.healthdata.ehr.controller;

import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.dto.*;
import com.healthdata.ehr.model.EhrConnectionStatus;
import com.healthdata.ehr.service.EhrConnectionManager;
import com.healthdata.ehr.service.EhrSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for EHR connector operations.
 * Provides endpoints for managing EHR connections and triggering data synchronization.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ehr/connections")
@RequiredArgsConstructor
@Tag(name = "EHR Connector", description = "EHR connection management and data synchronization API")
@SecurityRequirement(name = "bearer-jwt")
public class EhrConnectorController {

    private final EhrConnectionManager connectionManager;
    private final EhrSyncService syncService;

    @PostMapping
    @Operation(summary = "Register new EHR connection",
            description = "Creates and initializes a new connection to an EHR system")
    public Mono<ResponseEntity<ConnectionResponse>> registerConnection(
            @Valid @RequestBody ConnectionRequest request,
            Authentication authentication) {

        String tenantId = getTenantId(authentication);
        log.info("Registering new EHR connection {} for tenant {}", request.getConnectionId(), tenantId);

        EhrConnectionConfig config = mapToConfig(request, tenantId);

        return connectionManager.registerConnection(config)
                .flatMap(connectionId -> connectionManager.getConnectionStatus(connectionId, tenantId))
                .map(status -> mapToConnectionResponse(status, config))
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(error -> {
                    log.error("Failed to register connection", error);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    @GetMapping
    @Operation(summary = "List all connections",
            description = "Retrieves all active EHR connections for the tenant")
    public ResponseEntity<List<String>> getConnections(Authentication authentication) {
        String tenantId = getTenantId(authentication);
        List<String> connectionIds = connectionManager.getConnectionsByTenant(tenantId);
        return ResponseEntity.ok(connectionIds);
    }

    @GetMapping("/{connectionId}/status")
    @Operation(summary = "Get connection status",
            description = "Retrieves the current status and health of an EHR connection")
    public Mono<ResponseEntity<EhrConnectionStatus>> getConnectionStatus(
            @PathVariable String connectionId,
            Authentication authentication) {

        String tenantId = getTenantId(authentication);

        return connectionManager.getConnectionStatus(connectionId, tenantId)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Failed to get connection status", error);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    @PostMapping("/{connectionId}/test")
    @Operation(summary = "Test connection",
            description = "Tests the connection to the EHR system and returns status")
    public Mono<ResponseEntity<EhrConnectionStatus>> testConnection(
            @PathVariable String connectionId,
            Authentication authentication) {

        String tenantId = getTenantId(authentication);
        log.info("Testing connection {} for tenant {}", connectionId, tenantId);

        return connectionManager.testConnection(connectionId, tenantId)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Connection test failed", error);
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
                });
    }

    @PostMapping("/{connectionId}/sync")
    @Operation(summary = "Trigger data synchronization",
            description = "Initiates data sync for a patient from the EHR system")
    public Mono<ResponseEntity<SyncResponse>> syncPatientData(
            @PathVariable String connectionId,
            @Valid @RequestBody SyncRequest request,
            Authentication authentication) {

        String tenantId = getTenantId(authentication);
        log.info("Starting data sync for patient {} via connection {}",
                request.getEhrPatientId(), connectionId);

        return syncService.syncPatientData(
                        connectionId,
                        tenantId,
                        request.getEhrPatientId(),
                        request.getStartDate(),
                        request.getEndDate()
                )
                .map(this::mapToSyncResponse)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    log.error("Data sync failed", error);
                    SyncResponse errorResponse = SyncResponse.builder()
                            .patientId(request.getEhrPatientId())
                            .success(false)
                            .errorMessage(error.getMessage())
                            .build();
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(errorResponse));
                });
    }

    @DeleteMapping("/{connectionId}")
    @Operation(summary = "Remove connection",
            description = "Disconnects and removes an EHR connection")
    public Mono<ResponseEntity<Void>> removeConnection(
            @PathVariable String connectionId,
            Authentication authentication) {

        String tenantId = getTenantId(authentication);
        log.info("Removing connection {} for tenant {}", connectionId, tenantId);

        return connectionManager.removeConnection(connectionId, tenantId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(error -> {
                    log.error("Failed to remove connection", error);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    /**
     * Extract tenant ID from authentication context.
     */
    private String getTenantId(Authentication authentication) {
        // TODO: Extract from JWT claims or user details
        // For now, return a default tenant
        return "default-tenant";
    }

    /**
     * Map ConnectionRequest to EhrConnectionConfig.
     */
    private EhrConnectionConfig mapToConfig(ConnectionRequest request, String tenantId) {
        return EhrConnectionConfig.builder()
                .connectionId(request.getConnectionId())
                .tenantId(tenantId)
                .vendorType(request.getVendorType())
                .baseUrl(request.getBaseUrl())
                .clientId(request.getClientId())
                .clientSecret(request.getClientSecret())
                .tokenUrl(request.getTokenUrl())
                .scope(request.getScope())
                .timeoutMs(request.getTimeoutMs())
                .maxRetries(request.getMaxRetries())
                .enableCircuitBreaker(request.getEnableCircuitBreaker())
                .additionalProperties(request.getAdditionalProperties())
                .build();
    }

    /**
     * Map EhrConnectionStatus to ConnectionResponse.
     */
    private ConnectionResponse mapToConnectionResponse(EhrConnectionStatus status,
                                                         EhrConnectionConfig config) {
        return ConnectionResponse.builder()
                .connectionId(status.getConnectionId())
                .tenantId(status.getTenantId())
                .vendorType(status.getVendorType())
                .baseUrl(config.getBaseUrl())
                .status(status.getStatus())
                .lastSuccessfulConnection(status.getLastSuccessfulConnection())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Map SyncResult to SyncResponse.
     */
    private SyncResponse mapToSyncResponse(EhrConnector.SyncResult result) {
        return SyncResponse.builder()
                .patientId(result.patientId())
                .encountersRetrieved(result.encountersRetrieved())
                .observationsRetrieved(result.observationsRetrieved())
                .syncStartTime(result.syncStartTime())
                .syncEndTime(result.syncEndTime())
                .success(result.success())
                .errorMessage(result.errorMessage())
                .build();
    }
}

package com.healthdata.gateway.clinical.compliance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.gateway.clinical.compliance.dto.ComplianceErrorDto;
import com.healthdata.gateway.clinical.compliance.dto.ErrorSyncRequest;
import com.healthdata.gateway.clinical.compliance.entity.ComplianceErrorEntity;
import com.healthdata.gateway.clinical.compliance.repository.ComplianceErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceErrorService {
    private final ComplianceErrorRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int syncErrors(ErrorSyncRequest request, String tenantId) {
        if (request.getErrors() == null || request.getErrors().isEmpty()) {
            return 0;
        }

        int synced = 0;
        for (ComplianceErrorDto errorDto : request.getErrors()) {
            try {
                // Generate UUID from error ID (deterministic based on timestamp and random part)
                UUID errorId = parseErrorId(errorDto.getId());
                
                // Check if error already exists (deduplication)
                if (repository.existsById(errorId)) {
                    log.debug("Error {} already exists, skipping", errorId);
                    continue;
                }

                ComplianceErrorEntity entity = mapToEntity(errorDto, tenantId);
                // Ensure ID is set
                if (entity.getId() == null) {
                    entity.setId(errorId);
                }
                repository.save(entity);
                synced++;
            } catch (Exception e) {
                log.warn("Failed to sync error {}: {}", errorDto.getId(), e.getMessage(), e);
            }
        }

        log.info("Synced {} errors for tenant {}", synced, tenantId);
        return synced;
    }

    private ComplianceErrorEntity mapToEntity(ComplianceErrorDto dto, String tenantId) {
        try {
            ComplianceErrorEntity.ComplianceErrorEntityBuilder builder = ComplianceErrorEntity.builder()
                .id(parseErrorId(dto.getId()))
                .timestamp(Instant.parse(dto.getTimestamp()))
                .tenantId(dto.getContext() != null && dto.getContext().getTenantId() != null 
                    ? dto.getContext().getTenantId() : tenantId)
                .userId(dto.getContext() != null ? dto.getContext().getUserId() : null)
                .service(dto.getContext() != null ? dto.getContext().getService() : "Unknown")
                .endpoint(dto.getContext() != null ? dto.getContext().getEndpoint() : null)
                .operation(dto.getContext() != null ? dto.getContext().getOperation() : "Unknown")
                .errorCode(dto.getContext() != null ? dto.getContext().getErrorCode() : "ERR-9001")
                .severity(dto.getContext() != null ? dto.getContext().getSeverity() : "ERROR")
                .message(dto.getMessage())
                .stack(dto.getStack())
                .createdAt(Instant.now());

            if (dto.getContext() != null && dto.getContext().getAdditionalData() != null) {
                try {
                    builder.additionalData(objectMapper.writeValueAsString(dto.getContext().getAdditionalData()));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize additional data: {}", e.getMessage());
                }
            }

            return builder.build();
        } catch (Exception e) {
            log.error("Failed to map error DTO to entity: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to map error", e);
        }
    }

    private UUID parseErrorId(String errorId) {
        try {
            // Frontend format: err-1234567890-abc123def
            // Convert to UUID format: use hash of the full ID for deterministic UUID
            String cleaned = errorId.replace("err-", "");
            // Use MD5 hash of the error ID to generate deterministic UUID
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(cleaned.getBytes());
            // Convert to UUID format (version 3 style)
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }
            msb = (msb & 0xFFFFFFFFFFFF0FFFL) | 0x3000L; // Version 3
            lsb = (lsb & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L; // Variant
            return new UUID(msb, lsb);
        } catch (Exception e) {
            log.warn("Failed to parse error ID {}, generating random UUID: {}", errorId, e.getMessage());
            return UUID.randomUUID();
        }
    }

    public Page<ComplianceErrorEntity> getErrors(String tenantId, Pageable pageable) {
        return repository.findByTenantIdOrderByTimestampDesc(tenantId, pageable);
    }

    public Page<ComplianceErrorEntity> getErrorsBySeverity(String tenantId, String severity, Pageable pageable) {
        return repository.findByTenantIdAndSeverityOrderByTimestampDesc(tenantId, severity, pageable);
    }

    public Page<ComplianceErrorEntity> getErrorsByService(String tenantId, String service, Pageable pageable) {
        return repository.findByTenantIdAndServiceOrderByTimestampDesc(tenantId, service, pageable);
    }

    public long getErrorCountBySeverity(String tenantId, String severity) {
        return repository.countByTenantIdAndSeverity(tenantId, severity);
    }

    public long getErrorCountInRange(String tenantId, Instant startDate, Instant endDate) {
        return repository.countByTenantIdAndDateRange(tenantId, startDate, endDate);
    }

    @Transactional
    public int cleanupOldErrors(String tenantId, int retentionDays) {
        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        repository.deleteByTenantIdAndTimestampBefore(tenantId, cutoffDate);
        log.info("Cleaned up old errors for tenant {} (older than {} days)", tenantId, retentionDays);
        return 0;
    }
}

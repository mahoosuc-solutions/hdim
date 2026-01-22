package com.healthdata.audit.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.audit.entity.shared.AuditEventEntity;
import com.healthdata.audit.models.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper to convert between AuditEvent (domain model) and AuditEventEntity (JPA entity).
 */
public class AuditEventMapper {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventMapper.class);
    private final ObjectMapper objectMapper;

    public AuditEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convert AuditEvent to AuditEventEntity for persistence.
     */
    public AuditEventEntity toEntity(AuditEvent event) {
        AuditEventEntity entity = new AuditEventEntity();

        entity.setId(event.getId());
        entity.setTimestamp(event.getTimestamp());
        entity.setTenantId(event.getTenantId());
        entity.setUserId(event.getUserId());
        entity.setUsername(event.getUsername());
        entity.setRole(event.getRole());
        entity.setIpAddress(event.getIpAddress());
        entity.setUserAgent(event.getUserAgent());
        entity.setAction(event.getAction());
        entity.setResourceType(event.getResourceType());
        entity.setResourceId(event.getResourceId());
        entity.setOutcome(event.getOutcome());
        entity.setServiceName(event.getServiceName());
        entity.setMethodName(event.getMethodName());
        entity.setRequestPath(event.getRequestPath());
        entity.setPurposeOfUse(event.getPurposeOfUse());
        entity.setErrorMessage(event.getErrorMessage());
        entity.setDurationMs(event.getDurationMs());
        entity.setFhirAuditEventId(event.getFhirAuditEventId());
        entity.setEncrypted(event.isEncrypted());

        // Convert JsonNode to String for JSONB storage
        if (event.getRequestPayload() != null) {
            entity.setRequestPayload(event.getRequestPayload().toString());
        }
        if (event.getResponsePayload() != null) {
            entity.setResponsePayload(event.getResponsePayload().toString());
        }

        return entity;
    }

    /**
     * Convert AuditEventEntity to AuditEvent for business logic.
     */
    public AuditEvent toModel(AuditEventEntity entity) {
        AuditEvent event = new AuditEvent();

        event.setId(entity.getId());
        event.setTimestamp(entity.getTimestamp());
        event.setTenantId(entity.getTenantId());
        event.setUserId(entity.getUserId());
        event.setUsername(entity.getUsername());
        event.setRole(entity.getRole());
        event.setIpAddress(entity.getIpAddress());
        event.setUserAgent(entity.getUserAgent());
        event.setAction(entity.getAction());
        event.setResourceType(entity.getResourceType());
        event.setResourceId(entity.getResourceId());
        event.setOutcome(entity.getOutcome());
        event.setServiceName(entity.getServiceName());
        event.setMethodName(entity.getMethodName());
        event.setRequestPath(entity.getRequestPath());
        event.setPurposeOfUse(entity.getPurposeOfUse());
        event.setErrorMessage(entity.getErrorMessage());
        event.setDurationMs(entity.getDurationMs());
        event.setFhirAuditEventId(entity.getFhirAuditEventId());
        event.setEncrypted(entity.isEncrypted());

        // Convert String back to JsonNode
        if (entity.getRequestPayload() != null) {
            try {
                event.setRequestPayload(objectMapper.readTree(entity.getRequestPayload()));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse request payload as JSON", e);
            }
        }
        if (entity.getResponsePayload() != null) {
            try {
                event.setResponsePayload(objectMapper.readTree(entity.getResponsePayload()));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse response payload as JSON", e);
            }
        }

        return event;
    }
}

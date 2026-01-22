package com.healthdata.auditquery.service;

import com.healthdata.audit.entity.shared.AuditEventEntity;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.repository.shared.AuditEventRepository;
import com.healthdata.auditquery.dto.AuditEventResponse;
import com.healthdata.auditquery.dto.AuditSearchRequest;
import com.healthdata.auditquery.dto.AuditStatisticsResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for querying and analyzing audit events.
 *
 * <p>Provides multi-criteria search, statistics generation, and compliance reporting.
 * All queries enforce multi-tenant isolation via tenantId filtering.
 *
 * <p>Security:
 * <ul>
 *   <li>All methods require AUDITOR or ADMIN role (enforced at controller level)</li>
 *   <li>Payload decryption requires explicit permission (not implemented in this version)</li>
 *   <li>All queries filter by tenantId to prevent cross-tenant data leakage</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;
    private final EntityManager entityManager;

    /**
     * Search audit events with multi-criteria filtering.
     *
     * @param tenantId tenant ID for isolation
     * @param request search criteria
     * @return page of matching audit events
     */
    public Page<AuditEventResponse> searchAuditEvents(String tenantId, AuditSearchRequest request) {
        log.info("Searching audit events for tenant {} with filters: {}", tenantId, request);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEventEntity> query = cb.createQuery(AuditEventEntity.class);
        Root<AuditEventEntity> root = query.from(AuditEventEntity.class);

        // Build predicates
        List<Predicate> predicates = buildPredicates(cb, root, tenantId, request);
        query.where(predicates.toArray(new Predicate[0]));

        // Apply sorting
        if ("DESC".equalsIgnoreCase(request.sortDirection())) {
            query.orderBy(cb.desc(root.get(request.sortBy())));
        } else {
            query.orderBy(cb.asc(root.get(request.sortBy())));
        }

        // Execute query with pagination
        List<AuditEventEntity> results = entityManager.createQuery(query)
            .setFirstResult(request.page() * request.size())
            .setMaxResults(request.size())
            .getResultList();

        // Count total matching records
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AuditEventEntity> countRoot = countQuery.from(AuditEventEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, tenantId, request).toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        // Map to DTOs
        List<AuditEventResponse> dtos = results.stream()
            .map(this::mapToResponse)
            .toList();

        return new PageImpl<>(dtos, PageRequest.of(request.page(), request.size()), total);
    }

    /**
     * Get a specific audit event by ID.
     *
     * @param tenantId tenant ID for isolation
     * @param eventId audit event ID
     * @return audit event details
     */
    public Optional<AuditEventResponse> getAuditEvent(String tenantId, UUID eventId) {
        log.info("Fetching audit event {} for tenant {}", eventId, tenantId);

        return auditEventRepository.findById(eventId)
            .filter(event -> tenantId.equals(event.getTenantId()))
            .map(this::mapToResponse);
    }

    /**
     * Generate statistics for audit events in a time range.
     *
     * @param tenantId tenant ID for isolation
     * @param startTime start of time range
     * @param endTime end of time range
     * @return aggregated statistics
     */
    public AuditStatisticsResponse getStatistics(String tenantId, Instant startTime, Instant endTime) {
        log.info("Generating audit statistics for tenant {} from {} to {}", tenantId, startTime, endTime);

        // Default to last 30 days if no time range specified
        if (startTime == null) {
            startTime = Instant.now().minus(30, ChronoUnit.DAYS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        // Fetch all events in time range (consider adding pagination for very large datasets)
        Page<AuditEventEntity> events = auditEventRepository.findByTenantIdAndTimestampBetween(
            tenantId, startTime, endTime, PageRequest.of(0, 100000)
        );

        return calculateStatistics(events.getContent(), startTime, endTime);
    }

    /**
     * Build JPA criteria predicates from search request.
     */
    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        Root<AuditEventEntity> root,
        String tenantId,
        AuditSearchRequest request
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // Tenant isolation (REQUIRED)
        predicates.add(cb.equal(root.get("tenantId"), tenantId));

        // User filters
        if (request.userId() != null) {
            predicates.add(cb.equal(root.get("userId"), request.userId()));
        }
        if (request.username() != null) {
            predicates.add(cb.like(cb.lower(root.get("username")),
                "%" + request.username().toLowerCase() + "%"));
        }
        if (request.role() != null) {
            predicates.add(cb.equal(root.get("role"), request.role()));
        }

        // Resource filters
        if (request.resourceType() != null) {
            predicates.add(cb.equal(root.get("resourceType"), request.resourceType()));
        }
        if (request.resourceId() != null) {
            predicates.add(cb.equal(root.get("resourceId"), request.resourceId()));
        }

        // Action filters
        if (request.action() != null) {
            predicates.add(cb.equal(root.get("action"), request.action()));
        }
        if (request.outcome() != null) {
            predicates.add(cb.equal(root.get("outcome"), request.outcome()));
        }

        // Service filter
        if (request.serviceName() != null) {
            predicates.add(cb.equal(root.get("serviceName"), request.serviceName()));
        }

        // IP address filter
        if (request.ipAddress() != null) {
            predicates.add(cb.equal(root.get("ipAddress"), request.ipAddress()));
        }

        // Time range filters
        if (request.startTime() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), request.startTime()));
        }
        if (request.endTime() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), request.endTime()));
        }

        return predicates;
    }

    /**
     * Map entity to response DTO.
     */
    private AuditEventResponse mapToResponse(AuditEventEntity entity) {
        return new AuditEventResponse(
            entity.getId(),
            entity.getTimestamp(),
            entity.getTenantId(),
            entity.getUserId(),
            entity.getUsername(),
            entity.getRole(),
            entity.getIpAddress(),
            entity.getUserAgent(),
            entity.getAction(),
            entity.getResourceType(),
            entity.getResourceId(),
            entity.getOutcome(),
            entity.getServiceName(),
            entity.getMethodName(),
            entity.getRequestPath(),
            entity.getPurposeOfUse(),
            entity.isEncrypted() ? "{encrypted}" : entity.getRequestPayload(),
            entity.isEncrypted() ? "{encrypted}" : entity.getResponsePayload(),
            entity.getErrorMessage(),
            entity.getDurationMs(),
            entity.getFhirAuditEventId(),
            entity.isEncrypted()
        );
    }

    /**
     * Calculate statistics from a list of audit events.
     */
    private AuditStatisticsResponse calculateStatistics(
        List<AuditEventEntity> events,
        Instant startTime,
        Instant endTime
    ) {
        // Events by action
        Map<String, Long> eventsByAction = events.stream()
            .collect(Collectors.groupingBy(
                e -> e.getAction() != null ? e.getAction().name() : "UNKNOWN",
                Collectors.counting()
            ));

        // Events by outcome
        Map<String, Long> eventsByOutcome = events.stream()
            .collect(Collectors.groupingBy(
                e -> e.getOutcome() != null ? e.getOutcome().name() : "UNKNOWN",
                Collectors.counting()
            ));

        // Events by resource type
        Map<String, Long> eventsByResourceType = events.stream()
            .filter(e -> e.getResourceType() != null)
            .collect(Collectors.groupingBy(
                AuditEventEntity::getResourceType,
                Collectors.counting()
            ));

        // Events by service
        Map<String, Long> eventsByService = events.stream()
            .filter(e -> e.getServiceName() != null)
            .collect(Collectors.groupingBy(
                AuditEventEntity::getServiceName,
                Collectors.counting()
            ));

        // Top users (sorted by count, top 10)
        Map<String, Long> topUsers = events.stream()
            .filter(e -> e.getUserId() != null)
            .collect(Collectors.groupingBy(
                AuditEventEntity::getUserId,
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        // Top resources (sorted by count, top 10)
        Map<String, Long> topResources = events.stream()
            .filter(e -> e.getResourceId() != null)
            .collect(Collectors.groupingBy(
                e -> e.getResourceType() + "/" + e.getResourceId(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        // Failed events count (any non-SUCCESS outcome)
        long failedEvents = events.stream()
            .filter(e -> e.getOutcome() != null && !AuditOutcome.SUCCESS.equals(e.getOutcome()))
            .count();

        // PHI access events (Patient, Observation, Condition, etc.)
        Set<String> phiResourceTypes = Set.of("Patient", "Observation", "Condition",
            "MedicationRequest", "Procedure", "DiagnosticReport", "Immunization");
        long phiAccessEvents = events.stream()
            .filter(e -> phiResourceTypes.contains(e.getResourceType()))
            .count();

        // Unique users
        long uniqueUsers = events.stream()
            .map(AuditEventEntity::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .count();

        // Unique resources
        long uniqueResources = events.stream()
            .filter(e -> e.getResourceType() != null && e.getResourceId() != null)
            .map(e -> e.getResourceType() + "/" + e.getResourceId())
            .distinct()
            .count();

        return new AuditStatisticsResponse(
            events.size(),
            startTime,
            endTime,
            eventsByAction,
            eventsByOutcome,
            eventsByResourceType,
            eventsByService,
            topUsers,
            topResources,
            failedEvents,
            phiAccessEvents,
            uniqueUsers,
            uniqueResources
        );
    }
}

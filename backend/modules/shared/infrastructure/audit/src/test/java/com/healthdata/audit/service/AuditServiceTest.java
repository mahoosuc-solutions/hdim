package com.healthdata.audit.service;

import com.healthdata.audit.entity.AuditEventEntity;
import com.healthdata.audit.mapper.AuditEventMapper;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AuditService using TDD approach.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private AuditService auditService;
    private AuditEncryptionService encryptionService;
    private ObjectMapper objectMapper;
    private AuditEventMapper mapper;

    @Mock
    private AuditEventRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        encryptionService = new AuditEncryptionService(null);
        mapper = new AuditEventMapper(objectMapper);
        auditService = new AuditService(objectMapper, encryptionService, repository, mapper);
    }

    @Test
    void testLogAuditEvent() {
        // Given
        AuditEvent event = AuditEvent.builder()
            .tenantId("tenant-1")
            .userId("user-123")
            .action(AuditAction.READ)
            .resourceType("Patient")
            .resourceId("patient-456")
            .outcome(AuditOutcome.SUCCESS)
            .build();

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> auditService.logAuditEvent(event));
    }

    @Test
    void testLogEvent() {
        // When/Then
        assertDoesNotThrow(() ->
            auditService.logEvent("tenant-1", "user-123", AuditAction.CREATE,
                "Patient", "patient-789", AuditOutcome.SUCCESS)
        );
    }

    @Test
    void testLogAccess() {
        // When/Then
        assertDoesNotThrow(() ->
            auditService.logAccess("tenant-1", "user-123", "Patient", "patient-789", true)
        );

        assertDoesNotThrow(() ->
            auditService.logAccess("tenant-1", "user-123", "Patient", "patient-789", false)
        );
    }

    @Test
    void testLogLogin() {
        // When/Then
        assertDoesNotThrow(() ->
            auditService.logLogin("john.doe", "192.168.1.1", true, null)
        );

        assertDoesNotThrow(() ->
            auditService.logLogin("jane.doe", "192.168.1.2", false, "Invalid password")
        );
    }

    @Test
    void testLogEmergencyAccess() {
        // When/Then
        assertDoesNotThrow(() ->
            auditService.logEmergencyAccess("tenant-1", "doctor-123", "Patient",
                "patient-999", "Life-threatening emergency")
        );
    }

    @Test
    void testAuditEventBuilder() {
        // Given/When
        AuditEvent event = AuditEvent.builder()
            .tenantId("tenant-1")
            .userId("user-123")
            .username("john.doe")
            .role("DOCTOR")
            .ipAddress("192.168.1.1")
            .userAgent("Mozilla/5.0")
            .action(AuditAction.UPDATE)
            .resourceType("Patient")
            .resourceId("patient-456")
            .outcome(AuditOutcome.SUCCESS)
            .serviceName("PatientService")
            .methodName("updatePatient")
            .requestPath("/api/patients/456")
            .purposeOfUse("TREATMENT")
            .durationMs(123L)
            .encrypted(true)
            .build();

        // Then
        assertNotNull(event.getId());
        assertNotNull(event.getTimestamp());
        assertEquals("tenant-1", event.getTenantId());
        assertEquals("user-123", event.getUserId());
        assertEquals("john.doe", event.getUsername());
        assertEquals("DOCTOR", event.getRole());
        assertEquals("192.168.1.1", event.getIpAddress());
        assertEquals("Mozilla/5.0", event.getUserAgent());
        assertEquals(AuditAction.UPDATE, event.getAction());
        assertEquals("Patient", event.getResourceType());
        assertEquals("patient-456", event.getResourceId());
        assertEquals(AuditOutcome.SUCCESS, event.getOutcome());
        assertEquals("PatientService", event.getServiceName());
        assertEquals("updatePatient", event.getMethodName());
        assertEquals("/api/patients/456", event.getRequestPath());
        assertEquals("TREATMENT", event.getPurposeOfUse());
        assertEquals(123L, event.getDurationMs());
        assertTrue(event.isEncrypted());
    }

    // TDD Tests for Database Persistence Methods

    @Test
    void testFindByUserId_ReturnsEvents() {
        // Given
        String userId = "user-123";
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Pageable pageable = PageRequest.of(0, 10);

        List<AuditEventEntity> entities = createTestEntities(userId, "Patient", "patient-1", 3);
        Page<AuditEventEntity> entityPage = new PageImpl<>(entities, pageable, entities.size());

        when(repository.findByUserIdAndTimestampBetween(eq(userId), any(Instant.class), any(Instant.class), eq(pageable)))
            .thenReturn(entityPage);

        // When
        Page<AuditEvent> result = auditService.findByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(userId, result.getContent().get(0).getUserId());
        verify(repository).findByUserIdAndTimestampBetween(eq(userId), any(Instant.class), any(Instant.class), eq(pageable));
    }

    @Test
    void testFindByUserId_WithDateRange_ReturnsEvents() {
        // Given
        String userId = "user-456";
        Instant from = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Pageable pageable = PageRequest.of(0, 20);

        List<AuditEventEntity> entities = createTestEntities(userId, "Observation", "obs-1", 5);
        Page<AuditEventEntity> entityPage = new PageImpl<>(entities, pageable, entities.size());

        when(repository.findByUserIdAndTimestampBetween(userId, from, to, pageable))
            .thenReturn(entityPage);

        // When
        Page<AuditEvent> result = auditService.findByUserId(userId, from, to, pageable);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        verify(repository).findByUserIdAndTimestampBetween(userId, from, to, pageable);
    }

    @Test
    void testFindByUserId_EmptyResult() {
        // Given
        String userId = "nonexistent-user";
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Pageable pageable = PageRequest.of(0, 10);

        when(repository.findByUserIdAndTimestampBetween(eq(userId), any(Instant.class), any(Instant.class), eq(pageable)))
            .thenReturn(Page.empty(pageable));

        // When
        Page<AuditEvent> result = auditService.findByUserId(userId, pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testFindByResource_ReturnsEvents() {
        // Given
        String resourceType = "Patient";
        String resourceId = "patient-789";
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Pageable pageable = PageRequest.of(0, 10);

        List<AuditEventEntity> entities = createTestEntities("user-1", resourceType, resourceId, 4);
        Page<AuditEventEntity> entityPage = new PageImpl<>(entities, pageable, entities.size());

        when(repository.findByResourceTypeAndResourceIdAndTimestampBetween(
            eq(resourceType), eq(resourceId), any(Instant.class), any(Instant.class), eq(pageable)))
            .thenReturn(entityPage);

        // When
        Page<AuditEvent> result = auditService.findByResource(resourceType, resourceId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(4, result.getContent().size());
        assertEquals(resourceType, result.getContent().get(0).getResourceType());
        assertEquals(resourceId, result.getContent().get(0).getResourceId());
        verify(repository).findByResourceTypeAndResourceIdAndTimestampBetween(
            eq(resourceType), eq(resourceId), any(Instant.class), any(Instant.class), eq(pageable));
    }

    @Test
    void testFindByResource_WithDateRange_ReturnsEvents() {
        // Given
        String resourceType = "Observation";
        String resourceId = "obs-456";
        Instant from = Instant.now().minus(14, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Pageable pageable = PageRequest.of(0, 15);

        List<AuditEventEntity> entities = createTestEntities("user-2", resourceType, resourceId, 7);
        Page<AuditEventEntity> entityPage = new PageImpl<>(entities, pageable, entities.size());

        when(repository.findByResourceTypeAndResourceIdAndTimestampBetween(
            resourceType, resourceId, from, to, pageable))
            .thenReturn(entityPage);

        // When
        Page<AuditEvent> result = auditService.findByResource(resourceType, resourceId, from, to, pageable);

        // Then
        assertNotNull(result);
        assertEquals(7, result.getContent().size());
        verify(repository).findByResourceTypeAndResourceIdAndTimestampBetween(
            resourceType, resourceId, from, to, pageable);
    }

    @Test
    void testFindByTenant_ReturnsEvents() {
        // Given
        String tenantId = "tenant-org-1";
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Pageable pageable = PageRequest.of(0, 50);

        List<AuditEventEntity> entities = createTestEntitiesForTenant(tenantId, 10);
        Page<AuditEventEntity> entityPage = new PageImpl<>(entities, pageable, entities.size());

        when(repository.findByTenantIdAndTimestampBetween(
            eq(tenantId), any(Instant.class), any(Instant.class), eq(pageable)))
            .thenReturn(entityPage);

        // When
        Page<AuditEvent> result = auditService.findByTenant(tenantId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(10, result.getContent().size());
        assertEquals(tenantId, result.getContent().get(0).getTenantId());
        verify(repository).findByTenantIdAndTimestampBetween(
            eq(tenantId), any(Instant.class), any(Instant.class), eq(pageable));
    }

    @Test
    void testFindByTenant_WithDateRange_ReturnsEvents() {
        // Given
        String tenantId = "tenant-org-2";
        Instant from = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant to = Instant.now();
        Pageable pageable = PageRequest.of(0, 100);

        List<AuditEventEntity> entities = createTestEntitiesForTenant(tenantId, 25);
        Page<AuditEventEntity> entityPage = new PageImpl<>(entities, pageable, entities.size());

        when(repository.findByTenantIdAndTimestampBetween(tenantId, from, to, pageable))
            .thenReturn(entityPage);

        // When
        Page<AuditEvent> result = auditService.findByTenant(tenantId, from, to, pageable);

        // Then
        assertNotNull(result);
        assertEquals(25, result.getContent().size());
        verify(repository).findByTenantIdAndTimestampBetween(tenantId, from, to, pageable);
    }

    @Test
    void testFindByTenant_EnforcesMultiTenantIsolation() {
        // Given - Two different tenants
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";
        Pageable pageable = PageRequest.of(0, 10);

        List<AuditEventEntity> tenant1Entities = createTestEntitiesForTenant(tenant1, 5);
        List<AuditEventEntity> tenant2Entities = createTestEntitiesForTenant(tenant2, 3);

        when(repository.findByTenantIdAndTimestampBetween(
            eq(tenant1), any(Instant.class), any(Instant.class), eq(pageable)))
            .thenReturn(new PageImpl<>(tenant1Entities, pageable, tenant1Entities.size()));

        when(repository.findByTenantIdAndTimestampBetween(
            eq(tenant2), any(Instant.class), any(Instant.class), eq(pageable)))
            .thenReturn(new PageImpl<>(tenant2Entities, pageable, tenant2Entities.size()));

        // When
        Page<AuditEvent> tenant1Results = auditService.findByTenant(tenant1, pageable);
        Page<AuditEvent> tenant2Results = auditService.findByTenant(tenant2, pageable);

        // Then - Each tenant only sees their own events
        assertEquals(5, tenant1Results.getContent().size());
        assertEquals(3, tenant2Results.getContent().size());
        assertTrue(tenant1Results.getContent().stream().allMatch(e -> tenant1.equals(e.getTenantId())));
        assertTrue(tenant2Results.getContent().stream().allMatch(e -> tenant2.equals(e.getTenantId())));
    }

    @Test
    void testPurgeOldAuditEvents_DefaultRetention() {
        // Given - Default 7 year retention
        int expectedDeletedCount = 150;
        when(repository.deleteByTimestampBefore(any(Instant.class)))
            .thenReturn(expectedDeletedCount);

        // When
        int deletedCount = auditService.purgeOldAuditEvents();

        // Then
        assertEquals(expectedDeletedCount, deletedCount);

        // Verify the cutoff date is approximately 7 years ago
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteByTimestampBefore(cutoffCaptor.capture());

        Instant cutoff = cutoffCaptor.getValue();
        Instant expected7YearsAgo = Instant.now().minus(7 * 365, ChronoUnit.DAYS);
        long diffDays = ChronoUnit.DAYS.between(cutoff, expected7YearsAgo);
        assertTrue(Math.abs(diffDays) < 2, "Cutoff should be approximately 7 years ago");
    }

    @Test
    void testPurgeOldAuditEvents_CustomRetention() {
        // Given - Custom 10 year retention for special compliance
        int retentionYears = 10;
        int expectedDeletedCount = 50;
        when(repository.deleteByTimestampBefore(any(Instant.class)))
            .thenReturn(expectedDeletedCount);

        // When
        int deletedCount = auditService.purgeOldAuditEvents(retentionYears);

        // Then
        assertEquals(expectedDeletedCount, deletedCount);

        // Verify the cutoff date is approximately 10 years ago
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteByTimestampBefore(cutoffCaptor.capture());

        Instant cutoff = cutoffCaptor.getValue();
        Instant expected10YearsAgo = Instant.now().minus(10 * 365, ChronoUnit.DAYS);
        long diffDays = ChronoUnit.DAYS.between(cutoff, expected10YearsAgo);
        assertTrue(Math.abs(diffDays) < 2, "Cutoff should be approximately 10 years ago");
    }

    @Test
    void testPurgeOldAuditEvents_NoOldEvents() {
        // Given - No old events to delete
        when(repository.deleteByTimestampBefore(any(Instant.class)))
            .thenReturn(0);

        // When
        int deletedCount = auditService.purgeOldAuditEvents();

        // Then
        assertEquals(0, deletedCount);
        verify(repository).deleteByTimestampBefore(any(Instant.class));
    }

    @Test
    void testLogAuditEvent_PersistsToDatabase() {
        // Given
        AuditEvent event = AuditEvent.builder()
            .tenantId("tenant-1")
            .userId("user-123")
            .action(AuditAction.CREATE)
            .resourceType("Patient")
            .resourceId("patient-new")
            .outcome(AuditOutcome.SUCCESS)
            .build();

        when(repository.save(any(AuditEventEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        auditService.logAuditEvent(event);

        // Then
        ArgumentCaptor<AuditEventEntity> entityCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(repository).save(entityCaptor.capture());

        AuditEventEntity savedEntity = entityCaptor.getValue();
        assertEquals(event.getTenantId(), savedEntity.getTenantId());
        assertEquals(event.getUserId(), savedEntity.getUserId());
        assertEquals(event.getAction(), savedEntity.getAction());
        assertEquals(event.getResourceType(), savedEntity.getResourceType());
        assertEquals(event.getResourceId(), savedEntity.getResourceId());
    }

    // Helper methods for creating test data

    private List<AuditEventEntity> createTestEntities(String userId, String resourceType, String resourceId, int count) {
        List<AuditEventEntity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AuditEventEntity entity = new AuditEventEntity();
            entity.setUserId(userId);
            entity.setResourceType(resourceType);
            entity.setResourceId(resourceId);
            entity.setTenantId("tenant-test");
            entity.setAction(AuditAction.READ);
            entity.setOutcome(AuditOutcome.SUCCESS);
            entity.setTimestamp(Instant.now().minus(i, ChronoUnit.HOURS));
            entities.add(entity);
        }
        return entities;
    }

    private List<AuditEventEntity> createTestEntitiesForTenant(String tenantId, int count) {
        List<AuditEventEntity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AuditEventEntity entity = new AuditEventEntity();
            entity.setTenantId(tenantId);
            entity.setUserId("user-" + i);
            entity.setResourceType("Patient");
            entity.setResourceId("patient-" + i);
            entity.setAction(AuditAction.READ);
            entity.setOutcome(AuditOutcome.SUCCESS);
            entity.setTimestamp(Instant.now().minus(i, ChronoUnit.HOURS));
            entities.add(entity);
        }
        return entities;
    }
}

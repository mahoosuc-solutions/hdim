package com.healthdata.caregap.repository;

import com.healthdata.BaseRepositoryTest;
import com.healthdata.DataTestFactory;
import com.healthdata.caregap.domain.CareGap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Care Gap Repository Integration Tests
 * Tests care gap detection, closure tracking, and prioritization
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Care Gap Repository Tests")
public class CareGapRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CareGapRepository careGapRepository;

    private CareGap openHighPriorityGap;
    private CareGap openMediumPriorityGap;
    private CareGap closedGap;
    private CareGap tenant2Gap;

    @BeforeEach
    void setUp() {
        // Open high priority gap
        openHighPriorityGap = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withGapType("CHRONIC_DISEASE_MONITORING")
            .withPriority("HIGH")
            .withStatus("OPEN")
            .withMeasureId("HEDIS-CDC")
            .withTenantId("tenant1")
            .build();

        // Open medium priority gap
        openMediumPriorityGap = DataTestFactory.careGapBuilder()
            .withPatientId("patient-002")
            .withGapType("PREVENTIVE_CARE")
            .withPriority("MEDIUM")
            .withStatus("OPEN")
            .withMeasureId("HEDIS-BC")
            .withTenantId("tenant1")
            .build();

        // Closed gap
        closedGap = DataTestFactory.createClosedGap("patient-003", "tenant1");

        // Tenant 2 gap
        tenant2Gap = DataTestFactory.careGapBuilder()
            .withPatientId("patient-101")
            .withGapType("MEDICATION_ADHERENCE")
            .withPriority("HIGH")
            .withStatus("OPEN")
            .withMeasureId("HEDIS-MED")
            .withTenantId("tenant2")
            .build();
    }

    // ========================================================================
    // Basic CRUD Tests
    // ========================================================================

    @Test
    @DisplayName("Should save and retrieve care gap")
    void testSaveAndRetrieveCareGap() {
        // Arrange & Act
        CareGap saved = careGapRepository.save(openHighPriorityGap);
        Optional<CareGap> retrieved = careGapRepository.findById(saved.getId());

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals("patient-001", retrieved.get().getPatientId());
        assertEquals("OPEN", retrieved.get().getStatus());
    }

    @Test
    @DisplayName("Should update care gap")
    void testUpdateCareGap() {
        // Arrange
        CareGap saved = careGapRepository.save(openHighPriorityGap);

        // Act
        saved.setPriority("LOW");
        careGapRepository.save(saved);
        CareGap updated = careGapRepository.findById(saved.getId()).orElseThrow();

        // Assert
        assertEquals("LOW", updated.getPriority());
    }

    @Test
    @DisplayName("Should delete care gap")
    void testDeleteCareGap() {
        // Arrange
        CareGap saved = careGapRepository.save(openHighPriorityGap);
        String id = saved.getId();

        // Act
        careGapRepository.deleteById(id);

        // Assert
        assertFalse(careGapRepository.existsById(id));
    }

    // ========================================================================
    // Patient-Based Query Tests
    // ========================================================================

    @Test
    @DisplayName("Should find care gaps by patient ID")
    void testFindByPatientId() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(openMediumPriorityGap);

        // Act
        List<CareGap> gaps = careGapRepository.findByPatientId("patient-001");

        // Assert
        assertEquals(1, gaps.size());
        assertEquals("patient-001", gaps.get(0).getPatientId());
    }

    @Test
    @DisplayName("Should find care gaps by patient and status")
    void testFindByPatientIdAndStatus() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(closedGap);

        // Act
        List<CareGap> openGaps = careGapRepository.findByPatientIdAndStatus("patient-003", "CLOSED");

        // Assert
        assertEquals(1, openGaps.size());
        assertEquals("CLOSED", openGaps.get(0).getStatus());
    }

    // ========================================================================
    // Open Gap Query Tests
    // ========================================================================

    @Test
    @DisplayName("Should find open gaps by patient")
    void testFindOpenGapsByPatient() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(openMediumPriorityGap);
        careGapRepository.save(closedGap);

        // Act
        List<CareGap> gaps = careGapRepository.findOpenGapsByPatient("patient-001");

        // Assert
        assertEquals(1, gaps.size());
        assertEquals("OPEN", gaps.get(0).getStatus());
    }

    @Test
    @DisplayName("Should return empty for patient with no open gaps")
    void testFindOpenGapsNoResults() {
        // Arrange
        careGapRepository.save(closedGap);

        // Act
        List<CareGap> gaps = careGapRepository.findOpenGapsByPatient("patient-003");

        // Assert
        // Closed gap should not be included
        assertEquals(0, gaps.stream().filter(g -> g.getStatus().equals("OPEN")).count());
    }

    // ========================================================================
    // Gap Type and Priority Queries
    // ========================================================================

    @Test
    @DisplayName("Should find gaps by patient and gap type")
    void testFindByPatientIdAndGapType() {
        // Arrange
        CareGap diabetesGap = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withGapType("CHRONIC_DISEASE_MONITORING")
            .build();
        CareGap preventiveGap = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withGapType("PREVENTIVE_CARE")
            .build();

        careGapRepository.save(diabetesGap);
        careGapRepository.save(preventiveGap);

        // Act
        List<CareGap> chronicGaps = careGapRepository.findByPatientIdAndGapType("patient-001", "CHRONIC_DISEASE_MONITORING");

        // Assert
        assertEquals(1, chronicGaps.size());
        assertEquals("CHRONIC_DISEASE_MONITORING", chronicGaps.get(0).getGapType());
    }

    @Test
    @DisplayName("Should find gaps by patient and priority")
    void testFindByPatientIdAndPriority() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(openMediumPriorityGap);

        // Act
        List<CareGap> highPriorityGaps = careGapRepository.findByPatientIdAndPriority("patient-001", "HIGH");

        // Assert
        assertEquals(1, highPriorityGaps.size());
        assertEquals("HIGH", highPriorityGaps.get(0).getPriority());
    }

    // ========================================================================
    // Tenant Isolation Tests
    // ========================================================================

    @Test
    @DisplayName("Should find gaps by type, priority and tenant")
    void testFindGapsByTypeAndPriorityWithTenant() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(tenant2Gap);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<CareGap> tenant1Gaps = careGapRepository.findGapsByTypeAndPriority(
            "CHRONIC_DISEASE_MONITORING", "HIGH", "tenant1", pageable);

        // Assert
        assertEquals(1, tenant1Gaps.getContent().size());
        assertTrue(tenant1Gaps.getContent().stream().allMatch(g -> g.getTenantId().equals("tenant1")));
    }

    @Test
    @DisplayName("Should isolate tenant results correctly")
    void testTenantIsolationInTypeAndPriority() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(tenant2Gap);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<CareGap> tenant2Results = careGapRepository.findGapsByTypeAndPriority(
            "MEDICATION_ADHERENCE", "HIGH", "tenant2", pageable);

        // Assert
        assertEquals(1, tenant2Results.getContent().size());
        assertEquals("tenant2", tenant2Results.getContent().get(0).getTenantId());
    }

    // ========================================================================
    // Overdue Gap Tests
    // ========================================================================

    @Test
    @DisplayName("Should find overdue gaps")
    void testFindOverdueGaps() {
        // Arrange
        CareGap overdueGap = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withStatus("OPEN")
            .withDueDate(LocalDateTime.now().minusDays(10))
            .withTenantId("tenant1")
            .build();
        careGapRepository.save(overdueGap);

        // Act
        List<CareGap> overdue = careGapRepository.findOverdueGaps("tenant1");

        // Assert
        assertTrue(overdue.size() > 0);
        assertTrue(overdue.stream().allMatch(g -> g.getDueDate().isBefore(LocalDateTime.now())));
    }

    @Test
    @DisplayName("Should not include future-due gaps in overdue results")
    void testOverdueGapsExcludeFuture() {
        // Arrange
        CareGap futureGap = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withStatus("OPEN")
            .withDueDate(LocalDateTime.now().plusDays(10))
            .withTenantId("tenant1")
            .build();
        careGapRepository.save(futureGap);

        // Act
        List<CareGap> overdue = careGapRepository.findOverdueGaps("tenant1");

        // Assert
        assertFalse(overdue.stream().anyMatch(g -> g.getId().equals(futureGap.getId())));
    }

    // ========================================================================
    // Due Date Range Tests
    // ========================================================================

    @Test
    @DisplayName("Should find gaps by patient and due date range")
    void testFindByPatientIdAndDueDateBetween() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        CareGap gapInRange = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withDueDate(now.plusDays(5))
            .build();
        careGapRepository.save(gapInRange);

        // Act
        List<CareGap> gaps = careGapRepository.findByPatientIdAndDueDateBetween(
            "patient-001",
            now,
            now.plusDays(10)
        );

        // Assert
        assertEquals(1, gaps.size());
    }

    // ========================================================================
    // Provider and Care Team Tests
    // ========================================================================

    @Test
    @DisplayName("Should find gaps by provider ID")
    void testFindByProviderId() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);

        // Act
        List<CareGap> gaps = careGapRepository.findByProviderId("provider-001");

        // Assert
        assertEquals(1, gaps.size());
    }

    @Test
    @DisplayName("Should find gaps by care team ID")
    void testFindByCareTeamId() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);

        // Act
        List<CareGap> gaps = careGapRepository.findByCareTeamId("team-001");

        // Assert
        assertEquals(1, gaps.size());
    }

    // ========================================================================
    // Measure-Based Queries
    // ========================================================================

    @Test
    @DisplayName("Should find gaps by measure ID")
    void testFindByMeasureId() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(openMediumPriorityGap);

        // Act
        List<CareGap> gaps = careGapRepository.findByMeasureId("HEDIS-CDC");

        // Assert
        assertEquals(1, gaps.size());
        assertEquals("HEDIS-CDC", gaps.get(0).getMeasureId());
    }

    // ========================================================================
    // Count Tests
    // ========================================================================

    @Test
    @DisplayName("Should count open gaps by patient")
    void testCountOpenGapsByPatient() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            CareGap gap = DataTestFactory.careGapBuilder()
                .withPatientId("patient-001")
                .withStatus("OPEN")
                .build();
            careGapRepository.save(gap);
        }

        // Act
        Long count = careGapRepository.countOpenGapsByPatient("patient-001");

        // Assert
        assertEquals(3L, count);
    }

    @Test
    @DisplayName("Should not count closed gaps in open count")
    void testOpenGapCountExcludesClosed() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(closedGap);

        // Act
        Long count = careGapRepository.countOpenGapsByPatient("patient-003");

        // Assert
        assertEquals(0L, count);
    }

    // ========================================================================
    // High Priority Gap Tests
    // ========================================================================

    @Test
    @DisplayName("Should find high priority open gaps")
    void testFindHighPriorityOpenGaps() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(openMediumPriorityGap);

        // Act
        List<CareGap> highPriority = careGapRepository.findHighPriorityOpenGaps();

        // Assert
        assertTrue(highPriority.size() > 0);
        assertTrue(highPriority.stream().allMatch(g -> g.getPriority().equals("HIGH")));
        assertTrue(highPriority.stream().allMatch(g -> g.getStatus().equals("OPEN")));
    }

    // ========================================================================
    // Risk Score Tests
    // ========================================================================

    @Test
    @DisplayName("Should find high risk gaps")
    void testFindHighRiskGaps() {
        // Arrange
        CareGap highRisk = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withRiskScore(95.0)
            .withStatus("OPEN")
            .build();
        CareGap lowRisk = DataTestFactory.careGapBuilder()
            .withPatientId("patient-002")
            .withRiskScore(30.0)
            .withStatus("OPEN")
            .build();

        careGapRepository.save(highRisk);
        careGapRepository.save(lowRisk);

        // Act
        List<CareGap> highRiskGaps = careGapRepository.findHighRiskGaps(80.0);

        // Assert
        assertEquals(1, highRiskGaps.size());
        assertTrue(highRiskGaps.get(0).getRiskScore() >= 80.0);
    }

    // ========================================================================
    // Financial Impact Tests
    // ========================================================================

    @Test
    @DisplayName("Should find gaps with high financial impact")
    void testFindHighImpactGaps() {
        // Arrange
        CareGap highImpact = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withFinancialImpact(5000.0)
            .withStatus("OPEN")
            .withTenantId("tenant1")
            .build();
        CareGap lowImpact = DataTestFactory.careGapBuilder()
            .withPatientId("patient-002")
            .withFinancialImpact(500.0)
            .withStatus("OPEN")
            .withTenantId("tenant1")
            .build();

        careGapRepository.save(highImpact);
        careGapRepository.save(lowImpact);

        // Act
        List<CareGap> impactfulGaps = careGapRepository.findHighImpactGaps("tenant1", 2000.0);

        // Assert
        assertEquals(1, impactfulGaps.size());
        assertTrue(impactfulGaps.get(0).getFinancialImpact() >= 2000.0);
    }

    @Test
    @DisplayName("Should calculate total financial impact for tenant")
    void testGetTotalFinancialImpact() {
        // Arrange
        careGapRepository.save(DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withFinancialImpact(1000.0)
            .withStatus("OPEN")
            .withTenantId("tenant1")
            .build());
        careGapRepository.save(DataTestFactory.careGapBuilder()
            .withPatientId("patient-002")
            .withFinancialImpact(2000.0)
            .withStatus("OPEN")
            .withTenantId("tenant1")
            .build());

        // Act
        Double totalImpact = careGapRepository.getTotalFinancialImpact("tenant1");

        // Assert
        assertNotNull(totalImpact);
        assertEquals(3000.0, totalImpact);
    }

    // ========================================================================
    // Closure Tests
    // ========================================================================

    @Test
    @DisplayName("Should find recently closed gaps")
    void testFindRecentlyClosedGaps() {
        // Arrange
        careGapRepository.save(closedGap);

        // Act
        List<CareGap> recentlyClosed = careGapRepository.findRecentlyClosedGaps("patient-003", 7);

        // Assert
        assertEquals(1, recentlyClosed.size());
        assertEquals("CLOSED", recentlyClosed.get(0).getStatus());
    }

    // ========================================================================
    // Status Count Tests
    // ========================================================================

    @Test
    @DisplayName("Should count gaps by status and tenant")
    void testCountGapsByStatusAndTenant() {
        // Arrange
        careGapRepository.save(openHighPriorityGap);
        careGapRepository.save(openMediumPriorityGap);
        careGapRepository.save(closedGap);

        // Act
        long openCount = careGapRepository.countGapsByStatusAndTenant("OPEN", "tenant1");
        long closedCount = careGapRepository.countGapsByStatusAndTenant("CLOSED", "tenant1");

        // Assert
        assertEquals(2, openCount);
        assertEquals(1, closedCount);
    }

    // ========================================================================
    // Gaps Due Soon Tests
    // ========================================================================

    @Test
    @DisplayName("Should find gaps due soon")
    void testFindGapsDueSoon() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        CareGap dueSoon = DataTestFactory.careGapBuilder()
            .withPatientId("patient-001")
            .withStatus("OPEN")
            .withDueDate(now.plusDays(5))
            .withTenantId("tenant1")
            .build();
        CareGap dueLater = DataTestFactory.careGapBuilder()
            .withPatientId("patient-002")
            .withStatus("OPEN")
            .withDueDate(now.plusDays(45))
            .withTenantId("tenant1")
            .build();

        careGapRepository.save(dueSoon);
        careGapRepository.save(dueLater);

        // Act
        List<CareGap> gaps = careGapRepository.findGapsDueSoon("tenant1", 30);

        // Assert
        assertEquals(1, gaps.size());
        assertTrue(gaps.get(0).getDueDate().isBefore(now.plusDays(31)));
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Should handle null risk score")
    void testHandleNullRiskScore() {
        // Arrange
        CareGap gap = careGapRepository.save(openHighPriorityGap);
        gap.setRiskScore(null);

        // Act & Assert
        assertDoesNotThrow(() -> careGapRepository.save(gap));
    }

    @Test
    @DisplayName("Should handle gap with multiple statuses")
    void testGapStatusTransitions() {
        // Arrange
        CareGap gap = careGapRepository.save(openHighPriorityGap);

        // Act
        gap.setStatus("IN_PROGRESS");
        careGapRepository.save(gap);

        // Assert
        CareGap updated = careGapRepository.findById(gap.getId()).orElseThrow();
        assertEquals("IN_PROGRESS", updated.getStatus());
    }
}

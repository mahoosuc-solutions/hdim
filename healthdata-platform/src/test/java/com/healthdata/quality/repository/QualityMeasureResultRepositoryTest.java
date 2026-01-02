package com.healthdata.quality.repository;

import com.healthdata.BaseRepositoryTest;
import com.healthdata.DataTestFactory;
import com.healthdata.quality.domain.MeasureResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quality Measure Result Repository Integration Tests
 * Tests quality measure calculations and compliance tracking
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Quality Measure Result Repository Tests")
public class QualityMeasureResultRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private QualityMeasureResultRepository measureResultRepository;

    private MeasureResult compliantResult;
    private MeasureResult nonCompliantResult;
    private MeasureResult tenant2Result;

    @BeforeEach
    void setUp() {
        // Compliant result
        compliantResult = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withMeasureId("HEDIS-CDC")
            .withScore(85.0)
            .withNumerator(1)
            .withDenominator(1)
            .withCompliant(true)
            .withTenantId("tenant1")
            .build();

        // Non-compliant result
        nonCompliantResult = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-002")
            .withMeasureId("HEDIS-CDC")
            .withScore(45.0)
            .withNumerator(0)
            .withDenominator(1)
            .withCompliant(false)
            .withTenantId("tenant1")
            .build();

        // Tenant 2 result
        tenant2Result = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-101")
            .withMeasureId("HEDIS-HTN")
            .withScore(65.0)
            .withNumerator(1)
            .withDenominator(1)
            .withCompliant(true)
            .withTenantId("tenant2")
            .build();
    }

    // ========================================================================
    // Basic CRUD Tests
    // ========================================================================

    @Test
    @DisplayName("Should save and retrieve measure result")
    void testSaveAndRetrieveMeasureResult() {
        // Arrange & Act
        MeasureResult saved = measureResultRepository.save(compliantResult);
        Optional<MeasureResult> retrieved = measureResultRepository.findById(saved.getId());

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals("patient-001", retrieved.get().getPatientId());
        assertTrue(retrieved.get().isCompliant());
    }

    @Test
    @DisplayName("Should update measure result")
    void testUpdateMeasureResult() {
        // Arrange
        MeasureResult saved = measureResultRepository.save(compliantResult);

        // Act
        saved.setScore(90.0);
        measureResultRepository.save(saved);
        MeasureResult updated = measureResultRepository.findById(saved.getId()).orElseThrow();

        // Assert
        assertEquals(90.0, updated.getScore());
    }

    @Test
    @DisplayName("Should delete measure result")
    void testDeleteMeasureResult() {
        // Arrange
        MeasureResult saved = measureResultRepository.save(compliantResult);
        String id = saved.getId();

        // Act
        measureResultRepository.deleteById(id);

        // Assert
        assertFalse(measureResultRepository.existsById(id));
    }

    // ========================================================================
    // Patient and Measure Query Tests
    // ========================================================================

    @Test
    @DisplayName("Should find measure results by patient and measure")
    void testFindByPatientIdAndMeasureId() {
        // Arrange
        measureResultRepository.save(compliantResult);
        measureResultRepository.save(nonCompliantResult);

        // Act
        List<MeasureResult> results = measureResultRepository.findByPatientIdAndMeasureId("patient-001", "HEDIS-CDC");

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).isCompliant());
    }

    @Test
    @DisplayName("Should return empty for non-existent patient measure")
    void testFindByPatientAndMeasureNotFound() {
        // Arrange
        measureResultRepository.save(compliantResult);

        // Act
        List<MeasureResult> results = measureResultRepository.findByPatientIdAndMeasureId("nonexistent", "HEDIS-CDC");

        // Assert
        assertEquals(0, results.size());
    }

    // ========================================================================
    // Latest Result Tests
    // ========================================================================

    @Test
    @DisplayName("Should find latest result for patient and measure")
    void testFindLatestByPatientAndMeasure() {
        // Arrange
        MeasureResult oldResult = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withMeasureId("HEDIS-CDC")
            .withScore(70.0)
            .build();
        oldResult.setCalculationDate(LocalDateTime.now().minusDays(30));
        measureResultRepository.save(oldResult);

        MeasureResult newResult = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withMeasureId("HEDIS-CDC")
            .withScore(85.0)
            .build();
        measureResultRepository.save(newResult);

        // Act
        Optional<MeasureResult> latest = measureResultRepository.findLatestByPatientAndMeasure("patient-001", "HEDIS-CDC");

        // Assert
        assertTrue(latest.isPresent());
        assertEquals(85.0, latest.get().getScore());
    }

    // ========================================================================
    // Compliance Filter Tests
    // ========================================================================

    @Test
    @DisplayName("Should find compliant results for patient")
    void testFindCompliantResultsByPatient() {
        // Arrange
        MeasureResult compliant = DataTestFactory.createCompliantResult("patient-001", "HEDIS-CDC", "tenant1");
        MeasureResult nonCompliant = DataTestFactory.createNonCompliantResult("patient-001", "HEDIS-HTN", "tenant1");

        measureResultRepository.save(compliant);
        measureResultRepository.save(nonCompliant);

        // Act
        List<MeasureResult> results = measureResultRepository.findCompliantResultsByPatient("patient-001");

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).isCompliant());
    }

    @Test
    @DisplayName("Should find non-compliant results for patient")
    void testFindNonCompliantResultsByPatient() {
        // Arrange
        MeasureResult compliant = DataTestFactory.createCompliantResult("patient-001", "HEDIS-CDC", "tenant1");
        MeasureResult nonCompliant = DataTestFactory.createNonCompliantResult("patient-001", "HEDIS-HTN", "tenant1");

        measureResultRepository.save(compliant);
        measureResultRepository.save(nonCompliant);

        // Act
        List<MeasureResult> results = measureResultRepository.findNonCompliantResultsByPatient("patient-001");

        // Assert
        assertEquals(1, results.size());
        assertFalse(results.get(0).isCompliant());
    }

    // ========================================================================
    // Tenant Isolation Tests
    // ========================================================================

    @Test
    @DisplayName("Should find results by tenant and compliance status")
    void testFindByTenantIdAndCompliant() {
        // Arrange
        measureResultRepository.save(compliantResult);
        measureResultRepository.save(nonCompliantResult);
        measureResultRepository.save(tenant2Result);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<MeasureResult> tenant1Compliant = measureResultRepository.findByTenantIdAndCompliant("tenant1", true, pageable);
        Page<MeasureResult> tenant2Compliant = measureResultRepository.findByTenantIdAndCompliant("tenant2", true, pageable);

        // Assert
        assertEquals(1, tenant1Compliant.getContent().size());
        assertEquals(1, tenant2Compliant.getContent().size());
    }

    @Test
    @DisplayName("Should isolate results by tenant in paginated queries")
    void testTenantIsolationInPagination() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            MeasureResult result = DataTestFactory.measureResultBuilder()
                .withPatientId("patient-00" + i)
                .withMeasureId("HEDIS-CDC")
                .withCompliant(true)
                .withTenantId("tenant1")
                .build();
            measureResultRepository.save(result);
        }
        measureResultRepository.save(tenant2Result);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<MeasureResult> tenant1Results = measureResultRepository.findByTenantIdAndCompliant("tenant1", true, pageable);

        // Assert
        assertEquals(3, tenant1Results.getContent().size());
        assertTrue(tenant1Results.getContent().stream().allMatch(r -> r.getTenantId().equals("tenant1")));
    }

    // ========================================================================
    // Aggregation Query Tests
    // ========================================================================

    @Test
    @DisplayName("Should aggregate measure results by measure and tenant")
    void testAggregateMeasureResults() {
        // Arrange
        MeasureResult result1 = DataTestFactory.createCompliantResult("patient-001", "HEDIS-CDC", "tenant1");
        MeasureResult result2 = DataTestFactory.createCompliantResult("patient-002", "HEDIS-CDC", "tenant1");
        MeasureResult result3 = DataTestFactory.createNonCompliantResult("patient-003", "HEDIS-CDC", "tenant1");

        measureResultRepository.save(result1);
        measureResultRepository.save(result2);
        measureResultRepository.save(result3);

        // Act
        Object[] aggregation = measureResultRepository.aggregateMeasureResults("HEDIS-CDC", "tenant1");

        // Assert
        assertNotNull(aggregation);
        assertEquals(3L, aggregation[0]); // Total count
        assertEquals(2L, aggregation[1]); // Compliant count
    }

    // ========================================================================
    // Compliance Counting Tests
    // ========================================================================

    @Test
    @DisplayName("Should count compliant results by measure and tenant")
    void testCountCompliantByMeasureAndTenant() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            MeasureResult compliant = DataTestFactory.createCompliantResult("patient-00" + i, "HEDIS-CDC", "tenant1");
            measureResultRepository.save(compliant);
        }
        for (int i = 3; i < 5; i++) {
            MeasureResult nonCompliant = DataTestFactory.createNonCompliantResult("patient-00" + i, "HEDIS-CDC", "tenant1");
            measureResultRepository.save(nonCompliant);
        }

        // Act
        long compliantCount = measureResultRepository.countByMeasureAndTenantAndCompliant("HEDIS-CDC", "tenant1", true);
        long nonCompliantCount = measureResultRepository.countByMeasureAndTenantAndCompliant("HEDIS-CDC", "tenant1", false);

        // Assert
        assertEquals(3, compliantCount);
        assertEquals(2, nonCompliantCount);
    }

    // ========================================================================
    // Date Range Query Tests
    // ========================================================================

    @Test
    @DisplayName("Should find results within measurement period")
    void testFindByMeasurementPeriod() {
        // Arrange
        MeasureResult result2024 = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withMeasureId("HEDIS-CDC")
            .build();
        result2024.setPeriodStart(LocalDate.of(2024, 1, 1));
        result2024.setPeriodEnd(LocalDate.of(2024, 12, 31));
        measureResultRepository.save(result2024);

        // Act
        List<MeasureResult> results = measureResultRepository.findByTenantAndMeasurementPeriod(
            "tenant1",
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31)
        );

        // Assert
        assertTrue(results.size() > 0);
    }

    @Test
    @DisplayName("Should find results within calculation date range")
    void testFindByCalculationDateRange() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        MeasureResult recent = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withMeasureId("HEDIS-CDC")
            .build();
        recent.setCalculationDate(now.minusDays(1));
        measureResultRepository.save(recent);

        // Act
        List<MeasureResult> results = measureResultRepository.findByPatientIdAndCalculationDateRange(
            "patient-001",
            now.minusDays(5),
            now
        );

        // Assert
        assertEquals(1, results.size());
    }

    // ========================================================================
    // Performance Metrics Tests
    // ========================================================================

    @Test
    @DisplayName("Should calculate average score by measure")
    void testGetAverageScoreByMeasure() {
        // Arrange
        MeasureResult result1 = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withMeasureId("HEDIS-CDC")
            .withScore(80.0)
            .withTenantId("tenant1")
            .build();
        MeasureResult result2 = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-002")
            .withMeasureId("HEDIS-CDC")
            .withScore(90.0)
            .withTenantId("tenant1")
            .build();

        measureResultRepository.save(result1);
        measureResultRepository.save(result2);

        // Act
        Double average = measureResultRepository.getAverageScoreByMeasure("HEDIS-CDC", "tenant1");

        // Assert
        assertNotNull(average);
        assertEquals(85.0, average);
    }

    @Test
    @DisplayName("Should calculate compliance rate")
    void testGetComplianceRate() {
        // Arrange
        for (int i = 0; i < 80; i++) {
            MeasureResult compliant = DataTestFactory.createCompliantResult("patient-" + i, "HEDIS-CDC", "tenant1");
            measureResultRepository.save(compliant);
        }
        for (int i = 80; i < 100; i++) {
            MeasureResult nonCompliant = DataTestFactory.createNonCompliantResult("patient-" + i, "HEDIS-CDC", "tenant1");
            measureResultRepository.save(nonCompliant);
        }

        // Act
        Double complianceRate = measureResultRepository.getComplianceRate("HEDIS-CDC", "tenant1");

        // Assert
        assertNotNull(complianceRate);
        assertTrue(complianceRate > 75.0 && complianceRate < 85.0); // Should be around 80%
    }

    // ========================================================================
    // Denominator Tests
    // ========================================================================

    @Test
    @DisplayName("Should find results where patient is in denominator")
    void testFindByPatientAndMeasureInDenominator() {
        // Arrange
        MeasureResult inDenominator = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withMeasureId("HEDIS-CDC")
            .withNumerator(1)
            .withDenominator(1)
            .build();
        MeasureResult notInDenominator = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-002")
            .withMeasureId("HEDIS-CDC")
            .withNumerator(0)
            .withDenominator(0)
            .build();

        measureResultRepository.save(inDenominator);
        measureResultRepository.save(notInDenominator);

        // Act
        List<MeasureResult> results = measureResultRepository.findByPatientAndMeasureInDenominator("patient-001", "HEDIS-CDC");

        // Assert
        assertEquals(1, results.size());
        assertTrue(results.get(0).getDenominator() > 0);
    }

    // ========================================================================
    // Performance Ranking Tests
    // ========================================================================

    @Test
    @DisplayName("Should find top performers by score")
    void testFindTopPerformers() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            MeasureResult result = DataTestFactory.measureResultBuilder()
                .withPatientId("patient-" + i)
                .withMeasureId("HEDIS-CDC")
                .withScore(50.0 + (i * 10))
                .withTenantId("tenant1")
                .build();
            measureResultRepository.save(result);
        }

        // Act
        Pageable pageable = PageRequest.of(0, 2);
        Page<MeasureResult> topPerformers = measureResultRepository.findTopPerformers("tenant1", pageable);

        // Assert
        assertEquals(2, topPerformers.getContent().size());
        assertEquals(90.0, topPerformers.getContent().get(0).getScore()); // Highest score first
    }

    // ========================================================================
    // Below Threshold Tests
    // ========================================================================

    @Test
    @DisplayName("Should find results below performance threshold")
    void testFindBelowPerformanceThreshold() {
        // Arrange
        MeasureResult lowScore = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withMeasureId("HEDIS-CDC")
            .withScore(30.0)
            .withTenantId("tenant1")
            .build();
        MeasureResult highScore = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-002")
            .withMeasureId("HEDIS-CDC")
            .withScore(80.0)
            .withTenantId("tenant1")
            .build();

        measureResultRepository.save(lowScore);
        measureResultRepository.save(highScore);

        // Act
        List<MeasureResult> results = measureResultRepository.findBelowPerformanceThreshold("tenant1", 50.0);

        // Assert
        assertEquals(1, results.size());
        assertEquals(30.0, results.get(0).getScore());
    }

    // ========================================================================
    // Count Tests
    // ========================================================================

    @Test
    @DisplayName("Should count results by patient and measure")
    void testCountByPatientAndMeasure() {
        // Arrange
        for (int i = 0; i < 3; i++) {
            MeasureResult result = DataTestFactory.measureResultBuilder()
                .withPatientId("patient-001")
                .withMeasureId("HEDIS-CDC")
                .build();
            measureResultRepository.save(result);
        }

        // Act
        long count = measureResultRepository.countByPatientAndMeasure("patient-001", "HEDIS-CDC");

        // Assert
        assertEquals(3, count);
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Should handle null average for non-existent measure")
    void testAverageForNonExistentMeasure() {
        // Act
        Double average = measureResultRepository.getAverageScoreByMeasure("NONEXISTENT", "tenant1");

        // Assert
        assertNull(average);
    }

    @Test
    @DisplayName("Should calculate percentage correctly")
    void testCalculatePercentage() {
        // Arrange
        MeasureResult result = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withNumerator(3)
            .withDenominator(4)
            .build();

        // Act
        double percentage = result.getPercentage();

        // Assert
        assertEquals(75.0, percentage);
    }

    @Test
    @DisplayName("Should handle zero denominator in percentage")
    void testPercentageWithZeroDenominator() {
        // Arrange
        MeasureResult result = DataTestFactory.measureResultBuilder()
            .withPatientId("patient-001")
            .withNumerator(0)
            .withDenominator(0)
            .build();

        // Act
        double percentage = result.getPercentage();

        // Assert
        assertEquals(0.0, percentage);
    }

    @Test
    @DisplayName("Should return correct compliance status")
    void testGetComplianceStatus() {
        // Arrange
        MeasureResult compliant = DataTestFactory.createCompliantResult("patient-001", "HEDIS-CDC", "tenant1");
        MeasureResult nonCompliant = DataTestFactory.createNonCompliantResult("patient-002", "HEDIS-CDC", "tenant1");

        // Act
        String compliantStatus = compliant.getComplianceStatus();
        String nonCompliantStatus = nonCompliant.getComplianceStatus();

        // Assert
        assertEquals("COMPLIANT", compliantStatus);
        assertEquals("NON_COMPLIANT", nonCompliantStatus);
    }
}

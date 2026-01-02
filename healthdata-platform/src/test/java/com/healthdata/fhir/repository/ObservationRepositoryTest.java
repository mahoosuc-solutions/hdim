package com.healthdata.fhir.repository;

import com.healthdata.BaseRepositoryTest;
import com.healthdata.DataTestFactory;
import com.healthdata.fhir.domain.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Observation Repository Integration Tests
 * Tests FHIR observations with LOINC code searching
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Observation Repository Tests")
public class ObservationRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private ObservationRepository observationRepository;

    private Observation bloodPressureObs;
    private Observation glucoseObs;
    private Observation laboratoryObs;

    @BeforeEach
    void setUp() {
        // Blood Pressure observation
        bloodPressureObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6") // Systolic BP
            .withDisplay("Systolic Blood Pressure")
            .withValueQuantity(BigDecimal.valueOf(145.0))
            .withValueUnit("mmHg")
            .withCategory("vital-signs")
            .withTenantId("tenant1")
            .build();

        // Glucose observation
        glucoseObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("2345-7") // Glucose
            .withDisplay("Fasting Glucose")
            .withValueQuantity(BigDecimal.valueOf(158.0))
            .withValueUnit("mg/dL")
            .withCategory("laboratory")
            .withTenantId("tenant1")
            .build();

        // Another laboratory observation
        laboratoryObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-002")
            .withCode("4548-4") // HbA1c
            .withDisplay("Hemoglobin A1c")
            .withValueQuantity(BigDecimal.valueOf(8.5))
            .withValueUnit("%")
            .withCategory("laboratory")
            .withTenantId("tenant1")
            .build();
    }

    // ========================================================================
    // Basic CRUD Tests
    // ========================================================================

    @Test
    @DisplayName("Should save and retrieve observation")
    void testSaveAndRetrieveObservation() {
        // Arrange & Act
        Observation saved = observationRepository.save(bloodPressureObs);
        Optional<Observation> retrieved = observationRepository.findById(saved.getId());

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals("8480-6", retrieved.get().getCode());
        assertEquals(BigDecimal.valueOf(145.0), retrieved.get().getValueQuantity());
    }

    @Test
    @DisplayName("Should update observation")
    void testUpdateObservation() {
        // Arrange
        Observation saved = observationRepository.save(bloodPressureObs);

        // Act
        saved.setValueQuantity(BigDecimal.valueOf(150.0));
        observationRepository.save(saved);
        Observation updated = observationRepository.findById(saved.getId()).orElseThrow();

        // Assert
        assertEquals(BigDecimal.valueOf(150.0), updated.getValueQuantity());
    }

    @Test
    @DisplayName("Should delete observation")
    void testDeleteObservation() {
        // Arrange
        Observation saved = observationRepository.save(bloodPressureObs);
        String id = saved.getId();

        // Act
        observationRepository.deleteById(id);

        // Assert
        assertFalse(observationRepository.existsById(id));
    }

    // ========================================================================
    // LOINC Code Search Tests
    // ========================================================================

    @Test
    @DisplayName("Should find observations by patient and code")
    void testFindByPatientIdAndCode() {
        // Arrange
        observationRepository.save(bloodPressureObs);
        observationRepository.save(glucoseObs);

        // Act
        List<Observation> results = observationRepository.findByPatientIdAndCode("patient-001", "8480-6");

        // Assert
        assertEquals(1, results.size());
        assertEquals("Systolic Blood Pressure", results.get(0).getDisplay());
    }

    @Test
    @DisplayName("Should return empty for non-existent code")
    void testFindByPatientIdAndCodeNotFound() {
        // Arrange
        observationRepository.save(bloodPressureObs);

        // Act
        List<Observation> results = observationRepository.findByPatientIdAndCode("patient-001", "NONEXISTENT");

        // Assert
        assertEquals(0, results.size());
    }

    @Test
    @DisplayName("Should find observations by patient ID")
    void testFindByPatientId() {
        // Arrange
        observationRepository.save(bloodPressureObs);
        Observation obs2 = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("2345-7")
            .withDisplay("Fasting Glucose")
            .withValueQuantity(BigDecimal.valueOf(130.0))
            .withTenantId("tenant1")
            .build();
        observationRepository.save(obs2);

        // Act
        List<Observation> results = observationRepository.findByPatientId("patient-001");

        // Assert
        assertEquals(2, results.size());
    }

    // ========================================================================
    // Category-Based Search Tests
    // ========================================================================

    @Test
    @DisplayName("Should find observations by patient and category")
    void testFindByPatientIdAndCategory() {
        // Arrange
        observationRepository.save(bloodPressureObs);
        observationRepository.save(glucoseObs);
        observationRepository.save(laboratoryObs);

        // Act
        Page<Observation> vitalSigns = observationRepository.findByPatientIdAndCategoryOrderByEffectiveDateDesc(
            "patient-001", "vital-signs", PageRequest.of(0, 10));
        Page<Observation> labs = observationRepository.findByPatientIdAndCategoryOrderByEffectiveDateDesc(
            "patient-001", "laboratory", PageRequest.of(0, 10));

        // Assert
        assertEquals(1, vitalSigns.getContent().size());
        assertEquals(1, labs.getContent().size());
    }

    @Test
    @DisplayName("Should isolate observations by category correctly")
    void testCategoryIsolation() {
        // Arrange
        observationRepository.save(bloodPressureObs);
        observationRepository.save(glucoseObs);
        observationRepository.save(laboratoryObs);

        // Act
        Page<Observation> vitalSigns = observationRepository.findByPatientIdAndCategoryOrderByEffectiveDateDesc(
            "patient-001", "vital-signs", PageRequest.of(0, 10));

        // Assert
        assertEquals(1, vitalSigns.getContent().size());
        assertEquals("vital-signs", vitalSigns.getContent().get(0).getCategory());
    }

    // ========================================================================
    // Patient-Based Search Tests (from original file)
    // ========================================================================

    // Note: testFindByPatientId and testFindByPatientIdAndCode are already defined
    // in the LOINC Code Search Tests section above

    // ========================================================================
    // Date Range Query Tests
    // ========================================================================

    @Test
    @DisplayName("Should find observations within date range")
    void testFindByDateRange() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Observation oldObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .withEffectiveDate(now.minusDays(10))
            .withTenantId("tenant1")
            .build();
        Observation recentObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .withEffectiveDate(now.minusDays(1))
            .withTenantId("tenant1")
            .build();

        observationRepository.save(oldObs);
        observationRepository.save(recentObs);

        // Act
        LocalDateTime startDate = now.minusDays(5);
        LocalDateTime endDate = now;
        List<Observation> results = observationRepository.findByPatientIdAndDateRange(
            "patient-001", startDate, endDate);

        // Assert
        assertEquals(1, results.size());
        assertEquals(recentObs.getId(), results.get(0).getId());
    }

    @Test
    @DisplayName("Should return empty for date range with no observations")
    void testFindByDateRangeNoResults() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        observationRepository.save(bloodPressureObs);

        // Act
        LocalDateTime startDate = now.minusDays(30);
        LocalDateTime endDate = now.minusDays(20);
        List<Observation> results = observationRepository.findByPatientIdAndDateRange(
            "patient-001", startDate, endDate);

        // Assert
        assertEquals(0, results.size());
    }

    // ========================================================================
    // Tenant Isolation Tests
    // ========================================================================

    @Test
    @DisplayName("Should isolate observations by tenant")
    void testTenantIsolation() {
        // Arrange
        observationRepository.save(bloodPressureObs);
        observationRepository.save(glucoseObs);
        Observation tenant2Obs = DataTestFactory.observationBuilder()
            .withPatientId("patient-003")
            .withCode("8480-6")
            .withTenantId("tenant2")
            .build();
        observationRepository.save(tenant2Obs);

        // Act
        List<Observation> tenant1Results = observationRepository.findByTenantId("tenant1");
        List<Observation> tenant2Results = observationRepository.findByTenantId("tenant2");

        // Assert
        assertEquals(2, tenant1Results.size());
        assertEquals(1, tenant2Results.size());
    }

    // ========================================================================
    // Status Tests
    // ========================================================================

    @Test
    @DisplayName("Should find observations by patient and status")
    void testFindByPatientIdAndStatus() {
        // Arrange
        Observation finalObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .build(); // Status defaults to "final"
        Observation preliminaryObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("2345-7")
            .build();
        preliminaryObs.setStatus("preliminary");

        observationRepository.save(finalObs);
        observationRepository.save(preliminaryObs);

        // Act
        List<Observation> finalResults = observationRepository.findByPatientIdAndStatus("patient-001", "final");
        List<Observation> prelimResults = observationRepository.findByPatientIdAndStatus("patient-001", "preliminary");

        // Assert
        assertEquals(1, finalResults.size());
        assertEquals(1, prelimResults.size());
    }

    // ========================================================================
    // Abnormal Value Tests
    // ========================================================================

    @Test
    @DisplayName("Should find abnormal observations by patient, status and category")
    void testFindAbnormalObservations() {
        // Arrange
        Observation normalBP = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .withValueQuantity(BigDecimal.valueOf(120.0))
            .withTenantId("tenant1")
            .build();
        Observation highBP = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .withValueQuantity(BigDecimal.valueOf(180.0))
            .withTenantId("tenant1")
            .build();

        observationRepository.save(normalBP);
        observationRepository.save(highBP);

        // Act - find abnormal vitals
        List<Observation> results = observationRepository.findAbnormalObservations("patient-001", "final", "vital-signs");

        // Assert
        assertTrue(results.size() >= 1);
    }

    // ========================================================================
    // Count Tests
    // ========================================================================

    @Test
    @DisplayName("Should count observations by patient and category")
    void testCountByPatientAndCategory() {
        // Arrange
        observationRepository.save(bloodPressureObs);
        observationRepository.save(glucoseObs);

        // Act
        long count = observationRepository.countByPatientIdAndCategory("patient-001", "vital-signs");

        // Assert
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Should count all observations for a patient by category")
    void testCountByPatientIdAndLaboratory() {
        // Arrange
        observationRepository.save(bloodPressureObs);
        observationRepository.save(glucoseObs);

        // Act
        long count = observationRepository.countByPatientIdAndCategory("patient-001", "laboratory");

        // Assert
        assertEquals(1, count);
    }

    // ========================================================================
    // Complex Query Tests
    // ========================================================================

    @Test
    @DisplayName("Should find recent observations for patient by category with pagination")
    void testFindRecentObservationsByPatientAndCategory() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Observation recentVital = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .withCategory("vital-signs")
            .withEffectiveDate(now.minusHours(2))
            .withTenantId("tenant1")
            .build();
        Observation oldLab = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("2345-7")
            .withCategory("laboratory")
            .withEffectiveDate(now.minusDays(30))
            .withTenantId("tenant1")
            .build();

        observationRepository.save(recentVital);
        observationRepository.save(oldLab);

        // Act
        Page<Observation> vitalSigns = observationRepository.findByPatientIdAndCategoryOrderByEffectiveDateDesc(
            "patient-001", "vital-signs", PageRequest.of(0, 10));

        // Assert
        assertEquals(1, vitalSigns.getContent().size());
        assertEquals("vital-signs", vitalSigns.getContent().get(0).getCategory());
    }

    @Test
    @DisplayName("Should find all LOINC codes used for a patient")
    void testFindDistinctCodesForPatient() {
        // Arrange
        observationRepository.save(bloodPressureObs);
        observationRepository.save(glucoseObs);

        Observation diastolicBP = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8462-4") // Diastolic BP
            .withDisplay("Diastolic Blood Pressure")
            .withValueQuantity(BigDecimal.valueOf(92.0))
            .withTenantId("tenant1")
            .build();
        observationRepository.save(diastolicBP);

        // Act
        List<String> distinctCodes = observationRepository.findDistinctCodesByPatientId("patient-001");

        // Assert
        assertEquals(3, distinctCodes.size());
        assertTrue(distinctCodes.contains("8480-6"));
        assertTrue(distinctCodes.contains("2345-7"));
        assertTrue(distinctCodes.contains("8462-4"));
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    @DisplayName("Should handle null values gracefully")
    void testHandleNullValues() {
        // Arrange
        Observation obsWithoutQuantity = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .build();
        obsWithoutQuantity.setValueQuantity(null);

        // Act & Assert
        assertDoesNotThrow(() -> observationRepository.save(obsWithoutQuantity));
    }

    @Test
    @DisplayName("Should find observations by patient regardless of value type")
    void testFindByPatientIgnoresValueType() {
        // Arrange
        Observation quantityObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .withValueQuantity(BigDecimal.valueOf(145.0))
            .build();

        Observation stringObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("2345-7")
            .build();
        stringObs.setValueString("HIGH");

        observationRepository.save(quantityObs);
        observationRepository.save(stringObs);

        // Act
        List<Observation> results = observationRepository.findByPatientId("patient-001");

        // Assert
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should find latest observation by patient and code")
    void testFindLatestObservation() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Observation oldObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .withEffectiveDate(now.minusDays(30))
            .build();
        Observation newObs = DataTestFactory.observationBuilder()
            .withPatientId("patient-001")
            .withCode("8480-6")
            .withEffectiveDate(now.minusHours(1))
            .build();

        observationRepository.save(oldObs);
        observationRepository.save(newObs);

        // Act
        Optional<Observation> latest = observationRepository.findLatestByPatientIdAndCode("patient-001", "8480-6");

        // Assert
        assertTrue(latest.isPresent());
        assertEquals(newObs.getId(), latest.get().getId());
    }
}

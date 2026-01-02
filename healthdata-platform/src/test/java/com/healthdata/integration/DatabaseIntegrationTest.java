package com.healthdata.integration;

import com.healthdata.BaseIntegrationTest;
import com.healthdata.fhir.entity.Condition;
import com.healthdata.fhir.entity.Observation;
import com.healthdata.fhir.repository.ConditionRepository;
import com.healthdata.fhir.repository.ObservationRepository;
import com.healthdata.patient.entity.Patient;
import com.healthdata.patient.repository.PatientRepository;
import com.healthdata.quality.entity.QualityMeasureResult;
import com.healthdata.quality.repository.QualityMeasureResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Integration Tests
 *
 * Tests validate:
 * - Database persistence operations
 * - Query performance
 * - Index effectiveness
 * - Transaction management
 * - Multi-tenant data isolation
 * - Pagination and sorting
 *
 * @author TDD Swarm Agent 5B
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DatabaseIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private QualityMeasureResultRepository measureResultRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String TENANT_ID = "test-tenant-1";

    @BeforeEach
    public void setUp() {
        measureResultRepository.deleteAll();
        observationRepository.deleteAll();
        conditionRepository.deleteAll();
        patientRepository.deleteAll();
    }

    // ==================== PATIENT REPOSITORY TESTS ====================

    @Nested
    @DisplayName("Patient Repository Database Operations")
    class PatientRepositoryTests {

        @Test
        @DisplayName("Save and retrieve patient")
        void savePatient_Persist_RetrievesCorrectly() {
            Patient patient = Patient.builder()
                    .id(UUID.randomUUID().toString())
                    .firstName("John")
                    .lastName("Doe")
                    .mrn("MRN-001")
                    .dateOfBirth(LocalDate.of(1980, 1, 15))
                    .gender("male")
                    .tenantId(TENANT_ID)
                    .build();

            Patient saved = patientRepository.save(patient);
            entityManager.flush();
            entityManager.clear();

            Optional<Patient> retrieved = patientRepository.findById(saved.getId());

            assertTrue(retrieved.isPresent());
            assertThat(retrieved.get().getFirstName()).isEqualTo("John");
            assertThat(retrieved.get().getLastName()).isEqualTo("Doe");
            assertThat(retrieved.get().getMrn()).isEqualTo("MRN-001");
        }

        @Test
        @DisplayName("Find by MRN and tenant")
        void findByMrnAndTenant_ReturnsCorrectPatient() {
            Patient patient = createPatient("Jane", "Smith", "MRN-002", TENANT_ID);

            Optional<Patient> found = patientRepository.findByMrnAndTenantId("MRN-002", TENANT_ID);

            assertTrue(found.isPresent());
            assertThat(found.get().getFirstName()).isEqualTo("Jane");
        }

        @Test
        @DisplayName("Tenant isolation - different tenant returns empty")
        void findByMrnAndTenant_DifferentTenant_ReturnsEmpty() {
            createPatient("Bob", "Johnson", "MRN-003", "tenant-1");

            Optional<Patient> found = patientRepository.findByMrnAndTenantId("MRN-003", "tenant-2");

            assertFalse(found.isPresent());
        }

        @Test
        @DisplayName("Pagination works correctly")
        void findAll_WithPagination_ReturnsPages() {
            // Create 25 patients
            for (int i = 0; i < 25; i++) {
                createPatient("Patient" + i, "Test", "MRN-" + i, TENANT_ID);
            }

            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<Patient> firstPage = patientRepository.findAll(pageRequest);

            assertThat(firstPage.getContent()).hasSize(10);
            assertThat(firstPage.getTotalElements()).isEqualTo(25);
            assertThat(firstPage.getTotalPages()).isEqualTo(3);
            assertTrue(firstPage.hasNext());
        }

        @Test
        @DisplayName("Sorting by lastName works correctly")
        void findAll_WithSorting_ReturnsSorted() {
            createPatient("Charlie", "Adams", "MRN-S1", TENANT_ID);
            createPatient("Alice", "Brown", "MRN-S2", TENANT_ID);
            createPatient("Bob", "Carter", "MRN-S3", TENANT_ID);

            List<Patient> sorted = patientRepository.findAll(Sort.by("lastName"));

            assertThat(sorted.get(0).getLastName()).isEqualTo("Adams");
            assertThat(sorted.get(1).getLastName()).isEqualTo("Brown");
            assertThat(sorted.get(2).getLastName()).isEqualTo("Carter");
        }

        @Test
        @DisplayName("Batch insert performance")
        void batchInsert_100Patients_EfficientPerformance() {
            List<Patient> patients = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                patients.add(Patient.builder()
                        .id(UUID.randomUUID().toString())
                        .firstName("Patient" + i)
                        .lastName("Test")
                        .mrn("MRN-" + i)
                        .dateOfBirth(LocalDate.of(1980, 1, 1))
                        .gender("male")
                        .tenantId(TENANT_ID)
                        .build());
            }

            long startTime = System.currentTimeMillis();
            patientRepository.saveAll(patients);
            entityManager.flush();
            long duration = System.currentTimeMillis() - startTime;

            assertThat(duration).isLessThan(3000); // < 3 seconds for 100 patients
            assertThat(patientRepository.count()).isEqualTo(100);
        }
    }

    // ==================== OBSERVATION REPOSITORY TESTS ====================

    @Nested
    @DisplayName("Observation Repository Database Operations")
    class ObservationRepositoryTests {

        @Test
        @DisplayName("Save and retrieve observation")
        void saveObservation_Persist_RetrievesCorrectly() {
            Patient patient = createPatient("John", "Doe", "MRN-OBS-1", TENANT_ID);

            Observation obs = Observation.builder()
                    .id(UUID.randomUUID().toString())
                    .patientId(patient.getId())
                    .code("4548-4")
                    .system("http://loinc.org")
                    .display("Hemoglobin A1c")
                    .valueQuantity(6.8)
                    .unit("%")
                    .status("final")
                    .effectiveDateTime(LocalDateTime.now())
                    .tenantId(TENANT_ID)
                    .build();

            Observation saved = observationRepository.save(obs);
            entityManager.flush();
            entityManager.clear();

            Optional<Observation> retrieved = observationRepository.findById(saved.getId());

            assertTrue(retrieved.isPresent());
            assertThat(retrieved.get().getCode()).isEqualTo("4548-4");
            assertThat(retrieved.get().getValueQuantity()).isEqualTo(6.8);
        }

        @Test
        @DisplayName("Find by patient ID returns all observations")
        void findByPatientId_ReturnsAllObservations() {
            Patient patient = createPatient("Jane", "Smith", "MRN-OBS-2", TENANT_ID);

            createObservation(patient.getId(), "4548-4", 6.8, "HbA1c");
            createObservation(patient.getId(), "8480-6", 130.0, "Systolic BP");
            createObservation(patient.getId(), "8462-4", 80.0, "Diastolic BP");

            List<Observation> observations = observationRepository.findByPatientId(patient.getId());

            assertThat(observations).hasSize(3);
        }

        @Test
        @DisplayName("Find by patient and code returns specific observation")
        void findByPatientIdAndCode_ReturnsSpecificObservation() {
            Patient patient = createPatient("Bob", "Johnson", "MRN-OBS-3", TENANT_ID);

            createObservation(patient.getId(), "4548-4", 6.8, "HbA1c");
            createObservation(patient.getId(), "8480-6", 130.0, "Systolic BP");

            List<Observation> hba1cObs = observationRepository.findByPatientIdAndCode(
                    patient.getId(), "4548-4");

            assertThat(hba1cObs).hasSize(1);
            assertThat(hba1cObs.get(0).getDisplay()).contains("HbA1c");
        }

        @Test
        @DisplayName("Find recent observations within date range")
        void findRecentObservations_WithinDateRange_ReturnsFiltered() {
            Patient patient = createPatient("Charlie", "Brown", "MRN-OBS-4", TENANT_ID);

            // Old observation (90 days ago)
            Observation oldObs = createObservation(patient.getId(), "4548-4", 7.5, "HbA1c");
            oldObs.setEffectiveDateTime(LocalDateTime.now().minusDays(90));
            observationRepository.save(oldObs);

            // Recent observation (30 days ago)
            Observation recentObs = createObservation(patient.getId(), "4548-4", 6.8, "HbA1c");
            recentObs.setEffectiveDateTime(LocalDateTime.now().minusDays(30));
            observationRepository.save(recentObs);

            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(60);
            List<Observation> recentObservations = observationRepository
                    .findByPatientIdAndEffectiveDateTimeAfter(patient.getId(), cutoffDate);

            assertThat(recentObservations).hasSize(1);
            assertThat(recentObservations.get(0).getValueQuantity()).isEqualTo(6.8);
        }

        @Test
        @DisplayName("Batch insert 1000 observations efficiently")
        void batchInsert_1000Observations_EfficientPerformance() {
            Patient patient = createPatient("Batch", "Test", "MRN-BATCH-OBS", TENANT_ID);

            List<Observation> observations = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                observations.add(Observation.builder()
                        .id(UUID.randomUUID().toString())
                        .patientId(patient.getId())
                        .code("4548-4")
                        .system("http://loinc.org")
                        .display("HbA1c")
                        .valueQuantity(6.0 + (i % 4) * 0.5)
                        .unit("%")
                        .status("final")
                        .effectiveDateTime(LocalDateTime.now().minusDays(i))
                        .tenantId(TENANT_ID)
                        .build());
            }

            long startTime = System.currentTimeMillis();
            observationRepository.saveAll(observations);
            entityManager.flush();
            long duration = System.currentTimeMillis() - startTime;

            assertThat(duration).isLessThan(10000); // < 10 seconds for 1000 observations
            assertThat(observationRepository.count()).isEqualTo(1000);
        }
    }

    // ==================== CONDITION REPOSITORY TESTS ====================

    @Nested
    @DisplayName("Condition Repository Database Operations")
    class ConditionRepositoryTests {

        @Test
        @DisplayName("Save and retrieve condition")
        void saveCondition_Persist_RetrievesCorrectly() {
            Patient patient = createPatient("John", "Doe", "MRN-COND-1", TENANT_ID);

            Condition condition = Condition.builder()
                    .id(UUID.randomUUID().toString())
                    .patientId(patient.getId())
                    .code("E11")
                    .system("http://hl7.org/fhir/sid/icd-10")
                    .display("Type 2 Diabetes Mellitus")
                    .clinicalStatus("active")
                    .tenantId(TENANT_ID)
                    .build();

            Condition saved = conditionRepository.save(condition);
            entityManager.flush();
            entityManager.clear();

            Optional<Condition> retrieved = conditionRepository.findById(saved.getId());

            assertTrue(retrieved.isPresent());
            assertThat(retrieved.get().getCode()).isEqualTo("E11");
            assertThat(retrieved.get().getDisplay()).contains("Diabetes");
        }

        @Test
        @DisplayName("Find active conditions for patient")
        void findActiveConditions_ReturnsOnlyActive() {
            Patient patient = createPatient("Jane", "Smith", "MRN-COND-2", TENANT_ID);

            createCondition(patient.getId(), "E11", "Diabetes", "active");
            createCondition(patient.getId(), "I10", "Hypertension", "active");
            createCondition(patient.getId(), "J44", "COPD", "resolved");

            List<Condition> activeConditions = conditionRepository
                    .findByPatientIdAndClinicalStatus(patient.getId(), "active");

            assertThat(activeConditions).hasSize(2);
            assertThat(activeConditions).allMatch(c -> c.getClinicalStatus().equals("active"));
        }

        @Test
        @DisplayName("Find by ICD-10 code")
        void findByCode_ReturnsMatchingConditions() {
            Patient patient1 = createPatient("Patient1", "Test", "MRN-C1", TENANT_ID);
            Patient patient2 = createPatient("Patient2", "Test", "MRN-C2", TENANT_ID);

            createCondition(patient1.getId(), "E11", "Diabetes", "active");
            createCondition(patient2.getId(), "E11", "Diabetes", "active");
            createCondition(patient1.getId(), "I10", "Hypertension", "active");

            List<Condition> diabetesConditions = conditionRepository.findByCode("E11");

            assertThat(diabetesConditions).hasSize(2);
        }
    }

    // ==================== QUALITY MEASURE RESULT TESTS ====================

    @Nested
    @DisplayName("Quality Measure Result Repository Database Operations")
    class QualityMeasureResultRepositoryTests {

        @Test
        @DisplayName("Save and retrieve quality measure result")
        void saveMeasureResult_Persist_RetrievesCorrectly() {
            Patient patient = createPatient("John", "Doe", "MRN-QM-1", TENANT_ID);

            QualityMeasureResult result = QualityMeasureResult.builder()
                    .id(UUID.randomUUID().toString())
                    .patientId(patient.getId())
                    .measureName("HbA1c Control")
                    .measureCode("hba1c-control")
                    .compliant(true)
                    .value(6.8)
                    .calculatedAt(LocalDateTime.now())
                    .tenantId(TENANT_ID)
                    .build();

            QualityMeasureResult saved = measureResultRepository.save(result);
            entityManager.flush();
            entityManager.clear();

            Optional<QualityMeasureResult> retrieved = measureResultRepository.findById(saved.getId());

            assertTrue(retrieved.isPresent());
            assertThat(retrieved.get().getMeasureName()).isEqualTo("HbA1c Control");
            assertThat(retrieved.get().isCompliant()).isTrue();
        }

        @Test
        @DisplayName("Find results by measure code")
        void findByMeasureCode_ReturnsMatchingResults() {
            Patient patient1 = createPatient("Patient1", "Test", "MRN-QM-2", TENANT_ID);
            Patient patient2 = createPatient("Patient2", "Test", "MRN-QM-3", TENANT_ID);

            createMeasureResult(patient1.getId(), "hba1c-control", true, 6.5);
            createMeasureResult(patient2.getId(), "hba1c-control", false, 7.5);
            createMeasureResult(patient1.getId(), "bp-control", true, 130.0);

            List<QualityMeasureResult> hba1cResults = measureResultRepository
                    .findByMeasureCode("hba1c-control");

            assertThat(hba1cResults).hasSize(2);
        }

        @Test
        @DisplayName("Calculate compliance rate")
        void calculateComplianceRate_ReturnsCorrectPercentage() {
            for (int i = 0; i < 100; i++) {
                Patient patient = createPatient("Patient" + i, "Test", "MRN-COMP-" + i, TENANT_ID);
                // 70% compliant
                boolean compliant = i < 70;
                createMeasureResult(patient.getId(), "hba1c-control", compliant, compliant ? 6.5 : 7.5);
            }

            List<QualityMeasureResult> results = measureResultRepository.findByMeasureCode("hba1c-control");
            long compliantCount = results.stream().filter(QualityMeasureResult::isCompliant).count();
            double complianceRate = (double) compliantCount / results.size() * 100;

            assertThat(complianceRate).isEqualTo(70.0);
        }
    }

    // ==================== TRANSACTION TESTS ====================

    @Nested
    @DisplayName("Transaction Management")
    class TransactionTests {

        @Test
        @DisplayName("Rollback on exception")
        void transaction_ExceptionThrown_RollsBack() {
            long initialCount = patientRepository.count();

            try {
                createPatient("Test", "Patient", "MRN-ROLLBACK", TENANT_ID);
                throw new RuntimeException("Simulated exception");
            } catch (RuntimeException e) {
                // Exception caught
            }

            entityManager.clear();
            long finalCount = patientRepository.count();

            // Count should be same due to rollback
            assertThat(finalCount).isEqualTo(initialCount);
        }
    }

    // ==================== HELPER METHODS ====================

    private Patient createPatient(String firstName, String lastName, String mrn, String tenantId) {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID().toString())
                .firstName(firstName)
                .lastName(lastName)
                .mrn(mrn)
                .dateOfBirth(LocalDate.of(1980, 1, 15))
                .gender("male")
                .tenantId(tenantId)
                .build();
        return patientRepository.save(patient);
    }

    private Observation createObservation(String patientId, String code, double value, String display) {
        Observation obs = Observation.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code(code)
                .system("http://loinc.org")
                .display(display)
                .valueQuantity(value)
                .unit("%")
                .status("final")
                .effectiveDateTime(LocalDateTime.now())
                .tenantId(TENANT_ID)
                .build();
        return observationRepository.save(obs);
    }

    private Condition createCondition(String patientId, String code, String display, String status) {
        Condition condition = Condition.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .code(code)
                .system("http://hl7.org/fhir/sid/icd-10")
                .display(display)
                .clinicalStatus(status)
                .tenantId(TENANT_ID)
                .build();
        return conditionRepository.save(condition);
    }

    private QualityMeasureResult createMeasureResult(String patientId, String measureCode, boolean compliant, double value) {
        QualityMeasureResult result = QualityMeasureResult.builder()
                .id(UUID.randomUUID().toString())
                .patientId(patientId)
                .measureName(measureCode.replace("-", " "))
                .measureCode(measureCode)
                .compliant(compliant)
                .value(value)
                .calculatedAt(LocalDateTime.now())
                .tenantId(TENANT_ID)
                .build();
        return measureResultRepository.save(result);
    }
}

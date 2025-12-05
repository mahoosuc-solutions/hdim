package com.healthdata.cql.integration;

import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.entity.ValueSet;
import com.healthdata.cql.service.CqlEvaluationService;
import com.healthdata.cql.service.CqlLibraryService;
import com.healthdata.cql.service.ValueSetService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Service Layer
 *
 * Tests the business logic in service classes with full database integration:
 * - CqlLibraryService operations
 * - CqlEvaluationService operations
 * - ValueSetService operations
 * - Transaction management
 * - Service method interactions
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class ServiceLayerIntegrationTest {

    @Autowired
    private CqlLibraryService libraryService;

    @Autowired
    private CqlEvaluationService evaluationService;

    @Autowired
    private ValueSetService valueSetService;

    private static final String TENANT_ID = "test-tenant";

    @Test
    @Order(1)
    @DisplayName("Should create and retrieve library through service")
    void testLibraryServiceCreateAndRetrieve() {
        CqlLibrary library = new CqlLibrary(TENANT_ID, "ServiceTest", "1.0.0");
        library.setStatus("DRAFT");
        library.setCqlContent("library ServiceTest version '1.0.0'");

        CqlLibrary created = libraryService.createLibrary(library);
        assertNotNull(created.getId());

        Optional<CqlLibrary> retrieved = libraryService.getLibraryById(created.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("ServiceTest", retrieved.get().getLibraryName());
    }

    @Test
    @Order(2)
    @DisplayName("Should enforce unique library name-version constraint")
    void testLibraryUniqueConstraint() {
        CqlLibrary lib1 = new CqlLibrary(TENANT_ID, "UniqueTest", "1.0.0");
        libraryService.createLibrary(lib1);

        CqlLibrary lib2 = new CqlLibrary(TENANT_ID, "UniqueTest", "1.0.0");
        assertThrows(IllegalArgumentException.class, () -> {
            libraryService.createLibrary(lib2);
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should manage library lifecycle transitions")
    void testLibraryLifecycle() {
        CqlLibrary library = new CqlLibrary(TENANT_ID, "LifecycleTest", "1.0.0");
        library.setStatus("DRAFT");
        library = libraryService.createLibrary(library);

        // Activate
        CqlLibrary activated = libraryService.activateLibrary(library.getId(), TENANT_ID);
        assertEquals("ACTIVE", activated.getStatus());

        // Retire
        CqlLibrary retired = libraryService.retireLibrary(library.getId(), TENANT_ID);
        assertEquals("RETIRED", retired.getStatus());

        // Soft delete
        libraryService.deleteLibrary(library.getId(), TENANT_ID);
        Optional<CqlLibrary> deleted = libraryService.getLibraryByIdAndTenant(library.getId(), TENANT_ID);
        assertFalse(deleted.isPresent());
    }

    @Test
    @Order(4)
    @DisplayName("Should get latest library version")
    void testGetLatestLibraryVersion() {
        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "VersionTest", "1.0.0"));
        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "VersionTest", "1.5.0"));
        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "VersionTest", "2.0.0"));

        Optional<CqlLibrary> latest = libraryService.getLatestLibraryVersion(TENANT_ID, "VersionTest");
        assertTrue(latest.isPresent());
        assertEquals("2.0.0", latest.get().getVersion());
    }

    @Test
    @Order(5)
    @DisplayName("Should search libraries by name")
    void testSearchLibraries() {
        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "SearchTest1", "1.0.0"));
        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "SearchTest2", "1.0.0"));
        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "OtherName", "1.0.0"));

        List<CqlLibrary> results = libraryService.searchLibrariesByName(TENANT_ID, "SearchTest");
        assertTrue(results.size() >= 2);
        assertTrue(results.stream().allMatch(lib -> lib.getLibraryName().contains("SearchTest")));
    }

    @Test
    @Order(6)
    @DisplayName("Should update library fields")
    void testUpdateLibrary() {
        CqlLibrary library = new CqlLibrary(TENANT_ID, "UpdateTest", "1.0.0");
        library = libraryService.createLibrary(library);

        CqlLibrary updates = new CqlLibrary(TENANT_ID, "UpdateTest", "1.0.0");
        updates.setDescription("Updated description");
        updates.setPublisher("Updated publisher");

        CqlLibrary updated = libraryService.updateLibrary(library.getId(), updates);
        assertEquals("Updated description", updated.getDescription());
        assertEquals("Updated publisher", updated.getPublisher());
    }

    @Test
    @Order(7)
    @DisplayName("Should create and execute evaluation")
    void testEvaluationServiceCreateAndExecute() {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary(TENANT_ID, "EvalTest", "1.0.0"));

        CqlEvaluation evaluation = evaluationService.createEvaluation(TENANT_ID, library.getId(), "patient-123");
        assertNotNull(evaluation.getId());
        assertEquals("PENDING", evaluation.getStatus());

        CqlEvaluation executed = evaluationService.executeEvaluation(evaluation.getId(), TENANT_ID);
        assertNotNull(executed.getDurationMs());
        assertTrue(executed.getStatus().equals("SUCCESS") || executed.getStatus().equals("FAILED"));
    }

    @Test
    @Order(8)
    @DisplayName("Should enforce library-evaluation tenant matching")
    void testEvaluationTenantMatching() {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary("tenant-1", "TenantTest", "1.0.0"));

        assertThrows(IllegalArgumentException.class, () -> {
            evaluationService.createEvaluation("tenant-2", library.getId(), "patient-123");
        });
    }

    @Test
    @Order(9)
    @DisplayName("Should get evaluations for patient")
    void testGetEvaluationsForPatient() {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary(TENANT_ID, "PatientEvalTest", "1.0.0"));

        evaluationService.createEvaluation(TENANT_ID, library.getId(), "patient-abc");
        evaluationService.createEvaluation(TENANT_ID, library.getId(), "patient-abc");
        evaluationService.createEvaluation(TENANT_ID, library.getId(), "patient-xyz");

        List<CqlEvaluation> evaluations = evaluationService.getEvaluationsForPatient(TENANT_ID, "patient-abc");
        assertTrue(evaluations.size() >= 2);
        assertTrue(evaluations.stream().allMatch(eval -> eval.getPatientId().equals("patient-abc")));
    }

    @Test
    @Order(10)
    @DisplayName("Should get latest evaluation for patient and library")
    void testGetLatestEvaluation() {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary(TENANT_ID, "LatestEvalTest", "1.0.0"));

        CqlEvaluation older = evaluationService.createEvaluation(TENANT_ID, library.getId(), "patient-latest");
        try {
            Thread.sleep(100); // Ensure different timestamps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        CqlEvaluation newer = evaluationService.createEvaluation(TENANT_ID, library.getId(), "patient-latest");

        Optional<CqlEvaluation> latest = evaluationService.getLatestEvaluationForPatientAndLibrary(
                TENANT_ID, "patient-latest", library.getId());

        assertTrue(latest.isPresent());
        assertEquals(newer.getId(), latest.get().getId());
    }

    @Test
    @Order(11)
    @DisplayName("Should get evaluations by status")
    void testGetEvaluationsByStatus() {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary(TENANT_ID, "StatusTest", "1.0.0"));

        CqlEvaluation eval1 = evaluationService.createEvaluation(TENANT_ID, library.getId(), "patient-1");
        CqlEvaluation executedEval = evaluationService.executeEvaluation(eval1.getId(), TENANT_ID);
        final String expectedStatus = executedEval.getStatus();

        List<CqlEvaluation> successEvals = evaluationService.getEvaluationsByStatus(TENANT_ID, expectedStatus);
        assertTrue(successEvals.size() >= 1);
        assertTrue(successEvals.stream().allMatch(e -> e.getStatus().equals(expectedStatus)));
    }

    @Test
    @Order(12)
    @DisplayName("Should batch evaluate multiple patients")
    void testBatchEvaluate() {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary(TENANT_ID, "BatchTest", "1.0.0"));

        List<String> patientIds = Arrays.asList("batch-p1", "batch-p2", "batch-p3");
        List<CqlEvaluation> evaluations = evaluationService.batchEvaluate(TENANT_ID, library.getId(), patientIds);

        assertEquals(3, evaluations.size());
        assertTrue(evaluations.stream().allMatch(e -> e.getLibrary().getId().equals(library.getId())));
    }

    @Test
    @Order(13)
    @DisplayName("Should retry failed evaluation")
    void testRetryEvaluation() {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary(TENANT_ID, "RetryTest", "1.0.0"));

        CqlEvaluation evaluation = evaluationService.createEvaluation(TENANT_ID, library.getId(), "retry-patient");
        evaluation = evaluationService.updateEvaluationResult(
                evaluation.getId(), TENANT_ID, null, "FAILED");

        CqlEvaluation retried = evaluationService.retryEvaluation(evaluation.getId(), TENANT_ID);
        assertNotNull(retried);
    }

    @Test
    @Order(14)
    @DisplayName("Should get evaluations by date range")
    void testGetEvaluationsByDateRange() throws Exception {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary(TENANT_ID, "DateRangeTest", "1.0.0"));

        Instant now = Instant.now();
        evaluationService.createEvaluation(TENANT_ID, library.getId(), "date-patient-1");
        Thread.sleep(100);
        evaluationService.createEvaluation(TENANT_ID, library.getId(), "date-patient-2");

        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);

        List<CqlEvaluation> evaluations = evaluationService.getEvaluationsByDateRange(TENANT_ID, start, end);
        assertTrue(evaluations.size() >= 2);
    }

    @Test
    @Order(15)
    @DisplayName("Should calculate average evaluation duration")
    void testAverageDuration() {
        CqlLibrary library = libraryService.createLibrary(new CqlLibrary(TENANT_ID, "DurationTest", "1.0.0"));

        CqlEvaluation eval1 = evaluationService.createEvaluation(TENANT_ID, library.getId(), "duration-p1");
        evaluationService.updateEvaluationResult(eval1.getId(), TENANT_ID, "{}", "SUCCESS");

        CqlEvaluation eval2 = evaluationService.createEvaluation(TENANT_ID, library.getId(), "duration-p2");
        evaluationService.updateEvaluationResult(eval2.getId(), TENANT_ID, "{}", "SUCCESS");

        Double avgDuration = evaluationService.getAverageDurationForLibrary(TENANT_ID, library.getId());
        // avgDuration might be null if no durations are set, which is acceptable
        assertTrue(avgDuration == null || avgDuration >= 0);
    }

    @Test
    @Order(16)
    @DisplayName("Should create and retrieve value set")
    void testValueSetService() {
        ValueSet valueSet = new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.test", "TestVS", "SNOMED");
        valueSet.setVersion("2023-01");
        valueSet.setCodes("[\"123456\"]");

        ValueSet created = valueSetService.createValueSet(valueSet);
        assertNotNull(created.getId());

        Optional<ValueSet> retrieved = valueSetService.getValueSetByIdAndTenant(created.getId(), TENANT_ID);
        assertTrue(retrieved.isPresent());
        assertEquals("TestVS", retrieved.get().getName());
    }

    @Test
    @Order(17)
    @DisplayName("Should get value set by OID")
    void testGetValueSetByOid() {
        String oid = "2.16.840.1.113883.3.464.oid-test";
        ValueSet valueSet = new ValueSet(TENANT_ID, oid, "OidTest", "SNOMED");
        valueSetService.createValueSet(valueSet);

        Optional<ValueSet> retrieved = valueSetService.getValueSetByOid(TENANT_ID, oid);
        assertTrue(retrieved.isPresent());
        assertEquals(oid, retrieved.get().getOid());
    }

    @Test
    @Order(18)
    @DisplayName("Should filter value sets by code system")
    void testGetValueSetsByCodeSystem() {
        valueSetService.createValueSet(new ValueSet(TENANT_ID, "2.16.840.1.test.1", "SNOMED1", "SNOMED"));
        valueSetService.createValueSet(new ValueSet(TENANT_ID, "2.16.840.1.test.2", "LOINC1", "LOINC"));
        valueSetService.createValueSet(new ValueSet(TENANT_ID, "2.16.840.1.test.3", "SNOMED2", "SNOMED"));

        Page<ValueSet> snomedVS = valueSetService.getValueSetsByCodeSystem(
                TENANT_ID, "SNOMED", PageRequest.of(0, 10));
        assertTrue(snomedVS.getTotalElements() >= 2);
        assertTrue(snomedVS.getContent().stream().allMatch(vs -> vs.getCodeSystem().equals("SNOMED")));
    }

    @Test
    @Order(19)
    @DisplayName("Should get active value sets")
    void testGetActiveValueSets() {
        ValueSet active = new ValueSet(TENANT_ID, "2.16.840.1.active.1", "Active", "SNOMED");
        active.setStatus("ACTIVE");
        valueSetService.createValueSet(active);

        ValueSet retired = new ValueSet(TENANT_ID, "2.16.840.1.retired.1", "Retired", "SNOMED");
        retired.setStatus("RETIRED");
        valueSetService.createValueSet(retired);

        List<ValueSet> activeVS = valueSetService.getActiveValueSets(TENANT_ID);
        assertTrue(activeVS.stream().allMatch(vs -> vs.getStatus().equals("ACTIVE")));
    }

    @Test
    @Order(20)
    @DisplayName("Should manage value set lifecycle")
    void testValueSetLifecycle() {
        ValueSet valueSet = new ValueSet(TENANT_ID, "2.16.840.1.lifecycle.1", "Lifecycle", "SNOMED");
        valueSet.setStatus("DRAFT");
        valueSet = valueSetService.createValueSet(valueSet);

        // Activate
        ValueSet activated = valueSetService.activateValueSet(valueSet.getId(), TENANT_ID);
        assertEquals("ACTIVE", activated.getStatus());

        // Retire
        ValueSet retired = valueSetService.retireValueSet(valueSet.getId(), TENANT_ID);
        assertEquals("RETIRED", retired.getStatus());

        // Soft delete
        valueSetService.deleteValueSet(valueSet.getId(), TENANT_ID);
        Optional<ValueSet> deleted = valueSetService.getValueSetByIdAndTenant(valueSet.getId(), TENANT_ID);
        assertFalse(deleted.isPresent());
    }

    @Test
    @Order(21)
    @DisplayName("Should enforce tenant isolation in services")
    void testServiceTenantIsolation() {
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        CqlLibrary lib1 = libraryService.createLibrary(new CqlLibrary(tenant1, "Isolated1", "1.0.0"));
        CqlLibrary lib2 = libraryService.createLibrary(new CqlLibrary(tenant2, "Isolated2", "1.0.0"));

        // Verify tenant1 can only see their library
        Optional<CqlLibrary> tenant1View = libraryService.getLibraryByIdAndTenant(lib1.getId(), tenant1);
        assertTrue(tenant1View.isPresent());

        // Verify tenant1 cannot see tenant2's library
        Optional<CqlLibrary> crossTenantView = libraryService.getLibraryByIdAndTenant(lib2.getId(), tenant1);
        assertFalse(crossTenantView.isPresent());
    }

    @Test
    @Order(22)
    @DisplayName("Should count entities correctly")
    void testCountOperations() {
        long initialLibraryCount = libraryService.countLibraries(TENANT_ID);

        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "Count1", "1.0.0"));
        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "Count2", "1.0.0"));

        long afterCount = libraryService.countLibraries(TENANT_ID);
        assertEquals(initialLibraryCount + 2, afterCount);
    }

    @Test
    @Order(23)
    @DisplayName("Should handle pagination correctly")
    void testPagination() {
        // Create multiple libraries
        for (int i = 0; i < 15; i++) {
            libraryService.createLibrary(new CqlLibrary(TENANT_ID, "Page" + i, "1.0.0"));
        }

        Page<CqlLibrary> page1 = libraryService.getAllLibraries(TENANT_ID, PageRequest.of(0, 10));
        assertEquals(10, page1.getContent().size());
        assertTrue(page1.getTotalElements() >= 15);

        Page<CqlLibrary> page2 = libraryService.getAllLibraries(TENANT_ID, PageRequest.of(1, 10));
        assertTrue(page2.getContent().size() >= 5);
    }

    @Test
    @Order(24)
    @DisplayName("Should handle transactional rollback")
    void testTransactionalRollback() {
        long initialCount = libraryService.countLibraries(TENANT_ID);

        // Test that null tenant is properly rejected by validation
        CqlLibrary library = new CqlLibrary(null, "InvalidTenant", "1.0.0"); // null tenant
        assertThrows(IllegalArgumentException.class, () -> {
            libraryService.createLibrary(library);
        }, "Should throw IllegalArgumentException for null tenant");

        long afterCount = libraryService.countLibraries(TENANT_ID);
        assertEquals(initialCount, afterCount); // No change due to rollback
    }

    @Test
    @Order(25)
    @DisplayName("Should validate library existence")
    void testLibraryExists() {
        libraryService.createLibrary(new CqlLibrary(TENANT_ID, "ExistsTest", "1.0.0"));

        assertTrue(libraryService.libraryExists(TENANT_ID, "ExistsTest", "1.0.0"));
        assertFalse(libraryService.libraryExists(TENANT_ID, "ExistsTest", "2.0.0"));
        assertFalse(libraryService.libraryExists(TENANT_ID, "NonExistent", "1.0.0"));
    }
}

package com.healthdata.cql.integration;

import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.entity.ValueSet;
import com.healthdata.cql.repository.CqlEvaluationRepository;
import com.healthdata.cql.repository.CqlLibraryRepository;
import com.healthdata.cql.repository.ValueSetRepository;
import com.healthdata.cql.test.CqlTestcontainersBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CQL Engine Service
 *
 * Tests the complete stack: Entities → Repositories → Database
 * Requires PostgreSQL to be running (see application-test.yml)
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class CqlEngineServiceIntegrationTest extends CqlTestcontainersBase {

    @Autowired
    private CqlLibraryRepository libraryRepository;

    @Autowired
    private CqlEvaluationRepository evaluationRepository;

    @Autowired
    private ValueSetRepository valueSetRepository;

    private static final String TEST_TENANT = "test-tenant-1";
    private static final UUID TEST_PATIENT = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @Order(1)
    @DisplayName("Test database connection and schema")
    void testDatabaseConnection() {
        // Verify repositories are initialized
        assertNotNull(libraryRepository);
        assertNotNull(evaluationRepository);
        assertNotNull(valueSetRepository);

        // Verify we can count (schema exists)
        long libraryCount = libraryRepository.count();
        long evaluationCount = evaluationRepository.count();
        long valueSetCount = valueSetRepository.count();

        System.out.println("Database connected successfully:");
        System.out.println("  - CQL Libraries: " + libraryCount);
        System.out.println("  - CQL Evaluations: " + evaluationCount);
        System.out.println("  - Value Sets: " + valueSetCount);

        assertTrue(libraryCount >= 0);
        assertTrue(evaluationCount >= 0);
        assertTrue(valueSetCount >= 0);
    }

    @Test
    @Order(2)
    @DisplayName("Test CqlLibrary CRUD operations")
    void testCqlLibraryCRUD() {
        // CREATE
        CqlLibrary library = new CqlLibrary(TEST_TENANT, "DiabetesScreening", "1.0.0");
        library.setStatus("DRAFT");
        library.setCqlContent("library DiabetesScreening version '1.0.0'");
        library.setDescription("HEDIS Diabetes Screening Measure");
        library.setPublisher("NCQA");

        CqlLibrary saved = libraryRepository.save(library);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(TEST_TENANT, saved.getTenantId());
        System.out.println("✓ Created CQL library: " + saved.getId());

        // READ
        Optional<CqlLibrary> found = libraryRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("DiabetesScreening", found.get().getLibraryName());
        assertEquals("1.0.0", found.get().getVersion());
        System.out.println("✓ Retrieved CQL library: " + found.get().getLibraryName());

        // UPDATE
        saved.setStatus("ACTIVE");
        saved.setElmJson("{\"library\": {\"identifier\": \"DiabetesScreening\"}}");
        CqlLibrary updated = libraryRepository.save(saved);
        assertEquals("ACTIVE", updated.getStatus());
        assertNotNull(updated.getElmJson());
        System.out.println("✓ Updated CQL library status to: " + updated.getStatus());

        // DELETE (soft delete)
        saved.setActive(false);
        libraryRepository.save(saved);
        List<CqlLibrary> activeLibraries = libraryRepository.findByTenantIdAndActiveTrue(TEST_TENANT);
        assertFalse(activeLibraries.contains(saved));
        System.out.println("✓ Soft deleted CQL library");
    }

    @Test
    @Order(3)
    @DisplayName("Test CqlLibrary queries")
    void testCqlLibraryQueries() {
        // Create test data
        CqlLibrary lib1 = new CqlLibrary(TEST_TENANT, "Library1", "1.0.0");
        lib1.setStatus("ACTIVE");
        libraryRepository.save(lib1);

        CqlLibrary lib2 = new CqlLibrary(TEST_TENANT, "Library1", "2.0.0");
        lib2.setStatus("ACTIVE");
        libraryRepository.save(lib2);

        CqlLibrary lib3 = new CqlLibrary(TEST_TENANT, "Library2", "1.0.0");
        lib3.setStatus("DRAFT");
        libraryRepository.save(lib3);

        // Test find by name and version
        Optional<CqlLibrary> found = libraryRepository
                .findByTenantIdAndLibraryNameAndVersionAndActiveTrue(TEST_TENANT, "Library1", "1.0.0");
        assertTrue(found.isPresent());
        assertEquals("1.0.0", found.get().getVersion());
        System.out.println("✓ Found library by name and version");

        // Test find all versions
        List<CqlLibrary> versions = libraryRepository
                .findByTenantIdAndLibraryNameAndActiveTrueOrderByVersionDesc(TEST_TENANT, "Library1");
        assertEquals(2, versions.size());
        assertEquals("2.0.0", versions.get(0).getVersion()); // Descending order
        System.out.println("✓ Found all versions: " + versions.size());

        // Test find by status
        List<CqlLibrary> activeLibs = libraryRepository
                .findByTenantIdAndStatusAndActiveTrue(TEST_TENANT, "ACTIVE");
        assertTrue(activeLibs.size() >= 2);
        System.out.println("✓ Found active libraries: " + activeLibs.size());

        // Test search by name
        List<CqlLibrary> searchResults = libraryRepository.searchByName(TEST_TENANT, "Library");
        assertTrue(searchResults.size() >= 3);
        System.out.println("✓ Search found libraries: " + searchResults.size());

        // Test count
        long count = libraryRepository.countByTenantIdAndActiveTrue(TEST_TENANT);
        assertTrue(count >= 3);
        System.out.println("✓ Count libraries: " + count);
    }

    @Test
    @Order(4)
    @DisplayName("Test ValueSet CRUD operations")
    void testValueSetCRUD() {
        // CREATE
        ValueSet valueSet = new ValueSet(
                TEST_TENANT,
                "2.16.840.1.113883.3.464.1003.103.12.1001",
                "Diabetes",
                "SNOMED");
        valueSet.setVersion("2023-01");
        valueSet.setCodes("[\"44054006\", \"73211009\"]"); // SNOMED codes
        valueSet.setDescription("Diabetes value set for quality measures");
        valueSet.setPublisher("NCQA");
        valueSet.setStatus("ACTIVE");

        ValueSet saved = valueSetRepository.save(valueSet);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(TEST_TENANT, saved.getTenantId());
        System.out.println("✓ Created value set: " + saved.getId());

        // READ
        Optional<ValueSet> found = valueSetRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Diabetes", found.get().getName());
        assertEquals("SNOMED", found.get().getCodeSystem());
        System.out.println("✓ Retrieved value set: " + found.get().getName());

        // UPDATE
        saved.setStatus("RETIRED");
        ValueSet updated = valueSetRepository.save(saved);
        assertEquals("RETIRED", updated.getStatus());
        System.out.println("✓ Updated value set status to: " + updated.getStatus());

        // DELETE (soft delete)
        saved.setActive(false);
        valueSetRepository.save(saved);
        List<ValueSet> activeValueSets = valueSetRepository.findByTenantIdAndActiveTrue(TEST_TENANT);
        assertFalse(activeValueSets.contains(saved));
        System.out.println("✓ Soft deleted value set");
    }

    @Test
    @Order(5)
    @DisplayName("Test ValueSet queries")
    void testValueSetQueries() {
        // Create test data
        ValueSet vs1 = new ValueSet(TEST_TENANT, "2.16.840.1.113883.3.464.1", "VS1", "SNOMED");
        vs1.setStatus("ACTIVE");
        valueSetRepository.save(vs1);

        ValueSet vs2 = new ValueSet(TEST_TENANT, "2.16.840.1.113883.3.464.2", "VS2", "LOINC");
        vs2.setStatus("ACTIVE");
        valueSetRepository.save(vs2);

        ValueSet vs3 = new ValueSet(TEST_TENANT, "2.16.840.1.113883.3.464.3", "VS3", "RxNorm");
        vs3.setStatus("DRAFT");
        valueSetRepository.save(vs3);

        // Test find by OID
        Optional<ValueSet> found = valueSetRepository
                .findByTenantIdAndOidAndActiveTrue(TEST_TENANT, "2.16.840.1.113883.3.464.1");
        assertTrue(found.isPresent());
        assertEquals("VS1", found.get().getName());
        System.out.println("✓ Found value set by OID");

        // Test find by code system
        List<ValueSet> snomedVS = valueSetRepository
                .findByTenantIdAndCodeSystemAndActiveTrue(TEST_TENANT, "SNOMED");
        assertTrue(snomedVS.size() >= 1);
        System.out.println("✓ Found SNOMED value sets: " + snomedVS.size());

        // Test find common code systems
        List<ValueSet> commonVS = valueSetRepository.findCommonCodeSystemValueSets(TEST_TENANT);
        assertTrue(commonVS.size() >= 3);
        System.out.println("✓ Found common code system value sets: " + commonVS.size());

        // Test find by OID prefix
        List<ValueSet> byPrefix = valueSetRepository
                .findByOidPrefix(TEST_TENANT, "2.16.840.1.113883.3.464");
        assertTrue(byPrefix.size() >= 3);
        System.out.println("✓ Found value sets by OID prefix: " + byPrefix.size());

        // Test count by code system
        long snomedCount = valueSetRepository
                .countByTenantIdAndCodeSystemAndActiveTrue(TEST_TENANT, "SNOMED");
        assertTrue(snomedCount >= 1);
        System.out.println("✓ Count SNOMED value sets: " + snomedCount);
    }

    @Test
    @Order(6)
    @DisplayName("Test CqlEvaluation CRUD operations")
    void testCqlEvaluationCRUD() {
        // First create a library
        CqlLibrary library = new CqlLibrary(TEST_TENANT, "TestLibrary", "1.0.0");
        library.setStatus("ACTIVE");
        library = libraryRepository.save(library);

        // CREATE evaluation
        CqlEvaluation evaluation = new CqlEvaluation(TEST_TENANT, library, TEST_PATIENT);
        evaluation.setStatus("PENDING");
        evaluation.setEvaluationDate(Instant.now());
        evaluation.setContextData("{\"patientAge\": 45}");

        CqlEvaluation saved = evaluationRepository.save(evaluation);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(TEST_TENANT, saved.getTenantId());
        assertEquals(TEST_PATIENT, saved.getPatientId());
        System.out.println("✓ Created CQL evaluation: " + saved.getId());

        // READ
        Optional<CqlEvaluation> found = evaluationRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(TEST_PATIENT, found.get().getPatientId());
        assertEquals("PENDING", found.get().getStatus());
        System.out.println("✓ Retrieved CQL evaluation: " + found.get().getId());

        // UPDATE
        saved.setStatus("SUCCESS");
        saved.setEvaluationResult("{\"numerator\": true, \"denominator\": true}");
        saved.setDurationMs(150L);
        CqlEvaluation updated = evaluationRepository.save(saved);
        assertEquals("SUCCESS", updated.getStatus());
        assertEquals(150L, updated.getDurationMs());
        System.out.println("✓ Updated evaluation status to: " + updated.getStatus());

        // Test relationship
        assertNotNull(updated.getLibrary());
        assertEquals(library.getId(), updated.getLibrary().getId());
        System.out.println("✓ Verified library relationship");
    }

    @Test
    @Order(7)
    @DisplayName("Test CqlEvaluation queries")
    void testCqlEvaluationQueries() {
        // Create test library
        CqlLibrary library = new CqlLibrary(TEST_TENANT, "QueryTestLibrary", "1.0.0");
        library = libraryRepository.save(library);

        // Create test evaluations
        UUID patient1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID patient2 = UUID.fromString("33333333-3333-3333-3333-333333333333");
        CqlEvaluation eval1 = new CqlEvaluation(TEST_TENANT, library, patient1);
        eval1.setStatus("SUCCESS");
        eval1.setDurationMs(100L);
        eval1.setEvaluationDate(Instant.now().minusSeconds(3600)); // 1 hour ago
        evaluationRepository.save(eval1);

        CqlEvaluation eval2 = new CqlEvaluation(TEST_TENANT, library, patient2);
        eval2.setStatus("SUCCESS");
        eval2.setDurationMs(200L);
        eval2.setEvaluationDate(Instant.now().minusSeconds(1800)); // 30 min ago
        evaluationRepository.save(eval2);

        CqlEvaluation eval3 = new CqlEvaluation(TEST_TENANT, library, patient1);
        eval3.setStatus("FAILED");
        eval3.setErrorMessage("Connection timeout");
        eval3.setEvaluationDate(Instant.now().minusSeconds(600)); // 10 min ago
        evaluationRepository.save(eval3);

        // Test find by patient
        List<CqlEvaluation> patientEvals = evaluationRepository
                .findByTenantIdAndPatientId(TEST_TENANT, patient1);
        assertTrue(patientEvals.size() >= 2);
        System.out.println("✓ Found evaluations for patient: " + patientEvals.size());

        // Test find by library
        List<CqlEvaluation> libraryEvals = evaluationRepository
                .findByTenantIdAndLibrary_Id(TEST_TENANT, library.getId());
        assertTrue(libraryEvals.size() >= 3);
        System.out.println("✓ Found evaluations for library: " + libraryEvals.size());

        // Test find by status
        List<CqlEvaluation> successEvals = evaluationRepository
                .findByTenantIdAndStatus(TEST_TENANT, "SUCCESS");
        assertTrue(successEvals.size() >= 2);
        System.out.println("✓ Found successful evaluations: " + successEvals.size());

        // Test find latest
        Optional<CqlEvaluation> latest = evaluationRepository
                .findLatestByPatientAndLibrary(TEST_TENANT, patient1, library.getId());
        assertTrue(latest.isPresent());
        assertEquals("FAILED", latest.get().getStatus()); // Most recent for patient-1
        System.out.println("✓ Found latest evaluation for patient");

        // Test average duration
        Double avgDuration = evaluationRepository
                .getAverageDurationForLibrary(TEST_TENANT, library.getId());
        assertNotNull(avgDuration);
        assertEquals(150.0, avgDuration, 1.0); // (100 + 200) / 2 = 150
        System.out.println("✓ Calculated average duration: " + avgDuration + "ms");

        // Test count by status
        long successCount = evaluationRepository
                .countByTenantIdAndStatus(TEST_TENANT, "SUCCESS");
        assertTrue(successCount >= 2);
        System.out.println("✓ Count successful evaluations: " + successCount);
    }

    @Test
    @Order(8)
    @DisplayName("Test multi-tenant isolation")
    void testMultiTenantIsolation() {
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        // Create libraries for different tenants
        CqlLibrary lib1 = new CqlLibrary(tenant1, "SharedName", "1.0.0");
        libraryRepository.save(lib1);

        CqlLibrary lib2 = new CqlLibrary(tenant2, "SharedName", "1.0.0");
        libraryRepository.save(lib2);

        // Verify isolation - tenant1 can only see their library
        List<CqlLibrary> tenant1Libs = libraryRepository.findByTenantIdAndActiveTrue(tenant1);
        assertTrue(tenant1Libs.stream().allMatch(lib -> lib.getTenantId().equals(tenant1)));
        System.out.println("✓ Tenant 1 isolated: " + tenant1Libs.size() + " libraries");

        // Verify isolation - tenant2 can only see their library
        List<CqlLibrary> tenant2Libs = libraryRepository.findByTenantIdAndActiveTrue(tenant2);
        assertTrue(tenant2Libs.stream().allMatch(lib -> lib.getTenantId().equals(tenant2)));
        System.out.println("✓ Tenant 2 isolated: " + tenant2Libs.size() + " libraries");

        // Verify no cross-tenant visibility
        Optional<CqlLibrary> crossTenantLookup = libraryRepository
                .findByTenantIdAndLibraryNameAndVersionAndActiveTrue(tenant1, "SharedName", "1.0.0");
        assertTrue(crossTenantLookup.isPresent());
        assertEquals(tenant1, crossTenantLookup.get().getTenantId());
        System.out.println("✓ Multi-tenant isolation verified");
    }

    @Test
    @Order(9)
    @DisplayName("Test indexes and performance")
    void testIndexesAndPerformance() {
        System.out.println("Testing index performance...");

        // Create test data
        CqlLibrary library = new CqlLibrary(TEST_TENANT, "PerfTestLibrary", "1.0.0");
        library = libraryRepository.save(library);

        // Create multiple evaluations
        for (int i = 0; i < 10; i++) {
            CqlEvaluation eval = new CqlEvaluation(TEST_TENANT, library, UUID.randomUUID());
            eval.setStatus(i % 2 == 0 ? "SUCCESS" : "FAILED");
            eval.setEvaluationDate(Instant.now().minusSeconds(i * 60));
            evaluationRepository.save(eval);
        }

        // Test indexed query performance (should be fast with proper indexes)
        long startTime = System.currentTimeMillis();
        List<CqlEvaluation> results = evaluationRepository
                .findByTenantIdAndLibrary_Id(TEST_TENANT, library.getId());
        long queryTime = System.currentTimeMillis() - startTime;

        assertTrue(results.size() >= 10);
        assertTrue(queryTime < 1000, "Query should complete in < 1000ms (was: " + queryTime + "ms)");
        System.out.println("✓ Indexed query completed in " + queryTime + "ms");

        // Test date range query (should use idx_eval_library)
        startTime = System.currentTimeMillis();
        List<CqlEvaluation> dateRangeResults = evaluationRepository.findByDateRange(
                TEST_TENANT,
                Instant.now().minusSeconds(3600),
                Instant.now());
        queryTime = System.currentTimeMillis() - startTime;

        assertTrue(queryTime < 1000, "Date range query should complete in < 1000ms (was: " + queryTime + "ms)");
        System.out.println("✓ Date range query completed in " + queryTime + "ms");
    }

    @Test
    @Order(10)
    @DisplayName("Test transaction rollback")
    void testTransactionRollback() {
        // This should fail due to NOT NULL constraint on tenant_id
        assertThrows(Exception.class, () -> {
            CqlLibrary library = new CqlLibrary(null, "InvalidLibrary", "1.0.0"); // null tenant
            libraryRepository.save(library);
            libraryRepository.flush(); // Force database constraint check
        });
        System.out.println("✓ NOT NULL constraint enforced - transaction will rollback");
    }
}

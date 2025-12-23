package com.healthdata.cql.integration;

import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.entity.ValueSet;
import com.healthdata.cql.repository.CqlEvaluationRepository;
import com.healthdata.cql.repository.CqlLibraryRepository;
import com.healthdata.cql.repository.ValueSetRepository;
import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.test.CqlTestcontainersBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Performance
 *
 * Tests system performance, query optimization, and scalability:
 * - Bulk operations
 * - Large dataset handling
 * - Query response times
 * - Index effectiveness
 * - Pagination performance
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class PerformanceIntegrationTest extends CqlTestcontainersBase {

    @Autowired
    private CqlLibraryRepository libraryRepository;

    @Autowired
    private CqlEvaluationRepository evaluationRepository;

    @Autowired
    private ValueSetRepository valueSetRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final int BULK_SIZE = 100;

    @Test
    @Order(1)
    @DisplayName("Should handle bulk library creation efficiently")
    void testBulkLibraryCreation() {
        long startTime = System.currentTimeMillis();

        List<CqlLibrary> libraries = new ArrayList<>();
        for (int i = 0; i < BULK_SIZE; i++) {
            CqlLibrary library = new CqlLibrary(TENANT_ID, "BulkLib" + i, "1.0.0");
            library.setStatus("ACTIVE");
            libraries.add(library);
        }

        libraryRepository.saveAll(libraries);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 5000, "Bulk insert of " + BULK_SIZE + " libraries took too long: " + duration + "ms");
        System.out.println("Bulk created " + BULK_SIZE + " libraries in " + duration + "ms");
    }

    @Test
    @Order(2)
    @DisplayName("Should query libraries efficiently with indexes")
    void testLibraryQueryPerformance() {
        // Create test data
        for (int i = 0; i < 50; i++) {
            CqlLibrary library = new CqlLibrary(TENANT_ID, "QueryTest" + i, "1.0.0");
            library.setStatus("ACTIVE");
            libraryRepository.save(library);
        }

        // Test indexed query
        long startTime = System.currentTimeMillis();
        List<CqlLibrary> results = libraryRepository.findByTenantIdAndActiveTrue(TENANT_ID);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(results.size() >= 50);
        assertTrue(duration < 5000, "Query took too long: " + duration + "ms");
        System.out.println("Queried " + results.size() + " libraries in " + duration + "ms");
    }

    @Test
    @Order(3)
    @DisplayName("Should handle large paginated result sets efficiently")
    void testPaginationPerformance() {
        // Create large dataset
        for (int i = 0; i < 200; i++) {
            libraryRepository.save(new CqlLibrary(TENANT_ID, "PageTest" + i, "1.0.0"));
        }

        long startTime = System.currentTimeMillis();
        Page<CqlLibrary> page = libraryRepository.findByTenantIdAndActiveTrue(
                TENANT_ID, PageRequest.of(0, 20));
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(20, page.getContent().size());
        assertTrue(page.getTotalElements() >= 200);
        assertTrue(duration < 5000, "Pagination query took too long: " + duration + "ms");
        System.out.println("Paginated query (20 of " + page.getTotalElements() + ") in " + duration + "ms");
    }

    @Test
    @Order(4)
    @DisplayName("Should handle bulk evaluation creation efficiently")
    void testBulkEvaluationCreation() {
        CqlLibrary library = new CqlLibrary(TENANT_ID, "BulkEvalLib", "1.0.0");
        library = libraryRepository.save(library);

        long startTime = System.currentTimeMillis();

        List<CqlEvaluation> evaluations = new ArrayList<>();
        for (int i = 0; i < BULK_SIZE; i++) {
            CqlEvaluation evaluation = new CqlEvaluation(TENANT_ID, library, UUID.randomUUID());
            evaluation.setStatus("PENDING");
            evaluation.setEvaluationDate(Instant.now());
            evaluations.add(evaluation);
        }

        evaluationRepository.saveAll(evaluations);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 5000, "Bulk insert of " + BULK_SIZE + " evaluations took too long: " + duration + "ms");
        System.out.println("Bulk created " + BULK_SIZE + " evaluations in " + duration + "ms");
    }

    @Test
    @Order(5)
    @DisplayName("Should query evaluations by patient efficiently")
    void testEvaluationQueryByPatient() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "QueryEvalLib", "1.0.0"));

        UUID patientId = UUID.randomUUID();
        // Create evaluations for same patient
        for (int i = 0; i < 30; i++) {
            CqlEvaluation eval = new CqlEvaluation(TENANT_ID, library, patientId);
            eval.setEvaluationDate(Instant.now().minusSeconds(i * 60));
            evaluationRepository.save(eval);
        }

        long startTime = System.currentTimeMillis();
        List<CqlEvaluation> results = evaluationRepository.findByTenantIdAndPatientId(
                TENANT_ID, patientId);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(results.size() >= 30);
        assertTrue(duration < 5000, "Patient query took too long: " + duration + "ms");
        System.out.println("Queried " + results.size() + " evaluations for patient in " + duration + "ms");
    }

    @Test
    @Order(6)
    @DisplayName("Should query evaluations by library efficiently")
    void testEvaluationQueryByLibrary() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "LibQueryTest", "1.0.0"));

        // Create evaluations for library
        for (int i = 0; i < 40; i++) {
            CqlEvaluation eval = new CqlEvaluation(TENANT_ID, library, UUID.randomUUID());
            evaluationRepository.save(eval);
        }

        long startTime = System.currentTimeMillis();
        List<CqlEvaluation> results = evaluationRepository.findByTenantIdAndLibrary_Id(
                TENANT_ID, library.getId());
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(results.size() >= 40);
        assertTrue(duration < 1000, "Library query took too long: " + duration + "ms");
        System.out.println("Queried " + results.size() + " evaluations for library in " + duration + "ms");
    }

    @Test
    @Order(7)
    @DisplayName("Should handle date range queries efficiently")
    void testDateRangeQueryPerformance() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "DateRangeLib", "1.0.0"));

        Instant now = Instant.now();
        for (int i = 0; i < 60; i++) {
            CqlEvaluation eval = new CqlEvaluation(TENANT_ID, library, UUID.randomUUID());
            eval.setEvaluationDate(now.minusSeconds(i * 3600)); // Hours apart
            evaluationRepository.save(eval);
        }

        long startTime = System.currentTimeMillis();
        List<CqlEvaluation> results = evaluationRepository.findByDateRange(
                TENANT_ID,
                now.minusSeconds(24 * 3600),
                now);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(results.size() >= 24);
        assertTrue(duration < 5000, "Date range query took too long: " + duration + "ms");
        System.out.println("Date range query returned " + results.size() + " results in " + duration + "ms");
    }

    @Test
    @Order(8)
    @DisplayName("Should handle bulk value set creation efficiently")
    void testBulkValueSetCreation() {
        long startTime = System.currentTimeMillis();

        List<ValueSet> valueSets = new ArrayList<>();
        for (int i = 0; i < BULK_SIZE; i++) {
            ValueSet vs = new ValueSet(TENANT_ID, "2.16.840.1.bulk." + i, "BulkVS" + i, "SNOMED");
            valueSets.add(vs);
        }

        valueSetRepository.saveAll(valueSets);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 5000, "Bulk insert of " + BULK_SIZE + " value sets took too long: " + duration + "ms");
        System.out.println("Bulk created " + BULK_SIZE + " value sets in " + duration + "ms");
    }

    @Test
    @Order(9)
    @DisplayName("Should query value sets by code system efficiently")
    void testValueSetCodeSystemQuery() {
        // Create value sets for different code systems
        for (int i = 0; i < 30; i++) {
            valueSetRepository.save(new ValueSet(TENANT_ID, "2.16.840.1.snomed." + i, "SNOMED" + i, "SNOMED"));
        }
        for (int i = 0; i < 20; i++) {
            valueSetRepository.save(new ValueSet(TENANT_ID, "2.16.840.1.loinc." + i, "LOINC" + i, "LOINC"));
        }

        long startTime = System.currentTimeMillis();
        List<ValueSet> snomedVS = valueSetRepository.findByTenantIdAndCodeSystemAndActiveTrue(
                TENANT_ID, "SNOMED");
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(snomedVS.size() >= 30);
        assertTrue(duration < 5000, "Code system query took too long: " + duration + "ms");
        System.out.println("Queried " + snomedVS.size() + " SNOMED value sets in " + duration + "ms");
    }

    @Test
    @Order(10)
    @DisplayName("Should handle concurrent reads efficiently")
    void testConcurrentReads() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "ConcurrentReadTest", "1.0.0"));
        UUID libraryId = library.getId();

        long startTime = System.currentTimeMillis();

        // Simulate concurrent reads
        for (int i = 0; i < 20; i++) {
            libraryRepository.findById(libraryId);
        }

        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 2000, "20 concurrent reads took too long: " + duration + "ms");
        System.out.println("Performed 20 concurrent reads in " + duration + "ms");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle large text fields efficiently")
    void testLargeTextFields() {
        String largeCqlContent = "A".repeat(10000); // 10KB of text
        String largeElmJson = "{\"content\":\"" + "X".repeat(50000) + "\"}"; // 50KB

        long startTime = System.currentTimeMillis();

        CqlLibrary library = new CqlLibrary(TENANT_ID, "LargeTextLib", "1.0.0");
        library.setCqlContent(largeCqlContent);
        library.setElmJson(largeElmJson);
        library = libraryRepository.save(library);

        CqlLibrary retrieved = libraryRepository.findById(library.getId()).orElse(null);
        long duration = System.currentTimeMillis() - startTime;

        assertNotNull(retrieved);
        assertEquals(10000, retrieved.getCqlContent().length());
        assertTrue(duration < 5000, "Large text field operations took too long: " + duration + "ms");
        System.out.println("Saved and retrieved large text fields in " + duration + "ms");
    }

    @Test
    @Order(12)
    @DisplayName("Should handle count operations efficiently")
    void testCountPerformance() {
        // Create test data
        for (int i = 0; i < 100; i++) {
            libraryRepository.save(new CqlLibrary(TENANT_ID, "CountTest" + i, "1.0.0"));
        }

        long startTime = System.currentTimeMillis();
        long count = libraryRepository.countByTenantIdAndActiveTrue(TENANT_ID);
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(count >= 100);
        assertTrue(duration < 5000, "Count operation took too long: " + duration + "ms");
        System.out.println("Counted " + count + " libraries in " + duration + "ms");
    }

    @Test
    @Order(13)
    @DisplayName("Should handle search operations efficiently")
    void testSearchPerformance() {
        // Create libraries with searchable names
        for (int i = 0; i < 50; i++) {
            libraryRepository.save(new CqlLibrary(TENANT_ID, "SearchableLibrary" + i, "1.0.0"));
        }

        long startTime = System.currentTimeMillis();
        List<CqlLibrary> results = libraryRepository.searchByName(TENANT_ID, "Searchable");
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(results.size() >= 50);
        assertTrue(duration < 5000, "Search operation took too long: " + duration + "ms");
        System.out.println("Search found " + results.size() + " results in " + duration + "ms");
    }

    @Test
    @Order(14)
    @DisplayName("Should handle bulk updates efficiently")
    void testBulkUpdatePerformance() {
        List<CqlLibrary> libraries = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            libraries.add(libraryRepository.save(new CqlLibrary(TENANT_ID, "UpdateTest" + i, "1.0.0")));
        }

        long startTime = System.currentTimeMillis();

        // Update all libraries
        for (CqlLibrary library : libraries) {
            library.setStatus("RETIRED");
        }
        libraryRepository.saveAll(libraries);

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 3000, "Bulk update of 50 libraries took too long: " + duration + "ms");
        System.out.println("Bulk updated 50 libraries in " + duration + "ms");
    }

    @Test
    @Order(15)
    @DisplayName("Should handle complex join queries efficiently")
    void testComplexJoinQueryPerformance() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "JoinTest", "1.0.0"));

        // Create evaluations (which join with library)
        for (int i = 0; i < 30; i++) {
            CqlEvaluation eval = new CqlEvaluation(TENANT_ID, library, UUID.randomUUID());
            eval.setStatus("SUCCESS");
            evaluationRepository.save(eval);
        }

        long startTime = System.currentTimeMillis();
        List<CqlEvaluation> results = evaluationRepository.findByTenantIdAndStatus(TENANT_ID, "SUCCESS");
        long duration = System.currentTimeMillis() - startTime;

        // Access the joined library to ensure it's loaded
        for (CqlEvaluation eval : results) {
            assertNotNull(eval.getLibrary());
        }

        assertTrue(duration < 2000, "Complex join query took too long: " + duration + "ms");
        System.out.println("Join query with " + results.size() + " results in " + duration + "ms");
    }

    @Test
    @Order(16)
    @DisplayName("Should handle soft delete efficiently")
    void testSoftDeletePerformance() {
        List<CqlLibrary> libraries = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            libraries.add(libraryRepository.save(new CqlLibrary(TENANT_ID, "DeleteTest" + i, "1.0.0")));
        }

        long startTime = System.currentTimeMillis();

        // Soft delete all
        for (CqlLibrary library : libraries) {
            library.setActive(false);
        }
        libraryRepository.saveAll(libraries);

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 2000, "Soft delete of 30 libraries took too long: " + duration + "ms");
        System.out.println("Soft deleted 30 libraries in " + duration + "ms");
    }

    @Test
    @Order(17)
    @DisplayName("Should maintain performance with deep pagination")
    void testDeepPaginationPerformance() {
        // Create large dataset
        for (int i = 0; i < 150; i++) {
            libraryRepository.save(new CqlLibrary(TENANT_ID, "DeepPage" + i, "1.0.0"));
        }

        long startTime = System.currentTimeMillis();
        Page<CqlLibrary> deepPage = libraryRepository.findByTenantIdAndActiveTrue(
                TENANT_ID, PageRequest.of(7, 20)); // Page 7 (140-160)
        long duration = System.currentTimeMillis() - startTime;

        assertFalse(deepPage.getContent().isEmpty());
        assertTrue(duration < 1500, "Deep pagination query took too long: " + duration + "ms");
        System.out.println("Deep pagination (page 7) completed in " + duration + "ms");
    }
}

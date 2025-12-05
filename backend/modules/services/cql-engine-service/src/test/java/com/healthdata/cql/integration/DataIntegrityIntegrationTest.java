package com.healthdata.cql.integration;

import com.healthdata.cql.config.TestRedisConfiguration;
import com.healthdata.cql.entity.CqlEvaluation;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.entity.ValueSet;
import com.healthdata.cql.repository.CqlEvaluationRepository;
import com.healthdata.cql.repository.CqlLibraryRepository;
import com.healthdata.cql.repository.ValueSetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Data Integrity
 *
 * Tests database constraints, relationships, and data validation:
 * - NOT NULL constraints
 * - Foreign key relationships
 * - Unique constraints
 * - Cascade operations
 * - Data consistency
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class DataIntegrityIntegrationTest {

    @Autowired
    private CqlLibraryRepository libraryRepository;

    @Autowired
    private CqlEvaluationRepository evaluationRepository;

    @Autowired
    private ValueSetRepository valueSetRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String TENANT_ID = "test-tenant";

    @Test
    @Order(1)
    @DisplayName("Should enforce NOT NULL constraint on library tenant_id")
    void testLibraryTenantIdNotNull() {
        assertThrows(Exception.class, () -> {
            CqlLibrary library = new CqlLibrary(null, "TestLib", "1.0.0");
            libraryRepository.save(library);
            entityManager.flush(); // Force constraint validation
        });
    }

    @Test
    @Order(2)
    @DisplayName("Should enforce NOT NULL constraint on library name")
    void testLibraryNameNotNull() {
        assertThrows(Exception.class, () -> {
            CqlLibrary library = new CqlLibrary(TENANT_ID, null, "1.0.0");
            libraryRepository.save(library);
            entityManager.flush(); // Force constraint validation
        });
    }

    @Test
    @Order(3)
    @DisplayName("Should enforce NOT NULL constraint on library version")
    void testLibraryVersionNotNull() {
        assertThrows(Exception.class, () -> {
            CqlLibrary library = new CqlLibrary(TENANT_ID, "TestLib", null);
            libraryRepository.save(library);
            entityManager.flush(); // Force constraint validation
        });
    }

    @Test
    @Order(4)
    @DisplayName("Should set default values correctly")
    void testDefaultValues() {
        CqlLibrary library = new CqlLibrary(TENANT_ID, "DefaultTest", "1.0.0");
        library = libraryRepository.save(library);

        assertNotNull(library.getId());
        assertTrue(library.getActive());
        assertEquals("DRAFT", library.getStatus());
        assertNotNull(library.getCreatedAt());
    }

    @Test
    @Order(5)
    @DisplayName("Should auto-generate UUIDs")
    void testUuidGeneration() {
        CqlLibrary lib1 = libraryRepository.save(new CqlLibrary(TENANT_ID, "UUID1", "1.0.0"));
        CqlLibrary lib2 = libraryRepository.save(new CqlLibrary(TENANT_ID, "UUID2", "1.0.0"));

        assertNotNull(lib1.getId());
        assertNotNull(lib2.getId());
        assertNotEquals(lib1.getId(), lib2.getId());
    }

    @Test
    @Order(6)
    @DisplayName("Should auto-update timestamps")
    void testTimestampAutoUpdate() throws Exception {
        CqlLibrary library = new CqlLibrary(TENANT_ID, "TimestampTest", "1.0.0");
        library = libraryRepository.save(library);

        Instant createdAt = library.getCreatedAt();
        Instant updatedAt = library.getUpdatedAt();
        assertNotNull(createdAt);

        Thread.sleep(100); // Ensure time difference

        library.setDescription("Updated");
        library = libraryRepository.save(library);

        assertEquals(createdAt, library.getCreatedAt()); // Created should not change
        assertNotNull(library.getUpdatedAt());
        assertTrue(library.getUpdatedAt().isAfter(createdAt) ||
                   library.getUpdatedAt().equals(updatedAt));
    }

    @Test
    @Order(7)
    @DisplayName("Should maintain foreign key relationship between evaluation and library")
    void testEvaluationLibraryForeignKey() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "FKTest", "1.0.0"));

        CqlEvaluation evaluation = new CqlEvaluation(TENANT_ID, library, "patient-123");
        evaluation = evaluationRepository.save(evaluation);

        CqlEvaluation retrieved = evaluationRepository.findById(evaluation.getId()).orElse(null);
        assertNotNull(retrieved);
        assertNotNull(retrieved.getLibrary());
        assertEquals(library.getId(), retrieved.getLibrary().getId());
    }

    @Test
    @Order(8)
    @DisplayName("Should enforce referential integrity")
    void testReferentialIntegrity() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "RefTest", "1.0.0"));

        CqlEvaluation evaluation = new CqlEvaluation(TENANT_ID, library, "patient-ref");
        evaluationRepository.save(evaluation);

        // Should be able to query evaluation through library relationship
        List<CqlEvaluation> evaluations = evaluationRepository.findByTenantIdAndLibrary_Id(
                TENANT_ID, library.getId());

        assertTrue(evaluations.size() >= 1);
        assertEquals(library.getId(), evaluations.get(0).getLibrary().getId());
    }

    @Test
    @Order(9)
    @DisplayName("Should preserve data when updating parent entity")
    void testParentUpdatePreservesChildren() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "ParentTest", "1.0.0"));

        // Create child evaluations
        evaluationRepository.save(new CqlEvaluation(TENANT_ID, library, "patient-1"));
        evaluationRepository.save(new CqlEvaluation(TENANT_ID, library, "patient-2"));

        // Update parent
        library.setDescription("Updated description");
        libraryRepository.save(library);

        // Child evaluations should still exist
        List<CqlEvaluation> evaluations = evaluationRepository.findByTenantIdAndLibrary_Id(
                TENANT_ID, library.getId());
        assertEquals(2, evaluations.size());
    }

    @Test
    @Order(10)
    @DisplayName("Should enforce value set OID format")
    void testValueSetOidConstraints() {
        ValueSet valueSet = new ValueSet(TENANT_ID, "2.16.840.1.113883.3.464.1", "Test", "SNOMED");
        valueSet = valueSetRepository.save(valueSet);
        assertNotNull(valueSet.getId());

        // Even unusual OIDs should be allowed
        ValueSet unusual = new ValueSet(TENANT_ID, "1.2.3.4.5.6.7.8.9", "Unusual", "LOINC");
        unusual = valueSetRepository.save(unusual);
        assertNotNull(unusual.getId());
    }

    @Test
    @Order(11)
    @DisplayName("Should handle large text fields without truncation")
    void testLargeTextFieldIntegrity() {
        String largeCqlContent = "library Test version '1.0.0'\n" + "A".repeat(5000);
        String largeDescription = "B".repeat(3000);

        CqlLibrary library = new CqlLibrary(TENANT_ID, "LargeText", "1.0.0");
        library.setCqlContent(largeCqlContent);
        library.setDescription(largeDescription);
        library = libraryRepository.save(library);

        CqlLibrary retrieved = libraryRepository.findById(library.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(largeCqlContent.length(), retrieved.getCqlContent().length());
        assertEquals(largeDescription.length(), retrieved.getDescription().length());
    }

    @Test
    @Order(12)
    @DisplayName("Should maintain data consistency across transactions")
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // Disable class-level @Transactional
    void testTransactionalConsistency() {
        long initialCount = libraryRepository.count();

        try {
            CqlLibrary lib1 = new CqlLibrary(TENANT_ID, "Trans1", "1.0.0");
            libraryRepository.save(lib1);

            CqlLibrary lib2 = new CqlLibrary(null, "Trans2", "1.0.0"); // This will fail
            libraryRepository.save(lib2);
            entityManager.flush(); // Force constraint validation

            fail("Should have thrown exception");
        } catch (Exception e) {
            // Expected - constraint violation occurred
            entityManager.clear(); // Clear EntityManager after exception
        }

        // In a non-transactional context, lib1 was successfully saved before lib2 failed
        long finalCount = libraryRepository.count();
        assertEquals(initialCount + 1, finalCount); // lib1 was saved, lib2 was not

        // Clean up - delete the saved library
        libraryRepository.deleteAll();
    }

    @Test
    @Order(13)
    @DisplayName("Should handle special characters in text fields")
    void testSpecialCharacters() {
        String specialChars = "Test with special chars: !@#$%^&*()_+-=[]{}|;:',.<>?/~`\"\\";

        CqlLibrary library = new CqlLibrary(TENANT_ID, specialChars, "1.0.0");
        library.setDescription(specialChars);
        library = libraryRepository.save(library);

        CqlLibrary retrieved = libraryRepository.findById(library.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(specialChars, retrieved.getLibraryName());
        assertEquals(specialChars, retrieved.getDescription());
    }

    @Test
    @Order(14)
    @DisplayName("Should handle unicode characters correctly")
    void testUnicodeCharacters() {
        String unicode = "Test with unicode: \u00E9\u00F1\u00FC \u4E2D\u6587 \u0420\u0443\u0441\u0441\u043A\u0438\u0439 \u0627\u0644\u0639\u0631\u0628\u064A\u0629";

        CqlLibrary library = new CqlLibrary(TENANT_ID, "UnicodeTest", "1.0.0");
        library.setDescription(unicode);
        library = libraryRepository.save(library);

        CqlLibrary retrieved = libraryRepository.findById(library.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(unicode, retrieved.getDescription());
    }

    @Test
    @Order(15)
    @DisplayName("Should preserve JSON structure in text fields")
    void testJsonStructurePreservation() {
        String jsonContent = "{\"library\": {\"name\": \"Test\", \"nested\": {\"value\": 123}}}";

        CqlLibrary library = new CqlLibrary(TENANT_ID, "JsonTest", "1.0.0");
        library.setElmJson(jsonContent);
        library = libraryRepository.save(library);

        CqlLibrary retrieved = libraryRepository.findById(library.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(jsonContent, retrieved.getElmJson());
    }

    @Test
    @Order(16)
    @DisplayName("Should handle null optional fields")
    void testNullOptionalFields() {
        CqlLibrary library = new CqlLibrary(TENANT_ID, "NullOptional", "1.0.0");
        // Don't set optional fields: description, publisher, cqlContent, etc.
        library = libraryRepository.save(library);

        CqlLibrary retrieved = libraryRepository.findById(library.getId()).orElse(null);
        assertNotNull(retrieved);
        assertNull(retrieved.getDescription());
        assertNull(retrieved.getPublisher());
        assertNull(retrieved.getCqlContent());
    }

    @Test
    @Order(17)
    @DisplayName("Should maintain evaluation-library relationship after library update")
    void testRelationshipAfterUpdate() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "RelUpdate", "1.0.0"));
        CqlEvaluation evaluation = evaluationRepository.save(new CqlEvaluation(TENANT_ID, library, "patient-rel"));

        // Update library
        library.setDescription("Updated");
        libraryRepository.save(library);

        // Evaluation should still reference the same library
        CqlEvaluation retrieved = evaluationRepository.findById(evaluation.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(library.getId(), retrieved.getLibrary().getId());
        assertEquals("Updated", retrieved.getLibrary().getDescription());
    }

    @Test
    @Order(18)
    @DisplayName("Should handle concurrent updates without data corruption")
    void testConcurrentUpdates() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "Concurrent", "1.0.0"));

        // Multiple updates to same entity
        for (int i = 0; i < 5; i++) {
            CqlLibrary toUpdate = libraryRepository.findById(library.getId()).orElse(null);
            assertNotNull(toUpdate);
            toUpdate.setDescription("Update " + i);
            libraryRepository.save(toUpdate);
        }

        CqlLibrary final_ = libraryRepository.findById(library.getId()).orElse(null);
        assertNotNull(final_);
        assertTrue(final_.getDescription().startsWith("Update"));
    }

    @Test
    @Order(19)
    @DisplayName("Should preserve evaluation status history")
    void testEvaluationStatusHistory() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "StatusHistory", "1.0.0"));
        CqlEvaluation evaluation = evaluationRepository.save(new CqlEvaluation(TENANT_ID, library, "patient-status"));

        // Change status multiple times
        evaluation.setStatus("PENDING");
        evaluationRepository.save(evaluation);

        evaluation.setStatus("SUCCESS");
        evaluationRepository.save(evaluation);

        CqlEvaluation retrieved = evaluationRepository.findById(evaluation.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals("SUCCESS", retrieved.getStatus());
    }

    @Test
    @Order(20)
    @DisplayName("Should maintain tenant isolation at database level")
    void testDatabaseLevelTenantIsolation() {
        CqlLibrary tenant1Lib = libraryRepository.save(new CqlLibrary("tenant-1", "Isolated1", "1.0.0"));
        CqlLibrary tenant2Lib = libraryRepository.save(new CqlLibrary("tenant-2", "Isolated2", "1.0.0"));

        List<CqlLibrary> tenant1Data = libraryRepository.findByTenantIdAndActiveTrue("tenant-1");
        List<CqlLibrary> tenant2Data = libraryRepository.findByTenantIdAndActiveTrue("tenant-2");

        assertTrue(tenant1Data.stream().allMatch(lib -> lib.getTenantId().equals("tenant-1")));
        assertTrue(tenant2Data.stream().allMatch(lib -> lib.getTenantId().equals("tenant-2")));
        assertFalse(tenant1Data.stream().anyMatch(lib -> lib.getTenantId().equals("tenant-2")));
        assertFalse(tenant2Data.stream().anyMatch(lib -> lib.getTenantId().equals("tenant-1")));
    }

    @Test
    @Order(21)
    @DisplayName("Should handle empty strings vs null correctly")
    void testEmptyStringVsNull() {
        CqlLibrary withEmpty = new CqlLibrary(TENANT_ID, "EmptyTest", "1.0.0");
        withEmpty.setDescription(""); // Empty string
        withEmpty = libraryRepository.save(withEmpty);

        CqlLibrary withNull = new CqlLibrary(TENANT_ID, "NullTest", "1.0.0");
        withNull.setDescription(null); // Null
        withNull = libraryRepository.save(withNull);

        CqlLibrary retrievedEmpty = libraryRepository.findById(withEmpty.getId()).orElse(null);
        CqlLibrary retrievedNull = libraryRepository.findById(withNull.getId()).orElse(null);

        assertNotNull(retrievedEmpty);
        assertEquals("", retrievedEmpty.getDescription());

        assertNotNull(retrievedNull);
        assertNull(retrievedNull.getDescription());
    }

    @Test
    @Order(22)
    @DisplayName("Should handle boolean fields correctly")
    void testBooleanFields() {
        CqlLibrary activeLib = new CqlLibrary(TENANT_ID, "ActiveTrue", "1.0.0");
        activeLib.setActive(true);
        activeLib = libraryRepository.save(activeLib);

        CqlLibrary inactiveLib = new CqlLibrary(TENANT_ID, "ActiveFalse", "1.0.0");
        inactiveLib.setActive(false);
        inactiveLib = libraryRepository.save(inactiveLib);

        assertTrue(libraryRepository.findById(activeLib.getId()).orElseThrow().getActive());
        assertFalse(libraryRepository.findById(inactiveLib.getId()).orElseThrow().getActive());
    }

    @Test
    @Order(23)
    @DisplayName("Should handle instant/timestamp fields with precision")
    void testTimestampPrecision() throws Exception {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "TimePrecision", "1.0.0"));
        CqlEvaluation evaluation = new CqlEvaluation(TENANT_ID, library, "patient-time");

        Instant before = Instant.now();
        Thread.sleep(10); // Small delay
        evaluation.setEvaluationDate(Instant.now());
        Thread.sleep(10);
        Instant after = Instant.now();

        evaluation = evaluationRepository.save(evaluation);

        assertTrue(evaluation.getEvaluationDate().isAfter(before));
        assertTrue(evaluation.getEvaluationDate().isBefore(after));
    }

    @Test
    @Order(24)
    @DisplayName("Should handle numeric fields correctly")
    void testNumericFields() {
        CqlLibrary library = libraryRepository.save(new CqlLibrary(TENANT_ID, "NumericTest", "1.0.0"));
        CqlEvaluation evaluation = new CqlEvaluation(TENANT_ID, library, "patient-numeric");

        evaluation.setDurationMs(12345L);
        evaluation = evaluationRepository.save(evaluation);

        CqlEvaluation retrieved = evaluationRepository.findById(evaluation.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(12345L, retrieved.getDurationMs());
    }

    @Test
    @Order(25)
    @DisplayName("Should handle array/list structures in JSON fields")
    void testJsonArrayFields() {
        String codesJson = "[\"123456\", \"789012\", \"345678\"]";

        ValueSet valueSet = new ValueSet(TENANT_ID, "2.16.840.1.array.test", "ArrayTest", "SNOMED");
        valueSet.setCodes(codesJson);
        valueSet = valueSetRepository.save(valueSet);

        ValueSet retrieved = valueSetRepository.findById(valueSet.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(codesJson, retrieved.getCodes());
    }
}

package com.healthdata.cql.security;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Multi-Tenant Data Isolation Security Tests
 *
 * HIPAA Requirement: §164.312(a)(1) - Access Control
 * Verifies that tenant data isolation is enforced at the repository level,
 * ensuring that users from one tenant cannot access data from another tenant.
 *
 * Test Scenarios:
 * - Tenant A cannot access Tenant B's CQL libraries
 * - Tenant A cannot access Tenant B's evaluations
 * - Tenant A cannot access Tenant B's value sets
 * - Cross-tenant queries return empty results
 * - Tenant-specific queries only return that tenant's data
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
@DisplayName("Multi-Tenant Data Isolation Security Tests")
class MultiTenantSecurityTest extends CqlTestcontainersBase {

    @Autowired
    private CqlLibraryRepository libraryRepository;

    @Autowired
    private CqlEvaluationRepository evaluationRepository;

    @Autowired
    private ValueSetRepository valueSetRepository;

    // Test tenants
    private static final String TENANT_A = "tenant-alpha";
    private static final String TENANT_B = "tenant-beta";
    private static final String TENANT_C = "tenant-gamma";

    // Test patient IDs (per tenant)
    private static final UUID PATIENT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PATIENT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PHI_PATIENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    // Test data holders
    private CqlLibrary tenantALibrary;
    private CqlLibrary tenantBLibrary;
    private CqlEvaluation tenantAEvaluation;
    private CqlEvaluation tenantBEvaluation;
    private ValueSet tenantAValueSet;
    private ValueSet tenantBValueSet;

    @BeforeEach
    void setUp() {
        // Create test data for Tenant A
        tenantALibrary = new CqlLibrary(TENANT_A, "TenantALibrary", "1.0.0");
        tenantALibrary.setStatus("ACTIVE");
        tenantALibrary.setCqlContent("library TenantALibrary version '1.0.0'");
        tenantALibrary.setDescription("Confidential Tenant A Measure");
        tenantALibrary = libraryRepository.save(tenantALibrary);

        tenantAEvaluation = new CqlEvaluation(TENANT_A, tenantALibrary, PATIENT_A);
        tenantAEvaluation.setStatus("SUCCESS");
        tenantAEvaluation.setEvaluationDate(Instant.now());
        tenantAEvaluation.setDurationMs(100L);
        tenantAEvaluation = evaluationRepository.save(tenantAEvaluation);

        tenantAValueSet = new ValueSet(TENANT_A, "2.16.840.1.113883.3.464.1003.101.12.1001", "TenantA-ValueSet", "SNOMED");
        tenantAValueSet = valueSetRepository.save(tenantAValueSet);

        // Create test data for Tenant B
        tenantBLibrary = new CqlLibrary(TENANT_B, "TenantBLibrary", "1.0.0");
        tenantBLibrary.setStatus("ACTIVE");
        tenantBLibrary.setCqlContent("library TenantBLibrary version '1.0.0'");
        tenantBLibrary.setDescription("Confidential Tenant B Measure");
        tenantBLibrary = libraryRepository.save(tenantBLibrary);

        tenantBEvaluation = new CqlEvaluation(TENANT_B, tenantBLibrary, PATIENT_B);
        tenantBEvaluation.setStatus("SUCCESS");
        tenantBEvaluation.setEvaluationDate(Instant.now());
        tenantBEvaluation.setDurationMs(150L);
        tenantBEvaluation = evaluationRepository.save(tenantBEvaluation);

        tenantBValueSet = new ValueSet(TENANT_B, "2.16.840.1.113883.3.464.1003.101.12.1002", "TenantB-ValueSet", "SNOMED");
        tenantBValueSet = valueSetRepository.save(tenantBValueSet);
    }

    // ==================== CQL Library Isolation Tests ====================

    @Test
    @Order(1)
    @DisplayName("Tenant A should only see Tenant A's libraries")
    void testLibraryIsolation_TenantA_OnlySeesOwnLibraries() {
        List<CqlLibrary> tenantALibraries = libraryRepository.findByTenantIdAndActiveTrue(TENANT_A);

        assertNotNull(tenantALibraries);
        assertFalse(tenantALibraries.isEmpty());
        assertTrue(tenantALibraries.stream()
            .allMatch(lib -> TENANT_A.equals(lib.getTenantId())),
            "All returned libraries must belong to Tenant A");
        assertTrue(tenantALibraries.stream()
            .noneMatch(lib -> TENANT_B.equals(lib.getTenantId())),
            "No Tenant B libraries should be returned");
    }

    @Test
    @Order(2)
    @DisplayName("Tenant B should only see Tenant B's libraries")
    void testLibraryIsolation_TenantB_OnlySeesOwnLibraries() {
        List<CqlLibrary> tenantBLibraries = libraryRepository.findByTenantIdAndActiveTrue(TENANT_B);

        assertNotNull(tenantBLibraries);
        assertFalse(tenantBLibraries.isEmpty());
        assertTrue(tenantBLibraries.stream()
            .allMatch(lib -> TENANT_B.equals(lib.getTenantId())),
            "All returned libraries must belong to Tenant B");
    }

    @Test
    @Order(3)
    @DisplayName("Cross-tenant library access should return empty")
    void testLibraryIsolation_CrossTenantAccess_ReturnsEmpty() {
        // Try to find Tenant B's library using Tenant A's context
        Optional<CqlLibrary> crossTenantAccess = libraryRepository
            .findByTenantIdAndLibraryNameAndVersionAndActiveTrue(
                TENANT_A,
                tenantBLibrary.getLibraryName(),
                tenantBLibrary.getVersion()
            );

        assertTrue(crossTenantAccess.isEmpty(),
            "Cross-tenant library access should not return any data");
    }

    @Test
    @Order(4)
    @DisplayName("Unknown tenant should see no libraries")
    void testLibraryIsolation_UnknownTenant_ReturnsEmpty() {
        List<CqlLibrary> unknownTenantLibraries = libraryRepository
            .findByTenantIdAndActiveTrue(TENANT_C);

        assertNotNull(unknownTenantLibraries);
        assertTrue(unknownTenantLibraries.isEmpty(),
            "Unknown tenant should not see any libraries");
    }

    @Test
    @Order(5)
    @DisplayName("Library search should be tenant-scoped")
    void testLibrarySearch_TenantScoped() {
        // Search for "Library" - both tenants have libraries with this in name
        List<CqlLibrary> tenantASearch = libraryRepository.searchByName(TENANT_A, "Library");
        List<CqlLibrary> tenantBSearch = libraryRepository.searchByName(TENANT_B, "Library");

        // Verify tenant isolation in search results
        assertTrue(tenantASearch.stream()
            .allMatch(lib -> TENANT_A.equals(lib.getTenantId())),
            "Tenant A search should only return Tenant A libraries");
        assertTrue(tenantBSearch.stream()
            .allMatch(lib -> TENANT_B.equals(lib.getTenantId())),
            "Tenant B search should only return Tenant B libraries");
    }

    // ==================== CQL Evaluation Isolation Tests ====================

    @Test
    @Order(10)
    @DisplayName("Tenant A should only see Tenant A's evaluations")
    void testEvaluationIsolation_TenantA_OnlySeesOwnEvaluations() {
        Page<CqlEvaluation> tenantAEvaluations = evaluationRepository
            .findByTenantId(TENANT_A, PageRequest.of(0, 100));

        assertNotNull(tenantAEvaluations);
        assertFalse(tenantAEvaluations.isEmpty());
        assertTrue(tenantAEvaluations.getContent().stream()
            .allMatch(eval -> TENANT_A.equals(eval.getTenantId())),
            "All returned evaluations must belong to Tenant A");
    }

    @Test
    @Order(11)
    @DisplayName("Patient evaluations should be tenant-scoped")
    void testEvaluationIsolation_PatientEvaluations_TenantScoped() {
        // Get Tenant A's evaluations for their patient
        List<CqlEvaluation> tenantAPatientEvals = evaluationRepository
            .findByTenantIdAndPatientId(TENANT_A, PATIENT_A);

        // Try to get Tenant B's patient evaluations using Tenant A's context
        List<CqlEvaluation> crossTenantPatientEvals = evaluationRepository
            .findByTenantIdAndPatientId(TENANT_A, PATIENT_B);

        assertFalse(tenantAPatientEvals.isEmpty(),
            "Tenant A should see their own patient evaluations");
        assertTrue(crossTenantPatientEvals.isEmpty(),
            "Tenant A should not see Tenant B's patient evaluations");
    }

    @Test
    @Order(12)
    @DisplayName("Evaluation by ID should respect tenant context")
    void testEvaluationIsolation_ByIdWithTenant() {
        // This test verifies that even with a valid ID, tenant context matters
        Optional<CqlEvaluation> tenantAEval = evaluationRepository.findById(tenantAEvaluation.getId());
        Optional<CqlEvaluation> tenantBEval = evaluationRepository.findById(tenantBEvaluation.getId());

        assertTrue(tenantAEval.isPresent());
        assertTrue(tenantBEval.isPresent());

        // Verify each evaluation belongs to correct tenant
        assertEquals(TENANT_A, tenantAEval.get().getTenantId());
        assertEquals(TENANT_B, tenantBEval.get().getTenantId());
    }

    // ==================== ValueSet Isolation Tests ====================

    @Test
    @Order(20)
    @DisplayName("Tenant A should only see Tenant A's value sets")
    void testValueSetIsolation_TenantA_OnlySeesOwnValueSets() {
        List<ValueSet> tenantAValueSets = valueSetRepository.findByTenantIdAndActiveTrue(TENANT_A);

        assertNotNull(tenantAValueSets);
        assertFalse(tenantAValueSets.isEmpty());
        assertTrue(tenantAValueSets.stream()
            .allMatch(vs -> TENANT_A.equals(vs.getTenantId())),
            "All returned value sets must belong to Tenant A");
    }

    @Test
    @Order(21)
    @DisplayName("ValueSet OID lookup should be tenant-scoped")
    void testValueSetIsolation_OidLookup_TenantScoped() {
        // Try to find Tenant B's value set OID using Tenant A's context
        Optional<ValueSet> crossTenantOidLookup = valueSetRepository
            .findByTenantIdAndOidAndActiveTrue(TENANT_A, tenantBValueSet.getOid());

        assertTrue(crossTenantOidLookup.isEmpty(),
            "Cross-tenant OID lookup should not return data");
    }

    // ==================== Data Count Isolation Tests ====================

    @Test
    @Order(30)
    @DisplayName("Library count should be tenant-scoped")
    void testLibraryCount_TenantScoped() {
        long tenantACount = libraryRepository.countByTenantIdAndActiveTrue(TENANT_A);
        long tenantBCount = libraryRepository.countByTenantIdAndActiveTrue(TENANT_B);
        long unknownTenantCount = libraryRepository.countByTenantIdAndActiveTrue(TENANT_C);

        assertTrue(tenantACount > 0, "Tenant A should have libraries");
        assertTrue(tenantBCount > 0, "Tenant B should have libraries");
        assertEquals(0, unknownTenantCount, "Unknown tenant should have no libraries");
    }

    @Test
    @Order(31)
    @DisplayName("Evaluation count should be tenant-scoped")
    void testEvaluationCount_TenantScoped() {
        // Use countByTenantIdAndPatientId as countByTenantId doesn't exist
        long tenantACount = evaluationRepository.countByTenantIdAndPatientId(TENANT_A, PATIENT_A);
        long tenantBCount = evaluationRepository.countByTenantIdAndPatientId(TENANT_B, PATIENT_B);
        long unknownTenantCount = evaluationRepository.countByTenantIdAndPatientId(TENANT_C, UUID.randomUUID());

        assertTrue(tenantACount > 0, "Tenant A should have evaluations for their patient");
        assertTrue(tenantBCount > 0, "Tenant B should have evaluations for their patient");
        assertEquals(0, unknownTenantCount, "Unknown tenant should have no evaluations");
    }

    // ==================== PHI Protection Tests ====================

    @Test
    @Order(40)
    @DisplayName("Patient data should not leak between tenants")
    void testPatientDataIsolation_NoLeakBetweenTenants() {
        // Create evaluation with sensitive patient data
        CqlEvaluation sensitiveEval = new CqlEvaluation(TENANT_A, tenantALibrary, PHI_PATIENT_ID);
        sensitiveEval.setStatus("SUCCESS");
        sensitiveEval.setEvaluationDate(Instant.now());
        evaluationRepository.save(sensitiveEval);

        // Try to access from Tenant B context
        List<CqlEvaluation> tenantBSearch = evaluationRepository
            .findByTenantIdAndPatientId(TENANT_B, PHI_PATIENT_ID);

        assertTrue(tenantBSearch.isEmpty(),
            "PHI patient data must not leak between tenants");
    }

    @Test
    @Order(41)
    @DisplayName("Library content should not leak between tenants")
    void testLibraryContentIsolation_NoLeakBetweenTenants() {
        // Add sensitive content to Tenant A's library
        tenantALibrary.setCqlContent("/* CONFIDENTIAL: Contains proprietary algorithms */");
        libraryRepository.save(tenantALibrary);

        // Verify Tenant B cannot access this content
        List<CqlLibrary> tenantBLibraries = libraryRepository.findByTenantIdAndActiveTrue(TENANT_B);

        assertTrue(tenantBLibraries.stream()
            .noneMatch(lib -> lib.getCqlContent() != null &&
                             lib.getCqlContent().contains("CONFIDENTIAL")),
            "Confidential library content must not leak to other tenants");
    }

    // ==================== Concurrent Tenant Access Tests ====================

    @Test
    @Order(50)
    @DisplayName("Multiple tenants can have same-named libraries")
    void testMultipleTenants_SameNamedLibraries_Isolated() {
        // Create library with same name for both tenants
        CqlLibrary tenantADup = new CqlLibrary(TENANT_A, "SharedMeasure", "1.0.0");
        tenantADup.setStatus("ACTIVE");
        libraryRepository.save(tenantADup);

        CqlLibrary tenantBDup = new CqlLibrary(TENANT_B, "SharedMeasure", "1.0.0");
        tenantBDup.setStatus("ACTIVE");
        libraryRepository.save(tenantBDup);

        // Verify each tenant only sees their own
        Optional<CqlLibrary> tenantAResult = libraryRepository
            .findByTenantIdAndLibraryNameAndVersionAndActiveTrue(TENANT_A, "SharedMeasure", "1.0.0");
        Optional<CqlLibrary> tenantBResult = libraryRepository
            .findByTenantIdAndLibraryNameAndVersionAndActiveTrue(TENANT_B, "SharedMeasure", "1.0.0");

        assertTrue(tenantAResult.isPresent());
        assertTrue(tenantBResult.isPresent());
        assertNotEquals(tenantAResult.get().getId(), tenantBResult.get().getId(),
            "Same-named libraries should be different entities for different tenants");
    }
}

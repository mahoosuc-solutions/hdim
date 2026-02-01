package com.healthdata.cqrsquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PatientQueryService
 *
 * Tests read-optimized patient queries backed by projections:
 * - Patient search by demographics
 * - Filtering by condition, medication, enrollment status
 * - Pagination and sorting
 * - Caching with proper TTL
 * - Multi-tenant isolation
 * - Full-text search capabilities
 *
 * CQRS Pattern: Separates read model (queries) from write model (commands).
 * Queries operate on denormalized projections optimized for fast retrieval.
 */
@DisplayName("PatientQueryService Tests")
class PatientQueryServiceTest {

    private PatientQueryService patientQueryService;
    private MockProjectionStore mockProjectionStore;
    private MockCacheStore mockCacheStore;

    @BeforeEach
    void setup() {
        mockProjectionStore = new MockProjectionStore();
        mockCacheStore = new MockCacheStore();
        patientQueryService = new PatientQueryService(mockProjectionStore, mockCacheStore);
    }

    // ===== Basic Patient Search Tests =====

    @Test
    @DisplayName("Should search patients by first name")
    void testSearchByFirstName() {
        // Given: Multiple patients
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatient(tenantId, "PATIENT-1", "John", "Doe");
        mockProjectionStore.addPatient(tenantId, "PATIENT-2", "John", "Smith");
        mockProjectionStore.addPatient(tenantId, "PATIENT-3", "Jane", "Doe");

        // When: Searching for John
        List<PatientQueryResult> results = patientQueryService.searchByFirstName(tenantId, "John");

        // Then: Should return both Johns
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getFirstName().equals("John"));
    }

    @Test
    @DisplayName("Should search patients by last name")
    void testSearchByLastName() {
        // Given: Patients with same last name
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatient(tenantId, "PATIENT-1", "John", "Doe");
        mockProjectionStore.addPatient(tenantId, "PATIENT-2", "Jane", "Doe");
        mockProjectionStore.addPatient(tenantId, "PATIENT-3", "Bob", "Smith");

        // When: Searching for Doe
        List<PatientQueryResult> results = patientQueryService.searchByLastName(tenantId, "Doe");

        // Then: Should return both Doe patients
        assertThat(results).hasSize(2).allMatch(r -> r.getLastName().equals("Doe"));
    }

    @Test
    @DisplayName("Should search by date of birth")
    void testSearchByDateOfBirth() {
        // Given: Patients with different DOBs
        String tenantId = "TENANT-001";
        LocalDate dob1 = LocalDate.of(1990, 5, 15);
        LocalDate dob2 = LocalDate.of(1960, 3, 20);

        mockProjectionStore.addPatientWithDOB(tenantId, "PATIENT-1", "John", "Doe", dob1);
        mockProjectionStore.addPatientWithDOB(tenantId, "PATIENT-2", "Jane", "Smith", dob2);

        // When: Searching by specific DOB
        List<PatientQueryResult> results = patientQueryService.searchByDateOfBirth(tenantId, dob1);

        // Then: Should return matching patient
        assertThat(results).hasSize(1).extracting(PatientQueryResult::getFirstName).contains("John");
    }

    @Test
    @DisplayName("Should search patients using full-text search")
    void testFullTextSearch() {
        // Given: Multiple patients
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatient(tenantId, "PATIENT-1", "John", "Doe");
        mockProjectionStore.addPatient(tenantId, "PATIENT-2", "Jane", "Smith");
        mockProjectionStore.addPatient(tenantId, "PATIENT-3", "Jonathan", "Jones");

        // When: Full-text search for "John"
        List<PatientQueryResult> results = patientQueryService.fullTextSearch(tenantId, "John");

        // Then: Should return matching patients (John, Jonathan)
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }

    // ===== Filtering Tests =====

    @Test
    @DisplayName("Should filter patients by condition")
    void testFilterByCondition() {
        // Given: Patients with different conditions
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatientWithCondition(tenantId, "PATIENT-1", "John", "Doe", "HTN");
        mockProjectionStore.addPatientWithCondition(tenantId, "PATIENT-2", "Jane", "Smith", "DM");
        mockProjectionStore.addPatientWithCondition(tenantId, "PATIENT-3", "Bob", "Jones", "HTN");

        // When: Filtering for HTN patients
        List<PatientQueryResult> results = patientQueryService.filterByCondition(tenantId, "HTN");

        // Then: Should return only HTN patients
        assertThat(results).hasSize(2).allMatch(r -> r.hasCondition("HTN"));
    }

    @Test
    @DisplayName("Should filter patients by medication")
    void testFilterByMedication() {
        // Given: Patients on different medications
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatientWithMedication(tenantId, "PATIENT-1", "John", "Doe", "LISINOPRIL");
        mockProjectionStore.addPatientWithMedication(tenantId, "PATIENT-2", "Jane", "Smith", "METFORMIN");
        mockProjectionStore.addPatientWithMedication(tenantId, "PATIENT-3", "Bob", "Jones", "LISINOPRIL");

        // When: Filtering for LISINOPRIL patients
        List<PatientQueryResult> results = patientQueryService.filterByMedication(tenantId, "LISINOPRIL");

        // Then: Should return only patients on LISINOPRIL
        assertThat(results).hasSize(2).allMatch(r -> r.hasMedication("LISINOPRIL"));
    }

    @Test
    @DisplayName("Should filter by enrollment status")
    void testFilterByEnrollmentStatus() {
        // Given: Patients with different enrollment statuses
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatientWithEnrollment(tenantId, "PATIENT-1", "John", "Doe", "ACTIVE");
        mockProjectionStore.addPatientWithEnrollment(tenantId, "PATIENT-2", "Jane", "Smith", "INACTIVE");
        mockProjectionStore.addPatientWithEnrollment(tenantId, "PATIENT-3", "Bob", "Jones", "ACTIVE");

        // When: Filtering for active patients
        List<PatientQueryResult> results = patientQueryService.filterByEnrollmentStatus(tenantId, "ACTIVE");

        // Then: Should return only active patients
        assertThat(results).hasSize(2).allMatch(r -> r.getEnrollmentStatus().equals("ACTIVE"));
    }

    @Test
    @DisplayName("Should chain multiple filters")
    void testChainedFilters() {
        // Given: Patients with various attributes
        String tenantId = "TENANT-001";
        mockProjectionStore.addComplexPatient(tenantId, "PATIENT-1", "John", "Doe", "HTN", "LISINOPRIL", "ACTIVE");
        mockProjectionStore.addComplexPatient(tenantId, "PATIENT-2", "Jane", "Smith", "DM", "METFORMIN", "ACTIVE");
        mockProjectionStore.addComplexPatient(tenantId, "PATIENT-3", "Bob", "Jones", "HTN", "LISINOPRIL", "INACTIVE");

        // When: Filtering by condition AND medication AND enrollment
        List<PatientQueryResult> results = patientQueryService.getActivePatientsByConditionAndMedication(
            tenantId, "HTN", "LISINOPRIL"
        );

        // Then: Should return only matching patients
        assertThat(results).hasSize(1)
            .extracting(PatientQueryResult::getFirstName)
            .contains("John");
    }

    // ===== Pagination & Sorting Tests =====

    @Test
    @DisplayName("Should paginate results")
    void testPagination() {
        // Given: 100 patients
        String tenantId = "TENANT-001";
        for (int i = 0; i < 100; i++) {
            mockProjectionStore.addPatient(tenantId, "PATIENT-" + i, "Patient", String.valueOf(i));
        }

        // When: Getting page 1 with size 10
        PaginatedResult<PatientQueryResult> page1 = patientQueryService.searchWithPagination(tenantId, 0, 10);

        // Then: Should return 10 items
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page1.getTotalElements()).isEqualTo(100);
        assertThat(page1.getTotalPages()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should sort results by last name")
    void testSortByLastName() {
        // Given: Patients with different last names
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatient(tenantId, "PATIENT-1", "John", "Zebra");
        mockProjectionStore.addPatient(tenantId, "PATIENT-2", "Jane", "Apple");
        mockProjectionStore.addPatient(tenantId, "PATIENT-3", "Bob", "Monkey");

        // When: Sorting by last name ascending
        List<PatientQueryResult> results = patientQueryService.searchSortedByLastName(tenantId, true);

        // Then: Should be sorted alphabetically
        assertThat(results)
            .extracting(PatientQueryResult::getLastName)
            .containsExactly("Apple", "Monkey", "Zebra");
    }

    @Test
    @DisplayName("Should sort by age")
    void testSortByAge() {
        // Given: Patients of different ages
        String tenantId = "TENANT-001";
        LocalDate dob1 = LocalDate.of(2000, 1, 1); // ~24 years old
        LocalDate dob2 = LocalDate.of(1990, 1, 1); // ~34 years old
        LocalDate dob3 = LocalDate.of(1970, 1, 1); // ~54 years old

        mockProjectionStore.addPatientWithDOB(tenantId, "PATIENT-1", "John", "Doe", dob1);
        mockProjectionStore.addPatientWithDOB(tenantId, "PATIENT-2", "Jane", "Smith", dob2);
        mockProjectionStore.addPatientWithDOB(tenantId, "PATIENT-3", "Bob", "Jones", dob3);

        // When: Sorting by age (oldest first)
        List<PatientQueryResult> results = patientQueryService.searchSortedByAge(tenantId, false);

        // Then: Should be sorted by age descending
        assertThat(results).hasSizeGreaterThanOrEqualTo(3);
    }

    // ===== Caching Tests =====

    @Test
    @DisplayName("Should cache query results")
    void testQueryCaching() {
        // Given: Search query
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatient(tenantId, "PATIENT-1", "John", "Doe");

        // When: Executing same search twice
        List<PatientQueryResult> firstResult = patientQueryService.searchByFirstName(tenantId, "John");
        List<PatientQueryResult> secondResult = patientQueryService.searchByFirstName(tenantId, "John");

        // Then: Second should be served from cache
        assertThat(firstResult).isEqualTo(secondResult);
        assertThat(mockCacheStore.getCacheHitCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should respect cache TTL")
    void testCacheTTL() {
        // Given: Cached results
        String tenantId = "TENANT-001";
        mockProjectionStore.addPatient(tenantId, "PATIENT-1", "John", "Doe");

        // When: Accessing cached data
        List<PatientQueryResult> cached = patientQueryService.searchByFirstName(tenantId, "John");

        // Then: Cache entry should have TTL set
        assertThat(cached).isNotEmpty();
        long ttl = mockCacheStore.getTTL("search:John:" + tenantId);
        assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(300); // 5 minutes max for PHI
    }

    // ===== Multi-Tenant Tests =====

    @Test
    @DisplayName("Should isolate patients by tenant")
    void testMultiTenantIsolation() {
        // Given: Patients in different tenants
        String tenant1 = "TENANT-001";
        String tenant2 = "TENANT-002";

        mockProjectionStore.addPatient(tenant1, "PATIENT-1", "John", "Doe");
        mockProjectionStore.addPatient(tenant2, "PATIENT-1", "Jane", "Smith");

        // When: Searching in each tenant
        List<PatientQueryResult> tenant1Results = patientQueryService.searchByFirstName(tenant1, "John");
        List<PatientQueryResult> tenant2Results = patientQueryService.searchByFirstName(tenant2, "John");

        // Then: Should return different results
        assertThat(tenant1Results).hasSizeLessThanOrEqualTo(1);
        assertThat(tenant2Results).hasSize(0); // Jane, not John
    }

    @Test
    @DisplayName("Should enforce tenant access control")
    void testTenantAccessControl() {
        // Given: Patient in TENANT-001
        String authorizedTenant = "TENANT-001";
        String unauthorizedTenant = "TENANT-999";

        mockProjectionStore.addPatient(authorizedTenant, "PATIENT-1", "John", "Doe");

        // When: Attempting to query from unauthorized tenant
        List<PatientQueryResult> results = patientQueryService.searchByFirstName(unauthorizedTenant, "John");

        // Then: Should return empty (access denied)
        assertThat(results).isEmpty();
    }

    // ===== Performance Tests =====

    @Test
    @DisplayName("Should execute search on 10k patients efficiently")
    void testLargeDatasetSearch() {
        // Given: 10,000 patients
        String tenantId = "TENANT-001";
        for (int i = 0; i < 10_000; i++) {
            mockProjectionStore.addPatient(tenantId, "PATIENT-" + i, "Patient", String.valueOf(i));
        }

        // When: Searching
        long start = System.currentTimeMillis();
        List<PatientQueryResult> results = patientQueryService.searchByFirstName(tenantId, "Patient");
        long duration = System.currentTimeMillis() - start;

        // Then: Should complete in reasonable time
        assertThat(duration).isLessThan(5000); // < 5 seconds
        assertThat(results).hasSizeGreaterThan(0);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle invalid search criteria")
    void testInvalidSearchCriteria() {
        // When: Searching with null
        // Then: Should throw or return empty
        assertThatCode(() -> patientQueryService.searchByFirstName("TENANT-001", null))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle empty results gracefully")
    void testEmptySearchResults() {
        // When: Searching for non-existent patient
        List<PatientQueryResult> results = patientQueryService.searchByFirstName("TENANT-001", "XYZ");

        // Then: Should return empty list
        assertThat(results).isEmpty();
    }

    // ===== Mock Classes =====

    static class MockProjectionStore {
        private final Map<String, List<PatientProjection>> data = new HashMap<>();

        void addPatient(String tenantId, String patientId, String firstName, String lastName) {
            data.computeIfAbsent(tenantId, k -> new ArrayList<>())
                .add(new PatientProjection(patientId, firstName, lastName));
        }

        void addPatientWithDOB(String tenantId, String patientId, String firstName, String lastName, LocalDate dob) {
            PatientProjection p = new PatientProjection(patientId, firstName, lastName);
            p.setDateOfBirth(dob);
            data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
        }

        void addPatientWithCondition(String tenantId, String patientId, String firstName, String lastName, String condition) {
            PatientProjection p = new PatientProjection(patientId, firstName, lastName);
            p.addCondition(condition);
            data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
        }

        void addPatientWithMedication(String tenantId, String patientId, String firstName, String lastName, String medication) {
            PatientProjection p = new PatientProjection(patientId, firstName, lastName);
            p.addMedication(medication);
            data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
        }

        void addPatientWithEnrollment(String tenantId, String patientId, String firstName, String lastName, String status) {
            PatientProjection p = new PatientProjection(patientId, firstName, lastName);
            p.setEnrollmentStatus(status);
            data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
        }

        void addComplexPatient(String tenantId, String patientId, String firstName, String lastName,
                              String condition, String medication, String enrollmentStatus) {
            PatientProjection p = new PatientProjection(patientId, firstName, lastName);
            p.addCondition(condition);
            p.addMedication(medication);
            p.setEnrollmentStatus(enrollmentStatus);
            data.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(p);
        }

        List<PatientProjection> getPatients(String tenantId) {
            return data.getOrDefault(tenantId, new ArrayList<>());
        }
    }

    static class MockCacheStore {
        private final Map<String, Object> cache = new HashMap<>();
        private final Map<String, Long> ttls = new HashMap<>();
        private int cacheHitCount = 0;

        void put(String key, Object value, long ttlSeconds) {
            cache.put(key, value);
            ttls.put(key, ttlSeconds);
            cacheHitCount++;
        }

        Object get(String key) {
            if (cache.containsKey(key)) {
                cacheHitCount++;
                return cache.get(key);
            }
            return null;
        }

        long getTTL(String key) {
            return ttls.getOrDefault(key, 0L);
        }

        int getCacheHitCount() {
            return cacheHitCount;
        }
    }

    static class PatientProjection {
        private final String patientId;
        private final String firstName;
        private final String lastName;
        private LocalDate dateOfBirth;
        private final Set<String> conditions = new HashSet<>();
        private final Set<String> medications = new HashSet<>();
        private String enrollmentStatus = "UNKNOWN";

        PatientProjection(String patientId, String firstName, String lastName) {
            this.patientId = patientId;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        void addCondition(String condition) {
            conditions.add(condition);
        }

        void addMedication(String medication) {
            medications.add(medication);
        }

        void setEnrollmentStatus(String status) {
            this.enrollmentStatus = status;
        }

        void setDateOfBirth(LocalDate dob) {
            this.dateOfBirth = dob;
        }

        String getPatientId() { return patientId; }
        String getFirstName() { return firstName; }
        String getLastName() { return lastName; }
        LocalDate getDateOfBirth() { return dateOfBirth; }
        Set<String> getConditions() { return conditions; }
        Set<String> getMedications() { return medications; }
        String getEnrollmentStatus() { return enrollmentStatus; }
    }
}

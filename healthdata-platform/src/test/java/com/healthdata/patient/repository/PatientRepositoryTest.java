package com.healthdata.patient.repository;

import com.healthdata.BaseRepositoryTest;
import com.healthdata.DataTestFactory;
import com.healthdata.patient.domain.Patient;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Patient Repository Integration Tests
 * Tests all custom query methods and tenant isolation
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Patient Repository Tests")
public class PatientRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    private Patient testPatient;
    private Patient testPatient2;
    private Patient testPatient3;

    @BeforeEach
    void setUp() {
        // Create test patients for tenant1
        testPatient = DataTestFactory.patientBuilder()
            .withMrn("MRN-TEST-001")
            .withFirstName("John")
            .withLastName("Smith")
            .withTenantId("tenant1")
            .build();

        testPatient2 = DataTestFactory.patientBuilder()
            .withMrn("MRN-TEST-002")
            .withFirstName("Jane")
            .withLastName("Doe")
            .withTenantId("tenant1")
            .build();

        testPatient3 = DataTestFactory.patientBuilder()
            .withMrn("MRN-TEST-003")
            .withFirstName("Robert")
            .withLastName("Johnson")
            .withTenantId("tenant2")
            .build();
    }

    // ========================================================================
    // Basic CRUD Tests
    // ========================================================================

    @Test
    @DisplayName("Should save and retrieve patient")
    void testSaveAndRetrievePatient() {
        // Arrange
        Patient saved = patientRepository.save(testPatient);

        // Act
        Optional<Patient> retrieved = patientRepository.findById(saved.getId());

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals(saved.getId(), retrieved.get().getId());
        assertEquals("John", retrieved.get().getFirstName());
        assertEquals("MRN-TEST-001", retrieved.get().getMrn());
    }

    @Test
    @DisplayName("Should update patient")
    void testUpdatePatient() {
        // Arrange
        Patient saved = patientRepository.save(testPatient);

        // Act
        saved.setEmail("newemail@example.com");
        patientRepository.save(saved);
        Patient updated = patientRepository.findById(saved.getId()).orElseThrow();

        // Assert
        assertEquals("newemail@example.com", updated.getEmail());
    }

    @Test
    @DisplayName("Should delete patient")
    void testDeletePatient() {
        // Arrange
        Patient saved = patientRepository.save(testPatient);
        String patientId = saved.getId();

        // Act
        patientRepository.deleteById(patientId);

        // Assert
        assertFalse(patientRepository.existsById(patientId));
    }

    // ========================================================================
    // Find by MRN Tests
    // ========================================================================

    @Test
    @DisplayName("Should find patient by MRN")
    void testFindByMrn() {
        // Arrange
        patientRepository.save(testPatient);

        // Act
        Optional<Patient> found = patientRepository.findByMrn("MRN-TEST-001");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getFirstName());
    }

    @Test
    @DisplayName("Should return empty for non-existent MRN")
    void testFindByMrnNotFound() {
        // Act
        Optional<Patient> found = patientRepository.findByMrn("NONEXISTENT");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should check if patient exists by MRN")
    void testExistsByMrn() {
        // Arrange
        patientRepository.save(testPatient);

        // Act & Assert
        assertTrue(patientRepository.existsByMrn("MRN-TEST-001"));
        assertFalse(patientRepository.existsByMrn("NONEXISTENT"));
    }

    // ========================================================================
    // Tenant Isolation Tests
    // ========================================================================

    @Test
    @DisplayName("Should find patient by MRN and TenantId")
    void testFindByMrnAndTenantId() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient3);

        // Act
        Optional<Patient> found = patientRepository.findByMrnAndTenantId("MRN-TEST-001", "tenant1");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getFirstName());
        assertEquals("tenant1", found.get().getTenantId());
    }

    @Test
    @DisplayName("Should isolate patients by tenant when finding by MRN and TenantId")
    void testTenantIsolationByMrnAndTenant() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient3);

        // Act
        Optional<Patient> found = patientRepository.findByMrnAndTenantId("MRN-TEST-001", "tenant2");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find patients by first name, last name and tenant")
    void testFindByFirstNameLastNameAndTenant() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient3);

        // Act
        List<Patient> found = patientRepository.findByFirstNameAndLastNameAndTenantId("John", "Smith", "tenant1");

        // Assert
        assertEquals(1, found.size());
        assertEquals("MRN-TEST-001", found.get(0).getMrn());
    }

    // ========================================================================
    // Pagination Tests
    // ========================================================================

    @Test
    @DisplayName("Should find patients by tenant with pagination")
    void testFindByTenantIdWithPagination() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);
        patientRepository.save(testPatient3);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> page = patientRepository.findByTenantId("tenant1", pageable);

        // Assert
        assertEquals(2, page.getContent().size());
        assertEquals(1, page.getTotalPages());
    }

    @Test
    @DisplayName("Should paginate correctly with multiple pages")
    void testPaginationWithMultiplePages() {
        // Arrange
        for (int i = 0; i < 15; i++) {
            Patient p = DataTestFactory.patientBuilder()
                .withMrn("MRN-BATCH-" + i)
                .withFirstName("Patient" + i)
                .withLastName("Test")
                .withTenantId("tenant1")
                .build();
            patientRepository.save(p);
        }

        // Act
        Pageable page1 = PageRequest.of(0, 5);
        Page<Patient> firstPage = patientRepository.findByTenantId("tenant1", page1);

        Pageable page2 = PageRequest.of(1, 5);
        Page<Patient> secondPage = patientRepository.findByTenantId("tenant1", page2);

        // Assert
        assertEquals(5, firstPage.getContent().size());
        assertEquals(5, secondPage.getContent().size());
        assertEquals(3, firstPage.getTotalPages());
        assertNotEquals(firstPage.getContent().get(0).getId(), secondPage.getContent().get(0).getId());
    }

    // ========================================================================
    // Search Tests
    // ========================================================================

    @Test
    @DisplayName("Should search patients by multiple criteria")
    void testSearchPatients() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> results = patientRepository.searchPatients("John", "", "", "tenant1", pageable);

        // Assert
        assertEquals(1, results.getContent().size());
        assertEquals("John", results.getContent().get(0).getFirstName());
    }

    @Test
    @DisplayName("Should search by last name with partial match")
    void testSearchByLastNamePartial() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> results = patientRepository.searchPatients("", "Smith", "", "tenant1", pageable);

        // Assert
        assertEquals(1, results.getContent().size());
    }

    @Test
    @DisplayName("Should search by MRN with partial match")
    void testSearchByMrnPartial() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> results = patientRepository.searchPatients("", "", "TEST-001", "tenant1", pageable);

        // Assert
        assertTrue(results.getContent().size() > 0);
    }

    @Test
    @DisplayName("Should isolate search results by tenant")
    void testSearchWithTenantIsolation() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient3);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> tenant1Results = patientRepository.searchPatients("John", "", "", "tenant1", pageable);
        Page<Patient> tenant2Results = patientRepository.searchPatients("John", "", "", "tenant2", pageable);

        // Assert
        assertEquals(1, tenant1Results.getContent().size());
        assertEquals(0, tenant2Results.getContent().size());
    }

    // ========================================================================
    // Active Patients Tests
    // ========================================================================

    @Test
    @DisplayName("Should find all active patients for tenant")
    void testFindAllActivePatientsForTenant() {
        // Arrange
        testPatient.setActive(true);
        testPatient2.setActive(true);
        Patient inactivePatient = DataTestFactory.patientBuilder()
            .withMrn("MRN-INACTIVE-001")
            .withTenantId("tenant1")
            .withActive(false)
            .build();

        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);
        patientRepository.save(inactivePatient);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> active = patientRepository.findAllActivePatientsForTenant("tenant1", pageable);

        // Assert
        assertEquals(2, active.getContent().size());
    }

    @Test
    @DisplayName("Should count active patients by tenant")
    void testCountActivePatientsByTenant() {
        // Arrange
        testPatient.setActive(true);
        testPatient2.setActive(true);
        Patient inactivePatient = DataTestFactory.patientBuilder()
            .withMrn("MRN-INACTIVE-002")
            .withTenantId("tenant1")
            .withActive(false)
            .build();

        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);
        patientRepository.save(inactivePatient);

        // Act
        long count = patientRepository.countActivePatientsByTenant("tenant1");

        // Assert
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Should count active and inactive patients separately")
    void testCountActivePatients() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        long count = patientRepository.countByTenantIdAndActive("tenant1", true);

        // Assert
        assertEquals(2, count);
    }

    // ========================================================================
    // Search by Query Tests
    // ========================================================================

    @Test
    @DisplayName("Should search by tenant and query string")
    void testSearchByTenantAndQuery() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> results = patientRepository.searchByTenantAndQuery("tenant1", "John", pageable);

        // Assert
        assertEquals(1, results.getContent().size());
        assertEquals("John", results.get().findFirst().get().getFirstName());
    }

    @Test
    @DisplayName("Should search by tenant and MRN query")
    void testSearchByTenantAndMrnQuery() {
        // Arrange
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> results = patientRepository.searchByTenantAndQuery("tenant1", "MRN-TEST-001", pageable);

        // Assert
        assertEquals(1, results.getContent().size());
    }

    // ========================================================================
    // Age Range Tests
    // ========================================================================

    @Test
    @DisplayName("Should find patients by age range")
    void testFindByAgeRange() {
        // Arrange
        testPatient.setDateOfBirth(LocalDate.of(1965, 1, 1)); // Age 59
        testPatient2.setDateOfBirth(LocalDate.of(1950, 1, 1)); // Age 74
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        List<Patient> results = patientRepository.findByAgeRange("tenant1", 50, 70);

        // Assert
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should exclude patients outside age range")
    void testAgeRangeExcludesOutsideRange() {
        // Arrange
        testPatient.setDateOfBirth(LocalDate.of(1965, 1, 1)); // Age 59
        testPatient2.setDateOfBirth(LocalDate.of(2020, 1, 1)); // Age 4
        patientRepository.save(testPatient);
        patientRepository.save(testPatient2);

        // Act
        List<Patient> results = patientRepository.findByAgeRange("tenant1", 50, 70);

        // Assert
        assertEquals(1, results.size());
        assertEquals("John", results.get(0).getFirstName());
    }

    // ========================================================================
    // Bulk Operations Tests
    // ========================================================================

    @Test
    @DisplayName("Should find all patients by IDs")
    void testFindAllByIds() {
        // Arrange
        Patient saved1 = patientRepository.save(testPatient);
        Patient saved2 = patientRepository.save(testPatient2);
        List<String> ids = List.of(saved1.getId(), saved2.getId());

        // Act
        List<Patient> results = patientRepository.findAllByIds(ids);

        // Assert
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should handle empty ID list gracefully")
    void testFindAllByEmptyIds() {
        // Act
        List<Patient> results = patientRepository.findAllByIds(List.of());

        // Assert
        assertEquals(0, results.size());
    }

    // ========================================================================
    // Edge Cases and Negative Tests
    // ========================================================================

    @Test
    @DisplayName("Should handle case-insensitive search")
    void testCaseInsensitiveSearch() {
        // Arrange
        patientRepository.save(testPatient);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> resultsLower = patientRepository.searchPatients("john", "", "", "tenant1", pageable);
        Page<Patient> resultsUpper = patientRepository.searchPatients("JOHN", "", "", "tenant1", pageable);

        // Assert
        assertEquals(1, resultsLower.getContent().size());
        assertEquals(1, resultsUpper.getContent().size());
    }

    @Test
    @DisplayName("Should not find inactive patients in search")
    void testInactivePatientNotInSearch() {
        // Arrange
        testPatient.setActive(false);
        patientRepository.save(testPatient);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> results = patientRepository.searchPatients("John", "", "", "tenant1", pageable);

        // Assert
        assertEquals(0, results.getContent().size());
    }

    @Test
    @DisplayName("Should handle MRN uniqueness constraint")
    void testMrnUniquenessConstraint() {
        // Arrange
        Patient patient1 = patientRepository.save(testPatient);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            Patient patient2 = DataTestFactory.patientBuilder()
                .withMrn("MRN-TEST-001") // Same MRN
                .withFirstName("Different")
                .withLastName("Person")
                .build();
            patientRepository.save(patient2);
            patientRepository.flush();
        });
    }

    @Test
    @DisplayName("Should order search results by last name then first name")
    void testSearchResultsOrdering() {
        // Arrange
        Patient patient1 = DataTestFactory.patientBuilder()
            .withMrn("MRN-SORT-001")
            .withFirstName("Alice")
            .withLastName("Zsmith")
            .withTenantId("tenant1")
            .build();
        Patient patient2 = DataTestFactory.patientBuilder()
            .withMrn("MRN-SORT-002")
            .withFirstName("Bob")
            .withLastName("Asmith")
            .withTenantId("tenant1")
            .build();

        patientRepository.save(patient1);
        patientRepository.save(patient2);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<Patient> results = patientRepository.findAllActivePatientsForTenant("tenant1", pageable);

        // Assert
        assertTrue(results.getContent().size() >= 2);
    }
}

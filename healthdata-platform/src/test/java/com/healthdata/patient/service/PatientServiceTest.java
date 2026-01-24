package com.healthdata.patient.service;

import com.healthdata.DataTestFactory;
import com.healthdata.api.dto.PatientDTO;
import com.healthdata.patient.domain.Patient;
import com.healthdata.patient.events.PatientCreatedEvent;
import com.healthdata.patient.events.PatientUpdatedEvent;
import com.healthdata.patient.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Tests for PatientService
 * Tests all CRUD operations, business logic, and error handling
 * 40+ test methods covering all service functionality
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Service Tests")
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PatientService patientService;

    private Patient testPatient;
    private Patient testPatient2;

    @BeforeEach
    void setUp() {
        testPatient = DataTestFactory.patientBuilder()
            .withId("patient-1")
            .withMrn("MRN-001")
            .withFirstName("John")
            .withLastName("Smith")
            .withTenantId("tenant1")
            .build();

        testPatient2 = DataTestFactory.patientBuilder()
            .withId("patient-2")
            .withMrn("MRN-002")
            .withFirstName("Jane")
            .withLastName("Doe")
            .withTenantId("tenant1")
            .build();
    }

    // ========================================================================
    // CREATE Operations Tests
    // ========================================================================

    @Nested
    @DisplayName("Create Patient Tests")
    class CreatePatientTests {

        @Test
        @DisplayName("Should create patient successfully")
        void testCreatePatientSuccess() {
            // Arrange
            when(patientRepository.existsByMrn(testPatient.getMrn())).thenReturn(false);
            when(patientRepository.save(testPatient)).thenReturn(testPatient);

            // Act
            Patient created = patientService.createPatient(testPatient);

            // Assert
            assertNotNull(created);
            assertEquals(testPatient.getId(), created.getId());
            assertEquals(testPatient.getMrn(), created.getMrn());
            verify(patientRepository).save(testPatient);
            verify(eventPublisher).publishEvent(any(PatientCreatedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when MRN is duplicate")
        void testCreatePatientWithDuplicateMrn() {
            // Arrange
            when(patientRepository.existsByMrn(testPatient.getMrn())).thenReturn(true);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> patientService.createPatient(testPatient));
            verify(patientRepository, never()).save(testPatient);
        }

        @Test
        @DisplayName("Should throw exception when patient data is invalid")
        void testCreatePatientWithInvalidData() {
            // Arrange
            testPatient.setFirstName(null);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> patientService.createPatient(testPatient));
            verify(patientRepository, never()).save(testPatient);
        }

        @Test
        @DisplayName("Should throw exception when tenant ID is missing")
        void testCreatePatientWithoutTenant() {
            // Arrange
            testPatient.setTenantId(null);
            when(patientRepository.existsByMrn(testPatient.getMrn())).thenReturn(false);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> patientService.createPatient(testPatient));
        }

        @Test
        @DisplayName("Should throw exception when MRN is missing")
        void testCreatePatientWithoutMrn() {
            // Arrange
            testPatient.setMrn(null);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> patientService.createPatient(testPatient));
        }

        @Test
        @DisplayName("Should publish PatientCreatedEvent")
        void testCreatePatientPublishesEvent() {
            // Arrange
            when(patientRepository.existsByMrn(testPatient.getMrn())).thenReturn(false);
            when(patientRepository.save(testPatient)).thenReturn(testPatient);

            // Act
            patientService.createPatient(testPatient);

            // Assert
            ArgumentCaptor<PatientCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PatientCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            PatientCreatedEvent event = eventCaptor.getValue();
            assertNotNull(event);
        }
    }

    // ========================================================================
    // READ Operations Tests
    // ========================================================================

    @Nested
    @DisplayName("Get Patient Tests")
    class GetPatientTests {

        @Test
        @DisplayName("Should get patient by ID successfully")
        void testGetPatientSuccess() {
            // Arrange
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(testPatient));

            // Act
            Optional<Patient> result = patientService.getPatient("patient-1");

            // Assert
            assertTrue(result.isPresent());
            assertEquals(testPatient.getId(), result.get().getId());
        }

        @Test
        @DisplayName("Should return empty when patient not found")
        void testGetPatientNotFound() {
            // Arrange
            when(patientRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act
            Optional<Patient> result = patientService.getPatient("non-existent");

            // Assert
            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("Should throw exception for null patient ID")
        void testGetPatientWithNullId() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> patientService.getPatient(null));
        }

        @Test
        @DisplayName("Should throw exception for empty patient ID")
        void testGetPatientWithEmptyId() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> patientService.getPatient(""));
        }

        @Test
        @DisplayName("Should get patient or throw exception when not found")
        void testGetPatientOrThrow() {
            // Arrange
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(testPatient));

            // Act
            Patient result = patientService.getPatientOrThrow("patient-1");

            // Assert
            assertEquals(testPatient.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw PatientNotFoundException when patient not found")
        void testGetPatientOrThrowNotFound() {
            // Arrange
            when(patientRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PatientNotFoundException.class, () -> patientService.getPatientOrThrow("non-existent"));
        }
    }

    // ========================================================================
    // UPDATE Operations Tests
    // ========================================================================

    @Nested
    @DisplayName("Update Patient Tests")
    class UpdatePatientTests {

        @Test
        @DisplayName("Should update patient successfully")
        void testUpdatePatientSuccess() {
            // Arrange
            Patient updatedData = DataTestFactory.patientBuilder()
                .withFirstName("John Updated")
                .withLastName("Smith")
                .build();

            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(testPatient));
            when(patientRepository.save(any())).thenReturn(testPatient);

            // Act
            Patient result = patientService.updatePatient("patient-1", updatedData);

            // Assert
            assertNotNull(result);
            verify(patientRepository).save(any());
            verify(eventPublisher).publishEvent(any(PatientUpdatedEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when patient not found during update")
        void testUpdatePatientNotFound() {
            // Arrange
            when(patientRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PatientNotFoundException.class,
                () -> patientService.updatePatient("non-existent", testPatient));
        }

        @Test
        @DisplayName("Should throw exception when update data is invalid")
        void testUpdatePatientWithInvalidData() {
            // Arrange
            Patient invalidPatient = DataTestFactory.patientBuilder()
                .withFirstName(null)
                .build();

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> patientService.updatePatient("patient-1", invalidPatient));
        }

        @Test
        @DisplayName("Should publish PatientUpdatedEvent on successful update")
        void testUpdatePatientPublishesEvent() {
            // Arrange
            Patient updatedData = DataTestFactory.patientBuilder()
                .withFirstName("Updated Name")
                .withLastName("Smith")
                .build();

            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(testPatient));
            when(patientRepository.save(any())).thenReturn(testPatient);

            // Act
            patientService.updatePatient("patient-1", updatedData);

            // Assert
            verify(eventPublisher).publishEvent(any(PatientUpdatedEvent.class));
        }
    }

    // ========================================================================
    // DELETE Operations Tests
    // ========================================================================

    @Nested
    @DisplayName("Delete Patient Tests")
    class DeletePatientTests {

        @Test
        @DisplayName("Should delete patient successfully")
        void testDeletePatientSuccess() {
            // Arrange
            when(patientRepository.existsById("patient-1")).thenReturn(true);

            // Act
            patientService.deletePatient("patient-1");

            // Assert
            verify(patientRepository).deleteById("patient-1");
        }

        @Test
        @DisplayName("Should throw exception when patient not found during delete")
        void testDeletePatientNotFound() {
            // Arrange
            when(patientRepository.existsById("non-existent")).thenReturn(false);

            // Act & Assert
            assertThrows(PatientNotFoundException.class,
                () -> patientService.deletePatient("non-existent"));
            verify(patientRepository, never()).deleteById(anyString());
        }
    }

    // ========================================================================
    // SEARCH Operations Tests
    // ========================================================================

    @Nested
    @DisplayName("Search Patient Tests")
    class SearchPatientTests {

        @Test
        @DisplayName("Should search patients with query")
        void testSearchPatientsByQuery() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> expectedPage = new PageImpl<>(List.of(testPatient), pageable, 1);
            when(patientRepository.searchByTenantAndQuery("tenant1", "John", pageable))
                .thenReturn(expectedPage);

            // Act
            Page<Patient> result = patientService.searchPatients("tenant1", "John", pageable);

            // Assert
            assertEquals(1, result.getContent().size());
            verify(patientRepository).searchByTenantAndQuery("tenant1", "John", pageable);
        }

        @Test
        @DisplayName("Should search patients without query returns all by tenant")
        void testSearchPatientsWithoutQuery() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> expectedPage = new PageImpl<>(List.of(testPatient, testPatient2), pageable, 1);
            when(patientRepository.findByTenantId("tenant1", pageable)).thenReturn(expectedPage);

            // Act
            Page<Patient> result = patientService.searchPatients("tenant1", null, pageable);

            // Assert
            assertEquals(2, result.getContent().size());
        }

        @Test
        @DisplayName("Should throw exception when tenant ID is null")
        void testSearchPatientsWithoutTenant() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> patientService.searchPatients(null, "query", PageRequest.of(0, 10)));
        }

        @Test
        @DisplayName("Should search by criteria")
        void testSearchPatientsByCriteria() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> expectedPage = new PageImpl<>(List.of(testPatient), pageable, 1);
            when(patientRepository.searchPatients("John", "Smith", "MRN-001", "tenant1", pageable))
                .thenReturn(expectedPage);

            // Act
            Page<Patient> result = patientService.searchPatientsByCriteria("tenant1", "John", "Smith", "MRN-001", pageable);

            // Assert
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Should find patient by MRN and tenant")
        void testFindByMrnAndTenant() {
            // Arrange
            when(patientRepository.findByMrnAndTenantId("MRN-001", "tenant1"))
                .thenReturn(Optional.of(testPatient));

            // Act
            Optional<Patient> result = patientService.findByMrn("MRN-001", "tenant1");

            // Assert
            assertTrue(result.isPresent());
            assertEquals(testPatient.getId(), result.get().getId());
        }

        @Test
        @DisplayName("Should throw exception when MRN is null")
        void testFindByMrnWithNullMrn() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> patientService.findByMrn(null, "tenant1"));
        }

        @Test
        @DisplayName("Should find patients by name and tenant")
        void testFindByNameAndTenant() {
            // Arrange
            when(patientRepository.findByFirstNameAndLastNameAndTenantId("John", "Smith", "tenant1"))
                .thenReturn(List.of(testPatient));

            // Act
            List<Patient> result = patientService.findByNameAndTenant("John", "Smith", "tenant1");

            // Assert
            assertEquals(1, result.size());
            assertEquals("John", result.get(0).getFirstName());
        }

        @Test
        @DisplayName("Should get all active patients for tenant")
        void testGetAllActivePatientsForTenant() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> expectedPage = new PageImpl<>(List.of(testPatient), pageable, 1);
            when(patientRepository.findAllActivePatientsForTenant("tenant1", pageable))
                .thenReturn(expectedPage);

            // Act
            Page<Patient> result = patientService.getAllActivePatients("tenant1", pageable);

            // Assert
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Should find patients by age range using database-level filtering")
        void testFindByAgeRange() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate maxBirthDate = today.minusYears(40);
            LocalDate minBirthDate = today.minusYears(61).plusDays(1);

            when(patientRepository.findByTenantIdAndAgeRange("tenant1", minBirthDate, maxBirthDate))
                .thenReturn(List.of(testPatient));

            // Act
            List<Patient> result = patientService.findByAgeRange("tenant1", 40, 60);

            // Assert
            assertEquals(1, result.size());
            verify(patientRepository).findByTenantIdAndAgeRange("tenant1", minBirthDate, maxBirthDate);
        }

        @Test
        @DisplayName("Should throw exception for invalid age range (min > max)")
        void testFindByAgeRangeInvalid() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> patientService.findByAgeRange("tenant1", 60, 40));
        }

        @Test
        @DisplayName("Should throw exception for negative age range")
        void testFindByAgeRangeNegative() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> patientService.findByAgeRange("tenant1", -5, 30));
        }

        @Test
        @DisplayName("Should handle age range with boundary values (0-120)")
        void testFindByAgeRangeBoundary() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate maxBirthDate = today.minusYears(0);
            LocalDate minBirthDate = today.minusYears(121).plusDays(1);

            when(patientRepository.findByTenantIdAndAgeRange("tenant1", minBirthDate, maxBirthDate))
                .thenReturn(List.of(testPatient));

            // Act
            List<Patient> result = patientService.findByAgeRange("tenant1", 0, 120);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should handle exact age match (minAge = maxAge)")
        void testFindByExactAge() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate maxBirthDate = today.minusYears(30);
            LocalDate minBirthDate = today.minusYears(31).plusDays(1);

            when(patientRepository.findByTenantIdAndAgeRange("tenant1", minBirthDate, maxBirthDate))
                .thenReturn(List.of(testPatient));

            // Act
            List<Patient> result = patientService.findByAgeRange("tenant1", 30, 30);

            // Assert
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should find recently active patients")
        void testFindRecentlyActivePatients() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(patientRepository.findRecentlyActivePatients("tenant1", pageable))
                .thenReturn(List.of(testPatient));

            // Act
            List<Patient> result = patientService.findRecentlyActivePatients("tenant1", pageable);

            // Assert
            assertEquals(1, result.size());
        }
    }

    // ========================================================================
    // VALIDATION Tests
    // ========================================================================

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should validate patient data successfully")
        void testValidatePatientDataSuccess() {
            // Act
            boolean valid = patientService.validatePatientData(testPatient);

            // Assert
            assertTrue(valid);
        }

        @Test
        @DisplayName("Should reject null patient")
        void testValidateNullPatient() {
            // Act
            boolean valid = patientService.validatePatientData(null);

            // Assert
            assertFalse(valid);
        }

        @Test
        @DisplayName("Should reject patient without first name")
        void testValidatePatientWithoutFirstName() {
            // Arrange
            testPatient.setFirstName(null);

            // Act
            boolean valid = patientService.validatePatientData(testPatient);

            // Assert
            assertFalse(valid);
        }

        @Test
        @DisplayName("Should reject patient with future date of birth")
        void testValidatePatientWithFutureDateOfBirth() {
            // Arrange
            testPatient.setDateOfBirth(LocalDate.now().plusDays(1));

            // Act
            boolean valid = patientService.validatePatientData(testPatient);

            // Assert
            assertFalse(valid);
        }

        @Test
        @DisplayName("Should reject patient with unreasonable age")
        void testValidatePatientWithUnreasonableAge() {
            // Arrange
            testPatient.setDateOfBirth(LocalDate.of(1800, 1, 1));

            // Act
            boolean valid = patientService.validatePatientData(testPatient);

            // Assert
            assertFalse(valid);
        }

        @Test
        @DisplayName("Should reject patient without MRN")
        void testValidatePatientWithoutMrn() {
            // Arrange
            testPatient.setMrn(null);

            // Act
            boolean valid = patientService.validatePatientData(testPatient);

            // Assert
            assertFalse(valid);
        }

        @Test
        @DisplayName("Should reject patient without gender")
        void testValidatePatientWithoutGender() {
            // Arrange
            testPatient.setGender(null);

            // Act
            boolean valid = patientService.validatePatientData(testPatient);

            // Assert
            assertFalse(valid);
        }
    }

    // ========================================================================
    // DEACTIVATION Tests
    // ========================================================================

    @Nested
    @DisplayName("Deactivation Tests")
    class DeactivationTests {

        @Test
        @DisplayName("Should deactivate patient successfully")
        void testDeactivatePatientSuccess() {
            // Arrange
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(testPatient));
            when(patientRepository.save(testPatient)).thenReturn(testPatient);

            // Act
            patientService.deactivatePatient("patient-1", "Patient requested deactivation");

            // Assert
            verify(patientRepository).save(testPatient);
        }

        @Test
        @DisplayName("Should throw exception when deactivating non-existent patient")
        void testDeactivateNonExistentPatient() {
            // Arrange
            when(patientRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PatientNotFoundException.class,
                () -> patientService.deactivatePatient("non-existent", "reason"));
        }

        @Test
        @DisplayName("Should reactivate patient successfully")
        void testReactivatePatientSuccess() {
            // Arrange
            testPatient.setActive(false);
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(testPatient));
            when(patientRepository.save(testPatient)).thenReturn(testPatient);

            // Act
            patientService.reactivatePatient("patient-1");

            // Assert
            verify(patientRepository).save(testPatient);
        }
    }

    // ========================================================================
    // METRICS Tests
    // ========================================================================

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("Should get active patient count")
        void testGetActivePatientCount() {
            // Arrange
            when(patientRepository.countActivePatientsByTenant("tenant1")).thenReturn(5L);

            // Act
            long count = patientService.getActivePatientCount("tenant1");

            // Assert
            assertEquals(5L, count);
        }

        @Test
        @DisplayName("Should get inactive patient count")
        void testGetInactivePatientCount() {
            // Arrange
            when(patientRepository.countByTenantIdAndActive("tenant1", false)).thenReturn(2L);

            // Act
            long count = patientService.getInactivePatientCount("tenant1");

            // Assert
            assertEquals(2L, count);
        }

        @Test
        @DisplayName("Should get total patient count")
        void testGetTotalPatientCount() {
            // Arrange
            when(patientRepository.countByTenantIdAndActive("tenant1", true)).thenReturn(5L);
            when(patientRepository.countByTenantIdAndActive("tenant1", false)).thenReturn(2L);

            // Act
            long count = patientService.getTotalPatientCount("tenant1");

            // Assert
            assertEquals(7L, count);
        }

        @Test
        @DisplayName("Should throw exception when tenant ID is null for metrics")
        void testGetActivePatientCountWithoutTenant() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                () -> patientService.getActivePatientCount(null));
        }
    }

    // ========================================================================
    // BATCH Operations Tests
    // ========================================================================

    @Nested
    @DisplayName("Batch Operations Tests")
    class BatchOperationsTests {

        @Test
        @DisplayName("Should get patients by IDs")
        void testGetPatientsByIds() {
            // Arrange
            List<String> ids = List.of("patient-1", "patient-2");
            when(patientRepository.findAllById(ids)).thenReturn(List.of(testPatient, testPatient2));

            // Act
            List<Patient> result = patientService.getPatientsByIds(ids);

            // Assert
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Should handle empty ID list")
        void testGetPatientsByEmptyIds() {
            // Act
            List<Patient> result = patientService.getPatientsByIds(List.of());

            // Assert
            assertEquals(0, result.size());
            verify(patientRepository, never()).findAllById(any());
        }

        @Test
        @DisplayName("Should get patient DTOs by IDs")
        void testGetPatientDTOsByIds() {
            // Arrange
            List<String> ids = List.of("patient-1");
            when(patientRepository.findAllById(ids)).thenReturn(List.of(testPatient));

            // Act
            List<PatientDTO> result = patientService.getPatientDTOsByIds(ids);

            // Assert
            assertEquals(1, result.size());
            assertEquals("John", result.get(0).getFirstName());
        }
    }

    // ========================================================================
    // EXISTENCE CHECKS Tests
    // ========================================================================

    @Nested
    @DisplayName("Existence Checks Tests")
    class ExistenceChecksTests {

        @Test
        @DisplayName("Should check if patient exists")
        void testPatientExists() {
            // Arrange
            when(patientRepository.existsById("patient-1")).thenReturn(true);

            // Act
            boolean exists = patientService.patientExists("patient-1");

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("Should check if MRN exists")
        void testMrnExists() {
            // Arrange
            when(patientRepository.existsByMrn("MRN-001")).thenReturn(true);

            // Act
            boolean exists = patientService.mrnExists("MRN-001");

            // Assert
            assertTrue(exists);
        }

        @Test
        @DisplayName("Should check if patient is active")
        void testIsPatientActive() {
            // Arrange
            testPatient.setActive(true);
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(testPatient));

            // Act
            boolean active = patientService.isPatientActive("patient-1");

            // Assert
            assertTrue(active);
        }

        @Test
        @DisplayName("Should throw exception when checking if non-existent patient is active")
        void testIsPatientActiveNotFound() {
            // Arrange
            when(patientRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PatientNotFoundException.class,
                () -> patientService.isPatientActive("non-existent"));
        }
    }

    // ========================================================================
    // DTO CONVERSION Tests
    // ========================================================================

    @Nested
    @DisplayName("DTO Conversion Tests")
    class DTOConversionTests {

        @Test
        @DisplayName("Should get patient with associations as DTO")
        void testGetPatientWithAssociations() {
            // Arrange
            when(patientRepository.findById("patient-1")).thenReturn(Optional.of(testPatient));

            // Act
            PatientDTO result = patientService.getPatientWithAssociations("patient-1");

            // Assert
            assertNotNull(result);
            assertEquals(testPatient.getId(), result.getId());
            assertEquals(testPatient.getFirstName(), result.getFirstName());
        }

        @Test
        @DisplayName("Should throw exception when getting DTO for non-existent patient")
        void testGetPatientWithAssociationsNotFound() {
            // Arrange
            when(patientRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PatientNotFoundException.class,
                () -> patientService.getPatientWithAssociations("non-existent"));
        }
    }

    // ========================================================================
    // Edge Cases Tests
    // ========================================================================

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle patient with special characters in name")
        void testCreatePatientWithSpecialCharacters() {
            // Arrange
            testPatient.setFirstName("O'Brien-Smith");
            testPatient.setLastName("Müller-García");
            when(patientRepository.existsByMrn(testPatient.getMrn())).thenReturn(false);
            when(patientRepository.save(testPatient)).thenReturn(testPatient);

            // Act
            Patient result = patientService.createPatient(testPatient);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle patient with whitespace in names")
        void testCreatePatientWithWhitespace() {
            // Arrange
            testPatient.setFirstName("  John  ");
            testPatient.setLastName("  Smith  ");
            when(patientRepository.existsByMrn(testPatient.getMrn())).thenReturn(false);
            when(patientRepository.save(testPatient)).thenReturn(testPatient);

            // Act
            Patient result = patientService.createPatient(testPatient);

            // Assert
            assertNotNull(result);
        }

        @Test
        @DisplayName("Should handle search with empty query string")
        void testSearchWithEmptyQuery() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Patient> expectedPage = new PageImpl<>(List.of(), pageable, 0);
            when(patientRepository.findByTenantId("tenant1", pageable)).thenReturn(expectedPage);

            // Act
            Page<Patient> result = patientService.searchPatients("tenant1", "", pageable);

            // Assert
            assertEquals(0, result.getContent().size());
        }
    }
}

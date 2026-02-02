package com.healthdata.patient.contracts;

import com.healthdata.patient.entity.PatientDemographicsEntity;
import com.healthdata.patient.repository.PatientDemographicsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * State setup helper for Patient Service contract tests.
 *
 * <p>Provides methods to set up patient data for various provider states
 * required by consumer contract tests. Uses deterministic UUIDs to ensure
 * consistent test data across contract verification runs.
 *
 * <p>All patient data is created with the test tenant ID to ensure proper
 * multi-tenant isolation during contract verification.
 *
 * @see PatientServiceProviderTest
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PatientContractStateSetup {

    /**
     * Deterministic UUID for John Doe test patient (FHIR-compliant string format).
     * This ID is referenced in consumer contracts and must remain stable.
     */
    public static final String PATIENT_JOHN_DOE_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    /**
     * Deterministic UUID for Jane Smith test patient.
     */
    public static final String PATIENT_JANE_SMITH_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    /**
     * Test tenant ID for contract tests.
     * All test data is associated with this tenant.
     */
    public static final String TEST_TENANT_ID = "test-tenant-contracts";

    /**
     * Test MRN for John Doe patient.
     */
    public static final String TEST_MRN_JOHN_DOE = "MRN-12345";

    /**
     * Test MRN for Jane Smith patient.
     */
    public static final String TEST_MRN_JANE_SMITH = "MRN-67890";

    private final PatientDemographicsRepository patientRepository;

    /**
     * Sets up the John Doe test patient.
     *
     * <p>Creates a patient with:
     * <ul>
     *   <li>ID: f47ac10b-58cc-4372-a567-0e02b2c3d479</li>
     *   <li>Name: John Doe</li>
     *   <li>MRN: MRN-12345</li>
     *   <li>DOB: 1980-05-15</li>
     *   <li>Gender: male</li>
     *   <li>Active: true</li>
     * </ul>
     *
     * @return the created patient entity
     */
    @Transactional
    public PatientDemographicsEntity setupPatientJohnDoe() {
        log.info("Setting up contract test patient: John Doe (ID: {})", PATIENT_JOHN_DOE_ID);

        UUID patientId = UUID.fromString(PATIENT_JOHN_DOE_ID);

        // Check if already exists
        return patientRepository.findByIdAndTenantId(patientId, TEST_TENANT_ID)
            .orElseGet(() -> {
                PatientDemographicsEntity patient = PatientDemographicsEntity.builder()
                    .id(patientId)
                    .tenantId(TEST_TENANT_ID)
                    .fhirPatientId(PATIENT_JOHN_DOE_ID)
                    .mrn(TEST_MRN_JOHN_DOE)
                    .firstName("John")
                    .middleName("Michael")
                    .lastName("Doe")
                    .dateOfBirth(LocalDate.of(1980, 5, 15))
                    .gender("male")
                    .race("White")
                    .ethnicity("Non-Hispanic")
                    .preferredLanguage("en")
                    .email("john.doe@example.com")
                    .phone("555-123-4567")
                    .addressLine1("123 Main Street")
                    .addressLine2("Apt 4B")
                    .city("Springfield")
                    .state("IL")
                    .zipCode("62701")
                    .country("USA")
                    .active(true)
                    .deceased(false)
                    .pcpId("pcp-001")
                    .pcpName("Dr. Jane Smith")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

                return patientRepository.save(patient);
            });
    }

    /**
     * Sets up the Jane Smith test patient.
     *
     * <p>Creates a patient with:
     * <ul>
     *   <li>ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890</li>
     *   <li>Name: Jane Smith</li>
     *   <li>MRN: MRN-67890</li>
     *   <li>DOB: 1975-03-22</li>
     *   <li>Gender: female</li>
     *   <li>Active: true</li>
     * </ul>
     *
     * @return the created patient entity
     */
    @Transactional
    public PatientDemographicsEntity setupPatientJaneSmith() {
        log.info("Setting up contract test patient: Jane Smith (ID: {})", PATIENT_JANE_SMITH_ID);

        UUID patientId = UUID.fromString(PATIENT_JANE_SMITH_ID);

        // Check if already exists
        return patientRepository.findByIdAndTenantId(patientId, TEST_TENANT_ID)
            .orElseGet(() -> {
                PatientDemographicsEntity patient = PatientDemographicsEntity.builder()
                    .id(patientId)
                    .tenantId(TEST_TENANT_ID)
                    .fhirPatientId(PATIENT_JANE_SMITH_ID)
                    .mrn(TEST_MRN_JANE_SMITH)
                    .firstName("Jane")
                    .middleName("Elizabeth")
                    .lastName("Smith")
                    .dateOfBirth(LocalDate.of(1975, 3, 22))
                    .gender("female")
                    .race("Asian")
                    .ethnicity("Non-Hispanic")
                    .preferredLanguage("en")
                    .email("jane.smith@example.com")
                    .phone("555-987-6543")
                    .addressLine1("456 Oak Avenue")
                    .city("Riverside")
                    .state("CA")
                    .zipCode("92501")
                    .country("USA")
                    .active(true)
                    .deceased(false)
                    .pcpId("pcp-002")
                    .pcpName("Dr. Robert Johnson")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

                return patientRepository.save(patient);
            });
    }

    /**
     * Cleans up all test data created during contract verification.
     *
     * <p>Removes all patients associated with the test tenant ID.
     * This should be called after each test to ensure clean state.
     */
    @Transactional
    public void cleanupTestData() {
        log.info("Cleaning up contract test data for tenant: {}", TEST_TENANT_ID);

        // Delete specific test patients by ID
        UUID johnDoeId = UUID.fromString(PATIENT_JOHN_DOE_ID);
        UUID janeSmithId = UUID.fromString(PATIENT_JANE_SMITH_ID);

        patientRepository.findByIdAndTenantId(johnDoeId, TEST_TENANT_ID)
            .ifPresent(patientRepository::delete);

        patientRepository.findByIdAndTenantId(janeSmithId, TEST_TENANT_ID)
            .ifPresent(patientRepository::delete);

        log.info("Contract test data cleanup completed");
    }
}

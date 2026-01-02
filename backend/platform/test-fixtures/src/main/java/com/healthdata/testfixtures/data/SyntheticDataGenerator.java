package com.healthdata.testfixtures.data;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HIPAA-Compliant Synthetic Data Generator for HDIM Backend Tests.
 * <p>
 * This utility generates clearly synthetic test data that:
 * <ul>
 *   <li>Uses obvious test patterns (TEST-MRN-xxxxxx, Test-Patient-xxx)</li>
 *   <li>Never generates realistic PHI that could be confused with real data</li>
 *   <li>Includes proper tenant isolation for multi-tenant testing</li>
 *   <li>Follows FHIR R4 resource structure</li>
 * </ul>
 * <p>
 * <strong>HIPAA Compliance:</strong> All generated data is clearly synthetic and
 * cannot be mistaken for real patient information. MRN patterns start with "TEST-MRN-",
 * patient names start with "Test-Patient-", and all identifiers use UUID-based suffixes.
 * <p>
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a synthetic patient for testing
 * Patient patient = SyntheticDataGenerator.createPatient("tenant-001");
 * assertThat(patient.getIdentifierFirstRep().getValue()).startsWith("TEST-MRN-");
 *
 * // Create a patient with specific ID
 * Patient patient = SyntheticDataGenerator.createPatient("tenant-001", "patient-123");
 *
 * // Create related FHIR resources
 * Observation obs = SyntheticDataGenerator.createObservation("tenant-001", patient);
 * Condition cond = SyntheticDataGenerator.createCondition("tenant-001", patient);
 * }</pre>
 * <p>
 * <h2>Multi-Tenant Testing</h2>
 * All generated resources include tenant context. Use different tenant IDs to verify
 * tenant isolation in your tests:
 * <pre>{@code
 * Patient tenant1Patient = SyntheticDataGenerator.createPatient("tenant-001");
 * Patient tenant2Patient = SyntheticDataGenerator.createPatient("tenant-002");
 *
 * // Verify tenant isolation in repository
 * List<Patient> tenant1Results = repository.findByTenantId("tenant-001");
 * assertThat(tenant1Results).contains(tenant1Patient);
 * assertThat(tenant1Results).doesNotContain(tenant2Patient);
 * }</pre>
 *
 * @see BaseTestContainersConfiguration
 * @since 1.0
 */
public final class SyntheticDataGenerator {

    private static final Random RANDOM = new Random();
    private static final AtomicLong SEQUENCE = new AtomicLong(100000);

    // Synthetic name components - clearly fake
    private static final String[] FIRST_NAMES = {
            "Test", "Sample", "Demo", "Mock", "Synthetic"
    };

    private static final String[] LAST_NAME_SUFFIXES = {
            "Patient", "User", "Subject", "Person", "Individual"
    };

    // Common LOINC codes for observations
    private static final String LOINC_SYSTEM = "http://loinc.org";
    private static final String SNOMED_SYSTEM = "http://snomed.info/sct";

    private SyntheticDataGenerator() {
        // Utility class - prevent instantiation
    }

    // =========================================================================
    // Patient Generation
    // =========================================================================

    /**
     * Creates a synthetic FHIR Patient with auto-generated ID.
     *
     * @param tenantId the tenant identifier for multi-tenant isolation
     * @return a synthetic Patient resource
     */
    public static Patient createPatient(String tenantId) {
        return createPatient(tenantId, UUID.randomUUID().toString());
    }

    /**
     * Creates a synthetic FHIR Patient with specified ID.
     *
     * @param tenantId  the tenant identifier for multi-tenant isolation
     * @param patientId the patient identifier
     * @return a synthetic Patient resource
     */
    public static Patient createPatient(String tenantId, String patientId) {
        Patient patient = new Patient();
        patient.setId(patientId);

        // MRN - clearly synthetic pattern: TEST-MRN-xxxxxx
        String mrn = generateMrn();
        patient.addIdentifier()
                .setSystem("urn:hdim:mrn:" + tenantId)
                .setValue(mrn)
                .setType(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                                .setCode("MR")
                                .setDisplay("Medical Record Number")));

        // Name - clearly synthetic: "Test-Patient-xxx Synthetic-yyy"
        String firstName = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAME_SUFFIXES[RANDOM.nextInt(LAST_NAME_SUFFIXES.length)];
        String uniqueSuffix = String.format("%06d", SEQUENCE.incrementAndGet());

        patient.addName()
                .setFamily(lastName + "-" + uniqueSuffix)
                .addGiven(firstName + "-Patient");

        // Gender - random distribution
        patient.setGender(RANDOM.nextBoolean()
                ? Enumerations.AdministrativeGender.MALE
                : Enumerations.AdministrativeGender.FEMALE);

        // Birth date - random between 1940-2010
        patient.setBirthDate(generateBirthDate());

        // Meta - include tenant in meta for reference
        patient.getMeta()
                .addTag()
                .setSystem("urn:hdim:tenant")
                .setCode(tenantId)
                .setDisplay("Tenant: " + tenantId);

        return patient;
    }

    /**
     * Creates a batch of synthetic patients for load testing.
     *
     * @param tenantId the tenant identifier
     * @param count    number of patients to create
     * @return array of synthetic Patient resources
     */
    public static Patient[] createPatients(String tenantId, int count) {
        Patient[] patients = new Patient[count];
        for (int i = 0; i < count; i++) {
            patients[i] = createPatient(tenantId);
        }
        return patients;
    }

    // =========================================================================
    // Observation Generation
    // =========================================================================

    /**
     * Creates a synthetic blood pressure Observation for a patient.
     *
     * @param tenantId the tenant identifier
     * @param patient  the patient reference
     * @return a synthetic Observation resource
     */
    public static Observation createBloodPressureObservation(String tenantId, Patient patient) {
        Observation obs = createBaseObservation(tenantId, patient);

        // LOINC code for Blood pressure panel
        obs.getCode()
                .addCoding()
                .setSystem(LOINC_SYSTEM)
                .setCode("85354-9")
                .setDisplay("Blood pressure panel");

        // Systolic component (110-140 mmHg)
        obs.addComponent()
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(LOINC_SYSTEM)
                                .setCode("8480-6")
                                .setDisplay("Systolic blood pressure")))
                .setValue(new Quantity()
                        .setValue(110 + RANDOM.nextInt(31))
                        .setUnit("mmHg")
                        .setSystem("http://unitsofmeasure.org")
                        .setCode("mm[Hg]"));

        // Diastolic component (60-90 mmHg)
        obs.addComponent()
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(LOINC_SYSTEM)
                                .setCode("8462-4")
                                .setDisplay("Diastolic blood pressure")))
                .setValue(new Quantity()
                        .setValue(60 + RANDOM.nextInt(31))
                        .setUnit("mmHg")
                        .setSystem("http://unitsofmeasure.org")
                        .setCode("mm[Hg]"));

        return obs;
    }

    /**
     * Creates a synthetic HbA1c Observation for diabetes testing.
     *
     * @param tenantId the tenant identifier
     * @param patient  the patient reference
     * @return a synthetic Observation resource
     */
    public static Observation createHbA1cObservation(String tenantId, Patient patient) {
        Observation obs = createBaseObservation(tenantId, patient);

        // LOINC code for HbA1c
        obs.getCode()
                .addCoding()
                .setSystem(LOINC_SYSTEM)
                .setCode("4548-4")
                .setDisplay("Hemoglobin A1c/Hemoglobin.total in Blood");

        // Value (4.0-10.0%)
        double value = 4.0 + (RANDOM.nextDouble() * 6.0);
        obs.setValue(new Quantity()
                .setValue(Math.round(value * 10) / 10.0)
                .setUnit("%")
                .setSystem("http://unitsofmeasure.org")
                .setCode("%"));

        return obs;
    }

    /**
     * Creates a generic Observation with specified LOINC code.
     *
     * @param tenantId  the tenant identifier
     * @param patient   the patient reference
     * @param loincCode the LOINC code
     * @param display   the display name
     * @param value     the numeric value
     * @param unit      the unit of measurement
     * @return a synthetic Observation resource
     */
    public static Observation createObservation(
            String tenantId,
            Patient patient,
            String loincCode,
            String display,
            double value,
            String unit) {

        Observation obs = createBaseObservation(tenantId, patient);

        obs.getCode()
                .addCoding()
                .setSystem(LOINC_SYSTEM)
                .setCode(loincCode)
                .setDisplay(display);

        obs.setValue(new Quantity()
                .setValue(value)
                .setUnit(unit)
                .setSystem("http://unitsofmeasure.org")
                .setCode(unit));

        return obs;
    }

    // =========================================================================
    // Condition Generation
    // =========================================================================

    /**
     * Creates a synthetic Condition (diagnosis) for a patient.
     *
     * @param tenantId the tenant identifier
     * @param patient  the patient reference
     * @return a synthetic Condition resource
     */
    public static Condition createCondition(String tenantId, Patient patient) {
        return createCondition(tenantId, patient, "38341003", "Hypertensive disorder");
    }

    /**
     * Creates a synthetic Condition with specified SNOMED code.
     *
     * @param tenantId   the tenant identifier
     * @param patient    the patient reference
     * @param snomedCode the SNOMED CT code
     * @param display    the display name
     * @return a synthetic Condition resource
     */
    public static Condition createCondition(
            String tenantId,
            Patient patient,
            String snomedCode,
            String display) {

        Condition condition = new Condition();
        condition.setId(UUID.randomUUID().toString());

        // Patient reference
        condition.setSubject(new Reference()
                .setReference("Patient/" + patient.getIdElement().getIdPart())
                .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));

        // Clinical status - active
        condition.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                        .setCode("active")
                        .setDisplay("Active")));

        // Verification status - confirmed
        condition.setVerificationStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                        .setCode("confirmed")
                        .setDisplay("Confirmed")));

        // Code - SNOMED CT
        condition.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem(SNOMED_SYSTEM)
                        .setCode(snomedCode)
                        .setDisplay(display)));

        // Onset - random date in past 5 years
        LocalDate onsetDate = LocalDate.now().minusDays(RANDOM.nextInt(1825));
        condition.setOnset(new org.hl7.fhir.r4.model.DateTimeType(
                Date.from(onsetDate.atStartOfDay(ZoneId.systemDefault()).toInstant())));

        // Meta - include tenant
        condition.getMeta()
                .addTag()
                .setSystem("urn:hdim:tenant")
                .setCode(tenantId)
                .setDisplay("Tenant: " + tenantId);

        return condition;
    }

    /**
     * Creates a diabetes condition for HEDIS measure testing.
     *
     * @param tenantId the tenant identifier
     * @param patient  the patient reference
     * @return a synthetic Condition for Type 2 Diabetes
     */
    public static Condition createDiabetesCondition(String tenantId, Patient patient) {
        return createCondition(tenantId, patient, "44054006", "Type 2 diabetes mellitus");
    }

    // =========================================================================
    // Utility Methods
    // =========================================================================

    /**
     * Generates a synthetic MRN with clearly test-identifiable pattern.
     * Pattern: TEST-MRN-XXXXXX where X is a digit.
     *
     * @return synthetic MRN string
     */
    public static String generateMrn() {
        return String.format("TEST-MRN-%06d", SEQUENCE.incrementAndGet());
    }

    /**
     * Generates a synthetic tenant ID for testing.
     * Pattern: tenant-test-XXX
     *
     * @return synthetic tenant ID
     */
    public static String generateTenantId() {
        return "tenant-test-" + String.format("%03d", RANDOM.nextInt(1000));
    }

    /**
     * Generates a synthetic user ID for testing.
     * Pattern: user-test-XXXXXX
     *
     * @return synthetic user ID
     */
    public static String generateUserId() {
        return "user-test-" + String.format("%06d", SEQUENCE.incrementAndGet());
    }

    /**
     * Resets the sequence counter for deterministic testing.
     * Use with caution - only in test setup methods.
     */
    public static void resetSequence() {
        SEQUENCE.set(100000);
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private static Observation createBaseObservation(String tenantId, Patient patient) {
        Observation obs = new Observation();
        obs.setId(UUID.randomUUID().toString());

        // Status
        obs.setStatus(Observation.ObservationStatus.FINAL);

        // Patient reference
        obs.setSubject(new Reference()
                .setReference("Patient/" + patient.getIdElement().getIdPart())
                .setDisplay(patient.getNameFirstRep().getNameAsSingleString()));

        // Effective date - within last 30 days
        LocalDate effectiveDate = LocalDate.now().minusDays(RANDOM.nextInt(30));
        obs.setEffective(new org.hl7.fhir.r4.model.DateTimeType(
                Date.from(effectiveDate.atStartOfDay(ZoneId.systemDefault()).toInstant())));

        // Meta - include tenant
        obs.getMeta()
                .addTag()
                .setSystem("urn:hdim:tenant")
                .setCode(tenantId)
                .setDisplay("Tenant: " + tenantId);

        return obs;
    }

    private static Date generateBirthDate() {
        // Generate birth date between 1940 and 2010
        int year = 1940 + RANDOM.nextInt(71);
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28); // Safe for all months

        LocalDate birthDate = LocalDate.of(year, month, day);
        return Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}

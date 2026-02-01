package com.healthdata.ingestion.generator;

import ca.uhn.fhir.context.FhirContext;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Simplified synthetic patient generator using Datafaker.
 * No templates - generates random realistic patient data.
 */
@Component
@Slf4j
public class SimpleSyntheticPatientGenerator {

    private final Faker faker = new Faker();
    private final FhirContext fhirContext = FhirContext.forR4();

    /**
     * Generate a cohort of patients as a single transaction Bundle.
     *
     * @param patientCount Number of patients to generate
     * @param tenantId Tenant identifier
     * @return FHIR Bundle containing all patients
     */
    public Bundle generateCohort(int patientCount, String tenantId) {
        log.info("Generating cohort of {} patients for tenant {}", patientCount, tenantId);

        Bundle cohortBundle = new Bundle();
        cohortBundle.setType(Bundle.BundleType.TRANSACTION);
        cohortBundle.setId(UUID.randomUUID().toString());

        for (int i = 0; i < patientCount; i++) {
            Bundle patientBundle = generateSinglePatient(tenantId);

            // Add all entries from patient bundle to cohort
            for (Bundle.BundleEntryComponent entry : patientBundle.getEntry()) {
                cohortBundle.addEntry(entry);
            }

            if ((i + 1) % 100 == 0) {
                log.debug("Generated {} of {} patients", i + 1, patientCount);
            }
        }

        log.info("Cohort generation complete: {} patients, {} total resources",
                 patientCount, cohortBundle.getEntry().size());
        return cohortBundle;
    }

    /**
     * Generate a single patient with associated clinical data.
     *
     * @param tenantId Tenant identifier
     * @return FHIR Bundle containing patient and clinical resources
     */
    private Bundle generateSinglePatient(String tenantId) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        bundle.setId(UUID.randomUUID().toString());

        // 1. Create Patient resource
        Patient patient = createPatient(tenantId);
        addResourceToBundle(bundle, patient, "Patient");

        // 2. Add 2-4 conditions
        int conditionCount = faker.number().numberBetween(2, 5);
        for (int i = 0; i < conditionCount; i++) {
            Condition condition = createCondition(patient);
            addResourceToBundle(bundle, condition, "Condition");
        }

        // 3. Add 1-3 medications
        int medicationCount = faker.number().numberBetween(1, 4);
        for (int i = 0; i < medicationCount; i++) {
            MedicationRequest medication = createMedication(patient);
            addResourceToBundle(bundle, medication, "MedicationRequest");
        }

        // 4. Add 3-6 observations
        int observationCount = faker.number().numberBetween(3, 7);
        for (int i = 0; i < observationCount; i++) {
            Observation observation = createObservation(patient);
            addResourceToBundle(bundle, observation, "Observation");
        }

        // 5. Add 1-2 encounters
        int encounterCount = faker.number().numberBetween(1, 3);
        for (int i = 0; i < encounterCount; i++) {
            Encounter encounter = createEncounter(patient);
            addResourceToBundle(bundle, encounter, "Encounter");
        }

        return bundle;
    }

    private Patient createPatient(String tenantId) {
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID().toString());

        // Name
        HumanName name = new HumanName();
        name.setFamily(faker.name().lastName());
        name.addGiven(faker.name().firstName());
        patient.addName(name);

        // Gender
        boolean isMale = faker.bool().bool();
        patient.setGender(isMale ? Enumerations.AdministrativeGender.MALE : Enumerations.AdministrativeGender.FEMALE);

        // Birth date (age 18-85)
        int age = faker.number().numberBetween(18, 86);
        Date birthDate = Date.from(java.time.Instant.now().minus(java.time.Duration.ofDays(age * 365L)));
        patient.setBirthDate(birthDate);

        // Address
        Address address = new Address();
        address.addLine(faker.address().streetAddress());
        address.setCity(faker.address().city());
        address.setState(faker.address().stateAbbr());
        address.setPostalCode(faker.address().zipCode());
        address.setCountry("US");
        patient.addAddress(address);

        // Phone
        ContactPoint phone = new ContactPoint();
        phone.setSystem(ContactPoint.ContactPointSystem.PHONE);
        phone.setValue(faker.phoneNumber().phoneNumber());
        patient.addTelecom(phone);

        // Email
        ContactPoint email = new ContactPoint();
        email.setSystem(ContactPoint.ContactPointSystem.EMAIL);
        email.setValue(faker.internet().emailAddress());
        patient.addTelecom(email);

        // Tenant identifier
        Identifier tenantIdentifier = new Identifier();
        tenantIdentifier.setSystem("http://healthdata.com/tenant");
        tenantIdentifier.setValue(tenantId);
        patient.addIdentifier(tenantIdentifier);

        // MRN
        Identifier mrn = new Identifier();
        mrn.setSystem("http://hospital.example.org/mrn");
        mrn.setValue("MRN-" + faker.number().digits(8));
        patient.addIdentifier(mrn);

        return patient;
    }

    private Condition createCondition(Patient patient) {
        Condition condition = new Condition();
        condition.setId(UUID.randomUUID().toString());
        condition.setSubject(new Reference("Patient/" + patient.getId()));

        // Random common condition
        String[] conditions = {
            "E11.9|Type 2 Diabetes Mellitus",
            "I10|Essential Hypertension",
            "E78.5|Hyperlipidemia",
            "J44.9|Chronic Obstructive Pulmonary Disease",
            "M79.3|Chronic Pain",
            "F41.1|Generalized Anxiety Disorder",
            "E66.9|Obesity",
            "K21.9|Gastroesophageal Reflux Disease"
        };

        String selected = conditions[faker.number().numberBetween(0, conditions.length)];
        String[] parts = selected.split("\\|");

        CodeableConcept code = new CodeableConcept();
        code.addCoding()
            .setSystem("http://hl7.org/fhir/sid/icd-10-cm")
            .setCode(parts[0])
            .setDisplay(parts[1]);
        condition.setCode(code);

        CodeableConcept clinicalStatus = new CodeableConcept();
        clinicalStatus.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
            .setCode("active");
        condition.setClinicalStatus(clinicalStatus);

        condition.setOnset(new DateTimeType(Date.from(
            java.time.Instant.now().minus(java.time.Duration.ofDays(faker.number().numberBetween(30, 1825))))));

        return condition;
    }

    private MedicationRequest createMedication(Patient patient) {
        MedicationRequest medication = new MedicationRequest();
        medication.setId(UUID.randomUUID().toString());
        medication.setSubject(new Reference("Patient/" + patient.getId()));
        medication.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        medication.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);

        // Random common medication
        String[] medications = {
            "860974|Metformin 500 MG",
            "310539|Glipizide 5 MG",
            "314076|Lisinopril 20 MG",
            "617311|Atorvastatin 20 MG",
            "197361|Amlodipine 5 MG",
            "834060|Omeprazole 20 MG",
            "1049621|Levothyroxine 100 MCG",
            "855332|Sertraline 50 MG"
        };

        String selected = medications[faker.number().numberBetween(0, medications.length)];
        String[] parts = selected.split("\\|");

        CodeableConcept medicationCode = new CodeableConcept();
        medicationCode.addCoding()
            .setSystem("http://www.nlm.nih.gov/research/umls/rxnorm")
            .setCode(parts[0])
            .setDisplay(parts[1]);
        medication.setMedication(medicationCode);

        medication.setAuthoredOn(Date.from(
            java.time.Instant.now().minus(java.time.Duration.ofDays(faker.number().numberBetween(1, 365)))));

        return medication;
    }

    private Observation createObservation(Patient patient) {
        Observation observation = new Observation();
        observation.setId(UUID.randomUUID().toString());
        observation.setSubject(new Reference("Patient/" + patient.getId()));
        observation.setStatus(Observation.ObservationStatus.FINAL);

        // Random vital sign or lab
        String[] observations = {
            "4548-4|Hemoglobin A1c|5.0-9.0|%",
            "2093-3|Total Cholesterol|150-300|mg/dL",
            "29463-7|Body Weight|50-150|kg",
            "39156-5|BMI|18-40|kg/m2",
            "8480-6|Systolic BP|90-160|mmHg",
            "8462-4|Diastolic BP|60-100|mmHg",
            "2089-1|LDL Cholesterol|70-200|mg/dL",
            "2085-9|HDL Cholesterol|30-80|mg/dL"
        };

        String selected = observations[faker.number().numberBetween(0, observations.length)];
        String[] parts = selected.split("\\|");

        CodeableConcept code = new CodeableConcept();
        code.addCoding()
            .setSystem("http://loinc.org")
            .setCode(parts[0])
            .setDisplay(parts[1]);
        observation.setCode(code);

        // Generate random value in range
        String[] range = parts[2].split("-");
        double min = Double.parseDouble(range[0]);
        double max = Double.parseDouble(range[1]);
        double value = min + (max - min) * faker.random().nextDouble();

        Quantity quantity = new Quantity();
        quantity.setValue(Math.round(value * 10.0) / 10.0);
        quantity.setUnit(parts[3]);
        observation.setValue(quantity);

        observation.setEffective(new DateTimeType(Date.from(
            java.time.Instant.now().minus(java.time.Duration.ofDays(faker.number().numberBetween(1, 180))))));

        return observation;
    }

    private Encounter createEncounter(Patient patient) {
        Encounter encounter = new Encounter();
        encounter.setId(UUID.randomUUID().toString());
        encounter.setSubject(new Reference("Patient/" + patient.getId()));
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);

        CodeableConcept encounterClass = new CodeableConcept();
        encounterClass.addCoding()
            .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
            .setCode("AMB")
            .setDisplay("Ambulatory");
        encounter.setClass_(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
            .setCode("AMB")
            .setDisplay("Ambulatory"));

        Period period = new Period();
        Date encounterDate = Date.from(
            java.time.Instant.now().minus(java.time.Duration.ofDays(faker.number().numberBetween(1, 365))));
        period.setStart(encounterDate);
        period.setEnd(encounterDate);
        encounter.setPeriod(period);

        return encounter;
    }

    private void addResourceToBundle(Bundle bundle, Resource resource, String resourceType) {
        Bundle.BundleEntryComponent entry = bundle.addEntry();
        entry.setResource(resource);
        entry.getRequest()
            .setMethod(Bundle.HTTPVerb.POST)
            .setUrl(resourceType);
    }
}

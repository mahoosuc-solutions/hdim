package com.healthdata.ecr.service;

import com.healthdata.ecr.persistence.ElectronicCaseReportEntity;
import com.healthdata.ecr.persistence.ElectronicCaseReportEntity.TriggerCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * eICR (electronic Initial Case Report) Generator Service
 *
 * Generates FHIR R4 Bundles conforming to the HL7 eCR Implementation Guide
 * for submission to public health authorities via AIMS platform.
 *
 * Bundle Structure (eCR IG):
 * - Composition (eICR document)
 * - Patient
 * - Encounter
 * - Condition (trigger condition)
 * - Observation (lab results if applicable)
 * - ServiceRequest (lab orders if applicable)
 * - MedicationAdministration (medications if applicable)
 * - Organization (author, custodian)
 * - Practitioner (author)
 * - Location (facility)
 *
 * @see <a href="http://hl7.org/fhir/us/ecr/">HL7 eCR Implementation Guide</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EicrGeneratorService {

    @Value("${ecr.eicr.author-organization:HealthData-in-Motion}")
    private String authorOrganization;

    @Value("${ecr.eicr.custodian-oid:2.16.840.1.113883.3.xxx}")
    private String custodianOid;

    // eCR Profile URLs
    private static final String EICR_COMPOSITION_PROFILE = "http://hl7.org/fhir/us/ecr/StructureDefinition/eicr-composition";
    private static final String EICR_PATIENT_PROFILE = "http://hl7.org/fhir/us/ecr/StructureDefinition/us-ph-patient";
    private static final String EICR_CONDITION_PROFILE = "http://hl7.org/fhir/us/ecr/StructureDefinition/eicr-condition";
    private static final String EICR_ENCOUNTER_PROFILE = "http://hl7.org/fhir/us/ecr/StructureDefinition/eicr-encounter";

    // LOINC code for eCR document type
    private static final String EICR_DOCUMENT_TYPE_CODE = "55751-2";
    private static final String EICR_DOCUMENT_TYPE_DISPLAY = "Public health Case report";

    /**
     * Generate an eICR FHIR Bundle for a reportable condition.
     *
     * @param eCR The electronic case report entity
     * @param patientData Patient demographic data
     * @param encounterData Encounter data (if available)
     * @param triggerData The data that triggered the report
     * @return FHIR Bundle representing the eICR
     */
    public Bundle generateEicr(
            ElectronicCaseReportEntity eCR,
            PatientData patientData,
            EncounterData encounterData,
            TriggerData triggerData) {

        log.info("Generating eICR for patient {} with trigger code {}",
            eCR.getPatientId(), eCR.getTriggerCode());

        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.DOCUMENT);
        bundle.setTimestamp(new Date());

        // Create all resources
        Patient patient = createPatient(patientData);
        Organization authorOrg = createAuthorOrganization();
        Organization custodianOrg = createCustodianOrganization();
        Practitioner practitioner = createPractitioner(triggerData.getAuthorName());
        Encounter encounter = encounterData != null ? createEncounter(encounterData, patient) : null;

        // Create trigger-specific resources
        Resource triggerResource = createTriggerResource(eCR, triggerData, patient, encounter);

        // Create eICR Composition
        Composition composition = createComposition(
            eCR, patient, practitioner, authorOrg, custodianOrg, encounter, triggerResource);

        // Add resources to bundle in proper order
        addBundleEntry(bundle, composition);
        addBundleEntry(bundle, patient);
        addBundleEntry(bundle, practitioner);
        addBundleEntry(bundle, authorOrg);
        addBundleEntry(bundle, custodianOrg);
        if (encounter != null) {
            addBundleEntry(bundle, encounter);
        }
        addBundleEntry(bundle, triggerResource);

        log.info("Generated eICR bundle with {} entries for eCR {}",
            bundle.getEntry().size(), eCR.getId());

        return bundle;
    }

    private Composition createComposition(
            ElectronicCaseReportEntity eCR,
            Patient patient,
            Practitioner practitioner,
            Organization authorOrg,
            Organization custodianOrg,
            Encounter encounter,
            Resource triggerResource) {

        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.getMeta().addProfile(EICR_COMPOSITION_PROFILE);

        // Set status and type
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode(EICR_DOCUMENT_TYPE_CODE)
                .setDisplay(EICR_DOCUMENT_TYPE_DISPLAY)));

        // Set date and title
        composition.setDate(new Date());
        composition.setTitle("Electronic Initial Case Report - " + eCR.getConditionName());

        // Set subject (patient)
        composition.setSubject(new Reference(patient));

        // Set author
        composition.addAuthor(new Reference(practitioner));
        composition.addAuthor(new Reference(authorOrg));

        // Set custodian
        composition.setCustodian(new Reference(custodianOrg));

        // Set encounter if available
        if (encounter != null) {
            composition.setEncounter(new Reference(encounter));
        }

        // Add sections
        addReasonForReportSection(composition, eCR, triggerResource);
        addEncounterSection(composition, encounter);
        addHistoryOfPresentIllnessSection(composition);

        return composition;
    }

    private void addReasonForReportSection(
            Composition composition,
            ElectronicCaseReportEntity eCR,
            Resource triggerResource) {

        Composition.SectionComponent section = new Composition.SectionComponent();
        section.setTitle("Reason for Report");
        section.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode("29299-5")
                .setDisplay("Reason for visit")));

        section.getText().setStatus(Narrative.NarrativeStatus.GENERATED);
        section.getText().setDivAsString(
            "<div xmlns=\"http://www.w3.org/1999/xhtml\">" +
            "<p>Reportable condition detected: " + eCR.getConditionName() + "</p>" +
            "<p>Trigger code: " + eCR.getTriggerCode() + " (" + eCR.getTriggerDisplay() + ")</p>" +
            "</div>");

        section.addEntry(new Reference(triggerResource));
        composition.addSection(section);
    }

    private void addEncounterSection(Composition composition, Encounter encounter) {
        if (encounter == null) return;

        Composition.SectionComponent section = new Composition.SectionComponent();
        section.setTitle("Encounter Information");
        section.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode("46240-8")
                .setDisplay("History of Hospitalizations+Outpatient visits")));

        section.addEntry(new Reference(encounter));
        composition.addSection(section);
    }

    private void addHistoryOfPresentIllnessSection(Composition composition) {
        Composition.SectionComponent section = new Composition.SectionComponent();
        section.setTitle("History of Present Illness");
        section.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode("10164-2")
                .setDisplay("History of Present illness")));

        section.getText().setStatus(Narrative.NarrativeStatus.GENERATED);
        section.getText().setDivAsString(
            "<div xmlns=\"http://www.w3.org/1999/xhtml\">" +
            "<p>See attached clinical documentation.</p>" +
            "</div>");

        composition.addSection(section);
    }

    private Patient createPatient(PatientData data) {
        Patient patient = new Patient();
        patient.setId(data.getPatientId().toString());
        patient.getMeta().addProfile(EICR_PATIENT_PROFILE);

        // Add identifier
        patient.addIdentifier()
            .setSystem("urn:oid:2.16.840.1.113883.4.1")
            .setValue(data.getMrn());

        // Add name
        patient.addName()
            .setFamily(data.getLastName())
            .addGiven(data.getFirstName());

        // Add birth date
        if (data.getBirthDate() != null) {
            patient.setBirthDate(Date.from(
                data.getBirthDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        // Add gender
        if (data.getGender() != null) {
            patient.setGender(Enumerations.AdministrativeGender.fromCode(
                data.getGender().toLowerCase()));
        }

        // Add address
        if (data.getAddress() != null) {
            Address address = new Address();
            address.addLine(data.getAddress().getStreet());
            address.setCity(data.getAddress().getCity());
            address.setState(data.getAddress().getState());
            address.setPostalCode(data.getAddress().getZipCode());
            address.setCountry("US");
            patient.addAddress(address);
        }

        // Add telecom
        if (data.getPhone() != null) {
            patient.addTelecom()
                .setSystem(ContactPoint.ContactPointSystem.PHONE)
                .setValue(data.getPhone());
        }

        return patient;
    }

    private Encounter createEncounter(EncounterData data, Patient patient) {
        Encounter encounter = new Encounter();
        encounter.setId(data.getEncounterId().toString());
        encounter.getMeta().addProfile(EICR_ENCOUNTER_PROFILE);

        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.setClass_(new Coding()
            .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
            .setCode(data.getEncounterClass())
            .setDisplay(getEncounterClassDisplay(data.getEncounterClass())));

        encounter.setSubject(new Reference(patient));

        if (data.getStartTime() != null) {
            Period period = new Period();
            period.setStart(Date.from(data.getStartTime()
                .atZone(ZoneId.systemDefault()).toInstant()));
            if (data.getEndTime() != null) {
                period.setEnd(Date.from(data.getEndTime()
                    .atZone(ZoneId.systemDefault()).toInstant()));
            }
            encounter.setPeriod(period);
        }

        return encounter;
    }

    private Resource createTriggerResource(
            ElectronicCaseReportEntity eCR,
            TriggerData triggerData,
            Patient patient,
            Encounter encounter) {

        switch (eCR.getTriggerCategory()) {
            case DIAGNOSIS:
                return createCondition(eCR, triggerData, patient, encounter);
            case LAB_RESULT:
                return createLabObservation(eCR, triggerData, patient, encounter);
            case MEDICATION:
                return createMedicationAdministration(eCR, triggerData, patient, encounter);
            case PROCEDURE:
                return createProcedure(eCR, triggerData, patient, encounter);
            default:
                return createCondition(eCR, triggerData, patient, encounter);
        }
    }

    private Condition createCondition(
            ElectronicCaseReportEntity eCR,
            TriggerData triggerData,
            Patient patient,
            Encounter encounter) {

        Condition condition = new Condition();
        condition.setId(UUID.randomUUID().toString());
        condition.getMeta().addProfile(EICR_CONDITION_PROFILE);

        condition.setClinicalStatus(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-clinical")
                .setCode("active")));

        condition.setVerificationStatus(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/condition-ver-status")
                .setCode("confirmed")));

        condition.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(eCR.getTriggerCodeSystem())
                .setCode(eCR.getTriggerCode())
                .setDisplay(eCR.getTriggerDisplay())));

        condition.setSubject(new Reference(patient));

        if (encounter != null) {
            condition.setEncounter(new Reference(encounter));
        }

        if (triggerData.getOnsetDate() != null) {
            condition.setOnset(new DateTimeType(Date.from(
                triggerData.getOnsetDate().atZone(ZoneId.systemDefault()).toInstant())));
        }

        return condition;
    }

    private Observation createLabObservation(
            ElectronicCaseReportEntity eCR,
            TriggerData triggerData,
            Patient patient,
            Encounter encounter) {

        Observation observation = new Observation();
        observation.setId(UUID.randomUUID().toString());

        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.addCategory(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("laboratory")));

        observation.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(eCR.getTriggerCodeSystem())
                .setCode(eCR.getTriggerCode())
                .setDisplay(eCR.getTriggerDisplay())));

        observation.setSubject(new Reference(patient));

        if (encounter != null) {
            observation.setEncounter(new Reference(encounter));
        }

        if (triggerData.getLabValue() != null) {
            observation.setValue(new StringType(triggerData.getLabValue()));
        }

        return observation;
    }

    private MedicationAdministration createMedicationAdministration(
            ElectronicCaseReportEntity eCR,
            TriggerData triggerData,
            Patient patient,
            Encounter encounter) {

        MedicationAdministration medAdmin = new MedicationAdministration();
        medAdmin.setId(UUID.randomUUID().toString());

        medAdmin.setStatus(MedicationAdministration.MedicationAdministrationStatus.COMPLETED);

        medAdmin.setMedication(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(eCR.getTriggerCodeSystem())
                .setCode(eCR.getTriggerCode())
                .setDisplay(eCR.getTriggerDisplay())));

        medAdmin.setSubject(new Reference(patient));

        if (encounter != null) {
            medAdmin.setContext(new Reference(encounter));
        }

        return medAdmin;
    }

    private Procedure createProcedure(
            ElectronicCaseReportEntity eCR,
            TriggerData triggerData,
            Patient patient,
            Encounter encounter) {

        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID().toString());

        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);

        procedure.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(eCR.getTriggerCodeSystem())
                .setCode(eCR.getTriggerCode())
                .setDisplay(eCR.getTriggerDisplay())));

        procedure.setSubject(new Reference(patient));

        if (encounter != null) {
            procedure.setEncounter(new Reference(encounter));
        }

        return procedure;
    }

    private Organization createAuthorOrganization() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID().toString());
        org.setName(authorOrganization);
        org.addType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/organization-type")
                .setCode("prov")
                .setDisplay("Healthcare Provider")));
        return org;
    }

    private Organization createCustodianOrganization() {
        Organization org = new Organization();
        org.setId(UUID.randomUUID().toString());
        org.setName(authorOrganization);
        org.addIdentifier()
            .setSystem("urn:oid:2.16.840.1.113883.4.6")
            .setValue(custodianOid);
        return org;
    }

    private Practitioner createPractitioner(String name) {
        Practitioner practitioner = new Practitioner();
        practitioner.setId(UUID.randomUUID().toString());
        if (name != null) {
            String[] parts = name.split(" ", 2);
            HumanName humanName = new HumanName();
            if (parts.length > 0) humanName.addGiven(parts[0]);
            if (parts.length > 1) humanName.setFamily(parts[1]);
            practitioner.addName(humanName);
        }
        return practitioner;
    }

    private void addBundleEntry(Bundle bundle, Resource resource) {
        Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
        entry.setFullUrl("urn:uuid:" + resource.getId());
        entry.setResource(resource);
        bundle.addEntry(entry);
    }

    private String getEncounterClassDisplay(String code) {
        return switch (code) {
            case "IMP" -> "inpatient encounter";
            case "AMB" -> "ambulatory";
            case "EMER" -> "emergency";
            case "HH" -> "home health";
            default -> code;
        };
    }

    // Data transfer objects

    @lombok.Data
    @lombok.Builder
    public static class PatientData {
        private UUID patientId;
        private String mrn;
        private String firstName;
        private String lastName;
        private java.time.LocalDate birthDate;
        private String gender;
        private AddressData address;
        private String phone;
        private String email;
    }

    @lombok.Data
    @lombok.Builder
    public static class AddressData {
        private String street;
        private String city;
        private String state;
        private String zipCode;
    }

    @lombok.Data
    @lombok.Builder
    public static class EncounterData {
        private UUID encounterId;
        private String encounterClass; // IMP, AMB, EMER, HH
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String facilityName;
    }

    @lombok.Data
    @lombok.Builder
    public static class TriggerData {
        private String authorName;
        private LocalDateTime onsetDate;
        private String labValue;
        private String labUnit;
        private String interpretation;
    }
}

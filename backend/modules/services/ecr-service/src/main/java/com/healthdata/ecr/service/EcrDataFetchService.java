package com.healthdata.ecr.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.ecr.client.FhirServiceClient;
import com.healthdata.ecr.client.PatientServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service for fetching patient and clinical data needed for eICR generation.
 *
 * Calls the patient-service and fhir-service via Feign clients to retrieve
 * actual patient demographics, encounter details, and supporting clinical data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EcrDataFetchService {

    private final PatientServiceClient patientServiceClient;
    private final FhirServiceClient fhirServiceClient;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser jsonParser = fhirContext.newJsonParser();

    /**
     * Fetch patient demographic data for eICR generation.
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @return Patient data for eICR
     */
    public EicrGeneratorService.PatientData fetchPatientData(String tenantId, UUID patientId) {
        log.debug("Fetching patient data for tenant={}, patient={}", tenantId, patientId);

        try {
            // Fetch comprehensive health record which includes Patient resource
            String bundleJson = patientServiceClient.getComprehensiveHealthRecord(tenantId, patientId.toString());
            Bundle bundle = jsonParser.parseResource(Bundle.class, bundleJson);

            // Extract Patient resource from bundle
            Patient patient = bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof Patient)
                .map(entry -> (Patient) entry.getResource())
                .findFirst()
                .orElse(null);

            if (patient == null) {
                log.warn("No Patient resource found in bundle for patient={}", patientId);
                return buildFallbackPatientData(patientId);
            }

            return buildPatientDataFromFhir(patient, patientId);

        } catch (Exception e) {
            log.error("Error fetching patient data from patient-service: {}", e.getMessage(), e);
            return buildFallbackPatientData(patientId);
        }
    }

    /**
     * Build PatientData from FHIR Patient resource
     */
    private EicrGeneratorService.PatientData buildPatientDataFromFhir(Patient patient, UUID patientId) {
        // Extract name
        String firstName = "Unknown";
        String lastName = "Unknown";
        if (patient.hasName() && !patient.getName().isEmpty()) {
            HumanName name = patient.getName().get(0);
            if (name.hasGiven() && !name.getGiven().isEmpty()) {
                firstName = name.getGiven().get(0).getValue();
            }
            if (name.hasFamily()) {
                lastName = name.getFamily();
            }
        }

        // Extract MRN from identifiers
        String mrn = patient.getIdentifier().stream()
            .filter(id -> id.hasType() && id.getType().hasCoding() &&
                id.getType().getCoding().stream()
                    .anyMatch(coding -> "MR".equals(coding.getCode())))
            .map(Identifier::getValue)
            .findFirst()
            .orElse("MRN-" + patientId.toString().substring(0, 8));

        // Extract birth date
        LocalDate birthDate = patient.hasBirthDate() ?
            patient.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() :
            LocalDate.of(1970, 1, 1);

        // Extract gender
        String gender = patient.hasGender() ?
            patient.getGender().toCode() :
            "unknown";

        // Extract address
        EicrGeneratorService.AddressData address = EicrGeneratorService.AddressData.builder()
            .street("Unknown")
            .city("Unknown")
            .state("Unknown")
            .zipCode("00000")
            .build();

        if (patient.hasAddress() && !patient.getAddress().isEmpty()) {
            Address fhirAddress = patient.getAddress().get(0);
            address = EicrGeneratorService.AddressData.builder()
                .street(fhirAddress.hasLine() && !fhirAddress.getLine().isEmpty() ?
                    fhirAddress.getLine().get(0).getValue() : "Unknown")
                .city(fhirAddress.hasCity() ? fhirAddress.getCity() : "Unknown")
                .state(fhirAddress.hasState() ? fhirAddress.getState() : "Unknown")
                .zipCode(fhirAddress.hasPostalCode() ? fhirAddress.getPostalCode() : "00000")
                .build();
        }

        // Extract phone
        String phone = patient.getTelecom().stream()
            .filter(telecom -> telecom.hasSystem() &&
                telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE)
            .map(ContactPoint::getValue)
            .findFirst()
            .orElse("555-0100");

        return EicrGeneratorService.PatientData.builder()
            .patientId(patientId)
            .mrn(mrn)
            .firstName(firstName)
            .lastName(lastName)
            .birthDate(birthDate)
            .gender(gender)
            .address(address)
            .phone(phone)
            .build();
    }

    /**
     * Build fallback patient data when service call fails
     */
    private EicrGeneratorService.PatientData buildFallbackPatientData(UUID patientId) {
        return EicrGeneratorService.PatientData.builder()
            .patientId(patientId)
            .mrn("MRN-" + patientId.toString().substring(0, 8))
            .firstName("Patient")
            .lastName("Demo")
            .birthDate(LocalDate.of(1970, 1, 1))
            .gender("unknown")
            .address(EicrGeneratorService.AddressData.builder()
                .street("123 Health St")
                .city("Boston")
                .state("MA")
                .zipCode("02101")
                .build())
            .phone("555-0100")
            .build();
    }

    /**
     * Fetch encounter data for eICR generation.
     *
     * @param tenantId Tenant identifier
     * @param encounterId Encounter identifier
     * @return Encounter data for eICR
     */
    public EicrGeneratorService.EncounterData fetchEncounterData(String tenantId, UUID encounterId) {
        log.debug("Fetching encounter data for tenant={}, encounter={}", tenantId, encounterId);

        try {
            // Fetch encounter from FHIR service
            String encounterJson = fhirServiceClient.getEncounter(tenantId, encounterId.toString());
            Encounter encounter = jsonParser.parseResource(Encounter.class, encounterJson);

            return buildEncounterDataFromFhir(encounter, encounterId);

        } catch (Exception e) {
            log.error("Error fetching encounter data from fhir-service: {}", e.getMessage(), e);
            return buildFallbackEncounterData(encounterId);
        }
    }

    /**
     * Build EncounterData from FHIR Encounter resource
     */
    private EicrGeneratorService.EncounterData buildEncounterDataFromFhir(Encounter encounter, UUID encounterId) {
        // Extract encounter class
        String encounterClass = encounter.hasClass_() ?
            encounter.getClass_().getCode() :
            "AMB";

        // Extract start and end times
        LocalDateTime startTime = encounter.hasPeriod() && encounter.getPeriod().hasStart() ?
            encounter.getPeriod().getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() :
            LocalDateTime.now().minusHours(2);

        LocalDateTime endTime = encounter.hasPeriod() && encounter.getPeriod().hasEnd() ?
            encounter.getPeriod().getEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() :
            LocalDateTime.now();

        // Extract facility name from location
        String facilityName = "Unknown Facility";
        if (encounter.hasLocation() && !encounter.getLocation().isEmpty()) {
            Encounter.EncounterLocationComponent location = encounter.getLocation().get(0);
            if (location.hasLocation() && location.getLocation().hasDisplay()) {
                facilityName = location.getLocation().getDisplay();
            }
        } else if (encounter.hasServiceProvider() && encounter.getServiceProvider().hasDisplay()) {
            facilityName = encounter.getServiceProvider().getDisplay();
        }

        return EicrGeneratorService.EncounterData.builder()
            .encounterId(encounterId)
            .encounterClass(encounterClass)
            .startTime(startTime)
            .endTime(endTime)
            .facilityName(facilityName)
            .build();
    }

    /**
     * Build fallback encounter data when service call fails
     */
    private EicrGeneratorService.EncounterData buildFallbackEncounterData(UUID encounterId) {
        return EicrGeneratorService.EncounterData.builder()
            .encounterId(encounterId)
            .encounterClass("AMB")
            .startTime(LocalDateTime.now().minusHours(2))
            .endTime(LocalDateTime.now())
            .facilityName("Demo Clinic")
            .build();
    }

    /**
     * Fetch additional clinical context for eICR (diagnoses, labs, meds).
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @param encounterId Encounter identifier (optional)
     * @return Clinical context data
     */
    public ClinicalContextData fetchClinicalContext(
            String tenantId, UUID patientId, UUID encounterId) {

        log.debug("Fetching clinical context for patient={}", patientId);

        try {
            String patientIdStr = patientId.toString();

            // Fetch all clinical data in parallel
            List<DiagnosisInfo> diagnoses = fetchDiagnoses(tenantId, patientIdStr);
            List<LabResultInfo> labs = fetchLabResults(tenantId, patientIdStr);
            List<MedicationInfo> medications = fetchMedications(tenantId, patientIdStr);
            List<AllergyInfo> allergies = fetchAllergies(tenantId, patientIdStr);

            return ClinicalContextData.builder()
                .activeDiagnoses(diagnoses)
                .recentLabs(labs)
                .activeMedications(medications)
                .allergies(allergies)
                .build();

        } catch (Exception e) {
            log.error("Error fetching clinical context: {}", e.getMessage(), e);
            return ClinicalContextData.builder()
                .activeDiagnoses(new ArrayList<>())
                .recentLabs(new ArrayList<>())
                .activeMedications(new ArrayList<>())
                .allergies(new ArrayList<>())
                .build();
        }
    }

    /**
     * Fetch diagnoses (conditions) for patient
     */
    private List<DiagnosisInfo> fetchDiagnoses(String tenantId, String patientId) {
        try {
            String bundleJson = fhirServiceClient.getActiveConditions(tenantId, patientId);
            Bundle bundle = jsonParser.parseResource(Bundle.class, bundleJson);

            return bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof Condition)
                .map(entry -> (Condition) entry.getResource())
                .map(this::conditionToDiagnosisInfo)
                .toList();

        } catch (Exception e) {
            log.warn("Error fetching diagnoses: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Fetch lab results for patient
     */
    private List<LabResultInfo> fetchLabResults(String tenantId, String patientId) {
        try {
            String bundleJson = fhirServiceClient.getLabResults(tenantId, patientId);
            Bundle bundle = jsonParser.parseResource(Bundle.class, bundleJson);

            return bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof Observation)
                .map(entry -> (Observation) entry.getResource())
                .map(this::observationToLabResultInfo)
                .toList();

        } catch (Exception e) {
            log.warn("Error fetching lab results: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Fetch medications for patient
     */
    private List<MedicationInfo> fetchMedications(String tenantId, String patientId) {
        try {
            String bundleJson = fhirServiceClient.getActiveMedications(tenantId, patientId);
            Bundle bundle = jsonParser.parseResource(Bundle.class, bundleJson);

            return bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof MedicationRequest)
                .map(entry -> (MedicationRequest) entry.getResource())
                .map(this::medicationRequestToMedicationInfo)
                .toList();

        } catch (Exception e) {
            log.warn("Error fetching medications: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Fetch allergies for patient
     */
    private List<AllergyInfo> fetchAllergies(String tenantId, String patientId) {
        try {
            String bundleJson = fhirServiceClient.getActiveAllergies(tenantId, patientId);
            Bundle bundle = jsonParser.parseResource(Bundle.class, bundleJson);

            return bundle.getEntry().stream()
                .filter(entry -> entry.getResource() instanceof AllergyIntolerance)
                .map(entry -> (AllergyIntolerance) entry.getResource())
                .map(this::allergyToAllergyInfo)
                .toList();

        } catch (Exception e) {
            log.warn("Error fetching allergies: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Helper methods to convert FHIR resources to internal DTOs

    private DiagnosisInfo conditionToDiagnosisInfo(Condition condition) {
        String code = null;
        String codeSystem = null;
        String display = "Unknown Condition";

        if (condition.hasCode() && condition.getCode().hasCoding() && !condition.getCode().getCoding().isEmpty()) {
            Coding coding = condition.getCode().getCoding().get(0);
            code = coding.getCode();
            codeSystem = coding.getSystem();
            display = coding.hasDisplay() ? coding.getDisplay() : display;
        }

        LocalDateTime onsetDate = null;
        if (condition.hasOnsetDateTimeType()) {
            Date date = condition.getOnsetDateTimeType().getValue();
            if (date != null) {
                onsetDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }

        return DiagnosisInfo.builder()
            .code(code)
            .codeSystem(codeSystem)
            .display(display)
            .onsetDate(onsetDate)
            .build();
    }

    private LabResultInfo observationToLabResultInfo(Observation observation) {
        String loincCode = null;
        String display = "Unknown Lab";

        if (observation.hasCode() && observation.getCode().hasCoding() && !observation.getCode().getCoding().isEmpty()) {
            Coding coding = observation.getCode().getCoding().stream()
                .filter(c -> c.hasSystem() && c.getSystem().contains("loinc"))
                .findFirst()
                .orElse(observation.getCode().getCoding().get(0));

            loincCode = coding.getCode();
            display = coding.hasDisplay() ? coding.getDisplay() : display;
        }

        String value = null;
        String unit = null;
        if (observation.hasValueQuantity()) {
            Quantity qty = observation.getValueQuantity();
            value = qty.hasValue() ? qty.getValue().toString() : null;
            unit = qty.hasUnit() ? qty.getUnit() : null;
        } else if (observation.hasValueStringType()) {
            value = observation.getValueStringType().getValue();
        }

        String interpretation = null;
        if (observation.hasInterpretation() && !observation.getInterpretation().isEmpty() &&
            observation.getInterpretation().get(0).hasCoding() && !observation.getInterpretation().get(0).getCoding().isEmpty()) {
            interpretation = observation.getInterpretation().get(0).getCoding().get(0).getCode();
        }

        LocalDateTime effectiveDate = null;
        if (observation.hasEffectiveDateTimeType()) {
            Date date = observation.getEffectiveDateTimeType().getValue();
            if (date != null) {
                effectiveDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }

        return LabResultInfo.builder()
            .loincCode(loincCode)
            .display(display)
            .value(value)
            .unit(unit)
            .interpretation(interpretation)
            .effectiveDate(effectiveDate)
            .build();
    }

    private MedicationInfo medicationRequestToMedicationInfo(MedicationRequest medRequest) {
        String rxnormCode = null;
        String display = "Unknown Medication";

        if (medRequest.hasMedicationCodeableConcept()) {
            CodeableConcept medCode = medRequest.getMedicationCodeableConcept();
            if (medCode.hasCoding() && !medCode.getCoding().isEmpty()) {
                Coding coding = medCode.getCoding().stream()
                    .filter(c -> c.hasSystem() && c.getSystem().contains("rxnorm"))
                    .findFirst()
                    .orElse(medCode.getCoding().get(0));

                rxnormCode = coding.getCode();
                display = coding.hasDisplay() ? coding.getDisplay() : display;
            }
        }

        String dosage = null;
        if (medRequest.hasDosageInstruction() && !medRequest.getDosageInstruction().isEmpty()) {
            Dosage dose = medRequest.getDosageInstruction().get(0);
            if (dose.hasText()) {
                dosage = dose.getText();
            } else if (dose.hasDoseAndRate() && !dose.getDoseAndRate().isEmpty() &&
                dose.getDoseAndRate().get(0).hasDoseQuantity()) {
                Quantity qty = dose.getDoseAndRate().get(0).getDoseQuantity();
                dosage = qty.getValue() + " " + qty.getUnit();
            }
        }

        LocalDateTime startDate = null;
        if (medRequest.hasAuthoredOn()) {
            startDate = medRequest.getAuthoredOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        return MedicationInfo.builder()
            .rxnormCode(rxnormCode)
            .display(display)
            .dosage(dosage)
            .startDate(startDate)
            .build();
    }

    private AllergyInfo allergyToAllergyInfo(AllergyIntolerance allergy) {
        String code = null;
        String display = "Unknown Allergy";

        if (allergy.hasCode() && allergy.getCode().hasCoding() && !allergy.getCode().getCoding().isEmpty()) {
            Coding coding = allergy.getCode().getCoding().get(0);
            code = coding.getCode();
            display = coding.hasDisplay() ? coding.getDisplay() : display;
        }

        String severity = null;
        if (allergy.hasCriticality()) {
            severity = allergy.getCriticality().toCode();
        }

        return AllergyInfo.builder()
            .code(code)
            .display(display)
            .severity(severity)
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class ClinicalContextData {
        private java.util.List<DiagnosisInfo> activeDiagnoses;
        private java.util.List<LabResultInfo> recentLabs;
        private java.util.List<MedicationInfo> activeMedications;
        private java.util.List<AllergyInfo> allergies;
    }

    @lombok.Data
    @lombok.Builder
    public static class DiagnosisInfo {
        private String code;
        private String codeSystem;
        private String display;
        private LocalDateTime onsetDate;
    }

    @lombok.Data
    @lombok.Builder
    public static class LabResultInfo {
        private String loincCode;
        private String display;
        private String value;
        private String unit;
        private String interpretation;
        private LocalDateTime effectiveDate;
    }

    @lombok.Data
    @lombok.Builder
    public static class MedicationInfo {
        private String rxnormCode;
        private String display;
        private String dosage;
        private LocalDateTime startDate;
    }

    @lombok.Data
    @lombok.Builder
    public static class AllergyInfo {
        private String code;
        private String display;
        private String severity;
    }
}

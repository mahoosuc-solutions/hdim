package com.healthdata.ehr.connector.cerner;

import ca.uhn.fhir.context.FhirContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CernerDataMapper {

    private final FhirContext fhirContext;

    private static final String CERNER_EXTENSION_BASE = "http://fhir.cerner.com/extension/";
    private static final String CERNER_CODE_CONSOLE = "http://fhir.cerner.com/code-console";

    public Patient mapPatient(Patient patient) {
        if (patient == null) {
            return null;
        }

        log.debug("Mapping Cerner patient: {}", patient.getIdElement().getIdPart());
        return patient;
    }

    public Encounter mapEncounter(Encounter encounter) {
        if (encounter == null) {
            return null;
        }

        log.debug("Mapping Cerner encounter: {}", encounter.getIdElement().getIdPart());
        return encounter;
    }

    public Observation mapObservation(Observation observation) {
        if (observation == null) {
            return null;
        }

        log.debug("Mapping Cerner observation: {}", observation.getIdElement().getIdPart());
        return observation;
    }

    public Condition mapCondition(Condition condition) {
        if (condition == null) {
            return null;
        }

        log.debug("Mapping Cerner condition: {}", condition.getIdElement().getIdPart());
        return condition;
    }

    public MedicationRequest mapMedicationRequest(MedicationRequest medicationRequest) {
        if (medicationRequest == null) {
            return null;
        }

        log.debug("Mapping Cerner medication request: {}", medicationRequest.getIdElement().getIdPart());
        return medicationRequest;
    }

    public Immunization mapImmunization(Immunization immunization) {
        if (immunization == null) {
            return null;
        }

        log.debug("Mapping Cerner immunization: {}", immunization.getIdElement().getIdPart());
        return immunization;
    }

    public DiagnosticReport mapDiagnosticReport(DiagnosticReport report) {
        if (report == null) {
            return null;
        }

        log.debug("Mapping Cerner diagnostic report: {}", report.getIdElement().getIdPart());
        return report;
    }

    public String extractCernerExtension(DomainResource resource, String extensionUrl) {
        if (resource == null || extensionUrl == null) {
            return null;
        }

        List<Extension> extensions = resource.getExtensionsByUrl(extensionUrl);
        if (extensions.isEmpty()) {
            return null;
        }

        Extension extension = extensions.get(0);
        if (extension.getValue() instanceof StringType) {
            return ((StringType) extension.getValue()).getValue();
        }

        return null;
    }

    public CodeableConcept normalizeCoding(CodeableConcept concept) {
        if (concept == null) {
            return null;
        }

        return concept;
    }

    public boolean isCernerExtension(String url) {
        return url != null && url.startsWith(CERNER_EXTENSION_BASE);
    }

    public boolean isCernerCodeConsole(Coding coding) {
        return coding != null && CERNER_CODE_CONSOLE.equals(coding.getSystem());
    }
}

package com.healthdata.ehr.connector.epic;

import com.healthdata.ehr.connector.core.DataMapper;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps Epic-specific FHIR extensions to normalized models.
 * Handles Epic's custom extensions (http://open.epic.com/fhir/extensions/*).
 */
@Component
public class EpicDataMapper implements DataMapper<Map<String, Object>> {

    private static final String EPIC_EXTENSION_BASE = "http://open.epic.com/fhir/extensions/";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public Map<String, Object> mapPatient(Patient patient) {
        Map<String, Object> result = new HashMap<>();

        if (patient.hasId()) {
            result.put("id", patient.getIdElement().getIdPart());
        }

        if (patient.hasName() && !patient.getName().isEmpty()) {
            HumanName name = patient.getName().get(0);
            if (name.hasFamily()) {
                result.put("familyName", name.getFamily());
            }
            if (name.hasGiven()) {
                String givenNames = name.getGiven().stream()
                        .map(StringType::getValue)
                        .collect(Collectors.joining(" "));
                result.put("givenNames", givenNames);
            }
        }

        if (patient.hasGender()) {
            result.put("gender", patient.getGender().name());
        }

        if (patient.hasBirthDate()) {
            result.put("birthDate", DATE_FORMAT.format(patient.getBirthDate()));
        }

        // Extract MRN
        if (patient.hasIdentifier()) {
            for (Identifier identifier : patient.getIdentifier()) {
                if (identifier.hasType() && identifier.getType().hasCoding()) {
                    for (Coding coding : identifier.getType().getCoding()) {
                        if ("MR".equals(coding.getCode())) {
                            result.put("mrn", identifier.getValue());
                            break;
                        }
                    }
                }
            }
            result.put("identifiers", patient.getIdentifier().size());
        }

        // Extract Epic-specific extensions
        extractEpicExtensions(patient, result);

        return result;
    }

    @Override
    public Map<String, Object> mapEncounter(Encounter encounter) {
        Map<String, Object> result = new HashMap<>();

        if (encounter.hasId()) {
            result.put("id", encounter.getIdElement().getIdPart());
        }

        if (encounter.hasStatus()) {
            result.put("status", encounter.getStatus().name());
        }

        if (encounter.hasClass_()) {
            result.put("encounterClass", encounter.getClass_().getCode());
        }

        if (encounter.hasLocation() && !encounter.getLocation().isEmpty()) {
            Encounter.EncounterLocationComponent location = encounter.getLocation().get(0);
            if (location.hasLocation() && location.getLocation().hasDisplay()) {
                result.put("locationDisplay", location.getLocation().getDisplay());
            }
        }

        // Extract Epic-specific extensions
        extractEpicExtensions(encounter, result);

        return result;
    }

    @Override
    public Map<String, Object> mapObservation(Observation observation) {
        Map<String, Object> result = new HashMap<>();

        if (observation.hasId()) {
            result.put("id", observation.getIdElement().getIdPart());
        }

        if (observation.hasStatus()) {
            result.put("status", observation.getStatus().name());
        }

        if (observation.hasCode() && observation.getCode().hasCoding()) {
            for (Coding coding : observation.getCode().getCoding()) {
                if ("http://loinc.org".equals(coding.getSystem())) {
                    result.put("loincCode", coding.getCode());
                    result.put("display", coding.getDisplay());
                    break;
                }
            }
        }

        if (observation.hasValue()) {
            if (observation.getValue() instanceof Quantity) {
                Quantity quantity = (Quantity) observation.getValue();
                String valueString = quantity.getValue() + " " + quantity.getUnit();
                result.put("valueString", valueString);
            }
        }

        if (observation.hasReferenceRange() && !observation.getReferenceRange().isEmpty()) {
            Observation.ObservationReferenceRangeComponent range = observation.getReferenceRange().get(0);
            if (range.hasLow() && range.hasHigh()) {
                String rangeString = range.getLow().getValue() + "-" + range.getHigh().getValue();
                result.put("referenceRange", rangeString);
            }
        }

        if (observation.hasInterpretation() && !observation.getInterpretation().isEmpty()) {
            CodeableConcept interp = observation.getInterpretation().get(0);
            if (interp.hasCoding() && !interp.getCoding().isEmpty()) {
                result.put("interpretation", interp.getCoding().get(0).getCode());
            }
        }

        // Extract Epic-specific extensions
        extractEpicExtensions(observation, result);

        return result;
    }

    @Override
    public Map<String, Object> mapCondition(Condition condition) {
        Map<String, Object> result = new HashMap<>();

        if (condition.hasId()) {
            result.put("id", condition.getIdElement().getIdPart());
        }

        if (condition.hasClinicalStatus() && condition.getClinicalStatus().hasCoding()) {
            Coding coding = condition.getClinicalStatus().getCoding().get(0);
            result.put("clinicalStatus", coding.getCode());
        }

        if (condition.hasCode() && condition.getCode().hasCoding()) {
            for (Coding coding : condition.getCode().getCoding()) {
                if ("http://snomed.info/sct".equals(coding.getSystem())) {
                    result.put("snomedCode", coding.getCode());
                    result.put("display", coding.getDisplay());
                    break;
                }
            }
        }

        if (condition.hasOnset()) {
            if (condition.getOnset() instanceof DateTimeType) {
                DateTimeType onsetDate = (DateTimeType) condition.getOnset();
                result.put("onsetDate", DATE_FORMAT.format(onsetDate.getValue()));
            }
        }

        // Extract Epic-specific extensions
        extractEpicExtensions(condition, result);

        return result;
    }

    @Override
    public Map<String, Object> extractExtensions(Resource resource) {
        Map<String, Object> extensions = new HashMap<>();

        if (resource instanceof DomainResource domainResource && domainResource.hasExtension()) {
            for (Extension extension : domainResource.getExtension()) {
                String url = extension.getUrl();
                if (url != null && url.startsWith(EPIC_EXTENSION_BASE)) {
                    String key = url.substring(EPIC_EXTENSION_BASE.length());
                    Object value = extractExtensionValue(extension);
                    if (value != null) {
                        extensions.put(key, value);
                    }
                }
            }
        }

        return extensions;
    }

    /**
     * Extract Epic-specific extensions and add them to the result map.
     */
    private void extractEpicExtensions(Resource resource, Map<String, Object> result) {
        if (!(resource instanceof DomainResource domainResource) || !domainResource.hasExtension()) {
            return;
        }

        for (Extension extension : domainResource.getExtension()) {
            String url = extension.getUrl();
            if (url == null || !url.startsWith(EPIC_EXTENSION_BASE)) {
                continue;
            }

            String extensionName = url.substring(EPIC_EXTENSION_BASE.length());
            Object value = extractExtensionValue(extension);

            if (value != null) {
                // Map common Epic extensions to readable keys
                switch (extensionName) {
                    case "legal-sex":
                        result.put("epicLegalSex", value);
                        break;
                    case "patient-class":
                        result.put("epicPatientClass", value);
                        break;
                    case "mychart-status":
                        result.put("epicMyChartStatus", value);
                        break;
                    case "encounter-department":
                        result.put("epicDepartment", value);
                        break;
                    case "ordering-provider":
                        result.put("epicOrderingProvider", value);
                        break;
                    case "problem-list-status":
                        result.put("epicProblemListStatus", value);
                        break;
                    default:
                        result.put("epic" + capitalizeFirstLetter(extensionName), value);
                        break;
                }
            }
        }
    }

    /**
     * Extract the value from a FHIR extension.
     */
    private Object extractExtensionValue(Extension extension) {
        if (!extension.hasValue()) {
            return null;
        }

        Type value = extension.getValue();

        if (value instanceof StringType) {
            return ((StringType) value).getValue();
        } else if (value instanceof CodeType) {
            return ((CodeType) value).getValue();
        } else if (value instanceof BooleanType) {
            return ((BooleanType) value).getValue();
        } else if (value instanceof IntegerType) {
            return ((IntegerType) value).getValue();
        } else if (value instanceof DecimalType) {
            return ((DecimalType) value).getValue();
        } else if (value instanceof DateTimeType) {
            return DATE_FORMAT.format(((DateTimeType) value).getValue());
        } else if (value instanceof Coding) {
            return ((Coding) value).getCode();
        } else if (value instanceof CodeableConcept) {
            CodeableConcept cc = (CodeableConcept) value;
            if (cc.hasCoding() && !cc.getCoding().isEmpty()) {
                return cc.getCoding().get(0).getCode();
            }
        }

        return value.toString();
    }

    /**
     * Capitalize the first letter of a string.
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

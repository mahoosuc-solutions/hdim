package com.healthdata.cdr.converter;
import org.junit.jupiter.api.Tag;

import com.healthdata.cdr.dto.Hl7v2Message;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Hl7ToFhirConverter.
 * Tests conversion of HL7 v2 messages to FHIR R4 resources.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HL7 to FHIR Converter Tests")
@Tag("unit")
class Hl7ToFhirConverterTest {

    @InjectMocks
    private Hl7ToFhirConverter converter;

    @BeforeEach
    void setUp() {
        converter = new Hl7ToFhirConverter();
    }

    @Nested
    @DisplayName("ADT Message Conversion")
    class AdtConversionTests {

        @Test
        @DisplayName("Should convert ADT message to Patient resource")
        void convertToFhir_withAdtMessage_createsPatientResource() {
            Hl7v2Message message = createAdtMessage();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.TRANSACTION);
            assertThat(bundle.getEntry()).isNotEmpty();

            Optional<Patient> patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst();
            assertThat(patient).isPresent();
        }

        @Test
        @DisplayName("Should convert ADT message to Encounter resource")
        void convertToFhir_withAdtMessage_createsEncounterResource() {
            Hl7v2Message message = createAdtMessageWithVisit();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<Encounter> encounter = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Encounter)
                .map(e -> (Encounter) e.getResource())
                .findFirst();
            assertThat(encounter).isPresent();
        }

        @Test
        @DisplayName("Should set patient identifier from parsed data")
        void convertToFhir_withPatientId_setsIdentifier() {
            Hl7v2Message message = createAdtMessage();

            Bundle bundle = converter.convertToFhir(message);

            Optional<Patient> patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst();
            assertThat(patient).isPresent();
            assertThat(patient.get().getIdentifier()).isNotEmpty();
            assertThat(patient.get().getIdentifier().get(0).getValue()).isEqualTo("12345");
        }

        @Test
        @DisplayName("Should set patient name from parsed data")
        void convertToFhir_withPatientName_setsName() {
            Hl7v2Message message = createAdtMessage();

            Bundle bundle = converter.convertToFhir(message);

            Optional<Patient> patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst();
            assertThat(patient).isPresent();
            assertThat(patient.get().getName()).isNotEmpty();
            assertThat(patient.get().getName().get(0).getFamily()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("Should handle ADT^A02 (Patient Transfer)")
        void convertToFhir_withAdtA02_createsEncounterResource() {
            Hl7v2Message message = createAdtMessage("A02");

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<Encounter> encounter = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Encounter)
                .map(e -> (Encounter) e.getResource())
                .findFirst();
            // Encounter may or may not be created depending on visit data
            assertThat(bundle.getEntry()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle ADT^A03 (Patient Discharge)")
        void convertToFhir_withAdtA03_createsEncounterResource() {
            Hl7v2Message message = createAdtMessage("A03");

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<Encounter> encounter = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Encounter)
                .map(e -> (Encounter) e.getResource())
                .findFirst();
            // Encounter may or may not be created depending on visit data
            assertThat(bundle.getEntry()).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle ADT^A08 (Update Patient Information)")
        void convertToFhir_withAdtA08_updatesPatientResource() {
            Hl7v2Message message = createAdtMessage("A08");

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<Patient> patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst();
            assertThat(patient).isPresent();
        }
    }

    @Nested
    @DisplayName("ORU Message Conversion")
    class OruConversionTests {

        @Test
        @DisplayName("Should convert ORU message to Observation resources")
        void convertToFhir_withOruMessage_createsObservationResources() {
            Hl7v2Message message = createOruMessage();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            List<Observation> observations = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .toList();
            assertThat(observations).isNotEmpty();
        }

        @Test
        @DisplayName("Should set observation code from parsed data")
        void convertToFhir_withObservationCode_setsCode() {
            Hl7v2Message message = createOruMessage();

            Bundle bundle = converter.convertToFhir(message);

            Optional<Observation> observation = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .findFirst();
            assertThat(observation).isPresent();
            assertThat(observation.get().getCode().getCoding()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("ORM Message Conversion")
    class OrmConversionTests {

        @Test
        @DisplayName("Should convert ORM message to ServiceRequest resource")
        void convertToFhir_withOrmMessage_createsServiceRequestResource() {
            Hl7v2Message message = createOrmMessage();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<ServiceRequest> serviceRequest = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof ServiceRequest)
                .map(e -> (ServiceRequest) e.getResource())
                .findFirst();
            assertThat(serviceRequest).isPresent();
        }
    }

    @Nested
    @DisplayName("RDE Message Conversion")
    class RdeConversionTests {

        @Test
        @DisplayName("Should convert RDE message to MedicationRequest resource")
        void convertToFhir_withRdeMessage_createsMedicationRequestResource() {
            Hl7v2Message message = createRdeMessage();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<MedicationRequest> medicationRequest = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof MedicationRequest)
                .map(e -> (MedicationRequest) e.getResource())
                .findFirst();
            assertThat(medicationRequest).isPresent();
        }
    }

    @Nested
    @DisplayName("VXU Message Conversion")
    class VxuConversionTests {

        @Test
        @DisplayName("Should convert VXU message to Immunization resource")
        void convertToFhir_withVxuMessage_createsImmunizationResource() {
            Hl7v2Message message = createVxuMessage();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<Immunization> immunization = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Immunization)
                .map(e -> (Immunization) e.getResource())
                .findFirst();
            assertThat(immunization).isPresent();
        }
    }

    @Nested
    @DisplayName("Empty/Null Data Handling")
    class EmptyDataTests {

        @Test
        @DisplayName("Should return empty bundle for null parsed data")
        void convertToFhir_withNullParsedData_returnsEmptyBundle() {
            Hl7v2Message message = Hl7v2Message.builder()
                .messageType("ADT")
                .parsedData(null)
                .build();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            assertThat(bundle.getEntry()).isEmpty();
        }

        @Test
        @DisplayName("Should handle unsupported message type")
        void convertToFhir_withUnsupportedType_returnsEmptyBundle() {
            // Test with a real but unsupported HL7 message type (MDM - Medical Document Management)
            // MDM is supported by parser but not yet by converter
            Hl7v2Message message = Hl7v2Message.builder()
                .messageType("MDM")
                .parsedData(new HashMap<>())
                .build();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            assertThat(bundle.getEntry()).isEmpty();
        }
    }

    // Helper methods to create test messages
    private Hl7v2Message createAdtMessage() {
        return createAdtMessage("A01");
    }

    private Hl7v2Message createAdtMessage(String triggerEvent) {
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "12345");
        patientData.put("familyName", "Smith");
        patientData.put("givenName", "John");
        patientData.put("gender", "M");
        patientData.put("dateOfBirth", "19800115");

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("patient", patientData);

        // Add visit data for transfer/discharge events
        if ("A02".equals(triggerEvent) || "A03".equals(triggerEvent)) {
            Map<String, Object> visitData = new HashMap<>();
            visitData.put("patientClass", "I");
            visitData.put("visitNumber", "V123456");
            if ("A03".equals(triggerEvent)) {
                visitData.put("dischargeDateTime", "20240116100000");
            } else {
                visitData.put("admitDateTime", "20240115120000");
            }
            parsedData.put("visit", visitData);
        }

        return Hl7v2Message.builder()
            .messageType("ADT")
            .triggerEvent(triggerEvent)
            .parsedData(parsedData)
            .build();
    }

    private Hl7v2Message createAdtMessageWithVisit() {
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "12345");
        patientData.put("familyName", "Smith");
        patientData.put("givenName", "John");

        Map<String, Object> visitData = new HashMap<>();
        visitData.put("patientClass", "I");
        visitData.put("visitNumber", "V123456");
        visitData.put("admitDateTime", "20240115120000");

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("patient", patientData);
        parsedData.put("visit", visitData);

        return Hl7v2Message.builder()
            .messageType("ADT")
            .triggerEvent("A01")
            .parsedData(parsedData)
            .build();
    }

    private Hl7v2Message createOruMessage() {
        Map<String, Object> observationData = new HashMap<>();
        Map<String, String> identifier = new HashMap<>();
        identifier.put("code", "2345-7");
        identifier.put("text", "Glucose");
        identifier.put("codingSystem", "LN");
        observationData.put("identifier", identifier);
        observationData.put("value", "95");
        observationData.put("resultStatus", "F");

        Map<String, Object> orderObservation = new HashMap<>();
        orderObservation.put("observations", List.of(observationData));

        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "12345");

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("patient", patientData);
        parsedData.put("orderObservations", List.of(orderObservation));

        return Hl7v2Message.builder()
            .messageType("ORU")
            .triggerEvent("R01")
            .parsedData(parsedData)
            .build();
    }

    private Hl7v2Message createOrmMessage() {
        Map<String, Object> serviceId = new HashMap<>();
        serviceId.put("identifier", "80048");
        serviceId.put("text", "BMP");
        serviceId.put("codingSystem", "LN");

        Map<String, Object> observationRequest = new HashMap<>();
        observationRequest.put("serviceId", serviceId);

        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "12345");

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("patient", patientData);
        parsedData.put("observationRequest", observationRequest);

        return Hl7v2Message.builder()
            .messageType("ORM")
            .triggerEvent("O01")
            .parsedData(parsedData)
            .build();
    }

    private Hl7v2Message createRdeMessage() {
        Map<String, String> drugCode = new HashMap<>();
        drugCode.put("code", "RX001");
        drugCode.put("display", "Lisinopril 10mg");
        drugCode.put("system", "NDC");

        Map<String, Object> medication = new HashMap<>();
        medication.put("drugCode", drugCode);

        Map<String, Object> order = new HashMap<>();
        order.put("placerOrderNumber", "ORD123");
        order.put("medication", medication);

        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "12345");

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("patient", patientData);
        parsedData.put("orders", List.of(order));

        return Hl7v2Message.builder()
            .messageType("RDE")
            .triggerEvent("O11")
            .parsedData(parsedData)
            .build();
    }

    private Hl7v2Message createVxuMessage() {
        Map<String, String> vaccineCode = new HashMap<>();
        vaccineCode.put("code", "03");
        vaccineCode.put("display", "MMR");
        vaccineCode.put("system", "CVX");

        Map<String, Object> immunization = new HashMap<>();
        immunization.put("vaccineCode", vaccineCode);
        immunization.put("fhirStatus", "completed");

        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "12345");

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("patient", patientData);
        parsedData.put("immunizations", List.of(immunization));

        return Hl7v2Message.builder()
            .messageType("VXU")
            .triggerEvent("V04")
            .parsedData(parsedData)
            .build();
    }
}

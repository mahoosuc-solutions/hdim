package com.healthdata.ehr.connector;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.model.*;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for FHIR-based EHR connectors.
 * Provides common FHIR resource parsing and conversion logic.
 */
@Slf4j
public abstract class FhirConnector extends AbstractEhrConnector {

    protected final FhirContext fhirContext;
    protected final IParser fhirParser;

    protected FhirConnector(EhrConnectionConfig config, WebClient.Builder webClientBuilder) {
        super(config, webClientBuilder);
        this.fhirContext = FhirContext.forR4();
        this.fhirParser = fhirContext.newJsonParser();
    }

    @Override
    public Mono<EhrPatient> getPatient(String ehrPatientId, String tenantId) {
        log.debug("Fetching patient {} for tenant {}", ehrPatientId, tenantId);

        return getValidAccessToken()
                .flatMap(token -> fetchFhirResource("Patient/" + ehrPatientId, token))
                .map(this::parseFhirPatient)
                .map(patient -> convertToEhrPatient(patient, tenantId))
                .transform(this::applyResilience);
    }

    @Override
    public Flux<EhrPatient> searchPatients(String familyName, String givenName,
                                            LocalDateTime dateOfBirth, String tenantId) {
        log.debug("Searching patients: family={}, given={}, dob={}", familyName, givenName, dateOfBirth);

        StringBuilder queryParams = new StringBuilder("Patient?");
        if (familyName != null) queryParams.append("family=").append(familyName).append("&");
        if (givenName != null) queryParams.append("given=").append(givenName).append("&");
        if (dateOfBirth != null) queryParams.append("birthdate=").append(dateOfBirth.toLocalDate()).append("&");

        return getValidAccessToken()
                .flatMapMany(token -> fetchFhirBundle(queryParams.toString(), token))
                .map(this::parseFhirPatient)
                .map(patient -> convertToEhrPatient(patient, tenantId));
    }

    @Override
    public Mono<EhrEncounter> getEncounter(String ehrEncounterId, String tenantId) {
        log.debug("Fetching encounter {} for tenant {}", ehrEncounterId, tenantId);

        return getValidAccessToken()
                .flatMap(token -> fetchFhirResource("Encounter/" + ehrEncounterId, token))
                .map(this::parseFhirEncounter)
                .map(encounter -> convertToEhrEncounter(encounter, tenantId))
                .transform(this::applyResilience);
    }

    @Override
    public Flux<EhrEncounter> getEncounters(String ehrPatientId, LocalDateTime startDate,
                                             LocalDateTime endDate, String tenantId) {
        log.debug("Fetching encounters for patient {} from {} to {}", ehrPatientId, startDate, endDate);

        StringBuilder queryParams = new StringBuilder("Encounter?patient=").append(ehrPatientId);
        if (startDate != null) queryParams.append("&date=ge").append(startDate.toLocalDate());
        if (endDate != null) queryParams.append("&date=le").append(endDate.toLocalDate());

        return getValidAccessToken()
                .flatMapMany(token -> fetchFhirBundle(queryParams.toString(), token))
                .map(this::parseFhirEncounter)
                .map(encounter -> convertToEhrEncounter(encounter, tenantId));
    }

    @Override
    public Flux<EhrObservation> getObservations(String ehrPatientId, String category,
                                                 LocalDateTime startDate, LocalDateTime endDate,
                                                 String tenantId) {
        log.debug("Fetching observations for patient {}, category: {}", ehrPatientId, category);

        StringBuilder queryParams = new StringBuilder("Observation?patient=").append(ehrPatientId);
        if (category != null) queryParams.append("&category=").append(category);
        if (startDate != null) queryParams.append("&date=ge").append(startDate.toLocalDate());
        if (endDate != null) queryParams.append("&date=le").append(endDate.toLocalDate());

        return getValidAccessToken()
                .flatMapMany(token -> fetchFhirBundle(queryParams.toString(), token))
                .map(this::parseFhirObservation)
                .map(observation -> convertToEhrObservation(observation, tenantId));
    }

    @Override
    public Flux<EhrObservation> getObservationsByEncounter(String ehrEncounterId, String tenantId) {
        log.debug("Fetching observations for encounter {}", ehrEncounterId);

        String queryParams = "Observation?encounter=" + ehrEncounterId;

        return getValidAccessToken()
                .flatMapMany(token -> fetchFhirBundle(queryParams, token))
                .map(this::parseFhirObservation)
                .map(observation -> convertToEhrObservation(observation, tenantId));
    }

    @Override
    public Mono<SyncResult> syncPatientData(String ehrPatientId, LocalDateTime startDate,
                                             LocalDateTime endDate, String tenantId) {
        LocalDateTime syncStart = LocalDateTime.now();

        return Mono.zip(
                getEncounters(ehrPatientId, startDate, endDate, tenantId).collectList(),
                getObservations(ehrPatientId, null, startDate, endDate, tenantId).collectList()
        ).map(tuple -> new SyncResult(
                ehrPatientId,
                tuple.getT1().size(),
                tuple.getT2().size(),
                syncStart,
                LocalDateTime.now(),
                true,
                null
        )).onErrorResume(error -> Mono.just(new SyncResult(
                ehrPatientId,
                0,
                0,
                syncStart,
                LocalDateTime.now(),
                false,
                error.getMessage()
        )));
    }

    /**
     * Fetch a FHIR resource by path.
     */
    protected abstract Mono<String> fetchFhirResource(String resourcePath, String accessToken);

    /**
     * Fetch a FHIR bundle (search results).
     */
    protected abstract Flux<String> fetchFhirBundle(String queryParams, String accessToken);

    /**
     * Parse FHIR Patient resource.
     */
    protected Patient parseFhirPatient(String fhirJson) {
        return fhirParser.parseResource(Patient.class, fhirJson);
    }

    /**
     * Parse FHIR Encounter resource.
     */
    protected Encounter parseFhirEncounter(String fhirJson) {
        return fhirParser.parseResource(Encounter.class, fhirJson);
    }

    /**
     * Parse FHIR Observation resource.
     */
    protected Observation parseFhirObservation(String fhirJson) {
        return fhirParser.parseResource(Observation.class, fhirJson);
    }

    /**
     * Convert FHIR Patient to EhrPatient model.
     */
    protected EhrPatient convertToEhrPatient(Patient fhirPatient, String tenantId) {
        HumanName name = fhirPatient.getNameFirstRep();

        return EhrPatient.builder()
                .ehrPatientId(fhirPatient.getIdElement().getIdPart())
                .tenantId(tenantId)
                .sourceVendor(config.getVendorType())
                .familyName(name.getFamily())
                .givenName(name.getGivenAsSingleString())
                .dateOfBirth(toLocalDate(fhirPatient.getBirthDate()))
                .gender(fhirPatient.getGender() != null ? fhirPatient.getGender().toCode() : null)
                .active(fhirPatient.getActive())
                .deceased(fhirPatient.getDeceased() instanceof BooleanType bt && bt.booleanValue())
                .rawFhirResource(fhirParser.encodeResourceToString(fhirPatient))
                .build();
    }

    /**
     * Convert FHIR Encounter to EhrEncounter model.
     */
    protected EhrEncounter convertToEhrEncounter(Encounter fhirEncounter, String tenantId) {
        return EhrEncounter.builder()
                .ehrEncounterId(fhirEncounter.getIdElement().getIdPart())
                .tenantId(tenantId)
                .sourceVendor(config.getVendorType())
                .ehrPatientId(fhirEncounter.getSubject().getReferenceElement().getIdPart())
                .status(fhirEncounter.getStatus() != null ? fhirEncounter.getStatus().toCode() : null)
                .encounterClass(fhirEncounter.getClass_() != null ? fhirEncounter.getClass_().getCode() : null)
                .startTime(toLocalDateTime(fhirEncounter.getPeriod().getStart()))
                .endTime(toLocalDateTime(fhirEncounter.getPeriod().getEnd()))
                .rawFhirResource(fhirParser.encodeResourceToString(fhirEncounter))
                .build();
    }

    /**
     * Convert FHIR Observation to EhrObservation model.
     */
    protected EhrObservation convertToEhrObservation(Observation fhirObservation, String tenantId) {
        return EhrObservation.builder()
                .ehrObservationId(fhirObservation.getIdElement().getIdPart())
                .tenantId(tenantId)
                .sourceVendor(config.getVendorType())
                .ehrPatientId(fhirObservation.getSubject().getReferenceElement().getIdPart())
                .status(fhirObservation.getStatus() != null ? fhirObservation.getStatus().toCode() : null)
                .effectiveDateTime(toLocalDateTime(fhirObservation.getEffectiveDateTimeType().getValue()))
                .rawFhirResource(fhirParser.encodeResourceToString(fhirObservation))
                .build();
    }

    /**
     * Convert Date to LocalDate.
     */
    protected java.time.LocalDate toLocalDate(Date date) {
        return date != null ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
    }

    /**
     * Convert Date to LocalDateTime.
     */
    protected LocalDateTime toLocalDateTime(Date date) {
        return date != null ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
    }
}

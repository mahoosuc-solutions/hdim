package com.healthdata.fhir.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.fhir.persistence.AppointmentEntity;
import com.healthdata.fhir.persistence.AppointmentRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing FHIR Appointment resources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    private final AppointmentRepository appointmentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "appointments", key = "#tenantId + ':' + #result.idElement.idPart"),
            @CacheEvict(value = "appointmentsByPatient", allEntries = true)
    })
    public Appointment createAppointment(String tenantId, Appointment appointment, String createdBy) {
        log.info("Creating appointment for tenant: {}", tenantId);

        validateAppointment(appointment);
        UUID appointmentId = ensureAppointmentId(appointment);
        UUID patientId = extractPatientId(appointment);

        AppointmentEntity entity = toEntity(tenantId, appointmentId, patientId, appointment, createdBy, createdBy);
        AppointmentEntity saved = appointmentRepository.save(entity);

        Appointment savedAppointment = toFhirResource(saved);
        publishAppointmentEvent("fhir.appointments.created", tenantId, appointmentId.toString(), savedAppointment, createdBy);
        return savedAppointment;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "appointments", key = "#tenantId + ':' + #id")
    public Optional<Appointment> getAppointment(String tenantId, String id) {
        try {
            UUID appointmentId = UUID.fromString(id);
            return appointmentRepository.findByTenantIdAndId(tenantId, appointmentId)
                    .map(this::toFhirResource);
        } catch (IllegalArgumentException e) {
            log.error("Invalid appointment ID format: {}", id);
            return Optional.empty();
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "appointments", key = "#tenantId + ':' + #id"),
            @CacheEvict(value = "appointmentsByPatient", allEntries = true)
    })
    public Appointment updateAppointment(String tenantId, String id, Appointment appointment, String modifiedBy) {
        UUID appointmentId = UUID.fromString(id);

        AppointmentEntity existing = appointmentRepository.findByTenantIdAndId(tenantId, appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found: " + id));

        validateAppointment(appointment);
        appointment.setId(id);
        UUID patientId = extractPatientId(appointment);

        existing.setResourceJson(JSON_PARSER.encodeResourceToString(appointment));
        existing.setPatientId(patientId);
        updateExtractedFields(existing, appointment);

        AppointmentEntity updated = appointmentRepository.save(existing);
        Appointment updatedAppointment = toFhirResource(updated);

        publishAppointmentEvent("fhir.appointments.updated", tenantId, id, updatedAppointment, modifiedBy);
        return updatedAppointment;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "appointments", key = "#tenantId + ':' + #id"),
            @CacheEvict(value = "appointmentsByPatient", allEntries = true)
    })
    public void deleteAppointment(String tenantId, String id, String deletedBy) {
        UUID appointmentId = UUID.fromString(id);
        AppointmentEntity appointment = appointmentRepository.findByTenantIdAndId(tenantId, appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found: " + id));

        appointmentRepository.delete(appointment);
        publishAppointmentEvent("fhir.appointments.deleted", tenantId, id, null, deletedBy);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "appointmentsByPatient", key = "#tenantId + ':' + #patientId")
    public Bundle searchAppointmentsByPatient(String tenantId, String patientId, Pageable pageable) {
        UUID patientUuid = UUID.fromString(patientId);
        Page<AppointmentEntity> page = appointmentRepository.findByTenantIdAndPatientIdOrderByStartTimeAsc(
                tenantId, patientUuid, pageable);
        return createBundle(page.getContent(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Bundle searchAppointmentsByPatientAndDateRange(String tenantId, String patientId,
            LocalDateTime startDate, LocalDateTime endDate) {
        UUID patientUuid = UUID.fromString(patientId);
        List<AppointmentEntity> appointments = appointmentRepository.findByPatientAndDateRange(
                tenantId, patientUuid, startDate, endDate);
        return createBundle(appointments, appointments.size());
    }

    @Transactional(readOnly = true)
    public Bundle searchAppointmentsByDateRange(String tenantId, LocalDateTime startDate, LocalDateTime endDate) {
        List<AppointmentEntity> appointments = appointmentRepository.findByTenantAndDateRange(
                tenantId, startDate, endDate);
        return createBundle(appointments, appointments.size());
    }

    private void validateAppointment(Appointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment cannot be null");
        }
        if (!appointment.hasParticipant()) {
            throw new IllegalArgumentException("Appointment must have participants");
        }
        if (appointment.getStart() == null || appointment.getEnd() == null) {
            throw new IllegalArgumentException("Appointment must have start and end time");
        }
        extractPatientId(appointment);
    }

    private UUID ensureAppointmentId(Appointment appointment) {
        if (appointment.hasId()) {
            return UUID.fromString(appointment.getIdElement().getIdPart());
        }
        UUID newId = UUID.randomUUID();
        appointment.setId(newId.toString());
        return newId;
    }

    private UUID extractPatientId(Appointment appointment) {
        return appointment.getParticipant().stream()
                .map(participant -> participant.getActor())
                .filter(ref -> ref != null && ref.getReference() != null && ref.getReference().startsWith("Patient/"))
                .map(ref -> ref.getReference().substring(ref.getReference().lastIndexOf("/") + 1))
                .map(UUID::fromString)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Appointment must reference a Patient participant"));
    }

    private AppointmentEntity toEntity(String tenantId, UUID appointmentId, UUID patientId,
            Appointment appointment, String createdBy, String modifiedBy) {
        AppointmentEntity entity = AppointmentEntity.builder()
                .id(appointmentId)
                .tenantId(tenantId)
                .resourceType("Appointment")
                .resourceJson(JSON_PARSER.encodeResourceToString(appointment))
                .patientId(patientId)
                .createdBy(createdBy)
                .lastModifiedBy(modifiedBy)
                .build();
        updateExtractedFields(entity, appointment);
        return entity;
    }

    private void updateExtractedFields(AppointmentEntity entity, Appointment appointment) {
        if (appointment.hasStatus()) {
            entity.setStatus(appointment.getStatus().toCode());
        }
        entity.setStartTime(dateToLocalDateTime(appointment.getStart()));
        entity.setEndTime(dateToLocalDateTime(appointment.getEnd()));
        entity.setDescription(appointment.getDescription());

        if (appointment.hasAppointmentType()) {
            CodeableConcept type = appointment.getAppointmentType();
            if (type.hasCoding()) {
                Coding coding = type.getCodingFirstRep();
                entity.setAppointmentTypeCode(coding.getCode());
                entity.setAppointmentTypeDisplay(coding.getDisplay());
            }
        } else if (appointment.hasServiceType()) {
            CodeableConcept type = appointment.getServiceTypeFirstRep();
            if (type.hasCoding()) {
                Coding coding = type.getCodingFirstRep();
                entity.setAppointmentTypeCode(coding.getCode());
                entity.setAppointmentTypeDisplay(coding.getDisplay());
            }
        }

        String locationId = findParticipantReference(appointment, "Location/");
        if (locationId != null) {
            entity.setLocationId(locationId);
        }
        String practitionerId = findParticipantReference(appointment, "PractitionerRole/");
        if (practitionerId != null) {
            entity.setPractitionerId(practitionerId);
        }
    }

    private String findParticipantReference(Appointment appointment, String prefix) {
        return appointment.getParticipant().stream()
                .map(participant -> participant.getActor())
                .filter(ref -> ref != null && ref.getReference() != null && ref.getReference().startsWith(prefix))
                .map(Reference::getReference)
                .map(ref -> ref.substring(ref.lastIndexOf("/") + 1))
                .findFirst()
                .orElse(null);
    }

    private Appointment toFhirResource(AppointmentEntity entity) {
        return (Appointment) JSON_PARSER.parseResource(entity.getResourceJson());
    }

    private Bundle createBundle(List<AppointmentEntity> appointments, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        List<Bundle.BundleEntryComponent> entries = appointments.stream()
                .map(entity -> {
                    Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
                    entry.setResource(toFhirResource(entity));
                    entry.setFullUrl("Appointment/" + entity.getId());
                    return entry;
                })
                .collect(Collectors.toList());

        bundle.setEntry(entries);
        return bundle;
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void publishAppointmentEvent(
            String topic,
            String tenantId,
            String appointmentId,
            Appointment appointment,
            String actor) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("eventId", java.util.UUID.randomUUID().toString());
            event.put("eventType", topic);
            event.put("resourceType", "Appointment");
            event.put("resourceId", appointmentId);
            event.put("tenantId", tenantId);
            if (appointment != null) {
                event.put("patientId", extractPatientId(appointment).toString());
            }
            event.put("occurredAt", java.time.Instant.now().toString());
            event.put("actor", actor);
            if (appointment != null) {
                String json = JSON_PARSER.encodeResourceToString(appointment);
                try {
                    event.put("resource", objectMapper.readValue(json, java.util.Map.class));
                } catch (Exception e) {
                    event.put("resource", json);
                }
            }
            kafkaTemplate.send(topic, tenantId + ":" + appointmentId, event);
        } catch (Exception e) {
            log.error("Failed to publish appointment event", e);
        }
    }

    public static class AppointmentNotFoundException extends RuntimeException {
        public AppointmentNotFoundException(String message) {
            super(message);
        }
    }
}

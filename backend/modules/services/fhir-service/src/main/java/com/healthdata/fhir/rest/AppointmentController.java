package com.healthdata.fhir.rest;

import java.util.List;

import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.fhir.service.AppointmentService;
import com.healthdata.fhir.util.FhirDateRangeParser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController
@RequestMapping("/Appointment")
public class AppointmentController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> createAppointment(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String appointmentJson) {
        try {
            Appointment appointment = (Appointment) JSON_PARSER.parseResource(appointmentJson);
            Appointment created = appointmentService.createAppointment(tenantId, appointment, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/fhir/Appointment/" + created.getId())
                    .body(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getAppointment(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {
        return appointmentService.getAppointment(tenantId, id)
                .map(appointment -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(appointment)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> updateAppointment(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String appointmentJson) {
        try {
            Appointment appointment = (Appointment) JSON_PARSER.parseResource(appointmentJson);
            Appointment updated = appointmentService.updateAppointment(tenantId, id, appointment, userId);
            return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
        } catch (AppointmentService.AppointmentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id) {
        try {
            appointmentService.deleteAppointment(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (AppointmentService.AppointmentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> searchAppointments(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "date", required = false) List<String> dateParams,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            Bundle bundle;
            FhirDateRangeParser.DateRange range = FhirDateRangeParser.parseDateRange(dateParams);

            if (patientId != null && range != null) {
                bundle = appointmentService.searchAppointmentsByPatientAndDateRange(
                        tenantId, patientId, range.start(), range.end());
            } else if (patientId != null) {
                bundle = appointmentService.searchAppointmentsByPatient(tenantId, patientId, pageable);
            } else if (range != null) {
                bundle = appointmentService.searchAppointmentsByDateRange(tenantId, range.start(), range.end());
            } else {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"patient or date parameter is required\"}");
            }

            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}

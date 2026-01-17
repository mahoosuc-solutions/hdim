package com.healthdata.fhir.rest;

import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Task;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.fhir.service.TaskService;
import com.healthdata.fhir.util.FhirDateRangeParser;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController
@RequestMapping("/Task")
public class TaskController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> createTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String taskJson) {
        try {
            Task task = (Task) JSON_PARSER.parseResource(taskJson);
            Task created = taskService.createTask(tenantId, task, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/fhir/Task/" + created.getId())
                    .body(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> getTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {
        return taskService.getTask(tenantId, id)
                .map(task -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(task)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> updateTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String taskJson) {
        try {
            Task task = (Task) JSON_PARSER.parseResource(taskJson);
            Task updated = taskService.updateTask(tenantId, id, task, userId);
            return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
        } catch (TaskService.TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id) {
        try {
            taskService.deleteTask(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (TaskService.TaskNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    public ResponseEntity<String> searchTasks(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "authored-on", required = false) List<String> dateParams,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            Bundle bundle;
            FhirDateRangeParser.DateRange range = FhirDateRangeParser.parseDateRange(dateParams);

            if (patientId != null && range != null) {
                bundle = taskService.searchTasksByPatientAndDateRange(
                        tenantId, patientId, range.start(), range.end());
            } else if (patientId != null) {
                bundle = taskService.searchTasksByPatient(tenantId, patientId, pageable);
            } else if (range != null) {
                bundle = taskService.searchTasksByDateRange(tenantId, range.start(), range.end());
            } else {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"patient or authored-on parameter is required\"}");
            }

            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}

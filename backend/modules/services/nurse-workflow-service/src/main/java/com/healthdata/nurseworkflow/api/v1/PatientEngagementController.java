package com.healthdata.nurseworkflow.api.v1;

import com.healthdata.nurseworkflow.api.v1.dto.CreateEngagementThreadRequest;
import com.healthdata.nurseworkflow.api.v1.dto.CreateEscalationRequest;
import com.healthdata.nurseworkflow.api.v1.dto.PatientEngagementKpiResponse;
import com.healthdata.nurseworkflow.api.v1.dto.PostEngagementMessageRequest;
import com.healthdata.nurseworkflow.application.PatientEngagementService;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementEscalationEntity;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementMessageEntity;
import com.healthdata.nurseworkflow.domain.model.PatientEngagementThreadEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Patient-friendly explanation and secure async care-team messaging APIs.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/patient-engagement")
@RequiredArgsConstructor
@Tag(name = "Patient Engagement", description = "Secure patient-clinician engagement queue APIs")
@SecurityRequirement(name = "gateway-auth")
public class PatientEngagementController {

    private final PatientEngagementService patientEngagementService;

    @PostMapping("/threads")
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')")
    @Operation(summary = "Create secure patient engagement thread")
    public ResponseEntity<Map<String, Object>> createThread(
        @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
        @Valid @RequestBody CreateEngagementThreadRequest request
    ) {
        PatientEngagementService.EngagementThreadBundle bundle =
            patientEngagementService.createThread(tenantId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "thread", bundle.getThread(),
            "initialMessage", bundle.getInitialMessage()
        ));
    }

    @PostMapping("/threads/{threadId}/messages")
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')")
    @Operation(summary = "Post message to engagement thread")
    public ResponseEntity<PatientEngagementMessageEntity> postMessage(
        @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
        @PathVariable UUID threadId,
        @Valid @RequestBody PostEngagementMessageRequest request
    ) {
        PatientEngagementMessageEntity message =
            patientEngagementService.addMessage(tenantId, threadId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @GetMapping("/threads/{threadId}/messages")
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')")
    @Operation(summary = "Get ordered thread messages")
    public ResponseEntity<Page<PatientEngagementMessageEntity>> getMessages(
        @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
        @PathVariable UUID threadId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(
            patientEngagementService.getThreadMessages(tenantId, threadId, pageable)
        );
    }

    @PostMapping("/threads/{threadId}/escalations")
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_WRITE')")
    @Operation(summary = "Escalate thread to clinician and trigger secure email alert event")
    public ResponseEntity<PatientEngagementEscalationEntity> escalate(
        @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
        @PathVariable UUID threadId,
        @Valid @RequestBody CreateEscalationRequest request
    ) {
        PatientEngagementEscalationEntity escalation =
            patientEngagementService.escalateThread(tenantId, threadId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(escalation);
    }

    @GetMapping("/kpis/transitions")
    @PreAuthorize("hasPermission(#tenantId, 'PATIENT_READ')")
    @Operation(summary = "Get patient engagement transition KPI summary")
    public ResponseEntity<PatientEngagementKpiResponse> getKpis(
        @RequestHeader(value = "X-Tenant-ID", required = true) String tenantId,
        @RequestParam(value = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam(value = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return ResponseEntity.ok(patientEngagementService.getKpis(tenantId, from, to));
    }
}

package com.healthdata.healthixadapter.controller;

import com.healthdata.healthixadapter.audit.AtnaAuditService;
import com.healthdata.healthixadapter.ccda.CcdaIngestionService;
import com.healthdata.healthixadapter.fhir.FhirSubscriptionClient;
import com.healthdata.healthixadapter.hl7.Hl7AdtConsumer;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.MpiMatchRequest;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.MpiMatchResult;
import com.healthdata.healthixadapter.service.HealthixAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/external/healthix")
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@RequiredArgsConstructor
public class HealthixAdapterController {

    private final FhirSubscriptionClient fhirSubscriptionClient;
    private final Hl7AdtConsumer hl7AdtConsumer;
    private final VeratoMpiProxy mpiProxy;
    private final CcdaIngestionService ccdaIngestionService;
    private final HealthixAuditService auditService;
    private final AtnaAuditService atnaAuditService;

    @PostMapping("/fhir/notification")
    public ResponseEntity<Void> receiveFhirNotification(
            @RequestBody Map<String, Object> fhirResource,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        String correlationId = UUID.randomUUID().toString();
        String resourceType = (String) fhirResource.getOrDefault("resourceType", "Unknown");
        String resourceId = (String) fhirResource.getOrDefault("id", "");

        auditService.logPhiAccess(tenantId, "FHIR_NOTIFICATION_RECEIVED",
                resourceType, resourceId, null, correlationId);
        atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                tenantId, "FHIR_NOTIFICATION_RECEIVED", resourceType,
                resourceId, null, correlationId, "SUCCESS", null));

        fhirSubscriptionClient.handleFhirNotification(fhirResource, tenantId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/hl7/adt")
    public ResponseEntity<Void> receiveHl7Adt(
            @RequestBody Map<String, Object> hl7Message,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        String correlationId = UUID.randomUUID().toString();
        String patientId = (String) hl7Message.get("patientId");

        auditService.logPhiAccess(tenantId, "HL7_ADT_RECEIVED",
                "ADT", (String) hl7Message.get("triggerEvent"), patientId, correlationId);
        atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                tenantId, "HL7_ADT_RECEIVED", "ADT",
                (String) hl7Message.get("triggerEvent"), patientId, correlationId, "SUCCESS", null));

        hl7AdtConsumer.processAdtMessage(hl7Message, tenantId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/mpi/match")
    public ResponseEntity<MpiMatchResult> queryMpiMatch(
            @RequestBody MpiMatchRequest request) {
        String correlationId = UUID.randomUUID().toString();

        auditService.logPhiAccess(request.getTenantId(), "MPI_MATCH_QUERY",
                "Patient", null, null, correlationId);
        atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                request.getTenantId(), "MPI_MATCH_QUERY", "Patient",
                null, null, correlationId, "SUCCESS", null));

        return ResponseEntity.ok(mpiProxy.queryPatientMatch(request));
    }

    @PostMapping("/documents/webhook")
    public ResponseEntity<Void> documentWebhook(
            @RequestBody Map<String, Object> webhookPayload,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        String correlationId = UUID.randomUUID().toString();
        String documentId = (String) webhookPayload.get("documentId");

        auditService.logPhiAccess(tenantId, "DOCUMENT_WEBHOOK_RECEIVED",
                "Document", documentId, null, correlationId);
        atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                tenantId, "DOCUMENT_WEBHOOK_RECEIVED", "Document",
                documentId, null, correlationId, "SUCCESS", null));

        ccdaIngestionService.onDocumentWebhook(webhookPayload, tenantId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/documents/{documentId}/ingest")
    public ResponseEntity<Void> ingestDocument(
            @PathVariable String documentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        String correlationId = UUID.randomUUID().toString();

        auditService.logPhiAccess(tenantId, "DOCUMENT_INGESTION_REQUESTED",
                "Document", documentId, null, correlationId);
        atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                tenantId, "DOCUMENT_INGESTION_REQUESTED", "Document",
                documentId, null, correlationId, "SUCCESS", null));

        ccdaIngestionService.ingestDocument(documentId, tenantId);
        return ResponseEntity.accepted().build();
    }
}

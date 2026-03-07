package com.healthdata.hedisadapter.controller;

import com.healthdata.hedisadapter.audit.AtnaAuditService;
import com.healthdata.hedisadapter.crm.CrmSyncService;
import com.healthdata.hedisadapter.measures.CqlDelegationService;
import com.healthdata.hedisadapter.measures.CqlDelegationService.CalculationRequest;
import com.healthdata.hedisadapter.measures.CqlDelegationService.CalculationResult;
import com.healthdata.hedisadapter.measures.MeasureRegistrySyncService;
import com.healthdata.hedisadapter.websocket.KafkaToWebSocketBridge;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/external/hedis")
@ConditionalOnProperty(name = "external.hedis.enabled", havingValue = "true")
@RequiredArgsConstructor
public class HedisAdapterController {

    private final MeasureRegistrySyncService measureSyncService;
    private final CqlDelegationService cqlDelegationService;
    private final CrmSyncService crmSyncService;
    private final KafkaToWebSocketBridge webSocketBridge;
    private final AtnaAuditService atnaAuditService;

    @PostMapping("/measures/sync")
    public ResponseEntity<Void> triggerMeasureSync() {
        String correlationId = UUID.randomUUID().toString();
        try {
            measureSyncService.syncMeasureRegistry();
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    "SYSTEM", "MEASURE_REGISTRY_SYNC", "MeasureRegistry",
                    null, null, correlationId, "SUCCESS", null));
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    "SYSTEM", "MEASURE_REGISTRY_SYNC", "MeasureRegistry",
                    null, null, correlationId, "FAILURE", e.getMessage()));
            throw e;
        }
    }

    @PostMapping("/measures/calculate")
    public ResponseEntity<CalculationResult> calculateMeasure(
            @RequestBody CalculationRequest request) {
        String correlationId = UUID.randomUUID().toString();
        try {
            CalculationResult result = "STAR".equals(request.getMeasureType())
                    ? cqlDelegationService.calculateStarMeasure(request)
                    : cqlDelegationService.calculateMeasure(request);
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    "SYSTEM", "MEASURE_CALCULATION", "CalculationRequest",
                    null, null, correlationId, "SUCCESS", null));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    "SYSTEM", "MEASURE_CALCULATION", "CalculationRequest",
                    null, null, correlationId, "FAILURE", e.getMessage()));
            throw e;
        }
    }

    @PostMapping("/crm/webhook")
    public ResponseEntity<Void> crmWebhook(
            @RequestBody Map<String, Object> payload) {
        String correlationId = UUID.randomUUID().toString();
        try {
            crmSyncService.onCrmWebhook(payload);
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    "SYSTEM", "CRM_WEBHOOK_RECEIVED", "CrmWebhook",
                    null, null, correlationId, "SUCCESS", null));
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    "SYSTEM", "CRM_WEBHOOK_RECEIVED", "CrmWebhook",
                    null, null, correlationId, "FAILURE", e.getMessage()));
            throw e;
        }
    }

    @GetMapping("/stats/live")
    public ResponseEntity<Map<String, Object>> liveStats() {
        return ResponseEntity.ok(Map.of(
                "activeWebSocketConnections", webSocketBridge.getActiveConnectionCount(),
                "service", "hedis-adapter-service"
        ));
    }
}

package com.healthdata.corehiveadapter.controller;

import com.healthdata.corehiveadapter.audit.AtnaAuditService;
import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.model.CareGapScoringResponse;
import com.healthdata.corehiveadapter.model.VbcRoiRequest;
import com.healthdata.corehiveadapter.model.VbcRoiResponse;
import com.healthdata.corehiveadapter.service.CorehiveAdapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/external/corehive")
@ConditionalOnProperty(name = "external.corehive.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CorehiveAdapterController {

    private final CorehiveAdapterService adapterService;
    private final AtnaAuditService atnaAuditService;

    @PostMapping("/score")
    public ResponseEntity<CareGapScoringResponse> scoreCareGaps(
            @RequestBody CareGapScoringRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        String correlationId = UUID.randomUUID().toString();
        try {
            CareGapScoringResponse response = adapterService.scoreCareGaps(request, tenantId);
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    tenantId, "CARE_GAP_SCORING", "CareGapScoringRequest",
                    correlationId, null, correlationId, "SUCCESS", null));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    tenantId, "CARE_GAP_SCORING", "CareGapScoringRequest",
                    correlationId, null, correlationId, "FAILURE", e.getMessage()));
            throw e;
        }
    }

    @PostMapping("/roi")
    public ResponseEntity<VbcRoiResponse> calculateRoi(
            @RequestBody VbcRoiRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        String correlationId = UUID.randomUUID().toString();
        try {
            VbcRoiResponse response = adapterService.calculateRoi(request, tenantId);
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    tenantId, "VBC_ROI_CALCULATION", "VbcRoiRequest",
                    correlationId, null, correlationId, "SUCCESS", null));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
                    tenantId, "VBC_ROI_CALCULATION", "VbcRoiRequest",
                    correlationId, null, correlationId, "FAILURE", e.getMessage()));
            throw e;
        }
    }
}

package com.healthdata.gateway.admin.configversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/configs")
public class TenantServiceConfigController {

    private final TenantServiceConfigService configService;
    private final ObjectMapper objectMapper;

    public TenantServiceConfigController(
        TenantServiceConfigService configService,
        ObjectMapper objectMapper
    ) {
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{service}/tenants/{tenantId}/versions")
    public ResponseEntity<TenantServiceConfigResponse> createVersion(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId,
        @RequestBody CreateConfigVersionRequest request,
        Authentication authentication
    ) {
        TenantServiceConfigVersion version = configService.createVersion(
            tenantId,
            serviceName,
            request.config(),
            request.changeSummary(),
            request.activate() != null && request.activate(),
            actor(authentication)
        );
        return ResponseEntity.ok(toResponse(version));
    }

    @PostMapping("/{service}/tenants/{tenantId}/promote")
    public ResponseEntity<TenantServiceConfigResponse> promoteVersion(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId,
        @RequestBody PromoteConfigVersionRequest request,
        Authentication authentication
    ) {
        TenantServiceConfigVersion version = configService.promoteFromDemo(
            tenantId,
            serviceName,
            request.sourceVersionId(),
            request.changeSummary(),
            request.activate() == null || request.activate(),
            actor(authentication)
        );
        return ResponseEntity.ok(toResponse(version));
    }

    @PostMapping("/{service}/tenants/{tenantId}/activate/{versionId}")
    public ResponseEntity<TenantServiceConfigResponse> activateVersion(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId,
        @PathVariable UUID versionId,
        @RequestBody(required = false) ActivateConfigVersionRequest request,
        Authentication authentication
    ) {
        TenantServiceConfigVersion version = configService.activateVersion(
            tenantId,
            serviceName,
            versionId,
            actor(authentication),
            request != null ? request.reason() : "Activate configuration version"
        );
        return ResponseEntity.ok(toResponse(version));
    }

    @GetMapping("/{service}/tenants/{tenantId}/current")
    public ResponseEntity<TenantServiceConfigResponse> getCurrent(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId
    ) {
        return configService.getCurrentVersion(tenantId, serviceName)
            .map(version -> ResponseEntity.ok(toResponse(version)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{service}/tenants/{tenantId}/versions")
    public ResponseEntity<List<TenantServiceConfigResponse>> listVersions(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId
    ) {
        List<TenantServiceConfigResponse> versions = configService
            .listVersions(tenantId, serviceName)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{service}/tenants/{tenantId}/versions/{versionId}")
    public ResponseEntity<TenantServiceConfigResponse> getVersion(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId,
        @PathVariable UUID versionId
    ) {
        return configService.getVersion(tenantId, serviceName, versionId)
            .map(version -> ResponseEntity.ok(toResponse(version)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{service}/tenants/{tenantId}/audit")
    public ResponseEntity<List<TenantServiceConfigAuditResponse>> listAudit(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId
    ) {
        List<TenantServiceConfigAuditResponse> audits = configService
            .listAudit(tenantId, serviceName)
            .stream()
            .map(this::toAuditResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/{service}/tenants/{tenantId}/versions/{versionId}/approvals")
    public ResponseEntity<List<TenantServiceConfigApprovalResponse>> listApprovals(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId,
        @PathVariable UUID versionId
    ) {
        List<TenantServiceConfigApprovalResponse> approvals = configService
            .listApprovals(tenantId, serviceName, versionId)
            .stream()
            .map(this::toApprovalResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(approvals);
    }

    @PostMapping("/{service}/tenants/{tenantId}/versions/{versionId}/approvals/request")
    public ResponseEntity<TenantServiceConfigApprovalResponse> requestApproval(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId,
        @PathVariable UUID versionId,
        @RequestBody(required = false) ApprovalRequest request,
        Authentication authentication
    ) {
        TenantServiceConfigApproval approval = configService.requestApproval(
            tenantId,
            serviceName,
            versionId,
            actor(authentication),
            request != null ? request.comment() : null
        );
        return ResponseEntity.ok(toApprovalResponse(approval));
    }

    @PostMapping("/{service}/tenants/{tenantId}/versions/{versionId}/approvals/approve")
    public ResponseEntity<TenantServiceConfigApprovalResponse> approveVersion(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId,
        @PathVariable UUID versionId,
        @RequestBody(required = false) ApprovalRequest request,
        Authentication authentication
    ) {
        TenantServiceConfigApproval approval = configService.approveVersion(
            tenantId,
            serviceName,
            versionId,
            actor(authentication),
            request != null ? request.comment() : null
        );
        return ResponseEntity.ok(toApprovalResponse(approval));
    }

    @PostMapping("/{service}/tenants/{tenantId}/versions/{versionId}/approvals/reject")
    public ResponseEntity<TenantServiceConfigApprovalResponse> rejectVersion(
        @PathVariable("service") String serviceName,
        @PathVariable String tenantId,
        @PathVariable UUID versionId,
        @RequestBody(required = false) ApprovalRequest request,
        Authentication authentication
    ) {
        TenantServiceConfigApproval approval = configService.rejectVersion(
            tenantId,
            serviceName,
            versionId,
            actor(authentication),
            request != null ? request.comment() : null
        );
        return ResponseEntity.ok(toApprovalResponse(approval));
    }

    private TenantServiceConfigResponse toResponse(TenantServiceConfigVersion version) {
        return new TenantServiceConfigResponse(
            version.getId(),
            version.getTenantId(),
            version.getServiceName(),
            version.getVersionNumber(),
            version.getStatus().name(),
            readJson(version.getConfigJson()),
            version.getConfigHash(),
            version.getChangeSummary(),
            version.getSourceVersionId(),
            version.getCreatedBy(),
            version.getCreatedAt() != null ? version.getCreatedAt().toString() : null,
            version.getUpdatedAt() != null ? version.getUpdatedAt().toString() : null
        );
    }

    private TenantServiceConfigAuditResponse toAuditResponse(TenantServiceConfigAudit audit) {
        return new TenantServiceConfigAuditResponse(
            audit.getId(),
            audit.getTenantId(),
            audit.getServiceName(),
            audit.getVersionId(),
            audit.getAction().name(),
            audit.getActor(),
            readJson(audit.getDetails()),
            audit.getCreatedAt() != null ? audit.getCreatedAt().toString() : null
        );
    }

    private TenantServiceConfigApprovalResponse toApprovalResponse(TenantServiceConfigApproval approval) {
        return new TenantServiceConfigApprovalResponse(
            approval.getId(),
            approval.getTenantId(),
            approval.getServiceName(),
            approval.getVersionId(),
            approval.getAction().name(),
            approval.getActor(),
            approval.getComment(),
            approval.getCreatedAt() != null ? approval.getCreatedAt().toString() : null
        );
    }

    private JsonNode readJson(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private String actor(Authentication authentication) {
        return authentication != null && authentication.getName() != null
            ? authentication.getName()
            : "system";
    }

    public record CreateConfigVersionRequest(
        JsonNode config,
        String changeSummary,
        Boolean activate
    ) {}

    public record PromoteConfigVersionRequest(
        UUID sourceVersionId,
        String changeSummary,
        Boolean activate
    ) {}

    public record ActivateConfigVersionRequest(String reason) {}

    public record ApprovalRequest(String comment) {}

    public record TenantServiceConfigResponse(
        UUID id,
        String tenantId,
        String serviceName,
        int versionNumber,
        String status,
        JsonNode config,
        String configHash,
        String changeSummary,
        UUID sourceVersionId,
        String createdBy,
        String createdAt,
        String updatedAt
    ) {}

    public record TenantServiceConfigAuditResponse(
        UUID id,
        String tenantId,
        String serviceName,
        UUID versionId,
        String action,
        String actor,
        JsonNode details,
        String createdAt
    ) {}

    public record TenantServiceConfigApprovalResponse(
        UUID id,
        String tenantId,
        String serviceName,
        UUID versionId,
        String action,
        String actor,
        String comment,
        String createdAt
    ) {}
}

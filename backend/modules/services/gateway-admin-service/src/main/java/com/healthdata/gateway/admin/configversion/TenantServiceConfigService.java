package com.healthdata.gateway.admin.configversion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TenantServiceConfigService {

    private final TenantServiceConfigVersionRepository versionRepository;
    private final TenantServiceConfigCurrentRepository currentRepository;
    private final TenantServiceConfigAuditRepository auditRepository;
    private final TenantServiceConfigApprovalRepository approvalRepository;
    private final ObjectMapper objectMapper;
    private final ConfigPromotionProperties promotionProperties;

    public TenantServiceConfigService(
        TenantServiceConfigVersionRepository versionRepository,
        TenantServiceConfigCurrentRepository currentRepository,
        TenantServiceConfigAuditRepository auditRepository,
        TenantServiceConfigApprovalRepository approvalRepository,
        ObjectMapper objectMapper,
        ConfigPromotionProperties promotionProperties
    ) {
        this.versionRepository = versionRepository;
        this.currentRepository = currentRepository;
        this.auditRepository = auditRepository;
        this.approvalRepository = approvalRepository;
        this.objectMapper = objectMapper.copy()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.promotionProperties = promotionProperties;
    }

    public TenantServiceConfigVersion createVersion(
        String tenantId,
        String serviceName,
        JsonNode config,
        String changeSummary,
        boolean activate,
        String actor
    ) {
        enforceDemoTenant(tenantId);

        String configJson = serializeConfig(config);
        String configHash = sha256(configJson);
        int nextVersion = nextVersionNumber(tenantId, serviceName);

        TenantServiceConfigVersion version = new TenantServiceConfigVersion();
        version.setTenantId(tenantId);
        version.setServiceName(serviceName);
        version.setVersionNumber(nextVersion);
        version.setStatus(TenantServiceConfigVersion.Status.DRAFT);
        version.setConfigJson(configJson);
        version.setConfigHash(configHash);
        version.setChangeSummary(changeSummary);
        version.setCreatedBy(actor);
        versionRepository.save(version);

        recordAudit(version, TenantServiceConfigAudit.Action.CREATE, actor,
            changeSummaryDetails(changeSummary));

        if (activate) {
            activateVersion(tenantId, serviceName, version.getId(), actor, "Activated on create");
        }

        return version;
    }

    public TenantServiceConfigVersion promoteFromDemo(
        String tenantId,
        String serviceName,
        UUID sourceVersionId,
        String changeSummary,
        boolean activate,
        String actor
    ) {
        String demoTenantId = promotionProperties.getDemoTenantId();
        TenantServiceConfigVersion sourceVersion = versionRepository.findById(sourceVersionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source version not found"));

        if (!demoTenantId.equals(sourceVersion.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Source version must come from demo tenant");
        }
        if (promotionProperties.isRequireDemo() && sourceVersion.getStatus() != TenantServiceConfigVersion.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Source demo version must be active");
        }

        int nextVersion = nextVersionNumber(tenantId, serviceName);
        TenantServiceConfigVersion version = new TenantServiceConfigVersion();
        version.setTenantId(tenantId);
        version.setServiceName(serviceName);
        version.setVersionNumber(nextVersion);
        boolean approvalRequired = requiresApproval(tenantId);
        boolean activateNow = activate && !approvalRequired;
        version.setStatus(approvalRequired
            ? TenantServiceConfigVersion.Status.PENDING_APPROVAL
            : activateNow ? TenantServiceConfigVersion.Status.ACTIVE : TenantServiceConfigVersion.Status.DRAFT);
        version.setConfigJson(sourceVersion.getConfigJson());
        version.setConfigHash(sourceVersion.getConfigHash());
        version.setChangeSummary(changeSummary);
        version.setSourceVersionId(sourceVersion.getId());
        version.setCreatedBy(actor);
        versionRepository.save(version);

        recordAudit(version, TenantServiceConfigAudit.Action.PROMOTE, actor,
            promotionDetails(sourceVersionId, changeSummary));

        if (approvalRequired) {
            recordApproval(version, TenantServiceConfigApproval.Action.REQUESTED, actor, changeSummary);
            recordAudit(version, TenantServiceConfigAudit.Action.APPROVAL_REQUESTED, actor,
                Map.of("reason", "Promotion requires two-person approval"));
        }

        if (activateNow) {
            activateVersion(tenantId, serviceName, version.getId(), actor, "Activated on promote");
        }

        return version;
    }

    public TenantServiceConfigVersion activateVersion(
        String tenantId,
        String serviceName,
        UUID versionId,
        String actor,
        String reason
    ) {
        TenantServiceConfigVersion version = versionRepository
            .findByIdAndTenantIdAndServiceName(versionId, tenantId, serviceName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        ensureApprovedForActivation(version);

        Optional<TenantServiceConfigCurrent> currentOpt = currentRepository.findByTenantIdAndServiceName(
            tenantId, serviceName);
        currentOpt.ifPresent(current -> {
            if (!current.getActiveVersionId().equals(versionId)) {
                versionRepository.findById(current.getActiveVersionId()).ifPresent(previous -> {
                    previous.setStatus(TenantServiceConfigVersion.Status.SUPERSEDED);
                    versionRepository.save(previous);
                });
            }
        });

        version.setStatus(TenantServiceConfigVersion.Status.ACTIVE);
        versionRepository.save(version);

        TenantServiceConfigCurrent current = currentOpt.orElseGet(TenantServiceConfigCurrent::new);
        current.setTenantId(tenantId);
        current.setServiceName(serviceName);
        current.setActiveVersionId(version.getId());
        current.setUpdatedBy(actor);
        current.setUpdatedAt(Instant.now());
        currentRepository.save(current);

        recordAudit(version, TenantServiceConfigAudit.Action.ACTIVATE, actor,
            Map.of("reason", reason));

        return version;
    }

    public List<TenantServiceConfigVersion> listVersions(String tenantId, String serviceName) {
        return versionRepository.findByTenantIdAndServiceNameOrderByVersionNumberDesc(tenantId, serviceName);
    }

    public Optional<TenantServiceConfigVersion> getCurrentVersion(String tenantId, String serviceName) {
        return currentRepository.findByTenantIdAndServiceName(tenantId, serviceName)
            .flatMap(current -> versionRepository.findById(current.getActiveVersionId()));
    }

    public Optional<TenantServiceConfigVersion> getVersion(String tenantId, String serviceName, UUID versionId) {
        return versionRepository.findByIdAndTenantIdAndServiceName(versionId, tenantId, serviceName);
    }

    public List<TenantServiceConfigAudit> listAudit(String tenantId, String serviceName) {
        return auditRepository.findByTenantIdAndServiceNameOrderByCreatedAtDesc(tenantId, serviceName);
    }

    public TenantServiceConfigApproval requestApproval(
        String tenantId,
        String serviceName,
        UUID versionId,
        String actor,
        String comment
    ) {
        TenantServiceConfigVersion version = getVersionOrThrow(tenantId, serviceName, versionId);
        if (version.getStatus() == TenantServiceConfigVersion.Status.SUPERSEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot request approval for superseded version");
        }

        if (version.getStatus() != TenantServiceConfigVersion.Status.ACTIVE) {
            version.setStatus(TenantServiceConfigVersion.Status.PENDING_APPROVAL);
            versionRepository.save(version);
        }

        TenantServiceConfigApproval approval = recordApproval(
            version,
            TenantServiceConfigApproval.Action.REQUESTED,
            actor,
            comment
        );
        recordAudit(version, TenantServiceConfigAudit.Action.APPROVAL_REQUESTED, actor,
            comment != null ? Map.of("comment", comment) : null);
        return approval;
    }

    public TenantServiceConfigApproval approveVersion(
        String tenantId,
        String serviceName,
        UUID versionId,
        String actor,
        String comment
    ) {
        TenantServiceConfigVersion version = getVersionOrThrow(tenantId, serviceName, versionId);
        if (version.getStatus() == TenantServiceConfigVersion.Status.SUPERSEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot approve a superseded version");
        }
        if (version.getStatus() == TenantServiceConfigVersion.Status.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rejected version requires a new approval request");
        }
        if (version.getStatus() == TenantServiceConfigVersion.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Version is already active");
        }
        if (version.getCreatedBy() != null && version.getCreatedBy().equals(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Approval must be performed by a different user");
        }
        if (approvalRepository.existsByTenantIdAndServiceNameAndVersionIdAndActorAndAction(
            tenantId, serviceName, versionId, actor, TenantServiceConfigApproval.Action.APPROVED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approval already recorded for this user");
        }

        TenantServiceConfigApproval approval = recordApproval(
            version,
            TenantServiceConfigApproval.Action.APPROVED,
            actor,
            comment
        );
        recordAudit(version, TenantServiceConfigAudit.Action.APPROVAL_APPROVED, actor,
            comment != null ? Map.of("comment", comment) : null);

        if (requiresApproval(tenantId) && hasTwoPersonApproval(version)) {
            version.setStatus(TenantServiceConfigVersion.Status.APPROVED);
            versionRepository.save(version);
        }

        return approval;
    }

    public TenantServiceConfigApproval rejectVersion(
        String tenantId,
        String serviceName,
        UUID versionId,
        String actor,
        String comment
    ) {
        TenantServiceConfigVersion version = getVersionOrThrow(tenantId, serviceName, versionId);
        if (version.getStatus() == TenantServiceConfigVersion.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reject an active version");
        }
        if (approvalRepository.existsByTenantIdAndServiceNameAndVersionIdAndActorAndAction(
            tenantId, serviceName, versionId, actor, TenantServiceConfigApproval.Action.REJECTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rejection already recorded for this user");
        }

        TenantServiceConfigApproval approval = recordApproval(
            version,
            TenantServiceConfigApproval.Action.REJECTED,
            actor,
            comment
        );
        version.setStatus(TenantServiceConfigVersion.Status.REJECTED);
        versionRepository.save(version);

        recordAudit(version, TenantServiceConfigAudit.Action.APPROVAL_REJECTED, actor,
            comment != null ? Map.of("comment", comment) : null);
        return approval;
    }

    public List<TenantServiceConfigApproval> listApprovals(String tenantId, String serviceName, UUID versionId) {
        return approvalRepository.findByTenantIdAndServiceNameAndVersionIdOrderByCreatedAtAsc(
            tenantId, serviceName, versionId);
    }

    private int nextVersionNumber(String tenantId, String serviceName) {
        return versionRepository.findTopByTenantIdAndServiceNameOrderByVersionNumberDesc(tenantId, serviceName)
            .map(version -> version.getVersionNumber() + 1)
            .orElse(1);
    }

    private void recordAudit(
        TenantServiceConfigVersion version,
        TenantServiceConfigAudit.Action action,
        String actor,
        Map<String, Object> details
    ) {
        TenantServiceConfigAudit audit = new TenantServiceConfigAudit();
        audit.setTenantId(version.getTenantId());
        audit.setServiceName(version.getServiceName());
        audit.setVersionId(version.getId());
        audit.setAction(action);
        audit.setActor(actor);
        audit.setDetails(serializeDetails(details));
        auditRepository.save(audit);
    }

    private Map<String, Object> changeSummaryDetails(String changeSummary) {
        if (changeSummary == null || changeSummary.isBlank()) {
            return null;
        }
        return Map.of("changeSummary", changeSummary);
    }

    private Map<String, Object> promotionDetails(UUID sourceVersionId, String changeSummary) {
        Map<String, Object> details = new HashMap<>();
        details.put("sourceVersionId", sourceVersionId.toString());
        if (changeSummary != null && !changeSummary.isBlank()) {
            details.put("changeSummary", changeSummary);
        }
        return details;
    }

    private TenantServiceConfigApproval recordApproval(
        TenantServiceConfigVersion version,
        TenantServiceConfigApproval.Action action,
        String actor,
        String comment
    ) {
        TenantServiceConfigApproval approval = new TenantServiceConfigApproval();
        approval.setTenantId(version.getTenantId());
        approval.setServiceName(version.getServiceName());
        approval.setVersionId(version.getId());
        approval.setAction(action);
        approval.setActor(actor);
        approval.setComment(comment);
        return approvalRepository.save(approval);
    }

    private String serializeConfig(JsonNode config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid config payload");
        }
    }

    private String serializeDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private void enforceDemoTenant(String tenantId) {
        if (!promotionProperties.isRequireDemo()) {
            return;
        }
        if (!promotionProperties.getDemoTenantId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Config changes must be created in the demo tenant first");
        }
    }

    private TenantServiceConfigVersion getVersionOrThrow(String tenantId, String serviceName, UUID versionId) {
        return versionRepository.findByIdAndTenantIdAndServiceName(versionId, tenantId, serviceName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    }

    private boolean requiresApproval(String tenantId) {
        return promotionProperties.isRequireTwoPersonApproval()
            && !promotionProperties.getDemoTenantId().equals(tenantId);
    }

    private boolean hasTwoPersonApproval(TenantServiceConfigVersion version) {
        long approvers = approvalRepository.countDistinctActors(
            version.getTenantId(),
            version.getServiceName(),
            version.getId(),
            TenantServiceConfigApproval.Action.APPROVED
        );
        return approvers >= 1;
    }

    private void ensureApprovedForActivation(TenantServiceConfigVersion version) {
        if (!requiresApproval(version.getTenantId())) {
            return;
        }
        if (version.getStatus() == TenantServiceConfigVersion.Status.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Version is rejected and cannot be activated");
        }
        if (!hasTwoPersonApproval(version)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Second-person approval required before activation");
        }
        if (version.getStatus() == TenantServiceConfigVersion.Status.PENDING_APPROVAL) {
            version.setStatus(TenantServiceConfigVersion.Status.APPROVED);
            versionRepository.save(version);
        }
    }
}

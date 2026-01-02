package com.healthdata.agentbuilder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentVersion;
import com.healthdata.agentbuilder.domain.entity.AgentVersion.VersionStatus;
import com.healthdata.agentbuilder.repository.AgentConfigurationRepository;
import com.healthdata.agentbuilder.repository.AgentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing agent versions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentVersionService {

    private final AgentVersionRepository versionRepository;
    private final AgentConfigurationRepository agentRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get all versions for an agent.
     */
    @Transactional(readOnly = true)
    public Page<AgentVersion> getVersions(UUID agentId, Pageable pageable) {
        return versionRepository.findByAgentConfigurationId(agentId, pageable);
    }

    /**
     * Get version history for an agent.
     */
    @Transactional(readOnly = true)
    public List<AgentVersion> getVersionHistory(UUID agentId) {
        return versionRepository.findByAgentConfigurationIdOrderByCreatedAtDesc(agentId);
    }

    /**
     * Get a specific version.
     */
    @Transactional(readOnly = true)
    public Optional<AgentVersion> getVersion(UUID agentId, String versionNumber) {
        return versionRepository.findByAgentConfigurationIdAndVersionNumber(agentId, versionNumber);
    }

    /**
     * Get the latest version.
     */
    @Transactional(readOnly = true)
    public Optional<AgentVersion> getLatestVersion(UUID agentId) {
        return versionRepository.findLatestVersion(agentId);
    }

    /**
     * Get the current published version.
     */
    @Transactional(readOnly = true)
    public Optional<AgentVersion> getPublishedVersion(UUID agentId) {
        return versionRepository.findLatestPublishedVersion(agentId);
    }

    /**
     * Rollback to a previous version.
     */
    @Transactional
    public AgentConfiguration rollback(String tenantId, UUID agentId, String targetVersionNumber, String reason, String userId) {
        AgentConfiguration agent = agentRepository.findByTenantIdAndId(tenantId, agentId)
            .orElseThrow(() -> new AgentConfigurationService.AgentNotFoundException("Agent not found: " + agentId));

        AgentVersion targetVersion = versionRepository.findByAgentConfigurationIdAndVersionNumber(agentId, targetVersionNumber)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Version not found: " + targetVersionNumber));

        // Mark current version as rolled back
        versionRepository.findLatestVersion(agentId).ifPresent(current -> {
            current.setStatus(VersionStatus.ROLLED_BACK);
            current.setRolledBackAt(Instant.now());
            current.setRolledBackBy(userId);
            current.setRollbackReason(reason);
            versionRepository.save(current);
        });

        // Restore configuration from snapshot
        Map<String, Object> snapshot = targetVersion.getConfigurationSnapshot();
        restoreFromSnapshot(agent, snapshot);

        // Create new version for the rollback
        String newVersion = incrementVersion(agent.getVersion());
        agent.setVersion(newVersion);
        agent.setUpdatedBy(userId);

        AgentConfiguration saved = agentRepository.save(agent);

        // Create new version record
        AgentVersion rollbackVersion = AgentVersion.builder()
            .agentConfiguration(saved)
            .versionNumber(newVersion)
            .configurationSnapshot(snapshot)
            .status(VersionStatus.DRAFT)
            .changeSummary("Rolled back to version " + targetVersionNumber + ": " + reason)
            .changeType(AgentVersion.ChangeType.PATCH)
            .createdBy(userId)
            .build();

        versionRepository.save(rollbackVersion);

        log.info("Rolled back agent {} from {} to {}", agentId, agent.getVersion(), targetVersionNumber);
        return saved;
    }

    /**
     * Compare two versions.
     */
    @Transactional(readOnly = true)
    public VersionDiff compareVersions(UUID agentId, String version1, String version2) {
        AgentVersion v1 = versionRepository.findByAgentConfigurationIdAndVersionNumber(agentId, version1)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Version not found: " + version1));

        AgentVersion v2 = versionRepository.findByAgentConfigurationIdAndVersionNumber(agentId, version2)
            .orElseThrow(() -> new AgentConfigurationService.AgentBuilderException("Version not found: " + version2));

        return new VersionDiff(
            version1,
            version2,
            v1.getConfigurationSnapshot(),
            v2.getConfigurationSnapshot(),
            v1.getCreatedAt(),
            v2.getCreatedAt()
        );
    }

    private void restoreFromSnapshot(AgentConfiguration agent, Map<String, Object> snapshot) {
        if (snapshot.containsKey("name")) {
            agent.setName((String) snapshot.get("name"));
        }
        if (snapshot.containsKey("description")) {
            agent.setDescription((String) snapshot.get("description"));
        }
        if (snapshot.containsKey("personaName")) {
            agent.setPersonaName((String) snapshot.get("personaName"));
        }
        if (snapshot.containsKey("personaRole")) {
            agent.setPersonaRole((String) snapshot.get("personaRole"));
        }
        if (snapshot.containsKey("modelProvider")) {
            agent.setModelProvider((String) snapshot.get("modelProvider"));
        }
        if (snapshot.containsKey("modelId")) {
            agent.setModelId((String) snapshot.get("modelId"));
        }
        if (snapshot.containsKey("systemPrompt")) {
            agent.setSystemPrompt((String) snapshot.get("systemPrompt"));
        }
        if (snapshot.containsKey("welcomeMessage")) {
            agent.setWelcomeMessage((String) snapshot.get("welcomeMessage"));
        }
        // Additional fields can be restored as needed
    }

    private String incrementVersion(String version) {
        String[] parts = version.split("\\.");
        int patch = Integer.parseInt(parts[2]) + 1;
        return parts[0] + "." + parts[1] + "." + patch;
    }

    public record VersionDiff(
        String version1,
        String version2,
        Map<String, Object> config1,
        Map<String, Object> config2,
        Instant timestamp1,
        Instant timestamp2
    ) {}
}

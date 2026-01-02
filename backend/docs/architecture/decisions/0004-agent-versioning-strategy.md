# ADR-0004: Agent Versioning and Rollback Strategy

## Status

Accepted

## Date

2024-12-06

## Context

AI agents in production healthcare environments require robust version management due to:

1. **Regulatory Compliance** - Must be able to demonstrate what agent behavior was active at any point in time
2. **Incident Response** - Need ability to quickly rollback problematic agent versions
3. **A/B Testing** - Compare agent performance across versions
4. **Change Management** - Healthcare IT requires approval workflows for production changes
5. **Audit Requirements** - All changes must be traceable to who, what, when, and why

Unlike traditional software versioning, AI agent versions involve prompt changes, model swaps, and tool configuration updates - all of which can dramatically alter behavior.

## Decision

We will implement a **comprehensive versioning system** with immutable snapshots, semantic versioning, and one-click rollback capabilities.

### Version Model

```
┌─────────────────────────────────────────────────────────────────┐
│                    VERSION MANAGEMENT                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  AgentConfiguration (mutable working copy)                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  id: uuid                                                │    │
│  │  name: "Care Gap Optimizer"                              │    │
│  │  version: "2.3.0"  ◄───── Current semantic version       │    │
│  │  status: ACTIVE                                          │    │
│  │  ...configuration fields...                              │    │
│  └─────────────────────────────────────────────────────────┘    │
│                              │                                   │
│                              │ creates on every save             │
│                              ▼                                   │
│  AgentVersion (immutable snapshots)                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  id: uuid                                                │    │
│  │  agent_configuration_id: uuid (FK)                       │    │
│  │  version_number: "2.3.0"                                 │    │
│  │  configuration_snapshot: JSONB (complete frozen state)   │    │
│  │  status: DRAFT | ACTIVE | ROLLED_BACK                    │    │
│  │  change_summary: "Updated clinical safety guardrails"    │    │
│  │  created_by: "user-123"                                  │    │
│  │  created_at: timestamp                                   │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Semantic Versioning Rules

| Change Type | Version Bump | Examples |
|-------------|--------------|----------|
| **Major** (breaking) | X.0.0 | Model provider change, tool removal, fundamental prompt rewrite |
| **Minor** (feature) | x.Y.0 | New tool added, significant prompt enhancement, guardrail addition |
| **Patch** (fix) | x.y.Z | Typo fix, minor prompt tweak, configuration adjustment |

### Version Lifecycle

```
                    ┌─────────────────────────────────────────────┐
                    │              VERSION STATES                  │
                    └─────────────────────────────────────────────┘

    ┌─────────┐         ┌─────────┐         ┌─────────────┐
    │  DRAFT  │ ──────► │ ACTIVE  │ ──────► │ ROLLED_BACK │
    └─────────┘ publish └─────────┘ rollback└─────────────┘
         │                   │
         │                   │ new version published
         │                   ▼
         │              ┌─────────────┐
         │              │  SUPERSEDED │
         │              └─────────────┘
         │
         └──────────────► delete (if never published)
```

### Implementation Details

```java
@Service
@RequiredArgsConstructor
public class AgentVersionService {

    private final AgentVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new version snapshot when agent is modified.
     */
    @Transactional
    public AgentVersion createVersion(
            AgentConfiguration config,
            String changeSummary,
            String userId) {

        // Convert config to snapshot
        Map<String, Object> snapshot = objectMapper.convertValue(config, Map.class);

        // Calculate next version number
        String nextVersion = calculateNextVersion(config);

        AgentVersion version = AgentVersion.builder()
            .id(UUID.randomUUID())
            .agentConfigurationId(config.getId())
            .versionNumber(nextVersion)
            .configurationSnapshot(snapshot)
            .status(AgentVersion.VersionStatus.DRAFT)
            .changeSummary(changeSummary)
            .createdBy(userId)
            .createdAt(Instant.now())
            .build();

        return versionRepository.save(version);
    }

    /**
     * Rollback to a specific version.
     */
    @Transactional
    public AgentConfiguration rollback(
            String tenantId,
            UUID agentId,
            UUID versionId,
            String userId) {

        AgentVersion targetVersion = versionRepository.findById(versionId)
            .orElseThrow(() -> new VersionNotFoundException(versionId));

        // Verify ownership
        AgentConfiguration current = agentRepository.findByTenantIdAndId(tenantId, agentId)
            .orElseThrow(() -> new AgentNotFoundException(agentId));

        // Mark current version as rolled back
        versionRepository.findByAgentConfigurationIdAndStatus(agentId, ACTIVE)
            .ifPresent(v -> {
                v.setStatus(ROLLED_BACK);
                versionRepository.save(v);
            });

        // Apply snapshot to current config
        Map<String, Object> snapshot = targetVersion.getConfigurationSnapshot();
        AgentConfiguration restored = objectMapper.convertValue(snapshot, AgentConfiguration.class);

        // Preserve identity fields
        restored.setId(current.getId());
        restored.setTenantId(current.getTenantId());
        restored.setCreatedAt(current.getCreatedAt());
        restored.setCreatedBy(current.getCreatedBy());

        // Save and create new version record
        AgentConfiguration saved = agentRepository.save(restored);

        createVersion(saved, "Rollback to version " + targetVersion.getVersionNumber(), userId);

        auditLogger.logRollback(tenantId, agentId, versionId, userId);

        return saved;
    }

    /**
     * Get version diff between two versions.
     */
    public VersionDiff diff(UUID versionId1, UUID versionId2) {
        AgentVersion v1 = versionRepository.findById(versionId1).orElseThrow();
        AgentVersion v2 = versionRepository.findById(versionId2).orElseThrow();

        return VersionDiff.compare(v1.getConfigurationSnapshot(), v2.getConfigurationSnapshot());
    }
}
```

### Database Schema

```sql
CREATE TABLE agent_versions (
    id UUID PRIMARY KEY,
    agent_configuration_id UUID NOT NULL REFERENCES agent_configurations(id),
    version_number VARCHAR(32) NOT NULL,
    configuration_snapshot JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    change_summary TEXT,
    parent_version_id UUID REFERENCES agent_versions(id),

    -- Audit fields
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT unique_agent_version UNIQUE (agent_configuration_id, version_number),

    -- Indexes
    INDEX idx_agent_versions_agent (agent_configuration_id),
    INDEX idx_agent_versions_status (status),
    INDEX idx_agent_versions_created (created_at DESC)
);

-- Version lineage view
CREATE VIEW version_lineage AS
SELECT
    v.id,
    v.agent_configuration_id,
    v.version_number,
    v.status,
    v.created_at,
    v.created_by,
    v.parent_version_id,
    p.version_number as parent_version
FROM agent_versions v
LEFT JOIN agent_versions p ON v.parent_version_id = p.id;
```

### API Endpoints

```
# Version Management
GET    /api/v1/agents/{agentId}/versions              # List all versions
GET    /api/v1/agents/{agentId}/versions/{versionId}  # Get specific version
GET    /api/v1/agents/{agentId}/versions/latest       # Get latest version
POST   /api/v1/agents/{agentId}/rollback              # Rollback to version

# Version Comparison
GET    /api/v1/versions/{v1}/diff/{v2}                # Compare two versions

# Version History
GET    /api/v1/agents/{agentId}/history               # Full change history
```

### Rollback Safeguards

```java
public class RollbackValidator {

    public void validate(AgentConfiguration current, AgentVersion target) {
        List<String> warnings = new ArrayList<>();

        // Check model availability
        if (!isModelAvailable(target.getModelProvider(), target.getModelId())) {
            throw new RollbackException("Target version uses unavailable model");
        }

        // Check tool compatibility
        Set<String> missingTools = findMissingTools(target.getEnabledTools());
        if (!missingTools.isEmpty()) {
            warnings.add("Tools no longer available: " + missingTools);
        }

        // Check for deprecated patterns
        if (containsDeprecatedPatterns(target.getSystemPrompt())) {
            warnings.add("Target version contains deprecated prompt patterns");
        }

        // Log warnings but allow rollback
        warnings.forEach(log::warn);
    }
}
```

### Version Retention Policy

| Environment | Retention | Notes |
|-------------|-----------|-------|
| Production | 100 versions or 2 years | Whichever is greater |
| Staging | 50 versions or 6 months | Auto-cleanup enabled |
| Development | 20 versions or 30 days | Aggressive cleanup |

```yaml
hdim:
  agent-builder:
    versioning:
      max-versions-per-agent: 100
      retention-days: 730  # 2 years
      auto-cleanup: true
      cleanup-schedule: "0 0 3 * * ?"  # 3 AM daily
```

## Consequences

### Positive

- **Instant Rollback** - One API call to restore any previous version
- **Complete Audit Trail** - Full history of who changed what and when
- **Regulatory Compliance** - Can demonstrate exact agent state at any point
- **Safe Experimentation** - Can always revert if new version fails
- **Version Comparison** - Visual diff between versions aids debugging
- **Branching Support** - Foundation for A/B testing and gradual rollout

### Negative

- **Storage Growth** - JSONB snapshots consume significant space
- **Query Complexity** - Version history queries can be expensive
- **Migration Overhead** - Schema changes require snapshot migration
- **Cleanup Complexity** - Retention policies need careful tuning
- **Consistency Risk** - Rollback to version with deleted tools fails

### Neutral

- All version operations are synchronous (sub-second)
- Snapshots are complete - no partial restoration needed
- Version numbers are tenant-scoped (two tenants can have v1.0.0)

## Implementation Files

- `AgentVersionService.java` - Core versioning logic
- `AgentVersionRepository.java` - Data access
- `VersionDiff.java` - Comparison utility
- `V003__create_agent_versions_table.sql` - Flyway migration

## References

- Semantic Versioning: https://semver.org/
- HDIM Plan: `/home/mahoosuc-solutions/.claude/plans/agile-launching-church.md`
- Related ADR: ADR-0002 No-Code Agent Builder Design

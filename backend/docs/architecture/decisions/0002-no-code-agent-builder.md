# ADR-0002: No-Code Agent Builder Design

## Status

Accepted

## Date

2024-12-06

## Context

Enterprise healthcare organizations need to customize AI agents for their specific clinical workflows, regulatory requirements, and patient populations. The challenges include:

1. **Technical Barrier** - Clinical informaticists and quality managers lack programming skills to modify AI behavior
2. **Rapid Iteration** - Healthcare protocols change frequently (new measures, guidelines, regulations)
3. **Tenant Customization** - Each organization has unique workflows and terminology
4. **Governance Requirements** - All agent changes must be auditable and reversible
5. **Safety Constraints** - Agents must operate within clinical safety guardrails

Building custom agents requires prompt engineering, tool configuration, and testing - skills not typically found in healthcare IT teams.

## Decision

We will implement a **No-Code Agent Builder** platform that enables tenant administrators to create, configure, test, and deploy AI agents without writing code.

### Core Components

```
┌─────────────────────────────────────────────────────────────────┐
│                     AGENT BUILDER SERVICE                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  Configuration  │  │    Version      │  │     Test        │ │
│  │    Service      │  │    Service      │  │    Service      │ │
│  │                 │  │                 │  │                 │ │
│  │ • CRUD agents   │  │ • Snapshots     │  │ • Sandbox       │ │
│  │ • Validation    │  │ • Rollback      │  │ • Scenarios     │ │
│  │ • Publishing    │  │ • Diff/compare  │  │ • Results       │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │    Template     │  │    Guardrail    │  │    Runtime      │ │
│  │    Service      │  │    Service      │  │    Client       │ │
│  │                 │  │                 │  │                 │ │
│  │ • Prompt libs   │  │ • Safety rules  │  │ • Execution     │ │
│  │ • Variables     │  │ • PHI filters   │  │ • Health check  │ │
│  │ • Rendering     │  │ • Blocklists    │  │ • Monitoring    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Data Model

```sql
-- Agent Configuration (tenant-isolated)
CREATE TABLE agent_configurations (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE,
    description TEXT,
    status VARCHAR(32) NOT NULL,  -- DRAFT, TESTING, ACTIVE, DEPRECATED, ARCHIVED
    version VARCHAR(32) NOT NULL,

    -- Persona Configuration
    persona_name VARCHAR(255),
    persona_role VARCHAR(255),
    persona_traits JSONB,

    -- Model Configuration
    model_provider VARCHAR(64),
    model_id VARCHAR(128),
    temperature DECIMAL(3,2),
    max_tokens INTEGER,

    -- Prompts
    system_prompt TEXT,
    user_prompt_template TEXT,
    prompt_templates JSONB,

    -- Tools & Guardrails
    enabled_tools JSONB,
    tool_configuration JSONB,
    guardrail_configuration JSONB,

    -- Audit
    created_by VARCHAR(100),
    created_at TIMESTAMP,
    updated_by VARCHAR(100),
    updated_at TIMESTAMP,
    published_by VARCHAR(100),
    published_at TIMESTAMP,
    archived_at TIMESTAMP,

    CONSTRAINT unique_tenant_name UNIQUE (tenant_id, name),
    INDEX idx_tenant_status (tenant_id, status)
);

-- Version History (immutable snapshots)
CREATE TABLE agent_versions (
    id UUID PRIMARY KEY,
    agent_configuration_id UUID REFERENCES agent_configurations(id),
    version_number VARCHAR(32),
    configuration_snapshot JSONB NOT NULL,
    status VARCHAR(32),  -- DRAFT, ACTIVE, ROLLED_BACK
    change_summary TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP
);

-- Prompt Templates (reusable)
CREATE TABLE prompt_templates (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    category VARCHAR(64),  -- CLINICAL_SAFETY, COMPLIANCE, PERSONA, TASK_SPECIFIC
    content TEXT NOT NULL,
    variables JSONB,
    is_system BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(100),
    created_at TIMESTAMP
);

-- Test Sessions
CREATE TABLE agent_test_sessions (
    id UUID PRIMARY KEY,
    agent_configuration_id UUID REFERENCES agent_configurations(id),
    status VARCHAR(32),  -- RUNNING, COMPLETED, FAILED
    test_input JSONB,
    test_output JSONB,
    execution_time_ms INTEGER,
    tokens_used INTEGER,
    created_by VARCHAR(100),
    created_at TIMESTAMP
);
```

### Agent Lifecycle

```
    ┌─────────┐
    │  DRAFT  │ ─────────────────┐
    └────┬────┘                  │
         │ save                  │ delete
         ▼                       ▼
    ┌─────────┐            ┌──────────┐
    │ TESTING │            │ ARCHIVED │
    └────┬────┘            └──────────┘
         │ publish               ▲
         ▼                       │
    ┌─────────┐                  │
    │ ACTIVE  │──────────────────┘
    └────┬────┘   deprecate
         │
         ▼
    ┌────────────┐
    │ DEPRECATED │
    └────────────┘
```

### API Surface

```
# Agent CRUD
POST   /api/v1/agents                    # Create agent
GET    /api/v1/agents                    # List agents (paginated)
GET    /api/v1/agents/{id}               # Get agent
PUT    /api/v1/agents/{id}               # Update agent
DELETE /api/v1/agents/{id}               # Archive agent
POST   /api/v1/agents/{id}/clone         # Clone agent

# Agent Lifecycle
POST   /api/v1/agents/{id}/publish       # Publish to production
POST   /api/v1/agents/{id}/deprecate     # Mark deprecated
GET    /api/v1/agents/active             # Get active agents

# Version Management
GET    /api/v1/agents/{id}/versions      # List versions
GET    /api/v1/agents/{id}/versions/{v}  # Get version
POST   /api/v1/agents/{id}/rollback      # Rollback to version

# Testing
POST   /api/v1/agents/{id}/test          # Run test
GET    /api/v1/agents/{id}/test-sessions # List test sessions
GET    /api/v1/test-sessions/{id}        # Get test result

# Templates
POST   /api/v1/templates                 # Create template
GET    /api/v1/templates                 # List templates
POST   /api/v1/templates/{id}/render     # Render with variables

# Runtime Integration
GET    /api/v1/tools                     # Available tools
GET    /api/v1/providers                 # Supported LLM providers
GET    /api/v1/runtime/health            # Runtime health
```

### Configuration Schema

```json
{
  "id": "uuid",
  "name": "Care Gap Optimizer",
  "slug": "care-gap-optimizer",
  "persona": {
    "name": "Clinical Quality Advisor",
    "role": "Quality improvement specialist",
    "traits": ["evidence-based", "patient-focused", "actionable"]
  },
  "model": {
    "provider": "claude",
    "modelId": "claude-3-sonnet-20240229",
    "temperature": 0.3,
    "maxTokens": 4096
  },
  "systemPrompt": "You are a clinical quality advisor...",
  "tools": [
    {
      "name": "fhir_query",
      "enabled": true,
      "config": {
        "allowedResources": ["Patient", "Condition", "MeasureReport"]
      }
    },
    {
      "name": "cql_execution",
      "enabled": true,
      "config": {
        "measureLibraries": ["hedis-2024", "quality-measures"]
      }
    }
  ],
  "guardrails": {
    "phiFiltering": true,
    "clinicalDisclaimerRequired": true,
    "blockedPatterns": ["prescribe", "diagnose", "stop taking"],
    "maxOutputTokens": 2000
  }
}
```

### Multi-Tenancy

- All agents scoped to `tenant_id`
- System templates (`is_system = true`) available to all tenants
- Tenant admins can clone system templates
- Cross-tenant access strictly prohibited via repository queries

## Consequences

### Positive

- **Democratized AI** - Non-technical staff can create and modify agents
- **Rapid Iteration** - Changes can be deployed in minutes, not sprints
- **Full Audit Trail** - Every change is versioned and attributed
- **Safe Experimentation** - Sandbox testing before production deployment
- **Template Library** - Best practices shared across the platform
- **Instant Rollback** - One-click revert to any previous version

### Negative

- **Complexity** - Managing many tenant-specific agents increases operational load
- **Quality Control** - User-created agents may be suboptimal
- **Support Burden** - More agents means more potential issues
- **Testing Overhead** - Each agent needs proper testing
- **Resource Consumption** - Many agents may increase LLM costs

### Neutral

- Tenants responsible for their own agent quality
- Platform provides guardrails but cannot prevent all misuse
- Training required for tenant administrators

## Implementation Files

- `agent-builder-service/` - Backend service
  - `AgentConfigurationService.java` - Core CRUD operations
  - `AgentVersionService.java` - Version management
  - `AgentTestService.java` - Sandbox testing
  - `PromptTemplateService.java` - Template management
  - `AgentBuilderController.java` - REST endpoints

## References

- HDIM Plan: `/home/mahoosuc-solutions/.claude/plans/agile-launching-church.md` (Phase 3)
- Related ADR: ADR-0001 Multi-Provider LLM Architecture
- Related ADR: ADR-0004 Agent Versioning Strategy

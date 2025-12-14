# Agent Builder Service

A no-code platform for creating, configuring, testing, and deploying custom AI agents without programming.

## Purpose

Enables clinical informaticists and healthcare quality managers to customize AI agent behavior through a visual interface, addressing the challenge that:
- Healthcare protocols change frequently and require rapid iteration
- Non-developers need to configure AI behavior without writing code
- Agent configurations need version control and rollback capabilities

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Agent Builder Service                         │
│                         (Port 8096)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── AgentBuilderController (30+ REST endpoints)                │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── AgentConfigurationService  - CRUD, validation, publishing  │
│  ├── AgentVersionService        - Snapshots, rollback, diff     │
│  ├── AgentTestService           - Sandbox testing, metrics      │
│  └── PromptTemplateService      - Template library, rendering   │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  ├── AgentConfigurationRepository                               │
│  ├── AgentVersionRepository                                     │
│  ├── AgentTestSessionRepository                                 │
│  └── PromptTemplateRepository                                   │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  ├── AgentConfiguration   - Core agent definition               │
│  ├── AgentVersion         - Immutable configuration snapshots   │
│  ├── AgentTestSession     - Sandbox test sessions               │
│  └── PromptTemplate       - Reusable prompt library             │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ OpenFeign (HTTP)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Agent Runtime Service                          │
│                        (Port 8088)                               │
│  - LLM execution (Claude, Azure OpenAI, AWS Bedrock)            │
│  - Tool registry and execution                                   │
│  - Guardrail enforcement                                         │
│  - Memory management                                             │
└─────────────────────────────────────────────────────────────────┘
```

## Service Layer

### AgentConfigurationService
- **Purpose**: Manage agent configurations (CRUD operations)
- **Key Features**:
  - Create/update/delete agent configurations
  - Agent status lifecycle: DRAFT → TESTING → ACTIVE → DEPRECATED → ARCHIVED
  - Clone agents for rapid iteration
  - Tenant-scoped with configurable limits (max 50 agents/tenant)
  - Redis caching for active agents

### AgentVersionService
- **Purpose**: Version control for agent configurations
- **Key Features**:
  - Immutable configuration snapshots
  - Rollback to any previous version
  - Version comparison (diff between versions)
  - Change tracking with summaries and change types (MAJOR/MINOR/PATCH)

### AgentTestService
- **Purpose**: Sandbox testing before publishing
- **Key Features**:
  - Interactive test sessions with conversation history
  - Tool invocation tracking
  - Metrics collection (latency, tokens, guardrail triggers)
  - Test feedback and ratings
  - Async execution with dedicated thread pool

### PromptTemplateService
- **Purpose**: Reusable prompt building blocks
- **Key Features**:
  - Template categories: SYSTEM_PROMPT, CAPABILITIES, CONSTRAINTS, etc.
  - Variable extraction and rendering ({{variableName}} syntax)
  - Template validation and syntax checking
  - System-wide and tenant-specific templates

## Domain Entities

### AgentConfiguration
Core agent definition with:
- **Identity**: name, slug, description, persona (name, role, avatar)
- **LLM Config**: provider, model, temperature, maxTokens
- **Behavior**: systemPrompt, welcomeMessage
- **Tools**: toolConfiguration (JSON - enabled tools and settings)
- **Safety**: guardrailConfiguration (clinical safety, PHI protection)
- **Access**: allowedRoles, requiresPatientContext

### AgentVersion
Immutable snapshots with:
- **Versioning**: versionNumber (semantic), configurationSnapshot (JSON)
- **Status**: DRAFT, PUBLISHED, ROLLED_BACK, SUPERSEDED
- **Audit**: createdBy, publishedBy, rolledBackBy, changeSummary

### AgentTestSession
Test sessions with:
- **Types**: INTERACTIVE, AUTOMATED, SCENARIO
- **Data**: conversation messages, tool invocations, metrics
- **Feedback**: user ratings and comments

### PromptTemplate
Template library with:
- **Content**: template text with {{variable}} placeholders
- **Metadata**: category, description, variables with defaults
- **Scope**: isSystemTemplate flag for global vs tenant templates

## API Endpoints

### Agent Configuration
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/agent-builder/agents` | Create agent |
| PUT | `/api/v1/agent-builder/agents/{id}` | Update agent |
| GET | `/api/v1/agent-builder/agents/{id}` | Get agent |
| GET | `/api/v1/agent-builder/agents` | List agents |
| DELETE | `/api/v1/agent-builder/agents/{id}` | Archive agent |
| POST | `/api/v1/agent-builder/agents/{id}/publish` | Publish agent |
| POST | `/api/v1/agent-builder/agents/{id}/clone` | Clone agent |

### Version Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/agent-builder/agents/{id}/versions` | List versions |
| GET | `/api/v1/agent-builder/agents/{id}/versions/{ver}` | Get version |
| POST | `/api/v1/agent-builder/agents/{id}/rollback` | Rollback |
| GET | `/api/v1/agent-builder/agents/{id}/versions/compare` | Compare versions |

### Testing
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/agent-builder/agents/{id}/test/start` | Start session |
| POST | `/api/v1/agent-builder/test/sessions/{id}/message` | Send message |
| POST | `/api/v1/agent-builder/test/sessions/{id}/complete` | Complete test |
| GET | `/api/v1/agent-builder/agents/{id}/test/statistics` | Get metrics |

### Templates
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/agent-builder/templates` | Create template |
| GET | `/api/v1/agent-builder/templates` | List templates |
| POST | `/api/v1/agent-builder/templates/{id}/render` | Render template |
| POST | `/api/v1/agent-builder/templates/validate` | Validate syntax |

## Configuration

```yaml
# application.yml
hdim:
  agent-builder:
    max-agents-per-tenant: 50
    max-versions-per-agent: 100
    test-session-timeout-minutes: 30

spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation, Cache, Actuator
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis for active agent caching
- **Communication**: OpenFeign for agent-runtime-service integration
- **Resilience**: Resilience4j for circuit breakers

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:agent-builder-service:bootRun

# Or via Docker (ai profile)
docker compose --profile ai up agent-builder-service
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:agent-builder-service:test

# Integration tests
./gradlew :modules:services:agent-builder-service:integrationTest
```

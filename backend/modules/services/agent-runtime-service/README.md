# Agent Runtime Service

AI agent execution engine with multi-LLM provider support, tool orchestration, and clinical safety guardrails.

## Overview

The Agent Runtime Service provides a production-ready execution environment for AI agents in healthcare settings. It supports multiple LLM providers (Claude, Azure OpenAI, AWS Bedrock), orchestrates tool execution, manages conversation memory, and enforces clinical safety guardrails.

## Key Features

### Multi-Provider LLM Support
- **Claude (Anthropic)**: Primary provider with Claude 3.5 Sonnet
- **Azure OpenAI**: GPT-4 Turbo enterprise deployment
- **AWS Bedrock**: Claude on AWS infrastructure
- Automatic fallback chain for high availability
- Provider health monitoring and circuit breakers

### Agent Orchestration
- Synchronous and streaming execution modes
- Task lifecycle management (start, monitor, cancel)
- Maximum iteration limits to prevent runaway execution
- Concurrent task management (50 tasks default)
- Correlation IDs for distributed tracing

### Tool Integration
- FHIR resource retrieval and search
- CQL measure evaluation
- Care gap analysis
- Event publishing to Kafka
- Quality measure queries
- Approval workflow integration

### Memory Management
- Redis-based conversation memory
- Automatic summarization after 20 messages
- 15-minute TTL for conversation history
- Encrypted memory storage (HIPAA-compliant)
- Session-based context preservation

### Clinical Safety Guardrails
- PHI (Protected Health Information) detection and redaction
- Clinical safety checks (block definitive diagnoses)
- Required medical disclaimers
- Content filtering and moderation
- Rate limiting per user and tenant

### Custom Agents
- Load agent definitions from Agent Builder Service
- Dynamic agent configuration and system prompts
- Agent-specific tool permissions
- Role-based agent access control

## Technology Stack

- **Spring Boot 3.x**: Core reactive framework
- **Spring WebFlux**: Async/reactive API
- **PostgreSQL**: Agent execution history
- **Redis**: Conversation memory and caching
- **Apache Kafka**: Event streaming
- **Resilience4j**: Circuit breakers and rate limiting
- **Anthropic Claude API**: Primary LLM provider
- **Azure OpenAI**: Enterprise LLM provider
- **AWS Bedrock**: AWS-hosted LLM provider

## API Endpoints

### Agent Execution
```
POST /api/v1/agents/{agentType}/execute
     - Execute an agent task synchronously
     - Returns complete response with token usage

POST /api/v1/agents/{agentType}/stream
     - Execute agent with streaming response (SSE)
     - Returns text chunks as they're generated
```

### Task Management
```
GET    /api/v1/agents/tasks/{taskId}/status
       - Get status of running task

DELETE /api/v1/agents/tasks/{taskId}
       - Cancel a running task
```

### Tools and Providers
```
GET /api/v1/agents/tools
    - List available tools for agents

GET /api/v1/agents/providers
    - List LLM providers and health status
```

### Memory Management
```
DELETE /api/v1/agents/sessions/{sessionId}/memory
       - Clear conversation memory for session
```

### Health
```
GET /api/v1/agents/health
    - Service health with provider status
```

## Configuration

### LLM Providers
```yaml
hdim.agent.llm:
  default-provider: claude
  fallback-chain: [claude, azure-openai, bedrock]

  providers:
    claude:
      enabled: true
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-3-5-sonnet-20241022
      max-tokens: 4096
      temperature: 0.3

    azure-openai:
      enabled: true
      endpoint: ${AZURE_OPENAI_ENDPOINT}
      deployment-id: gpt-4-turbo

    bedrock:
      enabled: true
      region: us-east-1
      model-id: anthropic.claude-3-sonnet-20240229-v1:0
```

### Guardrails
```yaml
hdim.agent.guardrails:
  phi-protection:
    enabled: true
    cache-ttl-minutes: 5
    redact-patterns: [SSN, MRN patterns]

  clinical-safety:
    enabled: true
    block-definitive-diagnoses: true
    require-disclaimers: true

  rate-limiting:
    requests-per-minute-per-user: 30
    requests-per-minute-per-tenant: 500
```

### Memory
```yaml
hdim.agent.memory:
  conversation:
    max-messages: 50
    ttl-minutes: 15
    summarization-threshold: 20
  encryption:
    enabled: true
```

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+
- Anthropic API key (or Azure OpenAI credentials)

### Environment Variables
```bash
export ANTHROPIC_API_KEY=sk-ant-...
export AZURE_OPENAI_API_KEY=...  # Optional
export AZURE_OPENAI_ENDPOINT=...  # Optional
```

### Build
```bash
./gradlew :modules:services:agent-runtime-service:build
```

### Run
```bash
./gradlew :modules:services:agent-runtime-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:agent-runtime-service:test
```

## Integration

This service integrates with:
- **FHIR Service**: Clinical data retrieval
- **CQL Engine**: Measure evaluation
- **Care Gap Service**: Gap analysis
- **Quality Measure Service**: Quality metrics
- **Agent Builder Service**: Custom agent definitions
- **Approval Service**: Human-in-the-loop workflows

## Security

- JWT-based authentication
- Role-based tool access control
- Tenant isolation for all operations
- PHI detection and redaction
- Clinical safety guardrails
- Rate limiting and circuit breakers

## Resilience

### Circuit Breakers
- Per-provider circuit breakers
- Automatic failover to backup providers
- Health indicator registration
- Half-open state testing

### Rate Limiting
- 60 requests/minute per LLM provider
- User-level and tenant-level limits
- Token-based rate limiting (100k tokens/minute)

## API Documentation

Swagger UI available at:
```
http://localhost:8088/swagger-ui.html
```

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`
- Circuit breakers: `/actuator/circuitbreakers`

## License

Copyright (c) 2024 Mahoosuc Solutions

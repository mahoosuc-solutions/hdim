# AI Assistant Service

AI-powered clinical assistant service using Claude API for natural language clinical queries, patient summaries, and care gap analysis.

## Overview

The AI Assistant Service provides conversational AI capabilities for clinical workflows. It uses Anthropic's Claude API to answer clinical questions, generate patient summaries, analyze care gaps, and provide decision support. All interactions are HIPAA-compliant with proper safeguards and audit logging.

## Key Features

### Natural Language Queries
- Answer clinical questions in natural language
- Context-aware responses with clinical data
- Query type validation and routing
- Conversation context preservation

### Patient Summary Generation
- AI-generated patient summaries from FHIR data
- Key clinical findings extraction
- Active problems and medication lists
- Recent encounters and results

### Care Gap Analysis
- Intelligent care gap prioritization
- Actionable recommendations for gap closure
- Risk-based gap ranking
- Intervention suggestions

### Clinical Decision Support
- Quality measure interpretation
- Treatment guideline recommendations
- Medication interaction awareness
- Evidence-based practice suggestions

### Safety and Compliance
- Query type allowlisting
- Rate limiting per user/tenant
- Response validation and filtering
- Comprehensive audit trail
- No storage of patient data in AI logs

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **Claude API (Anthropic)**: AI language model
- **PostgreSQL**: Audit and session storage
- **Redis**: Response caching and rate limiting
- **Apache Kafka**: Event streaming for audit
- **Resilience4j**: Circuit breaker and rate limiting

## API Endpoints

### Chat Interface
```
POST /api/v1/ai/chat
     - Send natural language query to AI assistant
     - Request body: ChatRequest (query, queryType, context)
     - Returns: ChatResponse with AI-generated answer
```

### Patient Summaries
```
POST /api/v1/ai/patient-summary/{patientId}
     - Generate AI summary for a patient
     - Request body: Patient FHIR data (JSON)
     - Returns: ChatResponse with summary
```

### Care Gap Analysis
```
POST /api/v1/ai/care-gaps/analyze
     - Analyze care gaps with AI recommendations
     - Request body: Care gap data (JSON)
     - Returns: ChatResponse with analysis
```

### Clinical Queries
```
GET /api/v1/ai/query?query={query}&context={context}
    - Answer a clinical question
    - Query params: query, context (optional)
    - Returns: ChatResponse with answer
```

### Status and Health
```
GET /api/v1/ai/status
    - Get AI assistant capabilities and configuration

GET /api/v1/ai/health
    - Health check with AI availability status
```

## Configuration

### Claude API
```yaml
claude:
  enabled: true
  api-key: ${ANTHROPIC_API_KEY}
  api-url: https://api.anthropic.com/v1
  model: claude-3-5-sonnet-20241022
  max-tokens: 4096
  temperature: 0.3
  timeout-seconds: 60
  max-retries: 3
```

### Rate Limiting
```yaml
claude:
  rate-limit-per-minute: 60
  caching-enabled: true
  cache-ttl-seconds: 300
```

### Query Types
Configurable allowed query types:
- CLINICAL_QUESTION
- PATIENT_SUMMARY
- CARE_GAP_ANALYSIS
- QUALITY_MEASURE
- MEDICATION_REVIEW
- DIAGNOSIS_SUPPORT

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+
- Anthropic API key

### Environment Variables
```bash
export ANTHROPIC_API_KEY=sk-ant-...
export CLAUDE_ENABLED=true
export CLAUDE_MODEL=claude-3-5-sonnet-20241022
```

### Build
```bash
./gradlew :modules:services:ai-assistant-service:build
```

### Run
```bash
./gradlew :modules:services:ai-assistant-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:ai-assistant-service:test
```

## Security and Compliance

### HIPAA Compliance
- No PHI sent to AI logs or monitoring
- Request/response audit logging to Kafka
- Encrypted data in transit (TLS)
- No patient data persistence in service
- Response caching with TTL controls

### Safety Guardrails
- Query type validation and allowlisting
- Rate limiting per user (prevents abuse)
- Response content filtering
- Clinical disclaimer requirements
- Human-in-the-loop for critical decisions

### Authentication
- JWT-based authentication required
- Role-based access control
- Tenant isolation enforced
- User attribution on all queries

## Response Caching

### Cache Strategy
- 5-minute TTL for identical queries
- Cache key: query + context hash
- Redis-based distributed cache
- Automatic cache invalidation
- Reduced API costs and latency

## Token Usage Tracking

All responses include token usage metrics:
- Input tokens (prompt + context)
- Output tokens (response)
- Total tokens
- Processing time in milliseconds

## Error Handling

### Service Disabled
When `claude.enabled=false`, returns:
```json
{
  "response": "AI assistant is not enabled",
  "error": true,
  "errorMessage": "AI service disabled"
}
```

### API Errors
- Automatic retry with exponential backoff
- Circuit breaker protection
- Graceful degradation
- Detailed error logging

## API Documentation

Swagger UI available at:
```
http://localhost:8090/swagger-ui.html
```

## Monitoring

### Actuator Endpoints
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

### Custom Metrics
- Total queries processed
- Average response time
- Token usage statistics
- Error rate by type
- Cache hit rate

## Integration

This service is typically called by:
- **Frontend UI**: Chatbot interface
- **Agent Runtime Service**: AI agent tool
- **Quality Measure Service**: Measure interpretation
- **Care Gap Service**: Gap analysis

## Costs and Usage

### API Costs (Approximate)
- Claude 3.5 Sonnet: $3 per million input tokens, $15 per million output tokens
- Typical query: 500-2000 tokens total
- Caching reduces costs by 60-80%

### Best Practices
- Enable caching for repeated queries
- Use appropriate context size (don't over-send data)
- Leverage rate limiting to control costs
- Monitor token usage metrics

## License

Copyright (c) 2024 Mahoosuc Solutions

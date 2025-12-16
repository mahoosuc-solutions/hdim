# Gateway Service

API Gateway with intelligent routing, rate limiting, circuit breaking, and JWT authentication for the HDIM platform.

## Purpose

Provides a unified entry point for all microservices, addressing the challenge that:
- Multiple backend services need centralized authentication and authorization
- API consumers need a single endpoint rather than managing multiple service URLs
- Services require protection from cascading failures via circuit breakers
- Rate limiting and security policies must be enforced consistently

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Gateway Service                             │
│                         (Port 8080)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── ApiGatewayController (routing to 13 downstream services)   │
├─────────────────────────────────────────────────────────────────┤
│  Security & Middleware                                           │
│  ├── JWT Authentication     - Token validation                  │
│  ├── Rate Limiting          - 100 req/sec, burst: 150           │
│  └── Header Forwarding      - X-Tenant-ID, X-User-ID, Auth      │
├─────────────────────────────────────────────────────────────────┤
│  Resilience Layer (Resilience4j)                                │
│  ├── Circuit Breakers       - Per-service failure detection     │
│  ├── Retry Logic            - Exponential backoff (3 attempts)  │
│  └── Timeouts               - 15s-120s depending on service     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP (RestTemplate)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│  Backend Services (CQL Engine, FHIR, Patient, Care Gap, etc.)   │
│  - 13 microservices routed by path prefix                       │
│  - Auto-discovery via configured service URLs                   │
└─────────────────────────────────────────────────────────────────┘
```

## API Routes

| Path Prefix | Target Service | Port | Purpose |
|-------------|---------------|------|---------|
| `/api/cql/**` | cql-engine-service | 8081 | CQL evaluation |
| `/api/fhir/**` | fhir-service | 8085 | FHIR resources |
| `/api/patients/**` | patient-service | 8084 | Patient aggregation |
| `/api/care-gaps/**` | care-gap-service | 8086 | Care gap identification |
| `/api/consent/**` | consent-service | 8082 | Consent management |
| `/api/events/**` | event-processing-service | 8083 | Event processing |
| `/api/v1/agent-builder/**` | agent-builder-service | 8096 | AI agent configuration |
| `/api/v1/qrda/**` | qrda-export-service | 8104 | QRDA exports |
| `/api/v1/hcc/**` | hcc-service | 8105 | Risk adjustment |
| `/api/ecr/**` | ecr-service | 8101 | Electronic case reporting |
| `/api/v1/prior-auth/**` | prior-auth-service | 8102 | Prior authorization |

## Configuration

```yaml
# Gateway settings
gateway:
  auth:
    enabled: true
    enforced: false  # Set to true for production
  rate-limit:
    requests-per-second: 100
    burst-capacity: 150

# JWT configuration
jwt:
  secret: ${JWT_SECRET}  # Required via environment variable
  accessTokenExpirationMs: 900000  # 15 minutes
  issuer: healthdata-gateway

# Circuit breakers (per service)
resilience4j:
  circuitbreaker:
    instances:
      fhirService:
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

## Dependencies

- **Spring Boot**: Web, Security, Actuator
- **Database**: PostgreSQL (for auth state)
- **Resilience**: Resilience4j for circuit breakers, retries, rate limiting
- **HTTP Client**: RestTemplate for service communication

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:gateway-service:bootRun

# Or via Docker (gateway profile)
docker compose --profile gateway up gateway-service
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:gateway-service:test

# Test authentication
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

# Test routing (requires JWT token)
curl http://localhost:8080/api/fhir/Patient \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-ID: tenant-1"
```

## Security Notes

- JWT secrets MUST be provided via `JWT_SECRET` environment variable in production
- Rate limiting prevents abuse (100 req/sec default)
- Circuit breakers protect against cascading failures
- All authentication tokens forwarded to downstream services via headers

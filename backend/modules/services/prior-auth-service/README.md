# Prior Authorization Service

Prior authorization workflow management service implementing CMS Interoperability and Prior Authorization Rule (CMS-0057-F).

## Purpose

The Prior Authorization Service automates and streamlines the prior authorization (PA) process for medical procedures, medications, and services. It provides complete workflow management from PA request creation through payer submission, status tracking, and decision management with SLA monitoring.

## Key Features

- **PA Request Management**: Create, submit, and track prior authorization requests
- **Multi-payer Integration**: Support for multiple payer APIs and protocols
- **SLA Monitoring**: Automatic tracking of stat (72-hour) and routine (7-day) deadlines
- **Status Tracking**: Real-time status updates from payers (PENDING, SUBMITTED, APPROVED, DENIED, etc.)
- **Provider Access**: Separate provider access control for viewing PA status
- **Alert System**: SLA deadline alerts and notifications
- **Statistics Dashboard**: Approval rates, processing times, and payer performance metrics
- **Retry Logic**: Automatic retry with exponential backoff for transient failures
- **Multi-tenant Support**: Complete tenant isolation and security
- **Circuit Breaker**: Resilience4j circuit breaker for payer API reliability

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/prior-auth` | Create new prior authorization request |
| POST | `/api/v1/prior-auth/{requestId}/submit` | Submit PA request to payer |
| GET | `/api/v1/prior-auth/{requestId}` | Get PA request details |
| POST | `/api/v1/prior-auth/{requestId}/check-status` | Check current status with payer |
| POST | `/api/v1/prior-auth/{requestId}/cancel` | Cancel a PA request |
| GET | `/api/v1/prior-auth/patient/{patientId}` | Get all PA requests for patient |
| GET | `/api/v1/prior-auth/status/{status}` | Get PA requests by status |
| GET | `/api/v1/prior-auth/sla-alerts` | Get PA requests approaching SLA deadline |
| GET | `/api/v1/prior-auth/statistics` | Get PA statistics and metrics |

## Configuration

### Application Properties

```yaml
server:
  port: 8102

prior-auth:
  sla:
    stat-hours: 72
    routine-days: 7
    alert-threshold-hours: 24
  retry:
    max-attempts: 3
    initial-delay-ms: 1000
    max-delay-ms: 10000
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/healthdata
    username: healthdata
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: prior_auth
```

### Circuit Breaker

```yaml
resilience4j:
  circuitbreaker:
    instances:
      payerApi:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

## PA Request Status Flow

1. **DRAFT**: Request created but not yet submitted
2. **PENDING**: Submitted and awaiting payer processing
3. **SUBMITTED**: Confirmed receipt by payer
4. **UNDER_REVIEW**: Payer actively reviewing request
5. **APPROVED**: Prior authorization approved
6. **DENIED**: Prior authorization denied
7. **CANCELLED**: Request cancelled before decision
8. **EXPIRED**: Approved PA expired (typically 60-90 days)

## SLA Deadlines

### Stat Requests
- Deadline: 72 hours from submission
- Use cases: Urgent procedures, emergency situations
- Alert threshold: 24 hours before deadline

### Routine Requests
- Deadline: 7 days from submission
- Use cases: Standard procedures, non-urgent care
- Alert threshold: 24 hours before deadline

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:prior-auth-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:prior-auth-service:test
```

### Building

```bash
./gradlew :modules:services:prior-auth-service:build
```

## Example Usage

### Create PA Request

```bash
curl -X POST http://localhost:8102/api/v1/prior-auth \
  -H "X-Tenant-Id: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-123",
    "serviceType": "PROCEDURE",
    "procedureCode": "27447",
    "diagnosisCodes": ["M17.11"],
    "urgency": "STAT",
    "payerId": "payer-001",
    "providerNpi": "1234567890"
  }'
```

### Check SLA Alerts

```bash
curl http://localhost:8102/api/v1/prior-auth/sla-alerts?hoursUntilDeadline=24 \
  -H "X-Tenant-Id: tenant-001"
```

### Get Statistics

```bash
curl http://localhost:8102/api/v1/prior-auth/statistics \
  -H "X-Tenant-Id: tenant-001"
```

## Payer Integration

The service supports integration with:
- **Direct Payer APIs**: REST/SOAP APIs from major payers
- **X12 278**: Electronic prior authorization transactions
- **FHIR Prior Auth**: HL7 FHIR-based prior authorization
- **Portal Scraping**: Automated portal integration (when API unavailable)

## Statistics Provided

- Total PA requests
- Approval rate (%)
- Average processing time
- Requests by status
- Requests by payer
- SLA compliance rate
- Denied request reasons

## CMS Compliance

Implements CMS-0057-F requirements:
- Electronic PA submission
- Real-time status tracking
- Decision timeframes (72 hours stat, 7 days routine)
- Decision documentation
- Appeal process support

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions

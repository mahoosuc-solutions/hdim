# Consent Service

HIPAA-compliant patient consent management for data sharing, research, and treatment authorization.

## Purpose

Manages patient consent records and validates data access permissions, addressing the challenge that:
- HIPAA requires granular consent tracking for PHI data sharing
- Healthcare organizations must validate authorization before data access
- Consent can expire, be revoked, or have limited scope (purpose, category, data class)
- All consent changes require audit trails (7-year retention)

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Consent Service                              │
│                         (Port 8082)                              │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer                                                │
│  └── ConsentController (25+ REST endpoints)                     │
├─────────────────────────────────────────────────────────────────┤
│  Service Layer                                                   │
│  ├── ConsentService                                             │
│  │   ├── CRUD operations          - Create, update, delete      │
│  │   ├── Lifecycle management     - Revoke, expire checking     │
│  │   ├── Authorization validation - Scope, category, data class │
│  │   └── Query optimization       - Active, expired, expiring   │
│  └── AuditService (HIPAA logging)                               │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                                │
│  └── ConsentRepository (JPA + custom queries)                   │
├─────────────────────────────────────────────────────────────────┤
│  Domain Entities                                                 │
│  └── ConsentEntity                                              │
│      ├── Scope           - treatment, research, operations       │
│      ├── Category        - clinical-notes, lab-results, etc.    │
│      ├── Data Class      - demographics, medications, etc.      │
│      ├── Status          - ACTIVE, REVOKED, EXPIRED             │
│      └── Audit Fields    - createdBy, revokedBy, timestamps     │
└─────────────────────────────────────────────────────────────────┘
```

## API Endpoints

### Consent Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/consents` | Create consent |
| PUT | `/api/consents/{id}` | Update consent |
| DELETE | `/api/consents/{id}` | Delete consent |
| POST | `/api/consents/{id}/revoke` | Revoke consent |
| GET | `/api/consents/{id}` | Get consent by ID |

### Patient Queries
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/consents/patient/{patientId}` | All consents for patient |
| GET | `/api/consents/patient/{patientId}/active` | Active consents |
| GET | `/api/consents/patient/{patientId}/revoked` | Revoked consents |
| GET | `/api/consents/patient/{patientId}/expired` | Expired consents |
| GET | `/api/consents/patient/{patientId}/expiring-soon` | Expiring in N days |

### Consent Validation
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/consents/patient/{patientId}/check/scope/{scope}` | Check scope authorization |
| GET | `/api/consents/patient/{patientId}/check/category/{category}` | Check category authorization |
| GET | `/api/consents/patient/{patientId}/check/data-class/{dataClass}` | Check data class authorization |
| POST | `/api/consents/validate-access` | Validate data access request |

### Filtering
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/consents/patient/{patientId}/active/scope/{scope}` | Active by scope |
| GET | `/api/consents/patient/{patientId}/active/category/{category}` | Active by category |

## Configuration

```yaml
server:
  port: 8082
  servlet:
    context-path: /consent

# HIPAA audit retention
audit:
  enabled: true
  retention-days: 2555  # 7 years for HIPAA compliance

# Database
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_consent
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

## Consent Entity Structure

```json
{
  "id": "uuid",
  "tenantId": "tenant-1",
  "patientId": "uuid",
  "scope": "treatment",  // treatment, research, payment, operations
  "category": "clinical-notes",  // clinical-notes, lab-results, imaging
  "dataClass": "medications",  // demographics, conditions, medications
  "authorizedPartyId": "provider-123",
  "status": "ACTIVE",  // ACTIVE, REVOKED, EXPIRED
  "startDate": "2024-01-01",
  "expirationDate": "2025-01-01",
  "revocationDate": null,
  "revocationReason": null,
  "createdBy": "user-123",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

## Dependencies

- **Spring Boot**: Web, JPA, Validation
- **Database**: PostgreSQL with Liquibase migrations
- **Audit**: HIPAA-compliant audit service (7-year retention)
- **Resilience**: Resilience4j for circuit breakers

## Running Locally

```bash
# From backend directory
./gradlew :modules:services:consent-service:bootRun

# Or via Docker
docker compose --profile consent up consent-service
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:consent-service:test

# Create consent
curl -X POST http://localhost:8082/consent/api/consents \
  -H "X-Tenant-ID: tenant-1" \
  -H "X-User-ID: user-123" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-uuid",
    "scope": "treatment",
    "category": "clinical-notes",
    "startDate": "2024-01-01",
    "expirationDate": "2025-01-01"
  }'

# Validate access
curl -X POST http://localhost:8082/consent/api/consents/validate-access \
  -H "X-Tenant-ID: tenant-1" \
  -d '{
    "patientId": "patient-uuid",
    "scope": "treatment",
    "category": "clinical-notes"
  }'
```

## HIPAA Compliance

- 7-year audit retention for all consent changes
- Revocation reasons tracked for legal compliance
- Expiration checking prevents access to expired consents
- Granular authorization: scope + category + data class
- PHI not logged in audit trails (`include-phi: false`)

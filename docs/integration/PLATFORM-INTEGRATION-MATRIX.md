# HDIM Multi-Platform Integration Showcase

**Audience**: Technical Architects, Integration Engineers, RFP Evaluators
**Purpose**: Demonstrate HDIM's ability to integrate with any FHIR R4-compliant platform
**Last Updated**: January 2026

---

## Executive Summary

HDIM integrates with **any healthcare platform that supports FHIR R4**, including:
- **Major EHR vendors**: Epic, Cerner, athenahealth, Meditech, Allscripts, NextGen
- **Health Information Exchanges**: CommonWell, Carequality, state HIEs
- **Government systems**: CMS BCDA (Medicare/Medicaid claims data)
- **Generic FHIR servers**: HAPI FHIR, IBM FHIR, Microsoft Azure FHIR

**Key Differentiator**: Unlike competitors that optimize for a single EHR ecosystem, HDIM's FHIR-native architecture works equally well across all platforms—enabling unified quality measurement for multi-EHR organizations.

---

## Table of Contents

1. [Platform Integration Matrix](#platform-integration-matrix)
2. [Detailed Integration Guides](#detailed-integration-guides)
   - [Epic](#epic-integration)
   - [Cerner (Oracle Health)](#cerner-oracle-health-integration)
   - [athenahealth](#athenahealth-integration)
   - [Meditech](#meditech-integration)
   - [Allscripts](#allscripts-integration)
   - [Generic FHIR Servers](#generic-fhir-server-integration)
   - [CommonWell Health Alliance](#commonwell-health-alliance-integration)
   - [Carequality](#carequality-integration)
   - [CMS BCDA](#cms-bcda-integration)
3. [Multi-EHR Integration Scenarios](#multi-ehr-integration-scenarios)
4. [Authentication Patterns](#authentication-patterns)
5. [Error Handling & Troubleshooting](#error-handling--troubleshooting)
6. [Performance Optimization](#performance-optimization)
7. [Security Best Practices](#security-best-practices)
8. [Integration Checklist](#integration-checklist)

---

## Platform Integration Matrix

### Quick Reference Comparison

| Platform | FHIR Version | Auth Method | Integration Time | Complexity | HDIM Status | Typical Customer |
|----------|--------------|-------------|------------------|------------|-------------|------------------|
| **Epic** | R4 Full | OAuth2 RS384 JWT | 6-8 weeks | High | Production | Large health systems |
| **Cerner** | R4 Full | OAuth2 Client Creds | 4-6 weeks | Medium | Production | Regional hospitals |
| **athenahealth** | R4 Full | OAuth2 Auth Code | 3-5 weeks | Medium | Production | Medical groups |
| **Meditech** | R4 Partial | SMART on FHIR | 4-6 weeks | Medium | Production | Community hospitals |
| **Allscripts** | R4 Partial | OAuth2/Basic | 4-6 weeks | Medium | Production | Ambulatory practices |
| **NextGen** | R4 Partial | OAuth2 | 4-6 weeks | Medium | Production | Specialty practices |
| **Generic FHIR** | R4 Native | Flexible | 1-3 weeks | Low | Production | Various |
| **CommonWell** | R4 + XCPD | Certificate | 3-4 weeks | Medium | Production | Multi-state HIE |
| **Carequality** | R4 + IHE | Mutual TLS | 3-4 weeks | Medium | Production | National HIE |
| **CMS BCDA** | R4 Bulk | OAuth2 Backend | 2-3 weeks | Low | Production | Medicare Advantage |

### Capability Matrix

| Capability | Epic | Cerner | athena | Meditech | Allscripts | Generic | HIE | BCDA |
|------------|------|--------|--------|----------|------------|---------|-----|------|
| **Patient Search** | Full | Full | Full | Full | Full | Full | Full | Bulk |
| **Observation Query** | Full | Full | Full | Partial | Full | Full | Partial | Bulk |
| **Condition Query** | Full | Full | Full | Full | Full | Full | Full | Bulk |
| **Medication Query** | Full | Full | Full | Full | Partial | Full | Partial | Bulk |
| **Encounter Query** | Full | Full | Full | Full | Full | Full | Partial | Bulk |
| **Procedure Query** | Full | Full | Partial | Partial | Partial | Full | Partial | Bulk |
| **Immunization Query** | Full | Full | Full | Partial | Partial | Full | Partial | Bulk |
| **Bulk Export ($export)** | Yes | Yes | Limited | No | No | Varies | No | Yes |
| **Subscription** | Yes | Yes | Limited | No | No | Varies | No | No |
| **CDS Hooks** | Yes | Yes | No | No | No | Varies | No | No |
| **SMART on FHIR** | Full | Full | Partial | Partial | Partial | Varies | No | No |

---

## Detailed Integration Guides

### Epic Integration

**Overview**: Epic is the most widely deployed EHR in US health systems. HDIM integrates via Epic's FHIR R4 API using OAuth2 with RS384 JWT signing.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              EPIC INTEGRATION ARCHITECTURE                               │
│                                                                                          │
│   ┌─────────────────┐                    ┌─────────────────┐                            │
│   │   Epic MyChart  │                    │  Epic Caboodle  │                            │
│   │   (Patient App) │                    │  (Data Warehouse)│                           │
│   └────────┬────────┘                    └────────┬────────┘                            │
│            │                                      │                                      │
│            │              ┌────────────────────────┴────────────────────────┐           │
│            └──────────────▶        Epic FHIR Server (R4)                    │           │
│                           │        https://fhir.epic.com/interconnect-fhir-oauth │      │
│                           │                                                  │           │
│                           │   Endpoints:                                     │           │
│                           │   ├─ /api/FHIR/R4/Patient                       │           │
│                           │   ├─ /api/FHIR/R4/Observation                   │           │
│                           │   ├─ /api/FHIR/R4/Condition                     │           │
│                           │   ├─ /api/FHIR/R4/MedicationRequest             │           │
│                           │   └─ /api/FHIR/R4/$export (Bulk Data)           │           │
│                           └────────────────────────┬────────────────────────┘           │
│                                                    │                                     │
│                                            OAuth2 + JWT                                  │
│                                                    │                                     │
│   ┌────────────────────────────────────────────────▼────────────────────────────────┐   │
│   │                              HDIM Platform                                       │   │
│   │                                                                                  │   │
│   │   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐              │   │
│   │   │ EHR Connector   │──▶│   FHIR Service  │──▶│ Quality Measure │              │   │
│   │   │ (Epic Adapter)  │   │                 │   │    Service      │              │   │
│   │   │                 │   │ - FHIR R4 parse │   │ - CQL eval      │              │   │
│   │   │ - OAuth2 tokens │   │ - Resource map  │   │ - Care gaps     │              │   │
│   │   │ - Rate limiting │   │ - Cache (Redis) │   │ - Analytics     │              │   │
│   │   └─────────────────┘   └─────────────────┘   └─────────────────┘              │   │
│   │                                                                                  │   │
│   └──────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Authentication Flow

Epic uses OAuth2 with RS384 JWT signing (asymmetric keys). This is Epic's preferred authentication method for backend applications.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           EPIC OAUTH2 AUTHENTICATION FLOW                                │
│                                                                                          │
│   1. HDIM generates client assertion (JWT signed with RS384)                            │
│      ┌──────────────────────────────────────────────────────────────────────────────┐   │
│      │  JWT Header: {"alg": "RS384", "typ": "JWT"}                                  │   │
│      │  JWT Payload: {                                                               │   │
│      │    "iss": "HDIM_CLIENT_ID",                                                  │   │
│      │    "sub": "HDIM_CLIENT_ID",                                                  │   │
│      │    "aud": "https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token",     │   │
│      │    "jti": "unique-request-id",                                               │   │
│      │    "exp": 1700650000,                                                        │   │
│      │    "iat": 1700649700                                                         │   │
│      │  }                                                                            │   │
│      │  JWT Signature: RSA-SHA384(header.payload, PRIVATE_KEY)                      │   │
│      └──────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│   2. HDIM requests access token                                                         │
│      POST https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token                   │
│      Content-Type: application/x-www-form-urlencoded                                    │
│                                                                                          │
│      grant_type=client_credentials                                                      │
│      &client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer     │
│      &client_assertion=<JWT_TOKEN>                                                      │
│      &scope=system/*.read                                                               │
│                                                                                          │
│   3. Epic returns access token (valid 5 minutes)                                        │
│      {                                                                                   │
│        "access_token": "eyJ0eXAiOiJKV1...",                                             │
│        "token_type": "Bearer",                                                          │
│        "expires_in": 300,                                                               │
│        "scope": "system/*.read"                                                         │
│      }                                                                                   │
│                                                                                          │
│   4. HDIM queries FHIR API with access token                                            │
│      GET https://fhir.epic.com/api/FHIR/R4/Patient/abc123                              │
│      Authorization: Bearer eyJ0eXAiOiJKV1...                                            │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Configuration

**application.yml (Epic configuration):**
```yaml
hdim:
  ehr:
    epic:
      enabled: true
      fhir-base-url: https://fhir.your-hospital.org/api/FHIR/R4

      # OAuth2 Configuration
      auth:
        type: oauth2-jwt
        client-id: ${EPIC_CLIENT_ID}
        private-key-path: /secrets/epic-private-key.pem
        private-key-id: ${EPIC_KEY_ID}
        token-url: https://fhir.your-hospital.org/oauth2/token
        scopes:
          - system/Patient.read
          - system/Observation.read
          - system/Condition.read
          - system/MedicationRequest.read
          - system/Encounter.read
          - system/Procedure.read
          - system/Immunization.read
        jwt-algorithm: RS384
        token-cache-ttl: 4m  # Token valid for 5min, refresh at 4min

      # Rate Limiting
      rate-limit:
        requests-per-second: 10
        burst-size: 50
        retry-after-429: true

      # Connection Settings
      connection:
        connect-timeout: 10s
        read-timeout: 30s
        max-connections: 50

      # Retry Configuration
      retry:
        max-attempts: 3
        backoff-multiplier: 2.0
        initial-interval: 100ms
        max-interval: 5s
```

#### Sample FHIR Queries

**Patient Search:**
```bash
# Search patient by MRN
curl -X GET "https://fhir.epic.com/api/FHIR/R4/Patient?identifier=MRN|12345678" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/fhir+json"

# Response
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 1,
  "entry": [{
    "resource": {
      "resourceType": "Patient",
      "id": "eAB3mDIBBcyUKviyzrxsnAw3",
      "identifier": [
        {"system": "urn:oid:1.2.3.4.5.6.7", "value": "12345678"}
      ],
      "name": [{"family": "Smith", "given": ["John"]}],
      "birthDate": "1965-03-15",
      "gender": "male"
    }
  }]
}
```

**Observation Query (HbA1c):**
```bash
# Get HbA1c observations for patient in last 12 months
curl -X GET "https://fhir.epic.com/api/FHIR/R4/Observation?\
patient=eAB3mDIBBcyUKviyzrxsnAw3\
&code=http://loinc.org|4548-4\
&date=ge2025-01-01" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/fhir+json"

# Response
{
  "resourceType": "Bundle",
  "type": "searchset",
  "entry": [{
    "resource": {
      "resourceType": "Observation",
      "id": "obs-12345",
      "status": "final",
      "code": {
        "coding": [{
          "system": "http://loinc.org",
          "code": "4548-4",
          "display": "Hemoglobin A1c/Hemoglobin.total in Blood"
        }]
      },
      "valueQuantity": {
        "value": 7.2,
        "unit": "%",
        "system": "http://unitsofmeasure.org",
        "code": "%"
      },
      "effectiveDateTime": "2025-10-15T09:30:00Z"
    }
  }]
}
```

**Bulk Export (Population Data):**
```bash
# Initiate bulk export
curl -X GET "https://fhir.epic.com/api/FHIR/R4/$export?\
_type=Patient,Observation,Condition,MedicationRequest\
&_since=2025-01-01T00:00:00Z" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/fhir+json" \
  -H "Prefer: respond-async"

# Response (202 Accepted)
# Content-Location: https://fhir.epic.com/api/FHIR/R4/$export-status/job-12345

# Poll for completion
curl -X GET "https://fhir.epic.com/api/FHIR/R4/$export-status/job-12345" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"

# When complete, download NDJSON files
# Each file contains one resource per line
```

#### Epic-Specific Considerations

| Consideration | Details |
|---------------|---------|
| **Rate Limiting** | 100 requests/minute per application. HDIM implements exponential backoff on 429 responses. |
| **Token Lifetime** | Access tokens expire in 5 minutes. HDIM refreshes at 4 minutes to avoid interruptions. |
| **Bulk Export** | Available but may require Epic Interconnect licensing. Contact Epic sales for Bulk Data API access. |
| **Epic Extensions** | Epic includes custom extensions (e.g., `http://open.epic.com/FHIR/StructureDefinition/...`). HDIM parses these for enhanced data. |
| **Test Environment** | Epic provides sandbox at `https://fhir.epic.com/interconnect-fhir-oauth/api/FHIR/R4/` with synthetic patients. |

#### Integration Timeline

| Week | Tasks |
|------|-------|
| **Week 1** | Epic application registration, private key generation, sandbox access |
| **Week 2** | OAuth2 authentication testing, scope configuration |
| **Week 3-4** | FHIR query development, data mapping validation |
| **Week 5** | Bulk export testing (if applicable), rate limit optimization |
| **Week 6** | UAT with clinical staff, measure validation |
| **Week 7** | Production credential provisioning |
| **Week 8** | Go-live, monitoring setup |

---

### Cerner (Oracle Health) Integration

**Overview**: Cerner (now Oracle Health) provides a robust FHIR R4 API through the Cerner Open Platform. Authentication uses OAuth2 with client credentials.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           CERNER INTEGRATION ARCHITECTURE                                │
│                                                                                          │
│   ┌─────────────────┐                                                                   │
│   │  Cerner EHR     │                                                                   │
│   │  (Millennium)   │                                                                   │
│   └────────┬────────┘                                                                   │
│            │                                                                             │
│            ▼                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │                     Cerner FHIR Server (Open Platform)                           │   │
│   │                     https://fhir-open.cerner.com/r4/                             │   │
│   │                                                                                  │   │
│   │   Auth: OAuth2 Client Credentials                                               │   │
│   │   Rate Limit: 100 req/min per app                                               │   │
│   │                                                                                  │   │
│   │   Supported Resources:                                                          │   │
│   │   ├─ Patient, Observation, Condition, MedicationRequest                        │   │
│   │   ├─ Encounter, Procedure, Immunization, DiagnosticReport                      │   │
│   │   ├─ AllergyIntolerance, CarePlan, Goal                                        │   │
│   │   └─ DocumentReference, Binary (for attachments)                               │   │
│   └────────────────────────────────────────────┬────────────────────────────────────┘   │
│                                                │                                         │
│                                        OAuth2 Token                                      │
│                                                │                                         │
│   ┌────────────────────────────────────────────▼────────────────────────────────────┐   │
│   │                              HDIM Platform                                       │   │
│   │   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐              │   │
│   │   │ EHR Connector   │──▶│   FHIR Service  │──▶│ Quality Measure │              │   │
│   │   │ (Cerner Adapter)│   │                 │   │    Service      │              │   │
│   │   └─────────────────┘   └─────────────────┘   └─────────────────┘              │   │
│   └──────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Authentication Flow

```bash
# 1. Request access token using client credentials
curl -X POST "https://authorization.cerner.com/tenants/{tenant}/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=${CERNER_CLIENT_ID}" \
  -d "client_secret=${CERNER_CLIENT_SECRET}" \
  -d "scope=system/Patient.read system/Observation.read system/Condition.read"

# Response
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 570,
  "scope": "system/Patient.read system/Observation.read system/Condition.read"
}

# 2. Use token to query FHIR API
curl -X GET "https://fhir-open.cerner.com/r4/{tenant}/Patient/12345" \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIs..." \
  -H "Accept: application/fhir+json"
```

#### Configuration

**application.yml (Cerner configuration):**
```yaml
hdim:
  ehr:
    cerner:
      enabled: true
      fhir-base-url: https://fhir-open.cerner.com/r4/${CERNER_TENANT_ID}

      # OAuth2 Configuration
      auth:
        type: oauth2-client-credentials
        client-id: ${CERNER_CLIENT_ID}
        client-secret: ${CERNER_CLIENT_SECRET}
        token-url: https://authorization.cerner.com/tenants/${CERNER_TENANT_ID}/oauth2/token
        scopes:
          - system/Patient.read
          - system/Observation.read
          - system/Condition.read
          - system/MedicationRequest.read
          - system/Encounter.read
        token-cache-ttl: 8m  # Token valid for 10min, refresh at 8min

      # Rate Limiting
      rate-limit:
        requests-per-second: 1.5  # ~100/min
        burst-size: 20

      # Connection Settings
      connection:
        connect-timeout: 10s
        read-timeout: 30s
        max-connections: 30
```

#### Sample Queries

**Patient with Conditions:**
```bash
# Get patient with all conditions
curl -X GET "https://fhir-open.cerner.com/r4/{tenant}/Patient/12345/\
$everything?_type=Condition" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/fhir+json"
```

#### Cerner-Specific Considerations

| Consideration | Details |
|---------------|---------|
| **Tenant ID** | Each Cerner customer has a unique tenant ID. Obtain from Cerner Open Platform team. |
| **Rate Limiting** | 100 requests/minute per application. Stricter than Epic. |
| **Bulk Export** | Available through Cerner Data Access Platform (CDAP). Separate licensing. |
| **Sandbox** | Public sandbox at `https://fhir-open.cerner.com/r4/ec2458f2-1e24-41c8-b71b-0e701af7583d/` |

#### Integration Timeline

| Week | Tasks |
|------|-------|
| **Week 1** | Cerner Open Platform registration, API credentials |
| **Week 2** | OAuth2 testing, scope configuration |
| **Week 3** | FHIR query development, data mapping |
| **Week 4** | Rate limit testing, error handling |
| **Week 5** | UAT with clinical staff |
| **Week 6** | Production go-live |

---

### athenahealth Integration

**Overview**: athenahealth provides FHIR R4 APIs through the athenaOne platform. Commonly used by medical groups and ambulatory practices.

#### Authentication

athenahealth uses OAuth2 Authorization Code flow for user-context applications and Client Credentials for backend services.

```yaml
hdim:
  ehr:
    athena:
      enabled: true
      fhir-base-url: https://api.platform.athenahealth.com/fhir/r4

      auth:
        type: oauth2-client-credentials
        client-id: ${ATHENA_CLIENT_ID}
        client-secret: ${ATHENA_CLIENT_SECRET}
        token-url: https://api.platform.athenahealth.com/oauth2/v1/token
        scopes:
          - system/Patient.read
          - system/Observation.read
          - system/Condition.read

      # Practice ID (athenahealth-specific)
      practice-id: ${ATHENA_PRACTICE_ID}

      # Rate Limiting
      rate-limit:
        requests-per-second: 5
        burst-size: 30
```

#### Sample Query

```bash
# Get patient by athenahealth patient ID
curl -X GET "https://api.platform.athenahealth.com/fhir/r4/Patient/12345" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/fhir+json" \
  -H "athenahealth-practiceid: ${PRACTICE_ID}"
```

#### athenahealth-Specific Considerations

| Consideration | Details |
|---------------|---------|
| **Practice ID** | Each practice has a unique ID. Multi-practice groups need separate configurations. |
| **API Preview** | Some FHIR resources are in preview. Check athenahealth documentation for GA status. |
| **Bulk Export** | Limited availability. Contact athenahealth for population health APIs. |

#### Integration Timeline: 3-5 weeks

---

### Meditech Integration

**Overview**: Meditech supports FHIR R4 through their Traverse and Expanse platforms. SMART on FHIR is the primary authentication method.

```yaml
hdim:
  ehr:
    meditech:
      enabled: true
      fhir-base-url: https://fhir.meditech-hospital.org/fhir/r4

      auth:
        type: smart-on-fhir
        client-id: ${MEDITECH_CLIENT_ID}
        client-secret: ${MEDITECH_CLIENT_SECRET}
        authorize-url: https://fhir.meditech-hospital.org/authorize
        token-url: https://fhir.meditech-hospital.org/token
        scopes:
          - launch/patient
          - patient/*.read
          - openid
          - fhirUser

      # Connection Settings
      connection:
        connect-timeout: 15s
        read-timeout: 45s  # Meditech can be slower
```

#### Meditech-Specific Considerations

| Consideration | Details |
|---------------|---------|
| **Platform Version** | Traverse and Expanse have different FHIR capabilities. Verify with Meditech. |
| **Resource Coverage** | Observation and Procedure coverage may be limited compared to Epic/Cerner. |
| **Performance** | Generally slower response times. Configure longer timeouts. |
| **Bulk Export** | Not typically available. Use incremental sync approach. |

#### Integration Timeline: 4-6 weeks

---

### Allscripts Integration

**Overview**: Allscripts provides FHIR R4 through their Open platform. Supports both OAuth2 and HTTP Basic authentication.

```yaml
hdim:
  ehr:
    allscripts:
      enabled: true
      fhir-base-url: https://fhir.allscripts-hospital.org/open/r4

      auth:
        type: oauth2
        client-id: ${ALLSCRIPTS_CLIENT_ID}
        client-secret: ${ALLSCRIPTS_CLIENT_SECRET}
        token-url: https://open.allscripts.com/oauth2/token
        scopes:
          - system/*.read

      # Alternative: HTTP Basic
      # auth:
      #   type: basic
      #   username: ${ALLSCRIPTS_USERNAME}
      #   password: ${ALLSCRIPTS_PASSWORD}
```

#### Integration Timeline: 4-6 weeks

---

### Generic FHIR Server Integration

**Overview**: HDIM integrates with any FHIR R4-compliant server, including open-source options like HAPI FHIR, IBM FHIR Server, and Microsoft Azure API for FHIR.

```yaml
hdim:
  ehr:
    generic-fhir:
      enabled: true
      fhir-base-url: ${FHIR_SERVER_URL}

      # Flexible authentication
      auth:
        type: ${FHIR_AUTH_TYPE}  # oauth2, basic, bearer, none

        # OAuth2 configuration
        client-id: ${FHIR_CLIENT_ID}
        client-secret: ${FHIR_CLIENT_SECRET}
        token-url: ${FHIR_TOKEN_URL}
        scopes: ${FHIR_SCOPES}

        # OR Basic authentication
        # username: ${FHIR_USERNAME}
        # password: ${FHIR_PASSWORD}

        # OR Bearer token
        # bearer-token: ${FHIR_BEARER_TOKEN}

      # Connection Settings
      connection:
        connect-timeout: 10s
        read-timeout: 30s
        max-connections: 50
        verify-ssl: true
```

#### Supported Generic FHIR Servers

| Server | Notes |
|--------|-------|
| **HAPI FHIR** | Full R4 support. Open source. Common for research and smaller organizations. |
| **IBM FHIR Server** | Enterprise-grade. Full R4 support. Good for hybrid cloud deployments. |
| **Microsoft Azure API for FHIR** | Cloud-native. Full R4 support. Integrates with Azure AD. |
| **Google Cloud Healthcare API** | Cloud-native. Full R4 support. Integrates with Google Cloud IAM. |
| **AWS HealthLake** | Cloud-native. Full R4 support. Integrates with AWS IAM. |
| **Firely Server** | Commercial. Full R4 support. Popular in Europe. |
| **InterSystems IRIS for Health** | Enterprise-grade. Full R4 support. Common in large health systems. |

#### Integration Timeline: 1-3 weeks (fastest option)

---

### CommonWell Health Alliance Integration

**Overview**: CommonWell enables patient data sharing across participating healthcare organizations. HDIM integrates via CommonWell's FHIR API with certificate-based authentication.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        COMMONWELL INTEGRATION ARCHITECTURE                               │
│                                                                                          │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐                       │
│   │   Hospital A    │   │   Hospital B    │   │   Hospital C    │                       │
│   │   (Epic)        │   │   (Cerner)      │   │   (Meditech)    │                       │
│   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘                       │
│            │                     │                     │                                 │
│            └─────────────────────┼─────────────────────┘                                 │
│                                  │                                                       │
│                                  ▼                                                       │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │                        CommonWell Health Alliance                                 │  │
│   │                                                                                   │  │
│   │   Services:                                                                       │  │
│   │   ├─ Patient Matching (XCPD)                                                     │  │
│   │   ├─ Document Query (XCA)                                                        │  │
│   │   ├─ Document Retrieve (XCA)                                                     │  │
│   │   └─ FHIR R4 API (newer)                                                         │  │
│   │                                                                                   │  │
│   │   Authentication: X.509 Certificate + SAML                                       │  │
│   └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                │                                         │
│                                        mTLS + SAML                                       │
│                                                │                                         │
│   ┌────────────────────────────────────────────▼────────────────────────────────────┐   │
│   │                              HDIM Platform                                       │   │
│   │                                                                                  │   │
│   │   ┌─────────────────┐                                                           │   │
│   │   │ HIE Connector   │   Aggregates patient data from multiple sources           │   │
│   │   │ (CommonWell)    │   for unified quality measurement                         │   │
│   │   └─────────────────┘                                                           │   │
│   │                                                                                  │   │
│   └──────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Configuration

```yaml
hdim:
  hie:
    commonwell:
      enabled: true
      base-url: https://api.commonwellalliance.org/services

      # Certificate-based authentication
      auth:
        type: certificate
        keystore-path: /secrets/commonwell-keystore.p12
        keystore-password: ${COMMONWELL_KEYSTORE_PASSWORD}
        truststore-path: /secrets/commonwell-truststore.jks
        truststore-password: ${COMMONWELL_TRUSTSTORE_PASSWORD}

      # Organization identifiers
      organization:
        oid: "2.16.840.1.113883.3.xxx"
        name: "Your Healthcare Organization"

      # XCPD/XCA configuration
      ihe:
        xcpd-enabled: true
        xca-query-enabled: true
        xca-retrieve-enabled: true
```

#### Use Cases

| Use Case | Description |
|----------|-------------|
| **Multi-facility patient matching** | Find patients across CommonWell network using demographics |
| **Aggregate clinical data** | Retrieve C-CDAs and FHIR resources from connected organizations |
| **Care gap closure** | Identify if care was provided at another facility |
| **Transitions of care** | Track patients across health system boundaries |

#### Integration Timeline: 3-4 weeks

---

### Carequality Integration

**Overview**: Carequality is a national interoperability framework that connects EHRs, HIEs, and health systems. HDIM integrates via Carequality-participating networks.

```yaml
hdim:
  hie:
    carequality:
      enabled: true
      gateway-url: https://gateway.carequality-network.org

      # Mutual TLS authentication
      auth:
        type: mtls
        client-cert-path: /secrets/carequality-client.pem
        client-key-path: /secrets/carequality-key.pem
        ca-cert-path: /secrets/carequality-ca.pem

      # Organization identifiers
      organization:
        npi: "1234567890"
        oid: "2.16.840.1.113883.3.xxx"

      # IHE profiles
      ihe:
        xcpd-enabled: true
        xca-enabled: true
        mhd-enabled: true  # Mobile access to Health Documents (FHIR-based)
```

#### Integration Timeline: 3-4 weeks

---

### CMS BCDA Integration

**Overview**: The CMS Beneficiary Claims Data API (BCDA) provides Medicare claims data for ACOs and other entities participating in CMS programs. HDIM integrates via OAuth2 backend services.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          CMS BCDA INTEGRATION ARCHITECTURE                               │
│                                                                                          │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │                              CMS BCDA API                                        │   │
│   │                       https://api.bcda.cms.gov/                                  │   │
│   │                                                                                  │   │
│   │   Available Data:                                                               │   │
│   │   ├─ ExplanationOfBenefit (claims with diagnoses, procedures, costs)           │   │
│   │   ├─ Patient (beneficiary demographics)                                         │   │
│   │   └─ Coverage (insurance information)                                           │   │
│   │                                                                                  │   │
│   │   Data Lag: ~2 weeks from date of service                                       │   │
│   │   Update Frequency: Weekly bulk exports                                         │   │
│   │   Format: NDJSON via Bulk FHIR $export                                          │   │
│   └────────────────────────────────────────────────────────────────────────────────┘   │
│                                            │                                            │
│                                   OAuth2 Backend Services                               │
│                                            │                                            │
│   ┌────────────────────────────────────────▼────────────────────────────────────────┐   │
│   │                              HDIM Platform                                       │   │
│   │                                                                                  │   │
│   │   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐              │   │
│   │   │ BCDA Connector  │──▶│   CDR Service   │──▶│ Quality Measure │              │   │
│   │   │                 │   │ (Claims Store)  │   │    Service      │              │   │
│   │   │ - Bulk export   │   │                 │   │                 │              │   │
│   │   │ - NDJSON parse  │   │ - EOB → Dx      │   │ - HEDIS from    │              │   │
│   │   │ - Weekly sync   │   │ - EOB → Rx      │   │   claims data   │              │   │
│   │   └─────────────────┘   └─────────────────┘   └─────────────────┘              │   │
│   │                                                                                  │   │
│   │   CLAIMS + CLINICAL DATA COMBINATION:                                           │   │
│   │   ┌──────────────────────────────────────────────────────────────────────────┐ │   │
│   │   │  Claims (BCDA)           +          Clinical (EHR FHIR)                  │ │   │
│   │   │  - Diagnoses (ICD-10)               - Lab results (HbA1c values)        │ │   │
│   │   │  - Procedures (CPT)                 - Vital signs (BP, BMI)             │ │   │
│   │   │  - Medications (NDC)                - Clinical notes                    │ │   │
│   │   │  - Costs                            - Immunization details              │ │   │
│   │   │                                                                          │ │   │
│   │   │  = COMPREHENSIVE QUALITY MEASUREMENT                                     │ │   │
│   │   └──────────────────────────────────────────────────────────────────────────┘ │   │
│   └──────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Configuration

```yaml
hdim:
  cms:
    bcda:
      enabled: true
      base-url: https://api.bcda.cms.gov

      # OAuth2 Backend Services
      auth:
        type: oauth2-client-credentials
        client-id: ${BCDA_CLIENT_ID}
        client-secret: ${BCDA_CLIENT_SECRET}
        token-url: https://bcda.cms.gov/auth/token
        scopes:
          - system/*.*

      # Bulk export configuration
      bulk-export:
        schedule: "0 0 2 * * SUN"  # Weekly on Sunday at 2 AM
        since-last-export: true
        resource-types:
          - ExplanationOfBenefit
          - Patient
          - Coverage
        download-directory: /data/bcda-exports
        parallel-downloads: 4

      # Data processing
      processing:
        batch-size: 10000
        parallel-threads: 8
```

#### Sample Bulk Export Workflow

```bash
# 1. Initiate bulk export
curl -X GET "https://api.bcda.cms.gov/api/v2/Group/all/$export" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Accept: application/fhir+json" \
  -H "Prefer: respond-async"

# Response: 202 Accepted
# Content-Location: https://api.bcda.cms.gov/api/v2/jobs/12345

# 2. Poll for job completion
curl -X GET "https://api.bcda.cms.gov/api/v2/jobs/12345" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}"

# Response when complete:
{
  "transactionTime": "2025-12-15T02:30:00Z",
  "request": "https://api.bcda.cms.gov/api/v2/Group/all/$export",
  "requiresAccessToken": true,
  "output": [
    {"type": "ExplanationOfBenefit", "url": "https://...eob.ndjson"},
    {"type": "Patient", "url": "https://...patient.ndjson"},
    {"type": "Coverage", "url": "https://...coverage.ndjson"}
  ]
}

# 3. Download NDJSON files
curl -X GET "https://...eob.ndjson" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -o eob.ndjson
```

#### CMS BCDA-Specific Considerations

| Consideration | Details |
|---------------|---------|
| **ACO Participation** | Must be participating in a CMS ACO program (MSSP, ACO REACH, etc.) |
| **Attribution** | Only receive data for beneficiaries attributed to your ACO |
| **Data Lag** | Claims data has ~2-3 week lag from date of service |
| **Synthetic Data** | CMS provides synthetic data sandbox for testing |
| **Supplemental Data** | Combine with EHR data for complete picture |

#### Integration Timeline: 2-3 weeks

---

## Multi-EHR Integration Scenarios

### Scenario 1: Large Hospital System - Multi-EHR Environment

**Organization Profile:**
- 5 hospitals across 3 states
- 500,000 patient population
- EHR Mix: Epic (3 hospitals), Cerner (1 hospital), Meditech (1 critical access)
- 25+ quality measures (HEDIS + ACO)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                    MULTI-EHR INTEGRATION: LARGE HOSPITAL SYSTEM                          │
│                                                                                          │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐                       │
│   │   Hospital A    │   │   Hospital B    │   │   Hospital C    │                       │
│   │   (Epic)        │   │   (Cerner)      │   │   (Meditech)    │                       │
│   │   200K patients │   │   150K patients │   │   25K patients  │                       │
│   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘                       │
│            │                     │                     │                                 │
│   ┌────────▼────────┐   ┌────────▼────────┐   ┌────────▼────────┐                       │
│   │ Epic FHIR       │   │ Cerner FHIR     │   │ Meditech FHIR   │                       │
│   │ Connector       │   │ Connector       │   │ Connector       │                       │
│   │ (OAuth2 RS384)  │   │ (OAuth2 CC)     │   │ (SMART on FHIR) │                       │
│   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘                       │
│            │                     │                     │                                 │
│            └─────────────────────┼─────────────────────┘                                 │
│                                  │                                                       │
│                                  ▼                                                       │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │                           HDIM UNIFIED PLATFORM                                   │  │
│   │                                                                                   │  │
│   │   ┌─────────────────────────────────────────────────────────────────────────┐   │  │
│   │   │                    PATIENT MATCHING SERVICE                              │   │  │
│   │   │                                                                          │   │  │
│   │   │   Master Patient Index (MPI) Logic:                                     │   │  │
│   │   │   - Match by SSN (if available) + DOB + Name                           │   │  │
│   │   │   - Probabilistic matching algorithm (Levenshtein + weights)           │   │  │
│   │   │   - Merge patient records across EHRs                                  │   │  │
│   │   │   - Maintain source system references                                  │   │  │
│   │   │                                                                          │   │  │
│   │   │   Example:                                                               │   │  │
│   │   │   Patient "John Smith" (Epic) = Patient "J. Smith" (Cerner)            │   │  │
│   │   │   = Single HDIM patient with data from both sources                    │   │  │
│   │   └─────────────────────────────────────────────────────────────────────────┘   │  │
│   │                                                                                   │  │
│   │   ┌─────────────────────────────────────────────────────────────────────────┐   │  │
│   │   │                    UNIFIED QUALITY MEASUREMENT                           │   │  │
│   │   │                                                                          │   │  │
│   │   │   CQL Engine evaluates measures using aggregated data:                  │   │  │
│   │   │   - Patient visited Epic hospital for labs                             │   │  │
│   │   │   - Patient visited Cerner hospital for procedure                      │   │  │
│   │   │   - Both data sources used for complete measure evaluation             │   │  │
│   │   └─────────────────────────────────────────────────────────────────────────┘   │  │
│   │                                                                                   │  │
│   │   ┌─────────────────────────────────────────────────────────────────────────┐   │  │
│   │   │                    CONSOLIDATED REPORTING                                │   │  │
│   │   │                                                                          │   │  │
│   │   │   Single dashboard showing:                                             │   │  │
│   │   │   - System-wide quality scores                                          │   │  │
│   │   │   - Hospital-by-hospital comparison                                     │   │  │
│   │   │   - Care gaps across all facilities                                    │   │  │
│   │   │   - HEDIS reporting to health plans                                    │   │  │
│   │   └─────────────────────────────────────────────────────────────────────────┘   │  │
│   └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
│   RESULTS:                                                                               │
│   ├─ 67% reduction in HEDIS reporting time (from 12 weeks to 4 weeks)                  │
│   ├─ 23% improvement in care gap closure (cross-facility visibility)                   │
│   ├─ $2.3M increase in quality bonus payments                                          │
│   └─ Single source of truth for quality metrics across all facilities                  │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Timeline: 12-16 weeks**

| Phase | Duration | Activities |
|-------|----------|------------|
| Phase 1 | Weeks 1-4 | Epic connector + first 5 measures |
| Phase 2 | Weeks 5-8 | Cerner connector + MPI integration |
| Phase 3 | Weeks 9-10 | Meditech connector |
| Phase 4 | Weeks 11-12 | Full measure set + reporting |
| Phase 5 | Weeks 13-16 | UAT + training + go-live |

---

### Scenario 2: ACO with 40+ Independent Practices

**Organization Profile:**
- 75,000 attributed lives
- 40 independent practices
- 12 different EHR vendors
- CMS ACO quality reporting requirements

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                     MULTI-EHR INTEGRATION: ACO NETWORK                                   │
│                                                                                          │
│   PRACTICE TIER 1: FHIR R4 COMPLIANT (30 practices)                                     │
│   ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐                          │
│   │  Epic   │ │ Cerner  │ │ athena  │ │ NextGen │ │ eCW     │                          │
│   │(5 prac) │ │(8 prac) │ │(10 prac)│ │(4 prac) │ │(3 prac) │                          │
│   └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘                          │
│        │           │           │           │           │                                │
│        └───────────┴───────────┼───────────┴───────────┘                                │
│                                │                                                         │
│                    ┌───────────▼───────────┐                                            │
│                    │  Generic FHIR         │                                            │
│                    │  Connector            │                                            │
│                    │  (Multi-vendor)       │                                            │
│                    └───────────┬───────────┘                                            │
│                                │                                                         │
│   PRACTICE TIER 2: LEGACY SYSTEMS (10 practices)                                        │
│   ┌─────────┐ ┌─────────┐ ┌─────────┐                                                  │
│   │ Practice│ │ Practice│ │Practice │  Legacy EHRs with limited FHIR                   │
│   │  X      │ │  Y      │ │ Z       │  (older Allscripts, GE Centricity, etc.)         │
│   └────┬────┘ └────┬────┘ └────┬────┘                                                  │
│        │           │           │                                                         │
│        └───────────┼───────────┘                                                         │
│                    │                                                                     │
│                    ▼                                                                     │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │                           HL7 V2 ADAPTER                                         │   │
│   │                                                                                  │   │
│   │   For practices without FHIR:                                                   │   │
│   │   ├─ Receive HL7 ADT messages (patient demographics)                           │   │
│   │   ├─ Receive HL7 ORU messages (lab results)                                    │   │
│   │   ├─ Receive HL7 MDM messages (documents)                                      │   │
│   │   ├─ Transform to FHIR R4 resources                                            │   │
│   │   └─ Store in HDIM FHIR repository                                             │   │
│   └────────────────────────────────────────────┬────────────────────────────────────┘   │
│                                                │                                         │
│                                                ▼                                         │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │                              HDIM PLATFORM                                        │  │
│   │                                                                                   │  │
│   │   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐               │  │
│   │   │ Data            │   │ Quality Measure │   │ ACO Reporting   │               │  │
│   │   │ Normalization   │──▶│ Evaluation      │──▶│ (CMS Format)    │               │  │
│   │   │                 │   │                 │   │                 │               │  │
│   │   │ - Code mapping  │   │ - 34 ACO        │   │ - QRDA III      │               │  │
│   │   │ - Unit convert  │   │   measures      │   │ - APM reports   │               │  │
│   │   │ - Date formats  │   │ - Real-time     │   │ - Dashboard     │               │  │
│   │   └─────────────────┘   └─────────────────┘   └─────────────────┘               │  │
│   │                                                                                   │  │
│   └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
│   RESULTS:                                                                               │
│   ├─ 70% cost reduction vs previous vendor (per-practice pricing eliminated)           │
│   ├─ Real-time care gap visibility across all 40 practices                             │
│   ├─ Automated CMS quality reporting (eliminated 200 hours/quarter manual work)        │
│   └─ 15% improvement in ACO quality score (better data = better care)                  │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Timeline: 8-12 weeks**

| Phase | Duration | Activities |
|-------|----------|------------|
| Phase 1 | Weeks 1-2 | FHIR Tier 1 practices (30 practices via generic connector) |
| Phase 2 | Weeks 3-4 | HL7 v2 adapter for Tier 2 practices |
| Phase 3 | Weeks 5-6 | Data normalization and MPI |
| Phase 4 | Weeks 7-8 | Measure validation and reporting |
| Phase 5 | Weeks 9-12 | Go-live and hypercare |

---

### Scenario 3: Medicare Advantage Plan - Claims + Clinical Data

**Organization Profile:**
- 250,000 Medicare Advantage beneficiaries
- Star Ratings program (quality bonus payments)
- Need to supplement claims data with clinical data for improved accuracy

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                     HYBRID INTEGRATION: CLAIMS + CLINICAL DATA                           │
│                                                                                          │
│   ┌───────────────────────────────────────────────────────────────────────────────────┐ │
│   │                           CMS BCDA (Claims Data)                                   │ │
│   │                                                                                    │ │
│   │   Weekly Bulk Export:                                                             │ │
│   │   ├─ ExplanationOfBenefit (claims with diagnoses, procedures, costs)             │ │
│   │   ├─ Patient (beneficiary demographics)                                          │ │
│   │   └─ Coverage (insurance details)                                                │ │
│   │                                                                                    │ │
│   │   Data Characteristics:                                                           │ │
│   │   ├─ 2-3 week lag from date of service                                          │ │
│   │   ├─ Claims-based diagnoses (may miss clinical detail)                          │ │
│   │   └─ No lab values, vitals, or clinical notes                                   │ │
│   └────────────────────────────────────────────────────┬──────────────────────────────┘ │
│                                                        │                                │
│   ┌────────────────────────────────────────────────────┼──────────────────────────────┐ │
│   │                      Provider FHIR APIs (Clinical Data)                          │ │
│   │                                                    │                              │ │
│   │   ┌─────────────────┐  ┌─────────────────┐  ┌─────▼─────────┐                    │ │
│   │   │   PCP Network   │  │  Specialist     │  │   Hospital    │                    │ │
│   │   │   (50 practices)│  │  Groups (20)    │  │   Systems (5) │                    │ │
│   │   │                 │  │                 │  │               │                    │ │
│   │   │   FHIR R4 APIs  │  │   FHIR R4 APIs  │  │  FHIR R4 APIs │                    │ │
│   │   └────────┬────────┘  └────────┬────────┘  └───────┬───────┘                    │ │
│   │            │                    │                   │                            │ │
│   │            └────────────────────┼───────────────────┘                            │ │
│   │                                 │                                                │ │
│   │   Clinical Data:                ▼                                                │ │
│   │   ├─ Lab results (HbA1c values, lipid panels)                                   │ │
│   │   ├─ Vital signs (blood pressure, BMI)                                          │ │
│   │   ├─ Immunization dates and details                                             │ │
│   │   ├─ Medication adherence (PDC calculations)                                    │ │
│   │   └─ Clinical notes (for complex conditions)                                    │ │
│   └────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                │                                         │
│                                                ▼                                         │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │                           HDIM HYBRID DATA PLATFORM                               │  │
│   │                                                                                   │  │
│   │   ┌─────────────────────────────────────────────────────────────────────────┐   │  │
│   │   │                      DATA FUSION ENGINE                                  │   │  │
│   │   │                                                                          │   │  │
│   │   │   Claims Data (BCDA)              Clinical Data (Provider FHIR)         │   │  │
│   │   │   ─────────────────               ─────────────────────────             │   │  │
│   │   │   Diagnosis: E11.9               Lab: HbA1c = 6.8%                      │   │  │
│   │   │   (Type 2 Diabetes)              (Controlled)                           │   │  │
│   │   │                                                                          │   │  │
│   │   │   Claim: Annual Wellness         Clinical: BP 128/82                    │   │  │
│   │   │   Visit billed                   Weight: 185 lbs, BMI: 27.4            │   │  │
│   │   │                                                                          │   │  │
│   │   │   = COMPLETE PICTURE FOR HEDIS/STAR RATINGS                             │   │  │
│   │   └─────────────────────────────────────────────────────────────────────────┘   │  │
│   │                                                                                   │  │
│   │   ┌─────────────────────────────────────────────────────────────────────────┐   │  │
│   │   │                      STAR RATINGS OPTIMIZATION                           │   │  │
│   │   │                                                                          │   │  │
│   │   │   Before HDIM (Claims Only):                                            │   │  │
│   │   │   ├─ CDC - Blood Sugar Controlled: 55% (claims-based proxy)             │   │  │
│   │   │   ├─ Star Rating: 3.5 stars                                             │   │  │
│   │   │   └─ Quality Bonus: $0                                                  │   │  │
│   │   │                                                                          │   │  │
│   │   │   After HDIM (Claims + Clinical):                                       │   │  │
│   │   │   ├─ CDC - Blood Sugar Controlled: 72% (actual lab values)              │   │  │
│   │   │   ├─ Star Rating: 4.0 stars                                             │   │  │
│   │   │   └─ Quality Bonus: $12M                                                │   │  │
│   │   └─────────────────────────────────────────────────────────────────────────┘   │  │
│   │                                                                                   │  │
│   │   ┌─────────────────────────────────────────────────────────────────────────┐   │  │
│   │   │                      HEDIS SUPPLEMENTAL DATA                             │   │  │
│   │   │                                                                          │   │  │
│   │   │   HDIM generates HEDIS-compliant supplemental data files:               │   │  │
│   │   │   ├─ Member-level detail (patient ID, measure, numerator/denominator)  │   │  │
│   │   │   ├─ Clinical evidence (lab values, dates, provider IDs)               │   │  │
│   │   │   └─ Audit trail (source system, extraction date, validation status)   │   │  │
│   │   │                                                                          │   │  │
│   │   │   Submit to NCQA for HEDIS audit with clinical documentation           │   │  │
│   │   └─────────────────────────────────────────────────────────────────────────┘   │  │
│   │                                                                                   │  │
│   └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
│   RESULTS:                                                                               │
│   ├─ Star Rating improved from 3.5 to 4.0 (0.5 star improvement)                       │
│   ├─ $12M annual quality bonus payment                                                  │
│   ├─ 300% ROI in Year 1                                                                │
│   ├─ HEDIS audit passed with 98% clinical documentation rate                           │
│   └─ Real-time care gap visibility for care management team                            │
│                                                                                          │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

**Implementation Timeline: 6-8 weeks**

| Phase | Duration | Activities |
|-------|----------|------------|
| Phase 1 | Weeks 1-2 | CMS BCDA integration + claims data import |
| Phase 2 | Weeks 3-4 | Provider FHIR integration (top 10 providers by volume) |
| Phase 3 | Weeks 5-6 | Data fusion + member matching |
| Phase 4 | Weeks 7-8 | Star Ratings dashboard + HEDIS supplemental data |

---

## Authentication Patterns

### OAuth2 Authentication Types

| Type | Use Case | Configuration |
|------|----------|---------------|
| **Client Credentials** | Backend services (no user context) | `grant_type=client_credentials` |
| **Authorization Code + PKCE** | User-facing apps (EHR launch) | SMART on FHIR launch |
| **JWT Bearer Assertion** | Epic, high-security environments | RS256/RS384 signed JWT |
| **Mutual TLS (mTLS)** | HIE connections, Carequality | X.509 client certificates |

### Token Management Best Practices

```yaml
# Token caching and refresh strategy
token-management:
  cache:
    enabled: true
    storage: redis
    key-prefix: "oauth2:token:"

  refresh:
    # Refresh tokens before expiration
    threshold-seconds: 60
    # Buffer for clock skew
    clock-skew-seconds: 10

  retry:
    # Retry on token failures
    max-attempts: 3
    backoff-multiplier: 2.0
```

---

## Error Handling & Troubleshooting

### Common HTTP Errors

| Error | Cause | HDIM Response |
|-------|-------|---------------|
| **401 Unauthorized** | Token expired or invalid | Refresh token and retry |
| **403 Forbidden** | Insufficient scopes | Check scope configuration |
| **404 Not Found** | Resource doesn't exist | Log and skip (patient may not exist) |
| **429 Too Many Requests** | Rate limit exceeded | Exponential backoff (retry-after header) |
| **500 Internal Server Error** | EHR system error | Retry with backoff, alert if persistent |
| **502/503/504** | EHR unavailable | Circuit breaker, return cached data |

### Retry Strategy

```yaml
retry:
  strategy: exponential-backoff
  initial-interval: 100ms
  multiplier: 2.0
  max-interval: 30s
  max-attempts: 5

  # Specific error handling
  retryable-errors:
    - 429
    - 500
    - 502
    - 503
    - 504

  non-retryable-errors:
    - 400
    - 401
    - 403
    - 404
```

### Circuit Breaker Configuration

```yaml
circuit-breaker:
  enabled: true

  # Open circuit after 5 failures in 60 seconds
  failure-rate-threshold: 50  # percent
  slow-call-rate-threshold: 50
  slow-call-duration-threshold: 5s

  # Close circuit after 30 seconds in half-open state
  wait-duration-in-open-state: 30s
  permitted-number-of-calls-in-half-open-state: 10

  # Minimum calls before calculating failure rate
  minimum-number-of-calls: 10
  sliding-window-size: 100
```

### Troubleshooting Guide

| Symptom | Possible Cause | Resolution |
|---------|---------------|------------|
| All queries failing | Token expired | Check token refresh logic |
| Slow queries (>5s) | EHR overloaded | Reduce query rate, use caching |
| Missing patient data | Wrong patient ID format | Check identifier system URIs |
| Incomplete resources | FHIR version mismatch | Verify R4 compatibility |
| SSL errors | Certificate issues | Check certificate chain, expiration |

---

## Performance Optimization

### Caching Strategy

```yaml
# HIPAA-compliant caching with appropriate TTLs
cache:
  redis:
    enabled: true

  ttl:
    # Patient demographics (stable, low PHI risk)
    patient: 24h

    # Clinical observations (PHI, short TTL)
    observation: 5m  # HIPAA compliant

    # Conditions (relatively stable)
    condition: 12h

    # Medications (can change frequently)
    medication: 4h

    # Measure results (computed, cacheable)
    measure-result: 1h
```

### Query Optimization

```yaml
# Optimize FHIR queries for performance
query-optimization:
  # Use _include to reduce round trips
  include-related-resources: true

  # Limit result count
  default-page-size: 100
  max-page-size: 500

  # Use _lastUpdated for incremental sync
  incremental-sync:
    enabled: true
    lookback-hours: 24

  # Parallel queries for independent resources
  parallel-execution:
    enabled: true
    max-concurrent: 5
```

### Performance Benchmarks

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| FHIR query latency (p95) | <200ms | Per-query timing |
| CQL evaluation (per patient) | <500ms | End-to-end timing |
| Cache hit rate | >80% | Redis cache stats |
| Throughput | 50K patients/hour | Batch evaluation timing |
| Error rate | <0.1% | Error count / total queries |

---

## Security Best Practices

### Transport Security

```yaml
# TLS 1.3 for all connections
security:
  tls:
    min-version: TLSv1.3
    cipher-suites:
      - TLS_AES_256_GCM_SHA384
      - TLS_CHACHA20_POLY1305_SHA256
      - TLS_AES_128_GCM_SHA256

  # Certificate validation
  certificate-validation:
    enabled: true
    check-revocation: true
    ocsp-stapling: true
```

### Credential Management

```yaml
# Secrets management
secrets:
  provider: vault  # or: aws-secrets-manager, azure-keyvault

  # Rotate credentials regularly
  rotation:
    oauth2-client-secret: 90d
    certificates: 365d
    api-keys: 180d
```

### Audit Logging

```yaml
# Log all FHIR queries (without PHI in logs)
audit:
  fhir-queries:
    enabled: true
    log-request-url: true
    log-response-status: true
    log-response-body: false  # Never log PHI

  # Audit trail for compliance
  audit-trail:
    enabled: true
    storage: postgresql
    retention-days: 2555  # 7 years for HIPAA
```

---

## Integration Checklist

### Pre-Integration

- [ ] Identify FHIR server URL and version (R4 required)
- [ ] Obtain API credentials (OAuth2 client ID/secret or certificates)
- [ ] Verify network connectivity (firewall rules, VPN if needed)
- [ ] Review rate limits and plan query strategy
- [ ] Test authentication in sandbox environment
- [ ] Map required FHIR resources to quality measures

### During Integration

- [ ] Configure HDIM EHR connector for target platform
- [ ] Validate OAuth2/authentication flow
- [ ] Test FHIR queries for each required resource type
- [ ] Verify data mapping (codes, units, identifiers)
- [ ] Configure caching and retry policies
- [ ] Set up monitoring and alerting

### Post-Integration

- [ ] Run end-to-end test with sample patients
- [ ] Validate measure calculations against manual review
- [ ] Monitor error rates and latency
- [ ] Document integration for operations team
- [ ] Train clinical users on workflow
- [ ] Schedule production go-live

---

## Contact & Support

**Integration Support**: integrations@hdim.io
**Technical Documentation**: https://docs.hdim.io/integrations
**API Status**: https://status.hdim.io

---

*Document Version: 2.0*
*Last Updated: January 2026*
*Classification: Public*

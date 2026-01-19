# Gateway Architecture Guide

**Status**: Production ✅
**Last Updated**: January 19, 2026
**Modularization Date**: January 10, 2026 (commit c60cb168)

---

## Overview

HDIM uses a **modularized multi-gateway architecture** where a shared `gateway-core` module eliminates code duplication while specialized gateway services handle domain-specific routing, authentication, and request processing.

### Key Design Principles

- ✅ **Code Reuse**: Shared `gateway-core` module for common gateway functionality
- ✅ **Domain Separation**: Each gateway specializes in its domain (Admin, Clinical, FHIR)
- ✅ **Clean Architecture**: Gateway-trust authentication filters, rate limiting, routing
- ✅ **Service Isolation**: Multi-tenant isolation with tenant-aware rate limiting
- ✅ **Production Ready**: Standardized security configuration across all gateways

---

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                       │
│         (Clinical Portal, Admin Portal, External Systems)        │
└────────────┬─────────────────┬──────────────────┬────────────────┘
             │                 │                  │
      ┌──────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐
      │  gateway-   │  │  gateway-    │  │  gateway-    │
      │  admin-     │  │  clinical-   │  │  fhir-       │
      │  service    │  │  service     │  │  service     │
      │  (8002)     │  │  (8003)      │  │  (8004)      │
      └──────┬──────┘  └───────┬──────┘  └───────┬──────┘
             │                 │                  │
             └─────────────────┼──────────────────┘
                               │
             ┌─────────────────▼──────────────────┐
             │      gateway-core (Shared)         │
             │  ─────────────────────────────────│
             │  • Authentication (JWT validation)│
             │  • Authorization (RBAC)           │
             │  • Rate Limiting                  │
             │  • Request Routing                │
             │  • Tenant Isolation               │
             │  • Security Filters               │
             └─────────────────┬──────────────────┘
                               │
             ┌─────────────────▼──────────────────┐
             │  Service Routing & Forwarding      │
             │  (ServiceRoutingService,           │
             │   GatewayForwarder)                │
             └─────────────────┬──────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   ┌────▼────┐  ┌──────────┐  ┌──────────┐  ┌───────▼────┐
   │ Patient  │  │ Quality  │  │   FHIR   │  │    Care    │
   │ Service  │  │ Measure  │  │ Service  │  │    Gap     │
   │ (8084)   │  │ (8087)   │  │ (8085)   │  │ Service    │
   │          │  │          │  │          │  │ (8086)     │
   └──────────┘  └──────────┘  └──────────┘  └────────────┘
```

### Layer Descriptions

#### 1. Client Applications
External systems and user interfaces that initiate requests:
- Clinical Portal (web application)
- Admin Portal (management interface)
- EHR systems, external integrations
- Mobile applications

#### 2. Specialized Gateway Services (8002-8004)
Domain-specific gateway implementations that use shared gateway-core:

| Gateway | Port | Purpose | Specialization |
|---------|------|---------|-----------------|
| **gateway-admin-service** | 8002 | Administrative API gateway | Tenant configuration, service management, audit logging |
| **gateway-clinical-service** | 8003 | Clinical API gateway | Patient data access, quality measures, clinical workflows |
| **gateway-fhir-service** | 8004 | FHIR R4 API gateway | FHIR-compliant resource access, HL7 compatibility |
| **gateway-service** | 8001 | Legacy/Core gateway | General-purpose routing, multi-tenant support |

#### 3. Gateway-Core (Shared Module)
Located at: `backend/modules/shared/infrastructure/gateway-core`

**Components:**
- `GatewayAuthenticationFilter` - JWT validation and user extraction
- `TrustedHeaderAuthFilter` - Gateway-trust authentication pattern
- `TrustedTenantAccessFilter` - Tenant isolation and RBAC
- `AuthRateLimitFilter` + `TenantRateLimitFilter` - Rate limiting
- `ServiceRoutingService` - Service discovery and routing rules
- `GatewayForwarder` - Request forwarding and response handling
- `GatewaySecurityConfig` - Spring Security configuration
- `PublicPathRegistry` - Public vs protected endpoint definitions

#### 4. Service Routing & Forwarding
Routes requests to appropriate microservices based on:
- Request path (`/patient/*`, `/quality/*`, `/fhir/*`, `/care-gap/*`)
- Tenant ID (from X-Tenant-ID header)
- User roles and permissions
- Service availability

#### 5. Backend Services
Core microservices handling business logic:
- **Patient Service (8084)**: Demographics, risk scores
- **Quality Measure Service (8087)**: HEDIS measures, CQL evaluation
- **FHIR Service (8085)**: FHIR R4 resources
- **Care Gap Service (8086)**: Care gap detection

---

## Gateway-Core Module Components

### 1. Authentication & Authorization

```java
// GatewayAuthenticationFilter.java (in gateway-core)
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    // Validates JWT tokens from client requests
    // Extracts user, roles, tenant information
    // Sets SecurityContext for request processing
}

// TrustedHeaderAuthFilter.java (in backend services)
// Uses headers injected by gateway (gateway-trust pattern)
// Trusts: X-Auth-User-Id, X-Auth-Tenant-Ids, X-Auth-Roles
// Does NOT re-validate JWT or query database
```

**Key Pattern**: Gateway validates JWT, backend services trust gateway headers.

### 2. Rate Limiting

```java
// Tenant-aware rate limiting
// Default: 1000 requests/minute per tenant
// Prevents one tenant from impacting others
// Redis-backed for distributed deployments
```

### 3. Request Routing

```java
// ServiceRoutingService determines backend service
// Rules:
//   /patient/* → patient-service:8084
//   /quality-measure/* → quality-measure-service:8087
//   /fhir/* → fhir-service:8085
//   /care-gap/* → care-gap-service:8086
```

### 4. Tenant Isolation

```java
// Multi-tenant support
// X-Tenant-ID header validation
// Backend services filter all queries by tenant_id
// Rate limiting applied per-tenant
```

---

## Why Four Gateway Services?

The January 2026 refactoring (commit c60cb168) split the monolithic gateway into specialized services for several reasons:

### ✅ Domain-Driven Design

Each gateway handles a specific domain with specialized:
- **Authentication requirements** (different token scopes)
- **Rate limiting policies** (admin APIs may differ from clinical APIs)
- **Route configurations** (admin APIs different from clinical workflows)
- **Logging/auditing** (audit sensitive admin operations separately)

```
Admin Ops         Clinical Ops      FHIR Interop
  │                 │                  │
  ├─ Config        ├─ Measures       ├─ Standards
  ├─ Audit         ├─ Care Gaps      ├─ Compliance
  ├─ Users         └─ Patient Data   └─ Integration
  └─ Monitoring
```

### ✅ Independent Scaling

- Clinical traffic (frequent, high volume) vs Admin traffic (infrequent, moderate volume)
- Scale gateway-clinical-service independently for patient-facing workloads
- Scale gateway-admin-service independently for administrative tasks

### ✅ Security Boundaries

- Admin gateway can enforce stricter authentication
- Clinical gateway can optimize for performance
- FHIR gateway can enforce FHIR-specific compliance
- Each can be deployed to different network zones

### ✅ Deployment Flexibility

```yaml
# Example: Different deployment strategies
gateway-admin-service:     # Behind VPN or private network
  image: hdim/gateway-admin:v1.0
  ports: [8002:8080]
  networks: [internal]

gateway-clinical-service:  # Public-facing, high traffic
  image: hdim/gateway-clinical:v1.0
  ports: [8003:8080]
  networks: [public, internal]

gateway-fhir-service:      # Strict FHIR compliance
  image: hdim/gateway-fhir:v1.0
  ports: [8004:8080]
  networks: [public, internal]
```

### ✅ Code Maintainability

- No shared gateway code duplication
- Each specialized gateway is small, focused, testable
- Changes to clinical routing don't affect admin operations
- Clear separation of concerns

---

## How Gateway-Core Eliminates Duplication

### Before (Pre-January 2026)

```
gateway-service/
├── auth/
│   ├── JwtValidator.java      (duplicated in next service)
│   ├── TenantFilter.java       (duplicated in next service)
│   └── RateLimiter.java        (duplicated in next service)
├── routing/
│   ├── ServiceRouter.java      (duplicated in next service)
│   └── RequestForwarder.java   (duplicated in next service)
└── config/
    └── SecurityConfig.java     (duplicated in next service)

gateway-admin-service/         # Entire auth, routing, config DUPLICATED
gateway-clinical-service/      # Entire auth, routing, config DUPLICATED
gateway-fhir-service/          # Entire auth, routing, config DUPLICATED
```

### After (January 2026 Modularization)

```
gateway-core/ (SHARED)
├── auth/
│   ├── GatewayAuthenticationFilter.java
│   ├── PublicPathRegistry.java
│   └── CustomUserDetailsService.java
├── ratelimit/
│   ├── AuthRateLimitFilter.java
│   ├── TenantRateLimitFilter.java
│   └── TenantRateLimitService.java
├── filter/
│   └── RateLimitFilter.java
├── service/
│   ├── ServiceRoutingService.java
│   └── GatewayForwarder.java
└── config/
    ├── GatewayAuthenticationConfig.java
    ├── GatewaySecurityConfig.java
    └── GatewayCacheConfig.java

gateway-admin-service/         # Minimal, uses gateway-core
├── admin/
│   ├── GatewayAdminController.java
│   └── configversion/          # SPECIALIZED: Config management
│       ├── TenantServiceConfigService.java
│       └── ...
└── application.yml

gateway-clinical-service/      # Minimal, uses gateway-core
├── clinical/
│   └── GatewayClinicalController.java
└── application.yml

gateway-fhir-service/          # Minimal, uses gateway-core
├── fhir/
│   └── GatewayFhirController.java
└── application.yml
```

### Benefits Realized

| Issue | Before | After |
|-------|--------|-------|
| Code duplication | 1000+ lines duplicated 3-4x | 0% duplication (shared module) |
| Bug fixes | Fix in 3-4 places | Fix once in gateway-core |
| Consistency | Manually sync across services | Automatically consistent |
| Testing | Test auth/routing 3x separately | Test once in gateway-core |
| Maintenance | Complex, error-prone | Simple, centralized |

---

## Authentication Flow

```
┌─────────────────────┐
│  Client Request     │
│  + JWT Token        │
└──────────┬──────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│  gateway-admin|clinical|fhir-service         │
│  (One of the specialized gateways)           │
└──────────┬───────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│  GatewayAuthenticationFilter (gateway-core)  │
│  1. Extract JWT from Authorization header   │
│  2. Validate JWT signature                  │
│  3. Extract user, tenant, roles             │
│  4. Create SecurityContext                  │
│  5. Inject trusted headers:                 │
│     • X-Auth-User-Id                        │
│     • X-Auth-Tenant-Ids                     │
│     • X-Auth-Roles                          │
│     • X-Auth-Validated (HMAC signature)     │
└──────────┬───────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│  ServiceRoutingService (gateway-core)       │
│  Routes request to appropriate backend      │
│  service based on path                      │
└──────────┬───────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│  Backend Service (patient-service, etc)     │
│  1. Receives request with trusted headers   │
│  2. TrustedHeaderAuthFilter validates       │
│  3. Sets SecurityContext from headers       │
│  4. Processes request (no JWT re-validation)│
└──────────┬───────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────┐
│  Response with data (filtered by tenant)    │
└──────────────────────────────────────────────┘
```

**Key Point**: Backend services trust headers injected by gateway and don't re-validate JWT.

---

## Specialized Gateway Details

### Gateway-Admin-Service (Port 8002)

**Purpose**: Tenant configuration, service management, audit operations

**Specialized Components**:
```java
// TenantServiceConfig - Manage service configurations per tenant
public class TenantServiceConfig {
    private String tenantId;
    private String serviceName;
    private String configurationJson;
    private LocalDateTime approvalDate;
}

// TenantServiceConfigService - Admin operations
public TenantServiceConfigService {
    public void approveConfiguration(String configId);
    public void auditConfigChange(String configId, String changeReason);
    public List<TenantServiceConfig> getApprovalQueue();
}
```

**Use Cases**:
- Tenant configuration management
- Service configuration approvals
- Multi-tenant settings
- Audit trail access

### Gateway-Clinical-Service (Port 8003)

**Purpose**: Clinical data access, measure evaluation, clinical workflows

**Routing**:
```
/patient/* → patient-service
/quality-measure/* → quality-measure-service
/care-gap/* → care-gap-service
/clinical-workflow/* → clinical-workflow-service
```

**Rate Limiting**: May be higher for clinical operations (frequent patient queries)

**Use Cases**:
- Patient lookup and demographics
- Quality measure evaluation
- Care gap identification
- Clinical workflow management

### Gateway-FHIR-Service (Port 8004)

**Purpose**: FHIR R4-compliant resource access, HL7 interoperability

**FHIR Compliance**:
- FHIR R4 resource routing
- FHIR search parameters
- FHIR operation support
- Content negotiation (JSON/XML)

**Routing**:
```
/fhir/Patient/* → fhir-service
/fhir/Observation/* → fhir-service
/fhir/Condition/* → fhir-service
/fhir/MedicationRequest/* → fhir-service
```

**Use Cases**:
- EHR integration (FHIR-compliant APIs)
- External system data exchange
- HL7 FHIR compliance testing
- Care coordination with external providers

---

## Configuration

### Gateway-Core Configuration (Shared)

```yaml
# application.yml (used by all gateways)
gateway:
  auth:
    jwt-secret: ${JWT_SECRET}
    jwt-expiration-ms: 900000  # 15 minutes
    public-paths:
      - /health
      - /actuator/**
      - /public/**

  routing:
    patient-service: http://patient-service:8084
    quality-measure-service: http://quality-measure-service:8087
    fhir-service: http://fhir-service:8085
    care-gap-service: http://care-gap-service:8086

  rate-limit:
    requests-per-minute: 1000
    burst-capacity: 100
```

### Specialized Gateway Configuration

```yaml
# gateway-admin-service/application.yml
gateway:
  admin:
    config-approval-required: true
    max-config-size: 10MB
    audit-retention-days: 365

---

# gateway-clinical-service/application.yml
gateway:
  clinical:
    rate-limit:
      requests-per-minute: 2000  # Higher for clinical ops
    cache-ttl: 5m
    patient-data-encryption: true

---

# gateway-fhir-service/application.yml
gateway:
  fhir:
    fhir-version: R4
    supported-formats: [application/fhir+json, application/fhir+xml]
    conformance-checks: true
```

---

## Deployment Topology

### Local Development (docker-compose.yml)

```yaml
gateway-service:
  image: hdim/gateway-service:latest
  ports: [8001:8080]

gateway-admin-service:
  image: hdim/gateway-admin-service:latest
  ports: [8002:8080]

gateway-clinical-service:
  image: hdim/gateway-clinical-service:latest
  ports: [8003:8080]

gateway-fhir-service:
  image: hdim/gateway-fhir-service:latest
  ports: [8004:8080]
```

### Production Deployment (Kubernetes)

```yaml
# gateway-admin-service
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-admin
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: gateway-admin
        image: hdim/gateway-admin-service:v1.0.0
        ports: [8080]
        env:
        - name: GATEWAY_AUTH_DEV_MODE
          value: "false"
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: gateway-secrets
              key: jwt-secret
      # Admin gateway behind VPN or private network
      affinity:
        nodeAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - preference:
              matchExpressions:
              - key: node-type
                operator: In
                values: [internal-only]

---

# gateway-clinical-service (public-facing, high traffic)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-clinical
spec:
  replicas: 5  # Scale higher for clinical workloads
  template:
    spec:
      containers:
      - name: gateway-clinical
        image: hdim/gateway-clinical-service:v1.0.0

      # Pod disruption budget (ensure continuity)
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values: [gateway-clinical]
```

---

## Troubleshooting

### Gateway Won't Start

**Error**: `Cannot find gateway-core module`

**Solution**:
```bash
# Ensure gateway-core is built first
cd backend
./gradlew :modules:shared:infrastructure:gateway-core:build

# Then build specialized gateways
./gradlew :modules:services:gateway-admin-service:build
./gradlew :modules:services:gateway-clinical-service:build
```

### Authentication Failures

**Error**: `401 Unauthorized: Invalid JWT`

**Debugging**:
```bash
# Check if JWT is being validated correctly
curl -X POST http://localhost:8002/auth/login -d '{"username":"test", "password":"pass"}'

# Check gateway logs
docker compose logs gateway-admin-service | grep -i auth

# Verify JWT secret is set
docker compose exec gateway-admin-service env | grep JWT_SECRET
```

### Request Routing Failures

**Error**: `Cannot route to backend service`

**Debugging**:
```bash
# Check if backend service is running
docker compose ps

# Check service routing configuration
docker compose logs gateway-clinical-service | grep routing

# Manual test to backend service
curl http://patient-service:8084/patient/health
```

### Rate Limiting Issues

**Error**: `429 Too Many Requests`

**Debugging**:
```bash
# Check tenant rate limit status
docker exec hdim-redis redis-cli GET "rate_limit:tenant:TENANT001"

# Check rate limit configuration
grep -A 5 "rate-limit:" gateway-core/src/main/resources/application.yml

# Temporarily disable for testing
export GATEWAY_RATE_LIMIT_ENABLED=false
```

---

## Security Best Practices

### 1. Never Disable JWT Validation in Production

```java
// ❌ DO NOT DO THIS
if (environment.equals("local")) {
    skipJwtValidation = true;  // NEVER!
}

// ✅ DO THIS
@Value("${gateway.auth.jwt-secret}")
private String jwtSecret;  // Always validate, use different secrets per environment
```

### 2. Validate X-Auth Headers Integrity

```java
// gateway-core validates the HMAC signature on X-Auth-Validated header
// Backend services should TRUST this validation (gateway-trust pattern)
// DO NOT re-validate JWT in backend services (avoids repeated validation)
```

### 3. Tenant Isolation is Critical

```java
// ✅ Always filter by tenant
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(@Param("id") String id, @Param("tenantId") String tenantId);

// ❌ NEVER allow cross-tenant access
@Query("SELECT p FROM Patient p WHERE p.id = :id")  // Missing tenant filter!
Optional<Patient> findById(@Param("id") String id);
```

### 4. Rate Limiting Per Tenant

```java
// Rate limiting is tenant-aware
// One tenant cannot impact another
// Redis ensures distributed consistency
```

---

## Related Documentation

- **Event Sourcing Architecture**: `docs/architecture/EVENT_SOURCING_ARCHITECTURE.md` - How event services handle state
- **Authentication & Authorization**: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md` - Detailed auth flow
- **Service Catalog**: `docs/services/SERVICE_CATALOG.md` - All 51 services with ports and dependencies
- **System Architecture**: `docs/architecture/SYSTEM_ARCHITECTURE.md` - Complete platform design
- **Code Standards**: `backend/docs/CODING_STANDARDS.md` - Controller and security patterns

---

## Quick Reference: Which Gateway to Use?

| Client | Use This Gateway | Port | Use Case |
|--------|------------------|------|----------|
| Clinical Portal | gateway-clinical-service | 8003 | Patient data, measures, care gaps |
| Admin Portal | gateway-admin-service | 8002 | Configuration, tenant management |
| External EHR | gateway-fhir-service | 8004 | FHIR-compliant data exchange |
| Internal Tools | gateway-service (legacy) | 8001 | General-purpose routing |

---

_Last Updated: January 19, 2026_
_Version: 1.0 - Initial Documentation_
_Modularization Commit: c60cb168_

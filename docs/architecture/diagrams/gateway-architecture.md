# Gateway Modularization Architecture

Complete visual reference for 4-Gateway Architecture with shared gateway-core module.

---

## Overall Gateway Architecture

```mermaid
graph TB
    A["Clients"] -->|"Requests"| B["Load Balancer<br/>Port 8000-8004"]

    B -->|"Admin Routes<br/>/admin/*"| C["gateway-admin-service<br/>Port 8002"]
    B -->|"Clinical Routes<br/>/clinical/*"| D["gateway-clinical-service<br/>Port 8003"]
    B -->|"FHIR Routes<br/>/fhir/*"| E["gateway-fhir-service<br/>Port 8004"]
    B -->|"Legacy Routes<br/>/*"| F["gateway-service<br/>Port 8001"]

    C -->|"Depends on"| G["gateway-core<br/>(Shared Module)"]
    D -->|"Depends on"| G
    E -->|"Depends on"| G
    F -->|"Depends on"| G

    C -->|"Validates JWT"| H["JWT Tokens"]
    D -->|"Validates JWT"| H
    E -->|"Validates JWT"| H
    F -->|"Validates JWT"| H

    C -->|"Injects Headers"| I["X-Auth-* Headers"]
    D -->|"Injects Headers"| I
    E -->|"Injects Headers"| I
    F -->|"Injects Headers"| I

    C -->|"Routes to"| J["Backend Services<br/>(patient, quality, care-gap, etc)"]
    D -->|"Routes to"| J
    E -->|"Routes to"| J
    F -->|"Routes to"| J

    style G fill:#fff3e0
    style C fill:#e8f5e9
    style D fill:#e8f5e9
    style E fill:#e8f5e9
    style F fill:#e8f5e9
```

**Key Components**:
- **Load Balancer**: Routes incoming requests to appropriate gateway
- **4 Specialized Gateways**: Each handles specific domain
- **gateway-core**: Shared authentication, rate limiting, logging
- **Backend Services**: Receive authenticated requests from gateways

---

## Gateway-Core Shared Module

```mermaid
graph LR
    A["gateway-core"] --> B["TrustedHeaderAuthFilter<br/>Validates JWT"]
    A --> C["RateLimitingFilter<br/>Per-tenant limits"]
    A --> D["AuditLoggingFilter<br/>Tracks all requests"]
    A --> E["CorsFilter<br/>Origin validation"]
    A --> F["RequestIdFilter<br/>Trace ID"]

    B -->|"Provides"| G["X-Auth-User-Id<br/>X-Auth-Tenant-Ids<br/>X-Auth-Roles<br/>X-Auth-Validated"]

    style A fill:#fff3e0
    style B fill:#c8e6c9
    style C fill:#c8e6c9
    style D fill:#c8e6c9
    style E fill:#c8e6c9
    style F fill:#c8e6c9
    style G fill:#b3e5fc
```

**Shared Functionality**:
1. **JWT Validation**: Validates JWT token once at gateway entry
2. **Header Injection**: Injects X-Auth-* headers for backend services
3. **Rate Limiting**: Enforces per-tenant request limits
4. **Audit Logging**: Tracks all API requests
5. **CORS**: Manages cross-origin requests
6. **Request Tracing**: Generates trace IDs for distributed tracing

---

## Request Flow: Admin Gateway Example

```mermaid
graph TB
    A["Admin Client<br/>Requests /admin/users"] -->|"HTTP + JWT"| B["gateway-admin-service<br/>:8002"]

    B -->|"1. Extract JWT"| C["Parse Authorization<br/>Bearer token"]

    C -->|"2. Validate"| D["TrustedHeaderAuthFilter<br/>from gateway-core"]

    D -->|"3a. Invalid"| E["401 Unauthorized"]

    D -->|"3b. Valid"| F["Inject Headers"]

    F -->|"X-Auth-User-Id: uuid<br/>X-Auth-Tenant-Ids: t1,t2<br/>X-Auth-Roles: ADMIN<br/>X-Auth-Validated: hmac"| G["Request to Backend"]

    D -->|"4. Check Rate Limit"| H["RateLimitingFilter<br/>from gateway-core"]

    H -->|"4a. Exceeded"| I["429 Too Many Requests"]

    H -->|"4b. OK"| J["Log Request<br/>AuditLoggingFilter"]

    J -->|"5. Route to Backend"| K["Admin Service<br/>(patient-service, etc)"]

    K -->|"6. Response"| B

    B -->|"7. Response"| A

    style B fill:#e8f5e9
    style D fill:#c8e6c9
    style H fill:#c8e6c9
    style J fill:#c8e6c9
```

**Sequence**:
1. Admin client sends request with JWT
2. Admin gateway extracts and validates JWT
3. If valid, injects X-Auth-* headers
4. If invalid, returns 401
5. Checks rate limit (per tenant)
6. If exceeded, returns 429
7. Logs request for audit trail
8. Routes to appropriate backend service
9. Returns response

---

## Domain-Specific Optimizations

```mermaid
graph LR
    A["gateway-admin-service<br/>8002"] --> B["Admin Specializations"]
    B --> B1["Strict Auth<br/>(MFA encouraged)"]
    B --> B2["Lower RPS<br/>(100 req/sec)"]
    B --> B3["High Latency OK<br/>(500ms acceptable)"]

    C["gateway-clinical-service<br/>8003"] --> D["Clinical Specializations"]
    D --> D1["Fast Auth<br/>(Optimized)"]
    D --> D2["High RPS<br/>(2000+ req/sec)"]
    D --> D3["Low Latency<br/>(<200ms p99)"]

    E["gateway-fhir-service<br/>8004"] --> F["FHIR Specializations"]
    F --> F1["HL7 Compliance<br/>Check"]
    F --> F2["FHIR Validation"]
    F --> F3["Standard RPS<br/>(500 req/sec)"]

    style B1 fill:#ffcdd2
    style D1 fill:#c8e6c9
    style F1 fill:#b3e5fc
```

**Specialization Benefits**:
- **Admin**: Stricter security (MFA), lower throughput
- **Clinical**: High throughput optimization, fast response times
- **FHIR**: HL7 standards compliance, FHIR-specific validation

---

## Authentication Flow: Gateway-Trust Pattern

```mermaid
graph TB
    A["Client"] -->|"1. POST /login<br/>username + password"| B["gateway-*-service"]

    B -->|"2. Validate credentials"| C["User Database"]

    C -->|"3. Credentials OK"| D["Generate JWT"]

    D -->|"4. Return JWT"| B

    B -->|"5. JWT"| A

    A -->|"6. GET /api/resource<br/>Authorization: Bearer JWT"| B

    B -->|"7. Validate JWT<br/>Extract claims"| E["TrustedHeaderAuthFilter"]

    E -->|"8a. Invalid"| F["401 Unauthorized"]

    E -->|"8b. Valid<br/>Create signature"| G["X-Auth-User-Id<br/>X-Auth-Tenant-Ids<br/>X-Auth-Roles<br/>X-Auth-Validated=HMAC"]

    G -->|"9. Route with headers"| H["Backend Service"]

    H -->|"10. Trust headers<br/>(no re-validation)"| I["Process Request"]

    I -->|"11. Response"| H

    H -->|"12. Response"| B

    B -->|"13. Response"| A

    style E fill:#c8e6c9
    style G fill:#b3e5fc
    style H fill:#e8f5e9
```

**Key Points**:
1. Gateway validates JWT once
2. Gateway injects X-Auth-* headers (+ HMAC signature)
3. Backend services trust headers (no re-validation)
4. HMAC signature prevents header spoofing
5. Reduces per-service auth overhead

---

## Traffic Patterns and Independent Scaling

```mermaid
graph TB
    A["Load Balancer"] -->|"Incoming Traffic"| B["Distribution"]

    B -->|"80% (Patient/Measure)"| C["gateway-clinical-service<br/>8003"]
    B -->|"15% (Config)"| D["gateway-admin-service<br/>8002"]
    B -->|"4% (EHR Integration)"| E["gateway-fhir-service<br/>8004"]
    B -->|"1% (Legacy)"| F["gateway-service<br/>8001"]

    C -->|"High Traffic<br/>Scale UP"| C1["3x instances<br/>Optimized CPU"]
    D -->|"Moderate Traffic<br/>Scale 1x"| D1["1x instance"]
    E -->|"Low Traffic<br/>Scale 1x"| E1["1x instance"]
    F -->|"Legacy<br/>Scale 1x"| F1["1x instance"]

    style C fill:#fff9c4
    style C1 fill:#fff9c4
    style D fill:#c8e6c9
    style E fill:#b3e5fc
```

**Independent Scaling Benefits**:
- **Clinical**: Experiences traffic spike → scale to 3x instances
- **Admin**: Remains at 1x (no unnecessary scaling)
- **FHIR**: Independent of clinical traffic
- **Cost savings**: Scale only what's needed

---

## Error Handling and Circuit Breaker

```mermaid
graph TB
    A["Client Request"] -->|"Route"| B["gateway-*-service"]

    B -->|"Call Backend"| C{Backend<br/>Healthy?}

    C -->|"YES"| D["Forward Request"]
    D -->|"Response"| B

    C -->|"NO (failures > threshold)"| E["Circuit Breaker<br/>OPEN"]
    E -->|"Fail Fast<br/>503 Service Unavailable"| B

    E -->|"After timeout"| F["Try again<br/>Circuit HALF-OPEN"]
    F -->|"SUCCESS"| G["Reset<br/>Circuit CLOSED"]
    F -->|"FAILURE"| E

    B -->|"Response"| A

    style E fill:#ffcdd2
    style G fill:#c8e6c9
```

**Resilience Pattern**:
1. Gateway detects backend failures
2. Opens circuit (stops sending requests)
3. Returns 503 (fail fast, don't timeout)
4. After timeout, tries again (HALF-OPEN)
5. If backend recovered, closes circuit
6. If still failing, reopens circuit

---

## Integration Points

```mermaid
graph TB
    A["4 Gateway Services"] --> B["Common Integration Points"]

    B --> B1["OpenTelemetry<br/>(Distributed Tracing)"]
    B --> B2["Prometheus Metrics<br/>(Performance Monitoring)"]
    B --> B3["Spring Security<br/>(Authorization)"]
    B --> B4["Redis Cache<br/>(Rate Limit Tracking)"]

    B1 --> C["Jaeger UI<br/>Trace Visualization"]
    B2 --> D["Prometheus<br/>Scrapes metrics"]
    D --> E["Grafana Dashboards<br/>Performance visualization"]
    B4 --> F["Real-time Rate<br/>Limit Status"]

    style B fill:#fff3e0
    style C fill:#b3e5fc
    style E fill:#b3e5fc
```

---

## Code Organization

```
gateway-core/  (Shared)
├── filters/
│   ├── TrustedHeaderAuthFilter.java
│   ├── RateLimitingFilter.java
│   ├── AuditLoggingFilter.java
│   └── CorsFilter.java
├── config/
│   └── GatewayCoreConfiguration.java
└── pom.xml

gateway-admin-service/
├── config/
│   ├── AdminSecurityConfig.java (Strict auth)
│   └── AdminRateLimitConfig.java
└── pom.xml → depends on gateway-core

gateway-clinical-service/
├── config/
│   ├── ClinicalSecurityConfig.java (Fast auth)
│   └── ClinicalRateLimitConfig.java (High throughput)
└── pom.xml → depends on gateway-core

gateway-fhir-service/
├── config/
│   ├── FhirSecurityConfig.java
│   └── FhirValidationConfig.java
└── pom.xml → depends on gateway-core

gateway-service/
├── config/
│   └── GeneralGatewayConfig.java (Legacy/fallback)
└── pom.xml → depends on gateway-core
```

---

## References

- **[Gateway Architecture Guide](../GATEWAY_ARCHITECTURE.md)** - Implementation details
- **[ADR-002: Gateway Modularization](../decisions/ADR-002-gateway-modularization.md)** - Decision rationale
- **[Gateway Trust Architecture](../../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)** - Auth pattern details
- **[Service Catalog](../../services/SERVICE_CATALOG.md)** - Gateway services info

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Diagrams for 4-Gateway Modularization (January 2026)_

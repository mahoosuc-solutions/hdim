# ADR 0003: Frontend Direct Service Access in Development

**Date:** January 22, 2026
**Status:** Accepted ✅
**Deciders:** HDIM Development Team
**Technical Context:** Gateway Architecture, Frontend Configuration, Developer Experience

---

## Context

HDIM uses a modularized multi-gateway architecture with specialized gateways (admin-8002, clinical-8003, fhir-8004) that route requests to backend microservices. The frontend (Angular Clinical Portal) can either:

1. **Route through API Gateway**: All requests go through gateway-clinical-service (port 8003)
2. **Connect directly to services**: Frontend connects to backend services without gateway (CQL Engine-8081, Quality Measure-8087, FHIR-8085)

### Problem

During local development, developers encountered:

- **Gateway complexity**: Starting 4 specialized gateway services adds overhead
- **Authentication complexity**: JWT validation required for all requests through gateway
- **Debugging difficulty**: Request tracing through gateway adds layers
- **Build time**: Rebuilding gateway services for every change slows iteration
- **Port conflicts**: Managing 4 gateway ports (8001-8004) vs 5 backend service ports

### Discovery Process

1. Frontend was configured with `useApiGateway: false` in `environment.ts`
2. Gateway services were not included in `docker-compose.yml` for development
3. Backend services (CQL Engine, FHIR, Quality Measure, Care Gap, Patient) were directly accessible
4. CORS was properly configured on backend services for `localhost:4200` origin

---

## Decision

**For development environment ONLY**, the frontend will connect directly to backend services, bypassing the gateway layer.

### Configuration

**File:** `apps/clinical-portal/src/environments/environment.ts`

```typescript
apiConfig: {
  useApiGateway: false,  // ✅ Direct service access

  // Direct service URLs
  cqlEngineUrl: 'http://localhost:8081/cql-engine',
  qualityMeasureUrl: 'http://localhost:8087/quality-measure',
  fhirServerUrl: 'http://localhost:8085/fhir',

  // Gateway URL (unused in development)
  apiGatewayUrl: 'http://localhost:8080',

  defaultTenantId: 'DEMO001',
  timeoutMs: 30000,
  retryAttempts: 3,
}
```

**File:** `apps/clinical-portal/src/environments/environment.prod.ts`

```typescript
apiConfig: {
  useApiGateway: true,  // ✅ Gateway required in production
  apiGatewayUrl: 'https://gateway.healthdata-in-motion.com',
  // ... other config
}
```

---

## Consequences

### Positive

✅ **Faster Development Cycle**
- No need to rebuild gateway services during feature development
- Changes to backend services immediately available to frontend

✅ **Simplified Local Setup**
- Developers only need to run 5 backend services (not 4 gateways + 5 services)
- Fewer port conflicts to manage

✅ **Easier Debugging**
- Direct request tracing from frontend to backend
- No intermediate gateway layer to debug through
- Browser DevTools shows actual service responses

✅ **Reduced Docker Resource Usage**
- 5 containers instead of 9 containers in development
- Lower memory footprint on developer machines

✅ **CORS Properly Configured**
- Backend services already configured for `localhost:4200` origin
- No additional CORS configuration needed

### Negative

⚠️ **Authentication Differences**
- Development: Services accept requests without JWT tokens (trusts headers)
- Production: Gateway validates JWT tokens before forwarding

⚠️ **Rate Limiting Not Tested**
- Gateway rate limiting (10 login attempts/min) bypassed in development
- Rate limit logic only tested in production-like environments

⚠️ **Security Differences**
- Development: No JWT validation, no gateway-level security filters
- Production: Full JWT validation, RBAC, tenant isolation via gateway

⚠️ **Routing Differences**
- Development: Frontend hardcodes service URLs
- Production: Gateway handles service discovery and routing

⚠️ **Request Transformation Bypassed**
- Gateway request/response transformations not active in development
- Could lead to unexpected differences between dev and prod behavior

---

## Mitigation Strategies

### 1. Integration Testing with Gateway

**Action:** Create integration tests that use gateway-clinical-service

```bash
# Run integration tests with gateway
npm run test:integration:gateway
```

**Status:** ⏳ TODO - Create integration test suite

---

### 2. Production-like Environment

**Action:** Docker Compose profile for production-like setup

```bash
# Start with gateway (production-like)
docker compose --profile gateway-mode up -d gateway-clinical-service

# Frontend uses gateway
HDIM_USE_GATEWAY=true npm start
```

**Status:** ⏳ TODO - Add `gateway-mode` profile to docker-compose.yml

---

### 3. Pre-Production Testing

**Action:** Manual testing checklist before production deployment

```markdown
## Pre-Production Gateway Testing Checklist

- [ ] Login flow through gateway-clinical-service
- [ ] JWT token generation and validation
- [ ] Rate limiting (login attempts, API calls)
- [ ] RBAC enforcement (role-based access)
- [ ] Multi-tenant isolation
- [ ] Request routing to all backend services
- [ ] Error responses (401, 403, 429)
- [ ] CORS for production origins
```

**Status:** ⏳ TODO - Add to deployment runbook

---

### 4. CI/CD Gateway Tests

**Action:** GitHub Actions workflow tests gateway integration

```yaml
name: Gateway Integration Tests

on:
  pull_request:
    paths:
      - 'apps/clinical-portal/**'
      - 'backend/modules/services/gateway-clinical-service/**'

jobs:
  test-gateway-integration:
    runs-on: ubuntu-latest
    steps:
      - name: Start gateway and services
        run: docker compose --profile gateway-mode up -d

      - name: Run Playwright tests with gateway
        run: npm run test:e2e:gateway
```

**Status:** ⏳ TODO - Create GitHub Actions workflow

---

## Alternatives Considered

### Alternative 1: Always Use Gateway

**Approach:** Frontend always connects through gateway-clinical-service, even in development

**Pros:**
- Development environment matches production
- Gateway security, rate limiting, routing tested continuously
- No risk of dev/prod behavior differences

**Cons:**
- Slower development iteration (rebuild gateway for every change)
- More complex local setup (4 gateways + 5 services)
- Harder to debug (requests go through gateway layer)
- Higher resource usage on developer machines

**Decision:** ❌ Rejected - Developer experience too slow

---

### Alternative 2: Mock Gateway in Frontend

**Approach:** Frontend includes mock gateway layer that simulates routing/auth

**Pros:**
- No backend gateway services needed
- Fast development iteration
- Controlled testing of auth/routing logic

**Cons:**
- Mock logic must be maintained in sync with real gateway
- Risk of mock diverging from real gateway behavior
- Additional frontend code complexity
- Mock doesn't test actual gateway implementation

**Decision:** ❌ Rejected - Mock maintenance overhead too high

---

### Alternative 3: Dynamic Gateway Toggle

**Approach:** Allow developers to toggle gateway on/off via environment variable

**Example:**
```bash
# Without gateway (fast iteration)
HDIM_USE_GATEWAY=false npm start

# With gateway (production-like testing)
HDIM_USE_GATEWAY=true npm start
```

**Pros:**
- Flexibility: Developers choose based on task
- Can test gateway behavior when needed
- Fast iteration when gateway not needed

**Cons:**
- Requires conditional logic in frontend
- Two different code paths to maintain
- Risk of accidentally shipping wrong configuration

**Decision:** ✅ Partially Adopted - Use environment-specific configs (environment.ts vs environment.prod.ts), not runtime toggles

---

## Implementation Status

### Completed ✅

- [x] Frontend configured with `useApiGateway: false` in development
- [x] Backend services expose CORS for `localhost:4200`
- [x] Docker Compose includes only backend services (not gateways)
- [x] Environment-specific configuration (environment.ts vs environment.prod.ts)

### Pending ⏳

- [ ] Integration test suite using gateway-clinical-service
- [ ] Docker Compose `gateway-mode` profile for production-like testing
- [ ] Pre-production testing checklist in deployment runbook
- [ ] CI/CD GitHub Actions workflow for gateway integration tests
- [ ] Documentation: "When to test with gateway" guide for developers

---

## Related Documentation

- [Gateway Architecture Guide](../GATEWAY_ARCHITECTURE.md) - Complete gateway design
- [Frontend Environment Configuration](../../apps/clinical-portal/src/environments/README.md) - Environment files
- [CORS Configuration Guide](../CORS_CONFIGURATION.md) - CORS setup for services
- [Development Workflow](../../backend/docs/DEVELOPMENT_WORKFLOW.md) - Local development setup

---

## Review Notes

**Decision Date:** January 22, 2026
**Next Review:** March 2026 (after first production deployment)

**Review Questions:**
1. Has this decision caused any production issues due to dev/prod differences?
2. Are integration tests with gateway sufficient to catch issues?
3. Should we require gateway testing for certain types of PRs (auth, security)?
4. Has developer experience improved as expected?

---

_This ADR documents the decision to bypass the API gateway in development for improved developer experience, while maintaining gateway usage in production for security and routing._

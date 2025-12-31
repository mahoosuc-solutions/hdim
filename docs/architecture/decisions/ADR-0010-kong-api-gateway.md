# ADR-0010: Kong as API Gateway

**Date**: 2024-Q3
**Status**: Accepted
**Deciders**: Architecture Team, Platform Engineering Team
**Technical Story**: Need API gateway for routing, authentication, and rate limiting

---

## Context and Problem Statement

HDIM's 28 microservices require an API gateway to:

- Route external requests to appropriate services
- Validate JWT tokens before requests reach services
- Inject trusted authentication headers (Gateway Trust model)
- Rate limit requests to prevent abuse
- Provide SSL/TLS termination
- Enable observability (logging, metrics)
- Support multi-tenant request routing

The gateway is critical for the Gateway Trust Authentication model where backend services trust headers injected by the gateway rather than re-validating JWT.

---

## Decision Drivers

* **Security** - JWT validation, header injection, rate limiting
* **Performance** - Low latency for high-throughput scenarios
* **Plugin ecosystem** - Extensibility for custom authentication logic
* **Kubernetes native** - Ingress controller support for production
* **Open source** - Avoid vendor lock-in
* **Observability** - Logging, metrics, distributed tracing
* **Healthcare adoption** - Proven in healthcare environments

---

## Considered Options

1. **Kong Gateway** - Open source API gateway
2. **AWS API Gateway** - AWS managed service
3. **NGINX Plus** - Commercial web server/proxy
4. **Traefik** - Cloud-native edge router
5. **Spring Cloud Gateway** - Java-based gateway

---

## Decision Outcome

**Chosen option**: "Kong Gateway"

**Rationale**: Kong provides:
- Open source with enterprise upgrade path
- Excellent plugin ecosystem for JWT, rate limiting, logging
- Kubernetes Ingress Controller for production
- Custom plugin support for Gateway Trust header injection
- Proven at scale in healthcare organizations
- Active community and commercial support options

---

## Consequences

### Positive

* **Security**: Built-in JWT validation, rate limiting, IP filtering
* **Extensibility**: Plugin architecture for custom logic (Gateway Trust headers)
* **Kubernetes**: Native Ingress Controller for production
* **Performance**: High throughput with low latency (<10ms overhead)
* **Observability**: Prometheus metrics, logging plugins
* **Flexibility**: DB-less mode for simple deployments, PostgreSQL for HA

### Negative

* **Complexity**: Configuration learning curve
* **Operational overhead**: Requires management of Kong instances
* **Lua knowledge**: Custom plugins require Lua programming

**Mitigations**:
- Use Kong Manager UI for configuration visibility
- Document plugin configurations
- Use pre-built plugins where possible

### Neutral

* Can run in DB-less mode (Docker Compose) or with PostgreSQL (production)
* Supports both traditional and Kubernetes deployments

---

## Pros and Cons of Options

### Option 1: Kong Gateway

Open source API gateway from Kong Inc.

| Criterion | Assessment |
|-----------|------------|
| JWT Validation | **Good** - Built-in JWT plugin |
| Rate Limiting | **Good** - Multiple rate limiting plugins |
| Custom Plugins | **Good** - Lua-based plugin system |
| Kubernetes | **Good** - Native Ingress Controller |
| Performance | **Good** - <10ms latency overhead |
| Open Source | **Good** - Apache 2.0 license |
| Healthcare Adoption | **Good** - Used by healthcare organizations |

**Summary**: Best balance of features, extensibility, and open source.

---

### Option 2: AWS API Gateway

AWS managed API gateway service.

| Criterion | Assessment |
|-----------|------------|
| JWT Validation | **Good** - Cognito/Lambda authorizers |
| Rate Limiting | **Good** - Built-in throttling |
| Custom Logic | **Neutral** - Lambda required for custom auth |
| Kubernetes | **Bad** - Not native to Kubernetes |
| Performance | **Neutral** - Additional latency for Lambda |
| Vendor Lock-in | **Bad** - AWS-specific |

**Summary**: Good for AWS-only but creates vendor dependency.

---

### Option 3: NGINX Plus

Commercial web server and reverse proxy.

| Criterion | Assessment |
|-----------|------------|
| JWT Validation | **Neutral** - Requires NGINX Plus (commercial) |
| Rate Limiting | **Good** - Built-in rate limiting |
| Custom Plugins | **Neutral** - Lua (OpenResty) or commercial modules |
| Kubernetes | **Good** - NGINX Ingress Controller |
| Performance | **Good** - Excellent performance |
| Cost | **Bad** - Commercial license required for advanced features |

**Summary**: Capable but commercial licensing for JWT validation.

---

### Option 4: Traefik

Cloud-native edge router.

| Criterion | Assessment |
|-----------|------------|
| JWT Validation | **Neutral** - Requires middleware configuration |
| Rate Limiting | **Good** - Built-in rate limiting |
| Kubernetes | **Good** - Native Kubernetes integration |
| Custom Plugins | **Neutral** - Go-based middleware |
| Performance | **Good** - Low latency |
| Configuration | **Neutral** - Different paradigm than Kong |

**Summary**: Good cloud-native option but less plugin ecosystem.

---

### Option 5: Spring Cloud Gateway

Java-based API gateway.

| Criterion | Assessment |
|-----------|------------|
| JWT Validation | **Good** - Spring Security integration |
| Rate Limiting | **Good** - Redis-based rate limiting |
| Java Ecosystem | **Good** - Same stack as services |
| Performance | **Neutral** - JVM overhead |
| Kubernetes | **Neutral** - Requires custom deployment |
| Observability | **Good** - Spring Boot Actuator |

**Summary**: Good Java integration but higher resource usage.

---

## Implementation Notes

### Version Selected

**Kong 3.4.x** - Latest stable release

### Deployment Model

- **Development**: Docker Compose, DB-less mode (port 8000)
- **Production**: Kubernetes with PostgreSQL backend

### Gateway Trust Authentication Flow

```
1. Client sends request with JWT
   │
   ▼
2. Kong validates JWT (jwt plugin)
   │
   ▼
3. Kong injects X-Auth-* headers (custom plugin)
   - X-Auth-User-Id
   - X-Auth-Username
   - X-Auth-Tenant-Ids
   - X-Auth-Roles
   - X-Auth-Validated (HMAC signature)
   │
   ▼
4. Kong routes to backend service
   │
   ▼
5. Backend trusts X-Auth-* headers (no JWT re-validation)
```

### Kong Configuration (DB-less)

```yaml
# kong.yml
_format_version: "3.0"

services:
  - name: quality-measure-service
    url: http://quality-measure-service:8087
    routes:
      - name: quality-measure-route
        paths:
          - /quality-measure
        strip_path: true
    plugins:
      - name: jwt
        config:
          secret_is_base64: false
          claims_to_verify:
            - exp
      - name: rate-limiting
        config:
          minute: 100
          policy: local
      - name: gateway-auth-headers
        config:
          signing_secret: ${GATEWAY_AUTH_SIGNING_SECRET}

  - name: fhir-service
    url: http://fhir-service:8085
    routes:
      - name: fhir-route
        paths:
          - /fhir
        strip_path: false

  - name: care-gap-service
    url: http://care-gap-service:8086
    routes:
      - name: care-gap-route
        paths:
          - /care-gap
        strip_path: true

# Global plugins
plugins:
  - name: prometheus
  - name: correlation-id
    config:
      header_name: X-Correlation-ID
      generator: uuid
```

### Custom Plugin: Gateway Auth Headers

```lua
-- plugins/gateway-auth-headers/handler.lua
local GatewayAuthHeaders = {
  PRIORITY = 900,
  VERSION = "1.0.0",
}

function GatewayAuthHeaders:access(conf)
  local jwt_claims = kong.ctx.shared.authenticated_jwt_claims

  if jwt_claims then
    -- Inject authenticated user headers
    kong.service.request.set_header("X-Auth-User-Id", jwt_claims.sub)
    kong.service.request.set_header("X-Auth-Username", jwt_claims.username or "")
    kong.service.request.set_header("X-Auth-Tenant-Ids", jwt_claims.tenant_ids or "")
    kong.service.request.set_header("X-Auth-Roles", table.concat(jwt_claims.roles or {}, ","))

    -- HMAC signature for validation
    local payload = jwt_claims.sub .. ":" .. (jwt_claims.tenant_ids or "")
    local signature = kong.hmac.sha256(conf.signing_secret, payload)
    kong.service.request.set_header("X-Auth-Validated", ngx.encode_base64(signature))
  end
end

return GatewayAuthHeaders
```

### Docker Compose Configuration

```yaml
# docker-compose.yml
kong:
  image: kong:3.4
  environment:
    KONG_DATABASE: "off"
    KONG_DECLARATIVE_CONFIG: /kong/kong.yml
    KONG_PROXY_ACCESS_LOG: /dev/stdout
    KONG_ADMIN_ACCESS_LOG: /dev/stdout
    KONG_PROXY_ERROR_LOG: /dev/stderr
    KONG_ADMIN_ERROR_LOG: /dev/stderr
    KONG_ADMIN_LISTEN: 0.0.0.0:8001
    KONG_PROXY_LISTEN: 0.0.0.0:8000
  ports:
    - "8000:8000"  # Proxy
    - "8001:8001"  # Admin API
  volumes:
    - ./kong/kong.yml:/kong/kong.yml:ro
  healthcheck:
    test: ["CMD", "kong", "health"]
    interval: 10s
    timeout: 5s
    retries: 5
```

### Performance Targets

| Metric | Target | Actual (Dec 2024) |
|--------|--------|-------------------|
| Latency Overhead (p95) | <20ms | 8ms |
| Throughput | 10,000 req/sec | 12,500 req/sec |
| JWT Validation Time | <5ms | 3ms |
| Error Rate | <0.01% | 0.005% |

### Monitoring

```yaml
# Prometheus metrics endpoint
# http://kong:8001/metrics

# Key metrics
- kong_http_requests_total
- kong_request_latency_ms
- kong_upstream_latency_ms
- kong_bandwidth_bytes
- kong_nginx_connections_total
```

### Rate Limiting Configuration

| Endpoint Pattern | Rate Limit | Window | Rationale |
|------------------|------------|--------|-----------|
| /api/v1/patients/* | 100/min | Per consumer | Normal API usage |
| /api/v1/evaluations/* | 20/min | Per consumer | Expensive operations |
| /fhir/* | 500/min | Per consumer | FHIR queries |
| /actuator/* | 10/min | Per IP | Health checks |

---

## Links

* [Kong Documentation](https://docs.konghq.com/)
* [Kong Plugin Development](https://docs.konghq.com/gateway/latest/plugin-development/)
* [Gateway Trust Architecture](/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)
* [Kong Configuration](/docker/kong/)
* Related: [ADR-0009 - Spring Boot Framework](ADR-0009-spring-boot-framework.md)

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-Q3 | Architecture Team | Initial decision |
| 1.1 | 2024-12-30 | Architecture Team | Added Gateway Trust flow, plugin example |

---

*This ADR follows the template in `/docs/templates/ADR_TEMPLATE.md`*

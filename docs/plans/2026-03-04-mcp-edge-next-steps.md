# MCP Edge — Next Steps

**Date:** 2026-03-04
**Current version:** v0.1.0 (pushed to master)
**Status:** Release-ready, 1,307 tests, 99.35% coverage

---

## What Shipped (v0.1.0)

- 3 sidecars: platform (3100), devops (3200), clinical (3300)
- 3 clinical strategies: composite (25), high-value (15), full-surface (68)
- 123 MCP tools total across all sidecars
- RBAC exhaustive matrix (352+ test cases, 7 roles)
- PHI leak detection, cross-sidecar isolation, protocol contract tests
- Demo mode with per-tool fixtures
- stdio bridges for Claude Desktop/Code
- Structured pino logging, ESLint no-console, jest coverage thresholds
- CI/CD workflow for all 4 packages
- 61 commits, 3 audit passes, zero open issues

---

## v0.2.0 Roadmap

### Priority 1: PHI Audit Integration

Wire `phi-audit.js` into the MCP router so every clinical tool call logs structured PHI access events (PHI_ACCESS, PHI_WRITE, AUTH_DENIED). The module exists and is tested — needs router integration.

- Modify `mcp-edge-common/lib/mcp-router.js` to accept a `phiAuditLogger` option
- Clinical server.js passes `createPhiAuditLogger()` to the router
- Each tool definition declares `phi: true/false` and `write: true/false`
- Router logs PHI events with patientId, tenantId, role, tool, duration

### Priority 2: Live Gateway Integration Testing

Current tests use demo mode fixtures. Add integration tests that hit the real HDIM gateway.

- Create `__tests__/live/` directory with gateway-dependent tests
- Use `HDIM_BASE_URL` to target running gateway
- Test FHIR read/search against seeded demo data
- Skip in CI unless `LIVE_TESTS=true` env var is set

### Priority 3: Strategy Hot-Swap

Allow changing clinical tool strategy without restart via MCP admin command.

- Add `admin/set_strategy` tool (platform_admin only)
- Re-load tools and role policies dynamically
- Emit `tools/list_changed` notification per MCP spec

### Priority 4: Metrics & Observability

- Expose Prometheus metrics endpoint (`/metrics`)
- Track: tool call count, latency histogram, error rate, RBAC deny rate
- Add OpenTelemetry trace context propagation through gateway calls

### Priority 5: Production Hardening

- TLS termination (or document reverse proxy pattern)
- API key rotation mechanism
- Per-tenant rate limiting (currently global)
- Connection pooling for gateway HTTP client
- Circuit breaker for gateway calls (currently fails open)

---

## Backlog (v0.3.0+)

- **Streaming responses** — MCP spec supports streaming for large FHIR bundles
- **WebSocket transport** — Alternative to HTTP for persistent connections
- **Tool versioning** — Support multiple tool versions simultaneously
- **Multi-gateway** — Route to different gateway instances per tenant
- **Audit dashboard** — Query PHI audit logs via MCP tool
- **Custom strategy builder** — Compose strategies from tool subsets via config

---

## Quick Reference

```bash
# Run all MCP edge tests
npm run test:mcp-edge

# Run clinical with specific strategy
CLINICAL_TOOL_STRATEGY=high-value npm start --prefix mcp-edge-clinical

# Docker
docker compose -f docker-compose.mcp-edge.yml up -d

# Lint all packages
cd mcp-edge-common && npm run lint && cd ../mcp-edge-clinical && npm run lint && cd ../mcp-edge-platform && npm run lint && cd ../mcp-edge-devops && npm run lint
```

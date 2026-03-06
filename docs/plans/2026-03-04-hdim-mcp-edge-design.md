# HDIM MCP Edge — Sidecar Architecture Design

> **Status:** COMPLETE

**Date:** 2026-03-04
**Status:** Approved
**Author:** Claude Code + Aaron
**Approach:** C — Sidecar Per Service Group (CISO-required isolation boundaries)

## Overview

Standalone MCP edge servers that expose HDIM platform capabilities to Claude Desktop,
VS Code, and other MCP clients. Modeled after the west-bethel-motel-booking-system
MCP edge pattern: HTTP Express servers with stdio bridges for desktop integration.

The goal is to enable integrations supporting actions from the C-suite to the MA
working the ER to intake a patient — layered incrementally.

## Sidecar Topology

```
Claude Desktop / VS Code
    |
    +-- stdio --> hdim-platform-bridge.mjs --> hdim-platform-edge  :3100
    |                                           Gateway proxy (18080)
    |                                           PHI: aggregate only
    |
    +-- stdio --> hdim-devops-bridge.mjs   --> hdim-devops-edge    :3200
    |                                           Docker CLI + NX
    |                                           PHI: none
    |
    +-- stdio --> hdim-clinical-bridge.mjs --> hdim-clinical-edge  :3300
                                                Gateway proxy (clinical)
                                                PHI: full (Layer 2)
```

### Security Boundaries

| Sidecar | Port | PHI Access | Network Access | Docker Socket |
|---------|------|------------|----------------|---------------|
| platform-edge | 3100 | Aggregate only | Gateway (18080) | No |
| devops-edge | 3200 | None | Docker socket, localhost | Yes (read-only) |
| clinical-edge | 3300 | Full | Gateway (18080) | No |

## Layers

- **Layer 0+1 (this build):** platform-edge + devops-edge + bridges + docker compose
- **Layer 2:** clinical-edge (MA intake, vitals, care gaps, CDS)
- **Layer 3:** executive tools (dashboards, KPIs, provider performance, QRDA)
- **Layer 4:** HIPAA hardening (signed JWT tokens, full PHI audit logging)

## Directory Structure

```
hdim-master/
  mcp-edge-common/             # Shared library (npm workspace)
    package.json
    lib/
      jsonrpc.js               # JSON-RPC helpers
      health.js                # Express health endpoint factory
      mcp-router.js            # Base MCP router (initialize, tools/list, tools/call)
      auth.js                  # API key validation, role extraction
      demo-mode.js             # --demo flag, synthetic response loader
    __tests__/

  mcp-edge-platform/           # Sidecar 1: Platform tools
    package.json
    Dockerfile
    index.js
    server.js
    lib/
      tools/                   # One file per tool
        edge-health.js
        platform-health.js
        fhir-metadata.js
        service-catalog.js
        dashboard-stats.js
        demo-seed.js
        demo-status.js
        platform-info.js
      platform-client.js       # HTTP client for gateway
    fixtures/                  # Demo mode synthetic responses
    __tests__/

  mcp-edge-devops/             # Sidecar 2: DevOps tools
    package.json
    Dockerfile
    index.js
    server.js
    lib/
      tools/
        edge-health.js
        docker-status.js
        docker-logs.js
        docker-restart.js
        service-dependencies.js
        compose-config.js
        build-status.js
      docker-client.js         # Docker CLI wrapper
    fixtures/
    __tests__/

  scripts/mcp/
    mcp-edge-platform-bridge.mjs
    mcp-edge-devops-bridge.mjs

  docker-compose.mcp-edge.yml
```

## Tool Surface

### Platform Edge (port 3100)

| Tool | Description | Gateway Route |
|------|-------------|---------------|
| edge_health | Local edge status | None |
| platform_health | Gateway actuator health | GET /actuator/health |
| fhir_metadata | FHIR capability statement | GET /fhir/metadata |
| service_catalog | All services with status | GET /actuator/health per service |
| dashboard_stats | Executive KPIs (aggregate) | GET /analytics/dashboards |
| demo_status | Demo seeding state | GET /api/v1/demo/status |
| demo_seed | Trigger scenario loading | POST /api/v1/demo/scenarios/{name} |
| platform_info | Base URLs, version | None |

### DevOps Edge (port 3200)

| Tool | Description | Mechanism |
|------|-------------|-----------|
| edge_health | Local edge status | None |
| docker_status | Compose service states | docker compose ps --format json |
| docker_logs | Tail service logs | docker compose logs --tail N |
| docker_restart | Restart a service | docker compose restart |
| service_dependencies | Dependency graph | Static map |
| compose_config | Env var validation | docker compose config |
| build_status | NX/Gradle state | nx show projects --affected |

## Auth Model

### Current (Layer 0+1): API Keys

- MCP_EDGE_API_KEY env var on bridge, forwarded as Authorization: Bearer
- Platform edge forwards key to gateway for tenant/role resolution
- DevOps edge validates key locally (SHA-256 hash in .env)
- Operator role via x-operator-role header for tool filtering
- Role policies use regex matching (same as west-bethel tool-authz.js)

### Future (Layer 4): Signed JWT Tokens

- Replace API keys with signed JWT from gateway
- PHI access audit logging per MCP tool invocation
- Tenant-scoped tool isolation
- Token refresh via edge server

## Role-Based Tool Filtering

| Role | Platform Edge | DevOps Edge |
|------|--------------|-------------|
| platform_admin | All tools | All tools |
| developer | All tools | All tools |
| clinical_admin | edge_health, platform_health, dashboard_stats | None |
| quality_officer | edge_health, dashboard_stats | None |
| executive | edge_health, dashboard_stats, platform_info | None |
| clinician | edge_health, platform_health | None |
| care_coordinator | edge_health | None |

## Demo Mode

When started with --demo flag or HDIM_DEMO_MODE=true:
- Tool calls return synthetic JSON from fixtures/ directory
- No gateway connectivity required
- Useful for demos, testing, CI/CD validation
- Health endpoints still report real edge status

## Docker Compose

```yaml
services:
  mcp-edge-platform:
    build: ./mcp-edge-platform
    ports: ["3100:3100"]
    env_file: ./mcp-edge-platform/.env
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "node", "-e", "fetch('http://127.0.0.1:3100/health')..."]

  mcp-edge-devops:
    build: ./mcp-edge-devops
    ports: ["3200:3200"]
    env_file: ./mcp-edge-devops/.env
    restart: unless-stopped
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
    healthcheck:
      test: ["CMD", "node", "-e", "fetch('http://127.0.0.1:3200/health')..."]
```

## MCP Client Configuration

### .mcp.json
```json
{
  "mcpServers": {
    "hdim-platform-edge": {
      "type": "stdio",
      "command": "node",
      "args": ["scripts/mcp/mcp-edge-platform-bridge.mjs"],
      "env": {
        "MCP_EDGE_URL": "http://localhost:3100/mcp",
        "MCP_EDGE_OPERATOR_ROLE": "platform_admin"
      }
    },
    "hdim-devops-edge": {
      "type": "stdio",
      "command": "node",
      "args": ["scripts/mcp/mcp-edge-devops-bridge.mjs"],
      "env": {
        "MCP_EDGE_URL": "http://localhost:3200/mcp",
        "MCP_EDGE_OPERATOR_ROLE": "platform_admin"
      }
    }
  }
}
```

## Absorbed Existing MCP Scripts

The existing stdio-only MCPs are absorbed into the sidecars:
- hdim-platform-mcp.mjs logic → mcp-edge-platform tools
- hdim-docker-mcp.mjs logic → mcp-edge-devops tools
- nx-mcp.mjs logic → mcp-edge-devops build-status tool

The original scripts can remain as lightweight fallbacks but are not the primary interface.

## Testing Strategy

TDD with tests written before implementation:
- Unit tests per tool (input validation, response format, error handling)
- Unit tests for shared lib (jsonrpc, auth, demo-mode, health)
- Integration tests per sidecar (full JSON-RPC round-trip)
- Bridge tests (stdio parsing, tool name mapping)
- Docker compose smoke tests (health endpoints respond)

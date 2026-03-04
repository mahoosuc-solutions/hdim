# hdim-mcp-edge-platform

MCP edge sidecar exposing HDIM platform tools (health, FHIR metadata, service catalog, dashboard, demo) on port 3100.

## Tools (8)

| Tool | Description |
|------|-------------|
| `edge_health` | Edge sidecar health status |
| `platform_health` | Gateway health check |
| `platform_info` | Platform version and configuration |
| `fhir_metadata` | FHIR R4 capability statement |
| `service_catalog` | Registered microservice inventory |
| `dashboard_stats` | Quality measure dashboard metrics |
| `demo_status` | Demo mode status and configuration |
| `demo_seed` | Seed demo data via gateway |

## Quick Start

```bash
cp .env.example .env
npm install
npm start
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `3100` | HTTP listen port |
| `HDIM_BASE_URL` | `http://localhost:18080` | Gateway URL |
| `MCP_EDGE_API_KEY` | (empty) | Optional API key |
| `MCP_EDGE_ENFORCE_ROLE_AUTH` | `true` | Enable RBAC |
| `HDIM_DEMO_MODE` | `false` | Return fixture data |
| `LOG_LEVEL` | `info` | Pino log level |

## Testing

```bash
npm test
```

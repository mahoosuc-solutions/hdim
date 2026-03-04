# hdim-mcp-edge-clinical

MCP edge sidecar exposing HDIM clinical tools (FHIR, Patient, Care Gap, Quality Measure, CQL) on port 3300.

## Strategies

| Strategy | Tools | Use Case |
|----------|-------|----------|
| `composite` (default) | 25 | Balanced: 4 generic FHIR + 21 domain shortcuts |
| `high-value` | 15 | Resource-specific: top 5 FHIR types + key domain ops |
| `full-surface` | 68 | Complete: all 20 FHIR types + all domain tools |

Select via `CLINICAL_TOOL_STRATEGY` env var.

## Quick Start

```bash
cp .env.example .env
npm install
npm start
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `3300` | HTTP listen port |
| `HDIM_BASE_URL` | `http://localhost:18080` | Gateway URL for FHIR/API calls |
| `MCP_EDGE_API_KEY` | (empty) | Optional API key for edge auth |
| `MCP_EDGE_ENFORCE_ROLE_AUTH` | `true` | Enable RBAC enforcement |
| `CLINICAL_TOOL_STRATEGY` | `composite` | Tool strategy |
| `HDIM_DEMO_MODE` | `false` | Return fixture data instead of live calls |
| `LOG_LEVEL` | `info` | Pino log level |

## Testing

```bash
npm test                                    # all tests (~7s)
npx jest --coverage --coverageProvider=v8   # with coverage report
```

## Docker

```bash
docker compose -f docker-compose.mcp-edge.yml build mcp-edge-clinical
docker compose -f docker-compose.mcp-edge.yml up mcp-edge-clinical
```

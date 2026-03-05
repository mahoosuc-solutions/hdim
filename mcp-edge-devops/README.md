# hdim-mcp-edge-devops

MCP edge sidecar exposing HDIM DevOps tools (Docker operations, compose management) on port 3200.

## Tools (7)

| Tool | Description |
|------|-------------|
| `edge_health` | Edge sidecar health status |
| `docker_status` | Running container status |
| `docker_logs` | Container log retrieval |
| `docker_restart` | Container restart operations |
| `service_dependencies` | Service dependency graph |
| `compose_config` | Docker Compose configuration |
| `build_status` | Build pipeline status |

## Quick Start

```bash
cp .env.example .env
npm install
npm start
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `3200` | HTTP listen port |
| `HDIM_BASE_URL` | `http://localhost:18080` | Gateway URL |
| `HDIM_COMPOSE_FILE` | `docker-compose.demo.yml` | Compose file path |
| `DOCKER_BIN` | `docker` | Docker binary path |
| `MCP_EDGE_API_KEY` | (empty) | Optional API key |
| `MCP_EDGE_ENFORCE_ROLE_AUTH` | `true` | Enable RBAC |
| `HDIM_DEMO_MODE` | `false` | Return fixture data |
| `LOG_LEVEL` | `info` | Pino log level |

## Testing

```bash
npm test
```

## Docker

Requires Docker socket mount for container operations:

```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock:ro
```

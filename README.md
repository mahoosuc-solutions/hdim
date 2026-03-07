# HealthData-in-Motion (HDIM)

Healthcare interoperability and quality measurement platform.

## License Model

This project is **source-available** under the Business Source License 1.1.
See [LICENSE](./LICENSE) for allowed use and commercial terms.

## What HDIM Provides

- FHIR R4 interoperability services
- Quality measure and care-gap workflows
- Multi-service event processing with Kafka
- Clinical and operations portals
- Deployment patterns for local, staging, and production environments

## Architecture

Primary components:

- `backend/` - Java/Spring service modules
- `apps/` - Web portals and frontends
- `mcp-edge-*` - Node.js edge sidecars and integrations
- `docker-compose*.yml` - Local and environment-specific orchestration
- `docs/` - Architecture, operations, compliance, and release documentation

See [docs/architecture](./docs/architecture) for design records and flow docs.

## Quick Start (Local)

Prerequisites:

- Docker + Docker Compose
- Node.js 20+
- Java 21 (for backend builds/tests)

Run core stack:

```bash
docker compose --profile core up -d
```

Common endpoints (default local):

- API gateway: `http://localhost:8080`
- Clinical portal: `http://localhost:4200`
- Grafana (when enabled): `http://localhost:3001`

## Documentation

- Main docs index: [docs/README.md](./docs/README.md)
- Deployment and runbooks: [docs/deployment](./docs/deployment)
- Troubleshooting: [docs/troubleshooting](./docs/troubleshooting)
- Security policy: [SECURITY.md](./SECURITY.md)
- Contributing: [CONTRIBUTING.md](./CONTRIBUTING.md)

## Security Reporting

Report vulnerabilities privately via the process in [SECURITY.md](./SECURITY.md).
Do not open public issues for undisclosed security vulnerabilities.

## Project Status

Repository is under active development with frequent updates.
For release and validation artifacts, see [docs/releases](./docs/releases).

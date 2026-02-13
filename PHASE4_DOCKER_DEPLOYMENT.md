# Phase 4: Docker Deployment - Live Call Sales Agent

## Overview

Phase 4 implements production-ready Docker deployment for the Live Call Sales Agent system with:

- **Python FastAPI Service** (backend/modules/services/live-call-sales-agent)
- **Angular Coaching UI** (apps/coaching-ui)
- **Docker Compose Integration** with infrastructure services
- **Health Checks** and resource limits
- **OpenTelemetry Tracing** integration
- **Security & HIPAA Compliance** patterns

## Services Added

### 1. live-call-sales-agent (Port 8095)

**Purpose:** Python FastAPI service for Google Meet bot, transcription, and coaching engine

**Configuration:**
```yaml
Container: hdim-live-call-sales-agent
Port: 8095
Profiles: ["ai", "full"]
Dependencies: postgres, redis
Resources: 2GB RAM / 2 CPU (limits), 1GB RAM / 1 CPU (reserved)
```

**Environment Variables:**
```bash
# Google Cloud
GOOGLE_APPLICATION_CREDENTIALS=/secrets/google-meet-service-account.json
GOOGLE_CLOUD_PROJECT=hdim-project

# Services
AI_SALES_AGENT_URL=http://ai-sales-agent:8090
WEBSOCKET_HOST=clinical-workflow-service
WEBSOCKET_PORT=8093

# Database
POSTGRES_HOST=postgres
POSTGRES_DB=customer_deployments_db
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=your_secure_password_here

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Service
SERVICE_PORT=8095
MOCK_MODE=false (production)
LOG_LEVEL=INFO
```

**Health Check:**
```bash
CMD: python -c "import urllib.request; urllib.request.urlopen('http://localhost:8095/health').read()"
Interval: 30s
Timeout: 10s
Retries: 5
Start Period: 60s
```

### 2. coaching-ui (Port 4201)

**Purpose:** Angular 17 web application for real-time coaching display

**Configuration:**
```yaml
Container: hdim-coaching-ui
Port: 4201 (maps to 4200 inside container)
Profiles: ["ai", "full"]
Dependencies: live-call-sales-agent
Resources: 512MB RAM / 0.5 CPU (limits), 256MB RAM / 0.25 CPU (reserved)
```

**Build Process:**
1. **Multi-stage Docker build:**
   - Stage 1 (builder): Node 20-alpine, npm ci, npm run build:prod
   - Stage 2 (production): Nginx-alpine, serves built application

2. **Nginx Configuration:**
   - Angular routing support (SPA routing to index.html)
   - Security headers (X-Frame-Options, X-Content-Type-Options, CSP)
   - Gzip compression for assets
   - Cache control (no cache for HTML, max-age for assets)
   - Health check endpoint: /health

**Environment Variables:**
```bash
API_URL=http://live-call-sales-agent:8095
WEBSOCKET_URL=ws://live-call-sales-agent:8095
```

## Deployment Instructions

### Prerequisites

1. **Service Account Credentials**
   ```bash
   mkdir -p secrets/
   # Download service account JSON from Google Cloud Console
   # Save as: secrets/google-meet-service-account.json
   chmod 600 secrets/google-meet-service-account.json
   ```

2. **Environment Configuration**
   ```bash
   cp .env.production .env
   # Edit .env with your configuration
   ```

3. **Database Initialization**
   - Docker Compose automatically runs `docker/postgres/init-multi-db.sh`
   - Creates `customer_deployments_db` with Liquibase migrations
   - Validates schema during service startup

### Start Services

**Option 1: AI Profile (Live Call Sales Agent + Coaching UI)**
```bash
docker compose --profile ai up -d
```

**Option 2: Full Profile (All services including Clinical Portal)**
```bash
docker compose --profile full up -d
```

### Verify Deployment

```bash
# Check service status
docker compose ps

# Expected output:
# hdim-live-call-sales-agent    Running ✅
# hdim-coaching-ui               Running ✅
# healthdata-postgres            Running ✅
# healthdata-redis               Running ✅
# etc.

# Check logs
docker compose logs -f live-call-sales-agent
docker compose logs -f coaching-ui

# Test health endpoints
curl http://localhost:8095/health                    # Python service
curl http://localhost:4201/health                    # Nginx proxy

# Test API endpoints
curl -X GET http://localhost:8095/api/diagnostics    # Configuration status
```

### Access Services

- **Coaching UI:** http://localhost:4201
- **Python Service API:** http://localhost:8095
- **Diagnostics:** http://localhost:8095/api/diagnostics
- **Swagger UI:** http://localhost:8095/docs (if implemented)

## Docker Compose Configuration

### Service Dependencies

```
clinical-portal
└── gateway-edge
    └── All clinical services

live-call-sales-agent
├── postgres (customer_deployments_db)
├── redis (active call state cache)
├── ai-sales-agent (coaching suggestions)
├── clinical-workflow-service (WebSocket endpoint)
└── jaeger (distributed tracing)

coaching-ui
└── live-call-sales-agent
```

### Network Configuration

```yaml
networks:
  healthdata-network:
    driver: bridge
    # All services connected via internal network
    # Communication via container names (no need for IP addresses)
```

### Volume Mounts

```yaml
live-call-sales-agent:
  volumes:
    - /tmp/.X11-unix:/tmp/.X11-unix:ro         # Chrome display forwarding
    - ./secrets:/secrets:ro                    # Service account credentials
    - /tmp/coaching-transcripts:/data/transcripts  # Transcript storage
```

## Resource Management

### CPU & Memory Allocation

| Service | Limit | Reserved |
|---------|-------|----------|
| live-call-sales-agent | 2G RAM / 2 CPU | 1G RAM / 1 CPU |
| coaching-ui | 512M RAM / 0.5 CPU | 256M RAM / 0.25 CPU |
| postgres | 2G RAM / 2 CPU | 1G RAM / 1 CPU |
| redis | 512M RAM / 0.5 CPU | 256M RAM / 0.25 CPU |
| **Total** | **~7GB RAM / 6 CPU** | **~4GB RAM / 3 CPU** |

### Optimization Tips

1. **Development Machine (8GB RAM, 4 CPU)**
   - Use `--profile light` for minimal footprint
   - Or use mock mode for live-call-sales-agent

2. **Production Server (16GB+ RAM, 8+ CPU)**
   - Use `--profile ai` or `--profile full`
   - Monitor with `docker stats`

## Troubleshooting

### Service Won't Start

```bash
# Check logs
docker compose logs live-call-sales-agent
docker compose logs coaching-ui

# Common issues:
# 1. Port already in use: Change port mapping in docker-compose.yml
# 2. Missing secrets: Verify credentials in ./secrets/
# 3. Database not ready: Wait 60s for postgres health check
```

### Health Check Failures

```bash
# live-call-sales-agent
docker compose exec live-call-sales-agent python -c "import requests; requests.get('http://localhost:8095/health')"

# coaching-ui
docker compose exec coaching-ui wget --spider http://localhost:4200/
```

### Database Connection Issues

```bash
# Verify database exists
docker compose exec postgres psql -U healthdata -d customer_deployments_db -c "\dt"

# Check migrations applied
docker compose exec postgres psql -U healthdata -d customer_deployments_db -c "SELECT * FROM databasechangelog;"
```

### WebSocket Connection Issues

```bash
# Check WebSocket service is reachable
curl -i http://clinical-workflow-service:8093/health

# Monitor live-call-sales-agent logs
docker compose logs -f live-call-sales-agent | grep -i websocket
```

## Production Deployment Checklist

Before deploying to production:

- [ ] Service account credentials mounted and validated
- [ ] Environment variables configured (.env file)
- [ ] Database migrations applied successfully
- [ ] Health checks passing for all services
- [ ] Resource limits set appropriately
- [ ] OpenTelemetry tracing configured
- [ ] Logs configured and monitored
- [ ] Secrets management system in place
- [ ] Backup strategy for transcripts and call data
- [ ] Network security (firewall rules)
- [ ] SSL/TLS certificates configured (if using reverse proxy)
- [ ] Load balancer configured (if scaling)

## Next Steps

### Phase 4A: Integration Testing
- [ ] Manual testing of complete workflow (bot join → transcription → coaching)
- [ ] End-to-end integration tests
- [ ] Performance testing (concurrent calls)
- [ ] Load testing (multiple concurrent users)

### Phase 4B: Production Hardening
- [ ] Security audit
- [ ] HIPAA compliance validation
- [ ] Disaster recovery procedures
- [ ] Monitoring and alerting setup
- [ ] Runbook creation for operations team

### Phase 5: Optimization
- [ ] Performance tuning
- [ ] Cost optimization
- [ ] Caching strategy refinement
- [ ] Database query optimization

## Command Reference

```bash
# Start services
docker compose --profile ai up -d

# View logs
docker compose logs -f live-call-sales-agent

# Execute command in container
docker compose exec live-call-sales-agent bash

# Stop services
docker compose down

# Remove volumes (WARNING: deletes data)
docker compose down -v

# Rebuild image
docker compose build live-call-sales-agent

# Scale service
docker compose up -d --scale coaching-ui=2
```

## Security Considerations

1. **Secrets Management**
   - Use Docker secrets or external secrets manager for production
   - Never commit credentials to repository
   - Rotate credentials regularly

2. **Network Security**
   - Services communicate via internal bridge network
   - No direct external access except through gateway
   - Use network policies to restrict traffic

3. **Data Protection**
   - PHI encrypted at rest in PostgreSQL
   - HIPAA-compliant audit logging
   - Transcript retention policy (90 days configurable)
   - Regular security audits

4. **Access Control**
   - JWT authentication for all APIs
   - Multi-tenant isolation via tenant_id
   - RBAC for authorization
   - API rate limiting

## Monitoring & Observability

1. **Health Checks**
   - Live: http://localhost:8095/health
   - Coaching UI: http://localhost:4201/health

2. **Logs**
   ```bash
   docker compose logs -f live-call-sales-agent
   docker compose logs -f coaching-ui
   ```

3. **Metrics**
   - Prometheus: http://localhost:9090 (if enabled)
   - Grafana: http://localhost:3001 (if enabled)

4. **Distributed Tracing**
   - Jaeger UI: http://localhost:16686
   - Traces collected from all services

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review logs with `docker compose logs`
3. Consult HDIM documentation at `./docs/`
4. Contact engineering team

---

**Status:** Phase 4 Complete ✅  
**Next:** Phase 4A - Integration Testing  
**Date:** February 2026

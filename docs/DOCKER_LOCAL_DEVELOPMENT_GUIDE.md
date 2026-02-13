# Docker Local Development Guide - Sales Agent Services

**Setup Time:** ~5 minutes
**Development: 2-3 commands to start developing**

This guide walks you through setting up the Live Call Sales Agent services locally using Docker Compose with live code reloading.

---

## 📋 Quick Start (TL;DR)

```bash
# 1. Copy environment configuration
cp .env.example .env.dev

# 2. Start all services (infrastructure + all 3 services)
docker compose -f docker-compose.dev.sales-agents.yml --profile full up -d

# 3. Verify services are running
docker compose -f docker-compose.dev.sales-agents.yml ps

# 4. Access the services
# AI Sales Agent:      http://localhost:8090/docs (Swagger UI)
# Coaching UI:         http://localhost:4200
# Jaeger:              http://localhost:16686
# Grafana:             http://localhost:3001 (admin/admin)
# Prometheus:          http://localhost:9090
```

---

## 🚀 Getting Started

### Prerequisites

- **Docker Desktop** (version 4.10+)
- **Docker Compose** (included in Docker Desktop)
- **5+ GB free disk space** (for services and data)
- **8+ GB RAM** (for comfortable development)

### Environment Setup

**1. Copy and customize environment configuration:**

```bash
# Copy development configuration
cp .env.dev .env

# Optional: Edit .env for custom settings
# nano .env
```

**Key settings in .env.dev:**
```
DEBUG=true                    # Enable debug logging
LOG_LEVEL=DEBUG               # Verbose logging
OTEL_SAMPLER=always_on        # Collect all traces
MOCK_GOOGLE_MEET=true         # Use mock Google Meet (no credentials)
MOCK_SPEECH_TO_TEXT=false     # Use real Speech-to-Text if credentials available
POSTGRES_PASSWORD=healthdata_dev
REDIS_HOST=redis
JAEGER_UI_URL=http://localhost:16686
```

**2. Create required directories:**

```bash
# Create data directories for persistent storage
mkdir -p data/transcripts logs/{ai-sales-agent,live-call-sales-agent,coaching-ui}

# Create secrets directory for Google credentials (optional)
mkdir -p secrets
```

**3. (Optional) Add Google API credentials:**

If you want to test with real Google Meet and Speech-to-Text APIs:

```bash
# Download service account JSON from Google Cloud Console
# https://console.cloud.google.com → IAM → Service Accounts

# Copy to secrets directory
cp ~/Downloads/hdim-service-account.json secrets/google-meet-service-account.json

# Update .env
echo "GOOGLE_APPLICATION_CREDENTIALS=/secrets/google-meet-service-account.json" >> .env
echo "MOCK_GOOGLE_MEET=false" >> .env
echo "MOCK_SPEECH_TO_TEXT=false" >> .env
```

---

## 🐳 Docker Compose Profiles

The docker-compose file uses **profiles** for selective service startup:

| Profile | Services | Use Case |
|---------|----------|----------|
| `full` | All (services + infrastructure) | Full local development |
| `ai` | ai-sales-agent + live-call-sales-agent + infra | Python services only |
| `ui` | coaching-ui + infra | Frontend development |
| `db` | postgres + redis | Database-only (no services) |
| `observability` | jaeger + prometheus + grafana | Monitoring stack |

### Start Specific Services

```bash
# Start all services and infrastructure
docker compose -f docker-compose.dev.sales-agents.yml --profile full up -d

# Start only Python services (without Angular UI)
docker compose -f docker-compose.dev.sales-agents.yml --profile ai up -d

# Start only UI service
docker compose -f docker-compose.dev.sales-agents.yml --profile ui up -d

# Start only infrastructure (database, cache, observability)
docker compose -f docker-compose.dev.sales-agents.yml --profile db --profile observability up -d
```

---

## 📁 Volume Mounts & Live Reloading

The Docker Compose file mounts source code directories for live reloading:

### Python Services (FastAPI + uvicorn)

**Service:** `ai-sales-agent` and `live-call-sales-agent`

```bash
# Source code is mounted at:
./backend/modules/services/[service]/src → /app/src:ro

# Changes trigger automatic reload (via uvicorn --reload)
# Edit any Python file and uvicorn restarts
```

**Example:**
```bash
# Edit the main service
vim backend/modules/services/ai-sales-agent/src/main.py

# Save file → uvicorn detects change → service reloads
# Logs will show: "Restarting due to file change..."
```

### Angular UI Service

**Service:** `coaching-ui`

```bash
# Built app is mounted at:
./apps/coaching-ui/src → /usr/share/nginx/html/src:ro

# For hot reload, consider running Angular outside Docker:
cd apps/coaching-ui
ng serve --host 0.0.0.0 --port 4200
```

**Alternative:** Use Docker-based development with `ng serve` in container:

```bash
# Modify docker-compose to use ng serve instead of production build
# This requires additional setup but provides hot reload in container
```

---

## 🔍 Service Port Reference

All services are accessible locally:

| Service | Port | URL | Purpose |
|---------|------|-----|---------|
| **AI Sales Agent** | 8090 | http://localhost:8090 | Discovery Agent service |
| **Coaching UI** | 4200 | http://localhost:4200 | Real-time coaching interface |
| **Live Call Sales Agent** | 8095 | http://localhost:8095 | Google Meet integration |
| **PostgreSQL** | 5435 | localhost:5435 | customer_deployments_db |
| **Redis** | 6380 | localhost:6380 | Cache layer |
| **Jaeger UI** | 16686 | http://localhost:16686 | Distributed traces |
| **Prometheus** | 9090 | http://localhost:9090 | Metrics collection |
| **Grafana** | 3001 | http://localhost:3001 | Dashboards (admin/admin) |

---

## 🛠️ Common Development Tasks

### Adding Python Dependencies

For `ai-sales-agent` or `live-call-sales-agent`:

```bash
# 1. Update requirements.txt or pyproject.toml
vim backend/modules/services/ai-sales-agent/requirements.txt

# 2. Rebuild the Docker image
docker compose -f docker-compose.dev.sales-agents.yml build ai-sales-agent

# 3. Restart the service
docker compose -f docker-compose.dev.sales-agents.yml up -d ai-sales-agent

# 4. Verify the service started
docker compose -f docker-compose.dev.sales-agents.yml logs -f ai-sales-agent
```

### Adding Angular Dependencies

```bash
# 1. Add to package.json
cd apps/coaching-ui
npm install --save new-package

# 2. Rebuild the image
docker compose -f docker-compose.dev.sales-agents.yml build coaching-ui

# 3. Restart the service
docker compose -f docker-compose.dev.sales-agents.yml up -d coaching-ui
```

### Viewing Logs

```bash
# View logs from specific service
docker compose -f docker-compose.dev.sales-agents.yml logs -f ai-sales-agent

# View logs from multiple services
docker compose -f docker-compose.dev.sales-agents.yml logs -f ai-sales-agent live-call-sales-agent

# View only recent logs (last 50 lines)
docker compose -f docker-compose.dev.sales-agents.yml logs --tail 50 ai-sales-agent

# View logs with timestamps
docker compose -f docker-compose.dev.sales-agents.yml logs -f --timestamps ai-sales-agent
```

### Executing Commands in Running Container

```bash
# Run Python command in ai-sales-agent container
docker compose -f docker-compose.dev.sales-agents.yml exec ai-sales-agent python -c "import sys; print(sys.version)"

# Run bash shell in container
docker compose -f docker-compose.dev.sales-agents.yml exec ai-sales-agent /bin/bash

# Check installed Python packages
docker compose -f docker-compose.dev.sales-agents.yml exec ai-sales-agent pip list
```

### Database Operations

```bash
# Connect to PostgreSQL
docker compose -f docker-compose.dev.sales-agents.yml exec postgres psql -U healthdata -d customer_deployments_db

# View tables
\dt

# Run SQL query
SELECT * FROM lc_deployments;

# Exit psql
\q
```

### Debugging with Logs

```bash
# View structured JSON logs
docker compose -f docker-compose.dev.sales-agents.yml logs ai-sales-agent | grep "ERROR" | jq .

# Filter logs by service and level
docker compose -f docker-compose.dev.sales-agents.yml logs --follow ai-sales-agent | grep -i "error\|warning"

# Export logs to file for analysis
docker compose -f docker-compose.dev.sales-agents.yml logs ai-sales-agent > /tmp/logs.txt
```

---

## 🔍 Debugging & Monitoring

### Using Jaeger for Distributed Tracing

1. **Open Jaeger UI:** http://localhost:16686
2. **Select service:** Choose `ai-sales-agent` or `live-call-sales-agent`
3. **View traces:** See all requests and their spans
4. **Analyze latency:** Identify slow operations

**Example trace workflow:**
```
Request → Gateway → AI Sales Agent → Database → Response
Each step has timing, errors, and detailed context
```

### Using Prometheus & Grafana for Metrics

1. **Open Grafana:** http://localhost:3001
2. **Login:** admin / admin
3. **Add Prometheus datasource:** http://prometheus:9090
4. **Create dashboard:** Monitor service metrics (latency, errors, throughput)

**Key metrics to monitor:**
- Request latency (p50, p95, p99)
- Error rate (5xx, 4xx)
- Throughput (requests/second)
- Database connection pool status
- Redis cache hit rate

### Health Checks

```bash
# Check service health
curl http://localhost:8090/health

# Detailed response
curl -s http://localhost:8090/health | jq .

# Expected response:
# {
#   "status": "healthy",
#   "timestamp": "2026-02-13T13:00:00Z",
#   "checks": {
#     "database": "up",
#     "redis": "up",
#     "jaeger": "connected"
#   }
# }
```

---

## 🛑 Stopping and Cleanup

### Stop Services (Preserve Data)

```bash
# Stop all running services
docker compose -f docker-compose.dev.sales-agents.yml down

# Services stop but volumes (database, redis) are preserved
# Next `up` will restore the same state
```

### Clean Shutdown (Stop + Remove Containers)

```bash
# Stop and remove containers
docker compose -f docker-compose.dev.sales-agents.yml down --remove-orphans

# Data persists in volumes
```

### Full Cleanup (Remove Everything)

⚠️ **Warning: This deletes all data including databases**

```bash
# Remove containers, volumes, and networks
docker compose -f docker-compose.dev.sales-agents.yml down -v

# Start fresh with clean state
docker compose -f docker-compose.dev.sales-agents.yml --profile full up -d
```

### Partial Cleanup

```bash
# Remove only specific service
docker compose -f docker-compose.dev.sales-agents.yml down ai-sales-agent

# Remove specific volume
docker volume rm hdim-master_postgres_data_dev

# Remove all unused Docker resources
docker system prune -a
```

---

## 🐛 Troubleshooting

### Service Won't Start

**Problem:** Container exits immediately

```bash
# Check logs
docker compose -f docker-compose.dev.sales-agents.yml logs ai-sales-agent

# Rebuild image
docker compose -f docker-compose.dev.sales-agents.yml build --no-cache ai-sales-agent

# Try again
docker compose -f docker-compose.dev.sales-agents.yml up -d ai-sales-agent
```

### Port Already in Use

**Problem:** "Address already in use"

```bash
# Find process using port 8090
lsof -i :8090

# Kill the process
kill -9 <PID>

# Or use different port in docker-compose.dev.sales-agents.yml
# Change "8090:8090" to "8091:8090"
```

### Database Connection Errors

**Problem:** "Cannot connect to postgres"

```bash
# Check PostgreSQL health
docker compose -f docker-compose.dev.sales-agents.yml ps postgres

# View PostgreSQL logs
docker compose -f docker-compose.dev.sales-agents.yml logs postgres

# Verify database exists
docker compose -f docker-compose.dev.sales-agents.yml exec postgres psql -U healthdata -l | grep customer_deployments_db

# Reinitialize database
docker compose -f docker-compose.dev.sales-agents.yml down -v postgres
docker compose -f docker-compose.dev.sales-agents.yml up -d postgres
```

### Health Check Failing

**Problem:** "unhealthy" status in `docker compose ps`

```bash
# Check health check command
docker compose -f docker-compose.dev.sales-agents.yml logs ai-sales-agent | tail -20

# Manually run health check
docker compose -f docker-compose.dev.sales-agents.yml exec ai-sales-agent curl http://localhost:8090/health

# Increase start period if needed (service needs more time to initialize)
# Edit docker-compose.dev.sales-agents.yml: start_period: 30s
```

### Out of Memory

**Problem:** Docker containers crash or system becomes slow

```bash
# Check Docker resource limits
docker system df

# Remove unused images
docker image prune -a

# Reduce services running (remove observability profile)
docker compose -f docker-compose.dev.sales-agents.yml down

# Restart with only needed services
docker compose -f docker-compose.dev.sales-agents.yml --profile ai up -d
```

---

## 📚 Integration with IDE/Editor

### VS Code Integration

**Remote Containers Extension:**

```json
// .devcontainer/devcontainer.json
{
  "name": "HDIM Sales Agent",
  "dockerComposeFile": "../docker-compose.dev.sales-agents.yml",
  "service": "ai-sales-agent",
  "workspaceFolder": "/workspace",
  "customizations": {
    "vscode": {
      "extensions": ["ms-python.python", "charliermarsh.ruff"]
    }
  }
}
```

### PyCharm Integration

1. **Settings → Project → Python Interpreter**
2. **Add Interpreter → Docker Compose**
3. **Select service:** `ai-sales-agent`
4. **Configure:** Path to docker-compose file

---

## ✅ Verification Checklist

After starting services, verify everything works:

- [ ] AI Sales Agent responding: `curl http://localhost:8090/health`
- [ ] Coaching UI loading: http://localhost:4200
- [ ] PostgreSQL connected: `docker compose exec postgres psql -U healthdata -d customer_deployments_db -c "SELECT count(*) FROM lc_deployments;"`
- [ ] Redis connected: `docker compose exec redis redis-cli ping`
- [ ] Jaeger collecting traces: http://localhost:16686 (see services dropdown)
- [ ] Prometheus scraping: http://localhost:9090/api/v1/targets (all "UP")
- [ ] Grafana dashboard: http://localhost:3001 (login: admin/admin)

---

## 📞 Getting Help

**Common Issues:**
- Check logs: `docker compose logs -f [SERVICE]`
- Rebuild image: `docker compose build --no-cache [SERVICE]`
- Reset everything: `docker compose down -v && docker compose --profile full up -d`

**Documentation:**
- [Docker Compose Reference](https://docs.docker.com/compose/reference/)
- [Docker Multi-stage Builds](https://docs.docker.com/build/building/multi-stage/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)

---

## 🎯 Next Steps

1. **Start services:** `docker compose --profile full up -d`
2. **Make code change:** Edit `backend/modules/services/ai-sales-agent/src/main.py`
3. **See live reload:** Watch logs with `docker compose logs -f ai-sales-agent`
4. **View traces:** Open http://localhost:16686 to see distributed traces
5. **Test API:** Use Swagger UI at http://localhost:8090/docs

Happy developing! 🚀

---

**Last Updated:** February 13, 2026
**Docker Compose Version:** 3.9
**Services:** AI Sales Agent, Live Call Sales Agent, Coaching UI

# HDIM Platform Demo Documentation

This directory contains comprehensive guides for starting the HDIM platform in demo mode and capturing screenshots for marketing purposes.

## Documentation Files

### [QUICK_START.md](./QUICK_START.md)
**5-minute quick start guide** for experienced users who want to get the demo running quickly.

**Use this if:**
- You've run the demo before
- You need a quick reference
- You want the fastest path to screenshots

### [DEMO_STARTUP_GUIDE.md](./DEMO_STARTUP_GUIDE.md)
**Comprehensive step-by-step guide** with detailed instructions for every phase of the demo startup process.

**Use this if:**
- This is your first time running the demo
- You need detailed explanations
- You're troubleshooting issues
- You want to understand the architecture

## Quick Navigation

### I Want To...

- **Start the demo quickly:** → [QUICK_START.md](./QUICK_START.md)
- **Understand each step:** → [DEMO_STARTUP_GUIDE.md](./DEMO_STARTUP_GUIDE.md) - Phase 1-7
- **Troubleshoot issues:** → [DEMO_STARTUP_GUIDE.md](./DEMO_STARTUP_GUIDE.md) - Troubleshooting section
- **See the architecture:** → [DEMO_STARTUP_GUIDE.md](./DEMO_STARTUP_GUIDE.md) - Architecture Overview
- **Capture screenshots:** → [DEMO_STARTUP_GUIDE.md](./DEMO_STARTUP_GUIDE.md) - Phase 7

## Process Overview

### High-Level Flow

```
1. Infrastructure (PostgreSQL, Redis, Kafka, Jaeger)
   ↓
2. Backend Services (FHIR, CQL, Patient, Quality, Care Gap, Events)
   ↓
3. Gateway Services (Admin, FHIR, Clinical, Edge)
   ↓
4. Demo Seeding (Generate synthetic data)
   ↓
5. Clinical Portal (Frontend application)
   ↓
6. Verification (Health checks)
   ↓
7. Screenshot Capture (Playwright automation)
```

### Time Estimates

| Phase | Duration | Description |
|-------|----------|-------------|
| Infrastructure | 30s | Core services startup |
| Backend | 60s | Business logic services |
| Gateways | 60s | API routing services |
| Seeding | 30s | Demo data generation |
| Portal | 45s | Frontend startup |
| Verification | 30s | Health checks |
| Screenshots | 5-10m | Automated capture |
| **Total** | **~10-15m** | Complete process |

## Key Files

### Configuration Files
- `docker-compose.demo.yml` - Demo environment configuration
- `start-demo.sh` - Automated startup script
- `scripts/capture-screenshots.js` - Screenshot automation

### Output Directories
- `docs/screenshots/` - Captured screenshots organized by user type
- `docs/screenshots/INDEX.md` - Screenshot index and catalog

## Service Architecture

### Infrastructure Layer
- **PostgreSQL** (5435) - Primary database
- **Redis** (6380) - Caching layer
- **Kafka** (9094) - Message broker
- **Jaeger** (16686) - Distributed tracing

### Backend Services
- **FHIR Service** (8085) - FHIR R4 data storage
- **CQL Engine** (8081) - Clinical Quality Language evaluation
- **Patient Service** (8084) - Patient data management
- **Quality Measure** (8087) - HEDIS/CMS quality measures
- **Care Gap Service** (8086) - Care gap identification
- **Event Processing** (8083) - Event stream processing

### Gateway Services
- **Gateway Admin** (8080) - Administrative API gateway
- **Gateway FHIR** (8080) - FHIR API gateway
- **Gateway Clinical** (8080) - Clinical workflow gateway
- **Gateway Edge** (18080) - Main reverse proxy

### Frontend
- **Clinical Portal** (4200) - User-facing web application

### Demo Services
- **Demo Seeding** (8098) - Synthetic data generation

## Demo Credentials

| Role | Email | Password | Access Level |
|------|-------|----------|--------------|
| Admin | `demo_admin@hdim.ai` | `demo123` | Full administrative access |
| Analyst | `demo_analyst@hdim.ai` | `demo123` | Data analysis and reporting |
| Viewer | `demo_viewer@hdim.ai` | `demo123` | Read-only access |

## Common Commands

### Start Demo
```bash
docker compose -f docker-compose.demo.yml up -d
```

### Check Status
```bash
docker compose -f docker-compose.demo.yml ps
```

### View Logs
```bash
docker compose -f docker-compose.demo.yml logs -f
```

### Stop Demo
```bash
docker compose -f docker-compose.demo.yml down
```

### Capture Screenshots
```bash
node scripts/capture-screenshots.js
```

## Troubleshooting

### Services Not Starting
1. Check Docker is running: `docker ps`
2. Check logs: `docker logs <container-name>`
3. Verify ports are available: `netstat -tulpn | grep <port>`

### Health Check Failures
1. Wait longer for services to initialize (60-90 seconds)
2. Check service dependencies are healthy
3. Review service logs for errors

### Screenshot Capture Issues
1. Verify portal is accessible: `curl http://localhost:4200`
2. Check all services are healthy
3. Increase timeout in `capture-screenshots.js`
4. Verify demo credentials are correct

## Next Steps

After successfully running the demo:

1. **Review Screenshots:** Check `docs/screenshots/INDEX.md`
2. **Select Best Shots:** Choose highest quality images for marketing
3. **Update Materials:** Use screenshots in presentations and docs
4. **Archive:** Store in version control or asset management

## Support

For detailed troubleshooting and step-by-step instructions, see:
- [DEMO_STARTUP_GUIDE.md](./DEMO_STARTUP_GUIDE.md) - Comprehensive guide
- Service logs: `docker logs <container-name>`
- Health checks: Run verification scripts in the guide

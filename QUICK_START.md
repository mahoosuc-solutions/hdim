# Quick Start Guide

**HealthData In Motion HIE Platform**

Get up and running in 5 minutes!

---

## Current Deployment (Already Running)

**The platform is already running with Kong API Gateway!**

### Access Now

```bash
# View the UI
open http://localhost:4200

# Or test APIs directly
curl -H "X-Tenant-ID: default" \
  "http://localhost:8000/api/cql/api/v1/cql/evaluations?page=0&size=10"
```

### Verify Status

```bash
./verify-deployment.sh
```

---

## Access Points

| Service | URL | Description |
|---------|-----|-------------|
| **Clinical Portal** | http://localhost:4200 | Main Angular UI |
| **Kong Gateway** | http://localhost:8000 | API Gateway (secured) |
| **Kong Admin** | http://localhost:8001 | Kong configuration |
| **CQL Engine** | http://localhost:8000/api/cql | Via Kong |
| **Quality Measure** | http://localhost:8000/api/quality | Via Kong |
| **FHIR Server** | http://localhost:8000/api/fhir | Via Kong |

---

## Deploy on RHEL 7

### One-Command Deployment

```bash
./deploy-rhel7.sh
```

---

**Platform is ready at**: http://localhost:4200

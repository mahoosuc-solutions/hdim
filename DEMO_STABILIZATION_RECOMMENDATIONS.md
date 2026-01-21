# Demo Stabilization Recommendations

**Date**: January 15, 2026  
**Status**: ✅ **RECOMMENDATION READY**

---

## Recommendation: Use Demo Compose File

**Best Approach**: Use `demo/docker-compose.demo.yml` for demos

**Why**:
1. ✅ **Pre-configured** - Already set up for demo scenarios
2. ✅ **Minimal** - Only 14 services vs 19+ currently running
3. ✅ **Tested** - Has test scripts and validation
4. ✅ **Separate** - Doesn't interfere with dev environment
5. ✅ **No Migration Issues** - Uses demo profile with different DB setup
6. ✅ **Includes Demo Data** - Has seeding scripts ready

---

## Required Services for Demos

### Core Services (6)
- gateway-service
- fhir-service
- patient-service
- care-gap-service
- quality-measure-service
- cql-engine-service

### Infrastructure (7)
- postgres
- redis
- zookeeper
- kafka
- elasticsearch
- jaeger (optional)
- prometheus + grafana (optional)

### Frontend (1)
- clinical-portal

**Total**: 14 services

---

## Services Currently Running (19)

**Can Stop** (5 services):
- consent-service
- ecr-service
- event-processing-service
- event-router-service
- prior-auth-service

**Optional** (2 services):
- hcc-service (only for risk stratification demo)
- notification-service (may be needed)

**Keep** (12 services):
- postgres, redis, kafka, zookeeper, jaeger
- gateway-service, fhir-service, patient-service
- care-gap-service, quality-measure-service, cql-engine-service

---

## Execution Options

### Option A: Use Demo Compose (Recommended)
```bash
cd demo
docker compose -f docker-compose.demo.yml up -d
./seed-demo-data.sh
# Access at http://localhost:4200
```

**Pros**: Clean, tested, minimal
**Cons**: Separate environment

### Option B: Scale Down Current Environment
```bash
# Stop non-essential services
docker compose stop consent-service ecr-service event-processing-service \
  event-router-service prior-auth-service

# Fix gateway service (manual column add or fix migration)
# Verify required services healthy
```

**Pros**: Uses existing setup
**Cons**: Need to fix gateway service, more complex

---

## My Recommendation

**Use Option A (Demo Compose)** because:
1. Gateway service has migration issues - demo compose avoids this
2. Cleaner separation between dev and demo
3. Pre-configured with demo data seeding
4. Less resource usage
5. Already tested and documented

---

## Next Steps

1. **Stop current services**: `docker compose down`
2. **Start demo services**: `cd demo && docker compose -f docker-compose.demo.yml up -d`
3. **Seed demo data**: `./seed-demo-data.sh`
4. **Verify**: Check all services healthy
5. **Test**: Access http://localhost:4200

---

**Status**: ✅ **RECOMMENDATION PROVIDED - READY TO EXECUTE**

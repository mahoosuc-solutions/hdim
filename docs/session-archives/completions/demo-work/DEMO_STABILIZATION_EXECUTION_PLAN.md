# Demo Stabilization Execution Plan

**Date**: January 15, 2026  
**Status**: 📋 **READY TO EXECUTE**

---

## Executive Summary

**Goal**: Stabilize system and scale back to only services required for demo scenarios

**Approach**: Use demo compose file (`demo/docker-compose.demo.yml`) - pre-configured, minimal, tested

**Required Services**: 14 total (7 infrastructure + 6 core + 1 frontend)

---

## Demo Service Requirements

### Core Services (6)
1. ✅ `gateway-service` - API Gateway
2. ✅ `fhir-service` - FHIR R4 server
3. ✅ `patient-service` - Patient data
4. ✅ `care-gap-service` - Care gap identification
5. ✅ `quality-measure-service` - HEDIS measures
6. ✅ `cql-engine-service` - CQL execution

### Infrastructure (7)
1. ✅ `postgres` - Database
2. ✅ `redis` - Cache
3. ✅ `zookeeper` - Kafka dependency
4. ✅ `kafka` - Message queue
5. ✅ `elasticsearch` - FHIR search
6. ✅ `jaeger` - Tracing (optional)
7. ✅ `prometheus` + `grafana` - Monitoring (optional)

### Frontend (1)
1. ✅ `clinical-portal` - Angular frontend

---

## Execution Plan

### Phase 1: Stop Current Services (5 min)
```bash
# Stop all current services
docker compose down

# Verify all stopped
docker compose ps
```

### Phase 2: Start Demo Services (10 min)
```bash
# Navigate to demo directory
cd demo

# Start demo services
docker compose -f docker-compose.demo.yml up -d

# Wait for services to be healthy
docker compose -f docker-compose.demo.yml ps
```

### Phase 3: Initialize Demo Data (5 min)
```bash
# Seed demo data
./seed-demo-data.sh

# Verify data loaded
docker exec hdim-demo-postgres psql -U healthdata -d healthdata_demo -c "SELECT COUNT(*) FROM patients;"
```

### Phase 4: Verify Services (5 min)
```bash
# Check all services healthy
docker compose -f docker-compose.demo.yml ps

# Test health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8085/fhir/actuator/health
curl http://localhost:8084/patient/actuator/health
curl http://localhost:8086/care-gap/actuator/health
curl http://localhost:8087/quality-measure/actuator/health
curl http://localhost:8081/cql-engine/actuator/health
```

### Phase 5: Test Demo Portal (5 min)
```bash
# Open browser to http://localhost:4200
# Login: demo_user / demo_password
# Verify dashboard loads
```

---

## Alternative: Fix Current Environment

If you prefer to use main docker-compose.yml:

### Step 1: Fix Gateway Service
- Manually add token column OR
- Fix migration file

### Step 2: Stop Non-Essential Services
```bash
docker compose stop consent-service ecr-service event-processing-service \
  event-router-service prior-auth-service hcc-service notification-service backup
```

### Step 3: Verify Required Services
```bash
docker compose ps | grep -E "(gateway|patient|fhir|care-gap|quality-measure|cql-engine|postgres|redis|kafka|zookeeper)"
```

---

## Recommended: Use Demo Compose

**Why**:
- ✅ Pre-configured and tested
- ✅ Minimal resource usage
- ✅ Includes demo data seeding
- ✅ Separate from dev environment
- ✅ No migration issues (uses demo profile)

---

## Next Steps

1. **Choose approach** (Demo compose recommended)
2. **Execute stabilization plan**
3. **Verify all services healthy**
4. **Test demo scenarios**

---

**Status**: 📋 **READY TO EXECUTE - AWAITING USER DECISION**

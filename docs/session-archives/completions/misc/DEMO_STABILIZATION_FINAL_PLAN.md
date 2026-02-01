# Demo Stabilization - Final Execution Plan

**Date**: January 15, 2026  
**Status**: ✅ **READY TO EXECUTE**

---

## Recommendation: Use Demo Compose File

**Why**: 
- ✅ Avoids gateway migration issues (uses demo profile)
- ✅ Pre-configured and tested
- ✅ Minimal resource usage (14 vs 18 services)
- ✅ Includes demo data seeding
- ✅ Clean separation from dev environment

---

## Required Services for Demos

### Core Services (6)
1. `gateway-service` - API Gateway (8080)
2. `fhir-service` - FHIR R4 (8085)
3. `patient-service` - Patient data (8084)
4. `care-gap-service` - Care gaps (8086)
5. `quality-measure-service` - HEDIS (8087)
6. `cql-engine-service` - CQL execution (8081)

### Infrastructure (7)
1. `postgres` - Database (5435)
2. `redis` - Cache (6380)
3. `zookeeper` - Kafka dependency
4. `kafka` - Messaging (9094)
5. `elasticsearch` - FHIR search (9200)
6. `jaeger` - Tracing (16686) - Optional
7. `prometheus` + `grafana` - Monitoring - Optional

### Frontend (1)
1. `clinical-portal` - Angular UI (4200)

**Total**: 14 services

---

## Execution Steps

### Step 1: Stop Current Services
```bash
cd /home/webemo-aaron/projects/hdim-master
docker compose down
```

### Step 2: Start Demo Services
```bash
cd demo
docker compose -f docker-compose.demo.yml up -d
```

### Step 3: Wait for Services (2-3 minutes)
```bash
docker compose -f docker-compose.demo.yml ps
# Wait until all services show "healthy"
```

### Step 4: Seed Demo Data
```bash
./seed-demo-data.sh
```

### Step 5: Verify
```bash
# Check services
docker compose -f docker-compose.demo.yml ps

# Test portal
curl http://localhost:4200

# Test API
curl http://localhost:8080/actuator/health
```

---

## Current Status

**Running Services**: 18
**Required for Demo**: 14
**Can Stop**: 4+ services

**Issues**:
- Gateway service: Migration checksum error (avoided with demo compose)
- Patient service: ✅ Fixed (7 audit tables created)

---

## Next Actions

1. Stop current services
2. Start demo services
3. Seed demo data
4. Verify all healthy
5. Test demo portal

---

**Status**: ✅ **READY TO EXECUTE - AWAITING CONFIRMATION**

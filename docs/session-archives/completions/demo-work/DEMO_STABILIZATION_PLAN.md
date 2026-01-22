# Demo Services Stabilization Plan

**Date**: January 15, 2026  
**Status**: 📋 **PLANNING DEMO SERVICE STABILIZATION**

---

## Demo Service Requirements

Based on `demo/docker-compose.demo.yml` and demo documentation:

### ✅ Required Services for All Demo Scenarios

**Infrastructure** (7 services):
1. ✅ `postgres` - Database (port 5435)
2. ✅ `redis` - Cache (port 6380)
3. ✅ `zookeeper` - Kafka dependency
4. ✅ `kafka` - Message queue (port 9094)
5. ✅ `elasticsearch` - FHIR search (port 9200)
6. ✅ `jaeger` - Tracing (optional, port 16686)
7. ✅ `prometheus` + `grafana` - Monitoring (optional)

**Core Services** (6 services):
1. ✅ `gateway-service` - API Gateway (port 8080/8087)
2. ✅ `fhir-service` - FHIR R4 server (port 8085)
3. ✅ `patient-service` - Patient data (port 8084)
4. ✅ `care-gap-service` - Care gap identification (port 8086)
5. ✅ `quality-measure-service` - HEDIS measures (port 8087)
6. ✅ `cql-engine-service` - CQL execution (port 8081)

**Frontend** (1 service):
1. ✅ `clinical-portal` - Angular frontend (port 4200)

**Total**: 14 services (7 infrastructure + 6 core + 1 frontend)

---

## Services to Stop (Not Required for Demos)

Based on current running services, these can be stopped:

1. ❌ `consent-service` - Not needed for core demos
2. ❌ `ecr-service` - Not needed for core demos
3. ❌ `event-processing-service` - Not needed for core demos
4. ❌ `event-router-service` - Not needed for core demos
5. ❌ `prior-auth-service` - Not needed for core demos
6. ❌ `hcc-service` - Optional (only for risk stratification demo)
7. ❌ `notification-service` - Optional (may be needed for some demos)
8. ❌ `backup` - Not needed for demos

---

## Current Issues to Fix

### 1. Gateway Service - Token Column Issue
- **Status**: Migration failing
- **Options**:
  - **Option A**: Fix migration (current approach)
  - **Option B**: Manually add column and mark changeset as executed
  - **Option C**: Use demo compose file (separate environment)

### 2. Patient Service - Audit Tables
- **Status**: ✅ FIXED - 7 audit tables created successfully

---

## Stabilization Strategy

### Option 1: Use Demo Compose File (Recommended)
- Use `demo/docker-compose.demo.yml` which has minimal, tested configuration
- Separate demo environment from development environment
- Already configured for demo scenarios

### Option 2: Scale Down Current Environment
- Stop non-essential services
- Fix remaining issues
- Keep using main docker-compose.yml

---

## Recommended Approach: Use Demo Compose

**Advantages**:
- ✅ Pre-configured for demos
- ✅ Minimal resource usage
- ✅ Tested configuration
- ✅ Separate from dev environment
- ✅ Includes demo data seeding scripts

**Steps**:
1. Stop current services
2. Use demo compose file
3. Start demo services
4. Seed demo data
5. Verify all services healthy

---

## Next Steps

1. **Decide on approach** (Demo compose vs. scale down)
2. **Stop non-essential services** (if scaling down)
3. **Fix gateway service** (if using main compose)
4. **Verify required services healthy**
5. **Test demo scenarios**

---

**Status**: 📋 **PLANNING - AWAITING DECISION ON APPROACH**

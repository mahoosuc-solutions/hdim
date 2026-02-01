# Demo Stabilization - Ready to Execute

**Date**: January 15, 2026  
**Status**: ✅ **PLAN READY - AWAITING EXECUTION**

---

## Summary

**Current State**: 18 services running, some with issues  
**Target State**: 14 services (demo-required only)  
**Approach**: Use demo compose file for clean, tested environment

---

## Demo Service Requirements

### ✅ Required Services (14 total)

**Core Services** (6):
- gateway-service
- fhir-service  
- patient-service
- care-gap-service
- quality-measure-service
- cql-engine-service

**Infrastructure** (7):
- postgres
- redis
- zookeeper
- kafka
- elasticsearch
- jaeger (optional)
- prometheus + grafana (optional)

**Frontend** (1):
- clinical-portal

---

## Services to Stop (4+)

**Not Required for Demos**:
- consent-service
- ecr-service
- event-processing-service
- event-router-service
- prior-auth-service
- hcc-service (optional)
- notification-service (optional)
- backup

---

## Execution Plan

### Option A: Use Demo Compose (Recommended)
1. Stop current services: `docker compose down`
2. Start demo services: `cd demo && docker compose -f docker-compose.demo.yml up -d`
3. Seed data: `./seed-demo-data.sh`
4. Verify: Check all services healthy
5. Access: http://localhost:4200

**Advantages**: Clean, tested, avoids migration issues

### Option B: Scale Down Current
1. Stop non-essential services
2. Fix gateway service migration
3. Verify required services
4. Test demos

**Advantages**: Uses existing setup  
**Disadvantages**: Need to fix gateway service

---

## Recommendation

**Use Option A (Demo Compose)** - Cleaner, tested, avoids current issues

---

**Status**: ✅ **READY TO EXECUTE**

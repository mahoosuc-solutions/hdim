# Demo Services Stabilization Plan

**Date**: January 15, 2026  
**Status**: 📋 **PLANNING DEMO SERVICE REQUIREMENTS**

---

## Demo Scenarios Analysis

### Scenario 1: HEDIS Quality Measure Evaluation & Care Gaps
**Duration**: 3-5 minutes  
**Required Services**:
- ✅ Gateway Service (API routing)
- ✅ FHIR Service (FHIR data)
- ✅ Patient Service (Patient data)
- ✅ Quality Measure Service (HEDIS measures)
- ✅ Care Gap Service (Care gap identification)
- ✅ CQL Engine Service (Measure evaluation)
- ✅ Infrastructure: Postgres, Redis

### Scenario 2: Patient Care Journey
**Duration**: 4-6 minutes  
**Required Services**:
- ✅ Gateway Service
- ✅ FHIR Service
- ✅ Patient Service
- ✅ Care Gap Service
- ✅ CQL Engine Service
- ✅ Infrastructure: Postgres, Redis

### Scenario 3: Risk Stratification & Analytics
**Duration**: 3-4 minutes  
**Required Services**:
- ✅ Gateway Service
- ✅ Patient Service
- ✅ Analytics Service (Risk stratification)
- ✅ HCC Service (Risk scoring)
- ✅ Infrastructure: Postgres, Redis

### Scenario 4: Multi-Tenant Administration
**Duration**: 2-3 minutes  
**Required Services**:
- ✅ Gateway Service
- ✅ Infrastructure: Postgres, Redis

---

## Core Services Required for All Demos

### Essential Services (Must Have)
1. **postgres** - Database
2. **redis** - Caching
3. **gateway-service** - API Gateway
4. **fhir-service** - FHIR data
5. **patient-service** - Patient data
6. **care-gap-service** - Care gap identification
7. **quality-measure-service** - HEDIS measures
8. **cql-engine-service** - Measure evaluation

### Optional Services (Nice to Have)
9. **analytics-service** - For risk stratification demo
10. **hcc-service** - For risk scoring demo
11. **notification-service** - For notifications (if needed)

### Infrastructure Services
- **kafka** - Message queue (if services require it)
- **zookeeper** - Kafka dependency
- **jaeger** - Tracing (optional for demos)

---

## Services to Stop (Not Required for Demos)

Based on demo requirements, these services can be stopped:
- consent-service
- ecr-service
- event-processing-service
- event-router-service
- prior-auth-service
- documentation-service
- sdoh-service
- approval-service
- (and any others not listed above)

---

## Next Steps

1. Identify all currently running services
2. Stop non-essential services
3. Verify required services are healthy
4. Test demo scenarios
5. Document final service list

---

**Status**: 📋 **PLANNING IN PROGRESS**

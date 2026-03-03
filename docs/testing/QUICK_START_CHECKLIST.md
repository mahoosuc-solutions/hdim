# HDIM Development Work - Quick Start Checklist

**Last Updated**: January 13, 2026  
**Status**: Production-ready core, demo/audit features incomplete

---

## HIGH PRIORITY - DO FIRST (20-29 hours)

### Phase 1.1: Demo Seeding Service (4-6 hours)
**File**: `backend/modules/services/demo-seeding-service/src/main/java/com/healthdata/demo/generator/SyntheticPatientGenerator.java`

- [ ] Line 316: Implement `generateMedications()` - Create meds based on patient conditions
- [ ] Line 321: Implement `generateObservations()` - Generate vital signs, lab results
- [ ] Line 325: Implement `generateEncounters()` - Create visit/encounter history
- [ ] Line 330: Implement `generateProcedures()` - Add preventive procedures
- [ ] Lines 336-350: Implement template-based generation methods
- [ ] Test: Verify synthetic patients have realistic FHIR data

**Why**: Blocks demo platform from functioning. Critical for sales/marketing.

---

### Phase 1.2: AI Audit Metrics (10-15 hours)
**Files**: 
- `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/service/ai/AIAuditEventStore.java`
- `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/service/clinical/ClinicalDecisionService.java`

**AIAuditEventStore.java**:
- [ ] Line 71: Implement `updateAIDecisionMetrics()` - Track decision counts, confidence scores
- [ ] Line 82: Implement `analyzeDecisionPattern()` - Detect anomalies in AI decisions
- [ ] Add: Configuration history tracking
- [ ] Add: Performance impact monitoring
- [ ] Add: Alerting on low-confidence decisions
- [ ] Add: User behavior tracking

**ClinicalDecisionService.java**:
- [ ] Line 177: Calculate `overrideRate` from database data
- [ ] Line 179: Calculate `averageReviewTimeHours` from timestamps
- [ ] Line 183-184: Implement per-type metrics calculation
- [ ] Test: Verify metrics dashboard displays real data

**Why**: Cannot audit AI system performance. Critical for compliance.

---

### Phase 1.3: Decision Replay Service (6-8 hours)
**File**: `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/service/ai/DecisionReplayService.java`

- [ ] Line ~40: TODO comment says "Integrate with actual AI agent services"
- [ ] Implement replay logic: Call AI service with original inputs
- [ ] Implement validation: Compare original output with replay output
- [ ] Implement drift detection: Flag decisions that would change
- [ ] Add: Audit logging of replay operations
- [ ] Test: Verify decision replay accuracy

**Why**: Cannot investigate suspicious AI decisions. Required for regulatory compliance.

---

## MEDIUM PRIORITY - DO NEXT (20-30 hours)

### Phase 2.1: Care Gap & CQL Audit Integration (2-3 hours)
**Files**:
- `backend/modules/services/care-gap-service/src/main/java/com/healthdata/caregap/service/CareGapAuditIntegration.java` (line visible)
- `backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/service/CqlEvaluationService.java` (line 123)

- [ ] CareGapAuditIntegration: Implement TODO for configuration engine event publishing
- [ ] CqlEvaluationService line 123: Replace "system" with actual user from SecurityContext
- [ ] Test: Verify audit logs show correct user

**Why**: Improves audit completeness. Easy wins.

---

### Phase 2.2: Fix Disabled E2E Tests (3-4 hours each)
**Files**:
- `backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/integration/CareGapDetectionE2ETest.java`
- `backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/integration/EndToEndIntegrationTest.java`

- [ ] Remove @Disabled annotation
- [ ] Use Testcontainers for PostgreSQL, Kafka, Redis
- [ ] Verify tests pass locally
- [ ] Add to CI/CD pipeline

**Why**: Need full E2E test coverage in CI/CD.

---

### Phase 2.3: Database-Config Migration - HIGH Tier (4-6 hours)
**Services** (critical path):
- fhir-service
- quality-measure-service
- cql-engine-service

**Steps per service**:
1. [ ] Add to `build.gradle.kts`: `implementation(project(":modules:shared:infrastructure:database-config"))`
2. [ ] Update `application.yml`: Remove manual HikariCP config
3. [ ] Add: `healthdata.database.hikari.traffic-tier: HIGH`
4. [ ] Test: Service starts, no connection pool errors
5. [ ] Verify: Startup logs show pool configuration

**Guide**: See `backend/docs/DATABASE_CONFIG_ADOPTION_GUIDE.md`

**Why**: Prevents connection pool exhaustion in production.

---

### Phase 2.4: HL7/CDA Test Coverage (3-4 hours)
**Files**:
- `backend/modules/services/cdr-processor-service/src/test/java/com/healthdata/cdr/converter/Hl7ToFhirConverterTest.java`
- `backend/modules/services/cdr-processor-service/src/test/java/com/healthdata/cdr/parser/Hl7v2ParserServiceTest.java`

- [ ] Replace placeholder message types (XXX, Y01) with real HL7 codes
- [ ] Add tests for common HL7 message types (ADT, ORM, ORU, RGV)
- [ ] Add tests for CDA document sections
- [ ] Verify: 95%+ test coverage for converters

**Why**: Prevent data loss in EHR integrations.

---

## LOW PRIORITY - LATER (15-25 hours)

### Phase 3.1: Database-Config Migration - Remaining 25 Services
- [ ] patient-service, care-gap-service, event-router-service (MEDIUM - 10 services)
- [ ] approval-service, audit-service, ... (LOW - 10+ services)
- [ ] Can parallelize across team

**Effort**: 30-60 min per service

---

### Phase 3.2: Clean Up Demo Service
**Files**:
- `backend/modules/services/demo-seeding-service/src/main/java/com/healthdata/demo/application/DemoVerificationService.java`
- `backend/modules/services/demo-seeding-service/src/main/java/com/healthdata/demo/strategy/PatientJourneyStrategy.java`

- [ ] Implement DemoVerificationService FHIR calls (4 TODOs)
- [ ] Implement named persona generation
- [ ] Complete AI CQL generation prompts
- [ ] Implement pre-visit planning batch endpoint

**Effort**: 2-3 hours each

---

### Phase 3.3: Code Cleanup
- [ ] Remove deprecated JWT authentication code (cql-engine-service)
- [ ] Complete notification service migration
- [ ] Add Zoho sync background jobs

**Effort**: 1-2 hours

---

## VERIFICATION CHECKLIST

Before marking complete:

- [ ] All tests pass: `./gradlew test`
- [ ] No new TODO/FIXME comments introduced
- [ ] Code follows patterns in CLAUDE.md
- [ ] HIPAA compliance verified (if touching PHI)
- [ ] Changes documented in commit message
- [ ] Database migrations have rollback SQL (if applicable)

---

## QUICK COMMANDS

### Build & Test
```bash
cd backend
./gradlew build
./gradlew test
./gradlew :modules:services:demo-seeding-service:build
```

### Find TODOs
```bash
grep -r "TODO\|FIXME" backend/modules/services/demo-seeding-service --include="*.java"
grep -r "TODO\|FIXME" backend/modules/shared/infrastructure/audit --include="*.java"
```

### Run Demo Service
```bash
./gradlew :modules:services:demo-seeding-service:bootRun
```

### Database-Config Guide
See: `backend/docs/DATABASE_CONFIG_ADOPTION_GUIDE.md`

---

## Questions?

- **Architecture**: See `CLAUDE.md` section "System Architecture"
- **HIPAA Compliance**: See `backend/HIPAA-CACHE-COMPLIANCE.md`
- **Testing**: See `docs/TESTING_GUIDE.md`
- **Database**: See `backend/docs/ENTITY_MIGRATION_GUIDE.md`

---

## Status Tracking

- Total effort: 65-125 hours
- Critical path: 20-29 hours (Phase 1)
- Parallelizable: Database-config migration (can split 25 services across team)
- Blocker items: Demo seeding, AI audit metrics, decision replay

**Recommendation**: Start with Phase 1 items immediately. Block demo launches until complete.


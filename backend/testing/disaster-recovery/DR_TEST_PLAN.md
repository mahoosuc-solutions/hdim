# Annual Disaster Recovery Test Plan
**HDIM Platform - HealthData-in-Motion**

**Document Version:** 1.0
**Last Updated:** 2026-01-10
**HIPAA Reference:** §164.308(a)(7)(ii)(B) - Disaster Recovery Plan Testing
**Review Cycle:** Annual
**Next Scheduled Test:** January 1, 2027

---

## Executive Summary

This document defines the annual disaster recovery (DR) testing procedures for the HDIM healthcare interoperability platform. The purpose of DR testing is to:

1. **Validate Recovery Capabilities** - Verify that critical systems can be recovered from backups
2. **Measure RTO/RPO** - Confirm Recovery Time Objective (4 hours) and Recovery Point Objective (1 hour) targets are achievable
3. **Identify Gaps** - Discover weaknesses in DR procedures before a real disaster occurs
4. **HIPAA Compliance** - Satisfy §164.308(a)(7)(ii)(B) requirement for annual DR plan testing
5. **Continuous Improvement** - Update DR procedures based on test findings

**Scope:** All 28 microservices, 29 databases, supporting infrastructure (PostgreSQL, Redis, Kafka), and PHI data recovery.

**Test Date:** Annually on January 1st at 00:00 UTC (automated via GitHub Actions)

**Test Environment:** Isolated Docker Compose stack separate from production

---

## RTO/RPO Targets

### Recovery Time Objective (RTO)
**Target:** 4 hours (14,400 seconds)
**Definition:** Maximum acceptable time to restore critical services after a disaster
**Measurement:** Time from disaster initiation to full service operational status

**Breakdown by Component:**
| Component | RTO Target | Priority |
|-----------|------------|----------|
| PostgreSQL Database | 30 minutes | P0 - Critical |
| Core Services (gateway, patient, fhir, quality, cql) | 1 hour | P0 - Critical |
| Supporting Services (analytics, reports, etc.) | 2 hours | P1 - High |
| All 28 Services | 4 hours | P2 - Standard |

### Recovery Point Objective (RPO)
**Target:** 1 hour (3,600 seconds)
**Definition:** Maximum acceptable data loss measured in time
**Measurement:** Time difference between last successful backup and disaster occurrence

**Backup Schedule:**
- **Transaction logs**: Continuous replication (near-zero RPO for critical data)
- **Database backups**: Hourly automated backups
- **Configuration backups**: Daily snapshots
- **Code/Infrastructure**: Version-controlled (Git), zero data loss

---

## Test Scenarios

### Scenario 1: Database Corruption/Loss
**Probability:** Medium
**Impact:** HIGH - Complete loss of patient data
**Test Frequency:** Annual

**Objective:** Verify ability to restore all 29 databases from backup within RTO

**Steps:**
1. Generate synthetic PHI test data across all databases
2. Create full database backups using `pg_dump`
3. Simulate database corruption (DROP DATABASE)
4. Restore databases from backup
5. Validate data integrity (row counts, checksums)
6. Verify Liquibase migration state

**Success Criteria:**
- ✅ All 29 databases restored successfully
- ✅ Data integrity validated (100% match)
- ✅ Restore time < 30 minutes (RTO target)
- ✅ Zero data loss (RPO < 1 hour)
- ✅ All services connect to restored databases

**Failure Handling:**
- Document restore time if exceeds RTO
- Identify bottlenecks (network, disk I/O, parallelization)
- Update backup/restore procedures

---

### Scenario 2: Critical Service Outage
**Probability:** High
**Impact:** MEDIUM - Service degradation, API unavailability
**Test Frequency:** Annual

**Objective:** Verify service failover and recovery procedures

**Steps:**
1. Start all 28 services in DR test environment
2. Validate baseline health (all services green)
3. Simulate failure of 5 critical services:
   - gateway-service (authentication)
   - patient-service (patient data API)
   - fhir-service (FHIR R4 endpoints)
   - quality-measure-service (HEDIS evaluation)
   - cql-engine-service (CQL execution)
4. Measure time to detect failure (monitoring alerts)
5. Restart failed services using Docker Compose
6. Validate service recovery (health checks green)

**Success Criteria:**
- ✅ Failure detected within 1 minute (Prometheus alerts)
- ✅ Services restart successfully
- ✅ All health checks pass after restart
- ✅ Total recovery time < 15 minutes
- ✅ No data loss during service restart

**Failure Handling:**
- Document detection delay if > 1 minute
- Review alerting configuration
- Update service restart procedures

---

### Scenario 3: Backup Integrity Validation
**Probability:** N/A (Preventive)
**Impact:** CRITICAL - Corrupt backups = unrecoverable data
**Test Frequency:** Annual

**Objective:** Verify backup integrity and restorability

**Steps:**
1. Create fresh backups of all 29 databases
2. Generate checksums (SHA-256) for all backup files
3. Verify checksums match
4. Restore backups to temporary database instances
5. Compare restored data with source databases (pg_dump diff)
6. Validate Liquibase changelog state

**Success Criteria:**
- ✅ All checksums match (no file corruption)
- ✅ All backups restore successfully
- ✅ Restored data matches source (100% accuracy)
- ✅ Liquibase migration state valid
- ✅ No restore errors or warnings

**Failure Handling:**
- Identify corrupted backups
- Re-create backups with validated integrity
- Review backup storage (compression, encryption)

---

### Scenario 4: Complete Infrastructure Failure
**Probability:** Low
**Impact:** CRITICAL - Full system unavailability
**Test Frequency:** Annual

**Objective:** Simulate catastrophic failure and full disaster recovery

**Steps:**
1. Start production-like environment with all services
2. Generate synthetic PHI data (patients, observations, conditions)
3. Record baseline timestamp (RPO marker)
4. Create full system backup (databases, configurations)
5. Simulate catastrophic failure:
   - Stop all Docker containers
   - Delete all volumes (`docker compose down -v`)
   - Simulate infrastructure loss
6. Restore from scratch:
   - Recreate Docker environment
   - Restore all 29 databases
   - Start all 28 services
   - Validate service health
7. Measure total recovery time (RTO)
8. Calculate data loss window (RPO)

**Success Criteria:**
- ✅ Complete infrastructure rebuild successful
- ✅ All 29 databases restored
- ✅ All 28 services operational
- ✅ Health checks pass for all services
- ✅ Total recovery time < 4 hours (RTO)
- ✅ Data loss < 1 hour (RPO)

**Failure Handling:**
- Document actual RTO vs target
- Identify slowest recovery components
- Update DR runbook procedures
- Review infrastructure automation

---

## Pre-Test Preparation

### 2 Weeks Before Test

- [ ] Review and update DR test plan
- [ ] Verify backup infrastructure operational
- [ ] Confirm test environment resources available
- [ ] Schedule notification to stakeholders
- [ ] Review roles and responsibilities
- [ ] Update contact list (on-call personnel)

### 1 Week Before Test

- [ ] Verify Docker host capacity (disk, CPU, memory)
- [ ] Test backup/restore utilities in isolation
- [ ] Create test data generation scripts
- [ ] Validate GitHub Actions workflow
- [ ] Confirm monitoring stack operational (Prometheus, Grafana)

### 1 Day Before Test

- [ ] Final review of test procedures
- [ ] Ensure no conflicting maintenance windows
- [ ] Verify backup retention (last 7 days available)
- [ ] Test notification channels (Slack, email)
- [ ] Create test execution checklist

### Test Day (January 1st)

- [ ] Automated execution via GitHub Actions (00:00 UTC)
- [ ] Monitor test progress (GitHub Actions logs)
- [ ] Review test results upon completion
- [ ] Generate compliance report
- [ ] Archive test artifacts

---

## Test Execution Procedure

### Automated Execution (Recommended)

**Trigger:** GitHub Actions workflow runs automatically on January 1st at 00:00 UTC

**Steps:**
1. GitHub Actions workflow starts
2. Docker environment provisioned
3. Test suite executes sequentially:
   - Test 1: Database Restore (30 min)
   - Test 2: Service Failover (15 min)
   - Test 3: Backup Integrity (45 min)
   - Test 4: Full Disaster Simulation (2 hours)
4. Test report generated
5. Artifacts uploaded (logs, metrics, report)
6. Notification sent on completion/failure

**Monitoring:** View progress at https://github.com/{org}/hdim-master/actions

---

### Manual Execution (Fallback)

**Prerequisites:**
- Docker and Docker Compose installed
- Access to GitHub repository
- Database credentials configured

**Steps:**
```bash
# 1. Navigate to DR testing directory
cd backend/testing/disaster-recovery

# 2. Review environment configuration
cat .env.dr-test

# 3. Execute master test runner
./scripts/run-dr-test.sh

# 4. Monitor execution
tail -f reports/dr-test-$(date +%Y%m%d)*.log

# 5. Review test report
cat reports/$(ls -t reports/ | head -1)
```

**Execution Time:** Approximately 4 hours

---

## Roles & Responsibilities

### DR Test Coordinator
**Role:** Overall test planning and execution
**Responsibilities:**
- Schedule annual DR test
- Review test plan and update as needed
- Coordinate with stakeholders
- Ensure test environment prepared
- Monitor test execution
- Generate compliance documentation

**Personnel:** DevOps Lead or Security Officer

---

### Technical Lead
**Role:** Execute DR test procedures
**Responsibilities:**
- Configure test environment
- Execute test scripts
- Monitor service health during tests
- Troubleshoot failures
- Document technical findings

**Personnel:** Platform Engineer or SRE

---

### QA/Validation Lead
**Role:** Validate test results
**Responsibilities:**
- Verify data integrity after restore
- Validate service functionality
- Confirm RTO/RPO measurements accurate
- Review test report for completeness

**Personnel:** QA Engineer or Test Lead

---

### Compliance Officer
**Role:** Ensure HIPAA compliance
**Responsibilities:**
- Review test procedures for PHI handling
- Validate audit logging during tests
- Approve test report for compliance attestation
- Archive test documentation

**Personnel:** Security Officer or Compliance Manager

---

## Success Criteria

### Overall Test Success

The DR test is considered **PASSED** if ALL of the following criteria are met:

1. **All Test Scenarios Pass:**
   - ✅ Test 1: Database Restore - PASS
   - ✅ Test 2: Service Failover - PASS
   - ✅ Test 3: Backup Integrity - PASS
   - ✅ Test 4: Full Disaster Simulation - PASS

2. **RTO Target Achieved:**
   - Database restore: < 30 minutes
   - Critical services: < 1 hour
   - All services: < 4 hours

3. **RPO Target Achieved:**
   - Data loss < 1 hour
   - Zero data loss for transaction log backups

4. **Data Integrity Validated:**
   - 100% data match after restore
   - Liquibase migration state valid
   - No database corruption errors

5. **Service Health Validated:**
   - All 28 services pass health checks
   - API endpoints functional
   - Authentication operational

6. **Documentation Complete:**
   - Test report generated
   - RTO/RPO metrics recorded
   - Findings documented
   - Recommendations provided

---

## Failure Handling

### If Test Fails

**Immediate Actions:**
1. Stop test execution to prevent further issues
2. Preserve test environment for analysis
3. Document failure point and error messages
4. Notify DR Test Coordinator and Technical Lead

**Analysis:**
1. Review test logs for root cause
2. Identify specific component that failed
3. Reproduce failure in isolated environment
4. Determine if failure is:
   - Script/automation issue
   - Infrastructure limitation
   - DR procedure gap
   - Expected behavior requiring procedure update

**Remediation:**
1. Fix identified issues
2. Update DR test scripts as needed
3. Improve DR runbook procedures
4. Re-test failed scenario in isolation
5. Schedule full re-test within 30 days

**Compliance:**
- Document failure in DR test report
- Include remediation plan
- Escalate to management if RTO/RPO targets not achievable
- Update DR procedures before next annual test

---

## Post-Test Activities

### Immediate (Within 24 hours)

- [ ] Review automated test report
- [ ] Validate RTO/RPO measurements
- [ ] Document any anomalies or failures
- [ ] Clean up test environment (docker compose down -v)
- [ ] Archive test artifacts to secure storage

### Short-Term (Within 1 week)

- [ ] Conduct post-test review meeting
- [ ] Present findings to management
- [ ] Identify improvement opportunities
- [ ] Create action items for DR procedure updates
- [ ] Update DR runbook based on findings
- [ ] Review and update RTO/RPO targets if needed

### Long-Term (Within 30 days)

- [ ] Implement DR procedure improvements
- [ ] Update CLAUDE.md with lessons learned
- [ ] Train team on updated procedures
- [ ] Document compliance attestation
- [ ] Archive DR test report for HIPAA audit
- [ ] Update next year's test plan

---

## Metrics & Reporting

### Key Metrics Tracked

1. **Recovery Time Objective (RTO)**
   - Database restore time
   - Service restart time
   - Full system recovery time

2. **Recovery Point Objective (RPO)**
   - Time of last successful backup
   - Data loss window
   - Transaction log gap

3. **Success Rate**
   - Test scenarios passed vs total
   - Service recovery success rate
   - Data integrity validation pass rate

4. **Availability**
   - Service uptime during failover
   - Time to detection (MTTD)
   - Time to recovery (MTTR)

### Report Generation

**Format:** Markdown document
**Template:** `templates/DR_TEST_REPORT_TEMPLATE.md`
**Location:** `reports/{YYYY-MM-DD-HHMMSS}-dr-test-report.md`

**Contents:**
- Executive summary (pass/fail)
- Test scenario results
- RTO/RPO measurements
- Issues identified
- Recommendations
- HIPAA compliance attestation

**Distribution:**
- DR Test Coordinator
- Technical Lead
- Compliance Officer
- Management (summary)
- Audit file (HIPAA compliance)

---

## Test Environment Specifications

### Infrastructure

**Docker Host:**
- CPU: 8+ cores
- RAM: 32GB+
- Disk: 500GB+ SSD
- Network: 1Gbps+

**Services:**
- 28 microservices (HDIM platform)
- PostgreSQL 16 (29 databases)
- Redis 7 (cache)
- Apache Kafka 3.x (messaging)

**Network:**
- Isolated Docker network: `dr-test-network`
- Port offset: +10000 (e.g., production 5435 → test 15435)
- No external connectivity required

### Data

**Test Data Volume:**
- Patients: 10,000 synthetic records
- Observations: 100,000 FHIR resources
- Conditions: 50,000 clinical conditions
- Database size: ~5GB total

**PHI Compliance:**
- ⚠️ **IMPORTANT:** Use ONLY synthetic/fictitious PHI data
- ❌ **NEVER** use real patient data in DR tests
- ✅ Generate FHIR-compliant but fictional data
- ✅ Scrub all test data after completion

---

## Compliance & Audit

### HIPAA Requirements

**§164.308(a)(7)(ii)(B) - Disaster Recovery Plan Testing:**
> Establish (and implement as needed) procedures for periodic testing and revision of contingency plans.

**Compliance Evidence:**
- ✅ Annual DR test execution (documented)
- ✅ Test plan review and updates (version-controlled)
- ✅ Test report with RTO/RPO measurements
- ✅ Management review and approval
- ✅ Lessons learned and procedure updates

### Audit Trail

**Documents Retained:**
- DR test plan (current version)
- DR test reports (last 7 years)
- Test execution logs
- Post-test review notes
- DR procedure updates

**Storage:**
- GitHub repository (private, encrypted)
- HIPAA-compliant file storage
- Access restricted to authorized personnel

---

## Appendix

### A. Database List (29 databases)

| Database | Service | Priority |
|----------|---------|----------|
| `gateway_db` | Authentication | P0 - Critical |
| `patient_db` | Patient Service | P0 - Critical |
| `fhir_db` | FHIR Service | P0 - Critical |
| `quality_db` | Quality Measure | P0 - Critical |
| `cql_db` | CQL Engine | P0 - Critical |
| `caregap_db` | Care Gap Service | P1 - High |
| ... (24 more) | Various Services | P1-P2 |

### B. Service List (28 services)

| Service | Port | Health Endpoint |
|---------|------|-----------------|
| gateway-service | 8080 | /actuator/health |
| patient-service | 8084 | /actuator/health |
| fhir-service | 8085 | /actuator/health |
| quality-measure-service | 8087 | /actuator/health |
| cql-engine-service | 8081 | /actuator/health |
| ... (23 more) | Various | /actuator/health |

### C. Contact List

| Role | Name | Email | Phone |
|------|------|-------|-------|
| DR Test Coordinator | [TBD] | [TBD] | [TBD] |
| Technical Lead | [TBD] | [TBD] | [TBD] |
| Compliance Officer | [TBD] | [TBD] | [TBD] |
| On-Call Engineer | [TBD] | [TBD] | [TBD] |

### D. Useful Commands

**Backup:**
```bash
./scripts/backup-all-databases.sh
```

**Restore:**
```bash
./scripts/restore-all-databases.sh /path/to/backup
```

**Health Check:**
```bash
./scripts/health-check.sh
```

**Full DR Test:**
```bash
./scripts/run-dr-test.sh
```

---

**Document Owner:** DevOps Team
**Approval Required:** CTO, Compliance Officer, Security Officer
**Next Review:** December 1, 2026
**Distribution:** Internal - HIPAA Confidential

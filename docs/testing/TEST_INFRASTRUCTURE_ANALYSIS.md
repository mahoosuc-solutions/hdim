================================================================================
HDIM TEST INFRASTRUCTURE ANALYSIS - SLOW TEST IDENTIFICATION REPORT
Generated: 2026-02-01
================================================================================

EXECUTIVE SUMMARY
================================================================================

The HDIM backend contains 613 test files with significant performance bottlenecks:

CRITICAL FINDINGS:
  • 11 heavyweight tests with Kafka polling & Thread.sleep() calls
  • 6 tests marked @Disabled (Kafka/PostgreSQL dependencies)
  • 24 tests using Thread.sleep() for synchronization (anti-pattern)
  • 69 tests using @Testcontainers (Docker startup overhead)
  • 507 tests exceeding 100 lines (complex/slow execution)
  • Top 5 tests exceed 900 lines each

IMPACT: Combined test execution time estimated at 2-4 hours; CI/CD bottleneck

================================================================================
1. DISABLED TESTS (6 TESTS - CURRENTLY NOT RUNNING)
================================================================================

1.1 KAFKA-DEPENDENT TESTS (3 TESTS)
────────────────────────────────────────────────────────────────────────────

Test: CareGapAuditPerformanceTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/service/CareGapAuditPerformanceTest.java
Reason: Kafka-dependent tests disabled for local H2 testing. See class documentation for KRaft implementation plan.
Status: @Disabled
Impact: Performance testing of care gap audit stream unavailable

Test: CareGapAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/service/CareGapAuditIntegrationHeavyweightTest.java
Reason: Kafka-dependent tests disabled for local H2 testing. See class documentation for KRaft implementation plan.
Status: @Disabled
Impact: Care gap audit integration testing unavailable

Test: CdrProcessorAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cdr-processor-service/src/test/java/com/healthdata/cdr/audit/CdrProcessorAuditIntegrationHeavyweightTest.java
Lines: 497
Reason: Phase 1: Kafka Docker dependency removed - requires KRaft implementation for Phase 2
Status: @Disabled
Impact: CDR processor audit integration testing unavailable

1.2 PRE-EXISTING TEST ISSUES (2 TESTS)
────────────────────────────────────────────────────────────────────────────

Test: OAuth2IntegrationTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cms-connector-service/src/test/java/com/healthdata/cms/auth/OAuth2IntegrationTest.java
Reason: @RestClientTest does not provide RestTemplate bean for OAuth2Manager
Status: @Disabled
Fix: Update OAuth2Manager to use RestClient or provide RestTemplate mock

Test: OAuth2ManagerTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cms-connector-service/src/test/java/com/healthdata/cms/auth/OAuth2ManagerTest.java
Reason: Tests mock postForObject() but implementation uses exchange()
Status: @Disabled
Fix: Update test mocks to match actual OAuth2Manager.exchange() usage

1.3 DATABASE-SPECIFIC TESTS (1 TEST)
────────────────────────────────────────────────────────────────────────────

Test: EntityMigrationValidationTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cdr-processor-service/src/test/java/com/healthdata/cdr/integration/EntityMigrationValidationTest.java
Reason: Phase 2: Skipped in H2 tests - requires PostgreSQL for JSONB/ENUM types
Status: @Disabled
Impact: CDR processor entity-migration validation requires PostgreSQL with JSONB support

================================================================================
2. HEAVYWEIGHT TESTS - ACTIVE (11 TESTS)
================================================================================

These tests use Kafka polling, Docker containers, and Thread.sleep() calls.
Expected execution time: 10-30 seconds each = 2-5 minutes total

────────────────────────────────────────────────────────────────────────────
TIER 1: HEAVIEST TESTS (>450 LINES + KAFKA POLLING)
────────────────────────────────────────────────────────────────────────────

Test 1: PayerWorkflowsAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/payer-workflows-service/src/test/java/com/healthdata/payer/audit/PayerWorkflowsAuditIntegrationHeavyweightTest.java
Lines: 471
Thread.sleep() calls: 14 ⚠️ CRITICAL
Kafka poll() calls: 10
Est. execution time: 15-25 seconds
Features: Kafka consumer loop, message processing, audit event validation
Bottlenecks: Multiple Thread.sleep() waits in Kafka polling loops

Test 2: CdrProcessorAuditIntegrationHeavyweightTest (DISABLED - See section 1.1)
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cdr-processor-service/src/test/java/com/healthdata/cdr/audit/CdrProcessorAuditIntegrationHeavyweightTest.java
Lines: 497
Thread.sleep() calls: 8 ⚠️ HIGH
Kafka poll() calls: 6
Est. execution time: N/A (disabled)
Reason: Requires Kafka Docker container (Phase 2 KRaft implementation)

────────────────────────────────────────────────────────────────────────────
TIER 2: CROSS-SERVICE E2E TESTS (>350 LINES + THREAD.SLEEP)
────────────────────────────────────────────────────────────────────────────

Test 3: ClinicalDecisionAuditE2ETest
File: /mnt/wdblack/dev/projects/hdim-master/backend/testing/cross-service-audit/src/test/java/com/healthdata/testing/audit/ClinicalDecisionAuditE2ETest.java
Lines: 397
Thread.sleep() calls: 13 ⚠️ CRITICAL
Kafka poll() calls: 6
Est. execution time: 10-20 seconds
Features: Multi-service audit trail validation, Kafka message consumption
Bottlenecks: 13x Thread.sleep() waits for async event processing

Test 4: ApprovalAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/approval-service/src/test/java/com/healthdata/approval/audit/ApprovalAuditIntegrationHeavyweightTest.java
Lines: 397
Thread.sleep() calls: 13 ⚠️ CRITICAL
Kafka poll() calls: 9
Est. execution time: 10-20 seconds
Features: Approval workflow audit testing, event sourcing
Bottlenecks: 13x Thread.sleep() in polling loops

Test 5: EhrConnectorAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/ehr-connector-service/src/test/java/com/healthdata/ehr/audit/EhrConnectorAuditIntegrationHeavyweightTest.java
Lines: 353
Thread.sleep() calls: 7 ⚠️ HIGH
Kafka poll() calls: 8
Est. execution time: 10-15 seconds
Features: EHR connector Kafka audit events, FHIR resource processing
Bottlenecks: 7x Thread.sleep() in event consumption

────────────────────────────────────────────────────────────────────────────
TIER 3: MODERATE HEAVYWEIGHT TESTS (250-350 LINES + POLLING)
────────────────────────────────────────────────────────────────────────────

Test 6: PriorAuthAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/prior-auth-service/src/test/java/com/healthdata/priorauth/audit/PriorAuthAuditIntegrationHeavyweightTest.java
Lines: 333
Thread.sleep() calls: 9 ⚠️ HIGH
Kafka poll() calls: 8
Est. execution time: 8-12 seconds

Test 7: CqlAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cql-engine-service/src/test/java/com/healthdata/cql/service/CqlAuditIntegrationHeavyweightTest.java
Lines: 286
Thread.sleep() calls: 3
Kafka poll() calls: 4
Est. execution time: 5-10 seconds

Test 8: HIPAAAuditComplianceTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/testing/cross-service-audit/src/test/java/com/healthdata/testing/audit/HIPAAAuditComplianceTest.java
Lines: 298
Thread.sleep() calls: 11 ⚠️ HIGH
Kafka poll() calls: 5
Est. execution time: 8-12 seconds

────────────────────────────────────────────────────────────────────────────
TIER 4: LIGHTWEIGHT HEAVYWEIGHT TESTS (<250 LINES + MINIMAL POLLING)
────────────────────────────────────────────────────────────────────────────

Test 9: AgentRuntimeAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/agent-runtime-service/src/test/java/com/healthdata/agent/audit/AgentRuntimeAuditIntegrationHeavyweightTest.java
Lines: 367
Thread.sleep() calls: 3
Kafka poll() calls: 4
Est. execution time: 5-8 seconds

Test 10: CareGapAuditIntegrationHeavyweightTest
File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/service/CareGapAuditIntegrationHeavyweightTest.java
Lines: 193
Thread.sleep() calls: 3
Kafka poll() calls: 4
Est. execution time: 5-8 seconds
Status: @Disabled (see section 1.1)

================================================================================
3. TESTS WITH THREAD.SLEEP() - SLOW SYNCHRONIZATION PATTERN
================================================================================

Critical Issue: Using Thread.sleep() for synchronization is an anti-pattern that:
  ✗ Makes tests slow and unreliable
  ✗ Wastes CPU cycles (fixed delays instead of event-based waiting)
  ✗ Creates flaky tests (timing assumptions break under load)
  ✓ Solution: Use CountDownLatch, BDD async, Awaitility, or TestContainers wait strategies

24 tests use Thread.sleep() for synchronization:

CRITICAL (>10 calls):
  1. PayerWorkflowsAuditIntegrationHeavyweightTest - 14 calls (see section 2)
  2. ClinicalDecisionAuditE2ETest - 13 calls (see section 2)
  3. ApprovalAuditIntegrationHeavyweightTest - 13 calls (see section 2)
  4. HIPAAAuditComplianceTest - 11 calls (see section 2)

HIGH (5-9 calls):
  5. EmailNotificationServiceTest - 6 calls
     File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/approval-service/src/test/java/com/healthdata/approval/notification/EmailNotificationServiceTest.java
     Lines: 567
  6. PopulationBatchCalculationE2ETest - 5 calls
     File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/integration/PopulationBatchCalculationE2ETest.java
     Lines: 654
  7. EhrConnectorAuditIntegrationHeavyweightTest - 7 calls (see section 2)
  8. PriorAuthAuditIntegrationHeavyweightTest - 9 calls (see section 2)
  9. CacheIsolationSecurityE2ETest - 4 calls
     File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/patient-service/src/test/java/com/healthdata/patient/integration/CacheIsolationSecurityE2ETest.java
     Lines: 485
  10. DataIntegrityIntegrationTest - 3 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cql-engine-service/src/test/java/com/healthdata/cql/integration/DataIntegrityIntegrationTest.java
      Lines: 478
  11. ServiceLayerIntegrationTest - 3 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cql-engine-service/src/test/java/com/healthdata/cql/integration/ServiceLayerIntegrationTest.java
      Lines: 450

MODERATE (1-3 calls):
  12. AgentOrchestratorTest - 2 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/agent-runtime-service/src/test/java/com/healthdata/agent/core/AgentOrchestratorTest.java
      Lines: 911
  13. WebhookCallbackServiceTest - 3 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/approval-service/src/test/java/com/healthdata/approval/webhook/WebhookCallbackServiceTest.java
      Lines: 464
  14. ConcurrentReplayTest - 3 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/event-replay-service/src/test/java/com/healthdata/eventreplay/concurrent/ConcurrentReplayTest.java
      Lines: 463
  15. PopulationCalculationServiceTest - 3 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/service/PopulationCalculationServiceTest.java
      Lines: 456
  16. ApprovalEventPublisherTest - 2 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/approval-service/src/test/java/com/healthdata/approval/event/ApprovalEventPublisherTest.java
      Lines: 400
  17. CQRSEventFlowIntegrationTest - 2 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/patient-event-service/src/test/java/com/healthdata/patientevent/integration/CQRSEventFlowIntegrationTest.java
      Lines: 414
  18. MllpSourceConnectorTest - 1 call
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/migration-workflow-service/src/test/java/com/healthdata/migration/connector/MllpSourceConnectorTest.java
  19. PatientDemographicsRepositoryIntegrationTest - 2 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/patient-service/src/test/java/com/healthdata/patient/integration/PatientDemographicsRepositoryIntegrationTest.java
  20. RouteMetricsServiceTest - 2 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/event-router-service/src/test/java/com/healthdata/eventrouter/service/RouteMetricsServiceTest.java
  21. DataFlowTrackerTest - 1 call
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cql-engine-service/src/test/java/com/healthdata/cql/audit/DataFlowTrackerTest.java
  22. EventProcessingMetricsTest - 1 call
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/event-processing-service/src/test/java/com/healthdata/events/metrics/EventProcessingMetricsTest.java
  23. TenantRateLimitIntegrationTest - 3 calls
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/infrastructure/gateway-core/src/test/java/com/healthdata/gateway/ratelimit/TenantRateLimitIntegrationTest.java
  24. ApiKeyTest - 1 call
      File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/infrastructure/authentication/src/test/java/com/healthdata/authentication/entity/ApiKeyTest.java
      Lines: 492

================================================================================
4. @TESTCONTAINERS TESTS (69 TESTS)
================================================================================

Tests using Docker containers with overhead:

Services with highest @Testcontainers usage:
  • Clinical Workflow Service: 12+ tests
  • Quality Measure Service: 11+ tests
  • Patient Service: 9+ tests
  • Payer Workflows Service: 8+ tests
  • Approval Service: 7+ tests
  • Care Gap Service: 6+ tests
  • Other services: ~16+ tests

Total: 69 tests using Docker for integration testing

Performance Impact:
  • First test startup: 30-60 seconds (Docker image initialization)
  • Per-test overhead: 5-10 seconds (container setup/teardown)
  • Total estimated: 10+ minutes for all @Testcontainers tests

Opportunities for optimization:
  • Reuse Testcontainer instances across test class
  • Use Docker Compose mode for multi-service tests
  • Cache Docker images locally

================================================================================
5. VERY LARGE TEST FILES (>500 LINES - LIKELY SLOW & COMPLEX)
================================================================================

Top 10 largest test files by line count:

1. HealthScoreServiceTest.java - 1912 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/service/HealthScoreServiceTest.java
   Contains: Numerous test cases, complex setup, likely mock intensive
   Recommendation: Break into smaller test classes by feature

2. CdsServiceTest.java - 1376 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/service/CdsServiceTest.java
   Contains: Clinical decision support service test suite
   Recommendation: Refactor to use parameterized tests for readability

3. VitalSignsServiceTest.java - 1319 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/clinical-workflow-service/src/test/java/com/healthdata/clinicalworkflow/application/VitalSignsServiceTest.java
   Contains: Complex vital signs validation logic
   Recommendation: Extract test data builders, use @ParameterizedTest

4. CdsControllerTest.java - 1199 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/controller/CdsControllerTest.java
   Contains: REST API endpoint test coverage
   Recommendation: Use MockMvc with parameterized tests

5. PatientAggregationServiceTest.java - 1108 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/patient-service/src/test/java/com/healthdata/patient/service/PatientAggregationServiceTest.java
   Contains: Multi-tenant patient aggregation logic
   Recommendation: Extract to feature-based test classes

6. PatientHealthServiceTest.java - 1101 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/service/PatientHealthServiceTest.java
   Contains: Patient health status computation
   Recommendation: Use @ParameterizedTest for edge cases

7. StarRatingCalculatorTest.java - 998 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/payer-workflows-service/src/test/java/com/healthdata/payer/service/StarRatingCalculatorTest.java
   Contains: Star rating calculation algorithms
   Recommendation: Parameterize test cases by rating dimension

8. ClinicalAlertServiceTest.java - 971 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/service/ClinicalAlertServiceTest.java
   Contains: Alert routing and notification logic
   Recommendation: Extract alert type specific tests

9. EmailNotificationChannelTest.java - 952 lines
   File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/service/notification/EmailNotificationChannelTest.java
   Contains: Email notification service tests
   Recommendation: Extract template and delivery tests

10. MentalHealthAssessmentServiceTest.java - 940 lines
    File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/service/MentalHealthAssessmentServiceTest.java
    Contains: Mental health assessment scoring
    Recommendation: Use test fixtures and parameterized tests

================================================================================
6. TESTS WITH HIGH-VOLUME LOOPS (POTENTIALLY SLOW)
================================================================================

22 tests contain high-volume loops (1000+ iterations):

These tests process large datasets in test loops:

Sample problematic tests:
  1. PHIEncryptionTest.java
     File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/agent-runtime-service/src/test/java/com/healthdata/agent/security/PHIEncryptionTest.java
     Pattern: Likely 1000+ encryption operations in loop

  2. CdaToFhirConverterTest.java
     File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cdr-processor-service/src/test/java/com/healthdata/cdr/converter/CdaToFhirConverterTest.java
     Pattern: CDA to FHIR conversion with large datasets

  3. CqlEngineServiceIntegrationTest.java
     File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cql-engine-service/src/test/java/com/healthdata/cql/integration/CqlEngineServiceIntegrationTest.java
     Pattern: CQL expression evaluation on 1000+ patient records

  4. DeadLetterQueueServiceTest.java
     File: /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/event-processing-service/src/test/java/com/healthdata/events/service/DeadLetterQueueServiceTest.java
     Pattern: DLQ processing with 1000+ message retry cycles

Recommendation: Reduce dataset sizes for unit tests, use performance benchmarks for volume tests

================================================================================
7. SUMMARY & RECOMMENDATIONS
================================================================================

EXECUTION TIME BREAKDOWN (Estimated):

Component                          Count    Est. Time
─────────────────────────────────────────────────────
Disabled tests                       6      0 min (skipped)
Heavyweight Kafka tests             11      2-5 min
Thread.sleep() wait tests           24      2-3 min
Docker startup (@Testcontainers)    69      10+ min
Large test files (>500 lines)       10      3-5 min
High-volume loop tests              22      5-10 min
All other tests (~480)              480     10-15 min
─────────────────────────────────────────────────────
TOTAL ESTIMATED                     613      30-45 min

Current CI/CD bottleneck: 45+ minutes (estimate)

IMMEDIATE ACTIONS (High ROI):
═════════════════════════════════════════════════════

Priority 1 - Fix @Disabled tests (3 failing tests impact 3 services):
  □ Implement KRaft mode for Kafka in Docker Compose (Phase 2)
  □ Re-enable CareGapAuditIntegrationHeavyweightTest
  □ Re-enable CareGapAuditPerformanceTest
  □ Re-enable CdrProcessorAuditIntegrationHeavyweightTest

Priority 2 - Replace Thread.sleep() with Awaitility (saves 1-2 minutes):
  □ Replace 13 calls in PayerWorkflowsAuditIntegrationHeavyweightTest
  □ Replace 13 calls in ClinicalDecisionAuditE2ETest
  □ Replace 13 calls in ApprovalAuditIntegrationHeavyweightTest
  □ Replace 11 calls in HIPAAAuditComplianceTest
  Pattern: Thread.sleep(500) -> Awaitility.await().timeout(Duration.ofSeconds(5))

Priority 3 - Refactor 10 mega-tests (>900 lines):
  □ Break HealthScoreServiceTest (1912 lines) into 5 focused test classes
  □ Break CdsServiceTest (1376 lines) into 3 feature-based test classes
  □ Break VitalSignsServiceTest (1319 lines) into 4 test classes
  □ Other large tests similarly
  Benefit: Faster execution, better parallelization

Priority 4 - Optimize Docker container usage:
  □ Use Testcontainers static resources for repeated service startup
  □ Consolidate 69 @Testcontainers tests to shared container instances
  Estimate: Save 5-10 minutes per full test run

================================================================================

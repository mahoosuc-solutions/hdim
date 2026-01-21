# Entity-Migration Synchronization Validation Report

**Release Version:** v1.3.0
**Validation Date:** 2026-01-20 21:22:22
**Validator:** Entity-Migration Sync Script

---

## Overview

This report validates that all JPA entities have corresponding Liquibase migrations across all HDIM microservices, ensuring zero schema drift in production.

---

## Validation Results

### Service-by-Service Results

#### ❌ agent-builder-service

- **Status:** FAILED
- **Error Details:**

```
  found:    Map
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/agent-builder-service/src/main/java/com/healthdata/agentbuilder/service/AgentTestService.java:116: warning: [unchecked] unchecked cast
                        (Map<String, Object>) tool.get("arguments"),
                                                      ^
  required: Map<String,Object>
  found:    Object
3 warnings

1 test completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:agent-builder-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/agent-builder-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 3m 30s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:agent-builder-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ agent-runtime-service

- **Status:** FAILED
- **Error Details:**

```
  found:    Map
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/agent-runtime-service/src/main/java/com/healthdata/agent/llm/providers/BedrockProvider.java:327: warning: [unchecked] unchecked conversion
                            .arguments(objectMapper.convertValue(block.path("input"), Map.class))
                                                                ^
  required: Map<String,Object>
  found:    Map
7 warnings

2 tests completed, 2 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:agent-runtime-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/agent-runtime-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 1m 5s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:agent-runtime-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ analytics-service

- **Status:** FAILED
- **Error Details:**

```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
Hibernate: drop table if exists alert_rules cascade
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@4614a17c (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@389ad8e6 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@5b6681c8 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@56543d90 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@4d65d7c3 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@23d67b61 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@4c51d612 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@3b9ecab5 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@58a6c9ca (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:27:43 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@483bbccc (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:28:16 - SQL Error: 0, SQLState: 08001
2026-01-20 21:28:16 - HikariPool-1 - Connection is not available, request timed out after 30004ms (total=0, active=0, idle=0, waiting=0)
2026-01-20 21:28:16 - Connection to localhost:57370 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
2026-01-20 21:28:16 - Invocation of destroy method failed on bean with name 'entityManagerFactory': org.hibernate.exception.JDBCConnectionException: Unable to open JDBC Connection for DDL execution [HikariPool-1 - Connection is not available, request timed out after 30004ms (total=0, active=0, idle=0, waiting=0)] [n/a]
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:analytics-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ approval-service

- **Status:** FAILED
- **Error Details:**

```

2 tests completed, 2 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:approval-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/approval-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 28s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:approval-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ care-gap-service

- **Status:** FAILED
- **Error Details:**

```
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/integration/CareGapDetectionE2ETest.java:6: error: package com.healthdata.caregap.messaging does not exist
  import com.healthdata.caregap.messaging.CareGapClosureEventConsumer;
                                         ^
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/integration/CareGapDetectionE2ETest.java:61: error: cannot find symbol
      private CareGapRepository careGapRepository;
              ^
    symbol:   class CareGapRepository
    location: class CareGapDetectionE2ETest
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/care-gap-service/src/test/java/com/healthdata/caregap/integration/CareGapDetectionE2ETest.java:67: error: cannot find symbol
      private CareGapClosureEventConsumer careGapClosureEventConsumer;
              ^
    symbol:   class CareGapClosureEventConsumer
    location: class CareGapDetectionE2ETest
  5 errors

* Try:
> Check your code and dependencies to fix the compilation error(s)
> Run with --scan to get full insights.

BUILD FAILED in 13s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:care-gap-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ cdr-processor-service

- **Status:** FAILED
- **Error Details:**

```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:cdr-processor-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ consent-service

- **Status:** FAILED
- **Error Details:**

```

1 test completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:consent-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/consent-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 29s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:consent-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ cql-engine-service

- **Status:** FAILED
- **Error Details:**

```
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/cql-engine-service/src/test/java/com/healthdata/cql/security/JwtAuthenticationFilterTest.java:57: warning: [deprecation] JwtAuthenticationFilter in com.healthdata.cql.security has been deprecated
    private JwtAuthenticationFilter jwtAuthenticationFilter;
            ^
3 warnings
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
Hibernate: alter table if exists cql_evaluations drop constraint if exists fk_evaluation_library
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@2b5eb8b4 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@790ada81 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@274f560c (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@3fe25d (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@58c5daf1 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7734cdba (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@2a161a66 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@55443873 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@23a07278 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:30:54 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@72a822ab (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:31:28 - SQL Error: 0, SQLState: 08001
2026-01-20 21:31:28 - HikariPool-1 - Connection is not available, request timed out after 30002ms (total=0, active=0, idle=0, waiting=0)
2026-01-20 21:31:28 - Connection to localhost:58464 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
2026-01-20 21:31:28 - Invocation of destroy method failed on bean with name 'entityManagerFactory': org.hibernate.exception.JDBCConnectionException: Unable to open JDBC Connection for DDL execution [HikariPool-1 - Connection is not available, request timed out after 30002ms (total=0, active=0, idle=0, waiting=0)] [n/a]
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:cql-engine-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ data-enrichment-service

- **Status:** FAILED
- **Error Details:**

```

1 test completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:data-enrichment-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/data-enrichment-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 41s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:data-enrichment-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ documentation-service

- **Status:** FAILED
- **Error Details:**

```
2026-01-20T21:32:58.651-05:00  WARN 23800 --- [documentation-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@382cf2c5 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:32:58.652-05:00  WARN 23800 --- [documentation-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@12184230 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:32:58.654-05:00  WARN 23800 --- [documentation-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@6527004d (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:33:31.977-05:00  WARN 23800 --- [documentation-service-test] [ionShutdownHook] [                                                 ] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08001
2026-01-20T21:33:31.977-05:00 ERROR 23800 --- [documentation-service-test] [ionShutdownHook] [                                                 ] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 30000ms (total=0, active=0, idle=0, waiting=0)
2026-01-20T21:33:31.978-05:00 ERROR 23800 --- [documentation-service-test] [ionShutdownHook] [                                                 ] o.h.engine.jdbc.spi.SqlExceptionHelper   : Connection to localhost:57708 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
2026-01-20T21:33:31.982-05:00  WARN 23800 --- [documentation-service-test] [ionShutdownHook] [                                                 ] o.s.b.f.support.DisposableBeanAdapter    : Invocation of destroy method failed on bean with name 'entityManagerFactory': org.hibernate.exception.JDBCConnectionException: Unable to open JDBC Connection for DDL execution [HikariPool-1 - Connection is not available, request timed out after 30000ms (total=0, active=0, idle=0, waiting=0)] [n/a]

3 tests completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:documentation-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/documentation-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 1m 22s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:documentation-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ ecr-service

- **Status:** FAILED
- **Error Details:**

```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
Hibernate: drop table if exists ecr.electronic_case_reports cascade
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7abf938a (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@265d7190 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@20f88512 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7aaab1b3 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@68251c65 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@25b92100 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@418df0b0 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@6e05f61b (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@2e03198a (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:21 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@42de8c6b (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:34:54 - SQL Error: 0, SQLState: 08001
2026-01-20 21:34:54 - HikariPool-1 - Connection is not available, request timed out after 30000ms (total=0, active=0, idle=0, waiting=0)
2026-01-20 21:34:54 - Connection to localhost:57266 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
2026-01-20 21:34:54 - Invocation of destroy method failed on bean with name 'entityManagerFactory': org.hibernate.exception.JDBCConnectionException: Unable to open JDBC Connection for DDL execution [HikariPool-1 - Connection is not available, request timed out after 30000ms (total=0, active=0, idle=0, waiting=0)] [n/a]
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:ecr-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ ehr-connector-service

- **Status:** FAILED
- **Error Details:**

```
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/ehr-connector-service/src/main/java/com/healthdata/ehr/connector/epic/EpicAuthProvider.java:90: warning: [deprecation] setId(String) in ClaimsMutator has been deprecated
                    .setId(UUID.randomUUID().toString())
                    ^
  where T is a type-variable:
    T extends ClaimsMutator<T> declared in interface ClaimsMutator
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/ehr-connector-service/src/main/java/com/healthdata/ehr/connector/epic/EpicAuthProvider.java:91: warning: [deprecation] setIssuedAt(Date) in ClaimsMutator has been deprecated
                    .setIssuedAt(Date.from(now))
                    ^
  where T is a type-variable:
    T extends ClaimsMutator<T> declared in interface ClaimsMutator
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/ehr-connector-service/src/main/java/com/healthdata/ehr/connector/epic/EpicAuthProvider.java:92: warning: [deprecation] setExpiration(Date) in ClaimsMutator has been deprecated
                    .setExpiration(Date.from(expiration))
                    ^
  where T is a type-variable:
    T extends ClaimsMutator<T> declared in interface ClaimsMutator
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/ehr-connector-service/src/main/java/com/healthdata/ehr/connector/epic/EpicAuthProvider.java:93: warning: [deprecation] signWith(Key,SignatureAlgorithm) in JwtBuilder has been deprecated
                    .signWith(config.getPrivateKey(), SignatureAlgorithm.RS384)
                    ^
8 warnings
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:ehr-connector-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ event-processing-service

- **Status:** FAILED
- **Error Details:**

```

1 test completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:event-processing-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/event-processing-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 29s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:event-processing-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ event-router-service

- **Status:** FAILED
- **Error Details:**

```

1 test completed, 1 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:event-router-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/event-router-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 29s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:event-router-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ fhir-service

- **Status:** FAILED
- **Error Details:**

```
          patientService = new PatientService(patientRepository, validator, kafkaTemplate, cacheManager);
                           ^
    required: PatientRepository,PatientValidator,KafkaTemplate<String,Object>,CacheManager,MeterRegistry
    found:    PatientRepository,PatientValidator,KafkaTemplate<String,Object>,CacheManager
    reason: actual and formal argument lists differ in length
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/fhir-service/src/test/java/com/healthdata/fhir/service/PatientServiceTest.java:214: error: constructor PatientService in class PatientService cannot be applied to given types;
          PatientService noCacheService = new PatientService(patientRepository, validator, kafkaTemplate, noCacheManager);
                                          ^
    required: PatientRepository,PatientValidator,KafkaTemplate<String,Object>,CacheManager,MeterRegistry
    found:    PatientRepository,PatientValidator,KafkaTemplate<String,Object>,CacheManager
    reason: actual and formal argument lists differ in length
  Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
  4 errors
  2 warnings

* Try:
> Check your code and dependencies to fix the compilation error(s)
> Run with --scan to get full insights.

BUILD FAILED in 49s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:fhir-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ gateway-service

- **Status:** FAILED
- **Error Details:**

```
/mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/ratelimit/TenantRateLimitService.java:147: warning: [deprecation] Refill in io.github.bucket4j has been deprecated
                        Refill.greedy(effectiveRps, Duration.ofSeconds(1))))
                        ^
/mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/ratelimit/TenantRateLimitService.java:147: warning: [deprecation] greedy(long,Duration) in Refill has been deprecated
                        Refill.greedy(effectiveRps, Duration.ofSeconds(1))))
                              ^
/mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/ratelimit/TenantRateLimitService.java:146: warning: [deprecation] classic(long,Refill) in Bandwidth has been deprecated
                .addLimit(Bandwidth.classic(effectiveBurst,
                                   ^
/mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/ratelimit/AuthRateLimitFilter.java:201: warning: [deprecation] Refill in io.github.bucket4j has been deprecated
                Refill.intervally(tokensPerMinute, Duration.ofMinutes(1))))
                ^
/mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/ratelimit/AuthRateLimitFilter.java:201: warning: [deprecation] intervally(long,Duration) in Refill has been deprecated
                Refill.intervally(tokensPerMinute, Duration.ofMinutes(1))))
                      ^
/mnt/wdblack/dev/projects/hdim-master/backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/ratelimit/AuthRateLimitFilter.java:200: warning: [deprecation] classic(long,Refill) in Bandwidth has been deprecated
            .addLimit(Bandwidth.classic(tokensPerMinute,
                               ^
6 warnings
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:gateway-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ hcc-service

- **Status:** FAILED
- **Error Details:**

```
                                               ^
    symbol:   class HccCrosswalk
    location: class RafCalculationService
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/hcc-service/src/test/java/com/healthdata/hcc/integration/HccRiskAdjustmentE2ETest.java:340: error: cannot find symbol
                      new RafCalculationService.HccCrosswalk(ICD10_COPD, "HCC111", "HCC111")
                                               ^
    symbol:   class HccCrosswalk
    location: class RafCalculationService
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/hcc-service/src/test/java/com/healthdata/hcc/integration/HccRiskAdjustmentE2ETest.java:358: error: cannot find symbol
                      new RafCalculationService.HccCrosswalk("E11.9", "HCC19", "HCC37")
                                               ^
    symbol:   class HccCrosswalk
    location: class RafCalculationService
  14 errors

* Try:
> Check your code and dependencies to fix the compilation error(s)
> Run with --scan to get full insights.

BUILD FAILED in 12s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:hcc-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ migration-workflow-service

- **Status:** FAILED
- **Error Details:**

```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
Hibernate: 
    alter table if exists migration_checkpoints 
       drop constraint if exists FK1mmn19y57xvkwpr248w3uqvg5
2026-01-20T21:39:05.738-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7ecad3aa (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.741-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@2cf9b338 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.743-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@486728e (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.744-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@1d78fde7 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.745-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@1a1b2f33 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.746-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7b1a2f3b (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.746-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@4cbf20d4 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.747-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@16f27b61 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.747-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@55d3bc3b (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:05.748-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] com.zaxxer.hikari.pool.PoolBase          : HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7f9a324d (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20T21:39:38.938-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] o.h.engine.jdbc.spi.SqlExceptionHelper   : SQL Error: 0, SQLState: 08001
2026-01-20T21:39:38.939-05:00 ERROR 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] o.h.engine.jdbc.spi.SqlExceptionHelper   : HikariPool-1 - Connection is not available, request timed out after 30000ms (total=0, active=0, idle=0, waiting=0)
2026-01-20T21:39:38.941-05:00 ERROR 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] o.h.engine.jdbc.spi.SqlExceptionHelper   : Connection to localhost:57320 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
2026-01-20T21:39:38.944-05:00  WARN 31246 --- [migration-workflow-service-test] [ionShutdownHook] [                                                 ] o.s.b.f.support.DisposableBeanAdapter    : Invocation of destroy method failed on bean with name 'entityManagerFactory': org.hibernate.exception.JDBCConnectionException: Unable to open JDBC Connection for DDL execution [HikariPool-1 - Connection is not available, request timed out after 30000ms (total=0, active=0, idle=0, waiting=0)] [n/a]
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:migration-workflow-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ notification-service

- **Status:** FAILED
- **Error Details:**

```

2 tests completed, 2 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:notification-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/notification-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 30s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:notification-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ patient-service

- **Status:** FAILED
- **Error Details:**

```
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/patient-service/src/test/java/com/healthdata/patient/integration/CacheIsolationSecurityE2ETest.java:4: error: package com.healthdata.patient.domain.repository does not exist
  import com.healthdata.patient.domain.repository.PatientRepository;
                                                 ^
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/patient-service/src/test/java/com/healthdata/patient/integration/TenantIsolationSecurityE2ETest.java:60: error: cannot find symbol
      private PatientRepository patientRepository;
              ^
    symbol:   class PatientRepository
    location: class TenantIsolationSecurityE2ETest
  /mnt/wdblack/dev/projects/hdim-master/backend/modules/services/patient-service/src/test/java/com/healthdata/patient/integration/CacheIsolationSecurityE2ETest.java:77: error: cannot find symbol
      private PatientRepository patientRepository;
              ^
    symbol:   class PatientRepository
    location: class CacheIsolationSecurityE2ETest
  7 errors

* Try:
> Check your code and dependencies to fix the compilation error(s)
> Run with --scan to get full insights.

BUILD FAILED in 10s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:patient-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ payer-workflows-service

- **Status:** FAILED
- **Error Details:**

```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:payer-workflows-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ predictive-analytics-service

- **Status:** FAILED
- **Error Details:**

```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:predictive-analytics-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ prior-auth-service

- **Status:** FAILED
- **Error Details:**

```

2 tests completed, 2 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:prior-auth-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/prior-auth-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 27s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:prior-auth-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ qrda-export-service

- **Status:** FAILED
- **Error Details:**

```
2026-01-20 21:42:55 - Closing JPA EntityManagerFactory for persistence unit 'default'
Hibernate: 
    alter table if exists quality.cms_submissions 
       drop constraint if exists FKrfx511erph6ela3vo5xkpksfh
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7acff702 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@61745406 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7b5f4330 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@1e045572 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@2b662c8b (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@5489c916 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@55508075 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@69c3e068 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@73db9baf (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:42:55 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@69002dbb (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:43:29 - SQL Error: 0, SQLState: 08001
2026-01-20 21:43:29 - HikariPool-1 - Connection is not available, request timed out after 30000ms (total=0, active=0, idle=0, waiting=0)
2026-01-20 21:43:29 - Connection to localhost:58246 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
2026-01-20 21:43:29 - Invocation of destroy method failed on bean with name 'entityManagerFactory': org.hibernate.exception.JDBCConnectionException: Unable to open JDBC Connection for DDL execution [HikariPool-1 - Connection is not available, request timed out after 30000ms (total=0, active=0, idle=0, waiting=0)] [n/a]
2026-01-20 21:43:29 - HikariPool-1 - Shutdown initiated...
2026-01-20 21:43:29 - HikariPool-1 - Shutdown completed.
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:qrda-export-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ quality-measure-service

- **Status:** FAILED
- **Error Details:**

```
Note: Some input files additionally use unchecked or unsafe operations.
100 warnings
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@10ec01c0 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@2f3a49f4 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@16ac6740 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@33248140 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@323677a7 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@a272013 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@60e08126 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@3a607aca (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@6dfb500d (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@7a8c13e1 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:07 - HikariPool-1 - Failed to validate connection org.postgresql.jdbc.PgConnection@74c2d579 (This connection has been closed.). Possibly consider using a shorter maxLifetime value.
2026-01-20 21:45:27 - SQL Error: 0, SQLState: 08001
2026-01-20 21:45:27 - HikariPool-1 - Connection is not available, request timed out after 20004ms (total=0, active=0, idle=0, waiting=0)
2026-01-20 21:45:27 - Connection to localhost:58072 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
2026-01-20 21:45:27 - Invocation of destroy method failed on bean with name 'entityManagerFactory': org.hibernate.exception.JDBCConnectionException: Unable to open JDBC Connection for DDL execution [HikariPool-1 - Connection is not available, request timed out after 20004ms (total=0, active=0, idle=0, waiting=0)] [n/a]
JaCoCo coverage report generated at: build/reports/jacoco/test/html/index.html
Target coverage: ≥70% overall, ≥80% service layer
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:quality-measure-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ sales-automation-service

- **Status:** FAILED
- **Error Details:**

```

2 tests completed, 2 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:sales-automation-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/sales-automation-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 56s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:sales-automation-service:test --tests "*EntityMigrationValidationTest"`

#### ❌ sdoh-service

- **Status:** FAILED
- **Error Details:**

```
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/sdoh-service/src/main/java/com/healthdata/sdoh/entity/SdohDiagnosisEntity.java:24: warning: [deprecation] GenericGenerator in org.hibernate.annotations has been deprecated
    @GenericGenerator(name = "uuid", strategy = "uuid2")
     ^
/mnt/wdblack/dev/projects/hdim-master/backend/modules/services/sdoh-service/src/main/java/com/healthdata/sdoh/entity/SdohDiagnosisEntity.java:24: warning: [deprecation] strategy() in GenericGenerator has been deprecated
    @GenericGenerator(name = "uuid", strategy = "uuid2")
                                     ^
50 warnings

2 tests completed, 2 failed

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':modules:services:sdoh-service:test'.
> There were failing tests. See the report at: file:///mnt/wdblack/dev/projects/hdim-master/backend/modules/services/sdoh-service/build/reports/tests/test/index.html

* Try:
> Run with --scan to get full insights.

BUILD FAILED in 30s
```

- **Remediation:**
  1. Review entity annotations vs migration column types
  2. Check ENTITY_MIGRATION_GUIDE.md for type mapping
  3. Update entity OR migration to match
  4. Re-run: `./gradlew :modules:services:sdoh-service:test --tests "*EntityMigrationValidationTest"`

---

### DDL Auto Configuration Check

- ❌ `modules/services/sales-automation-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/sales-automation-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/sdoh-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/sdoh-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/sdoh-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/sdoh-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/ecr-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/ecr-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/patient-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/patient-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/patient-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/patient-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/demo-seeding-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/qrda-export-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/qrda-export-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/care-gap-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/care-gap-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/care-gap-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/care-gap-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/hcc-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/hcc-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/fhir-service/src/main/resources/application-demo.yml`: **ddl-auto: create** (MUST be 'validate')
- ❌ `modules/services/fhir-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/fhir-service/src/test/resources/application-kafka-it.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/fhir-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/fhir-service/build/resources/main/application-demo.yml`: **ddl-auto: create** (MUST be 'validate')
- ❌ `modules/services/fhir-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/fhir-service/build/resources/test/application-kafka-it.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/fhir-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/prior-auth-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/prior-auth-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/predictive-analytics-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/predictive-analytics-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/predictive-analytics-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/predictive-analytics-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/documentation-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/documentation-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/documentation-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/documentation-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/consent-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/consent-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/consent-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/consent-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/cql-engine-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/cql-engine-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/cql-engine-service/build-user/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/cql-engine-service/build-user/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/cost-analysis-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/data-enrichment-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/data-enrichment-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/data-enrichment-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/data-enrichment-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/payer-workflows-service/src/main/resources/application.yml`: **ddl-auto: validate
create-drop** (MUST be 'validate')
- ❌ `modules/services/payer-workflows-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/payer-workflows-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/payer-workflows-service/build/resources/main/application.yml`: **ddl-auto: validate
create-drop** (MUST be 'validate')
- ❌ `modules/services/payer-workflows-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/payer-workflows-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/cms-connector-service/target/classes/application.yml`: **ddl-auto: validate
validate
create-drop** (MUST be 'validate')
- ❌ `modules/services/cms-connector-service/src/main/resources/application.yml`: **ddl-auto: validate
validate
create-drop** (MUST be 'validate')
- ❌ `modules/services/cms-connector-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/analytics-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/analytics-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/analytics-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/analytics-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/event-processing-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/event-processing-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/event-processing-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/event-processing-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/cdr-processor-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/cdr-processor-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/cdr-processor-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/cdr-processor-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/approval-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/approval-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/approval-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/approval-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/migration-workflow-service/src/main/resources/application-docker.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/migration-workflow-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/migration-workflow-service/build/resources/main/application-docker.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/migration-workflow-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/agent-builder-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/agent-builder-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/agent-builder-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/agent-builder-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/notification-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/notification-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/quality-measure-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/quality-measure-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/quality-measure-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/quality-measure-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/ai-assistant-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/ai-assistant-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/gateway-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/gateway-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/ehr-connector-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/ehr-connector-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/ehr-connector-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/ehr-connector-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/event-router-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/event-router-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/event-router-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/event-router-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/agent-runtime-service/src/main/resources/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/agent-runtime-service/src/test/resources/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')
- ❌ `modules/services/agent-runtime-service/build/resources/main/application-kubernetes.yml`: **ddl-auto: none** (MUST be 'validate')
- ❌ `modules/services/agent-runtime-service/build/resources/test/application-test.yml`: **ddl-auto: create-drop** (MUST be 'validate')

**Remediation:**
- Change all ddl-auto values to 'validate' in the files listed above
- NEVER use 'create' or 'update' (causes data loss and schema drift)

---

## Summary

| Metric | Count |
|--------|-------|
| Total Services Tested | 27 |
| Passed | 0 |
| Failed | 27 |
| DDL Auto Violations | 107 |


### ❌ Overall Status: FAILED

Entity-migration synchronization issues detected. Review failures above and remediate before release.

# HikariCP Connection Pool Configuration Validation Report

**Release Version:** v0.0.0-test
**Validation Date:** 2026-03-07 00:08:28
**Validator:** HikariCP Validation Script

---

## Overview

This report validates HikariCP connection pool configuration across all HDIM microservices.

**Timing Formula Requirement:**
```
max-lifetime ≥ 6 × idle-timeout
```

**Standard Configuration:**
- idle-timeout: 300,000ms (5 minutes)
- max-lifetime: 1,800,000ms (30 minutes) - 6× idle-timeout
- keepalive-time: 240,000ms (4 minutes) - must be < idle-timeout

**Traffic Tier Pool Sizes:**
- HIGH: maximum-pool-size=50
- MEDIUM: maximum-pool-size=20
- LOW: maximum-pool-size=10

---

## Service-by-Service Validation

### ⚠️ investor-dashboard-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ investor-dashboard-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ investor-dashboard-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ care-gap-event-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ care-gap-event-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ care-gap-event-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ sales-automation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ sales-automation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ sales-automation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ sdoh-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ sdoh-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ sdoh-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-replay-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ event-replay-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ ecr-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ ecr-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ ecr-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ patient-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ patient-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ patient-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ demo-seeding-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ demo-seeding-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ demo-seeding-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-admin-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-admin-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-admin-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ clinical-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 300000ms
- **Max Lifetime:** 1200000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ clinical-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 300000ms
- **Max Lifetime:** 1200000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ clinical-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 300000ms
- **Max Lifetime:** 1200000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ qrda-export-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ qrda-export-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ qrda-export-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ care-gap-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ care-gap-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ care-gap-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ hcc-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ hcc-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ hcc-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ healthix-adapter-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 20 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ healthix-adapter-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 20 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cqrs-query-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ cqrs-query-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ audit-query-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 600000ms
- **Max Lifetime:** 1800000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ audit-query-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 600000ms
- **Max Lifetime:** 1800000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ audit-query-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 600000ms
- **Max Lifetime:** 1800000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ clinical-workflow-event-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ clinical-workflow-event-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ clinical-workflow-event-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ patient-event-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ patient-event-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ patient-event-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ fhir-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ fhir-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ fhir-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ prior-auth-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ prior-auth-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ prior-auth-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ predictive-analytics-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ predictive-analytics-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ predictive-analytics-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ documentation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ documentation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ documentation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ quality-measure-event-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ quality-measure-event-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ quality-measure-event-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ consent-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ consent-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ consent-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cql-engine-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cql-engine-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cql-engine-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ fhir-event-bridge-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ fhir-event-bridge-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ cost-analysis-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ cost-analysis-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ cost-analysis-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ data-ingestion-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 5 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ data-ingestion-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 5 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ data-ingestion-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 5 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ data-enrichment-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ data-enrichment-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ data-enrichment-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ payer-workflows-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ payer-workflows-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ payer-workflows-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ nurse-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ nurse-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ nurse-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ query-api-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 300000ms
- **Max Lifetime:** 1200000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 20 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ query-api-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 300000ms
- **Max Lifetime:** 1200000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 20 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ query-api-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 300000ms
- **Max Lifetime:** 1200000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 20 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cms-connector-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cms-connector-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cms-connector-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ hedis-adapter-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 15 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ hedis-adapter-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 15 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ analytics-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ analytics-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ analytics-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-processing-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-processing-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-processing-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cdr-processor-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cdr-processor-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ cdr-processor-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ approval-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ approval-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ approval-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ migration-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ migration-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ migration-workflow-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ agent-validation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ agent-validation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ agent-validation-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-clinical-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-clinical-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-clinical-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-fhir-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-fhir-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ gateway-fhir-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ devops-agent-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ devops-agent-service

- **HikariCP Configuration:** Not found

**Note:** If this service uses a database, configure HikariCP in application.yml.

### ⚠️ agent-builder-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ agent-builder-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ agent-builder-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ notification-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ notification-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ notification-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ corehive-adapter-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ corehive-adapter-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 10 (expected: 10) ✅
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ quality-measure-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ quality-measure-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ quality-measure-service

- **Traffic Tier:** HIGH
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 50) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 50  # HIGH traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ ai-assistant-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ ai-assistant-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ ai-assistant-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ ehr-connector-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ ehr-connector-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ ehr-connector-service

- **Traffic Tier:** MEDIUM
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 20) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 20  # MEDIUM traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-router-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-router-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-router-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ agent-runtime-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ agent-runtime-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ agent-runtime-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 0ms
- **Max Lifetime:** 0ms ⚠️
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 0 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-store-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 600000ms
- **Max Lifetime:** 1800000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 20 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-store-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 600000ms
- **Max Lifetime:** 1800000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 20 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

### ⚠️ event-store-service

- **Traffic Tier:** LOW
- **Idle Timeout:** 600000ms
- **Max Lifetime:** 1800000ms ❌
- **Keepalive Time:** 0ms ⚠️
- **Max Pool Size:** 20 (expected: 10) ⚠️
- **Leak Detection:** 0ms ⚠️

**Remediation:**
```yaml
healthdata:
  database:
    hikari:
      idle-timeout: 300000        # 5 minutes
      max-lifetime: 1800000       # 30 minutes (6× idle-timeout)
      keepalive-time: 240000      # 4 minutes (< idle-timeout)
      maximum-pool-size: 10  # LOW traffic tier
      leak-detection-threshold: 60000  # 60 seconds
```

---

## Summary

| Metric | Count | Status |
|--------|-------|--------|
| Total Services | 149 | - |
| Timing Formula Violations | 149 | ❌ |
| Pool Size Mismatches | 124 | ⚠️ |
| Keepalive Violations | 135 | ❌ |
| Leak Detection Missing | 135 | ⚠️ |

## HikariCP Timing Formula

The timing formula prevents connection pool exhaustion:

```
max-lifetime ≥ 6 × idle-timeout

Standard values:
- idle-timeout:   300,000ms (5 minutes)
- max-lifetime: 1,800,000ms (30 minutes)
- keepalive-time: 240,000ms (4 minutes, < idle-timeout)
```

## References

- **Database Config Guide:** backend/docs/DATABASE_CONFIG_ADOPTION_GUIDE.md
- **HikariCP Documentation:** backend/modules/shared/infrastructure/database-config/README.md


### ❌ Overall Status: FAILED

HikariCP configuration issues detected. Review violations above and update application.yml files before release.

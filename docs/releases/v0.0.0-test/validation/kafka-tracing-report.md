# Kafka Trace Propagation Validation Report

**Release Version:** v0.0.0-test
**Validation Date:** 2026-03-07 00:08:28

---

## Overview

Validates Kafka configuration for OpenTelemetry trace propagation across all 19 Kafka-enabled services.

**Required Configuration:**
- Producer: `spring.json.add.type.headers: false`
- Consumer: `spring.json.use.type.headers: false`
- Producer Interceptor: `KafkaProducerTraceInterceptor`
- Consumer Interceptor: `KafkaConsumerTraceInterceptor`

---

## Validation Results

### ❌ qrda-export-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ qrda-export-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ qrda-export-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ care-gap-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ care-gap-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ care-gap-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ hcc-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ hcc-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ hcc-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ cql-engine-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ cql-engine-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ cql-engine-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ cms-connector-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ cms-connector-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ quality-measure-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ quality-measure-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### ❌ quality-measure-service

- **Producer Type Headers Disabled:** ❌
- **Consumer Type Headers Disabled:** ❌
- **Producer Trace Interceptor:** ⚠️
- **Consumer Trace Interceptor:** ⚠️

**Remediation:**
```yaml
spring:
  kafka:
    producer:
      properties:
        spring.json.add.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        spring.json.use.type.headers: false  # CRITICAL: Prevents ClassNotFoundException
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

---

## ClassNotFoundException Check

- ℹ️ **Logs:** Docker not available (check skipped)

---

## Summary

| Check | Status |
|-------|--------|
| Type Headers Disabled | ❌ FAIL (34 violations) |
| Trace Interceptors | ⚠️ WARN (34 missing) |

## References

- **Distributed Tracing Guide:** backend/docs/DISTRIBUTED_TRACING_GUIDE.md
- **Kafka Config:** CLAUDE.md (Kafka section)

### ❌ Overall Status: FAILED

Kafka configuration issues detected. Type headers MUST be disabled to prevent ClassNotFoundException errors.

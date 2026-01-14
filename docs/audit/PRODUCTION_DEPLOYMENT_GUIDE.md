# Audit Infrastructure - Production Deployment Guide

## 🎯 Overview

This guide provides comprehensive instructions for deploying the AI Agent Decision Audit infrastructure to production, ensuring HIPAA/SOC 2 compliance, high availability, and optimal performance.

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Kafka Production Configuration](#kafka-production-configuration)
3. [Performance Tuning](#performance-tuning)
4. [Monitoring & Alerting](#monitoring--alerting)
5. [Disaster Recovery](#disaster-recovery)
6. [Security Configuration](#security-configuration)
7. [Scaling Guidelines](#scaling-guidelines)
8. [Operational Procedures](#operational-procedures)

---

## Prerequisites

### Infrastructure Requirements

**Kafka Cluster**:
- Minimum 3 brokers for high availability
- Replication factor: 3
- Min in-sync replicas: 2
- Storage: SSD with 500+ IOPS
- Memory: 32GB+ per broker
- CPU: 8+ cores per broker

**Database (PostgreSQL)**:
- Version: 14+
- Storage: 500GB+ SSD
- Memory: 64GB+
- CPU: 16+ cores
- Replication: Streaming replication with 2+ replicas

**Network**:
- Low latency (< 10ms within region)
- Bandwidth: 1Gbps+
- Dedicated VPC for audit services

### Software Requirements

- Java 17+
- Spring Boot 3.2+
- Kafka 3.8+
- PostgreSQL 14+
- Redis 7+ (for caching)

---

## Kafka Production Configuration

### 1. Topic Configuration

```yaml
# ai-agent-decisions topic configuration
name: ai.agent.decisions
partitions: 30  # Scale based on throughput (10K events/sec ~= 30 partitions)
replication-factor: 3
min-insync-replicas: 2

# Topic-level configs
configs:
  retention.ms: 189216000000  # 6 years (HIPAA requirement)
  retention.bytes: -1  # Unlimited
  segment.ms: 604800000  # 7 days
  compression.type: lz4
  cleanup.policy: delete
  max.message.bytes: 1048576  # 1MB
```

### 2. Producer Configuration

```yaml
# application-prod.yml
spring:
  kafka:
    bootstrap-servers: kafka-1:9092,kafka-2:9092,kafka-3:9092
    producer:
      acks: all  # Wait for all replicas
      retries: 3
      compression-type: lz4
      batch-size: 32768  # 32KB
      linger-ms: 10  # Batch delay
      buffer-memory: 67108864  # 64MB
      max-in-flight-requests-per-connection: 5
      enable-idempotence: true  # Exactly-once semantics
      
      properties:
        # Timeout configs
        request.timeout.ms: 30000
        delivery.timeout.ms: 120000
        max.block.ms: 60000
        
        # Security (enable in production)
        security.protocol: SSL
        ssl.truststore.location: /etc/kafka/truststore.jks
        ssl.truststore.password: ${KAFKA_TRUSTSTORE_PASSWORD}
        ssl.keystore.location: /etc/kafka/keystore.jks
        ssl.keystore.password: ${KAFKA_KEYSTORE_PASSWORD}
        ssl.key.password: ${KAFKA_KEY_PASSWORD}
```

### 3. Consumer Configuration

```yaml
spring:
  kafka:
    consumer:
      group-id: audit-event-consumer-${INSTANCE_ID}
      auto-offset-reset: earliest
      enable-auto-commit: false  # Manual commit for reliability
      fetch-min-size: 1024  # 1KB
      fetch-max-wait: 500ms
      max-poll-records: 500
      max-poll-interval-ms: 300000  # 5 minutes
      
      properties:
        isolation.level: read_committed  # Only read committed transactions
        session.timeout.ms: 30000
        heartbeat.interval.ms: 3000
```

---

## Performance Tuning

### 1. JVM Configuration

```bash
# JVM options for audit services
JAVA_OPTS="
  -Xms4g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:InitiatingHeapOccupancyPercent=45
  -XX:G1HeapRegionSize=16M
  -XX:+ParallelRefProcEnabled
  -XX:+UseStringDeduplication
  -XX:MaxMetaspaceSize=512m
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/var/log/audit/heap-dump.hprof
  -XX:+UseCompressedOops
  -XX:+AlwaysPreTouch
  -Djava.awt.headless=true
  -Djava.net.preferIPv4Stack=true
"
```

### 2. Database Connection Pool

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: AuditHikariPool
      
      # Performance tuning
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
```

### 3. Batch Processing Configuration

```yaml
audit:
  batch:
    enabled: true
    size: 100  # Batch insert size
    timeout: 1000  # 1 second max batch delay
    max-queue-size: 10000  # In-memory queue size
```

### 4. Redis Caching

```yaml
spring:
  redis:
    host: redis-cluster
    port: 6379
    password: ${REDIS_PASSWORD}
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 50
        max-idle: 10
        min-idle: 5
        max-wait: -1ms
```

---

## Monitoring & Alerting

### 1. Key Metrics to Monitor

#### Kafka Metrics

```yaml
# Prometheus configuration
- job_name: 'kafka'
  metrics_path: /metrics
  static_configs:
    - targets: ['kafka-1:9999', 'kafka-2:9999', 'kafka-3:9999']
  
  # Critical metrics
  metric_relabel_configs:
    # Broker metrics
    - source_labels: [__name__]
      regex: 'kafka_server_brokertopicmetrics_messagesinpersec'
      action: keep
    - source_labels: [__name__]
      regex: 'kafka_server_brokertopicmetrics_bytesinpersec'
      action: keep
    
    # Replication metrics
    - source_labels: [__name__]
      regex: 'kafka_server_replicamanager_underreplicatedpartitions'
      action: keep
    - source_labels: [__name__]
      regex: 'kafka_controller_kafkacontroller_offlinepartitionscount'
      action: keep
```

#### Application Metrics

```java
// Micrometer metrics configuration
@Configuration
public class AuditMetricsConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(
                "application", "audit-service",
                "environment", "${spring.profiles.active}"
            );
    }
    
    // Key metrics to track
    // - audit.events.published (Counter)
    // - audit.events.failed (Counter)
    // - audit.publish.latency (Timer)
    // - audit.batch.size (DistributionSummary)
    // - audit.queue.size (Gauge)
}
```

### 2. Alert Rules

```yaml
# Prometheus alert rules
groups:
  - name: audit_alerts
    rules:
      # High event publish failure rate
      - alert: AuditEventFailureRateHigh
        expr: rate(audit_events_failed_total[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High audit event failure rate"
          description: "Audit event failure rate is {{ $value }} events/sec"
      
      # Kafka lag increasing
      - alert: KafkaConsumerLagIncreasing
        expr: kafka_consumer_lag > 1000 and rate(kafka_consumer_lag[5m]) > 0
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Kafka consumer lag increasing"
          description: "Consumer lag is {{ $value }} and increasing"
      
      # Under-replicated partitions
      - alert: KafkaUnderReplicatedPartitions
        expr: kafka_server_replicamanager_underreplicatedpartitions > 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Kafka has under-replicated partitions"
          description: "{{ $value }} partitions are under-replicated"
      
      # Database connection pool exhaustion
      - alert: DatabaseConnectionPoolExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Database connection pool near exhaustion"
          description: "{{ $value }}% of connections in use"
      
      # High publish latency
      - alert: AuditPublishLatencyHigh
        expr: histogram_quantile(0.99, rate(audit_publish_latency_seconds_bucket[5m])) > 1.0
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High audit event publish latency"
          description: "P99 latency is {{ $value }} seconds"
```

### 3. Grafana Dashboards

**Dashboard 1: Audit Event Flow**
- Events published per second
- Event types distribution
- Publish success/failure rate
- End-to-end latency (P50, P95, P99)

**Dashboard 2: Kafka Health**
- Messages in/out per second
- Under-replicated partitions
- Consumer lag by group
- Disk usage per broker

**Dashboard 3: Database Performance**
- Connection pool usage
- Query latency
- Transaction rate
- Table sizes and growth

---

## Disaster Recovery

### 1. Backup Strategy

**Kafka Backup**:
```bash
# MirrorMaker 2 for cross-region replication
# config/mirror-maker.properties
clusters = primary, dr
primary.bootstrap.servers = kafka-1:9092,kafka-2:9092,kafka-3:9092
dr.bootstrap.servers = dr-kafka-1:9092,dr-kafka-2:9092,dr-kafka-3:9092

primary->dr.enabled = true
primary->dr.topics = ai.agent.decisions
primary->dr.sync.group.offsets.enabled = true
```

**Database Backup**:
```bash
# Continuous WAL archiving
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'aws s3 cp %p s3://audit-wal-archive/%f'

# Daily full backup
pg_basebackup -h primary -D /backup -Ft -z -P
```

### 2. Recovery Procedures

**Kafka Recovery**:
```bash
# 1. Restore topic from DR cluster
kafka-mirror-maker --consumer.config dr-consumer.properties \
                    --producer.config primary-producer.properties \
                    --whitelist ai.agent.decisions

# 2. Verify data integrity
kafka-console-consumer --bootstrap-server primary:9092 \
                       --topic ai.agent.decisions \
                       --from-beginning --max-messages 1000 | wc -l
```

**Database Recovery**:
```bash
# 1. Restore from base backup
pg_restore -d audit_db /backup/base.tar.gz

# 2. Replay WAL logs
recovery.conf:
restore_command = 'aws s3 cp s3://audit-wal-archive/%f %p'
recovery_target_time = '2024-01-14 12:00:00'
```

### 3. RTO/RPO Targets

| Component | RTO | RPO | Strategy |
|-----------|-----|-----|----------|
| Kafka | < 15 min | < 5 min | Multi-region replication |
| Database | < 30 min | < 1 min | Streaming replication + WAL archiving |
| Application | < 5 min | N/A | Auto-scaling + health checks |

---

## Security Configuration

### 1. Encryption

**At Rest**:
```yaml
# PostgreSQL encryption
ssl = on
ssl_cert_file = '/etc/postgresql/server.crt'
ssl_key_file = '/etc/postgresql/server.key'
ssl_ca_file = '/etc/postgresql/root.crt'

# Transparent Data Encryption (TDE)
# Use AWS RDS encryption or similar cloud provider encryption
```

**In Transit**:
```yaml
# Kafka SSL/TLS
security.protocol: SSL
ssl.protocol: TLSv1.3
ssl.enabled.protocols: TLSv1.3
ssl.cipher.suites: TLS_AES_256_GCM_SHA384
```

### 2. Access Control

**Kafka ACLs**:
```bash
# Create ACLs for audit-service
kafka-acls --bootstrap-server kafka:9092 \
  --add \
  --allow-principal User:audit-service \
  --operation Write \
  --topic ai.agent.decisions

kafka-acls --bootstrap-server kafka:9092 \
  --add \
  --allow-principal User:audit-consumer \
  --operation Read \
  --topic ai.agent.decisions \
  --group audit-event-consumer
```

**Database Roles**:
```sql
-- Create read-only role for analysts
CREATE ROLE audit_analyst LOGIN PASSWORD 'secure_password';
GRANT SELECT ON ALL TABLES IN SCHEMA audit TO audit_analyst;

-- Create read-write role for application
CREATE ROLE audit_service LOGIN PASSWORD 'secure_password';
GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA audit TO audit_service;
```

---

## Scaling Guidelines

### Horizontal Scaling

**Kafka Brokers**:
```bash
# Add broker to cluster
# 1. Provision new broker with same config
# 2. Add to cluster
kafka-broker-api-versions --bootstrap-server new-broker:9092

# 3. Rebalance partitions
kafka-reassign-partitions --bootstrap-server kafka:9092 \
  --reassignment-json-file increase-replication.json \
  --execute
```

**Application Instances**:
```yaml
# Kubernetes HPA configuration
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: audit-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: audit-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: audit_events_per_second
      target:
        type: AverageValue
        averageValue: "1000"
```

### Vertical Scaling

**Resource Allocation**:
```yaml
# Kubernetes resource limits
resources:
  requests:
    memory: "4Gi"
    cpu: "2000m"
  limits:
    memory: "8Gi"
    cpu: "4000m"
```

---

## Operational Procedures

### 1. Rolling Updates

```bash
# Zero-downtime deployment
kubectl rollout status deployment/audit-service
kubectl set image deployment/audit-service \
  audit-service=audit-service:v2.0.0
kubectl rollout status deployment/audit-service
```

### 2. Health Checks

```yaml
# Kubernetes liveness/readiness probes
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

### 3. Log Aggregation

```yaml
# Filebeat configuration for log shipping
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/audit/*.log
  fields:
    service: audit-service
    environment: production
  multiline.pattern: '^[0-9]{4}-[0-9]{2}-[0-9]{2}'
  multiline.negate: true
  multiline.match: after

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "audit-logs-%{+yyyy.MM.dd}"
```

---

## Compliance Checklist

### HIPAA Compliance

- ✅ Encryption at rest (database TDE)
- ✅ Encryption in transit (TLS 1.3)
- ✅ Access controls (RBAC)
- ✅ Audit logging (all PHI access)
- ✅ 6-year retention
- ✅ Backup and disaster recovery
- ✅ Integrity controls (checksums)

### SOC 2 Compliance

- ✅ Security monitoring
- ✅ Change management (rolling updates)
- ✅ Incident response procedures
- ✅ Access logging and review
- ✅ Encryption key management
- ✅ Availability monitoring (99.9% SLA)

---

## Support and Maintenance

### Runbook References

- [Kafka Troubleshooting](./runbooks/kafka-troubleshooting.md)
- [Database Performance Tuning](./runbooks/database-tuning.md)
- [Incident Response](./runbooks/incident-response.md)
- [Capacity Planning](./runbooks/capacity-planning.md)

### On-Call Escalation

1. **Level 1**: Application alerts → On-call engineer
2. **Level 2**: Infrastructure issues → Platform team
3. **Level 3**: Data integrity → Senior architect + DBA
4. **Level 4**: Security incidents → CISO + Security team

---

**Document Version**: 1.0.0  
**Last Updated**: 2026-01-14  
**Next Review**: 2026-04-14

# Phase 6: High Availability & Disaster Recovery

**Status**: Not Started  
**Duration**: 3 weeks  
**Priority**: 🟠 HIGH  
**Team**: DevOps, DBA, SRE  

## Overview

Phase 6 ensures the system can survive failures and maintain service continuity. High availability (HA) is measured by uptime SLA (e.g., 99.9% = max 43 minutes downtime/month).

---

## Key Concepts

### SLA vs Uptime
- **99%** = 3.6 hours downtime/month (unacceptable)
- **99.9%** = 43 minutes downtime/month (acceptable for most services)
- **99.95%** = 22 minutes downtime/month (good)
- **99.99%** = 4 minutes downtime/month (excellent, very expensive)

### RTO & RPO
- **RTO** (Recovery Time Objective): Max time to recover = 1 hour
- **RPO** (Recovery Point Objective): Max data loss acceptable = 15 minutes

---

## Week 1: Database High Availability

### Objectives
- Automatic failover
- Data redundancy
- Backup automation
- Point-in-time recovery

### PostgreSQL Streaming Replication

```hcl
# infrastructure/databases-ha.tf

# Primary Database
resource "aws_db_instance" "primary" {
  identifier            = "cms-primary"
  engine               = "postgres"
  instance_class       = "db.t3.large"
  allocated_storage    = 100
  multi_az             = true  # Standby in different AZ
  
  # Replication
  backup_retention_period = 30
  backup_window           = "03:00-04:00"
  
  # Enhanced backups
  enabled_cloudwatch_logs_exports = ["postgresql"]
  monitoring_interval             = 60
  
  tags = {
    Name = "cms-primary"
  }
}

# Automatic RDS standby (not the same as read replica)
# RDS handles automatic failover with no application changes

# Enhanced monitoring
resource "aws_cloudwatch_metric_alarm" "database_failover" {
  alarm_name          = "cms-database-failover"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "DBInstanceIdentifier"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1"
  alarm_description   = "Alert on database failover"
}
```

### Backup Strategy

**Local Backups** (RDS automated)
- Daily snapshots, 30-day retention
- Automatic point-in-time recovery up to 5 minutes

**Cross-Region Backups** (for disaster recovery)
```hcl
resource "aws_db_instance_backup" "cross_region" {
  source_db_instance_identifier = aws_db_instance.primary.id
  backup_identifier            = "cms-primary-cross-region-backup"
  
  # Copy to secondary region for DR
  skip_final_snapshot = false
}
```

**Backup Validation**
```bash
#!/bin/bash
# scripts/test-database-backup.sh

# Restore from latest backup to test DB
AWS_REGION=us-east-1 aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier cms-restore-test \
  --db-snapshot-identifier <latest-snapshot-id>

# Wait for restore
aws rds wait db-instance-available \
  --db-instance-identifier cms-restore-test

# Test connectivity
psql -h cms-restore-test.c12345.us-east-1.rds.amazonaws.com \
  -U cms_admin -d cms_production -c "SELECT COUNT(*) FROM claims;"

# Delete test DB
aws rds delete-db-instance \
  --db-instance-identifier cms-restore-test \
  --skip-final-snapshot

echo "✓ Backup test successful"
```

### Success Criteria
- [ ] Multi-AZ enabled on primary database
- [ ] Automatic failover tested and working (< 1 minute)
- [ ] 30-day backup retention configured
- [ ] Cross-region backups enabled
- [ ] Backup restoration tested monthly
- [ ] Point-in-time recovery working
- [ ] RTO < 1 hour, RPO < 15 minutes

---

## Week 2: Application High Availability

### Kubernetes Pod Disruption Budgets

```yaml
# k8s/pod-disruption-budget.yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: cms-connector-pdb
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app: cms-connector
---

# k8s/deployment-ha.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cms-connector
spec:
  replicas: 3  # Always 3+ for HA
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0  # Zero downtime deployments

  template:
    metadata:
      labels:
        app: cms-connector
    spec:
      # Spread across nodes
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - cms-connector
              topologyKey: kubernetes.io/hostname

      # Graceful shutdown
      terminationGracePeriodSeconds: 30

      containers:
      - name: cms-connector
        image: cms-connector:latest
        
        # Health checks
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 40
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3

        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 5
          failureThreshold: 3

        # Resource limits
        resources:
          requests:
            cpu: 500m
            memory: 512Mi
          limits:
            cpu: 1000m
            memory: 1024Mi

        # Graceful shutdown hook
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 15"]
```

### Redis Cluster High Availability

```hcl
# AWS ElastiCache Cluster Mode Enabled

resource "aws_elasticache_cluster" "redis_ha" {
  cluster_id           = "cms-redis-ha"
  engine               = "redis"
  engine_version       = "7.0"
  node_type            = "cache.t3.medium"
  num_cache_nodes      = 3

  # Automatic failover
  automatic_failover_enabled = true
  multi_az_enabled           = true

  # Encryption
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  # Backup
  snapshot_retention_limit = 5
  snapshot_window          = "03:00-05:00"

  # Maintenance
  maintenance_window = "sun:04:00-sun:05:00"

  # Monitoring
  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis_logs.name
    destination_type = "cloudwatch-logs"
    log_type         = "slow-log"
  }
}
```

### Success Criteria
- [ ] 3+ application instances running
- [ ] Zero-downtime rolling updates working
- [ ] Pod disruption budgets configured
- [ ] Health checks passing
- [ ] Redis cluster with automatic failover
- [ ] Connection pooling optimized
- [ ] Circuit breakers for dependency failures

---

## Week 3: Disaster Recovery Testing

### DR Plan Document (`procedures/disaster-recovery-plan.md`)

```markdown
# Disaster Recovery Plan

## Overview
This document outlines procedures for recovering from various failure scenarios.

## Scope
- Total outage of availability zone
- Complete data center failure
- Corruption of database
- Security breach
- Ransomware attack

## RTO/RPO Targets
- **RTO**: 1 hour to restore service
- **RPO**: 15 minutes of data loss acceptable

## Critical Assets to Protect
1. Database (PostgreSQL)
2. Configuration (Secrets Manager)
3. Code repository (GitHub)
4. SSL certificates (ACM)
5. DNS configuration (Route 53)

## Failure Scenarios & Recovery Procedures

### Scenario 1: Single Node Failure
**Impact**: Degraded performance  
**RTO**: 5 minutes (automatic)  
**Recovery**: Automatic via Kubernetes scheduler  
**Testing**: Monthly pod failure simulations

### Scenario 2: Availability Zone Failure
**Impact**: Partial outage  
**RTO**: 10 minutes  
**Recovery**: Traffic rerouted to other AZs  
**Testing**: Monthly AZ failure simulations

### Scenario 3: Database Failure
**Impact**: Application cannot read/write data  
**RTO**: 30 minutes  
**Recovery**: Promote RDS read replica or restore from backup  
**Procedure**:
1. Detect database is down (monitoring alert)
2. Failover to standby (automatic via RDS Multi-AZ)
3. Update application database URL
4. Validate data integrity
5. Scale application to handle increased load

### Scenario 4: Complete Regional Failure
**Impact**: Total outage  
**RTO**: 1 hour  
**RPO**: 15 minutes  
**Recovery**: Failover to secondary region  
**Procedure**:
1. Update Route 53 to failover to secondary region
2. Promote secondary database from backup
3. Scale application in secondary region
4. Validate data consistency
5. Verify all services operational

## DR Testing Schedule
- **Monthly**: Pod failure, node failure, AZ failure
- **Quarterly**: Database failover, backup restoration
- **Annually**: Full regional failover

## Contact Information
- **On-Call DBA**: [Phone/Slack]
- **On-Call DevOps**: [Phone/Slack]
- **VP Engineering**: [Contact]

## Post-Incident
- [ ] Document what happened
- [ ] Identify root cause
- [ ] Create tickets to prevent recurrence
- [ ] Schedule blameless post-mortem
```

### DR Testing Script

```bash
#!/bin/bash
# procedures/run-dr-test.sh

set -e

TEST_NAME="CMS Connector DR Test - $(date +%Y-%m-%d)"
echo "=== Starting: $TEST_NAME ==="

# Step 1: Create timestamp
SNAPSHOT_TIME=$(date +%s)
echo "1. Creating database snapshot..."
aws rds create-db-snapshot \
  --db-instance-identifier cms-primary \
  --db-snapshot-identifier cms-dr-test-$SNAPSHOT_TIME

# Step 2: Wait for snapshot
echo "2. Waiting for snapshot to complete..."
aws rds wait db-snapshot-available \
  --db-snapshot-identifier cms-dr-test-$SNAPSHOT_TIME

# Step 3: Restore to test environment
echo "3. Restoring to test database..."
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier cms-dr-test-$SNAPSHOT_TIME \
  --db-snapshot-identifier cms-dr-test-$SNAPSHOT_TIME

# Step 4: Wait for restore
echo "4. Waiting for restore to complete..."
aws rds wait db-instance-available \
  --db-instance-identifier cms-dr-test-$SNAPSHOT_TIME

# Step 5: Test connectivity
echo "5. Testing database connectivity..."
RESTORED_HOST=$(aws rds describe-db-instances \
  --db-instance-identifier cms-dr-test-$SNAPSHOT_TIME \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

psql -h $RESTORED_HOST \
  -U cms_admin \
  -d cms_production \
  -c "SELECT COUNT(*) as total_records FROM claims;" || {
  echo "❌ Database test failed"
  exit 1
}

echo "✅ Database connectivity successful"

# Step 6: Validate data
echo "6. Validating data integrity..."
RECORD_COUNT=$(psql -h $RESTORED_HOST \
  -U cms_admin \
  -d cms_production \
  -t -c "SELECT COUNT(*) FROM claims;")

EXPECTED_COUNT=1000000  # Adjust based on your data
if [ "$RECORD_COUNT" -ge "$EXPECTED_COUNT" ]; then
  echo "✅ Data integrity validated ($RECORD_COUNT records)"
else
  echo "❌ Data integrity check failed (expected $EXPECTED_COUNT, got $RECORD_COUNT)"
  exit 1
fi

# Step 7: Cleanup
echo "7. Cleaning up test resources..."
aws rds delete-db-instance \
  --db-instance-identifier cms-dr-test-$SNAPSHOT_TIME \
  --skip-final-snapshot

aws rds delete-db-snapshot \
  --db-snapshot-identifier cms-dr-test-$SNAPSHOT_TIME

echo ""
echo "✅ DR Test Completed Successfully"
echo "   RTO: $(($(date +%s) - SNAPSHOT_TIME)) seconds"
echo "   RPO: 15 minutes (acceptable)"
```

### Success Criteria
- [ ] DR plan documented and approved
- [ ] All failure scenarios documented
- [ ] RTO < 1 hour achieved
- [ ] RPO < 15 minutes achieved
- [ ] Monthly DR drills completed
- [ ] Team trained on procedures
- [ ] Post-incident review process in place

---

## Monitoring & Validation

```yaml
# monitoring/ha-checks.yml

checks:
  - name: "Primary Database Alive"
    endpoint: "jdbc:postgresql://cms-primary:5432/cms_production"
    expected: "healthy"
    alert: "critical"

  - name: "Secondary Database Reachable"
    endpoint: "jdbc:postgresql://cms-secondary:5432/cms_production"
    expected: "healthy"
    alert: "warning"

  - name: "Redis Cluster Nodes"
    metric: "aws_elasticache_number_of_nodes"
    expected_value: 3
    alert: "critical"

  - name: "Application Replica Count"
    metric: "kubernetes_replicas"
    expected_value: 3
    alert: "critical"
```

---

## Budget Estimate
- Additional infrastructure (standby DB, replicas): $2,000-5,000/month
- Backup storage (30-day retention): $100-500/month
- DR testing tools: $0-1,000/month
- Development effort: 80-120 hours

## Key Files
```
infrastructure/
├── databases-ha.tf
├── redis-ha.tf
└── deployment-ha.yaml

procedures/
├── disaster-recovery-plan.md
├── run-dr-test.sh
├── rto-rpo-metrics.md
└── post-incident-template.md

monitoring/
└── ha-checks.yml
```

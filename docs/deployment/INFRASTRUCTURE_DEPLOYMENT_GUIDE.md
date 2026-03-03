# Infrastructure Deployment Guide

**Owner:** Infrastructure/DevOps Team
**Timeline:** February 15-20, 2026 (5 days)
**Objective:** Provision and configure production environment
**Status:** Ready to execute

---

## Overview

This guide provides step-by-step procedures for provisioning HDIM's production environment. All infrastructure code is complete and tested. This phase focuses on deployment to cloud provider (AWS/GCP/Azure) and configuration management.

---

## Day 1-2: VPC & Networking (Feb 15-16)

### Prerequisites
- [ ] AWS/GCP/Azure account with appropriate permissions
- [ ] Project/subscription created
- [ ] Billing configured
- [ ] Team access provisioned

### VPC Configuration

**Step 1: Create VPC**
```bash
# AWS Example
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --region us-east-1
# GCP Example
gcloud compute networks create hdim-vpc --subnet-mode=custom
# Azure Example
az network vnet create --resource-group hdim-prod --name hdim-vnet --address-prefix 10.0.0.0/16
```

**Step 2: Create Subnets**
```
Public Subnet (for API Gateway):
  - CIDR: 10.0.1.0/24
  - AZ: us-east-1a
  - Route to Internet Gateway

Private Subnet (for services):
  - CIDR: 10.0.2.0/24
  - AZ: us-east-1a
  - Route to NAT Gateway

Database Subnet (for RDS):
  - CIDR: 10.0.3.0/24
  - AZ: us-east-1b (different AZ for HA)
  - No direct internet route
```

**Step 3: Internet Gateway & NAT**
```bash
# Create Internet Gateway
aws ec2 create-internet-gateway

# Attach to VPC
aws ec2 attach-internet-gateway --internet-gateway-id igw-xxx --vpc-id vpc-xxx

# Create NAT Gateway (for private subnet egress)
aws ec2 allocate-address --domain vpc
aws ec2 create-nat-gateway --subnet-id subnet-public --allocation-id eipalloc-xxx
```

**Step 4: Route Tables**
```
Public Route Table:
  0.0.0.0/0 → Internet Gateway

Private Route Table:
  0.0.0.0/0 → NAT Gateway

Database Route Table:
  (No default route - internal only)
```

### Security Groups

**API Gateway Security Group**
```
Inbound:
  - HTTP (80) from 0.0.0.0/0
  - HTTPS (443) from 0.0.0.0/0

Outbound:
  - All traffic to internal services (10.0.0.0/16)
```

**Microservices Security Group**
```
Inbound:
  - API Gateway SG (ports 8084, 8086, 8087, 8098)
  - Internal services (all ports, same SG)

Outbound:
  - Database SG (port 5435)
  - Redis SG (port 6380)
  - All outbound to Jaeger (port 4318)
```

**Database Security Group**
```
Inbound:
  - Microservices SG (port 5435)

Outbound:
  - None (database only receives)
```

**Redis Security Group**
```
Inbound:
  - Microservices SG (port 6380)

Outbound:
  - None (cache only receives)
```

**Jaeger Security Group**
```
Inbound:
  - Microservices SG (port 4318 - OTLP)
  - Restricted IP ranges (port 16686 - UI, for CS access)

Outbound:
  - Storage backend (based on config)
```

---

## Day 2-3: Database Setup (Feb 16-17)

### PostgreSQL 16 Provisioning

**Step 1: Create RDS Instance**
```bash
aws rds create-db-instance \
  --db-instance-identifier hdim-postgres-prod \
  --db-instance-class db.t3.large \
  --engine postgres \
  --engine-version 16.1 \
  --master-username admin \
  --master-user-password "$(openssl rand -base64 32)" \
  --allocated-storage 100 \
  --storage-type gp3 \
  --vpc-security-group-ids sg-xxx \
  --db-subnet-group-name hdim-db-subnet \
  --multi-az \
  --backup-retention-period 30 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "sun:04:00-sun:05:00" \
  --enable-cloudwatch-logs-exports postgresql
```

**Step 2: Configure Connection Settings**
- [ ] Modify security group to allow microservices SG access (port 5435)
- [ ] Document endpoint: `hdim-postgres-prod.xxxxx.rds.amazonaws.com`
- [ ] Test connection from bastion host
- [ ] Set up SSL/TLS certificate

**Step 3: Create Application Databases (29 total)**
```sql
-- Run from management console or psql
CREATE DATABASE patient_db OWNER postgres;
CREATE DATABASE care_gap_db OWNER postgres;
CREATE DATABASE quality_measure_db OWNER postgres;
CREATE DATABASE payer_workflows_db OWNER postgres;
-- ... (25 more databases per service requirements)
```

**Step 4: Create Application User**
```sql
CREATE ROLE hdim_app WITH LOGIN PASSWORD 'xxxxx';

-- Grant permissions on all databases
GRANT CONNECT ON DATABASE patient_db TO hdim_app;
-- ... (repeat for all 29 databases)

-- In each database:
CREATE SCHEMA IF NOT EXISTS public;
GRANT USAGE ON SCHEMA public TO hdim_app;
GRANT CREATE ON SCHEMA public TO hdim_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO hdim_app;
```

**Step 5: Test Connectivity**
```bash
# From application server
psql -h hdim-postgres-prod.xxxxx.rds.amazonaws.com \
     -U hdim_app \
     -d patient_db \
     -c "SELECT 1"
```

**Step 6: Backup Configuration**
```bash
# Verify automated backups enabled
aws rds describe-db-instances --db-instance-identifier hdim-postgres-prod \
  --query 'DBInstances[0].[BackupRetentionPeriod,PreferredBackupWindow]'

# Create initial snapshot
aws rds create-db-snapshot \
  --db-instance-identifier hdim-postgres-prod \
  --db-snapshot-identifier hdim-postgres-prod-baseline
```

---

## Day 3: Redis Cache Setup (Feb 17)

### ElastiCache Redis Provisioning

**Step 1: Create Redis Cluster**
```bash
aws elasticache create-replication-group \
  --replication-group-description "HDIM Production Cache" \
  --replication-group-id hdim-redis-prod \
  --engine redis \
  --engine-version 7.0 \
  --cache-node-type cache.t3.large \
  --num-cache-clusters 3 \
  --automatic-failover-enabled \
  --subnet-group-name hdim-cache-subnet \
  --security-group-ids sg-xxx \
  --preferred-maintenance-window "sun:05:00-sun:06:00" \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled
```

**Step 2: Configure Parameters**
```
maxmemory-policy: allkeys-lru
timeout: 300
tcp-keepalive: 300
```

**Step 3: Test Connection**
```bash
# Get endpoint
aws elasticache describe-replication-groups --replication-group-id hdim-redis-prod \
  --query 'ReplicationGroups[0].PrimaryEndpoint'

# Connect from application server
redis-cli -h hdim-redis-prod.xxxxx.cache.amazonaws.com -p 6379 ping
```

---

## Day 4: Jaeger Backend Deployment (Feb 18)

### Option 1: Managed Jaeger Service (Recommended)

**GCP Cloud Trace:**
```bash
gcloud trace sink create hdim-jaeger \
  --log-filter='resource.type="gce_instance"' \
  --destination="cloud-trace"
```

**AWS X-Ray (Alternative):**
```bash
aws xray create-sampling-rule \
  --cli-input-json file://sampling-rule.json
```

### Option 2: Self-Hosted Jaeger (If Using)

**Deploy Jaeger to Kubernetes:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: jaeger
spec:
  ports:
  - name: otlp-http
    port: 4318
    protocol: TCP
  - name: ui
    port: 16686
    protocol: TCP
  selector:
    app: jaeger
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger
spec:
  replicas: 2
  selector:
    matchLabels:
      app: jaeger
  template:
    metadata:
      labels:
        app: jaeger
    spec:
      containers:
      - name: jaeger
        image: jaegertracing/all-in-one:latest
        ports:
        - containerPort: 4318  # OTLP HTTP
        - containerPort: 16686 # UI
        env:
        - name: COLLECTOR_OTLP_ENABLED
          value: "true"
        - name: SPAN_STORAGE_TYPE
          value: "elasticsearch"
        - name: ES_SERVER_URLS
          value: "http://elasticsearch:9200"
```

**Step 1: Deploy Storage Backend**
```bash
# Elasticsearch
helm install elasticsearch elastic/elasticsearch \
  --namespace jaeger \
  --values elasticsearch-values.yaml

# Or use managed service (AWS OpenSearch, GCP Datastore)
```

**Step 2: Deploy Jaeger**
```bash
kubectl apply -f jaeger-deployment.yaml
```

**Step 3: Configure Trace Retention**
```yaml
# In Elasticsearch index template:
{
  "settings": {
    "index": {
      "lifecycle": {
        "name": "jaeger-rollover",
        "rollover_alias": "jaeger-span"
      }
    }
  }
}
```

**Step 4: Test OTLP Endpoint**
```bash
curl -X POST http://jaeger:4318/v1/traces \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

## Day 5: Load Balancing & DNS (Feb 19-20)

### Application Load Balancer Setup

**Step 1: Create ALB**
```bash
aws elbv2 create-load-balancer \
  --name hdim-alb-prod \
  --subnets subnet-public-1a subnet-public-1b \
  --security-groups sg-alb \
  --scheme internet-facing \
  --type application
```

**Step 2: Create Target Groups**
```bash
# API Gateway (port 8001)
aws elbv2 create-target-group \
  --name hdim-api-gw \
  --protocol HTTP \
  --port 8001 \
  --vpc-id vpc-xxx

# Patient Service (port 8084)
aws elbv2 create-target-group \
  --name hdim-patient-svc \
  --protocol HTTP \
  --port 8084 \
  --vpc-id vpc-xxx

# Care Gap Service (port 8086)
aws elbv2 create-target-group \
  --name hdim-caregap-svc \
  --protocol HTTP \
  --port 8086 \
  --vpc-id vpc-xxx

# Quality Measure Service (port 8087)
aws elbv2 create-target-group \
  --name hdim-quality-svc \
  --protocol HTTP \
  --port 8087 \
  --vpc-id vpc-xxx
```

**Step 3: Configure Health Checks**
```bash
aws elbv2 modify-target-group \
  --target-group-arn arn:aws:elasticloadbalancing:... \
  --health-check-protocol HTTP \
  --health-check-path /health \
  --health-check-interval-seconds 15 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3
```

**Step 4: Create Listener Rules**
```bash
# Primary listener (HTTPS)
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:... \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:... \
  --ssl-policy ELBSecurityPolicy-TLS-1-2-2017-01

# HTTP redirect to HTTPS
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:... \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=redirect,RedirectConfig='{Protocol=HTTPS,Port=443,StatusCode=HTTP_301}'
```

**Step 5: Configure Path-Based Routing**
```bash
# Route /api/* to API Gateway
aws elbv2 create-rule \
  --listener-arn arn:aws:elasticloadbalancing:... \
  --conditions Field=path-pattern,Values="/api/*" \
  --actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:.../hdim-api-gw

# Route /traces to Jaeger UI
aws elbv2 create-rule \
  --listener-arn arn:aws:elasticloadbalancing:... \
  --conditions Field=path-pattern,Values="/traces/*" \
  --actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:.../hdim-jaeger
```

### DNS Configuration

**Step 1: Create Route 53 Records (AWS)**
```bash
# API endpoint
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "api.hdim.com",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "Z35SXDOTRQ7X7K",
          "DNSName": "hdim-alb-prod-xxx.us-east-1.elb.amazonaws.com",
          "EvaluateTargetHealth": true
        }
      }
    }]
  }'

# Jaeger UI
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "traces.hdim.com",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "Z35SXDOTRQ7X7K",
          "DNSName": "hdim-alb-prod-xxx.us-east-1.elb.amazonaws.com",
          "EvaluateTargetHealth": true
        }
      }
    }]
  }'
```

**Step 2: SSL/TLS Certificates**
```bash
# Request certificate (if not already done)
aws acm request-certificate \
  --domain-name hdim.com \
  --subject-alternative-names "*.hdim.com" \
  --validation-method DNS

# Verify DNS records and wait for validation
# Associate with ALB listener (done in Step 4 above)
```

**Step 3: Test DNS Resolution**
```bash
nslookup api.hdim.com
nslookup traces.hdim.com

# Should resolve to ALB IP
```

---

## Validation Checklist

### Networking ✅
```
[ ] VPC created with correct CIDR blocks
[ ] Public/private/database subnets created
[ ] Internet Gateway attached and routing configured
[ ] NAT Gateway created for private egress
[ ] Security groups configured for all components
[ ] Network ACLs reviewed (default OK for most)
```

### Database ✅
```
[ ] PostgreSQL 16 RDS instance running
[ ] Multi-AZ replication enabled
[ ] Automated backups configured (30-day retention)
[ ] All 29 databases created
[ ] Application user created with correct permissions
[ ] Connection test successful from bastion
[ ] SSL/TLS certificate installed
```

### Cache ✅
```
[ ] Redis 7 cluster created (3+ nodes for HA)
[ ] Encryption at rest enabled
[ ] Encryption in transit (TLS) enabled
[ ] Eviction policy set to allkeys-lru
[ ] Connection test successful
[ ] Monitor connections and memory usage
```

### Jaeger ✅
```
[ ] Jaeger backend deployed (managed or self-hosted)
[ ] OTLP HTTP endpoint available (port 4318)
[ ] Storage backend configured (30-day retention)
[ ] Trace ingestion tested
[ ] UI accessible at traces.hdim.com
[ ] Customer read-only credentials generated
```

### Load Balancer & DNS ✅
```
[ ] Application Load Balancer created
[ ] Target groups configured for all services
[ ] Health checks configured
[ ] HTTPS listener with valid certificate
[ ] HTTP → HTTPS redirect working
[ ] DNS records created and resolving
[ ] Path-based routing rules configured
[ ] SSL/TLS certificate valid and properly installed
```

---

## Monitoring & Verification

### Pre-Deployment Checklist
```
[ ] All infrastructure created and tested
[ ] Database backups working
[ ] Health checks responding
[ ] DNS resolving correctly
[ ] Load balancer routing correctly
[ ] Jaeger endpoint responding
[ ] Security groups allowing correct traffic
[ ] No critical CloudWatch alarms firing
```

### Post-Deployment
```
[ ] All services healthy in target groups
[ ] Database connections established
[ ] Cache connected and responding
[ ] Traces flowing to Jaeger
[ ] Monitoring dashboard populated
[ ] No errors in application logs
```

---

## Troubleshooting

### Issue: RDS Connection Timeout
**Solution:**
- Verify security group allows port 5435 from service SG
- Check route tables for NAT Gateway
- Verify RDS endpoint is correct
- Test from bastion host: `psql -h endpoint -U user -d database`

### Issue: Cache Eviction Errors
**Solution:**
- Check Redis memory usage: `redis-cli info memory`
- Increase node size if needed
- Verify eviction policy: `redis-cli config get maxmemory-policy`

### Issue: DNS Not Resolving
**Solution:**
- Verify Route53 records created: `aws route53 list-resource-record-sets`
- Check nameserver propagation: `nslookup -type=NS hdim.com`
- Wait up to 24 hours for full propagation

### Issue: Jaeger Not Receiving Traces
**Solution:**
- Verify OTLP endpoint is accessible: `curl -v http://jaeger:4318/v1/traces`
- Check firewall rules allow port 4318
- Verify environment variables in application config

---

## Security Validation

```
[ ] All data in transit encrypted (TLS 1.2+)
[ ] Database encryption at rest enabled
[ ] Backup encryption configured
[ ] Security groups follow least-privilege principle
[ ] No services exposed to internet (except ALB)
[ ] VPC Flow Logs enabled for troubleshooting
[ ] CloudTrail enabled for audit logging
```

---

## Rollback Procedures

If deployment fails:

```bash
# Destroy all resources (if starting over)
terraform destroy -auto-approve

# Or selectively destroy:
aws rds delete-db-instance \
  --db-instance-identifier hdim-postgres-prod \
  --skip-final-snapshot

aws elasticache delete-replication-group \
  --replication-group-id hdim-redis-prod \
  --retain-primary-cluster
```

---

## Next Steps

1. ✅ Complete all infrastructure provisioning
2. ✅ Document all endpoints and credentials
3. ✅ Store credentials in Vault/Secrets Manager
4. ⏳ Proceed to Phase 2B: Team Training (Feb 20-25)

---

**Generated:** February 14, 2026
**Timeline:** Feb 15-20 (5 days)
**Next Phase:** Monitoring & Team Training Setup
**Status:** Ready to execute

# GCP Deployment Guide - HealthData In Motion

**Date**: November 20, 2025
**Version**: 1.0
**Status**: Production-Ready for Healthcare

---

## Overview

This guide covers deploying HealthData In Motion to Google Cloud Platform (GCP) with full HIPAA compliance for healthcare use cases.

### Why GCP for Healthcare

✅ **HIPAA Compliant** - Sign Business Associate Agreement (BAA)
✅ **Healthcare API** - Built-in FHIR support (Cloud Healthcare API)
✅ **Competitive Pricing** - Often 20-30% cheaper than AWS
✅ **Global Network** - Premium tier with low latency
✅ **Security** - VPC Service Controls, binary authorization
✅ **Proven** - Used by Mayo Clinic, HCA Healthcare, Stanford Health

---

## Architecture

```
Internet
    ↓
Cloud CDN / Load Balancer (HTTPS)
    ↓
┌────────────────────────────────────────────────┐
│   Vercel (Frontend)                            │
│   clinical.healthdata-in-motion.com            │
└────────────────────────────────────────────────┘
    ↓ HTTPS API Calls
┌────────────────────────────────────────────────┐
│   GCP Cloud Load Balancer                      │
│   api.healthdata-in-motion.com                 │
│   ┌──────────────────────────────────────────┐ │
│   │ Cloud Armor (DDoS, WAF)                  │ │
│   └──────────────────────────────────────────┘ │
└────────────────┬───────────────────────────────┘
                 ↓
┌────────────────────────────────────────────────┐
│   GCP Cloud Run (Managed Containers)           │
│   ┌──────────────────────────────────────────┐ │
│   │ Quality Measure Service                  │ │
│   │ (Docker container, auto-scaling)         │ │
│   │ Min: 1, Max: 10 instances                │ │
│   └──────────────────────────────────────────┘ │
│   ┌──────────────────────────────────────────┐ │
│   │ CQL Engine Service                       │ │
│   │ (Docker container, auto-scaling)         │ │
│   └──────────────────────────────────────────┘ │
│   ┌──────────────────────────────────────────┐ │
│   │ FHIR Server (HAPI FHIR)                  │ │
│   │ (Docker container, auto-scaling)         │ │
│   └──────────────────────────────────────────┘ │
└────────────────┬───────────────────────────────┘
                 ↓
┌────────────────────────────────────────────────┐
│   GCP VPC (Private Network)                    │
│   ┌──────────────────┬───────────────────────┐ │
│   │ Cloud SQL        │ Memorystore (Redis)   │ │
│   │ PostgreSQL 16    │ High availability     │ │
│   │ Encrypted        │ 4 GB RAM              │ │
│   │ Auto backups     │                       │ │
│   └──────────────────┴───────────────────────┘ │
└────────────────────────────────────────────────┘
                 ↓
┌────────────────────────────────────────────────┐
│   GCP Security & Compliance                    │
│   ┌──────────────────────────────────────────┐ │
│   │ Secret Manager - JWT secrets, DB creds  │ │
│   │ Cloud KMS - Encryption keys              │ │
│   │ Cloud Audit Logs - HIPAA audit trail     │ │
│   │ VPC Service Controls - Data perimeter    │ │
│   └──────────────────────────────────────────┘ │
└────────────────────────────────────────────────┘
```

---

## Prerequisites

### 1. GCP Account Setup
```bash
# Create GCP account at cloud.google.com
# Enable billing
# Create new project: healthdata-in-motion
```

### 2. Install Google Cloud SDK
```bash
# macOS
brew install --cask google-cloud-sdk

# Linux
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# Initialize
gcloud init
# Select: healthdata-in-motion project
# Select: us-central1 region (Iowa - HIPAA compliant)
```

### 3. Enable Required APIs
```bash
gcloud services enable \
  run.googleapis.com \
  sql-component.googleapis.com \
  sqladmin.googleapis.com \
  redis.googleapis.com \
  secretmanager.googleapis.com \
  cloudkms.googleapis.com \
  cloudbuild.googleapis.com \
  containerregistry.googleapis.com \
  compute.googleapis.com \
  healthcare.googleapis.com
```

### 4. Sign HIPAA BAA
```bash
# Go to: https://cloud.google.com/terms/hipaa
# Complete BAA process with Google
# Required for production healthcare workloads
```

---

## Cost Estimation

### Monthly Costs (Production Setup)

| Service | Configuration | Monthly Cost |
|---------|---------------|--------------|
| **Cloud Run** (Quality Service) | 1-3 instances, 1 vCPU, 2GB RAM | $25-50 |
| **Cloud Run** (CQL Service) | 1-2 instances, 1 vCPU, 2GB RAM | $20-40 |
| **Cloud Run** (FHIR Service) | 1-2 instances, 1 vCPU, 2GB RAM | $20-40 |
| **Cloud SQL PostgreSQL** | db-n1-standard-1 (1 vCPU, 3.75GB) | $50 |
| **Memorystore Redis** | 4 GB basic tier | $25 |
| **Cloud Load Balancer** | With SSL | $20 |
| **Cloud Storage** | Backups and logs (100GB) | $2 |
| **Secret Manager** | ~20 secrets | $1 |
| **Cloud Logging** | 50 GB/month | $2.50 |
| **Egress Traffic** | ~100 GB/month | $12 |
| **Total** | | **$177-232/month** |

**Budget Notes:**
- Cloud Run scales to zero (pay only for usage)
- First 2 million requests free per month
- Development/staging can run <$50/month (smaller instances, scale to zero)

---

## Quick Deploy (Step-by-Step)

### Step 1: Set Up Project and Environment (10 min)

```bash
# Set project
export PROJECT_ID=healthdata-in-motion
export REGION=us-central1
export ZONE=us-central1-a

gcloud config set project $PROJECT_ID
gcloud config set compute/region $REGION
gcloud config set compute/zone $ZONE

# Create VPC network (for private connectivity)
gcloud compute networks create healthdata-vpc \
  --subnet-mode=auto \
  --bgp-routing-mode=regional

# Create VPC connector for Cloud Run
gcloud compute networks vpc-access connectors create healthdata-connector \
  --region=$REGION \
  --network=healthdata-vpc \
  --range=10.8.0.0/28
```

---

### Step 2: Create Cloud SQL PostgreSQL Database (15 min)

```bash
# Generate secure password
DB_PASSWORD=$(openssl rand -base64 32)
echo "Database password: $DB_PASSWORD"
# SAVE THIS PASSWORD IN YOUR PASSWORD MANAGER!

# Create Cloud SQL instance
gcloud sql instances create healthdata-postgres \
  --database-version=POSTGRES_16 \
  --tier=db-n1-standard-1 \
  --region=$REGION \
  --network=projects/$PROJECT_ID/global/networks/healthdata-vpc \
  --no-assign-ip \
  --storage-type=SSD \
  --storage-size=20GB \
  --storage-auto-increase \
  --backup-start-time=03:00 \
  --retained-backups-count=7 \
  --enable-bin-log \
  --maintenance-window-day=SUN \
  --maintenance-window-hour=3 \
  --database-flags=cloudsql.iam_authentication=on

# Set root password
gcloud sql users set-password postgres \
  --instance=healthdata-postgres \
  --password=$DB_PASSWORD

# Create application database
gcloud sql databases create healthdata_cql \
  --instance=healthdata-postgres

# Create application user
gcloud sql users create healthdata \
  --instance=healthdata-postgres \
  --password=$DB_PASSWORD

# Get connection name
gcloud sql instances describe healthdata-postgres --format="value(connectionName)"
# Output: PROJECT_ID:REGION:healthdata-postgres
# Save this - you'll need it for DATABASE_URL
```

**Database URL Format:**
```
DATABASE_URL=postgresql://healthdata:$DB_PASSWORD@/healthdata_cql?host=/cloudsql/PROJECT_ID:REGION:healthdata-postgres
```

---

### Step 3: Create Memorystore Redis (10 min)

```bash
# Create Redis instance
gcloud redis instances create healthdata-redis \
  --size=4 \
  --region=$REGION \
  --network=projects/$PROJECT_ID/global/networks/healthdata-vpc \
  --redis-version=redis_7_0 \
  --tier=basic

# Get Redis host and port
gcloud redis instances describe healthdata-redis \
  --region=$REGION \
  --format="value(host)"
# Save this IP

gcloud redis instances describe healthdata-redis \
  --region=$REGION \
  --format="value(port)"
# Default: 6379
```

**Redis URL:**
```
REDIS_URL=redis://[REDIS_HOST]:6379
```

---

### Step 4: Store Secrets in Secret Manager (5 min)

```bash
# Generate JWT secret
JWT_SECRET=$(openssl rand -base64 64)

# Store database password
echo -n "$DB_PASSWORD" | gcloud secrets create database-password \
  --data-file=- \
  --replication-policy=automatic

# Store JWT secret
echo -n "$JWT_SECRET" | gcloud secrets create jwt-secret \
  --data-file=- \
  --replication-policy=automatic

# Store full database URL
DB_CONNECTION_NAME=$(gcloud sql instances describe healthdata-postgres --format="value(connectionName)")
DATABASE_URL="postgresql://healthdata:$DB_PASSWORD@/healthdata_cql?host=/cloudsql/$DB_CONNECTION_NAME"

echo -n "$DATABASE_URL" | gcloud secrets create database-url \
  --data-file=- \
  --replication-policy=automatic

# Verify secrets
gcloud secrets list
```

---

### Step 5: Build and Push Docker Images (15 min)

```bash
# Enable Artifact Registry (replaces Container Registry)
gcloud services enable artifactregistry.googleapis.com

# Create Artifact Registry repository
gcloud artifacts repositories create healthdata-docker \
  --repository-format=docker \
  --location=$REGION \
  --description="HealthData In Motion Docker images"

# Configure Docker authentication
gcloud auth configure-docker ${REGION}-docker.pkg.dev

# Build Quality Measure Service
cd backend/modules/services/quality-measure-service
docker build -t ${REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/quality-measure-service:latest \
  -f Dockerfile ../../../

# Push to Artifact Registry
docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/quality-measure-service:latest

# Build CQL Engine Service
cd ../cql-engine-service
docker build -t ${REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/cql-engine-service:latest \
  -f Dockerfile ../../../

docker push ${REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/cql-engine-service:latest

# Verify images
gcloud artifacts docker images list ${REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker
```

---

### Step 6: Deploy to Cloud Run (20 min)

```bash
# Deploy Quality Measure Service
gcloud run deploy quality-measure-service \
  --image=${REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/quality-measure-service:latest \
  --platform=managed \
  --region=$REGION \
  --allow-unauthenticated \
  --vpc-connector=healthdata-connector \
  --set-env-vars="SPRING_PROFILES_ACTIVE=docker" \
  --add-cloudsql-instances=${PROJECT_ID}:${REGION}:healthdata-postgres \
  --set-secrets="DATABASE_URL=database-url:latest,JWT_SECRET=jwt-secret:latest" \
  --min-instances=1 \
  --max-instances=10 \
  --memory=2Gi \
  --cpu=1 \
  --timeout=300 \
  --port=8087 \
  --service-account=quality-measure-sa@${PROJECT_ID}.iam.gserviceaccount.com

# Get service URL
gcloud run services describe quality-measure-service \
  --region=$REGION \
  --format="value(status.url)"
# Output: https://quality-measure-service-xxxxx-uc.a.run.app

# Deploy CQL Engine Service
gcloud run deploy cql-engine-service \
  --image=${REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/cql-engine-service:latest \
  --platform=managed \
  --region=$REGION \
  --allow-unauthenticated \
  --vpc-connector=healthdata-connector \
  --set-env-vars="SPRING_PROFILES_ACTIVE=docker" \
  --add-cloudsql-instances=${PROJECT_ID}:${REGION}:healthdata-postgres \
  --set-secrets="DATABASE_URL=database-url:latest" \
  --min-instances=1 \
  --max-instances=5 \
  --memory=2Gi \
  --cpu=1 \
  --timeout=300 \
  --port=8081

# Get CQL service URL
gcloud run services describe cql-engine-service \
  --region=$REGION \
  --format="value(status.url)"
```

---

### Step 7: Create Service Accounts (Security Best Practice)

```bash
# Create service account for Quality Measure Service
gcloud iam service-accounts create quality-measure-sa \
  --display-name="Quality Measure Service Account"

# Grant Cloud SQL client role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:quality-measure-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"

# Grant Secret Manager accessor role
gcloud secrets add-iam-policy-binding database-url \
  --member="serviceAccount:quality-measure-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

gcloud secrets add-iam-policy-binding jwt-secret \
  --member="serviceAccount:quality-measure-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

# Update Cloud Run service to use service account
gcloud run services update quality-measure-service \
  --region=$REGION \
  --service-account=quality-measure-sa@${PROJECT_ID}.iam.gserviceaccount.com
```

---

### Step 8: Set Up Load Balancer with Custom Domain (20 min)

```bash
# Reserve static IP
gcloud compute addresses create healthdata-api-ip \
  --global

# Get the IP address
gcloud compute addresses describe healthdata-api-ip \
  --global \
  --format="value(address)"
# Output: 34.120.xxx.xxx
# Add this as A record in your DNS: api.healthdata-in-motion.com

# Create backend service
gcloud compute backend-services create quality-measure-backend \
  --global \
  --load-balancing-scheme=EXTERNAL_MANAGED \
  --protocol=HTTP

# Create serverless NEG for Cloud Run
gcloud compute network-endpoint-groups create quality-measure-neg \
  --region=$REGION \
  --network-endpoint-type=serverless \
  --cloud-run-service=quality-measure-service

# Add backend
gcloud compute backend-services add-backend quality-measure-backend \
  --global \
  --network-endpoint-group=quality-measure-neg \
  --network-endpoint-group-region=$REGION

# Create URL map
gcloud compute url-maps create healthdata-api-lb \
  --default-service=quality-measure-backend

# Create SSL certificate (managed by Google)
gcloud compute ssl-certificates create healthdata-api-cert \
  --domains=api.healthdata-in-motion.com \
  --global

# Create HTTPS proxy
gcloud compute target-https-proxies create healthdata-api-proxy \
  --ssl-certificates=healthdata-api-cert \
  --url-map=healthdata-api-lb

# Create forwarding rule
gcloud compute forwarding-rules create healthdata-api-https \
  --global \
  --target-https-proxy=healthdata-api-proxy \
  --address=healthdata-api-ip \
  --ports=443
```

**DNS Configuration:**
```
A record:
api.healthdata-in-motion.com → 34.120.xxx.xxx (your static IP)

Wait 5-10 minutes for SSL certificate provisioning
```

---

### Step 9: Configure Cloud Armor (DDoS Protection & WAF) (10 min)

```bash
# Create security policy
gcloud compute security-policies create healthdata-waf \
  --description="WAF for HealthData API"

# Add rate limiting rule (100 requests per minute per IP)
gcloud compute security-policies rules create 1000 \
  --security-policy=healthdata-waf \
  --expression="origin.region_code != 'US'" \
  --action=deny-403 \
  --description="Block non-US traffic"

# Add rate limiting
gcloud compute security-policies rules create 2000 \
  --security-policy=healthdata-waf \
  --expression="true" \
  --action=rate-based-ban \
  --rate-limit-threshold-count=100 \
  --rate-limit-threshold-interval-sec=60 \
  --ban-duration-sec=600 \
  --conform-action=allow \
  --exceed-action=deny-429 \
  --enforce-on-key=IP

# Attach to backend service
gcloud compute backend-services update quality-measure-backend \
  --global \
  --security-policy=healthdata-waf
```

---

### Step 10: Deploy Frontend to Vercel (5 min)

```bash
# Get Cloud Run URLs
QUALITY_MEASURE_URL=$(gcloud run services describe quality-measure-service \
  --region=$REGION \
  --format="value(status.url)")

CQL_ENGINE_URL=$(gcloud run services describe cql-engine-service \
  --region=$REGION \
  --format="value(status.url)")

# Set Vercel environment variables
vercel env add API_GATEWAY_URL production
# Enter: https://api.healthdata-in-motion.com (once DNS propagates)
# Or temporarily: $QUALITY_MEASURE_URL

vercel env add QUALITY_MEASURE_URL production
# Enter: $QUALITY_MEASURE_URL

vercel env add CQL_ENGINE_URL production
# Enter: $CQL_ENGINE_URL

# Deploy to Vercel
vercel --prod
```

---

## HIPAA Compliance Configuration

### 1. Enable Audit Logging

```bash
# Create audit log sink for HIPAA compliance
gcloud logging sinks create healthdata-audit-sink \
  gs://healthdata-audit-logs-${PROJECT_ID} \
  --log-filter='protoPayload.serviceName="run.googleapis.com" OR
                protoPayload.serviceName="sqladmin.googleapis.com" OR
                protoPayload.serviceName="secretmanager.googleapis.com"'

# Create bucket with retention policy
gsutil mb -p $PROJECT_ID -c STANDARD -l $REGION gs://healthdata-audit-logs-${PROJECT_ID}

# Set 7-year retention (HIPAA requirement)
gsutil retention set 7y gs://healthdata-audit-logs-${PROJECT_ID}

# Lock retention policy (cannot be removed)
gsutil retention lock gs://healthdata-audit-logs-${PROJECT_ID}
```

### 2. Enable VPC Service Controls (Data Perimeter)

```bash
# Create access policy
gcloud access-context-manager policies create \
  --title="HealthData Access Policy"

# Get policy name
POLICY_NAME=$(gcloud access-context-manager policies list --format="value(name)")

# Create access level (restrict to your organization)
gcloud access-context-manager levels create healthdata_trusted \
  --policy=$POLICY_NAME \
  --title="HealthData Trusted Access" \
  --basic-level-spec=access_level.yaml

# access_level.yaml:
cat > access_level.yaml <<EOF
- ipSubnetworks:
  - YOUR_OFFICE_IP/32
  members:
  - user:admin@healthdata-in-motion.com
EOF

# Create service perimeter
gcloud access-context-manager perimeters create healthdata_perimeter \
  --policy=$POLICY_NAME \
  --title="HealthData Service Perimeter" \
  --resources=projects/${PROJECT_NUMBER} \
  --restricted-services=storage.googleapis.com,sqladmin.googleapis.com,secretmanager.googleapis.com \
  --access-levels=healthdata_trusted
```

### 3. Enable Binary Authorization (Container Security)

```bash
# Create attestor
gcloud container binauthz attestors create healthdata-attestor \
  --attestation-authority-note=healthdata-note \
  --attestation-authority-note-project=$PROJECT_ID

# Create policy requiring attestation
cat > binauthz-policy.yaml <<EOF
globalPolicyEvaluationMode: ENABLE
defaultAdmissionRule:
  requireAttestationsBy:
  - projects/$PROJECT_ID/attestors/healthdata-attestor
  enforcementMode: ENFORCED_BLOCK_AND_AUDIT_LOG
EOF

gcloud container binauthz policy import binauthz-policy.yaml
```

### 4. Configure Customer-Managed Encryption Keys (CMEK)

```bash
# Create KMS keyring
gcloud kms keyrings create healthdata-keyring \
  --location=$REGION

# Create encryption key for database
gcloud kms keys create database-encryption-key \
  --location=$REGION \
  --keyring=healthdata-keyring \
  --purpose=encryption \
  --rotation-period=90d \
  --next-rotation-time=$(date -d '+90 days' +%Y-%m-%dT%H:%M:%S%z)

# Grant Cloud SQL service account access to key
gcloud kms keys add-iam-policy-binding database-encryption-key \
  --location=$REGION \
  --keyring=healthdata-keyring \
  --member=serviceAccount:service-${PROJECT_NUMBER}@gcp-sa-cloud-sql.iam.gserviceaccount.com \
  --role=roles/cloudkms.cryptoKeyEncrypterDecrypter

# Update Cloud SQL instance to use CMEK
gcloud sql instances patch healthdata-postgres \
  --disk-encryption-key=projects/$PROJECT_ID/locations/$REGION/keyRings/healthdata-keyring/cryptoKeys/database-encryption-key
```

---

## Monitoring & Logging

### 1. Set Up Cloud Monitoring Dashboard

```bash
# Create monitoring workspace (automatic for new projects)
# Navigate to: https://console.cloud.google.com/monitoring

# Create dashboard via CLI
gcloud monitoring dashboards create --config-from-file=dashboard.json
```

**dashboard.json:**
```json
{
  "displayName": "HealthData In Motion",
  "mosaicLayout": {
    "columns": 12,
    "tiles": [
      {
        "width": 6,
        "height": 4,
        "widget": {
          "title": "Cloud Run Request Count",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "resource.type=\"cloud_run_revision\" resource.label.service_name=\"quality-measure-service\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_RATE"
                  }
                }
              }
            }]
          }
        }
      },
      {
        "width": 6,
        "height": 4,
        "widget": {
          "title": "Cloud Run Request Latency (p95)",
          "xyChart": {
            "dataSets": [{
              "timeSeriesQuery": {
                "timeSeriesFilter": {
                  "filter": "resource.type=\"cloud_run_revision\" metric.type=\"run.googleapis.com/request_latencies\"",
                  "aggregation": {
                    "alignmentPeriod": "60s",
                    "perSeriesAligner": "ALIGN_DELTA",
                    "crossSeriesReducer": "REDUCE_PERCENTILE_95"
                  }
                }
              }
            }]
          }
        }
      }
    ]
  }
}
```

### 2. Set Up Alerts

```bash
# Create alert for high error rate
gcloud alpha monitoring policies create --notification-channels=CHANNEL_ID \
  --display-name="High Error Rate" \
  --condition-display-name="Error rate > 5%" \
  --condition-threshold-value=0.05 \
  --condition-threshold-duration=300s \
  --condition-filter='resource.type="cloud_run_revision" AND metric.type="run.googleapis.com/request_count" AND metric.label.response_code_class="5xx"'

# Create alert for high latency
gcloud alpha monitoring policies create --notification-channels=CHANNEL_ID \
  --display-name="High Latency" \
  --condition-display-name="P95 latency > 1000ms" \
  --condition-threshold-value=1000 \
  --condition-threshold-duration=300s \
  --condition-filter='resource.type="cloud_run_revision" AND metric.type="run.googleapis.com/request_latencies"'

# Create alert for database CPU
gcloud alpha monitoring policies create --notification-channels=CHANNEL_ID \
  --display-name="High Database CPU" \
  --condition-display-name="Database CPU > 80%" \
  --condition-threshold-value=0.8 \
  --condition-threshold-duration=300s \
  --condition-filter='resource.type="cloudsql_database" AND metric.type="cloudsql.googleapis.com/database/cpu/utilization"'
```

### 3. Set Up Log-Based Metrics

```bash
# Count authentication failures
gcloud logging metrics create auth-failures \
  --description="Count of authentication failures" \
  --log-filter='resource.type="cloud_run_revision" AND
                jsonPayload.message=~"Authentication failed" AND
                severity="ERROR"'

# Count PHI access (for HIPAA audit)
gcloud logging metrics create phi-access \
  --description="Count of PHI access events" \
  --log-filter='resource.type="cloud_run_revision" AND
                jsonPayload.message=~"PHI accessed" AND
                jsonPayload.patientId!=""'
```

---

## Backup & Disaster Recovery

### 1. Database Backups

```bash
# Automated backups are already enabled (7-day retention)
# Create on-demand backup
gcloud sql backups create \
  --instance=healthdata-postgres \
  --description="Pre-deployment backup"

# List backups
gcloud sql backups list --instance=healthdata-postgres

# Restore from backup (if needed)
gcloud sql backups restore BACKUP_ID \
  --backup-instance=healthdata-postgres \
  --backup-id=BACKUP_ID
```

### 2. Export Database (for long-term retention)

```bash
# Create bucket for exports
gsutil mb -p $PROJECT_ID -c NEARLINE -l $REGION gs://healthdata-db-exports-${PROJECT_ID}

# Export database
gcloud sql export sql healthdata-postgres \
  gs://healthdata-db-exports-${PROJECT_ID}/export-$(date +%Y%m%d).sql.gz \
  --database=healthdata_cql

# Schedule monthly exports with Cloud Scheduler
gcloud scheduler jobs create http db-export-monthly \
  --schedule="0 2 1 * *" \
  --uri="https://sqladmin.googleapis.com/sql/v1beta4/projects/${PROJECT_ID}/instances/healthdata-postgres/export" \
  --http-method=POST \
  --message-body="{\"exportContext\":{\"fileType\":\"SQL\",\"uri\":\"gs://healthdata-db-exports-${PROJECT_ID}/monthly-export.sql.gz\",\"databases\":[\"healthdata_cql\"]}}" \
  --oauth-service-account-email=quality-measure-sa@${PROJECT_ID}.iam.gserviceaccount.com
```

### 3. Multi-Region Disaster Recovery (Optional - Enterprise)

```bash
# Create read replica in different region (for DR)
gcloud sql instances create healthdata-postgres-replica \
  --master-instance-name=healthdata-postgres \
  --tier=db-n1-standard-1 \
  --region=us-east1

# In disaster scenario, promote replica to master
gcloud sql instances promote-replica healthdata-postgres-replica
```

---

## Security Best Practices Checklist

### Infrastructure Security
- [x] VPC with private IP addresses for database
- [x] VPC Service Controls (data perimeter)
- [x] Cloud Armor (DDoS + WAF)
- [x] SSL/TLS everywhere (managed certificates)
- [x] Service accounts with least privilege
- [x] Binary authorization for containers

### Data Security
- [x] Customer-managed encryption keys (CMEK)
- [x] Database encrypted at rest and in transit
- [x] Secrets in Secret Manager (not environment variables)
- [x] Audit logging with 7-year retention
- [x] Automated backups (7-day retention)

### Application Security
- [ ] JWT authentication enabled (configure in Spring Boot)
- [ ] CORS restricted to Vercel domain only
- [ ] Rate limiting (via Cloud Armor)
- [ ] Input validation and sanitization
- [ ] SQL injection protection (parameterized queries)

### Compliance
- [x] BAA signed with Google
- [x] Audit logs enabled
- [x] Encryption at rest and in transit
- [x] Access controls (IAM)
- [ ] Regular security scanning (add Cloud Security Command Center)
- [ ] Vulnerability assessments (add Web Security Scanner)

---

## Cost Optimization Tips

### 1. Right-Size Instances
```bash
# Monitor actual usage for 2 weeks
# Then adjust Cloud Run memory/CPU if over-provisioned
gcloud run services update quality-measure-service \
  --region=$REGION \
  --memory=1Gi \
  --cpu=1
```

### 2. Use Cloud Run Scale-to-Zero
```bash
# For non-production environments
gcloud run services update quality-measure-service \
  --region=$REGION \
  --min-instances=0 \
  --max-instances=3
# Saves money during off-hours
```

### 3. Use Committed Use Discounts
```bash
# For predictable workloads, commit to 1-year or 3-year
# Savings: 25-55%
# Purchase in console: Billing → Commitments
```

### 4. Enable Cloud SQL High Availability Only in Production
```bash
# Staging/dev can use single-zone
gcloud sql instances patch healthdata-postgres-staging \
  --no-availability-type=REGIONAL
# Saves ~50% on database costs
```

### 5. Use Preemptible or Spot VMs (for batch jobs)
```bash
# If you add batch processing later
# Use preemptible VMs (up to 80% cheaper)
```

---

## Troubleshooting

### Issue: Cloud Run service won't start

**Check logs:**
```bash
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=quality-measure-service" \
  --limit=50 \
  --format=json
```

**Common fixes:**
- Database connection: Verify Cloud SQL connection string
- Secrets: Check Secret Manager permissions
- VPC: Ensure VPC connector is attached

### Issue: Database connection timeout

```bash
# Test connectivity from Cloud Run
gcloud run services update quality-measure-service \
  --region=$REGION \
  --vpc-connector=healthdata-connector \
  --add-cloudsql-instances=${PROJECT_ID}:${REGION}:healthdata-postgres

# Check Cloud SQL logs
gcloud sql operations list --instance=healthdata-postgres
```

### Issue: SSL certificate not provisioning

```bash
# Check certificate status
gcloud compute ssl-certificates describe healthdata-api-cert \
  --global

# Common causes:
# 1. DNS not propagated (wait 10-30 minutes)
# 2. DNS pointing to wrong IP
# 3. Firewall blocking port 443
```

### Issue: High costs

```bash
# Check usage by service
gcloud billing accounts list
gcloud billing projects link $PROJECT_ID --billing-account=BILLING_ACCOUNT_ID

# View cost breakdown
# Go to: console.cloud.google.com/billing/reports

# Top cost drivers:
# 1. Cloud Run instances (scale to zero when not in use)
# 2. Cloud SQL (downgrade tier if over-provisioned)
# 3. Egress traffic (use Cloud CDN for static assets)
```

---

## CI/CD with Cloud Build

### Automated Deployment on Git Push

**Create cloudbuild.yaml:**
```yaml
steps:
  # Build Docker image
  - name: 'gcr.io/cloud-builders/docker'
    args: [
      'build',
      '-t', '${_REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/quality-measure-service:$COMMIT_SHA',
      '-t', '${_REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/quality-measure-service:latest',
      '-f', 'backend/modules/services/quality-measure-service/Dockerfile',
      'backend/'
    ]

  # Push to Artifact Registry
  - name: 'gcr.io/cloud-builders/docker'
    args: [
      'push',
      '${_REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/quality-measure-service:$COMMIT_SHA'
    ]

  # Deploy to Cloud Run
  - name: 'gcr.io/google.com/cloudsdktool/cloud-sdk'
    entrypoint: gcloud
    args: [
      'run', 'deploy', 'quality-measure-service',
      '--image', '${_REGION}-docker.pkg.dev/${PROJECT_ID}/healthdata-docker/quality-measure-service:$COMMIT_SHA',
      '--region', '${_REGION}',
      '--platform', 'managed'
    ]

substitutions:
  _REGION: us-central1

timeout: 1200s
```

**Set up trigger:**
```bash
# Connect GitHub repository
gcloud builds triggers create github \
  --name=quality-measure-deploy \
  --repo-name=healthdata-in-motion \
  --repo-owner=YOUR_GITHUB_USERNAME \
  --branch-pattern="^main$" \
  --build-config=cloudbuild.yaml
```

---

## Next Steps

### This Week
1. [ ] Create GCP project and enable APIs
2. [ ] Sign HIPAA BAA
3. [ ] Deploy Cloud SQL database
4. [ ] Deploy Quality Measure Service to Cloud Run
5. [ ] Test health endpoint

### Next Week
1. [ ] Deploy remaining services
2. [ ] Set up custom domain and SSL
3. [ ] Configure Cloud Armor
4. [ ] Enable monitoring and alerts
5. [ ] Update frontend to use GCP backend

### This Month
1. [ ] Set up VPC Service Controls
2. [ ] Configure audit logging
3. [ ] Security scan with Cloud Security Command Center
4. [ ] Load testing
5. [ ] Production launch

---

## Support Resources

### Documentation
- GCP Healthcare Solutions: https://cloud.google.com/solutions/healthcare
- Cloud Run: https://cloud.google.com/run/docs
- Cloud SQL: https://cloud.google.com/sql/docs
- HIPAA Compliance: https://cloud.google.com/security/compliance/hipaa

### Support
- GCP Support: console.cloud.google.com/support
- Community: stackoverflow.com (tag: google-cloud-platform)
- Status: status.cloud.google.com

---

**Document Version**: 1.0
**Last Updated**: November 20, 2025
**Next Review**: After first production deployment

---

*Your production-grade, HIPAA-compliant deployment on Google Cloud Platform is ready to go!*

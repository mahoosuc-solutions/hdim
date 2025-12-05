# Backend Deployment Options - HealthData In Motion

**Date**: November 20, 2025
**Version**: 1.0
**Purpose**: Compare cloud deployment options for Spring Boot backend services

---

## Overview

The HealthData In Motion backend consists of:
- Quality Measure Service (Port 8087)
- CQL Engine Service (Port 8081)
- FHIR Server (Port 8083)
- PostgreSQL Database
- Redis Cache
- Kong API Gateway (Port 8000)

All services are containerized with Docker and can be deployed to various cloud platforms.

---

## Deployment Architecture

```
Internet
    ↓
Load Balancer / CDN
    ↓
┌────────────────────────────────────┐
│   Vercel (Frontend)                │
│   clinical.healthdata.com          │
└────────────────────────────────────┘
    ↓ HTTPS API Calls
┌────────────────────────────────────┐
│   Kong API Gateway                 │
│   api.healthdata.com:8000          │
│   - Route /api/quality → :8087     │
│   - Route /api/cql → :8081         │
│   - Route /api/fhir → :8083        │
└────────────────────────────────────┘
    ↓
┌────────────────────────────────────┐
│   Backend Services (Docker)        │
│   ┌──────────────────────────────┐ │
│   │ Quality Measure Service      │ │
│   │ (Spring Boot + JWT)          │ │
│   └──────────────────────────────┘ │
│   ┌──────────────────────────────┐ │
│   │ CQL Engine Service           │ │
│   └──────────────────────────────┘ │
│   ┌──────────────────────────────┐ │
│   │ FHIR Server                  │ │
│   └──────────────────────────────┘ │
└────────────────────────────────────┘
    ↓
┌────────────────────────────────────┐
│   Data Layer                       │
│   ┌──────────┬──────────┐          │
│   │PostgreSQL│  Redis   │          │
│   └──────────┴──────────┘          │
└────────────────────────────────────┘
```

---

## Option 1: Railway (⭐ Recommended for Quick Start)

### Overview
Railway is a modern platform-as-a-service that simplifies deployment with automatic HTTPS, Git integration, and built-in database services.

### Pros
✅ **Easiest to set up** - Deploy in <30 minutes
✅ **Git integration** - Auto-deploy on push
✅ **Built-in PostgreSQL** - One-click database
✅ **Automatic HTTPS** - Free SSL certificates
✅ **Simple pricing** - $5/month starter, pay-as-you-go
✅ **Docker support** - Works with your existing images
✅ **Environment variables** - Easy secrets management
✅ **Metrics & logs** - Built-in monitoring

### Cons
❌ **Smaller scale** - Best for <100K requests/day
❌ **Limited customization** - Less control than AWS
❌ **No HIPAA BAA** - Not suitable for PHI in production (yet)

### Pricing
- **Starter**: $5/month (includes $5 credit)
- **Developer**: $20/month (includes $20 credit)
- **Team**: $50/month (includes $50 credit)
- **Usage-based**: ~$0.000463/GB-hr (compute) + $0.25/GB (egress)

**Estimated Monthly Cost**:
- Quality Measure Service: $15
- CQL Engine Service: $10
- FHIR Server: $10
- PostgreSQL: $10
- Redis: $5
- **Total: ~$50/month**

### Setup Steps

1. **Create Railway Account**:
```bash
# Sign up at railway.app
# Or install CLI
npm install -g @railway/cli
railway login
```

2. **Create New Project**:
```bash
railway init
# Select "Empty Project"
```

3. **Add PostgreSQL**:
```bash
railway add postgresql
# Railway provisions database automatically
```

4. **Deploy Quality Measure Service**:
```bash
# From backend/modules/services/quality-measure-service/
railway up

# Or connect GitHub repository
# Railway auto-detects Dockerfile and builds
```

5. **Add Environment Variables**:
```bash
# Railway Dashboard → Variables
DATABASE_URL=${DATABASE_URL}  # Auto-set by Railway
SPRING_PROFILES_ACTIVE=docker
JWT_SECRET=your-production-secret
REDIS_URL=${REDIS_URL}
```

6. **Get Public URL**:
```bash
# Railway generates: quality-measure-production.up.railway.app
# Add custom domain: api.healthdata-in-motion.com
```

### Custom Domain Setup

```bash
# In Railway Dashboard
# Project → Settings → Domains → Add Custom Domain
# Enter: api.healthdata-in-motion.com

# Add CNAME in your DNS:
# api.healthdata-in-motion.com → quality-measure-production.up.railway.app
```

---

## Option 2: Render (⭐ Good Balance of Features & Cost)

### Overview
Render is a cloud platform that offers more control than Railway but is simpler than AWS, with automatic SSL and Git deployment.

### Pros
✅ **Zero-downtime deploys** - Blue-green deployments
✅ **Auto-scaling** - Horizontal pod autoscaler
✅ **PostgreSQL included** - Managed database
✅ **Free SSL** - Automatic HTTPS
✅ **Docker support** - Native support
✅ **Cron jobs** - Scheduled tasks
✅ **More scale** - Handles 1M+ requests/day

### Cons
❌ **More expensive** - $7/month minimum per service
❌ **US/EU only** - Limited regions
❌ **No HIPAA BAA** - Not certified for PHI

### Pricing
- **Free Tier**: $0/month (spins down after 15 min idle)
- **Starter**: $7/month per service
- **Standard**: $25/month per service (auto-scaling)
- **Pro**: $85/month per service (dedicated CPU)
- **PostgreSQL**: $7/month (Starter), $25/month (Standard)

**Estimated Monthly Cost**:
- Quality Measure Service: $25 (Standard)
- CQL Engine Service: $25 (Standard)
- FHIR Server: $25 (Standard)
- PostgreSQL: $25 (Standard)
- Redis: $7 (Starter)
- **Total: ~$107/month**

### Setup Steps

1. **Create Render Account**:
```bash
# Sign up at render.com
# Connect GitHub repository
```

2. **Create Blueprint** (`render.yaml`):
```yaml
services:
  - type: web
    name: quality-measure-service
    env: docker
    dockerfilePath: ./backend/modules/services/quality-measure-service/Dockerfile
    dockerContext: ./backend
    envVars:
      - key: DATABASE_URL
        fromDatabase:
          name: healthdata-postgres
          property: connectionString
      - key: SPRING_PROFILES_ACTIVE
        value: docker
      - key: JWT_SECRET
        generateValue: true
      - key: PORT
        value: 8087

  - type: web
    name: cql-engine-service
    env: docker
    dockerfilePath: ./backend/modules/services/cql-engine-service/Dockerfile
    dockerContext: ./backend
    envVars:
      - key: DATABASE_URL
        fromDatabase:
          name: healthdata-postgres
          property: connectionString
      - key: PORT
        value: 8081

databases:
  - name: healthdata-postgres
    databaseName: healthdata_cql
    user: healthdata
    plan: starter
```

3. **Deploy via Dashboard or CLI**:
```bash
# Via Dashboard
# New → Blueprint → Connect Repository

# Or via CLI
curl -X POST https://api.render.com/v1/services \
  -H "Authorization: Bearer $RENDER_API_KEY" \
  -d @render.yaml
```

4. **Custom Domain**:
```bash
# In Render Dashboard
# Service → Settings → Custom Domain
# Add: api.healthdata-in-motion.com

# Update DNS CNAME to Render's domain
```

---

## Option 3: AWS ECS + Fargate (⭐ Production-Grade, HIPAA-Compliant)

### Overview
AWS Elastic Container Service with Fargate provides enterprise-grade deployment with full HIPAA compliance, but requires more setup and expertise.

### Pros
✅ **HIPAA compliant** - Sign BAA with AWS
✅ **Highly scalable** - Auto-scaling, load balancing
✅ **Full control** - Configure everything
✅ **Integrated ecosystem** - RDS, ElastiCache, CloudWatch, etc.
✅ **Multi-region** - Deploy globally
✅ **Enterprise support** - 24/7 support available
✅ **Security** - VPC, IAM, KMS encryption

### Cons
❌ **Complex setup** - Steep learning curve
❌ **Higher cost** - $150-500/month minimum
❌ **More maintenance** - Requires DevOps expertise

### Pricing (Estimated)
- **ECS Fargate** (Quality Service): $40/month (0.5 vCPU, 1 GB RAM)
- **ECS Fargate** (CQL Service): $40/month
- **ECS Fargate** (FHIR Service): $40/month
- **RDS PostgreSQL** (db.t3.micro): $25/month
- **ElastiCache Redis** (cache.t3.micro): $15/month
- **Application Load Balancer**: $20/month
- **Data Transfer**: $10-50/month
- **CloudWatch Logs**: $5/month
- **Route 53 DNS**: $1/month
- **Total: ~$196-236/month**

### Setup Steps (High-Level)

1. **Prerequisites**:
```bash
# Install AWS CLI
brew install awscli  # macOS
# or: apt-get install awscli  # Linux

# Configure AWS credentials
aws configure
# Enter: Access Key ID, Secret Access Key, Region (us-east-1)
```

2. **Create VPC and Networking**:
```bash
# Use CloudFormation template or Terraform
# Create VPC with public and private subnets
# Set up NAT Gateway for private subnets
# Configure security groups
```

3. **Create RDS PostgreSQL**:
```bash
aws rds create-db-instance \
  --db-instance-identifier healthdata-postgres \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --allocated-storage 20 \
  --master-username healthdata \
  --master-user-password <strong-password> \
  --vpc-security-group-ids sg-xxxxxx \
  --db-subnet-group-name healthdata-subnet-group \
  --backup-retention-period 7 \
  --storage-encrypted \
  --enable-iam-database-authentication
```

4. **Build and Push Docker Images to ECR**:
```bash
# Create ECR repositories
aws ecr create-repository --repository-name quality-measure-service

# Build and tag images
docker build -t quality-measure-service:latest \
  -f backend/modules/services/quality-measure-service/Dockerfile \
  backend/

# Authenticate Docker to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Push image
docker tag quality-measure-service:latest \
  <account-id>.dkr.ecr.us-east-1.amazonaws.com/quality-measure-service:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/quality-measure-service:latest
```

5. **Create ECS Cluster**:
```bash
aws ecs create-cluster --cluster-name healthdata-cluster --capacity-providers FARGATE
```

6. **Create Task Definitions**:
```json
{
  "family": "quality-measure-service",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "quality-measure",
      "image": "<account-id>.dkr.ecr.us-east-1.amazonaws.com/quality-measure-service:latest",
      "portMappings": [
        {
          "containerPort": 8087,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "docker"
        }
      ],
      "secrets": [
        {
          "name": "DATABASE_URL",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:xxxx:secret:healthdata/database"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:xxxx:secret:healthdata/jwt"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/quality-measure-service",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

7. **Create ECS Service with Load Balancer**:
```bash
aws ecs create-service \
  --cluster healthdata-cluster \
  --service-name quality-measure-service \
  --task-definition quality-measure-service:1 \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:us-east-1:xxx:targetgroup/quality-measure/xxx,containerName=quality-measure,containerPort=8087"
```

8. **Configure Route 53 DNS**:
```bash
# Create hosted zone
aws route53 create-hosted-zone --name healthdata-in-motion.com

# Add A record pointing to load balancer
# api.healthdata-in-motion.com → ALB DNS name
```

---

## Option 4: DigitalOcean App Platform

### Overview
DigitalOcean's platform offering, middle ground between Railway/Render and AWS.

### Pros
✅ **Simple pricing** - Predictable costs
✅ **Docker support** - Native container deployment
✅ **Managed databases** - PostgreSQL and Redis
✅ **Good performance** - SSD-backed infrastructure
✅ **Global CDN** - Built-in
✅ **Free SSL** - Automatic certificates

### Cons
❌ **No HIPAA BAA** - Not certified
❌ **Fewer regions** - 8 datacenters vs AWS's 30+
❌ **Less mature** - Newer platform

### Pricing
- **Basic** ($5/month): 512 MB RAM, 1 vCPU
- **Professional** ($12/month): 1 GB RAM, 1 vCPU
- **Professional XL** ($24/month): 2 GB RAM, 2 vCPU
- **PostgreSQL** ($15/month): 1 GB RAM, 10 GB storage
- **Redis** ($15/month): 1 GB RAM

**Estimated Monthly Cost**:
- Quality Measure Service: $24
- CQL Engine Service: $24
- FHIR Server: $24
- PostgreSQL: $15
- Redis: $15
- **Total: ~$102/month**

---

## Comparison Matrix

| Feature | Railway | Render | AWS ECS | DigitalOcean |
|---------|---------|--------|---------|--------------|
| **Ease of Setup** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **Monthly Cost** | $50 | $107 | $200+ | $102 |
| **HIPAA Compliance** | ❌ | ❌ | ✅ | ❌ |
| **Auto-Scaling** | Limited | ✅ | ✅ | ✅ |
| **Custom Domains** | ✅ | ✅ | ✅ | ✅ |
| **Free SSL** | ✅ | ✅ | ✅ | ✅ |
| **Git Integration** | ✅ | ✅ | ❌ | ✅ |
| **Docker Support** | ✅ | ✅ | ✅ | ✅ |
| **Managed DB** | ✅ | ✅ | ✅ | ✅ |
| **Monitoring** | Basic | Good | Excellent | Good |
| **Support** | Community | Email | 24/7 (paid) | Ticket |
| **Best For** | Demos, MVPs | Startups | Enterprise | Small teams |

---

## Recommended Deployment Strategy

### Phase 1: MVP / Demo (Months 1-3)
**Platform**: Railway
- **Why**: Fastest setup, lowest cost, perfect for demos
- **Cost**: ~$50/month
- **Timeline**: Deploy in 1 day

### Phase 2: Beta / Early Customers (Months 4-6)
**Platform**: Render
- **Why**: Better performance, auto-scaling, production-ready
- **Cost**: ~$107/month
- **Timeline**: Migrate in 1-2 days

### Phase 3: Production / Healthcare Organizations (Months 7+)
**Platform**: AWS ECS + RDS
- **Why**: HIPAA compliant, enterprise-grade, scalable
- **Cost**: ~$200-500/month (scales with usage)
- **Timeline**: Plan 2-3 weeks for proper AWS setup

---

## Quick Start: Deploy to Railway (Recommended First Step)

### 1. Install Railway CLI
```bash
npm install -g @railway/cli
railway login
```

### 2. Create Project
```bash
# From project root
cd /home/webemo-aaron/projects/healthdata-in-motion
railway init
# Select: Empty Project
```

### 3. Add PostgreSQL
```bash
railway add postgresql
# Railway auto-provisions database
```

### 4. Deploy Quality Measure Service
```bash
cd backend/modules/services/quality-measure-service
railway up
# Railway detects Dockerfile and builds
```

### 5. Get Service URL
```bash
railway status
# Note the URL: quality-measure-service-production.up.railway.app
```

### 6. Set Environment Variables
```bash
# In Railway Dashboard → Variables
DATABASE_URL=${DATABASE_URL}  # Auto-set by Railway PostgreSQL
SPRING_PROFILES_ACTIVE=docker
JWT_SECRET=$(openssl rand -base64 32)
REDIS_URL=${REDIS_URL}  # If Redis added
```

### 7. Test Deployment
```bash
curl https://quality-measure-service-production.up.railway.app/actuator/health
# Should return: {"status":"UP"}
```

### 8. Repeat for Other Services
```bash
# CQL Engine Service
cd ../../cql-engine-service
railway up

# FHIR Server (if deploying custom FHIR server)
# Or use HAPI FHIR cloud instance
```

### 9. Update Frontend Environment Variables
```bash
# In Vercel Dashboard → Environment Variables
API_GATEWAY_URL=https://quality-measure-service-production.up.railway.app
QUALITY_MEASURE_URL=https://quality-measure-service-production.up.railway.app
CQL_ENGINE_URL=https://cql-engine-service-production.up.railway.app
```

### 10. Redeploy Frontend
```bash
vercel --prod
```

---

## Monitoring & Maintenance

### Railway Monitoring
```bash
# View logs
railway logs

# View metrics
railway status

# Shell into container
railway shell
```

### AWS CloudWatch (if using AWS)
```bash
# View logs
aws logs tail /ecs/quality-measure-service --follow

# View metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/ECS \
  --metric-name CPUUtilization \
  --dimensions Name=ServiceName,Value=quality-measure-service \
  --statistics Average \
  --start-time 2025-11-20T00:00:00Z \
  --end-time 2025-11-20T23:59:59Z \
  --period 3600
```

---

## Security Best Practices

### Environment Variables
```bash
# NEVER commit these to git:
DATABASE_URL=postgresql://...
JWT_SECRET=...
REDIS_URL=redis://...
API_KEYS=...

# Use platform secrets management:
# - Railway: Built-in variables
# - Render: Environment variables
# - AWS: AWS Secrets Manager
```

### Database Security
```bash
# Use strong passwords
DB_PASSWORD=$(openssl rand -base64 32)

# Enable SSL connections
DATABASE_URL=postgresql://...?sslmode=require

# Restrict access to private network only
# - Railway: Automatic
# - Render: Private networking
# - AWS: VPC with security groups
```

### API Security
```bash
# Enable CORS only for your domain
CORS_ALLOWED_ORIGINS=https://clinical.healthdata-in-motion.com

# Use HTTPS everywhere
# - Railway: Automatic
# - Render: Automatic
# - AWS: ALB with ACM certificate

# Rotate JWT secrets regularly
JWT_SECRET=<rotate quarterly>
```

---

## Disaster Recovery

### Database Backups

**Railway**:
- Automatic daily backups (retained 7 days)
- Manual snapshots: Dashboard → Database → Create Backup

**Render**:
- Automatic daily backups (Standard plan: 7 days, Pro: 30 days)
- Point-in-time recovery available

**AWS RDS**:
- Automatic backups: Configure retention (1-35 days)
- Manual snapshots: Retained until deleted
- Cross-region replication for DR

### Application Redundancy

**Minimum**:
- 2 instances per service (different availability zones)
- Load balancer for traffic distribution
- Health checks with automatic restart

**Recommended**:
- 3+ instances across multiple regions
- Database read replicas
- Redis cluster for high availability
- CDN for static assets (Cloudflare, AWS CloudFront)

---

## Cost Optimization Tips

### 1. Right-Size Instances
```bash
# Start small, scale up based on metrics
# Don't over-provision

# Railway: Start with $5 plan
# Render: Start with Starter ($7)
# AWS: Start with t3.micro (0.5 vCPU, 1 GB RAM)
```

### 2. Use Spot/Preemptible Instances (AWS)
```bash
# Save up to 70% with spot instances
# Good for non-critical workloads
```

### 3. Auto-Scaling Policies
```bash
# Scale down during off-hours
# Example: 1 instance at night, 3 instances during business hours
```

### 4. Cache Aggressively
```bash
# Use Redis for:
# - Quality measure results (2-minute TTL)
# - FHIR resource queries
# - Patient health overviews

# Reduces database load and cost
```

### 5. Optimize Docker Images
```bash
# Use multi-stage builds (already done)
# Minimize layers
# Use alpine base images (smaller = cheaper transfer costs)
```

---

## Next Steps

### This Week
1. [ ] Choose deployment platform (Railway recommended for quick start)
2. [ ] Create account and set up billing
3. [ ] Deploy PostgreSQL database
4. [ ] Deploy Quality Measure Service
5. [ ] Test health endpoint

### Next Week
1. [ ] Deploy remaining backend services
2. [ ] Configure custom domain
3. [ ] Set up monitoring
4. [ ] Load testing
5. [ ] Update frontend to use production backend

### Month 1
1. [ ] User acceptance testing
2. [ ] Performance optimization
3. [ ] Security audit
4. [ ] Backup/DR testing
5. [ ] Production launch

---

**Document Version**: 1.0
**Last Updated**: November 20, 2025
**Next Review**: After first deployment

---

*Choose the platform that matches your current needs and budget. You can always migrate later as you scale.*

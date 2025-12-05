# RHEL 7 Deployment Guide

**HealthData In Motion HIE Platform**

This guide provides step-by-step instructions for deploying the complete HealthData In Motion platform on Red Hat Enterprise Linux 7, including Kong API Gateway and all microservices.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start (Automated)](#quick-start-automated)
3. [Manual Deployment](#manual-deployment)
4. [Testing in Docker Container](#testing-in-docker-container)
5. [Production Configuration](#production-configuration)
6. [Troubleshooting](#troubleshooting)
7. [Maintenance](#maintenance)

---

## Prerequisites

### System Requirements

| Component | Requirement |
|-----------|-------------|
| OS | RHEL 7.x / CentOS 7.x |
| CPU | 4+ cores recommended |
| RAM | 8GB minimum, 16GB recommended |
| Disk | 50GB free space |
| Network | Ports 4200, 8000, 8001, 8081, 8087, 8083, 5432 available |

### Software Requirements

```bash
# Java 21 (or Java 11 minimum)
sudo yum install java-21-openjdk java-21-openjdk-devel

# Node.js 20.x
curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -
sudo yum install -y nodejs

# Docker CE
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y docker-ce docker-ce-cli containerd.io

# Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# PostgreSQL client (for admin tasks)
sudo yum install -y postgresql

# Git
sudo yum install -y git
```

### Enable and Start Docker

```bash
# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Add current user to docker group (logout/login required)
sudo usermod -aG docker $USER

# Verify Docker installation
docker --version
docker-compose --version
```

---

## Quick Start (Automated)

The fastest way to deploy the complete platform:

### 1. Clone Repository

```bash
cd /opt
sudo git clone https://github.com/your-org/healthdata-in-motion.git
cd healthdata-in-motion
sudo chown -R $USER:$USER .
```

### 2. Run Automated Deployment Script

```bash
chmod +x deploy-rhel7.sh
./deploy-rhel7.sh
```

The script will:
- ✅ Run pre-flight checks (Docker, Java, Node.js)
- ✅ Build Java backend services
- ✅ Build Angular frontend
- ✅ Start infrastructure (PostgreSQL, Redis, Kafka)
- ✅ Start backend microservices
- ✅ Deploy Kong API Gateway
- ✅ Configure Kong routes and security
- ✅ Start Angular Clinical Portal
- ✅ Verify all services are running

**Expected Time**: 10-15 minutes (first run, includes dependency downloads)

### 3. Access the Platform

Once deployment completes:

```bash
# Access Points
Clinical Portal:     http://localhost:4200
Kong API Gateway:    http://localhost:8000
Kong Admin API:      http://localhost:8001
Konga Admin UI:      http://localhost:1337
```

Open your browser to **http://localhost:4200** to see the UI.

---

## Manual Deployment

For step-by-step manual deployment:

### Step 1: Prepare Environment

```bash
# Create project directory
sudo mkdir -p /opt/healthdata-in-motion
cd /opt/healthdata-in-motion

# Clone repository
git clone https://github.com/your-org/healthdata-in-motion.git .

# Create necessary directories
mkdir -p logs data/postgres data/redis data/kafka kong/certs

# Set permissions
chmod +x kong/kong-setup.sh
chmod +x deploy-rhel7.sh
```

### Step 2: Install Node.js Dependencies

```bash
npm install
```

This installs all required packages for the Angular frontend and build tools.

### Step 3: Build Backend Services

```bash
cd backend

# Build all Java services
./gradlew clean build -x test

# Or build individual services
./gradlew :cql-engine-service:build
./gradlew :quality-measure-service:build
./gradlew :fhir-service:build

cd ..
```

### Step 4: Build Frontend

```bash
# Production build
npx nx build clinical-portal --configuration production

# Development build (faster)
npx nx build clinical-portal
```

Build output: `dist/apps/clinical-portal/`

### Step 5: Start Infrastructure Services

```bash
# Start PostgreSQL, Redis, Kafka, Zookeeper
docker-compose up -d postgres redis zookeeper kafka

# Wait for services to be ready
sleep 30

# Verify PostgreSQL
docker-compose exec postgres pg_isready -U healthdata
```

### Step 6: Start Backend Microservices

```bash
# Start CQL Engine, Quality Measure, FHIR services
docker-compose up -d cql-engine quality-measure fhir-mock

# Wait for services to start
sleep 30

# Check health
curl http://localhost:8081/cql-engine/actuator/health
curl http://localhost:8087/quality-measure/actuator/health
curl http://localhost:8083/fhir/metadata
```

### Step 7: Deploy Kong API Gateway

```bash
# Start Kong and database
docker-compose -f kong/docker-compose-kong.yml up -d

# Wait for Kong to be ready
sleep 45

# Verify Kong
curl http://localhost:8001/

# Configure Kong routes
./kong/kong-setup.sh
```

### Step 8: Start Angular Frontend

#### Option A: Serve Production Build

```bash
# Install http-server globally
npm install -g http-server

# Serve production build
http-server dist/apps/clinical-portal -p 4200 --proxy http://localhost:8000
```

#### Option B: Development Mode

```bash
npx nx serve clinical-portal --host 0.0.0.0 --port 4200
```

### Step 9: Verify Deployment

```bash
# Test Kong routes
curl -H "X-Tenant-ID: default" \
  "http://localhost:8000/api/cql/api/v1/cql/evaluations?page=0&size=5"

curl -H "X-Tenant-ID: default" \
  "http://localhost:8000/api/quality/quality-measure/results?page=0&size=5"

curl "http://localhost:8000/api/fhir/Patient?_count=5"

# Test Angular UI
curl http://localhost:4200
```

---

## Testing in Docker Container

To test the deployment locally in a RHEL 7-like environment:

### Option 1: Using Pre-built Dockerfile

```bash
# Build RHEL 7 test container
docker build -f Dockerfile.rhel7 -t healthdata-rhel7 .

# Run container with port mappings
docker run -d \
  --name healthdata-platform \
  --privileged \
  -p 4200:4200 \
  -p 8000:8000 \
  -p 8001:8001 \
  -p 8081:8081 \
  -p 8087:8087 \
  -p 8083:8083 \
  healthdata-rhel7

# Monitor startup
docker logs -f healthdata-platform

# Access UI
# Wait 2-3 minutes for all services to start, then:
open http://localhost:4200
```

### Option 2: Interactive Testing

```bash
# Start CentOS 7 container
docker run -it --privileged \
  -p 4200:4200 -p 8000:8000 -p 8001:8001 \
  -v $(pwd):/opt/healthdata \
  centos:7 /bin/bash

# Inside container, install prerequisites
yum update -y
yum install -y wget curl git unzip

# Install Docker inside container (Docker-in-Docker)
curl -fsSL https://get.docker.com | sh
systemctl start docker

# Install Java, Node.js
yum install -y java-11-openjdk java-11-openjdk-devel
curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
yum install -y nodejs

# Deploy platform
cd /opt/healthdata
./deploy-rhel7.sh
```

---

## Production Configuration

### 1. Firewall Configuration

```bash
# Allow required ports
sudo firewall-cmd --permanent --add-port=4200/tcp  # Angular
sudo firewall-cmd --permanent --add-port=8000/tcp  # Kong HTTP
sudo firewall-cmd --permanent --add-port=8443/tcp  # Kong HTTPS
sudo firewall-cmd --permanent --add-port=8001/tcp  # Kong Admin (restrict!)
sudo firewall-cmd --reload

# Or disable firewall (development only)
sudo systemctl stop firewalld
sudo systemctl disable firewalld
```

### 2. SELinux Configuration

```bash
# Check SELinux status
getenforce

# Option 1: Disable (not recommended for production)
sudo setenforce 0
sudo sed -i 's/SELINUX=enforcing/SELINUX=permissive/g' /etc/selinux/config

# Option 2: Configure SELinux policies (recommended)
# Allow Docker to bind to ports
sudo setsebool -P docker_transition_unconfined 1
```

### 3. System Limits

```bash
# Increase file descriptors for Kafka, PostgreSQL
sudo tee -a /etc/security/limits.conf > /dev/null <<EOF
*   soft    nofile  65536
*   hard    nofile  65536
EOF

# Reload limits
sudo sysctl -p
```

### 4. SSL/TLS Configuration

```bash
# Generate self-signed certificate (testing)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout kong/certs/kong.key \
  -out kong/certs/kong.crt \
  -subj "/CN=healthdata.example.com"

# Add certificate to Kong
curl -X POST http://localhost:8001/certificates \
  -F "cert=@kong/certs/kong.crt" \
  -F "key=@kong/certs/kong.key" \
  -F "snis=healthdata.example.com"

# Test HTTPS endpoint
curl -k https://localhost:8443/api/cql/evaluations
```

### 5. OIDC Authentication

```bash
# Configure environment variables
export OIDC_ISSUER="https://your-idp.example.com/realms/healthdata"
export OIDC_CLIENT_ID="healthdata-api-gateway"
export OIDC_CLIENT_SECRET="your-client-secret-here"
export OIDC_DISCOVERY="https://your-idp.example.com/realms/healthdata/.well-known/openid-configuration"

# Run OIDC setup script
./kong/kong-oidc-setup.sh
```

### 6. Systemd Service (Auto-start on Boot)

Create systemd service for Angular:

```bash
sudo tee /etc/systemd/system/healthdata-angular.service > /dev/null <<EOF
[Unit]
Description=HealthData In Motion Angular Clinical Portal
After=docker.service
Requires=docker.service

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/healthdata-in-motion
ExecStart=/usr/bin/npx nx serve clinical-portal --host 0.0.0.0 --port 4200
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable healthdata-angular
sudo systemctl start healthdata-angular
```

### 7. Environment Variables

Create `.env` file for environment-specific configuration:

```bash
# Create .env file
cat > .env <<EOF
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=healthdata
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=change_me_in_production

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Kong
KONG_ADMIN_URL=http://localhost:8001
KONG_PROXY_URL=http://localhost:8000

# Application
DEFAULT_TENANT_ID=default
LOG_LEVEL=INFO
EOF

# Secure .env file
chmod 600 .env
```

---

## Troubleshooting

### Issue: Docker Daemon Not Starting

```bash
# Check Docker status
sudo systemctl status docker

# View Docker logs
sudo journalctl -u docker -f

# Restart Docker
sudo systemctl restart docker

# Check for port conflicts
sudo netstat -tulpn | grep -E '2375|2376'
```

### Issue: Kong Not Starting

```bash
# Check Kong logs
docker logs healthdata-kong

# Check Kong database
docker logs healthdata-kong-db

# Verify migration completed
docker logs healthdata-kong-migration

# Restart Kong
docker-compose -f kong/docker-compose-kong.yml restart kong
```

### Issue: Java Services Not Starting

```bash
# Check Java version
java -version

# Check service logs
docker logs healthdata-cql-engine
docker logs healthdata-quality-measure
docker logs healthdata-fhir-mock

# Check PostgreSQL connectivity
docker-compose exec cql-engine \
  curl -v telnet://postgres:5432

# Rebuild services
cd backend
./gradlew clean build
cd ..
docker-compose restart cql-engine quality-measure fhir-mock
```

### Issue: Angular Not Building

```bash
# Check Node.js version
node --version  # Should be 20.x

# Clear node_modules and rebuild
rm -rf node_modules package-lock.json
npm install

# Check disk space
df -h

# Build with verbose output
npx nx build clinical-portal --verbose
```

### Issue: Ports Already in Use

```bash
# Check what's using the port
sudo netstat -tulpn | grep :4200
sudo netstat -tulpn | grep :8000

# Kill process using port
sudo kill -9 $(sudo lsof -t -i:4200)

# Or change port in configuration
# Edit: apps/clinical-portal/project.json
# Change: "port": 4200 to "port": 4201
```

### Issue: Cannot Access UI from External Network

```bash
# Check firewall
sudo firewall-cmd --list-all

# Allow port through firewall
sudo firewall-cmd --permanent --add-port=4200/tcp
sudo firewall-cmd --reload

# Verify Angular is listening on 0.0.0.0 (not 127.0.0.1)
sudo netstat -tulpn | grep :4200

# If listening on localhost only, restart with:
npx nx serve clinical-portal --host 0.0.0.0 --port 4200
```

---

## Maintenance

### Start All Services

```bash
# Start infrastructure and backend
docker-compose up -d

# Start Kong
docker-compose -f kong/docker-compose-kong.yml up -d

# Start Angular
npx nx serve clinical-portal --host 0.0.0.0 --port 4200 &
```

### Stop All Services

```bash
# Stop Angular (if running in background)
kill $(cat logs/angular.pid)

# Stop all Docker services
docker-compose down
docker-compose -f kong/docker-compose-kong.yml down
```

### Restart Specific Service

```bash
# Restart CQL Engine
docker-compose restart cql-engine

# Restart Kong
docker-compose -f kong/docker-compose-kong.yml restart kong

# Rebuild and restart
docker-compose up -d --build cql-engine
```

### View Logs

```bash
# View all logs
docker-compose logs -f

# View specific service
docker-compose logs -f cql-engine
docker logs -f healthdata-kong

# View Angular logs
tail -f logs/angular.log
```

### Update Configuration

```bash
# Update Kong routes
./kong/kong-setup.sh

# Reload Angular (hot reload automatic in dev mode)
# For production build, rebuild:
npx nx build clinical-portal --configuration production
```

### Backup Database

```bash
# Backup PostgreSQL
docker-compose exec postgres pg_dump -U healthdata healthdata > backup_$(date +%Y%m%d).sql

# Restore PostgreSQL
docker-compose exec -T postgres psql -U healthdata healthdata < backup_20250119.sql
```

### Upgrade Platform

```bash
# Pull latest code
git pull origin main

# Rebuild backend
cd backend
./gradlew clean build
cd ..

# Rebuild frontend
npm install
npx nx build clinical-portal --configuration production

# Restart services
docker-compose restart
docker-compose -f kong/docker-compose-kong.yml restart
```

---

## Performance Tuning

### JVM Options for Java Services

Edit `docker-compose.yml`:

```yaml
cql-engine:
  environment:
    JAVA_OPTS: >-
      -Xms2g
      -Xmx4g
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
```

### PostgreSQL Tuning

```bash
# Edit PostgreSQL configuration
docker-compose exec postgres psql -U healthdata -c "
  ALTER SYSTEM SET shared_buffers = '2GB';
  ALTER SYSTEM SET effective_cache_size = '6GB';
  ALTER SYSTEM SET max_connections = 200;
"

# Restart PostgreSQL
docker-compose restart postgres
```

### Kong Performance

```bash
# Increase worker processes
curl -X PATCH http://localhost:8001/configuration \
  -d "nginx_worker_processes=4"

# Enable caching
curl -X POST http://localhost:8001/plugins \
  -d "name=proxy-cache" \
  -d "config.strategy=memory" \
  -d "config.cache_ttl=300"
```

---

## Production Checklist

Before going to production:

- [ ] Change all default passwords
- [ ] Configure SSL/TLS certificates
- [ ] Enable OIDC authentication
- [ ] Set up centralized logging (ELK stack)
- [ ] Configure monitoring (Prometheus + Grafana)
- [ ] Set up automated backups
- [ ] Configure firewall rules
- [ ] Enable SELinux with proper policies
- [ ] Set up log rotation
- [ ] Configure system limits
- [ ] Enable auto-start on boot
- [ ] Document disaster recovery procedures
- [ ] Set up staging environment
- [ ] Perform load testing
- [ ] Configure IP restrictions for Kong Admin API

---

## Support

### Documentation

- [Main README](README.md)
- [Kong Integration Guide](KONG_INTEGRATION_COMPLETE.md)
- [Kong Quick Start](kong/QUICKSTART.md)
- [Backend Documentation](backend/README.md)
- [HIE Deployment Readiness](HIE_DEPLOYMENT_READINESS.md)

### Getting Help

- **Kong Issues**: https://discuss.konghq.com/
- **Docker Issues**: https://forums.docker.com/
- **RHEL Issues**: https://access.redhat.com/support

---

## Appendix: Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│  RHEL 7 Host System                                          │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Docker Containers                                     │ │
│  │                                                        │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐            │ │
│  │  │PostgreSQL│  │  Redis   │  │  Kafka   │            │ │
│  │  └────┬─────┘  └──────────┘  └──────────┘            │ │
│  │       │                                               │ │
│  │  ┌────┴───────────────────────────────┐              │ │
│  │  │                                    │              │ │
│  │  │  ┌──────────┐  ┌──────────┐  ┌────┴─────┐        │ │
│  │  │  │CQL Engine│  │ Quality  │  │   FHIR   │        │ │
│  │  │  │ :8081    │  │ Measure  │  │  :8083   │        │ │
│  │  │  │          │  │ :8087    │  │          │        │ │
│  │  │  └──────────┘  └──────────┘  └──────────┘        │ │
│  │  │                                                   │ │
│  │  │              ▲  ▲  ▲                              │ │
│  │  └──────────────┼──┼──┼──────────────────────────────┘ │
│  │                 │  │  │                                 │
│  │  ┌──────────────┴──┴──┴─────────────────────────────┐  │
│  │  │  Kong API Gateway (:8000, :8443)                 │  │
│  │  │  - CORS, Rate Limiting, Security                 │  │
│  │  │  - Routes: /api/cql, /api/quality, /api/fhir     │  │
│  │  └──────────────────┬───────────────────────────────┘  │
│  └─────────────────────┼──────────────────────────────────┘
│                        │                                    │
│  ┌─────────────────────┴──────────────────────────────────┐ │
│  │  Angular Clinical Portal (Node.js :4200)              │ │
│  │  - Dashboard, Patients, Evaluations, Reports          │ │
│  └───────────────────────────────────────────────────────┘ │
│                        │                                    │
└────────────────────────┼────────────────────────────────────┘
                         │
                         ▼
                    User Browser
              http://localhost:4200
```

---

**Document Version**: 1.0
**Last Updated**: November 19, 2025
**Platform Version**: 1.0.0

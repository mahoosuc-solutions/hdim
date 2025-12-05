# HealthData In Motion - Deployment Guide

**Complete HIE Platform with Kong API Gateway**

---

## 🚀 Quick Start

### Current Deployment (Already Running)

The platform is **already running** with Kong API Gateway integrated!

```bash
# Access the UI now:
http://localhost:4200

# Verify everything is working:
./verify-deployment.sh
```

### Deploy on RHEL 7

```bash
# One command deployment:
./deploy-rhel7.sh
```

That's it! The script handles everything.

---

## 📚 Documentation

### Start Here

1. **[QUICK_START.md](QUICK_START.md)** - Get started in 5 minutes
2. **[DEPLOYMENT_COMPLETE.md](DEPLOYMENT_COMPLETE.md)** - Complete deployment summary
3. **[verify-deployment.sh](verify-deployment.sh)** - Health check script (run this first!)

### RHEL 7 Deployment

1. **[RHEL7_DEPLOYMENT_SUMMARY.md](RHEL7_DEPLOYMENT_SUMMARY.md)** - Executive overview
2. **[RHEL7_DEPLOYMENT_GUIDE.md](RHEL7_DEPLOYMENT_GUIDE.md)** - Step-by-step guide (20+ pages)
3. **[deploy-rhel7.sh](deploy-rhel7.sh)** - Automated deployment script

### Kong API Gateway

1. **[KONG_INTEGRATION_COMPLETE.md](KONG_INTEGRATION_COMPLETE.md)** - Complete integration details
2. **[kong/QUICKSTART.md](kong/QUICKSTART.md)** - Kong in 10 minutes
3. **[kong/README.md](kong/README.md)** - Kong configuration reference

### Backend & Frontend

1. **[backend/README.md](backend/README.md)** - Backend microservices
2. **[FRONTEND_IMPLEMENTATION_SUMMARY.md](FRONTEND_IMPLEMENTATION_SUMMARY.md)** - Angular UI features
3. **[HIE_DEPLOYMENT_READINESS.md](HIE_DEPLOYMENT_READINESS.md)** - Production checklist

---

## 🎯 Current Status

✅ **All Systems Operational**

From `./verify-deployment.sh`:
- 8/8 Docker containers healthy
- Backend services (CQL, Quality, FHIR) responding
- Kong API Gateway routing traffic
- Angular UI accessible
- Sample data loaded

### Access Points

| Service | URL |
|---------|-----|
| **Clinical Portal** | http://localhost:4200 |
| **Kong Gateway** | http://localhost:8000 |
| **Kong Admin** | http://localhost:8001 |

---

## 🏗️ Architecture

```
User Browser → Angular (:4200) → Kong (:8000) → Backend Services
                                                   ├─ CQL Engine (:8081)
                                                   ├─ Quality Measure (:8087)
                                                   └─ FHIR Server (:8083)
                                                        ↓
                                                   PostgreSQL, Redis, Kafka
```

All API calls from Angular go through Kong for security, rate limiting, and routing.

---

## 🔧 Common Commands

### Check Status
```bash
./verify-deployment.sh
```

### View Logs
```bash
docker-compose logs -f
docker logs -f healthdata-kong
```

### Restart Services
```bash
docker-compose restart
docker-compose -f kong/docker-compose-kong.yml restart
```

### Stop Everything
```bash
docker-compose down
docker-compose -f kong/docker-compose-kong.yml down
```

---

## 🧪 Testing RHEL 7 Locally

### Option 1: Current System (Fastest)
```bash
./verify-deployment.sh
open http://localhost:4200
```

### Option 2: RHEL 7 Docker Container
```bash
docker build -f Dockerfile.rhel7 -t healthdata-rhel7 .
docker run -d --privileged -p 4200:4200 -p 8000:8000 healthdata-rhel7
docker logs -f healthdata-rhel7
# Wait 2-3 minutes, then: open http://localhost:4200
```

---

## 📋 Files Created

### Scripts
- `deploy-rhel7.sh` - Automated RHEL 7 deployment
- `verify-deployment.sh` - Health check and verification
- `docker/rhel7-startup.sh` - Container startup script

### Documentation
- `DEPLOYMENT_COMPLETE.md` - Complete deployment summary
- `RHEL7_DEPLOYMENT_GUIDE.md` - Comprehensive guide
- `RHEL7_DEPLOYMENT_SUMMARY.md` - Executive summary
- `KONG_INTEGRATION_COMPLETE.md` - Kong details
- `QUICK_START.md` - Quick reference

### Docker
- `Dockerfile.rhel7` - RHEL 7 test container
- `docker-compose.yml` - Service orchestration
- `kong/docker-compose-kong.yml` - Kong infrastructure

---

## ✅ Production Ready

The platform includes:
- ✅ Kong API Gateway with security plugins
- ✅ CORS, rate limiting, security headers
- ✅ Multi-tenant support (X-Tenant-ID)
- ✅ All services containerized
- ✅ Automated deployment scripts
- ✅ Comprehensive documentation
- ✅ Health check verification
- ✅ RHEL 7 test environment

---

## 🎉 Next Steps

1. **Test the UI**: http://localhost:4200
2. **Review docs**: [DEPLOYMENT_COMPLETE.md](DEPLOYMENT_COMPLETE.md)
3. **Deploy to RHEL 7**: `./deploy-rhel7.sh`
4. **Configure production**: SSL, OIDC, monitoring

---

**Questions? Check the docs above or run `./verify-deployment.sh` to see current status.**

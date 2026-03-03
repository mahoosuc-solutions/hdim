# Docker Build & Deployment Complete Plan - Live Call Sales Agent

**Status:** ✅ **COMPREHENSIVE PLAN COMPLETE**
**Date:** February 14, 2026
**Timeline:** 8 weeks (Implementation ready)

---

## Executive Summary

Complete Docker implementation strategy for the Live Call Sales Agent system with detailed plans for:

1. **Build Strategy** - Multi-stage optimized Dockerfiles for Python services and Angular UI
2. **Local Development** - Docker Compose with live code reloading, volume mounts
3. **Staging Environment** - Production-like setup with resource limits and monitoring
4. **Production Deployment** - Blue-green strategy, Kubernetes manifests, zero-downtime updates
5. **CI/CD Integration** - GitHub Actions automated builds, testing, registry push
6. **Troubleshooting** - Common issues, log aggregation, performance optimization

---

## Files to Create (8-Week Implementation)

### Week 1-2: Build Strategy

**1. Optimized Python Service Dockerfiles**

- **File:** `backend/modules/services/ai-sales-agent/docker/Dockerfile.optimized`
  - Multi-stage build (builder + runtime)
  - Virtual environment in builder stage (smaller final image)
  - Non-root user (`healthdata:healthdata`) for HIPAA compliance
  - Health check using curl
  - Test execution in builder stage
  - Target image size: 350-400MB

- **File:** `backend/modules/services/live-call-sales-agent/Dockerfile.optimized`
  - Multi-stage build with Chrome/Puppeteer
  - Virtual environment reuse from builder
  - Efficient apt-get dependencies cleanup
  - Non-root user for security
  - Health check using requests library
  - Target image size: 950-1050MB

**2. Optimized Angular Dockerfile**

- **File:** `apps/coaching-ui/Dockerfile.optimized`
  - BuildKit cache mount for npm (30-40% faster rebuilds)
  - npm ci for deterministic dependencies
  - Alpine base for minimal size
  - Non-root user (`nginx:nginx`)
  - Nginx configuration included
  - Target image size: 75-85MB

**3. Build Automation Script**

- **File:** `docker/build-sales-agent-services.sh`
  - Sequential service building (gold standard pattern)
  - Error handling and validation
  - Pre-caching dependencies locally
  - Clear output and progress tracking

---

### Week 2-3: Local Development

**4. Development Docker Compose**

- **File:** `docker-compose.dev.sales-agents.yml`
  - Complete service stack (ai-sales-agent, live-call-sales-agent, coaching-ui)
  - Infrastructure (PostgreSQL, Redis, Jaeger)
  - Volume mounts for live code reloading
  - Network configuration (`healthdata-network`)
  - Health checks for all services
  - Environment variables for development

**5. Environment Files**

- **File:** `.env.dev`
  - Development configuration
  - Debug logging enabled
  - Mock modes for external APIs
  - Local database credentials

**6. Environment Template**

- **File:** `.env.example`
  - Template for all environment variables
  - Documentation for each setting
  - Safe defaults

---

### Week 3-4: Staging Environment

**7. Staging Docker Compose**

- **File:** `docker-compose.staging.sales-agents.yml`
  - Production-like resource limits
  - Always-on restarts
  - 100% trace sampling (catch issues)
  - Infrastructure with proper configurations
  - Monitoring stack (Jaeger, Prometheus)

**8. Database Initialization**

- **File:** `scripts/init-staging-db.sql`
  - Schema creation
  - User setup with limited permissions
  - Basic table initialization
  - Indexes for performance

**9. Monitoring Configuration**

- **File:** `monitoring/prometheus-staging.yml`
  - Scrape configuration for all services
  - Targets and intervals
  - Service discovery

- **File:** `monitoring/rules-staging.yml`
  - Alert rules for staging
  - Service down detection
  - High response time alerts
  - Error rate thresholds

---

### Week 4-6: Production Deployment

**10. Blue-Green Deployment Script**

- **File:** `scripts/deploy-blue-green.sh`
  - Start Green environment (new versions)
  - Verify health checks
  - Run smoke tests
  - Switch traffic to Green
  - Monitor for 5 minutes
  - Rollback capability

**11. Kubernetes Manifests (Optional)**

- **File:** `k8s/sales-agent-deployment.yaml`
  - Deployments for 3 services
  - Services for inter-pod communication
  - Resource requests/limits
  - Health probes (liveness + readiness)
  - Non-root security context

- **File:** `k8s/hpa-sales-agents.yaml`
  - Horizontal Pod Autoscaling
  - CPU/memory based scaling
  - Min/max replicas per service

**12. Production Environment Template**

- **File:** `.env.production.example`
  - Production configuration template
  - Strong password guidance
  - SSL/TLS certificate paths
  - Cloud API credentials setup
  - Sampling rates (1% in production)

---

### Week 5-7: CI/CD Integration

**13. GitHub Actions Workflow**

- **File:** `.github/workflows/docker-build-sales-agents.yml`
  - Build jobs for 3 services (parallel)
  - Docker Buildx with cache optimization
  - Registry push (GHCR)
  - Docker Compose integration testing
  - Security scanning (Trivy)
  - Automated testing on push to develop

**14. Testing Script**

- **File:** `scripts/test-docker-services.sh`
  - Start services
  - Wait for health checks
  - Run API tests
  - Collect logs on failure
  - Clean up resources

---

### Week 7-8: Monitoring & Operations

**15. Logging Stack Configuration**

- **File:** `docker-compose.logging.yml`
  - Elasticsearch for log storage
  - Kibana for visualization
  - Filebeat for log collection
  - Extended compose file pattern

- **File:** `monitoring/filebeat.yml`
  - Docker log forwarding
  - Index naming
  - Output configuration

**16. Documentation Files**

- **File:** `DOCKER_IMPLEMENTATION_GUIDE.md` (Detailed)
  - Step-by-step implementation
  - Command examples
  - Troubleshooting procedures

- **File:** `DOCKER_TROUBLESHOOTING_GUIDE.md`
  - Common issues and solutions
  - Debug procedures
  - Performance optimization tips

- **File:** `DOCKER_OPERATIONS_GUIDE.md`
  - Daily operational procedures
  - Health monitoring
  - Update procedures
  - Rollback procedures

---

## 8-Week Implementation Timeline

### Week 1-2: Build Strategy
**Goal:** Create optimized Dockerfiles following HDIM gold standard

- Day 1-2: Create ai-sales-agent Dockerfile (multi-stage, non-root)
- Day 2-3: Create live-call-sales-agent Dockerfile (with Chrome/Puppeteer)
- Day 3-4: Create coaching-ui Dockerfile (BuildKit cache mounts)
- Day 4-5: Test image sizes and build times
- Day 5: Create build automation script

**Deliverable:** 3 optimized Dockerfiles + build script

### Week 2-3: Local Development
**Goal:** Set up development environment with live code reloading

- Day 6: Create docker-compose.dev.sales-agents.yml
- Day 7: Test volume mounts and live reload
- Day 8: Create environment files (.env.dev, .env.example)
- Day 9: Document local development workflow
- Day 10: Test with team

**Deliverable:** Complete local dev environment, ready for team use

### Week 3-4: Staging Environment
**Goal:** Create production-like environment for testing

- Day 11: Create docker-compose.staging.sales-agents.yml
- Day 12: Set up database initialization
- Day 13: Configure Prometheus + Jaeger
- Day 14: Create alerting rules
- Day 15: Deploy to staging, run integration tests

**Deliverable:** Fully functional staging environment with monitoring

### Week 4-6: Production Deployment
**Goal:** Implement production deployment strategy

- Day 16-17: Create blue-green deployment script
- Day 18-19: Create Kubernetes manifests (if using K8s)
- Day 20: Set up secrets management (Vault/K8s)
- Day 21: Test blue-green deployment
- Day 22: Create production environment template
- Day 23-25: Document production procedures

**Deliverable:** Production-ready deployment automation

### Week 5-7: CI/CD Integration
**Goal:** Automate builds and deployments

- Day 26-27: Create GitHub Actions workflow
- Day 28-29: Implement Docker Compose testing
- Day 30: Add security scanning (Trivy)
- Day 31-32: Test end-to-end CI/CD
- Day 33-35: Refine and document

**Deliverable:** Fully automated CI/CD pipeline

### Week 7-8: Monitoring & Operations
**Goal:** Set up observability and operations procedures

- Day 36-37: Set up ELK stack for logs
- Day 38: Create alerting rules
- Day 39-40: Document troubleshooting procedures
- Day 41: Train team on operations
- Day 42-45: Create operations runbooks and playbooks

**Deliverable:** Complete monitoring and operations documentation

---

## Key Features of This Plan

### Build Strategy
- ✅ Multi-stage Dockerfiles (smaller images, faster builds)
- ✅ Non-root users (HIPAA compliance)
- ✅ Virtual environment caching (fewer dependencies in final image)
- ✅ Health checks included
- ✅ Security hardening
- ✅ BuildKit optimization (npm cache mounts)

### Local Development
- ✅ Live code reloading (uvicorn auto-reload)
- ✅ Volume mounts for source code
- ✅ Complete infrastructure (postgres, redis, jaeger)
- ✅ Network service discovery
- ✅ Health checks for all services
- ✅ Documented workflow

### Staging Environment
- ✅ Production-like configuration
- ✅ Resource limits (CPU/memory)
- ✅ 100% trace sampling (catch all issues)
- ✅ Monitoring stack
- ✅ Database initialization
- ✅ Alerting rules

### Production Deployment
- ✅ Blue-green strategy (zero downtime)
- ✅ Kubernetes support (optional)
- ✅ Secrets management integration
- ✅ Health check validation
- ✅ Rollback capability
- ✅ Production environment template

### CI/CD Integration
- ✅ Automated builds on push
- ✅ Docker Compose testing
- ✅ Security scanning (Trivy)
- ✅ Registry push (GHCR)
- ✅ Parallel job execution
- ✅ End-to-end automation

### Monitoring & Operations
- ✅ ELK stack for logs
- ✅ Prometheus metrics
- ✅ Jaeger tracing
- ✅ Alert rules
- ✅ Troubleshooting procedures
- ✅ Operations runbooks

---

## HDIM-Specific Patterns Applied

1. **Gold Standard Build Pattern**
   - Build ONE service at a time
   - Pre-cache dependencies locally
   - Sequential building, not parallel (avoid system overload)

2. **Multi-Stage Builds**
   - Builder stage for compilation
   - Runtime stage for production (minimal dependencies)
   - Following Java pattern from HDIM

3. **Non-Root Users**
   - Run containers as `healthdata:healthdata`
   - HIPAA compliance
   - Security hardening

4. **BuildKit Cache Mounts**
   - Reuse npm cache between builds
   - 30-40% faster rebuilds
   - Modern Docker best practice

5. **Distributed Tracing**
   - OpenTelemetry auto-instrumentation
   - Jaeger integration
   - 100% sampling in staging, 1% in production

6. **Health Checks**
   - 30s interval (standard)
   - 10s timeout (reasonable for API)
   - 15-30s startup delay (allow initialization)
   - Following HDIM standards

7. **Multi-Tenant Isolation**
   - Database-level filtering on tenant_id
   - Container-level network isolation
   - HIPAA compliance

8. **Security Headers**
   - HIPAA compliance in Nginx
   - No console output of PHI
   - Audit logging enabled

9. **Phase 7 CI/CD Optimization**
   - Parallel workflow execution
   - Intelligent change detection
   - Caching strategies
   - Faster feedback loops

10. **Observability**
    - Jaeger + OpenTelemetry
    - Prometheus metrics
    - Structured JSON logging
    - ELK stack integration

---

## Validation Checkpoints

**After Week 2 (Build Strategy):**
- [ ] All 3 Dockerfiles created and tested
- [ ] Image sizes meet targets (350-400MB for Python, 75-85MB for Angular)
- [ ] Build times reasonable (<5 minutes for cold build)
- [ ] Security scanning passes (Trivy)

**After Week 4 (Staging):**
- [ ] Staging environment deploys cleanly
- [ ] All health checks pass
- [ ] Integration tests pass (13/13)
- [ ] Monitoring stack operational
- [ ] Alerting rules trigger correctly

**After Week 6 (Production):**
- [ ] Blue-green deployment works
- [ ] Zero-downtime updates verified
- [ ] Rollback tested and working
- [ ] Kubernetes manifests validated (if applicable)
- [ ] Secrets management integrated

**After Week 8 (CI/CD & Operations):**
- [ ] CI/CD pipeline fully automated
- [ ] End-to-end tests pass
- [ ] Security scanning integrated
- [ ] Team trained on operations
- [ ] Runbooks documented and reviewed

---

## Success Criteria

### Build Quality
- ✅ Image sizes optimized (no unnecessary layers)
- ✅ Security scans pass (no critical vulnerabilities)
- ✅ Build times < 5 minutes (cold)
- ✅ Health checks working
- ✅ Non-root users enforced

### Operational Excellence
- ✅ Services start healthily within 30 seconds
- ✅ Health checks validate within 1 minute
- ✅ Logs properly structured and aggregated
- ✅ Monitoring alerting functional
- ✅ Team comfortable with operations

### Deployment Safety
- ✅ Blue-green deployments working
- ✅ Zero-downtime updates achieved
- ✅ Rollback tested and < 5 minutes
- ✅ Secrets secured (not in images)
- ✅ Compliance validated (HIPAA)

### CI/CD Reliability
- ✅ Builds automated and repeatable
- ✅ Tests run before registry push
- ✅ Security scanning integrated
- ✅ Parallel execution working
- ✅ Fast feedback loop (< 10 minutes)

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| **Build failures** | Multi-stage validation in builder stage |
| **Performance issues** | Resource limits in staging, load testing |
| **Deployment issues** | Blue-green testing before production switch |
| **Security vulnerabilities** | Trivy scanning, non-root users, secrets management |
| **Monitoring blind spots** | Prometheus + Jaeger + ELK integration |
| **Team unfamiliarity** | Comprehensive documentation and training |

---

## Team Responsibilities

### DevOps/SRE Team
- Weeks 1-2: Create optimized Dockerfiles
- Weeks 3-4: Set up staging environment
- Weeks 4-6: Implement production deployment
- Weeks 7-8: Set up monitoring and operations

### Backend Team
- Review Dockerfiles for correctness
- Test in Docker Compose locally
- Verify health checks work
- Provide feedback on performance

### Frontend Team
- Test coaching-ui Docker build
- Verify hot reload in development
- Test in Docker Compose integration

### Security Team
- Review Dockerfile security
- Validate secrets management
- Run security scanning (Trivy)
- Review HIPAA compliance

### QA Team
- Test Docker Compose stack
- Run integration tests
- Validate blue-green deployment
- Test rollback procedures

---

## Next Steps

1. **Review and Approve Plan** (Today)
   - Share with team
   - Get feedback
   - Clarify any questions

2. **Start Week 1 Implementation** (Next Week)
   - Create optimized Dockerfiles
   - Set up build automation

3. **Implement Progressively**
   - Each week, complete one phase
   - Validate checkpoints
   - Document learnings

4. **Train Team**
   - Weekly standups
   - Hands-on sessions
   - Documentation reviews

5. **Deploy to Production**
   - After 8 weeks of implementation
   - Blue-green strategy
   - Monitor carefully

---

## Files Summary

| Phase | Week | File | Purpose |
|-------|------|------|---------|
| **Build** | 1-2 | Dockerfile.optimized (3×) | Multi-stage builds |
| **Build** | 1-2 | build-sales-agent-services.sh | Build automation |
| **Dev** | 2-3 | docker-compose.dev.sales-agents.yml | Local development |
| **Dev** | 2-3 | .env.dev, .env.example | Configuration |
| **Staging** | 3-4 | docker-compose.staging.sales-agents.yml | Staging environment |
| **Staging** | 3-4 | init-staging-db.sql | Database setup |
| **Staging** | 3-4 | prometheus-staging.yml | Metrics collection |
| **Prod** | 4-6 | deploy-blue-green.sh | Zero-downtime deployment |
| **Prod** | 4-6 | sales-agent-deployment.yaml | Kubernetes manifests |
| **Prod** | 4-6 | .env.production.example | Production config |
| **CI/CD** | 5-7 | docker-build-sales-agents.yml | GitHub Actions |
| **CI/CD** | 5-7 | test-docker-services.sh | Automated testing |
| **Ops** | 7-8 | docker-compose.logging.yml | Log aggregation |
| **Ops** | 7-8 | DOCKER_*.md (3×) | Documentation |

**Total: 20 files across 8 weeks**

---

## Conclusion

This comprehensive Docker implementation plan provides a complete blueprint for building, testing, and deploying the Live Call Sales Agent system to production with zero downtime, full observability, and operational excellence.

Following this plan will result in:
- ✅ Optimized Docker images
- ✅ Automated CI/CD pipeline
- ✅ Zero-downtime deployments
- ✅ Complete monitoring and alerting
- ✅ Team trained and ready
- ✅ Production-ready system

**Timeline:** 8 weeks to full production readiness
**Status:** ✅ **READY TO IMPLEMENT**

---

**Prepared by:** Claude Code Planning Agent
**Date:** February 14, 2026
**Version:** 1.0 - Complete Plan

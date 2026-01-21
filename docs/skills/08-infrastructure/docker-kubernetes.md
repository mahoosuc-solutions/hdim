# Docker & Infrastructure - Containerization & Orchestration Guide

> **This is a comprehensive guide for containerizing HDIM services and orchestrating them.**
> **Understanding Docker and orchestration is essential for local development and production deployment.**

---

## Overview

### What is This Skill?

Docker packages applications and their dependencies into containers—isolated, reproducible units. Docker Compose orchestrates multiple containers locally. Kubernetes (K8s) orchestrates at scale in production.

**Example:**
- Patient Service runs in container
- PostgreSQL runs in container
- Redis runs in container
- All networks configured; all ports exposed
- Single command: `docker compose up` starts everything

### Why is This Important for HDIM?

Healthcare systems demand reliability and reproducibility:

- **Local Development:** Developers can't test against production database; Docker provides safe, isolated environments
- **Consistency:** "Works on my machine" problems eliminated; all environments identical
- **Production Deployment:** Same container image deployed everywhere (dev, staging, prod)
- **Scalability:** Kubernetes enables horizontal scaling for high-traffic services

### Business Impact

- **Reduced Onboarding Time:** New developers set up environment in minutes (not days)
- **Fewer Production Incidents:** Configuration drift eliminated
- **Cost Efficiency:** Resource optimization; waste elimination
- **Disaster Recovery:** Container snapshots enable rapid recovery
- **Compliance:** Automated infrastructure ensures HIPAA compliance is enforced

### Key Services Using Docker

All 51 HDIM services containerized:
- Backend services (Java/Spring Boot) in Docker containers
- PostgreSQL database container
- Redis cache container
- Kafka message broker container
- Monitoring services (Prometheus, Grafana) in containers

### Estimated Learning Time

1-2 weeks (hands-on containerization, compose configuration, orchestration)

---

## Key Concepts

### Concept 1: Docker Images & Containers

**Image:** Template for creating containers (like a class definition)
```dockerfile
FROM openjdk:21-slim
COPY app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Container:** Running instance of image (like an object instance)
```bash
docker run -p 8080:8080 patient-service:1.0  # Creates container from image
```

### Concept 2: Dockerfile Build Context

**Dockerfile:** Step-by-step instructions for building image
```dockerfile
# Multi-stage build (optimized)
FROM openjdk:21 as builder
COPY . .
RUN ./gradlew build

FROM openjdk:21-slim
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Concept 3: Docker Compose for Local Development

**docker-compose.yml:** Define services, volumes, networks in single file
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_PASSWORD: password

  redis:
    image: redis:7
    ports:
      - "6379:6379"

  patient-service:
    build: ./backend/modules/services/patient-service
    ports:
      - "8084:8084"
    depends_on:
      - postgres
      - redis
```

### Concept 4: Networking & Service Discovery

Docker Compose creates network; services discover each other by name:
```
Service: patient-service
Host within compose: http://patient-service:8084

Service: postgres
Host within compose: postgres:5432
```

### Concept 5: Volumes & Persistence

**Bind Mounts:** Mount host directory into container
```yaml
volumes:
  - ./data:/data  # Host directory : Container directory
```

**Named Volumes:** Managed by Docker; persist across restarts
```yaml
volumes:
  postgres_data:
services:
  postgres:
    volumes:
      - postgres_data:/var/lib/postgresql/data
```

---

## Architecture Pattern

### HDIM Multi-Stage Deployment

```
┌────── Development ──────┐
│  docker-compose.yml    │
│  (local machine)       │
│  51 services running   │
│  Rapid iteration       │
└─────────────┬──────────┘
              │
              ▼
┌────── Staging ──────┐
│  Kubernetes cluster │
│  Canary deployment  │
│  Integration tests  │
└────────────┬────────┘
             │
             ▼
┌────── Production ──────┐
│  Kubernetes cluster    │
│  Multi-zone redundancy │
│  Auto-scaling enabled  │
│  24/7 monitoring       │
└────────────────────────┘
```

### Image Registry Architecture

```
┌──────────────────────┐
│  Docker Hub/Registry │
│  (Central artifact   │
│   repository)        │
└──────────┬───────────┘
           │
      ┌────┴─────┬────────┬──────────┐
      │           │        │          │
      ▼           ▼        ▼          ▼
   Dev PC      Staging   Prod East   Prod West
   (pull)      Cluster   Cluster     Cluster
```

---

## Implementation Guide

### Step 1: Create Dockerfile for Service

```dockerfile
# Multi-stage: Build stage
FROM openjdk:21 as builder

WORKDIR /build

# Copy Gradle build files
COPY build.gradle.kts .
COPY gradle ./gradle
COPY gradlew .

# Download dependencies
RUN ./gradlew downloadDependencies --no-daemon -x test

# Copy source code
COPY src ./src

# Build application
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM openjdk:21-slim

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /build/build/libs/*.jar app.jar

# Create non-root user for security
RUN useradd -m -u 1000 hdim && \
    chown -R hdim:hdim /app

USER hdim

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD java -cp app.jar sun.tools.jps.Jps || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 2: Configure Docker Compose

**docker-compose.yml for HDIM:**
```yaml
version: '3.8'

services:
  # PostgreSQL Database (multi-tenant support)
  postgres:
    image: postgres:16
    container_name: hdim-postgres
    environment:
      POSTGRES_USER: healthdata
      POSTGRES_PASSWORD: ${DB_PASSWORD}
      POSTGRES_MULTIPLE_DATABASES: "patient_db,fhir_db,quality_db,gap_db"
    ports:
      - "5435:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-databases.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U healthdata"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis Cache
  redis:
    image: redis:7
    container_name: hdim-redis
    ports:
      - "6380:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Kafka Message Broker
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: hdim-kafka
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_INTERNAL://kafka:29092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
    ports:
      - "9094:9092"
    depends_on:
      - zookeeper

  # Patient Service (Example)
  patient-service:
    build:
      context: ./backend
      dockerfile: ./modules/services/patient-service/Dockerfile
    container_name: hdim-patient-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/patient_db
      SPRING_DATASOURCE_USERNAME: healthdata
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      TENANT_ID: ${TENANT_ID}
    ports:
      - "8084:8084"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8084/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Gateway Service
  gateway-service:
    build:
      context: ./backend
      dockerfile: ./modules/services/gateway-service/Dockerfile
    container_name: hdim-gateway
    ports:
      - "8001:8001"
    depends_on:
      - patient-service
    environment:
      PATIENT_SERVICE_URL: http://patient-service:8084

volumes:
  postgres_data:

networks:
  default:
    name: hdim-network
```

### Step 3: Build Docker Image

```bash
# Build specific service
docker compose build patient-service

# Build all services
docker compose build

# Build with no cache (fresh)
docker compose build --no-cache patient-service
```

### Step 4: Start Containers

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f patient-service

# Stop all services
docker compose down

# Stop and remove volumes
docker compose down -v
```

### Step 5: Kubernetes Deployment (Production)

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: patient-service
  namespace: hdim
spec:
  replicas: 3
  selector:
    matchLabels:
      app: patient-service
  template:
    metadata:
      labels:
        app: patient-service
    spec:
      containers:
      - name: patient-service
        image: hdim/patient-service:1.0.0
        ports:
        - containerPort: 8084
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8084
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8084
          initialDelaySeconds: 10
          periodSeconds: 5
```

---

## Best Practices

- ✅ **DO use multi-stage Dockerfile**
  - Why: Reduces image size (builder stage discarded)
  - Example: First stage builds, second stage runs

- ✅ **DO use non-root user in container**
  - Why: Reduces security risk from container escape
  - Example: `RUN useradd -m hdim && ... USER hdim`

- ✅ **DO implement health checks**
  - Why: Docker/Kubernetes can restart failed containers
  - Example: `HEALTHCHECK --interval=30s ...`

- ✅ **DO use environment variables for configuration**
  - Why: Same image runs in dev/staging/prod with different config
  - Example: `SPRING_DATASOURCE_URL: ${DB_URL}`

- ✅ **DO keep images small**
  - Why: Faster downloads, faster container starts, less storage
  - Example: Use slim base image, remove build tools

- ❌ **DON'T run container as root**
  - Why: Security vulnerability; container escape could compromise host
  - Example: Don't skip `USER non-root-user`

- ❌ **DON'T put secrets in Dockerfile**
  - Why: Secrets embedded in image; visible in docker history
  - Example: Don't hardcode DB_PASSWORD in ENV

- ❌ **DON'T ignore resource limits**
  - Why: Container can consume all host resources
  - Example: Always set memory/CPU limits in K8s

---

## Common Commands

### Docker Commands

```bash
# Build image
docker build -t patient-service:1.0 .

# List images
docker image ls

# Run container
docker run -p 8080:8080 patient-service:1.0

# View logs
docker logs -f <container_id>

# Stop container
docker stop <container_id>

# Remove image
docker rmi patient-service:1.0

# Remove unused images
docker image prune -a
```

### Docker Compose Commands

```bash
# Start services
docker compose up -d

# Stop services
docker compose down

# View logs
docker compose logs -f patient-service

# Restart service
docker compose restart patient-service

# Execute command in container
docker compose exec patient-service bash

# View running containers
docker compose ps
```

### Kubernetes Commands

```bash
# Deploy
kubectl apply -f deployment.yaml

# View pods
kubectl get pods -n hdim

# View logs
kubectl logs -f deployment/patient-service -n hdim

# Scale deployment
kubectl scale deployment patient-service --replicas=5 -n hdim

# Update image
kubectl set image deployment/patient-service \
  patient-service=hdim/patient-service:1.1.0 -n hdim
```

---

## Troubleshooting

### Issue: "Failed to build image"

**Cause:** Gradle dependencies not cached locally; Docker network timeout

**Solution:** Pre-cache dependencies on host
```bash
cd backend
./gradlew downloadDependencies --no-daemon
docker compose build patient-service
```

### Issue: "Port already in use"

**Cause:** Service already running on same port

**Solution:** Stop existing service or change port
```bash
# Find what's using port 8084
lsof -i :8084

# Change compose port mapping
ports:
  - "8085:8084"  # Host:Container
```

### Issue: "Container exits immediately"

**Cause:** Application crashed on startup

**Solution:** Check logs
```bash
docker compose logs patient-service
```

---

## References

- Docker Documentation: https://docs.docker.com
- Docker Compose: https://docs.docker.com/compose
- Kubernetes Documentation: https://kubernetes.io/docs
- HDIM Docker Setup: `docker-compose.yml`

---

**Last Updated:** January 20, 2026
**Difficulty Level:** ⭐⭐⭐⭐ (4/5 stars)
**Time Investment:** 1-2 weeks
**Prerequisite Skills:** Linux basics, command line, Spring Boot

---

**← [Skills Hub](../README.md)** | **→ [Next: API Design & OpenAPI](../09-api-design/openapi-design.md)**

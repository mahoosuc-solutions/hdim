# Demo Configuration Redesign Plan

**Created**: January 15, 2026  
**Status**: Planning

## Overview

Complete redesign of the demo docker-compose configuration to fix all identified issues, add gateway-edge service for clinical portal compatibility, include data processing/auditing/error management services, improve network configuration, and ensure robust initialization.

## Issues to Address

1. **Network Resolution Failures**: Services cannot resolve hostnames (postgres, zookeeper)
2. **Clinical Portal Configuration**: Nginx expects `gateway-edge` but only `gateway-service` exists
3. **Database Connection Errors**: Wrong database name references
4. **Service Dependencies**: Some services created but not started
5. **Initialization Timing**: Services starting before dependencies are ready
6. **Missing Data Processing**: event-processing-service, event-router-service, cdr-processor-service not included
7. **Auditing Infrastructure**: Ensure audit tables and Kafka publishing are operational
8. **Error Management**: Dead letter queue and error handling operational

## Implementation Plan

### 1. Add Gateway-Edge Service

**File**: `demo/docker-compose.demo.yml`

Add `gateway-edge` service as an nginx reverse proxy that forwards to `gateway-service`:

```yaml
gateway-edge:
  container_name: hdim-demo-gateway-edge
  image: nginx:1.27-alpine
  restart: unless-stopped
  depends_on:
    gateway-service:
      condition: service_healthy
  ports:
    - "18080:8080"
  volumes:
    - ./nginx-gateway-edge.conf:/etc/nginx/conf.d/default.conf:ro
  healthcheck:
    test: ["CMD-SHELL", "nginx -t && curl -f http://localhost:8080/health || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 5
  networks:
    - hdim-demo-network
```

**New File**: `demo/nginx-gateway-edge.conf`

Create nginx configuration that proxies all requests to `gateway-service:8080`:

```nginx
upstream gateway_backend {
    server gateway-service:8080;
}

server {
    listen 8080;
    server_name _;
    
    location / {
        proxy_pass http://gateway_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

### 2. Add Data Processing Services

**File**: `demo/docker-compose.demo.yml`

Add three data processing services:

#### 2.1 Event Processing Service
- Port: 8083
- Database: event_db
- Purpose: Kafka event processing with dead letter queue management
- Dependencies: postgres, kafka

#### 2.2 Event Router Service
- Port: 8095
- Database: event_router_db
- Purpose: Event routing and orchestration
- Dependencies: postgres, kafka

#### 2.3 CDR Processor Service
- Port: 8099
- Database: cdr_db
- Purpose: Clinical data repository processing
- Dependencies: postgres, kafka

### 3. Update Database Initialization

**File**: `demo/init-demo-db.sh`

Add databases for data processing services:
- event_db
- event_router_db
- cdr_db

Update script to use correct database name (`healthdata_demo` instead of `healthdata`).

### 4. Configure Auditing

**File**: `demo/docker-compose.demo.yml`

Ensure all services have audit configuration:
- `AUDIT_KAFKA_ENABLED: "true"`
- `AUDIT_KAFKA_TOPIC: "audit-events"`
- Kafka bootstrap servers configured

Verify audit tables exist in patient_db (already added via migrations).

### 5. Configure Error Management

**File**: `demo/docker-compose.demo.yml`

Ensure event-processing-service has:
- Dead letter queue enabled
- Retry logic configured
- Health checks for DLQ status

### 6. Fix Network Configuration

**File**: `demo/docker-compose.demo.yml`

- Ensure all services explicitly use `hdim-demo-network`
- Add network aliases for better service discovery
- Verify network is created with proper driver

### 7. Update Service Dependencies

**File**: `demo/docker-compose.demo.yml`

- Fix dependency chains to ensure proper startup order
- Use `condition: service_healthy` instead of `condition: service_started` where appropriate
- Add health checks for all services
- Ensure Kafka waits for Zookeeper to be fully ready

### 8. Update Clinical Portal Dependencies

**File**: `demo/docker-compose.demo.yml`

Change clinical-portal to depend on `gateway-edge` instead of `gateway-service`:

```yaml
clinical-portal:
  depends_on:
    - gateway-edge
    - init-infrastructure
```

### 9. Enhance Infrastructure Initialization

**File**: `demo/init-infrastructure.sh`

- Add better error handling for network resolution
- Verify services can resolve hostnames before proceeding
- Add retry logic with exponential backoff
- Improve logging for troubleshooting
- Verify audit infrastructure is operational
- Verify error management (DLQ) is operational

### 10. Update Gateway Service Configuration

**File**: `demo/docker-compose.demo.yml`

Add service URLs for data processing services:
- `BACKEND_SERVICES_EVENTS_URL: http://event-processing-service:8083/events`
- `BACKEND_SERVICES_EVENT_ROUTER_URL: http://event-router-service:8095`
- `BACKEND_SERVICES_CDR_PROCESSOR_URL: http://cdr-processor-service:8099`

### 11. Create Validation Script

**New File**: `demo/validate-demo.sh`

Create script to validate demo deployment:
- Check all services are healthy
- Verify network connectivity
- Test service endpoints
- Validate database connections
- Check demo users exist
- Verify audit events are being published
- Verify DLQ is operational
- Test error handling

### 12. Update Documentation

**Files**: 
- `demo/README.md`
- `demo/DEMO_WALKTHROUGH.md`

Update documentation to reflect:
- New gateway-edge service
- Data processing services
- Auditing capabilities
- Error management features
- Updated startup procedures
- New port mappings
- Updated troubleshooting guide

## Service Architecture

```
┌─────────────────────────────────────────┐
│         Clinical Portal (4200)          │
│         (nginx frontend)                 │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│      Gateway-Edge (18080)                │
│      (nginx reverse proxy)               │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│      Gateway-Service (8080)              │
│      (Spring Cloud Gateway)              │
└───┬───┬───┬───┬───┬───┬───┬──────────────┘
    │   │   │   │   │   │   │
    ▼   ▼   ▼   ▼   ▼   ▼   ▼
  FHIR Patient Care Quality CQL Events ...
    │   │   │   │   │   │   │
    └───┴───┴───┴───┴───┴───┘
               │
               ▼
┌─────────────────────────────────────────┐
│      Kafka (Event Streaming)             │
│      - audit-events                      │
│      - evaluation.completed              │
│      - care-gap.identified               │
└───┬─────────────────────────────────────┘
    │
    ├──► Event Processing Service (8083)
    │    - Dead Letter Queue
    │    - Error Management
    │
    ├──► Event Router Service (8095)
    │    - Event Routing
    │
    └──► CDR Processor Service (8099)
         - Data Processing
```

## Files to Modify

1. `demo/docker-compose.demo.yml` - Complete redesign with all services
2. `demo/init-demo-db.sh` - Add event_db, event_router_db, cdr_db
3. `demo/init-infrastructure.sh` - Enhance error handling and add validation
4. `demo/README.md` - Update documentation

## Files to Create

1. `demo/nginx-gateway-edge.conf` - Nginx config for gateway-edge
2. `demo/validate-demo.sh` - Deployment validation script

## Testing Plan

1. **Network Validation**: Verify all services can resolve hostnames
2. **Service Health**: All services reach healthy state
3. **Database Connectivity**: All services can connect to databases
4. **API Endpoints**: All service endpoints respond correctly
5. **Clinical Portal**: Frontend loads and can communicate with backend
6. **Demo Users**: Users can authenticate and access portal
7. **Audit Events**: Verify audit events are published to Kafka
8. **Error Management**: Test DLQ functionality
9. **Data Processing**: Verify event processing and routing works

## Success Criteria

- All 19 services start successfully (14 original + 3 data processing + 1 gateway-edge + 1 init)
- All services reach healthy state within 5 minutes
- Network resolution works for all services
- Clinical portal loads without errors
- Demo users can authenticate
- All health endpoints return 200 OK
- Database connections successful
- Audit events published to Kafka
- Dead letter queue operational
- No critical errors in logs

## Rollback Plan

- Keep backup of current `docker-compose.demo.yml`
- Document all changes for easy rollback
- Test in isolated environment first

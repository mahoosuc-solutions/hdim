---
name: hdim-service
description: Manage HDIM services (start, stop, restart, logs, health)
category: hdim
---

# HDIM Service Management Command

Manage HDIM microservices with quick commands.

## Usage

```
/hdim-service <action> [service-name]
```

**Actions**:
- `start` - Start all services or specific service
- `stop` - Stop all services or specific service
- `restart` - Restart all services or specific service
- `logs` - View logs for specific service
- `health` - Check health of all services
- `list` - List all available services

## Examples

```bash
# Start all services
/hdim-service start

# Start specific service
/hdim-service start quality-measure-service

# View logs
/hdim-service logs quality-measure-service

# Check health
/hdim-service health

# List all services
/hdim-service list
```

## Implementation

```bash
#!/bin/bash

ACTION=$1
SERVICE=$2

case "$ACTION" in
  start)
    if [ -z "$SERVICE" ]; then
      echo "🚀 Starting all HDIM services..."
      docker compose up -d
      echo "✅ Services started"
      echo "Run '/hdim-service health' to check status"
    else
      echo "🚀 Starting $SERVICE..."
      docker compose up -d "$SERVICE"
      echo "✅ $SERVICE started"
    fi
    ;;

  stop)
    if [ -z "$SERVICE" ]; then
      echo "⏹️  Stopping all HDIM services..."
      docker compose down
      echo "✅ Services stopped"
    else
      echo "⏹️  Stopping $SERVICE..."
      docker compose stop "$SERVICE"
      echo "✅ $SERVICE stopped"
    fi
    ;;

  restart)
    if [ -z "$SERVICE" ]; then
      echo "🔄 Restarting all HDIM services..."
      docker compose restart
      echo "✅ Services restarted"
    else
      echo "🔄 Restarting $SERVICE..."
      docker compose restart "$SERVICE"
      echo "✅ $SERVICE restarted"
    fi
    ;;

  logs)
    if [ -z "$SERVICE" ]; then
      echo "❌ Please specify a service name"
      echo "Example: /hdim-service logs quality-measure-service"
      exit 1
    fi
    echo "📋 Showing logs for $SERVICE (Ctrl+C to exit)..."
    docker compose logs -f "$SERVICE"
    ;;

  health)
    echo "🏥 Checking HDIM service health..."
    bash .serena/tools/check-service-health.sh
    ;;

  list)
    echo "📦 HDIM Services:"
    echo ""
    echo "Core Services:"
    echo "  gateway-service           (8001) - API Gateway & Auth"
    echo "  cql-engine-service        (8081) - CQL Evaluation"
    echo "  patient-service           (8084) - Patient Data"
    echo "  fhir-service              (8085) - FHIR R4 Server"
    echo "  care-gap-service          (8086) - Care Gap Detection"
    echo "  quality-measure-service   (8087) - HEDIS Measures"
    echo ""
    echo "Infrastructure:"
    echo "  postgres                  (5435) - Database"
    echo "  redis                     (6380) - Cache"
    echo "  kafka                     (9094) - Event Streaming"
    echo ""
    echo "Usage: /hdim-service <action> [service-name]"
    ;;

  *)
    echo "Unknown action: $ACTION"
    echo ""
    echo "Available actions:"
    echo "  start [service]    - Start services"
    echo "  stop [service]     - Stop services"
    echo "  restart [service]  - Restart services"
    echo "  logs <service>     - View logs"
    echo "  health             - Check health"
    echo "  list               - List services"
    exit 1
    ;;
esac
```

## Service Names

**Core Services**:
- gateway-service
- cql-engine-service
- patient-service
- fhir-service
- care-gap-service
- quality-measure-service

**Infrastructure**:
- postgres
- redis
- kafka

## Quick Tasks

### Start Development Environment
```bash
/hdim-service start
/hdim-service health
```

### Debug Service Issues
```bash
/hdim-service logs quality-measure-service
/hdim-service restart quality-measure-service
```

### Shutdown for Maintenance
```bash
/hdim-service stop
```

## Related Commands

- `/hdim-validate health` - Detailed health check
- `/hdim-memory troubleshooting` - Troubleshooting guide
- `/docker:*` - Advanced Docker operations

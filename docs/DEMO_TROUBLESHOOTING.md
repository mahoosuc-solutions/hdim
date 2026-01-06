# HDIM Demo Platform - Troubleshooting Guide

**Version**: 1.0
**Last Updated**: January 2026
**Audience**: Demo Operators, Sales Engineers, DevOps

---

## Quick Diagnostics

Run this first to understand system state:

```bash
# Check service status
docker compose -f docker-compose.demo.yml ps

# Check demo status
./backend/tools/demo-cli/demo-cli.sh status

# View recent logs
docker compose -f docker-compose.demo.yml logs --tail=50
```

---

## Common Issues & Solutions

### 1. Services Won't Start

**Symptoms**:
- `docker compose up` fails
- Services show as "unhealthy" or "restarting"

**Diagnosis**:
```bash
# Check which services are failing
docker compose -f docker-compose.demo.yml ps

# View logs for failing service
docker compose -f docker-compose.demo.yml logs gateway-service
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| Port already in use | Stop other services using ports 4200, 8080-8098 |
| Not enough memory | Increase Docker memory to 8GB+ |
| Old containers | Run `docker compose down -v` and start fresh |
| Corrupt images | Run `docker compose build --no-cache` |

**Full Reset**:
```bash
# Stop everything and remove volumes
docker compose -f docker-compose.demo.yml down -v

# Remove orphan containers
docker container prune -f

# Start fresh
docker compose -f docker-compose.demo.yml up -d
```

---

### 2. Login Fails

**Symptoms**:
- "Invalid credentials" message
- Login page redirects back without error

**Diagnosis**:
```bash
# Check gateway service logs
docker compose -f docker-compose.demo.yml logs gateway-service | grep -i "auth\|login"

# Verify users exist
docker exec -it hdim-demo-postgres psql -U healthdata -d gateway_db -c \
  "SELECT username, enabled FROM users WHERE username LIKE 'demo%';"
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| Users not seeded | Run `./demo-cli.sh initialize` |
| Database empty | Run `./demo-cli.sh reset` |
| Gateway down | Restart: `docker compose restart gateway-service` |
| Cookie issues | Clear browser cookies for localhost |

**Re-seed Users**:
```bash
# Initialize demo data including users
./demo-cli.sh initialize

# Or run full reset
./demo-cli.sh reset
```

---

### 3. Demo Data Missing

**Symptoms**:
- Patient list is empty
- Care gaps show zero count
- Quality measures show no results

**Diagnosis**:
```bash
# Check demo status
./demo-cli.sh status

# Check patient count
docker exec -it hdim-demo-postgres psql -U healthdata -d fhir_db -c \
  "SELECT COUNT(*) FROM patient_entity;"

# Check care gaps
docker exec -it hdim-demo-postgres psql -U healthdata -d caregap_db -c \
  "SELECT COUNT(*) FROM care_gaps;"
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| No scenario loaded | Run `./demo-cli.sh load-scenario hedis-evaluation` |
| Data not persisted | Check FHIR service is healthy |
| Wrong tenant | Verify tenant ID in query params |

**Regenerate Data**:
```bash
# Load specific scenario
./demo-cli.sh load-scenario hedis-evaluation

# Or generate custom cohort
./demo-cli.sh generate-patients --count 5000 --tenant acme-health --care-gap-percentage 28
```

---

### 4. Demo Mode UI Not Working

**Symptoms**:
- No demo control bar
- Tooltips not appearing
- Highlights not visible

**Diagnosis**:
- Check URL includes `?demo=true`
- Check browser console for JavaScript errors
- Verify Angular app loaded correctly

**Solutions**:

| Issue | Solution |
|-------|----------|
| Missing URL param | Add `?demo=true` to URL |
| JavaScript errors | Clear cache, hard refresh (Ctrl+Shift+R) |
| Component not loaded | Check Network tab for 404s |
| CSS not applied | Check for SCSS compilation errors |

**Force Refresh**:
```bash
# Clear Angular cache and rebuild
cd apps/clinical-portal
rm -rf node_modules/.cache
npm run build

# Restart clinical portal container
docker compose -f docker-compose.demo.yml restart clinical-portal
```

---

### 5. Slow Performance

**Symptoms**:
- Page load > 5 seconds
- Quality measure evaluation > 30 seconds
- UI feels sluggish

**Diagnosis**:
```bash
# Check resource usage
docker stats

# Check database connections
docker exec -it hdim-demo-postgres psql -U healthdata -d healthdata_db -c \
  "SELECT count(*) FROM pg_stat_activity;"

# Check Redis memory
docker exec -it hdim-demo-redis redis-cli INFO memory
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| Memory pressure | Increase Docker RAM to 12GB+ |
| Database not indexed | Run `./demo-cli.sh reset` to reapply migrations |
| Too many patients | Use smaller dataset (2000 patients) |
| Redis cache miss | Warm cache with `./demo-cli.sh status` |

**Optimize Resources**:
```bash
# Increase Docker resources in Docker Desktop:
# - Memory: 12GB
# - CPUs: 4+
# - Swap: 4GB

# Or use resource-limited dataset
./demo-cli.sh load-scenario patient-journey  # Uses 1000 patients
```

---

### 6. Snapshot/Restore Fails

**Symptoms**:
- Snapshot creation hangs
- Restore fails with error
- Snapshot file not found

**Diagnosis**:
```bash
# List snapshots
./demo-cli.sh snapshot list

# Check snapshot directory
ls -la /path/to/demo-snapshots/

# Check disk space
df -h
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| Disk full | Free up space, remove old snapshots |
| Permission denied | Check volume mount permissions |
| Snapshot corrupted | Create new snapshot |
| Directory missing | Create: `mkdir -p ./demo-snapshots` |

**Recreate Snapshot**:
```bash
# Remove old snapshots
./demo-cli.sh snapshot list
# Note names, then manually delete from snapshot directory

# Create fresh snapshot
./demo-cli.sh load-scenario hedis-evaluation
./demo-cli.sh snapshot create "fresh-baseline"
```

---

### 7. Demo CLI Not Working

**Symptoms**:
- `./demo-cli.sh` command not found
- Permission denied
- Java errors

**Diagnosis**:
```bash
# Check CLI location
ls -la backend/tools/demo-cli/

# Check Java version
java -version

# Check if built
ls -la backend/tools/demo-cli/build/libs/
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| Not executable | `chmod +x backend/tools/demo-cli/demo-cli.sh` |
| Not built | `cd backend && ./gradlew :tools:demo-cli:bootJar` |
| Wrong Java | Use Java 21+ |
| Path issues | Use full path to script |

**Rebuild CLI**:
```bash
cd backend
./gradlew :tools:demo-cli:bootJar
chmod +x tools/demo-cli/demo-cli.sh
```

---

### 8. Database Connection Issues

**Symptoms**:
- "Connection refused" errors
- Services can't reach database
- Timeout on queries

**Diagnosis**:
```bash
# Check postgres is running
docker compose -f docker-compose.demo.yml ps postgres

# Check postgres logs
docker compose -f docker-compose.demo.yml logs postgres | tail -50

# Test connection
docker exec -it hdim-demo-postgres pg_isready
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| Postgres not started | `docker compose restart postgres` |
| Wrong credentials | Check environment variables in compose file |
| Port conflict | Change port in docker-compose.demo.yml |
| Container crashed | Check logs, restart container |

**Database Reset**:
```bash
# Stop services
docker compose -f docker-compose.demo.yml stop

# Remove postgres volume
docker volume rm hdim-master_demo_postgres_data

# Restart
docker compose -f docker-compose.demo.yml up -d
```

---

### 9. FHIR Service Errors

**Symptoms**:
- Patient creation fails
- 500 errors on FHIR endpoints
- Invalid FHIR resources

**Diagnosis**:
```bash
# Check FHIR service logs
docker compose -f docker-compose.demo.yml logs fhir-service | tail -100

# Test FHIR endpoint
curl -X GET http://localhost:8085/fhir/Patient?_count=1
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| Service unhealthy | `docker compose restart fhir-service` |
| Invalid resources | Check generator for FHIR R4 compliance |
| Database full | Clear old test data |
| Memory issues | Increase container memory limit |

---

### 10. Frontend Build Errors

**Symptoms**:
- Angular app won't load
- 404 on static assets
- White screen

**Diagnosis**:
```bash
# Check clinical portal container
docker compose -f docker-compose.demo.yml logs clinical-portal

# Check if nginx is serving
curl -I http://localhost:4200
```

**Solutions**:

| Issue | Solution |
|-------|----------|
| Build failed | Rebuild: `docker compose build clinical-portal` |
| Assets missing | Check Dockerfile copies correctly |
| Nginx config error | Check nginx.conf syntax |
| Base href wrong | Verify Angular build has correct base |

**Rebuild Frontend**:
```bash
# Full rebuild
docker compose -f docker-compose.demo.yml build --no-cache clinical-portal
docker compose -f docker-compose.demo.yml up -d clinical-portal
```

---

## Emergency Recovery

If all else fails, complete reset:

```bash
#!/bin/bash
# emergency-reset.sh

echo "Stopping all demo services..."
docker compose -f docker-compose.demo.yml down -v

echo "Removing all demo containers..."
docker container prune -f

echo "Removing demo volumes..."
docker volume rm hdim-master_demo_postgres_data hdim-master_demo_snapshots 2>/dev/null

echo "Rebuilding all services..."
docker compose -f docker-compose.demo.yml build --no-cache

echo "Starting services..."
docker compose -f docker-compose.demo.yml up -d

echo "Waiting for services to be healthy..."
sleep 60

echo "Initializing demo data..."
./backend/tools/demo-cli/demo-cli.sh reset
./backend/tools/demo-cli/demo-cli.sh initialize

echo "Loading HEDIS scenario..."
./backend/tools/demo-cli/demo-cli.sh load-scenario hedis-evaluation

echo "Creating baseline snapshot..."
./backend/tools/demo-cli/demo-cli.sh snapshot create "emergency-recovery-baseline"

echo "Recovery complete!"
./backend/tools/demo-cli/demo-cli.sh status
```

---

## Contact Support

If issues persist after trying these solutions:

1. **Gather Information**:
   - `./demo-cli.sh status` output
   - `docker compose logs > demo-logs.txt`
   - Browser console errors (F12)
   - Steps to reproduce

2. **Create Issue**:
   - Label: `[DEMO]`
   - Priority: P1 (if blocking demo)
   - Include all gathered information

3. **Escalation Path**:
   - Demo issues blocking prospect calls → Engineering Lead
   - Infrastructure issues → DevOps
   - Feature questions → Product Team

---

## Preventive Maintenance

### Daily (Before Demos)
- Check `./demo-cli.sh status`
- Verify services healthy
- Test login works
- Create fresh snapshot

### Weekly
- Clear old snapshots (keep last 5)
- Check disk space
- Review error logs
- Update demo data if needed

### Monthly
- Pull latest demo images
- Update demo scripts if features changed
- Review performance metrics
- Refresh patient data for current dates

---

**Last Updated**: January 2026

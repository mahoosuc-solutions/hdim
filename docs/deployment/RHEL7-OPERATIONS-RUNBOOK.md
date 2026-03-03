# HDIM RHEL 7 Operations Runbook

> **Audience:** Operations engineers and on-call staff
> **Purpose:** Day-2 operations, incident response, and maintenance procedures
> **Related docs:** [Deployment Guide](./RHEL7-DEPLOYMENT-GUIDE.md) | [Architecture Reference](./RHEL7-ARCHITECTURE.md)

---

## Quick Reference Card

```bash
# Start / Stop / Restart
sudo systemctl start hdim
sudo systemctl stop hdim
sudo systemctl restart hdim

# Status
sudo systemctl status hdim
sudo ./installer/rhel7/hdim-install.sh status

# Logs
journalctl -u hdim -f                              # systemd journal
docker compose -f /opt/hdim/current/source/docker-compose.yml logs -f  # all containers
docker compose -f /opt/hdim/current/source/docker-compose.yml logs -f patient-service  # specific

# Health
curl -s http://localhost:8080/actuator/health | python -m json.tool
/opt/hdim/current/source/scripts/smoke-tests.sh http://localhost:8080 --quick

# Upgrade / Rollback
sudo ./installer/rhel7/hdim-install.sh upgrade --version <tag>
sudo ./installer/rhel7/hdim-install.sh rollback

# Shell aliases (after login)
hdim-status    # = systemctl status hdim + container status
hdim-logs      # = docker compose logs -f
hdim-restart   # = systemctl restart hdim
hdim-stop      # = systemctl stop hdim
hdim-start     # = systemctl start hdim
```

---

## Log Locations

| Log | Location | Purpose |
|-----|----------|---------|
| Install log | `/opt/hdim/shared/logs/install.log` | Installation/upgrade history |
| Docker container logs | `docker compose logs <service>` | Application output |
| systemd journal | `journalctl -u hdim` | Service lifecycle events |
| Docker daemon | `journalctl -u docker` | Container engine events |
| Audit trail | Via audit-service API | HIPAA compliance |

### Useful journalctl Queries

```bash
# Last 100 lines from hdim service
journalctl -u hdim -n 100 --no-pager

# Errors only
journalctl -u hdim -p err --since "1 hour ago"

# Follow in real-time
journalctl -u hdim -f

# Docker daemon errors
journalctl -u docker -p err --since today
```

---

## Common Failure Scenarios

### 1. Service Won't Start After Reboot

**Symptoms:** `systemctl status hdim` shows failed state

**Diagnosis:**
```bash
# Check systemd logs
journalctl -u hdim --since "10 minutes ago"

# Check Docker daemon
systemctl status docker

# Check disk space
df -h /opt /var/lib/docker

# Check memory
free -h
```

**Resolution:**
```bash
# If Docker not running
sudo systemctl restart docker
sudo systemctl restart hdim

# If disk full
docker system prune --volumes  # WARNING: removes unused volumes
sudo systemctl restart hdim

# If OOM killed
# Reduce profile or add RAM
```

### 2. Database Connection Failures

**Symptoms:** Services log `Connection refused` or `FATAL: password authentication failed`

**Diagnosis:**
```bash
# Check PostgreSQL container
docker ps --filter 'name=postgres'
docker compose logs postgres | tail -20

# Test connection
docker exec -it $(docker ps -q --filter 'name=postgres') \
  psql -U healthdata -c "SELECT 1;"
```

**Resolution:**
```bash
# If container stopped
cd /opt/hdim/current/source
docker compose up -d postgres
sleep 10

# If password mismatch (after secrets regeneration)
# Check .env matches what PostgreSQL was initialized with
cat /opt/hdim/current/.env | grep POSTGRES_PASSWORD
```

### 3. Kafka Broker Unhealthy

**Symptoms:** Event services can't publish/consume, logs show `LEADER_NOT_AVAILABLE`

**Diagnosis:**
```bash
docker compose logs kafka | tail -30
docker compose logs zookeeper | tail -20

# Check ulimits inside container
docker exec $(docker ps -q --filter 'name=kafka') cat /proc/self/limits | grep "open files"
```

**Resolution:**
```bash
# Restart Kafka (preserves data)
docker compose restart kafka

# If ulimits are too low, verify /etc/docker/daemon.json has:
# "default-ulimits": { "nofile": { "Hard": 65536, "Soft": 65536 } }
sudo systemctl restart docker
sudo systemctl restart hdim
```

### 4. Out of Memory (OOM)

**Symptoms:** Containers killed, `dmesg | grep -i oom` shows kills

**Diagnosis:**
```bash
# Check which containers are using most memory
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}"

# Check system memory
free -h
```

**Resolution:**
- Reduce profile (e.g., `full` → `core`)
- Add swap (temporary): `fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile`
- Add physical RAM

### 5. Port Conflict

**Symptoms:** Container exits with `bind: address already in use`

**Diagnosis:**
```bash
# Find what's using the port
ss -tlnp | grep <port>
lsof -i :<port>
```

**Resolution:**
- Stop the conflicting service
- Or modify port mappings in docker-compose.yml

---

## Upgrade Walkthrough

### Pre-Upgrade Checklist

- [ ] Verify current platform is healthy: `hdim-install.sh status`
- [ ] Ensure sufficient disk for backup + new release (~20 GB free)
- [ ] Communicate maintenance window to users
- [ ] Verify new version is tested in staging

### Upgrade Procedure

```bash
# 1. Transfer new source to server (if not using git)
scp hdim-new-version.tar.gz admin@server:/tmp/

# 2. Run upgrade (handles backup, build, cutover, validation)
sudo ./installer/rhel7/hdim-install.sh upgrade --version <tag>

# 3. If upgrade succeeds — verify
hdim-status
/opt/hdim/current/source/scripts/smoke-tests.sh http://localhost:8080

# 4. If upgrade fails — it auto-rolls back
# Manual rollback if needed:
sudo ./installer/rhel7/hdim-install.sh rollback
```

### Post-Upgrade Verification

```bash
# Check all services healthy
curl -s http://localhost:8080/actuator/health

# Verify version
cat /etc/hdim/hdim.conf | grep HDIM_VERSION

# Check for any error logs
docker compose logs --since "10 minutes ago" 2>&1 | grep -i error | head -20
```

---

## Rollback Decision Tree

```
Service health check fails after upgrade?
│
├─ YES → Were database migrations applied?
│   │
│   ├─ YES → Rollback with database restore:
│   │         hdim-install.sh rollback --to <release>
│   │         (Select "Y" when prompted for DB restore)
│   │
│   └─ NO  → Rollback without DB restore:
│             hdim-install.sh rollback --to <release>
│             (Select "N" when prompted for DB restore)
│
└─ NO → Platform is healthy, upgrade successful
```

---

## Backup Verification

### Manual Backup

```bash
# Create ad-hoc backup
BACKUP_DIR="/opt/hdim/backups/$(date '+%Y%m%d-%H%M%S')"
mkdir -p "$BACKUP_DIR"

PG_CONTAINER=$(docker ps --filter 'name=postgres' --format '{{.Names}}' | head -1)

# List databases
docker exec $PG_CONTAINER psql -U healthdata -t -c \
  "SELECT datname FROM pg_database WHERE datistemplate = false AND datname != 'postgres';"

# Dump each database
for db in $(docker exec $PG_CONTAINER psql -U healthdata -t -c \
  "SELECT datname FROM pg_database WHERE datistemplate = false AND datname != 'postgres';" | tr -d ' '); do
    docker exec $PG_CONTAINER pg_dump -U healthdata -Fc "$db" > "${BACKUP_DIR}/${db}.dump"
    echo "Backed up: $db ($(du -h "${BACKUP_DIR}/${db}.dump" | cut -f1))"
done
```

### Verify Backup Integrity

```bash
# Check dump file is valid
pg_restore --list "${BACKUP_DIR}/patient_db.dump" | head -20
```

### Backup Retention

Backups are created automatically during upgrades in `/opt/hdim/backups/`. Clean up old backups periodically:

```bash
# List backups sorted by date
ls -lt /opt/hdim/backups/

# Remove backups older than 30 days
find /opt/hdim/backups/ -maxdepth 1 -type d -mtime +30 -exec rm -rf {} \;
```

---

## HIPAA Incident Response

### Suspected PHI Breach

1. **Contain:** Stop external access immediately
   ```bash
   firewall-cmd --remove-port=8080/tcp --permanent
   firewall-cmd --remove-port=4200/tcp --permanent
   firewall-cmd --reload
   ```

2. **Preserve evidence:** Do NOT delete logs
   ```bash
   # Copy audit logs
   cp -r /opt/hdim/shared/logs/ /tmp/incident-$(date +%Y%m%d)/
   docker compose logs > /tmp/incident-$(date +%Y%m%d)/docker-all.log
   ```

3. **Investigate:** Check audit trail
   ```bash
   # Query audit service for recent access
   curl -s http://localhost:8091/audit/api/v1/audit-events?since=2026-03-01 | python -m json.tool
   ```

4. **Report:** Follow your organization's HIPAA breach notification procedures

### Security Event Investigation

```bash
# Check for failed authentication attempts
docker compose logs gateway-service 2>&1 | grep -i "authentication failed" | tail -20

# Check for unauthorized access attempts
docker compose logs audit-service 2>&1 | grep -i "forbidden\|unauthorized" | tail -20

# Check for unusual API patterns
docker compose logs gateway-service 2>&1 | grep -c "$(date +%Y-%m-%d)" # Total requests today
```

---

## Escalation Procedures

| Severity | Criteria | Response Time | Escalation |
|----------|----------|---------------|------------|
| **P1 - Critical** | Platform down, PHI exposure risk | Immediate | On-call + management |
| **P2 - Major** | Service degraded, some features unavailable | 1 hour | On-call engineer |
| **P3 - Minor** | Non-critical service warning, performance issue | Next business day | Ops team |
| **P4 - Info** | Maintenance, planned upgrade | Scheduled | Ops team |

### P1 Quick Actions

```bash
# 1. Check overall health
sudo systemctl status hdim
docker ps --filter "status=exited"

# 2. Restart if needed
sudo systemctl restart hdim

# 3. If restart fails, check resources
df -h /opt /var/lib/docker
free -h
dmesg | tail -20

# 4. If all else fails, rollback
sudo ./installer/rhel7/hdim-install.sh rollback
```

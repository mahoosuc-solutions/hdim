# HDIM RHEL 7 Deployment Guide

> **Audience:** IT administrators performing initial deployment
> **Time estimate:** 45-90 minutes (depending on network speed and profile)
> **Related docs:** [Architecture Reference](./RHEL7-ARCHITECTURE.md) | [Operations Runbook](./RHEL7-OPERATIONS-RUNBOOK.md)

---

## Table of Contents

1. [Pre-Installation Checklist](#1-pre-installation-checklist)
2. [Installation](#2-installation)
3. [Post-Install Verification](#3-post-install-verification)
4. [TLS Certificate Installation](#4-tls-certificate-installation)
5. [Firewall Configuration](#5-firewall-configuration)
6. [Upgrading](#6-upgrading)
7. [Support](#7-support)

---

## 1. Pre-Installation Checklist

### Hardware Requirements

| Profile | RAM | CPU Cores | Disk (/opt) | Containers |
|---------|-----|-----------|-------------|------------|
| **light** | 8 GB | 4 | 20 GB | ~3 (infra only) |
| **core** | 16 GB | 4 | 40 GB | ~16 (services + gateways) |
| **ai** | 24 GB | 8 | 50 GB | ~20 |
| **analytics** | 24 GB | 8 | 50 GB | ~22 |
| **full** | 32 GB+ | 8+ | 60 GB+ | ~55 (all services) |

### Operating System

- RHEL 7.7+ or CentOS 7.7+ (kernel ≥ 3.10.0-1062)
- glibc ≥ 2.17
- Active RHEL subscription (for yum repos) or CentOS mirrors

### Network Requirements

- Internet access to:
  - `github.com` (Temurin JDK download)
  - `download.docker.com` (Docker CE repo)
  - `rpm.nodesource.com` (Node.js repo)
  - `registry.hub.docker.com` (base Docker images)
- Outbound HTTPS (port 443) required during install
- See [Architecture Reference](./RHEL7-ARCHITECTURE.md) for port map

### Pre-Installation Tasks

- [ ] Verify hardware meets profile requirements
- [ ] Ensure NTP is synchronized (`timedatectl set-ntp true`)
- [ ] Set ulimits: Add to `/etc/security/limits.conf`:
  ```
  * soft nofile 65536
  * hard nofile 65536
  ```
- [ ] Reboot after ulimit changes
- [ ] Ensure no conflicting services on ports 5435, 6380, 8080-8099, 4200
- [ ] Obtain TLS certificates for production (optional for initial install)

---

## 2. Installation

### Step 1: Transfer Installer

Copy the HDIM source to the target server:

```bash
# Option A: Git clone (if server has git + internet)
git clone https://github.com/your-org/hdim.git /tmp/hdim-source
cd /tmp/hdim-source

# Option B: Tarball transfer
scp hdim-release.tar.gz admin@server:/tmp/
ssh admin@server
tar xzf /tmp/hdim-release.tar.gz
cd hdim-source
```

### Step 2: Run Installer

```bash
sudo ./installer/rhel7/hdim-install.sh install --profile core
```

The installer runs 10 phases automatically:

| Phase | Duration | Description |
|-------|----------|-------------|
| 0. Pre-flight | ~30s | System validation |
| 1. Packages | ~2 min | Docker CE, Node.js, utilities |
| 2. Java | ~1 min | Temurin JDK 21 (SHA256-verified) |
| 3. Docker | ~30s | Daemon config, Compose v2, hdim user |
| 4. Profile | ~10s | Profile selection + config |
| 5. Secrets | ~10s | Cryptographic secret generation |
| 6. Release | ~1 min | Source copy to /opt/hdim/releases/ |
| 7. Build | ~15-30 min | Gradle JARs + Docker images |
| 8. Start | ~2-5 min | Docker Compose startup |
| 9. Health | ~2-5 min | Service health verification |

### Step 3: Save Generated Secrets

During Phase 5, the installer displays generated secrets **once**. Save them in your organization's secrets manager (e.g., HashiCorp Vault, CyberArk).

### Step 4: Verify

```bash
# Check systemd service
systemctl status hdim

# Check platform status
sudo ./installer/rhel7/hdim-install.sh status

# Check individual containers
docker compose -f /opt/hdim/current/source/docker-compose.yml ps
```

---

## 3. Post-Install Verification

### Access the Clinical Portal

Open a browser to: `http://<server-hostname>:4200`

### Verify API Gateway

```bash
curl -s http://localhost:8080/actuator/health | python -m json.tool
```

Expected response:
```json
{
    "status": "UP"
}
```

### Verify Services (by profile)

```bash
# List all running containers
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# Check specific service health
curl -s http://localhost:8084/patient/actuator/health
curl -s http://localhost:8086/care-gap/actuator/health
curl -s http://localhost:8085/fhir/actuator/health
```

### Run Smoke Tests

```bash
/opt/hdim/current/source/scripts/smoke-tests.sh http://localhost:8080 --quick
```

---

## 4. TLS Certificate Installation

For production deployments, configure TLS:

1. Place certificate files in `/opt/hdim/shared/ssl/`:
   ```bash
   cp server.crt server.key ca-bundle.crt /opt/hdim/shared/ssl/
   chown hdim:hdim /opt/hdim/shared/ssl/*
   chmod 600 /opt/hdim/shared/ssl/server.key
   ```

2. Update the `.env` file:
   ```bash
   # Edit /opt/hdim/current/.env
   SSL_ENABLED=true
   SSL_CERT_PATH=/opt/hdim/shared/ssl/server.crt
   SSL_KEY_PATH=/opt/hdim/shared/ssl/server.key
   ```

3. Restart the platform:
   ```bash
   sudo systemctl restart hdim
   ```

---

## 5. Firewall Configuration

The installer automatically opens external-facing ports. Internal ports (database, cache, broker) are **not** exposed externally.

| Port | Service | External Access |
|------|---------|----------------|
| 80 | HTTP redirect | Yes |
| 443 | HTTPS | Yes |
| 8080 | API Gateway | Yes |
| 4200 | Clinical Portal | Yes |
| 5435 | PostgreSQL | **No** (localhost only) |
| 6380 | Redis | **No** (localhost only) |
| 9094 | Kafka | **No** (localhost only) |

To restrict access further:

```bash
# Allow only specific subnets
firewall-cmd --permanent --add-rich-rule='rule family=ipv4 source address=10.0.0.0/8 port port=8080 protocol=tcp accept'
firewall-cmd --reload
```

---

## 6. Upgrading

### Standard Upgrade

```bash
# Transfer new source to server, then:
sudo ./installer/rhel7/hdim-install.sh upgrade --version <git-tag>
```

The upgrade process:
1. Backs up all PostgreSQL databases to `/opt/hdim/backups/`
2. Creates a new release directory
3. Copies secrets from current release
4. Builds updated services
5. Performs atomic symlink swap
6. Restarts via systemd
7. Runs smoke tests — **auto-rolls back if tests fail**

### Rollback

If you need to manually roll back after an upgrade:

```bash
# Roll back to the previous release
sudo ./installer/rhel7/hdim-install.sh rollback

# Or specify a specific release
sudo ./installer/rhel7/hdim-install.sh rollback --to 2026.03.01-abc1234
```

---

## 7. Support

### Log Locations

| Log | Location |
|-----|----------|
| Install log | `/opt/hdim/shared/logs/install.log` |
| Docker logs | `docker compose logs -f <service>` |
| Systemd journal | `journalctl -u hdim -f` |

### Quick Troubleshooting

| Symptom | Action |
|---------|--------|
| Service won't start | `docker compose logs <service> \| tail -50` |
| Port conflict | `ss -tlnp \| grep <port>` |
| Disk full | `df -h /opt && docker system prune` |
| Memory pressure | `free -h && docker stats --no-stream` |

### Getting Help

- **Operations Runbook:** [RHEL7-OPERATIONS-RUNBOOK.md](./RHEL7-OPERATIONS-RUNBOOK.md)
- **Architecture Reference:** [RHEL7-ARCHITECTURE.md](./RHEL7-ARCHITECTURE.md)
- **HDIM Documentation Portal:** [docs/README.md](../README.md)

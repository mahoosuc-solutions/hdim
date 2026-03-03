# HDIM RHEL 7 Architecture Reference

> **Audience:** Enterprise architects and security reviewers
> **Purpose:** Evaluate the installer's scope, security controls, and HIPAA alignment
> **Related docs:** [Deployment Guide](./RHEL7-DEPLOYMENT-GUIDE.md) | [Operations Runbook](./RHEL7-OPERATIONS-RUNBOOK.md)

---

## Table of Contents

1. [Installer Scope](#1-installer-scope)
2. [Filesystem Layout](#2-filesystem-layout)
3. [Profile Comparison](#3-profile-comparison)
4. [Network Architecture](#4-network-architecture)
5. [Secrets Management](#5-secrets-management)
6. [HIPAA Controls](#6-hipaa-controls)
7. [RHEL 7 Constraints](#7-rhel-7-constraints)
8. [Upgrade & Rollback Architecture](#8-upgrade--rollback-architecture)

---

## 1. Installer Scope

### What the Installer Configures

| Component | Action | Notes |
|-----------|--------|-------|
| Eclipse Temurin JDK 21 | Download + SHA256-verify + extract | `/opt/java/temurin-21/` |
| Docker CE | yum install + daemon.json | overlay2 driver, log rotation |
| Docker Compose v2 | Plugin install | `/usr/local/lib/docker/cli-plugins/` |
| Node.js 20 LTS | yum install (NodeSource repo) | For Angular build |
| PostgreSQL client | yum install | For `pg_dump` during upgrades |
| hdim system user | useradd + docker group | Non-login, runs containers |
| systemd unit | Template-rendered | `hdim.service` |
| Firewall rules | firewalld or iptables | External ports only |
| Logrotate | Template-rendered | 14-day retention |
| SELinux contexts | `container_var_lib_t` | Only if SELinux enforcing |

### What the Installer Does NOT Configure

| Component | Responsibility |
|-----------|---------------|
| TLS certificates | Customer IT (placed in `/opt/hdim/shared/ssl/`) |
| DNS records | Customer IT |
| Load balancer | Customer IT (if HA required) |
| LDAP/SSO integration | Post-install configuration |
| HashiCorp Vault | Optional replacement for `.env` secrets |
| Backup scheduling | Customer ops (cron + `hdim-install.sh upgrade` handles pre-upgrade) |
| OS patching | Customer IT (standard RHEL patching) |
| Kubernetes | Not applicable (Docker Compose deployment) |

---

## 2. Filesystem Layout

```
/opt/hdim/                              ← HDIM_BASE (owned by hdim:hdim)
├── releases/                           ← Versioned deployments
│   ├── 2026.03.02-a79a69d/             ← <date>-<short-sha>
│   │   ├── source/                     ← Project files (docker-compose.yml, backend/, apps/)
│   │   ├── .env                        ← Generated secrets (mode 0600)
│   │   └── compose-profile.txt         ← Selected profile name
│   └── 2026.03.15-b81c3e2/            ← After upgrade
├── current → releases/2026.03.15-.../  ← Symlink (atomic swap on upgrade)
├── shared/                             ← Persists across releases
│   ├── data/
│   │   ├── postgres/                   ← Database files (bind-mount)
│   │   ├── redis/                      ← Cache persistence
│   │   └── kafka/                      ← Message broker data
│   ├── ssl/                            ← Customer-provided TLS certs
│   └── logs/                           ← install.log
└── backups/                            ← Pre-upgrade pg_dump archives
    └── 20260315-143022/

/etc/hdim/hdim.conf                     ← Site config (HDIM_PROFILE, JAVA_HOME)
/etc/systemd/system/hdim.service        ← Systemd unit
/opt/java/temurin-21/                   ← JDK installation
/etc/profile.d/hdim-java.sh             ← JAVA_HOME export
/etc/profile.d/hdim-ops.sh              ← Convenience aliases
/etc/logrotate.d/hdim                   ← Log rotation config
```

### Design Rationale

- **Symlink swap:** `current → releases/<tag>` enables atomic cutover during upgrades. If the new release fails, the symlink reverts to the previous directory.
- **Shared data:** Database, cache, and broker data lives outside release directories so it survives upgrades.
- **Mode 0600 on .env:** Secrets file readable only by the hdim user, preventing credential exposure.

---

## 3. Profile Comparison

| Profile | Services | Containers | RAM | CPU | Disk | Use Case |
|---------|----------|------------|-----|-----|------|----------|
| **light** | Infra only | ~3 | 8 GB | 4 | 20 GB | Development, testing |
| **core** | 12 services + 4 gateways | ~16 | 16 GB | 4 | 40 GB | Pilot deployment, single tenant |
| **ai** | Core + AI/ML | ~20 | 24 GB | 8 | 50 GB | AI-assisted clinical workflows |
| **analytics** | Core + reporting | ~22 | 24 GB | 8 | 50 GB | Quality measure reporting |
| **full** | All 51+ services | ~55 | 32 GB+ | 8+ | 60 GB+ | Multi-tenant production |

### Core Profile Services

| Category | Services |
|----------|----------|
| **Gateways** | gateway-service, clinical-gateway, fhir-gateway, admin-gateway |
| **Clinical** | patient-service, care-gap-service, quality-measure-service |
| **Data** | fhir-service, cql-engine-service |
| **Platform** | auth-service, notification-service, audit-service |
| **Infrastructure** | PostgreSQL 16, Redis 7, Kafka 3 |

---

## 4. Network Architecture

### Port Exposure

```
                    ┌─────────────────────┐
   Internet/LAN     │   RHEL 7 Host       │
   ─────────────►   │                     │
   :80, :443        │   ┌─────────────┐   │
   :8080            │   │ Docker Net  │   │
   :4200            │   │             │   │
                    │   │ :5435 PG    │   │  ← NOT exposed externally
                    │   │ :6380 Redis │   │  ← NOT exposed externally
                    │   │ :9094 Kafka │   │  ← NOT exposed externally
                    │   │ :8080 GW    │   │  ← Exposed (API entry)
                    │   │ :4200 UI    │   │  ← Exposed (Clinical Portal)
                    │   │ :8081-8099  │   │  ← docker-compose.prod.yml
                    │   └─────────────┘   │     binds to 127.0.0.1 only
                    └─────────────────────┘
```

### docker-compose.prod.yml Security Hardening

The production overlay (`docker-compose.prod.yml`) rebinds internal service ports to `127.0.0.1`, preventing external access:

```yaml
# Example from docker-compose.prod.yml
services:
  postgres:
    ports:
      - "127.0.0.1:5435:5432"  # Localhost only
  redis:
    ports:
      - "127.0.0.1:6380:6379"  # Localhost only
```

---

## 5. Secrets Management

### Generated Secrets

| Secret | Purpose | Length |
|--------|---------|--------|
| `POSTGRES_PASSWORD` | Database authentication | 32 bytes (base64) |
| `REDIS_PASSWORD` | Cache authentication | 32 bytes |
| `JWT_SECRET` | JWT token signing | 64 bytes |
| `GATEWAY_AUTH_SIGNING_SECRET` | Gateway-to-service HMAC | 32 bytes |
| `AUDIT_ENCRYPTION_KEY` | Audit log encryption | 32 bytes |
| `GRAFANA_ADMIN_PASSWORD` | Monitoring dashboard | 32 bytes |

### Generation Method

- `openssl rand -base64 <bytes>` — cryptographically secure
- Generated once during install, displayed to operator terminal
- Stored in `.env` file with mode `0600`, owned by `hdim:hdim`
- Production hardening: `GATEWAY_AUTH_DEV_MODE=false`

### HashiCorp Vault Integration (Optional)

For organizations using Vault, replace the `.env` file with Vault agent integration:

1. Install Vault agent on the RHEL 7 host
2. Configure Vault agent template to render `.env` from Vault secrets
3. Update systemd unit `ExecStartPre` to refresh secrets before startup

---

## 6. HIPAA Controls

The HDIM platform implements the following HIPAA Technical Safeguards. The installer configures the infrastructure layer; application-layer controls are built into the HDIM software.

| HIPAA Requirement | Section | Implementation |
|-------------------|---------|----------------|
| **Access Control** | §164.312(a)(1) | JWT authentication, RBAC (5 roles), multi-tenant isolation |
| **Automatic Logoff** | §164.312(a)(2)(iii) | 15-minute session timeout with audit logging |
| **Audit Controls** | §164.312(b) | HTTP Audit Interceptor (100% API coverage), audit-service |
| **Integrity Controls** | §164.312(c)(1) | SHA256 checksum on JDK, database migrations via Liquibase |
| **Transmission Security** | §164.312(e)(1) | TLS support (customer-provided certs), Docker network isolation |
| **PHI Cache Controls** | §164.306 | Cache TTL ≤ 5 minutes, no-store headers |
| **Encryption at Rest** | §164.312(a)(2)(iv) | Audit log encryption (AUDIT_ENCRYPTION_KEY) |
| **Log Integrity** | §164.312(b) | Logrotate with compression, journald integration |
| **Time Accuracy** | §164.312(b) | NTP synchronization verified during pre-flight |

### Infrastructure-Level Controls (Installer-Managed)

- `.env` file mode `0600` — secrets not world-readable
- Docker log rotation — prevents disk exhaustion from log growth
- SELinux context labeling — container filesystem isolation
- Firewall rules — internal ports not externally accessible
- `GATEWAY_AUTH_DEV_MODE=false` — production authentication enforced

---

## 7. RHEL 7 Constraints

RHEL 7 reaches End of Extended Life Support in June 2028. The installer accounts for these platform constraints:

| Constraint | Impact | Mitigation |
|------------|--------|------------|
| **Kernel 3.10** | Docker overlay2 requires `d_type=true` on XFS | Pre-flight checks kernel version |
| **No Java 21 in repos** | `java-21-openjdk` not available via yum | Bundled Temurin 21 tarball |
| **iptables-legacy** | No nftables support | Installer uses iptables or firewalld |
| **glibc 2.17** | Older than some modern binary requirements | Temurin 21 supports glibc 2.17+ |
| **systemd 219** | Older systemd feature set | Unit file uses compatible directives only |
| **Python 2.7 default** | Python 3 available via SCL or IUS | Node.js used for frontend (Python optional) |
| **EOL June 2028** | No further security patches after EOL | Plan migration to RHEL 8/9 |

### Compatibility Testing

The installer pre-flight validates:
- Kernel ≥ 3.10.0-1062 (overlay2 + Docker compatibility)
- glibc ≥ 2.17 (Temurin JDK requirement)
- SELinux mode (configures contexts if enforcing)
- ulimits (Kafka requires ≥ 65536 open files)

---

## 8. Upgrade & Rollback Architecture

### Upgrade Flow

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ pg_dump  │───►│ New      │───►│ Build    │───►│ Symlink  │───►│ Smoke    │
│ backup   │    │ release  │    │ services │    │ swap +   │    │ tests    │
│          │    │ dir      │    │          │    │ restart  │    │          │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └────┬─────┘
                                                                     │
                                                        ┌────────────┤
                                                        │ PASS       │ FAIL
                                                        ▼            ▼
                                                   ┌─────────┐ ┌──────────┐
                                                   │ Success  │ │ Auto-    │
                                                   │          │ │ rollback │
                                                   └─────────┘ └──────────┘
```

### Rollback Flow

```
┌──────────┐    ┌──────────┐    ┌───────────┐    ┌──────────┐
│ Stop     │───►│ Restore  │───►│ Optional  │───►│ Restart  │
│ services │    │ symlink  │    │ pg_restore │   │ + verify │
└──────────┘    └──────────┘    └───────────┘    └──────────┘
```

### Atomic Cutover

The symlink swap (`ln -sfn`) is an atomic filesystem operation on Linux. This means the `current` symlink always points to a valid release directory — there is no window where it points to an incomplete state.

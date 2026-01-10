---
name: hdim-memory
description: Access HDIM Serena memories (HIPAA, auth, patterns, troubleshooting)
category: hdim
---

# HDIM Memory Access Command

Quickly access HDIM Serena memories for development guidance.

## Usage

```
/hdim-memory <memory-name>
```

**Available Memories**:
- `hipaa` - HIPAA compliance checklist
- `auth` - Gateway trust authentication guide
- `entity` - Entity-migration synchronization guide
- `patterns` - Common coding patterns
- `services` - Service registry (28 services)
- `architecture` - System architecture overview
- `troubleshooting` - Common issues and solutions

## Examples

```bash
# View HIPAA compliance checklist
/hdim-memory hipaa

# View authentication patterns
/hdim-memory auth

# View common coding patterns
/hdim-memory patterns

# List all available memories
/hdim-memory list
```

## Implementation

```bash
#!/bin/bash

MEMORY=${1:-list}

case "$MEMORY" in
  hipaa)
    cat .serena/memories/hipaa-compliance-checklist.md
    ;;
  auth)
    cat .serena/memories/gateway-trust-auth.md
    ;;
  entity)
    cat .serena/memories/entity-migration-sync.md
    ;;
  patterns)
    cat .serena/memories/common-patterns.md
    ;;
  services)
    cat .serena/memories/service-registry.md
    ;;
  architecture)
    cat .serena/memories/architecture-overview.md
    ;;
  troubleshooting)
    cat .serena/memories/troubleshooting-guide.md
    ;;
  list)
    echo "📚 Available HDIM Memories:"
    echo ""
    echo "  hipaa           - HIPAA compliance checklist (9.5K)"
    echo "  auth            - Gateway trust authentication (13K)"
    echo "  entity          - Entity-migration sync (15K)"
    echo "  patterns        - Common coding patterns (21K)"
    echo "  services        - Service registry (13K)"
    echo "  architecture    - System architecture (5.2K)"
    echo "  troubleshooting - Common issues (13K)"
    echo ""
    echo "Usage: /hdim-memory <memory-name>"
    ;;
  *)
    echo "Unknown memory: $MEMORY"
    echo "Run '/hdim-memory list' to see available memories"
    exit 1
    ;;
esac
```

## Memory Contents

### HIPAA Compliance Checklist
- Cache TTL requirements (≤ 5 minutes)
- HTTP cache headers for PHI
- Audit logging patterns
- Multi-tenant isolation
- Pre-commit checklist

### Gateway Trust Authentication
- Authentication flow diagrams
- Header injection details
- Filter configuration examples
- HMAC signing for production
- Migration checklist

### Entity-Migration Sync
- Column type mapping reference
- Entity pattern examples
- Migration file templates
- Validation testing
- Troubleshooting guide

### Common Patterns
- Controller pattern (with RBAC)
- Service pattern (with caching)
- Repository pattern (multi-tenant)
- Entity pattern
- Testing patterns

### Service Registry
- Complete catalog of 28 services
- Port numbers and context paths
- Dependencies map
- Build commands
- Health check endpoints

### Architecture Overview
- Tech stack summary
- Request flow diagrams
- Core services overview
- Infrastructure components
- Build status

### Troubleshooting Guide
- Build & compilation issues
- Docker & container problems
- Authentication debugging
- Database issues
- Performance optimization

## When to Use

- Before implementing security features (use `auth`)
- Before committing PHI code (use `hipaa`)
- Before modifying entities (use `entity`)
- When writing new controllers (use `patterns`)
- When looking up service ports (use `services`)
- When debugging issues (use `troubleshooting`)

## Related Commands

- `/hdim-validate` - Run validation checks
- `/hdim-service-create` - Create new service
- `/dev:implement` - Implement features with guidance

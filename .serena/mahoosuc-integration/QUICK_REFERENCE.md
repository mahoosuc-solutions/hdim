# HDIM + Mahoosuc Quick Reference Card

## Installation (One-Time Setup)

```bash
# Install HDIM commands
mkdir -p ~/.claude/commands/hdim
cp .serena/mahoosuc-integration/commands/*.md ~/.claude/commands/hdim/

# Install HDIM skills
mkdir -p ~/.claude/skills/hdim
cp .serena/mahoosuc-integration/skills/*.md ~/.claude/skills/hdim/
```

---

## HDIM Commands (4)

| Command | Purpose | Example |
|---------|---------|---------|
| `/hdim-validate` | Run validation checks | `/hdim-validate hipaa` |
| `/hdim-service-create` | Create new service | `/hdim-service-create billing-service 8102` |
| `/hdim-memory` | Access Serena memories | `/hdim-memory patterns` |
| `/hdim-service` | Manage services | `/hdim-service start` |

---

## Validation Checks

```bash
/hdim-validate hipaa      # HIPAA compliance
/hdim-validate tenant     # Multi-tenant isolation
/hdim-validate entity     # Entity-migration sync
/hdim-validate health     # Service health
/hdim-validate all        # All checks (pre-commit)
```

---

## Serena Memories

```bash
/hdim-memory hipaa           # HIPAA compliance (9.5K)
/hdim-memory auth            # Gateway trust auth (13K)
/hdim-memory entity          # Entity-migration (15K)
/hdim-memory patterns        # Coding patterns (21K)
/hdim-memory services        # Service registry (13K)
/hdim-memory architecture    # System overview (5.2K)
/hdim-memory troubleshooting # Common issues (13K)
/hdim-memory list            # List all memories
```

---

## Service Management

```bash
/hdim-service start [name]     # Start service(s)
/hdim-service stop [name]      # Stop service(s)
/hdim-service restart [name]   # Restart service(s)
/hdim-service logs <name>      # View logs
/hdim-service health           # Check health
/hdim-service list             # List services
```

---

## Daily Workflow

### Morning Startup
```bash
/hdim-service start
/hdim-service health
```

### Before Coding
```bash
/hdim-memory <relevant-topic>
```

### Before Committing
```bash
/hdim-validate all
```

### End of Day
```bash
/hdim-service stop
```

---

## Common Tasks

### Implement New Feature
```bash
1. /hdim-memory patterns
2. Code with patterns
3. /hdim-validate all
4. /commit
```

### Create New Service
```bash
1. /hdim-service-create <name> <port>
2. Add to docker-compose.yml
3. /hdim-service start <name>
4. /hdim-service health
```

### Debug Service
```bash
1. /hdim-service logs <name>
2. /hdim-memory troubleshooting
3. /hdim-service restart <name>
```

### Work with PHI Data
```bash
1. /hdim-memory hipaa
2. Implement with guidance
3. /hdim-validate hipaa
```

### Modify Database
```bash
1. /hdim-memory entity
2. Update entity + create migration
3. /hdim-validate entity
```

---

## HDIM Services

| Service | Port | Context |
|---------|------|---------|
| gateway-service | 8001 | `/` |
| cql-engine-service | 8081 | `/cql-engine` |
| patient-service | 8084 | `/patient` |
| fhir-service | 8085 | `/fhir` |
| care-gap-service | 8086 | `/care-gap` |
| quality-measure-service | 8087 | `/quality-measure` |

---

## Key Patterns

### Controller Pattern
```java
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Validated
public class ResourceController {
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    public ResponseEntity<ResourceResponse> get(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // Implementation with PHI headers
    }
}
```

### Repository Pattern
```java
@Query("SELECT r FROM Resource r WHERE r.id = :id AND r.tenantId = :tenantId")
Optional<Resource> findByIdAndTenant(
    @Param("id") String id,
    @Param("tenantId") String tenantId
);
```

---

## Compliance Checklist

### Before Committing PHI Code
- [ ] Cache TTL ≤ 5 minutes
- [ ] Cache-Control headers
- [ ] @Audited annotation
- [ ] No PHI in logs
- [ ] `/hdim-validate hipaa` passes

### Before Committing Queries
- [ ] ALL queries filter by tenantId
- [ ] Repository methods use AndTenant
- [ ] `/hdim-validate tenant` passes

### Before Committing Entities
- [ ] Entity matches migration
- [ ] ddl-auto: validate
- [ ] `/hdim-validate entity` passes

---

## Troubleshooting

### Validation Failed
```bash
# Check specific validation
/hdim-validate hipaa
/hdim-validate tenant
/hdim-validate entity

# View guidance
/hdim-memory <relevant-topic>
```

### Service Not Starting
```bash
/hdim-service logs <service-name>
/hdim-memory troubleshooting
```

### Authentication Issues
```bash
/hdim-memory auth
/hdim-service logs gateway-service
```

---

## Combining with Mahoosuc

| Task | Mahoosuc | HDIM |
|------|----------|------|
| General dev | `/dev:implement` | `/hdim-memory patterns` |
| Testing | `/testing:*` | `/hdim-validate` |
| Commit | `/commit` | `/hdim-validate all` first |
| Documentation | `/content:*` | `/hdim-memory` for code |
| Deployment | `/devops:*` | `/hdim-service health` |

---

## Get Help

```bash
# View memory list
/hdim-memory list

# View troubleshooting
/hdim-memory troubleshooting

# Check service status
/hdim-service health

# Run all validations
/hdim-validate all
```

---

## File Locations

| Resource | Location |
|----------|----------|
| Serena Memories | `.serena/memories/` |
| Validation Tools | `.serena/tools/` |
| Workflows | `.serena/workflows/` |
| Service Configs | `backend/modules/services/[service]/.serena/` |
| Interactive Menu | `.serena/hdim-tools.sh` |
| Mahoosuc Commands | `~/.claude/commands/hdim/` |
| Mahoosuc Skills | `~/.claude/skills/hdim/` |

---

**Keep this card handy for quick reference during HDIM development!**

*Print-friendly version - Save or print for desk reference*

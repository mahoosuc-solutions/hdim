# Mahoosuc Operating System + Serena Integration Guide

## Overview

This guide explains how to integrate HDIM's Serena configuration with your Mahoosuc Operating System (283 commands + 4 skills) for a unified development environment.

---

## Integration Architecture

```
Mahoosuc Operating System (Global)
  ├── 283 Slash Commands
  ├── 4 Skills (brand-voice, content-optimizer, stripe-revenue-analyzer, vercel-landing-page-builder)
  └── Global CLAUDE.md

                    +

HDIM Serena Configuration (Project-Specific)
  ├── 7 Memory Modules (90K knowledge base)
  ├── 6 Service Configurations
  ├── 4 Validation Tools
  ├── 2 Automation Workflows
  └── HDIM-specific Commands & Skills

                    ↓

Unified Development Environment
  - Access to all Mahoosuc commands globally
  - HDIM-specific commands for healthcare development
  - Serena memories for instant knowledge access
  - Automated validation and compliance checking
```

---

## Installation

### Step 1: Install HDIM Commands to Mahoosuc

Copy HDIM commands to your global Mahoosuc commands directory:

```bash
# Create HDIM command directory
mkdir -p ~/.claude/commands/hdim

# Copy HDIM commands
cp .serena/mahoosuc-integration/commands/*.md ~/.claude/commands/hdim/

# Verify installation
ls ~/.claude/commands/hdim/
```

**Installed Commands**:
- `hdim-validate.md` - Validation checks
- `hdim-service-create.md` - Create new service
- `hdim-memory.md` - Access Serena memories
- `hdim-service.md` - Service management

### Step 2: Install HDIM Skills to Mahoosuc

Copy HDIM skills to your global Mahoosuc skills directory:

```bash
# Create HDIM skill directory
mkdir -p ~/.claude/skills/hdim

# Copy HDIM skills
cp .serena/mahoosuc-integration/skills/*.md ~/.claude/skills/hdim/

# Verify installation
ls ~/.claude/skills/hdim/
```

**Installed Skills**:
- `hdim-dev.md` - HDIM development workflow with Serena integration

### Step 3: Update Global CLAUDE.md (Optional)

Add HDIM project reference to your global CLAUDE.md:

```bash
# Edit global CLAUDE.md
nano ~/.claude/CLAUDE.md
```

Add this section:

```markdown
## HDIM Project

When working in the HDIM project (`/mnt/wd-black/dev/projects/hdim-master`):

### Available HDIM Commands
- `/hdim-validate` - Run validation checks (HIPAA, multi-tenant, entity-migration)
- `/hdim-service-create` - Scaffold new microservice
- `/hdim-memory` - Access Serena memories
- `/hdim-service` - Manage services (start, stop, logs, health)

### Available HDIM Skills
- `hdim-dev` - HDIM development workflow with compliance guidance

### Serena Memories
Access via `/hdim-memory <name>`:
- hipaa, auth, entity, patterns, services, architecture, troubleshooting

### Project Guidelines
- HIPAA compliance is MANDATORY
- Gateway trust authentication (NO JWT re-validation in services)
- Multi-tenant isolation (ALL queries filter by tenantId)
- Entity-migration sync (JPA ↔ Liquibase, ddl-auto: validate)

See: `/mnt/wd-black/dev/projects/hdim-master/CLAUDE.md` for complete guidelines
```

---

## Command Reference

### HDIM Commands (4 total)

#### 1. /hdim-validate

Run validation checks for HDIM compliance.

```bash
# Run all checks
/hdim-validate

# Run specific check
/hdim-validate hipaa
/hdim-validate tenant
/hdim-validate entity
/hdim-validate health
```

**Use Cases**:
- Before committing code
- After security changes
- Daily development workflow
- CI/CD pipeline integration

#### 2. /hdim-service-create

Scaffold a new HDIM microservice.

```bash
/hdim-service-create prescription-service 8091
```

**Creates**:
- Complete directory structure
- Spring Boot configuration
- Security setup (Gateway Trust)
- Entity-migration validation test
- Liquibase setup

#### 3. /hdim-memory

Access Serena memories for development guidance.

```bash
# View specific memory
/hdim-memory hipaa
/hdim-memory auth
/hdim-memory patterns

# List all memories
/hdim-memory list
```

**Available Memories**:
- `hipaa` - HIPAA compliance (9.5K)
- `auth` - Gateway trust auth (13K)
- `entity` - Entity-migration sync (15K)
- `patterns` - Coding patterns (21K)
- `services` - Service registry (13K)
- `architecture` - System architecture (5.2K)
- `troubleshooting` - Common issues (13K)

#### 4. /hdim-service

Manage HDIM services.

```bash
# Start/stop services
/hdim-service start
/hdim-service start quality-measure-service
/hdim-service stop

# View logs
/hdim-service logs quality-measure-service

# Check health
/hdim-service health

# List services
/hdim-service list
```

---

## Skills Reference

### hdim-dev Skill

Comprehensive HDIM development workflow with Serena memory integration.

**When to Use**:
- Implementing new HDIM features
- Working with PHI data
- Creating services or controllers
- Modifying database entities
- Debugging authentication

**What It Provides**:
- Automatic reference to relevant Serena memories
- Compliance checklists (HIPAA, multi-tenant)
- Code pattern guidance
- Validation integration
- Pre-commit checks

**Example Invocation**:
```
I need to add a new endpoint to retrieve patient prescriptions with HIPAA compliance.
```

The skill will:
1. Reference HIPAA compliance checklist
2. Provide controller pattern
3. Ensure multi-tenant filtering
4. Add cache-control headers
5. Include audit logging
6. Validate implementation

---

## Workflow Integration

### Daily Development Workflow

Combining Mahoosuc commands with HDIM tools:

```bash
# Morning: Start services
/hdim-service start
/hdim-service health

# Development: Access guidance
/hdim-memory patterns  # View coding patterns

# Implementation: Use general dev commands
/dev:implement  # Mahoosuc command for implementation

# Validation: Run HDIM-specific checks
/hdim-validate hipaa
/hdim-validate tenant

# Commit: Use Mahoosuc commit with HDIM validation
/hdim-validate all && /commit
```

### Feature Implementation Workflow

```bash
# 1. Plan feature
/dev:implement <feature-description>

# 2. Reference HDIM patterns
/hdim-memory patterns

# 3. Check service dependencies
/hdim-memory services

# 4. Implement with HIPAA guidance
/hdim-memory hipaa

# 5. Validate before commit
/hdim-validate all

# 6. Commit with validation
/commit
```

### New Service Creation Workflow

```bash
# 1. Create service scaffold
/hdim-service-create billing-service 8102

# 2. Review architecture
/hdim-memory architecture

# 3. Implement following patterns
/hdim-memory patterns

# 4. Add to docker-compose.yml manually

# 5. Validate
/hdim-validate entity
/hdim-validate tenant

# 6. Start and test
/hdim-service start billing-service
/hdim-service logs billing-service
```

---

## Combining Mahoosuc + Serena Features

### Content Generation + HDIM Validation

```bash
# Use Mahoosuc content skills
/content:write-docs <documentation-topic>

# Validate code in docs
/hdim-validate hipaa  # If docs include code samples
```

### Financial Analysis + Service Metrics

```bash
# Use Stripe revenue analyzer (Mahoosuc skill)
stripe-revenue-analyzer

# Check service health for correlation
/hdim-service health
```

### Social Media + HDIM Announcements

```bash
# Use content optimizer (Mahoosuc skill)
content-optimizer

# Reference HDIM features
/hdim-memory services  # Get service details for content
```

---

## Best Practices

### 1. Use HDIM Commands for HDIM-Specific Tasks

**HDIM-Specific**:
- `/hdim-validate` - HDIM compliance checks
- `/hdim-service-create` - HDIM service scaffolding
- `/hdim-memory` - HDIM knowledge access

**General Development** (use Mahoosuc):
- `/dev:implement` - Feature implementation
- `/testing:*` - Testing automation
- `/commit` - Git commits
- `/cicd:*` - CI/CD setup

### 2. Leverage Serena Memories Before Coding

Before writing HDIM code:
1. Check relevant memory: `/hdim-memory <topic>`
2. Review patterns
3. Understand compliance requirements
4. Implement with guidance

### 3. Always Validate Before Committing

```bash
# Complete validation workflow
/hdim-validate all

# Or specific validations
/hdim-validate hipaa
/hdim-validate tenant
/hdim-validate entity
```

### 4. Use hdim-dev Skill for Guided Development

Instead of asking "how do I...", invoke the skill:

```
Use hdim-dev skill:

I need to implement a new patient search endpoint with HIPAA compliance and multi-tenant isolation.
```

The skill provides context-aware guidance with Serena memories.

---

## Quick Reference

### When to Use What

| Task | Use |
|------|-----|
| HIPAA validation | `/hdim-validate hipaa` |
| View coding patterns | `/hdim-memory patterns` |
| Create new service | `/hdim-service-create <name> <port>` |
| Start services | `/hdim-service start` |
| Service health check | `/hdim-service health` |
| Pre-commit validation | `/hdim-validate all` |
| Feature implementation | `/dev:implement` + `/hdim-memory patterns` |
| Testing | `/testing:*` (Mahoosuc) + `/hdim-validate` |
| Git commit | `/hdim-validate all` + `/commit` |
| Documentation | `/content:*` (Mahoosuc) |
| Financial analysis | `stripe-revenue-analyzer` (Mahoosuc) |

### Memory Quick Access

| Topic | Command |
|-------|---------|
| HIPAA compliance | `/hdim-memory hipaa` |
| Authentication | `/hdim-memory auth` |
| Database changes | `/hdim-memory entity` |
| Code patterns | `/hdim-memory patterns` |
| Service info | `/hdim-memory services` |
| Architecture | `/hdim-memory architecture` |
| Troubleshooting | `/hdim-memory troubleshooting` |

---

## Examples

### Example 1: Implement Patient Care Gap Endpoint

**Using Mahoosuc + HDIM**:

```bash
# 1. Start with Mahoosuc dev command
/dev:implement "Create endpoint to retrieve patient care gaps"

# 2. Reference HDIM patterns
/hdim-memory patterns  # Get controller/service patterns

# 3. Check HIPAA requirements
/hdim-memory hipaa  # Understand PHI handling

# 4. Check multi-tenant requirements
/hdim-memory auth  # Understand tenant isolation

# 5. Implement code with patterns

# 6. Validate implementation
/hdim-validate hipaa
/hdim-validate tenant

# 7. Run tests (Mahoosuc)
/testing:run-unit-tests

# 8. Commit
/hdim-validate all && /commit
```

### Example 2: Create New Prescription Service

**Using HDIM Commands**:

```bash
# 1. Create service scaffold
/hdim-service-create prescription-service 8091

# 2. Review service structure
/hdim-memory services

# 3. Check architecture
/hdim-memory architecture

# 4. Start service
/hdim-service start prescription-service

# 5. Check health
/hdim-service health

# 6. View logs
/hdim-service logs prescription-service

# 7. Validate
/hdim-validate all
```

### Example 3: Debug Authentication Issue

**Using Both Systems**:

```bash
# 1. Check service health
/hdim-service health

# 2. View logs
/hdim-service logs gateway-service

# 3. Reference auth guide
/hdim-memory auth

# 4. Check troubleshooting guide
/hdim-memory troubleshooting

# 5. Test with curl (use Mahoosuc if needed)
# Use existing Mahoosuc commands for API testing

# 6. Fix and validate
/hdim-validate all
```

---

## Advanced Integration

### Custom Mahoosuc Commands for HDIM

You can create additional Mahoosuc commands that leverage Serena:

**Example**: `~/.claude/commands/hdim/hdim-deploy.md`

```markdown
---
name: hdim-deploy
description: Deploy HDIM to staging with validation
category: hdim
---

# HDIM Deploy Command

Validates and deploys HDIM to staging environment.

## Implementation

#!/bin/bash
# Run all validations
bash .serena/workflows/pre-commit-check.sh || exit 1

# Deploy
kubectl apply -f k8s/staging/

# Verify
/hdim-service health
```

### Custom Skills for HDIM

Create skills that combine Mahoosuc capabilities with HDIM:

**Example**: `~/.claude/skills/hdim/hdim-onboarding.md`

```markdown
---
name: hdim-onboarding
description: Onboard new developer to HDIM
category: hdim
---

# HDIM Onboarding Skill

Guide new developers through HDIM setup and patterns.

Steps:
1. Introduce HDIM architecture (/hdim-memory architecture)
2. Explain HIPAA requirements (/hdim-memory hipaa)
3. Show authentication pattern (/hdim-memory auth)
4. Demonstrate service creation (/hdim-service-create)
5. Walk through validation (/hdim-validate)
```

---

## Troubleshooting Integration

### Commands Not Found

If HDIM commands aren't recognized:

```bash
# Verify installation
ls ~/.claude/commands/hdim/

# Reinstall if needed
cp .serena/mahoosuc-integration/commands/*.md ~/.claude/commands/hdim/
```

### Serena Memories Not Accessible

If `/hdim-memory` can't find memories:

```bash
# Check paths are correct in command implementation
cat ~/.claude/commands/hdim/hdim-memory.md

# Ensure you're in HDIM project directory
cd /mnt/wd-black/dev/projects/hdim-master
```

### Validation Tools Not Working

```bash
# Ensure scripts are executable
chmod +x .serena/tools/*.sh
chmod +x .serena/workflows/*.sh

# Test directly
bash .serena/tools/check-hipaa-compliance.sh
```

---

## Summary

### What You Get

**From Mahoosuc** (Global):
- ✅ 283 commands for general development
- ✅ 4 skills for content, analytics, deployment
- ✅ Cross-tool automation
- ✅ Business workflows

**From Serena** (HDIM-Specific):
- ✅ 7 memory modules (90K knowledge)
- ✅ 6 service configurations
- ✅ 4 validation tools
- ✅ 2 automation workflows
- ✅ HDIM-specific commands
- ✅ Healthcare development guidance

**Combined**:
- ✅ Unified development environment
- ✅ Automated HIPAA compliance
- ✅ Rapid service creation
- ✅ Knowledge at your fingertips
- ✅ Best of both worlds

---

*Last Updated: January 10, 2026*
*Mahoosuc + Serena Integration for HDIM*

# v2.9.0 Release Documentation Implementation Plan

> **Status:** COMPLETE

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Update 7 stale files and create a validation script so v2.9.0 is fully documented before push.

**Architecture:** TDD validation-first — Task 1 writes a bash validation script with 16 grep assertions that all fail initially. Tasks 2-6 edit files in parallel (5 independent agents) until all assertions pass. Task 7 runs verification. Task 8 commits and pushes.

**Tech Stack:** Bash (validation script), Markdown (7 docs/config files), Git

---

### Task 1: Write the Failing Validation Script

**Files:**
- Create: `scripts/release-readiness-check.sh`

**Step 1: Write the validation script**

```bash
#!/usr/bin/env bash
# Release Readiness Check for v2.9.0
# Validates that all documentation and config files are current.
# Usage: bash scripts/release-readiness-check.sh

set -euo pipefail

PASS=0
FAIL=0
ERRORS=()

check() {
  local desc="$1" file="$2" pattern="$3"
  if grep -qP "$pattern" "$file" 2>/dev/null; then
    printf "  PASS  %s\n" "$desc"
    ((PASS++))
  else
    printf "  FAIL  %s\n" "$desc"
    ERRORS+=("$desc ($file)")
    ((FAIL++))
  fi
}

echo "=== v2.9.0 Release Readiness Check ==="
echo ""

# --- docs/README.md (3 assertions) ---
echo "[docs/README.md]"
check "Timestamp updated to March 2026" \
  "docs/README.md" "March [0-9]+, 2026"
check "MCP Edge mentioned in body" \
  "docs/README.md" "MCP Edge"
check "Link to MCP Edge doc" \
  "docs/README.md" "mcp.*edge.*design|MCP_EDGE"

# --- docs/services/SERVICE_CATALOG.md (4 assertions) ---
echo ""
echo "[docs/services/SERVICE_CATALOG.md]"
check "MCP Edge section header" \
  "docs/services/SERVICE_CATALOG.md" "## MCP Edge"
check "mcp-edge-platform with port 3100" \
  "docs/services/SERVICE_CATALOG.md" "mcp-edge-platform.*3100"
check "mcp-edge-devops with port 3200" \
  "docs/services/SERVICE_CATALOG.md" "mcp-edge-devops.*3200"
check "Service count updated to 60" \
  "docs/services/SERVICE_CATALOG.md" "60 microservices"

# --- .github/CODEOWNERS (2 assertions) ---
echo ""
echo "[.github/CODEOWNERS]"
check "MCP Edge path ownership" \
  ".github/CODEOWNERS" "mcp-edge"
check "MCP Edge docker-compose ownership" \
  ".github/CODEOWNERS" "docker-compose\.mcp-edge"

# --- CONTRIBUTING.md (1 assertion) ---
echo ""
echo "[CONTRIBUTING.md]"
check "MCP Edge test command documented" \
  "CONTRIBUTING.md" "test:mcp-edge"

# --- .github/pull_request_template.md (1 assertion) ---
echo ""
echo "[.github/pull_request_template.md]"
check "MCP Edge validation checkbox" \
  ".github/pull_request_template.md" "mcp-edge"

# --- CLAUDE.md (4 assertions) ---
echo ""
echo "[CLAUDE.md]"
check "Timestamp updated to March 5" \
  "CLAUDE.md" "March 5, 2026"
check "MCP Edge port 3100 in ports table" \
  "CLAUDE.md" "3100.*MCP"
check "Version bumped to 4.2" \
  "CLAUDE.md" "Version: 4\.2"
check "v2.9.0 referenced" \
  "CLAUDE.md" "v2\.9\.0"

# --- CHANGELOG.md (1 assertion) ---
echo ""
echo "[CHANGELOG.md]"
check "1307 tests disambiguated as MCP Edge" \
  "CHANGELOG.md" "1,307 MCP Edge"

# --- Summary ---
echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
if [ ${#ERRORS[@]} -gt 0 ]; then
  echo ""
  echo "Failures:"
  for e in "${ERRORS[@]}"; do
    echo "  - $e"
  done
  exit 1
fi
echo "All assertions passed. Release docs are ready."
exit 0
```

**Step 2: Run it to verify all 16 assertions fail**

Run: `bash scripts/release-readiness-check.sh`
Expected: 16 FAIL, exit code 1

**Step 3: Commit the validation script**

```bash
git add scripts/release-readiness-check.sh
git commit -m "test: add v2.9.0 release readiness validation script (16 assertions, all failing)

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 2: Update docs/README.md and docs/services/SERVICE_CATALOG.md

**Files:**
- Modify: `docs/README.md:5` (timestamp), `:52-58` (Architecture section), `:159-164` (Recent Updates)
- Modify: `docs/services/SERVICE_CATALOG.md:3-4` (count+timestamp), `:136-137` (insert MCP Edge section)

**Step 1: Update docs/README.md timestamp (line 5)**

Replace:
```
**Last Updated**: February 3, 2026
```
With:
```
**Last Updated**: March 5, 2026
```

**Step 2: Add MCP Edge to Architecture & Design section (after line 58)**

After the line `- **Database Architecture**: [29 databases, schema management](architecture/database/)`, add:
```
- **MCP Edge Design**: [Claude Desktop/Code integration sidecars](plans/2026-03-04-hdim-mcp-edge-design.md)
```

**Step 3: Update Recent Updates section (line 159-164)**

Replace:
```
## Recent Updates

- **January 19, 2026**: New modular documentation structure with centralized navigation
- **January 12, 2026**: Liquibase Development Workflow guide
- **January 10, 2026**: Database architecture standardization complete
- See [Archive](archive/) for historical documentation
```
With:
```
## Recent Updates

- **March 5, 2026**: v2.9.0 release — MCP Edge Layer v0.1.0 (3 sidecars, 1,307 tests), test coverage improvements (66 new tests across 5 services)
- **March 3, 2026**: v2.8.0 system remediation — security hardening, CI/CD normalization, docs reorganization
- **January 19, 2026**: New modular documentation structure with centralized navigation
- See [Archive](archive/) for historical documentation
```

**Step 4: Update SERVICE_CATALOG.md service count (lines 3-4)**

Replace:
```
**Total Services**: 57 microservices (including event sourcing, AI agents, gateways, and sales services)
```
With:
```
**Total Services**: 60 microservices (including event sourcing, AI agents, gateways, MCP Edge sidecars, and sales services)
```

**Step 5: Update SERVICE_CATALOG.md timestamp (line 4)**

Replace:
```
**Last Updated**: March 3, 2026
```
With:
```
**Last Updated**: March 5, 2026
```

**Step 6: Insert MCP Edge section in SERVICE_CATALOG.md (after line 136)**

After the `devops-agent-service` row and before `---`, insert:

```markdown

---

## MCP Edge Services (v2.9.0)

Claude Desktop/Code integration sidecars providing AI-assisted platform operations via Model Context Protocol. See [MCP Edge Design](../plans/2026-03-04-hdim-mcp-edge-design.md) for architecture.

| Service | Port | Purpose | Tech Stack | Documentation |
|---------|------|---------|-----------|-----------------|
| **mcp-edge-platform** | 3100 | Platform health, FHIR metadata, service catalog (15 tools) | Node.js, Express, Pino | [README](../../mcp-edge-platform/README.md) |
| **mcp-edge-devops** | 3200 | Docker ops, logs, topology, release gates (15 tools) | Node.js, Express, Pino | [README](../../mcp-edge-devops/README.md) |
| **mcp-edge-clinical** | 3300 | Clinical workflows, care gaps, CDS hooks (68+ tools) | Node.js, Express, Pino | [README](../../mcp-edge-clinical/README.md) |

**Key features:** RBAC matrix (7 roles x 108 tools), PHI leak detection, demo mode, stdio bridges for Claude Desktop.
**Tests:** 1,307 across 4 packages (99.35% statement coverage, 98.12% branch coverage).
```

**Step 7: Run validation for this task's assertions**

Run: `bash scripts/release-readiness-check.sh 2>&1 | grep -E "(docs/README|SERVICE_CATALOG)"`
Expected: 7 PASS for these two files

**Step 8: Commit**

```bash
git add docs/README.md docs/services/SERVICE_CATALOG.md
git commit -m "docs: update docs portal and service catalog for MCP Edge v0.1.0

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 3: Update .github/CODEOWNERS and .github/pull_request_template.md

**Files:**
- Modify: `.github/CODEOWNERS:19` (append MCP Edge section)
- Modify: `.github/pull_request_template.md:12` (add checkbox)

**Step 1: Add MCP Edge section to CODEOWNERS (after line 19)**

Append after the landing page section:

```

# MCP Edge sidecar ownership and quality gates
mcp-edge-*/** @webemo-aaron
scripts/mcp/mcp-edge-*.mjs @webemo-aaron
docker-compose.mcp-edge.yml @webemo-aaron
.github/workflows/mcp-edge-ci.yml @webemo-aaron
```

**Step 2: Add MCP Edge checkbox to PR template (after line 12)**

After the landing-page checkbox line, add:
```
- [ ] If `mcp-edge-*` code changed, ran `npm run test:mcp-edge`
```

**Step 3: Run validation for this task's assertions**

Run: `bash scripts/release-readiness-check.sh 2>&1 | grep -E "(CODEOWNERS|pull_request)"`
Expected: 3 PASS

**Step 4: Commit**

```bash
git add .github/CODEOWNERS .github/pull_request_template.md
git commit -m "chore: add MCP Edge to CODEOWNERS and PR template

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 4: Update CONTRIBUTING.md

**Files:**
- Modify: `CONTRIBUTING.md:20-21` (add MCP Edge validation), `:26-29` (add merge gate)

**Step 1: Add MCP Edge validation section (after line 20)**

After the `landing-page-v0` validation line, add:
```
- For `mcp-edge-*/**` changes, run:
  - `npm run test:mcp-edge`
```

**Step 2: Add MCP Edge merge gate (after line 28)**

After the `Landing Page Validation` line, add:
```
- `MCP Edge CI` (for `mcp-edge-*/**` changes)
```

**Step 3: Run validation for this task's assertion**

Run: `bash scripts/release-readiness-check.sh 2>&1 | grep CONTRIBUTING`
Expected: 1 PASS

**Step 4: Commit**

```bash
git add CONTRIBUTING.md
git commit -m "docs: add MCP Edge validation to contributor guide

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 5: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md:125` (Tech Stack — add MCP Edge subsection)
- Modify: `CLAUDE.md:143` (Service Ports table — add 3 rows)
- Modify: `CLAUDE.md:1292-1293` (version and timestamp)

**Step 1: Add MCP Edge to Tech Stack (after line 125)**

After `- **Secrets:** HashiCorp Vault`, add:

```

### MCP Edge (Claude Integration)
- **Runtime:** Node.js 20+
- **Framework:** Express 4.x
- **Protocol:** Model Context Protocol (MCP) over stdio/HTTP
- **Logging:** Pino with PHI scrubbing
```

**Step 2: Add MCP Edge ports to Service Ports table (after line 143)**

After the `| Grafana | 3001 | Dashboard UI |` row, add:

```
| MCP Edge Platform | 3100 | Claude AI platform tools |
| MCP Edge DevOps | 3200 | Claude AI DevOps tools |
| MCP Edge Clinical | 3300 | Claude AI clinical tools |
```

**Step 3: Update version and timestamp (lines 1292-1293)**

Replace:
```
_Last Updated: March 3, 2026_
_Version: 4.1_ - **v2.8.0 System Remediation** (March 2026): Security hardening, landing page SEO/accessibility, CI/CD normalization, docs reorganization. See [docs/phases/](./docs/phases/) for phase summaries, [docs/deployment/CI_CD_BEST_PRACTICES.md](./docs/deployment/CI_CD_BEST_PRACTICES.md) for CI/CD procedures. Infrastructure modernization (Phases 1-7) complete. Platform achievements: ✅ HIPAA Compliance, ✅ OCR Document Processing, ✅ API Documentation, ✅ Test Performance Optimization (Phase 6), ✅ CI/CD Parallelization (Phase 7) - all production-ready.
```
With:
```
_Last Updated: March 5, 2026_
_Version: 4.2_ - **v2.9.0 MCP Edge + Test Coverage** (March 2026): MCP Edge Layer v0.1.0 (3 sidecars, 1,307 tests), 66 new backend tests, system remediation complete. See [docs/phases/](./docs/phases/) for phase summaries, [docs/plans/2026-03-04-hdim-mcp-edge-design.md](./docs/plans/2026-03-04-hdim-mcp-edge-design.md) for MCP Edge architecture. Infrastructure modernization (Phases 1-7) complete. Platform achievements: ✅ HIPAA Compliance, ✅ OCR Document Processing, ✅ API Documentation, ✅ Test Performance Optimization (Phase 6), ✅ CI/CD Parallelization (Phase 7), ✅ MCP Edge Layer (v2.9.0) - all production-ready.
```

**Step 4: Run validation for this task's assertions**

Run: `bash scripts/release-readiness-check.sh 2>&1 | grep CLAUDE`
Expected: 4 PASS

**Step 5: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md to v4.2 with MCP Edge and v2.9.0

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 6: Clarify CHANGELOG.md Test Counts

**Files:**
- Modify: `CHANGELOG.md:20` (disambiguate MCP Edge test count)
- Modify: `CHANGELOG.md:60` (clarify backend test count)

**Step 1: Disambiguate MCP Edge test count (line 20)**

Replace:
```
- **1,307 tests** with 99.35% statement coverage, 98.12% branch coverage
```
With:
```
- **1,307 MCP Edge tests** with 99.35% statement coverage, 98.12% branch coverage
```

**Step 2: Clarify backend test count (line 60)**

Replace:
```
- Test suite: 259 → 709+ unit tests passing
```
With:
```
- Backend test suite: 259 → 709+ unit tests passing (excludes 1,307 MCP Edge tests)
```

**Step 3: Run validation for this task's assertion**

Run: `bash scripts/release-readiness-check.sh 2>&1 | grep CHANGELOG`
Expected: 1 PASS

**Step 4: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs: clarify backend vs MCP Edge test counts in CHANGELOG

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

### Task 7: Run Full Verification

**Files:** None (read-only)

**Step 1: Run the full validation script**

Run: `bash scripts/release-readiness-check.sh`
Expected: `16 passed, 0 failed` and exit code 0

**Step 2: If any assertions fail, fix the specific file and re-run**

The error output tells you exactly which file and assertion failed. Fix and re-run until all 16 pass.

**Step 3: Run git diff to review all changes**

Run: `git diff HEAD~6 --stat`
Expected: 8 files changed (7 docs/config + 1 validation script)

---

### Task 8: Squash, Tag, and Push

**Step 1: Interactive squash of documentation commits into one**

The 6 task commits (Tasks 1-6) should be squashed into a single clean commit for the push. Use soft reset to HEAD~6 (the v2.9.0 tag commit), then recommit:

```bash
git reset --soft HEAD~6
git commit -m "docs: complete v2.9.0 release documentation for MCP Edge

- Update docs portal and service catalog with MCP Edge v0.1.0 (3 sidecars)
- Add MCP Edge to CODEOWNERS, PR template, CONTRIBUTING guide
- Update CLAUDE.md to v4.2 with MCP Edge ports and tech stack
- Clarify backend vs MCP Edge test counts in CHANGELOG
- Add release-readiness validation script (16 assertions)

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

**Step 2: Move the v2.9.0 tag to include documentation**

```bash
git tag -d v2.9.0
git tag -a v2.9.0 -m "Release v2.9.0: MCP Edge Layer v0.1.0, system remediation, test coverage"
```

**Step 3: Push with tags**

```bash
git push origin master --tags
```

**Step 4: Final verification**

```bash
git log --oneline -3
git tag -l 'v2.9*'
bash scripts/release-readiness-check.sh
```

Expected: All green, tag on latest commit, 16/16 assertions pass.

---

## Parallel Execution Map

Tasks 2-6 are fully independent and can run in parallel:

```
Task 1 (validation script) ──────────────────────────┐
                                                       │
    ┌──── Task 2 (README + SERVICE_CATALOG) ────┐     │
    ├──── Task 3 (CODEOWNERS + PR template) ────┤     │
    ├──── Task 4 (CONTRIBUTING.md) ─────────────┤ parallel
    ├──── Task 5 (CLAUDE.md) ───────────────────┤     │
    └──── Task 6 (CHANGELOG.md) ────────────────┘     │
                                                       │
Task 7 (full verification) ───────────────────────────┘
Task 8 (squash + tag + push) ─────────────────────────
```

## Agent Assignment for Swarm Execution

| Agent | Tasks | Est. Time |
|-------|-------|-----------|
| Main thread | Task 1 (write script), Task 7 (verify), Task 8 (squash+push) | 5 min |
| Agent A | Task 2 (docs portal + service catalog) | 3 min |
| Agent B | Task 3 (CODEOWNERS + PR template) | 2 min |
| Agent C | Task 4 (CONTRIBUTING.md) | 2 min |
| Agent D | Task 5 (CLAUDE.md) | 3 min |
| Agent E | Task 6 (CHANGELOG.md) | 1 min |

**Total wall-clock time:** ~10 minutes (Tasks 2-6 run in parallel)

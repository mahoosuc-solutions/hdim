# v2.9.0 Release Documentation Completion — Design

> **Status:** COMPLETE

**Status:** Approved
**Date:** 2026-03-05
**Scope:** 7 files, 16 validation assertions, 1 commit

## Problem

v2.9.0 is committed and tagged but not pushed. Several documentation and config files are stale — they don't reference MCP Edge v0.1.0, the marquee feature of this release. Pushing without updating these creates a confusing first impression for anyone reading the docs portal, service catalog, or contributor guides.

## Approach: TDD Validation-First

Write a release-readiness validation script that defines "fully baked" as executable grep assertions. Then dispatch parallel agents to edit files until all assertions pass. The script becomes a reusable release gate artifact.

## Validation Contract (16 Assertions)

### docs/README.md (3 assertions)
1. Timestamp contains "March 5, 2026" (not "February 3, 2026")
2. Contains "MCP Edge" in body text
3. Contains link to MCP Edge design or architecture doc

### docs/services/SERVICE_CATALOG.md (4 assertions)
4. Contains "MCP Edge" section header
5. Contains "mcp-edge-platform" with "3100"
6. Contains "mcp-edge-devops" with "3200"
7. Service count updated to "60"

### .github/CODEOWNERS (2 assertions)
8. Contains "mcp-edge" path pattern
9. Contains "docker-compose.mcp-edge" path

### CONTRIBUTING.md (1 assertion)
10. Contains "test:mcp-edge" command reference

### .github/pull_request_template.md (1 assertion)
11. Contains "mcp-edge" validation checkbox

### CLAUDE.md (4 assertions)
12. Contains "March 5, 2026" or "2026-03-05"
13. Contains port "3100" in Service Ports table
14. Contains "Version: 4.2"
15. Contains "v2.9.0" reference

### CHANGELOG.md (1 assertion)
16. "1,307" on same line as "MCP Edge" (disambiguated from backend tests)

## Agent Assignments

| Agent | Files | Coupling Reason |
|-------|-------|-----------------|
| 1 | docs/README.md, docs/services/SERVICE_CATALOG.md | Both reference MCP Edge services |
| 2 | .github/CODEOWNERS, .github/pull_request_template.md | Both gate MCP Edge paths |
| 3 | CONTRIBUTING.md | Standalone contributor guide |
| 4 | CLAUDE.md | Standalone, largest change |
| 5 | CHANGELOG.md | Surgical 2-line clarification |

## Verification

After all agents complete, a verification agent runs the validation script and reports pass/fail per assertion. All 16 must pass before commit.

## Commit Strategy

Single commit with all 7 file changes + the validation script. Push with `--tags` to deliver both the v2.9.0 tag and this documentation commit.

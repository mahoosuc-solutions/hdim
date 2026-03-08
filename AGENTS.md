# HDIM Agent Configuration

This repository enforces folder-level hygiene for all user-level AI agents.

## Mandatory Workflow

1. Preflight
- `git status --short --branch`
- `npm run hygiene:audit`

2. Execute task-scoped changes only.

3. Postflight
- `npm run hygiene:audit`
- `git status --short --branch`

## Forbidden Generated Paths

Do not commit or leave dirty generated outputs in:

- `backend/modules/**/bin/main/**`
- `backend/modules/**/bin/test/**`
- `backend/platform/**/bin/main/**`
- `backend/platform/**/bin/test/**`
- `backend/tools/**/bin/main/**`
- `backend/tools/**/bin/test/**`

## Recovery Command

If forbidden dirtiness appears:

- `npm run hygiene:clean`
- `npm run hygiene:audit`

## Source Of Truth

- Standard and runbook: `HYGIENE.md`
- Agent guide: `docs/agents/AGENTS.md`
- Agent capabilities: `docs/agents/SKILLS.md`

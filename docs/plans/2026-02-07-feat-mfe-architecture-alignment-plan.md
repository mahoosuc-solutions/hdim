---
title: "feat: Align MFE architecture and migrate Measure Builder"
type: feat
date: 2026-02-07
---

# feat: Align MFE architecture and migrate Measure Builder

## Summary

We will standardize on `shell-app` as the primary host, migrate Measure Builder into a new MFE, and use shared auth, tenant, and clinical data services to support complex, multi-source measure design and evaluation workflows.

## Repository Research Summary

### Architecture & Structure
- Existing MFE host is `apps/shell-app` using Nx Module Federation. `apps/clinical-portal` is standalone.
- Remotes exist: `mfe-patients`, `mfe-quality`, `mfe-care-gaps`, `mfe-reports`.
- `MICROFRONTEND_QUICK_START.md` documents MFE phases and shared libs.
- Shared libs already exist for cross-MFE orchestration:
- `@health-platform/shared/data-access` includes Clinical 360 pipeline and EventBus.
- `@health-platform/shared/util-auth` and `@health-platform/shared/state` provide auth patterns and NgRx state.

### Implementation Patterns
- MFEs expose `./Routes` via `remote-entry/entry.routes.ts`.
- Shell routes load remotes via `import('mfeX/Routes')`.
- Current remotes are placeholders or partial implementations.

## Institutional Learnings Search Results

### Search Context
- **Feature/Task**: MFE platform alignment and Measure Builder MFE migration.
- **Keywords Used**: mfe, module federation, shell-app, remote-entry, measure builder.
- **Files Scanned**: 0 (no `docs/solutions/` directory in repo).
- **Relevant Matches**: 0.

### Critical Patterns (Always Check)
- No `docs/solutions/patterns/critical-patterns.md` found.

## SpecFlow Analysis Summary

### User Flow Overview
1. User logs into shell-app and selects tenant.
2. User navigates to Measure Builder MFE via shell routes.
3. Measure Builder loads draft measures and existing CQL.
4. User edits CQL and visual algorithm and saves drafts.
5. User evaluates measures against selected patient context (from MFE Patients).
6. User publishes or shares measures across tenants (if authorized).

### Missing Elements & Gaps
- Tenant selection propagation between shell and remote.
- Cross-MFE patient selection propagation into Measure Builder.
- Shared UI layout and navigation parity between host and remote.
- Authorization checks for multi-tenant publish and evaluate flows.
- Offline or partial data behavior for FHIR sources.

### Critical Questions
1. Should tenant switching be allowed inside the Measure Builder MFE or only from the shell?
2. Do measure evaluations run against a single patient or population by default?
3. What should happen when required FHIR data sources are unavailable?

## Proposed Architecture

### Primary Host
- Use `apps/shell-app` as the canonical host for all MFEs.

### New Remote
- Create `apps/mfe-measure-builder` as a remote.
- Expose `./Routes` and mount in shell at `/mfeMeasureBuilder`.

### Shared Contracts
- Use shared auth and tenant state from `@health-platform/shared/util-auth` and `@health-platform/shared/state`.
- Use Clinical 360 pipeline and EventBus for patient context and cross-MFE coordination.
- Establish a shared interface for measure evaluation requests and results.

## Background Agent Workstreams

### Agent A: Architecture Lead
- Validate MFE host decision and shell routing model.
- Define shared layout strategy and navigation consistency.
- Produce migration checklist for existing pages.

### Agent B: Measure Builder Remote
- Create `mfe-measure-builder` remote with Routes.
- Port Measure Builder components and services.
- Wire tenant context and auth through shared libs.
- Add base MFE layout integration.

### Agent C: Shared Data & Event Bus
- Integrate patient selection events from `mfe-patients`.
- Wire measure evaluation requests to Clinical 360 pipeline.
- Define event types for measure builder workflows.

### Agent D: Quality MFE Alignment
- Review `mfe-quality` and identify overlap with Measure Builder.
- Ensure the MFE-quality supports shared evaluation results.
- Plan incremental migration of quality measure dashboards.

### Agent E: Testing & Tooling
- Update E2E to include shell + measure builder remote.
- Add smoke tests for tenant propagation and patient context.
- Validate builds for shell + remote + shared libs.

## Migration Sequence

1. Standardize shell navigation and tenant header propagation.
2. Create Measure Builder remote and mount route in shell.
3. Wire patient selection and tenant context across MFEs.
4. Align quality MFE with shared measure evaluation flows.
5. Remove legacy clinical-portal route for measure builder when remote is stable.

## Acceptance Criteria

- Measure Builder runs as a remote loaded from shell-app.
- Tenant selection in shell drives API requests in Measure Builder.
- Patient selection in mfe-patients updates Measure Builder context.
- Measure evaluation works with multi-source FHIR data and returns results.
- UI layout is no longer cramped and supports complex measure editing.

## Risks

- Duplicate UI styling between host and remote.
- Auth/session drift between shell and remote.
- Performance and bundling regressions due to shared dependency mismatches.

## Success Metrics

- Load time for Measure Builder MFE under 3 seconds on dev machine.
- Successful evaluation of a complex measure using multiple FHIR resources.
- At least one E2E flow covers patient select → measure edit → evaluate.

## Next Steps

- Run the detailed implementation plan and split tasks into execution milestones.
- Confirm tenant switching UX in shell vs remote.

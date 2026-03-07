# Duplicate Content Alignment Report

**Date:** 2026-03-07  
**Scope:** Migration-readiness duplicate review for launch/legal/commercial/doc pack

## 1) Summary

A repository-wide duplicate scan found many repeated documents, but most are
intentional template replication (for customer-type runbook packs, release
versioned docs, and archived validation evidence).

Action was taken only on migration-risk duplicates that could cause ambiguity in
the immediate launch/migration scope.

## 2) Actionable Duplicates Fixed

1. Finance forecast duplicates
   - Removed:
     - `docs/finance/FINANCIAL_FORECAST_2026-2028_CURRENT_STRUCTURE.md`
   - Canonical retained:
     - `docs/finance/CURRENT_STRUCTURE_FINANCIAL_FORECAST_2026-2028.md`

2. GitHub issue template duplicates (underscore vs hyphen naming)
   - Removed:
     - `.github/ISSUE_TEMPLATE/bug_report.md`
     - `.github/ISSUE_TEMPLATE/feature_request.md`
   - Canonical retained:
     - `.github/ISSUE_TEMPLATE/bug-report.md`
     - `.github/ISSUE_TEMPLATE/feature-request.md`
     - `.github/ISSUE_TEMPLATE/config.yml`

## 3) Intentional Duplicate Families (No Change)

These are considered deliberate and should remain:

- Customer-type week-1 runbook packs under
  `docs/runbooks/customer-types/operations/*-week1-execution-pack/`
- Versioned release docs under `docs/releases/v*/`
- Archived validation evidence under `docs/marketing/web/evidence/archive/`
- Role- or context-specific docs with similar names but different audience
  (`docs/user/` vs `docs/users/`, `docs/services/` vs `docs/runbooks/`)

## 4) Migration Cohesion Status (Launch Pack)

- BSL/legal/commercial/pricing docs are aligned and cross-referenced.
- Canonical finance forecast is singular.
- GitHub community templates are singular and cohesive.
- Gitleaks and env-audit evidence docs are present.

## 5) Remaining Non-Content Blockers

The following remaining items are external owner/permission dependencies:

- GitHub org/repo creation permissions under `mahoosuc-solutions`
- Domain and email setup completion
- ROBS-experienced attorney identification


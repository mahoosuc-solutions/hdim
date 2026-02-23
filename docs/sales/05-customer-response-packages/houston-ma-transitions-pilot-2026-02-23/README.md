# Houston MA Transitions Pilot Response Package

Prepared: 2026-02-23
Owner: Solutions Engineering
Audience: Prospective Medicare Advantage customer sponsor and clinical leadership

## Package Contents

1. `01-customer-response-letter.md`
2. `02-phase-1-pilot-sow.md`
3. `03-technical-architecture-and-security-baseline.md`
4. `04-implementation-plan-and-product-deltas.md`
5. `05-estimated-costs-and-commercials.md`
6. `06-sendable-email-draft.md`
7. `07-deployment-models-hosted-vs-onprem.md`
8. `08-stakeholder-messaging-map.md`
9. `09-internal-talk-track-and-objections.md`

## Public Share Pack

Public-safe bundle for web sharing:

1. `public-share-pack/00-publish-manifest.md`
2. `public-share-pack/01-public-summary-page.md`
3. `public-share-pack/02-public-pilot-brief.md`
4. `public-share-pack/03-public-deployment-options.md`
5. `public-share-pack/04-public-publish-checklist.md`

## Purpose

This package converts the discovery conversation into a concrete Phase 1 pilot proposal for a 90-day care transitions program covering 200-500 discharges.

## Current Product Implementation Status

The following implementation has already been started in-platform:

- New patient engagement domain and APIs in `nurse-workflow-service`.
- Gateway and ingress routing for `/nurse-workflow/**`.
- Pilot overlay updates for service deployment.
- Tenant-aware AI controller/service paths in `ai-assistant-service`.

Validation completed:

- `./gradlew :modules:services:ai-assistant-service:test`
- `./gradlew :modules:services:nurse-workflow-service:test`

Both test suites are passing as of 2026-02-23.

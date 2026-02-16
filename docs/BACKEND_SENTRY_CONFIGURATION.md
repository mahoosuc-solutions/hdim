# Backend Sentry Configuration

This document defines the backend Sentry setup used by all Spring services.

## Scope

- Applies to backend service modules through shared Gradle wiring in `backend/build.gradle.kts`
- Uses shared auto-configuration module: `backend/modules/shared/infrastructure/sentry`
- Adds PHI-safe event sanitization before events are sent to Sentry

## Environment Variables

Set these in deployment environments:

- `SENTRY_DSN`
- `SENTRY_ENVIRONMENT`
- `SENTRY_TRACES_SAMPLE_RATE`
- `HDIM_SENTRY_PHI_FILTER_ENABLED` (default: `true`)
- `HDIM_SENTRY_PHI_REDACTION_TEXT` (default: `[REDACTED]`)

## HIPAA/PHI Safety Controls

The shared `BeforeSend` callback redacts sensitive payload data before egress:

- Removes request cookies and query strings
- Replaces request body payload with redaction text
- Redacts non-allowlisted request headers
- Removes user identifiers (`id`, `email`, `ipAddress`, etc.)
- Redacts sensitive keys in tags/extras/breadcrumbs

The callback tags all processed events with `phi_filtered=true` for auditability.

## Validation

Run:

```bash
./backend/gradlew :modules:shared:infrastructure:sentry:test
```

This validates callback behavior for request, user, tags, extras, and breadcrumbs.

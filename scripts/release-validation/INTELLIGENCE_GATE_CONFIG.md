# Intelligence Gate CI Configuration

This document lists GitHub secrets/variables used by intelligence deployment gate checks in `.github/workflows/deploy-docker.yml`.

## Required Secrets (Staging)

- `STAGING_INTELLIGENCE_EVENT_SERVICE_BASE_URL`

## Optional but Recommended Secrets (Staging)

- `STAGING_INTELLIGENCE_TENANT_ID`
- `STAGING_INTELLIGENCE_OTHER_TENANT_ID`
- `STAGING_GATEWAY_VALIDATED_HEADER`
- `STAGING_INTELLIGENCE_REVIEWER_ROLES`
- `STAGING_INTELLIGENCE_VIEWER_ROLES`

## Deep-Check Secrets (Staging)

Required only when deep checks are enabled:

- `STAGING_INTELLIGENCE_DATABASE_URL` (if `STAGING_INTELLIGENCE_CHECK_MIGRATIONS=true`)
- `STAGING_INTELLIGENCE_KAFKA_BOOTSTRAP` (if `STAGING_INTELLIGENCE_CHECK_KAFKA_TOPICS=true`)

## Required Secrets (Production)

- `PRODUCTION_INTELLIGENCE_EVENT_SERVICE_BASE_URL`

## Optional but Recommended Secrets (Production)

- `PRODUCTION_INTELLIGENCE_TENANT_ID`
- `PRODUCTION_INTELLIGENCE_OTHER_TENANT_ID`
- `PRODUCTION_GATEWAY_VALIDATED_HEADER`
- `PRODUCTION_INTELLIGENCE_REVIEWER_ROLES`
- `PRODUCTION_INTELLIGENCE_VIEWER_ROLES`

## Deep-Check Secrets (Production)

Required only when deep checks are enabled:

- `PRODUCTION_INTELLIGENCE_DATABASE_URL` (if `PRODUCTION_INTELLIGENCE_CHECK_MIGRATIONS=true`)
- `PRODUCTION_INTELLIGENCE_KAFKA_BOOTSTRAP` (if `PRODUCTION_INTELLIGENCE_CHECK_KAFKA_TOPICS=true`)

## GitHub Variables

- `STAGING_INTELLIGENCE_CHECK_MIGRATIONS` (`true`/`false`, default `false`)
- `STAGING_INTELLIGENCE_CHECK_KAFKA_TOPICS` (`true`/`false`, default `false`)
- `PRODUCTION_INTELLIGENCE_CHECK_MIGRATIONS` (`true`/`false`, default `false`)
- `PRODUCTION_INTELLIGENCE_CHECK_KAFKA_TOPICS` (`true`/`false`, default `false`)

## Local Validation

You can validate configuration before deployment with:

```bash
EVENT_SERVICE_BASE_URL=http://localhost:8083/events \
CHECK_MIGRATIONS=false \
CHECK_KAFKA_TOPICS=false \
./scripts/release-validation/validate-intelligence-gate-config.sh staging
```

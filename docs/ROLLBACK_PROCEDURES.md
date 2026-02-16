# Rollback Procedures

This runbook defines rollback procedures for deployment incidents.

## Rollback Triggers

- Health checks fail for > 5 minutes after deployment.
- P95 latency degrades by > 50% from baseline.
- Error rate exceeds 5% on critical endpoints.
- Data integrity validation fails.

## Standard Rollback Steps

1. Freeze deployments and announce incident in operations channel.
2. Identify impacted services and current release tag.
3. Re-deploy previous known-good image tags.
4. Re-apply previous config versions for affected services.
5. Verify health, smoke, and key business workflows.
6. Confirm data integrity checks pass.
7. Publish incident summary and corrective actions.

## Database Rollback

1. Stop write traffic to affected service(s).
2. Validate latest successful migration in `flyway_schema_history`.
3. Restore point-in-time backup if destructive migration occurred.
4. Re-run migration validator and smoke tests before reopening writes.

## Configuration Rollback

1. Identify last approved configuration version.
2. Activate prior config through config service or deployment manifest.
3. Validate tenant-specific overrides.
4. Confirm no stale cache/config propagation issues.

## Validation After Rollback

Run:

- `deployment-tests/health-check-validator.sh`
- `deployment-tests/config-validator.sh`
- `deployment-tests/smoke-tests.sh`

If database rollback occurred, also run:

- `deployment-tests/migration-validator.sh`

## Related Operational Runbooks

Operational runbooks are maintained under `docs/runbooks/` and include more than 8 procedures (incident response, database, performance, security, failover, and service-specific operations).

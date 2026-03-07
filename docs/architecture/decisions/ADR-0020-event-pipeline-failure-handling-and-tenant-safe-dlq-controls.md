# ADR-0020: Event Pipeline Failure Handling and Tenant-Safe DLQ Controls

**Status**: Accepted
**Date**: 2026-03-07
**Deciders**: Platform Lead, Security Lead

## Context
Event-driven healthcare pipelines require safe failure handling without cross-tenant leakage in DLQ workflows.

## Decision
Enforce tenant-safe DLQ and mutation controls.
- All DLQ reads and writes are tenant-scoped.
- DLQ tenant context is explicit on request paths.
- Retry/resolve/exhaust operations require elevated roles.
- DLQ security regressions are mandatory in CI.

## Consequences
### Positive
- Failure operations remain secure and compliant.
- Lower risk of cross-tenant incident during remediation.

### Negative
- Operational actions require clearer role management.

## References
- `backend/modules/services/event-processing-service/src/main/java/com/healthdata/events/controller/DeadLetterQueueController.java`
- `backend/modules/services/event-processing-service/src/test/java/com/healthdata/events/controller/DeadLetterQueueControllerSecurityTest.java`

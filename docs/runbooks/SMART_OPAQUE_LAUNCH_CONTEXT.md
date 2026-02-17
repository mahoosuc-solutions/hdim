# SMART Opaque Launch Context

Server-side launch context strategy for SMART on FHIR EHR embedding.

## Behavior

- If `launch` is an inline payload (JSON/base64url/JWT payload), the service:
  - extracts context fields
  - stores the context behind a generated opaque ID
  - uses that opaque ID for downstream launch continuity
- If `launch` is already opaque, the service resolves it from server-side storage.

## TTL And Cleanup

- Launch contexts are stored with TTL (`smart.launch-context.ttl-seconds`, default `600`).
- Cleanup is performed lazily on store/resolve operations by removing expired entries.

## Fallback

- If inline decode fails and opaque lookup misses/expired:
  - request continues with scope-based fallback context
  - no hard failure is returned from authorize flow for missing launch context alone

## Local Validation

```bash
cd backend
./gradlew :modules:services:fhir-service:test \
  --tests '*SmartAuthorizationControllerTest' \
  --tests '*SmartLaunchContextStoreTest' \
  --tests '*SmartAuthorizationServiceTest' \
  --tests '*SmartConfigurationControllerTest' \
  --no-daemon
```

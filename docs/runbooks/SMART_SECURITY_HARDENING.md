# SMART Security Hardening

This runbook documents the security guardrails enforced for SMART on FHIR OAuth authorization flows.

## Enforced Controls

1. `state` is required at `/oauth/authorize`.
2. `aud` is required and must be an absolute `http(s)` URL.
3. `launch` is required when `launch` scope is requested.
4. PKCE is required for:
   - public SMART clients
   - any client with `requirePkce=true`
5. Only `S256` is accepted for `code_challenge_method`.
6. Token exchange (`/oauth/token`) enforces the same PKCE policy, including `S256` verification and required `code_verifier`.

## Validation Scope

Validated with focused backend SMART tests:

```bash
cd backend
./gradlew :modules:services:fhir-service:test \
  --tests '*SmartAuthorizationServiceTest' \
  --tests '*SmartLaunchContextStoreTest' \
  --no-daemon
```

Note: `SmartAuthorizationControllerTest` is tagged `integration` and may be excluded by local JUnit tag filtering depending on task selection.

## Rollback

If rollout causes compatibility issues with legacy clients:

1. Revert the SMART security hardening commit.
2. Re-run SMART backend tests.
3. Coordinate client migration to `S256` PKCE + required `state`/`aud` before re-enabling.

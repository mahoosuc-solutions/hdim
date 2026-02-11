# Dockerfile Validation Testing

## Purpose
Ensure every service Dockerfile meets runtime consistency requirements for production reliability and operability.

This validation enforces:
- A non-root `USER` in the runtime stage
- A `HEALTHCHECK` definition
- Standardized `JAVA_OPTS`
- `wget` is installed in the runtime stage if used by healthchecks

## Why This Matters
Consistent Dockerfiles reduce deployment drift, improve health reporting accuracy, and make runtime behavior predictable across services.

## What This Test Covers
- All service Dockerfiles under `backend/modules/services/*/Dockerfile`
- Runtime stage validation only (multi-stage builds handled correctly)

## How To Run
From the repository root:

```bash
./scripts/validate-dockerfiles.sh
```

## Expected Output
On success:

```
Dockerfile validation passed for 41 services.
```

## Failure Output
If a Dockerfile violates the policy, the script exits non-zero and prints the list of violations. Example:

```
Dockerfile validation failed:

- backend/modules/services/agent-runtime-service/Dockerfile: missing HEALTHCHECK in runtime stage
```

## Remediation Guidelines
- If `HEALTHCHECK` is missing, add a health endpoint that matches the service context path.
- If `JAVA_OPTS` is missing, add the standard container JVM flags.
- If `wget` is used in the healthcheck, ensure it is installed in the runtime stage.
- If `USER` is missing, add a non-root user and `USER` directive.

## Related Files
- `scripts/validate-dockerfiles.sh`
- `scripts/README.md`

## Validation Scope Notes
This test does not validate correctness of healthcheck URLs or service ports. It ensures minimum structural consistency for production reliability.

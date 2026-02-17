# SMART Conformance Test Lane

Deterministic local lane for SMART on FHIR launch conformance checks.

Parent issue: `#439`  
Strategic parent: `#302`

## Coverage

1. Standalone SMART launch automation
2. EHR SMART launch automation
3. Scope/context assertions for launch context behavior
4. Token exchange and PKCE policy checks

## Execute

```bash
./scripts/run-smart-conformance-lane.sh
```

## What runs

The lane executes focused SMART tests in `fhir-service`:

- `SmartConformanceLaneTest`
- `SmartAuthorizationServiceTest`
- `SmartLaunchContextStoreTest`

## Expected result

- All SMART conformance tests pass (`0 failed`)
- Output includes both standalone and EHR launch conformance test cases

## Notes

- This lane is local-first and deterministic.
- It avoids broad suite noise by targeting SMART authorization and launch behavior only.

# HMAC Enforcement Test Results

Date: 2026-02-16
Environment: local demo stack (`docker-compose.demo.yml`)
Gateway edge: `http://localhost:18080`

Reference plan: `docs/HMAC_ENFORCEMENT_TEST_PLAN.md`

## Summary

| Test | Result | Evidence |
|---|---|---|
| Test 1: Basic login with HMAC path | PASS | `POST /api/v1/auth/login` returned `200` |
| Test 2: Patient data access through gateway | PASS | `GET /fhir/Patient?_count=1` returned `200` after login |
| Test 3: Multi-tenant isolation | PASS | `GET /fhir/Patient` with unauthorized tenant returned `403` |
| Test 4: Role-based access control | PASS | JWT payload includes roles `EVALUATOR,ADMIN` |
| Test 5: Token refresh with HMAC | PASS | `POST /api/v1/auth/refresh` returned `200` and new access token |
| Test 6: Invalid signature rejection | PASS | Direct call to FHIR service with fake `X-Auth-Validated` returned `403` |

## Command Evidence

### Test 1

```bash
curl -X POST http://localhost:18080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_admin","password":"demo123"}'
# HTTP 200
```

### Test 2

```bash
curl -b <cookie_jar> http://localhost:18080/fhir/Patient?_count=1
# HTTP 200
```

### Test 3

```bash
curl -b <cookie_jar> \
  -H 'X-Tenant-ID: other-tenant-xyz' \
  http://localhost:18080/fhir/Patient?_count=1
# HTTP 403
```

### Test 4

```bash
# Decode JWT payload from login response
# roles: EVALUATOR,ADMIN
```

### Test 5

```bash
curl -X POST -b <cookie_jar> -c <cookie_jar> \
  -H 'Content-Type: application/json' \
  -d '{}' \
  http://localhost:18080/api/v1/auth/refresh
# HTTP 200
```

### Test 6

```bash
curl -H 'X-Tenant-ID: DEMO001' \
  -H 'X-Auth-User-Id: test-user' \
  -H 'X-Auth-Username: test' \
  -H 'X-Auth-Tenant-Id: DEMO001' \
  -H 'X-Auth-Tenant-Ids: DEMO001' \
  -H 'X-Auth-Roles: ADMIN' \
  -H 'X-Auth-Validated: invalid-signature-fake-hash' \
  'http://localhost:8085/fhir/Patient?_count=1'
# HTTP 403
```

## Log Verification

Observed in `hdim-demo-fhir` logs during execution:

- `Invalid gateway validation signature for request to /fhir/Patient`
- `SECURITY: User ... attempted to access unauthorized tenant`

These confirm signature rejection and tenant isolation enforcement paths are active.

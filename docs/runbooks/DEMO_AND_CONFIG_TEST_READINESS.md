# Demo + Config Test Readiness

This runbook captures what must be completed and validated before running tests for:
- Demo scenario progress/cancel/stop/reset flows.
- Admin portal config versioning (create/promote/approve/activate).

## Scope
- Demo mode UI: `apps/clinical-portal/src/app/demo-mode/components/demo-control-bar/demo-control-bar.component.ts`
- Demo mode API client: `apps/clinical-portal/src/app/demo-mode/services/demo-mode.service.ts`
- Demo seeding service: `backend/modules/services/demo-seeding-service/`
- Admin portal config versions UI + service: `apps/admin-portal/src/app/pages/config-versions/` and `apps/admin-portal/src/app/services/admin.service.ts`
- Gateway config versioning endpoints: `apps/admin-portal/src/app/config/api.config.ts`

## Pre-Reqs and Dependencies
- Demo seeding service deployed and reachable by the clinical portal.
- Gateway routes for config versioning endpoints are implemented and reachable by the admin portal.
- Liquibase migrations applied for demo progress tracking.
- CORS configuration allows portal origins.

## Configuration Checklist
- Environment-specific values (fill these in for your setup):
  - Clinical portal URL: `<CLINICAL_PORTAL_URL>`
  - Admin portal URL: `<ADMIN_PORTAL_URL>`
  - Demo seeding service base URL: `<DEMO_SERVICE_URL>`
  - API gateway base URL: `<API_GATEWAY_URL>`
  - Gateway config service name: `<CONFIG_SERVICE_NAME>`
  - Demo tenant ID: `<DEMO_TENANT_ID>`
  - Target tenant ID (promotion): `<TARGET_TENANT_ID>`
  - Sample config version payload (JSON): `<CONFIG_JSON>`
- Demo API base URL resolution:
  - Clinical portal uses `API_CONFIG.API_GATEWAY_URL` if set.
  - Environment gateway settings are consistent with actual origin.
- Admin portal uses gateway URL for config versioning:
  - `API_CONFIG.GATEWAY_URL` is set and accessible.
- CORS allowlist:
  - `demo.cors.allowed-origins` includes clinical/admin portal origins or `*` for dev.
  - Decide whether to keep both `CorsConfig` and `DemoCorsFilter`.
- Demo persistence:
  - `demo.persistence.enabled` set appropriately (dev often true).
- Database:
  - Demo-seeding service DB reachable.
  - Liquibase is enabled (`ddl-auto: validate`, Liquibase change log wired).

## Data Contracts (Expected Shapes)

### DemoStatus (`GET /api/v1/demo/status`)
```
{
  "ready": boolean,
  "scenarioCount": number,
  "templateCount": number,
  "currentSessionId": string | null,
  "currentScenario": string | null,
  "sessionStatus": string | null
}
```

### LoadScenarioResponse (`POST /api/v1/demo/scenarios/{scenarioName}`)
```
{
  "scenarioName": string,
  "sessionId": string | null,
  "patientCount": number,
  "careGapCount": number,
  "loadTimeMs": number,
  "success": boolean,
  "errorMessage": string | null
}
```

### DemoProgress (`GET /api/v1/demo/sessions/current/progress`)
```
{
  "sessionId": string,
  "scenarioName": string,
  "tenantId": string,
  "stage": string,
  "progressPercent": number,
  "patientsGenerated": number | null,
  "patientsPersisted": number | null,
  "careGapsCreated": number | null,
  "measuresSeeded": number | null,
  "message": string | null,
  "updatedAt": string | null,
  "cancelRequested": boolean
}
```

### ConfigVersion (Admin Portal)
```
{
  "id": string,
  "tenantId": string,
  "serviceName": string,
  "versionNumber": number,
  "status": "DRAFT" | "PENDING_APPROVAL" | "APPROVED" | "REJECTED" | "ACTIVE" | "SUPERSEDED",
  "config": object | null,
  "configHash": string,
  "changeSummary": string | null,
  "sourceVersionId": string | null,
  "createdBy": string,
  "createdAt": string | null,
  "updatedAt": string | null
}
```

### ConfigApproval (Admin Portal)
```
{
  "id": string,
  "tenantId": string,
  "serviceName": string,
  "versionId": string,
  "action": "REQUESTED" | "APPROVED" | "REJECTED",
  "actor": string,
  "comment": string | null,
  "createdAt": string | null
}
```

## Endpoint Inventory (Required)

### Demo Seeding Service
- `GET /api/v1/demo/status` -> DemoStatus
- `GET /api/v1/demo/scenarios` -> DemoScenario[]
- `POST /api/v1/demo/scenarios/{scenarioName}` -> LoadScenarioResponse
- `GET /api/v1/demo/sessions/current/progress` -> DemoProgress
- `GET /api/v1/demo/sessions/{sessionId}/progress` -> DemoProgress
- `POST /api/v1/demo/sessions/current/cancel` -> 202 Accepted
- `POST /api/v1/demo/sessions/current/stop` -> 200 OK
- `POST /api/v1/demo/reset` -> ResetResponse
- `POST /api/v1/demo/reset/current-tenant` -> ResetResponse

### Gateway Config Versioning (Admin Portal)
- `GET /v1/configs/{service}/tenants/{tenant}/versions`
- `GET /v1/configs/{service}/tenants/{tenant}/current`
- `POST /v1/configs/{service}/tenants/{tenant}/versions`
- `POST /v1/configs/{service}/tenants/{tenant}/promote`
- `POST /v1/configs/{service}/tenants/{tenant}/activate/{versionId}`
- `GET /v1/configs/{service}/tenants/{tenant}/audit`
- `GET /v1/configs/{service}/tenants/{tenant}/versions/{versionId}/approvals`
- `POST /v1/configs/{service}/tenants/{tenant}/versions/{versionId}/approvals/request`
- `POST /v1/configs/{service}/tenants/{tenant}/versions/{versionId}/approvals/approve`
- `POST /v1/configs/{service}/tenants/{tenant}/versions/{versionId}/approvals/reject`

## Liquibase Migrations
- Ensure `backend/modules/services/demo-seeding-service/src/main/resources/db/changelog/0007-create-demo-session-progress-table.xml` is applied.
- Ensure `backend/modules/services/demo-seeding-service/src/main/resources/db/changelog/0008-add-demo-session-progress-cancel.xml` is applied.
- Confirm `db.changelog-master.xml` includes both.

## UI Expectations (Manual Sanity)

### Clinical Portal Demo Control Bar
- Scenario list loads dynamically when backend is available.
- Load scenario starts progress polling; stage and percent update.
- Cancel is only enabled while stage is not terminal.
- Stop session ends the current session and resets progress display.
- Reset current scenario clears data for the active tenant.

### Admin Portal Config Versions Page
- Tenant/service selection loads versions and current active version.
- Selected version details render config JSON.
- Approval actions update approvals list and versions table.
- Promote/create/activate flows show success/error messages.

## Cancellation and Progress Semantics
- Cancel request sets `cancelRequested=true`.
- Progress stage becomes `CANCELLED` and percent goes to 100.
- Subsequent polling stops when stage is terminal: `COMPLETE`, `FAILED`, `CANCELLED`.
- Cancel should be checked during generation and persistence (not only after completion).

## Pre-Test Validation Steps (Manual)
1. Verify demo backend availability by calling `/api/v1/demo/status`.
2. Confirm scenarios list returns expected demo scenarios.
3. Trigger a scenario load and confirm progress updates within 2-3 seconds.
4. Request cancellation mid-load and verify stage becomes `CANCELLED`.
5. Reset current tenant and verify session progress cleared.
6. Admin portal: load versions for a known service/tenant and confirm current version appears.
7. Create a draft config version and verify it appears in the list.
8. Request approval and verify approval list updates.
9. Approve and activate (if allowed) and verify current version changes.

## Curl / API Examples

Replace the placeholder variables with values from the Configuration Checklist.

```bash
# Demo status
curl -sS "<DEMO_SERVICE_URL>/api/v1/demo/status"

# List scenarios
curl -sS "<DEMO_SERVICE_URL>/api/v1/demo/scenarios"

# Load scenario
curl -sS -X POST "<DEMO_SERVICE_URL>/api/v1/demo/scenarios/<SCENARIO_NAME>"

# Poll current progress
curl -sS "<DEMO_SERVICE_URL>/api/v1/demo/sessions/current/progress"

# Cancel current load
curl -sS -X POST "<DEMO_SERVICE_URL>/api/v1/demo/sessions/current/cancel"

# Stop current session
curl -sS -X POST "<DEMO_SERVICE_URL>/api/v1/demo/sessions/current/stop"

# Reset demo data
curl -sS -X POST "<DEMO_SERVICE_URL>/api/v1/demo/reset"

# Reset current tenant
curl -sS -X POST "<DEMO_SERVICE_URL>/api/v1/demo/reset/current-tenant"
```

```bash
# Config versions list (gateway)
curl -sS "<API_GATEWAY_URL>/v1/configs/<CONFIG_SERVICE_NAME>/tenants/<DEMO_TENANT_ID>/versions" \
  -H "X-Tenant-ID: <DEMO_TENANT_ID>"

# Current config version
curl -sS "<API_GATEWAY_URL>/v1/configs/<CONFIG_SERVICE_NAME>/tenants/<DEMO_TENANT_ID>/current" \
  -H "X-Tenant-ID: <DEMO_TENANT_ID>"

# Create config version
curl -sS -X POST "<API_GATEWAY_URL>/v1/configs/<CONFIG_SERVICE_NAME>/tenants/<DEMO_TENANT_ID>/versions" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <DEMO_TENANT_ID>" \
  -d '{
    "config": <CONFIG_JSON>,
    "changeSummary": "Demo config update",
    "activate": false
  }'

# Promote config version
curl -sS -X POST "<API_GATEWAY_URL>/v1/configs/<CONFIG_SERVICE_NAME>/tenants/<TARGET_TENANT_ID>/promote" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <TARGET_TENANT_ID>" \
  -d '{
    "sourceVersionId": "<SOURCE_VERSION_ID>",
    "changeSummary": "Promote demo config",
    "activate": false
  }'

# Request approval
curl -sS -X POST "<API_GATEWAY_URL>/v1/configs/<CONFIG_SERVICE_NAME>/tenants/<DEMO_TENANT_ID>/versions/<VERSION_ID>/approvals/request" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <DEMO_TENANT_ID>" \
  -d '{"comment": "Requesting approval"}'

# Approve
curl -sS -X POST "<API_GATEWAY_URL>/v1/configs/<CONFIG_SERVICE_NAME>/tenants/<DEMO_TENANT_ID>/versions/<VERSION_ID>/approvals/approve" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <DEMO_TENANT_ID>" \
  -d '{"comment": "Approved"}'

# Reject
curl -sS -X POST "<API_GATEWAY_URL>/v1/configs/<CONFIG_SERVICE_NAME>/tenants/<DEMO_TENANT_ID>/versions/<VERSION_ID>/approvals/reject" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <DEMO_TENANT_ID>" \
  -d '{"comment": "Rejected"}'

# Activate
curl -sS -X POST "<API_GATEWAY_URL>/v1/configs/<CONFIG_SERVICE_NAME>/tenants/<DEMO_TENANT_ID>/activate/<VERSION_ID>" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: <DEMO_TENANT_ID>" \
  -d '{"reason": "Activated via runbook"}'
```

## Known Risks / Gaps to Resolve Before Testing
- Dual CORS layers (`CorsConfig` and `DemoCorsFilter`) can conflict; select one.
- Ensure the demo load endpoint path used by the clinical portal is implemented server-side.
- Progress polling assumes `/sessions/current/progress` exists even when no session; must return 404 cleanly.
- Validate that demo progress table uses UUID generation (DB default or app-provided).
- Confirm that cancellation checks run often enough for large scenarios.
- Ensure `API_CONFIG.API_GATEWAY_URL` and `API_CONFIG.GATEWAY_URL` are both configured or consolidated.

## Minimal Test Set (Manual)
- Demo: load scenario, observe progress, cancel, stop, reset current tenant.
- Admin: load versions, create version, request approval, approve/reject, activate.
- Regression: Verify existing demo reset and snapshot functionality still works.

## Optional Automated Tests (If Added Later)
- Demo progress endpoint returns terminal state and stops polling in UI.
- Config versions endpoint responses map to UI models correctly.
- Cancellation flow marks `cancelRequested` and terminates generation.

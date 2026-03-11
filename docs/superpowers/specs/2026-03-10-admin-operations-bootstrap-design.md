# HDIM Admin Operations & Bootstrap System — Design Spec

**Date:** 2026-03-10
**Status:** Approved
**Author:** Claude Code + Aaron
**Scope:** Bootstrap seeding, admin APIs, admin UI

---

## Problem Statement

HDIM requires manual SQL scripts and workarounds to create users, tenants, and seed data before the platform is usable. A user who clones the repo and runs `docker compose up` or deploys to K3s cannot log in without manual database intervention. The admin UI components exist as mocks with no backend integration. This blocks the open-source BSL go-live experience where users expect to download, run, and be functional immediately.

## Design Goals

1. **Zero-touch bootstrap:** First start creates a demo tenant, admin users, and sample data automatically
2. **Complete admin lifecycle:** Full CRUD for users, tenants, roles — no manual SQL ever needed
3. **Functional admin UI:** Wire existing Angular mock components to real APIs
4. **Demo tenant as regression fixture:** Pre-seeded data persists across upgrades for validation
5. **Clean production path:** Single env var disables demo tenant for production deployments

## Non-Goals

- Email-based self-service password reset (requires SMTP infrastructure)
- Custom role definitions (13-role enum is sufficient)
- API key management UI (REST API is sufficient for API consumers)
- OAuth2 provider configuration UI
- User invitation flow
- Multi-tenant admin switching UI

---

## Layer A: Bootstrap & Demo Tenant

### Automatic Seeding

A `DemoTenantBootstrap` ApplicationRunner in `gateway-admin-service` executes on startup when `spring.profiles.active` includes `demo`:

1. Create "demo" tenant (id: `demo`, name: `HDIM Demo`, status: `ACTIVE`)
2. Create admin user (`demo_admin` / `changeme123`) with `ADMIN` + `EVALUATOR` roles
3. Create analyst user (`demo_analyst` / `changeme123`) with `ANALYST` + `EVALUATOR` roles
4. Create viewer user (`demo_viewer` / `changeme123`) with `VIEWER` role
5. Assign all users to `demo` tenant
6. Set `force_password_change=true` on all accounts
7. Log bootstrap summary

**Idempotency:** All inserts use conflict-safe semantics. Safe to re-run on every restart. Existing users are not modified (preserves password changes made after first boot).

**Password encoding:** Uses the existing `PasswordEncoder` bean (BCrypt) — no raw SQL hashes.

### Demo Tenant Kill Switch

```yaml
hdim:
  demo-tenant:
    enabled: ${HDIM_DEMO_TENANT_ENABLED:true}
    id: "demo"
```

- `enabled: true` (default): Bootstrap runs, demo tenant is ACTIVE
- `enabled: false`: Bootstrap skips seeding, `DemoTenantDisableRunner` sets tenant status to INACTIVE
- No data deletion — disabling preserves data for upgrade regression testing
- Re-enabling reactivates the tenant

### Upgrade Regression Pattern

The demo tenant's data persists across version upgrades. New Liquibase migrations execute against demo data. If a migration breaks demo data (schema drift, constraint violations), the upgrade fails visibly — catching issues before they reach customer tenants.

---

## Layer B: Backend Admin APIs

### Schema Changes (Liquibase Migration)

```sql
-- users table additions
ALTER TABLE users ADD COLUMN force_password_change BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN deactivated_at TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE users ADD COLUMN deactivated_by UUID;
```

### UserManagementController

Base path: `/api/v1/users`

| Method | Path | Permission | Purpose |
|--------|------|------------|---------|
| GET | `/api/v1/users` | USER_READ | Paginated user list, filterable by role/tenant/active |
| GET | `/api/v1/users/{id}` | USER_READ | Full user detail with roles and tenants |
| PUT | `/api/v1/users/{id}` | USER_WRITE | Update name, email, notes |
| POST | `/api/v1/users/{id}/deactivate` | USER_WRITE | Set active=false, revoke all sessions |
| POST | `/api/v1/users/{id}/reactivate` | USER_WRITE | Set active=true |
| PUT | `/api/v1/users/{id}/roles` | USER_MANAGE_ROLES | Replace role set |
| PUT | `/api/v1/users/{id}/tenants` | USER_MANAGE_ROLES | Replace tenant set |
| POST | `/api/v1/users/{id}/unlock` | USER_WRITE | Clear lock state and failed attempts |
| POST | `/api/v1/users/{id}/reset-password` | USER_WRITE | Set temp password + force_password_change=true |

### PasswordController

Base path: `/api/v1/auth/password`

| Method | Path | Permission | Purpose |
|--------|------|------------|---------|
| POST | `/api/v1/auth/password/change` | Authenticated | Current + new password, clears force_password_change |
| POST | `/api/v1/auth/password/force-change` | Authenticated | New password only, requires force_password_change=true |

### TenantManagementController

Extends existing TenantController. Base path: `/api/v1/tenants`

| Method | Path | Permission | Purpose |
|--------|------|------------|---------|
| GET | `/api/v1/tenants` | TENANT_READ | All tenants with status and user count |
| GET | `/api/v1/tenants/{id}` | TENANT_READ | Tenant detail with config summary |
| PUT | `/api/v1/tenants/{id}` | TENANT_MANAGE | Update name, settings |
| GET | `/api/v1/tenants/{id}/users` | TENANT_READ | Users assigned to tenant |

### Login Flow Change

`JwtAuthenticationResponse` gains one field:

```java
private boolean mustChangePassword;  // true when force_password_change=true on user
```

Frontend checks this field. If true, redirects to `/change-password` instead of `/dashboard`.

### Audit

All user management actions use the existing `@Audited` annotation pattern. Every create, update, deactivate, role change, and password reset is logged with actor, timestamp, and tenant context.

---

## Layer C: Frontend Admin UI

### Navigation

Add "Administration" section to sidebar (visible to ADMIN role):

```
Administration
  ├── Users              → /admin/users
  ├── Tenants            → /admin/tenant-settings
  ├── Audit Logs         → /admin/audit-logs
  └── Demo Management    → /admin/demo-seeding  (existing, working)
```

### AdminUsersComponent (rewrite from mock)

- **List view:** Paginated table — username, email, name, roles (chips), tenant, active, last login
- **Filters:** Role dropdown, active/inactive toggle, search by name/email
- **Row actions:** Edit, Deactivate/Reactivate, Reset Password, Manage Roles
- **Create user:** Dialog form using `POST /api/v1/auth/register`
- **Edit user:** Dialog form calling `PUT /api/v1/users/{id}`
- **Deactivate:** Confirmation dialog → `POST /api/v1/users/{id}/deactivate`
- **Reset password:** Confirmation dialog, shows temp password once
- **Role management:** Multi-select chip list → `PUT /api/v1/users/{id}/roles`

### AdminTenantSettingsComponent (rewrite from mock)

- **Tenant list:** Table with status, user count, created date
- **Tenant detail:** Click to view/edit name, see assigned users
- **Status actions:** Activate/Suspend/Deactivate buttons (existing endpoints)
- **Demo tenant indicator:** Visual badge + kill switch toggle

### AdminAuditLogsComponent (rewrite from stub)

- **Connected to:** audit-query-service via gateway forwarding (`/api/v1/audit/**`)
- **Search:** Date range, user, action type, resource type
- **Table:** Timestamp, user, action, resource, expandable details
- **Export:** CSV/JSON buttons (existing export endpoint)

### PasswordChangeComponent (new)

- **Route:** `/change-password`
- **Trigger:** `mustChangePassword: true` in login response
- **Form:** New password + confirm, strength indicator
- **Calls:** `POST /api/v1/auth/password/change` or `/force-change`
- **Success:** Redirects to dashboard

### New Angular Services

```typescript
// user-management.service.ts
getUsers(params): Observable<Page<UserResponse>>
getUser(id): Observable<UserResponse>
updateUser(id, request): Observable<UserResponse>
deactivateUser(id): Observable<void>
reactivateUser(id): Observable<void>
updateRoles(id, roles): Observable<void>
updateTenants(id, tenantIds): Observable<void>
unlockAccount(id): Observable<void>
resetPassword(id): Observable<TempPasswordResponse>

// tenant-management.service.ts
getTenants(): Observable<TenantResponse[]>
getTenant(id): Observable<TenantResponse>
updateTenant(id, request): Observable<TenantResponse>
getTenantUsers(id): Observable<UserResponse[]>

// password.service.ts
changePassword(request): Observable<void>
forceChangePassword(request): Observable<void>
```

### HIPAA Compliance

All admin components follow existing patterns:
- HTTP calls auto-audited (existing interceptor)
- LoggerService only, no console.log (ESLint enforced)
- PHI filtering on patient-adjacent data
- 15-minute session timeout applies

---

## Sizing

| Layer | New/Modified Files | Scope |
|-------|-------------------|-------|
| A: Bootstrap | 3-4 Java classes, 1 Liquibase migration, K3s config | Smallest |
| B: Backend APIs | 3 controllers, 2-3 services, 1 Liquibase migration, tests | Medium |
| C: Frontend UI | 4 component rewrites, 3 services, 1 new component, nav update | Largest |

## Build Order

1. **Layer A** — unblocks login immediately
2. **Layer B** — each controller independently testable
3. **Layer C** — wires UI as APIs become available

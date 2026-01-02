# Auth Module Stub

## Purpose

The `platform:auth` Gradle module encapsulates the interfaces and default (no-op) implementations required to integrate authentication and tenant scoping across services. While the current project does not implement real authentication, these contracts allow a future auth provider to plug in without refactoring.

## Key Components

- `AuthenticationService`
  - Validates inbound requests and produces an `AuthPrincipal`.
  - Default implementation (`NoOpAuthenticationService`) returns an anonymous principal so downstream code can rely on non-null context.
- `TenantResolver`
  - Resolves tenant identifiers from request metadata. The default resolver (`HeaderTenantResolver`) reads the `X-Tenant-Id` header and falls back to `tenant-1`.
- `AuthContext` / `ScopedTenant`
  - Thread-local helpers to expose the current principal and tenant.
  - Populated by the `AuthContextFilter` in service modules (e.g. `fhir-service`).

## Integration Notes

- Services depending on `platform:auth` should add the module as a Gradle dependency and register the `AuthContextFilter` in their security configuration (as the FHIR service does).
- When a real auth provider is introduced, implement `AuthenticationService` and `TenantResolver`, then mark the existing no-op beans with `@Profile("!auth")` or `@ConditionalOnMissingBean` as appropriate.
- Kafka producers, cache utilities, and other cross-cutting components should call `AuthContext.currentPrincipal()` / `ScopedTenant.currentTenant()` to enrich metadata.

## Tasks Remaining

- Replace `NoOpAuthenticationService` with an implementation that validates JWT/OAuth tokens.
- Propagate `AuthPrincipal` into Kafka headers and audit logs.
- Provide an `AuthClient` on the frontend that can obtain and refresh tokens, wiring it into `AdminPortalApiClient`.
- Enforce role-based authorization once real principals are available.

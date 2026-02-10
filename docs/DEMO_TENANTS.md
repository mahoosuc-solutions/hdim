# Demo Tenants

This document captures the intended purpose of demo tenants and how they are used
in the local demo stack.

## Primary demo tenants

- `demo-tenant`
  Primary demo tenant used across scripts and docs for baseline scenarios.
- `acme-health`
  Payer demo tenant used for HEDIS and quality measure workflows.

## Multi-tenant administration demo

- `summit-care`
  ACO tenant used for multi-tenant admin workflows and data isolation demos.
- `valley-health`
  Health system tenant used for multi-tenant admin workflows and data isolation demos.

## Admin-only tenant

- `demo-admin`
  Reserved for super-admin workflows and platform configuration. This tenant is
  not seeded with clinical data by default.

## Notes

- Demo user allowlists include all of the above tenant IDs in `docker-compose.demo.yml`.
- Seed scripts should default to `demo-tenant` or `acme-health` unless a specific
  scenario requires otherwise.

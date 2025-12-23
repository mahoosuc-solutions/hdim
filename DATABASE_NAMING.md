# Database Naming & Initialization (Canonical: healthdata_*)

This repository uses multiple naming conventions across scripts and service configs. The canonical naming standard for all new work is:

`healthdata_<service>`

This document captures the canonical names, required Postgres extensions, and the creation order for the full stack.

## Canonical Database Names

### Core Clinical Services
- `healthdata_cql` (CQL engine + shared auth tables)
- `healthdata_quality_measure`
- `healthdata_fhir`
- `healthdata_patient`
- `healthdata_care_gap`
- `healthdata_consent`
- `healthdata_events`
- `healthdata_event_router`
- `healthdata_gateway`

### Analytics
- `healthdata_analytics`
- `healthdata_predictive`
- `healthdata_sdoh`

### AI / Agent Services
- `healthdata_agent`
- `healthdata_agent_runtime`
- `healthdata_ai_assistant`

### Data Processing
- `healthdata_enrichment`
- `healthdata_cdr`

### Workflow / Integration
- `healthdata_approval`
- `healthdata_payer`
- `healthdata_migration`
- `healthdata_ehr_connector`
- `healthdata_prior_auth`
- `healthdata_ecr`

### Support / Docs
- `healthdata_docs`
- `healthdata_audit`

## Required Postgres Extensions

Apply these per database unless noted:
- `uuid-ossp` (commonly used for UUID generation)
- `pgcrypto` (needed for `gen_random_uuid()` in seed scripts)
- `pg_stat_statements` (performance diagnostics)
- `pg_trgm` (required by FHIR service for `gin_trgm_ops` indexes)

Recommended defaults:
- Enable `pg_trgm` at least on `healthdata_fhir`.
- Enable `pgcrypto` at least on `healthdata_cql` if using `backend/test-users.sql`.

## Canonical Creation Script (psql)

Use this as the single source of truth for creation. Keep it in sync with service configs:

```sql
-- Core clinical
CREATE DATABASE healthdata_cql;
CREATE DATABASE healthdata_quality_measure;
CREATE DATABASE healthdata_fhir;
CREATE DATABASE healthdata_patient;
CREATE DATABASE healthdata_care_gap;
CREATE DATABASE healthdata_consent;
CREATE DATABASE healthdata_events;
CREATE DATABASE healthdata_event_router;
CREATE DATABASE healthdata_gateway;

-- Analytics
CREATE DATABASE healthdata_analytics;
CREATE DATABASE healthdata_predictive;
CREATE DATABASE healthdata_sdoh;

-- AI / agents
CREATE DATABASE healthdata_agent;
CREATE DATABASE healthdata_agent_runtime;
CREATE DATABASE healthdata_ai_assistant;

-- Data processing
CREATE DATABASE healthdata_enrichment;
CREATE DATABASE healthdata_cdr;

-- Workflow / integration
CREATE DATABASE healthdata_approval;
CREATE DATABASE healthdata_payer;
CREATE DATABASE healthdata_migration;
CREATE DATABASE healthdata_ehr_connector;
CREATE DATABASE healthdata_prior_auth;
CREATE DATABASE healthdata_ecr;

-- Support / docs
CREATE DATABASE healthdata_docs;
CREATE DATABASE healthdata_audit;
```

## Extensions (psql)

```sql
-- Example: run per database
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
```

## Migrations (Liquibase)

The canonical migration order is captured in `backend/run-migrations-simple.sh`:
- `healthdata_fhir`
- `healthdata_cql`
- `healthdata_consent`
- `healthdata_events`
- `healthdata_patient`
- `healthdata_care_gap`
- `healthdata_analytics`
- `healthdata_quality_measure`
- `healthdata_audit`

## Known Inconsistencies to Address

- Some Docker scripts create `*_db` databases (non-canonical).
- Several service configs default to `healthdata_db` (generic DB) instead of service-specific names.
- Gateway defaults to `healthdata_cql` for auth tables; this is currently the canonical location for auth data.

Standardize future work on the `healthdata_*` naming above and update any remaining configs as needed.

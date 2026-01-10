-- HDIM Healthcare Platform - Windows PostgreSQL Setup
-- Run this in pgAdmin or psql on Windows as admin user
--
-- Usage (Windows cmd/PowerShell):
--   psql -U admin -d postgres -f external-db-setup-windows.sql

-- Create all required databases
CREATE DATABASE healthdata_db;
CREATE DATABASE fhir_db;
CREATE DATABASE cql_db;
CREATE DATABASE quality_db;
CREATE DATABASE patient_db;
CREATE DATABASE caregap_db;
CREATE DATABASE consent_db;
CREATE DATABASE event_db;
CREATE DATABASE event_router_db;
CREATE DATABASE gateway_db;
CREATE DATABASE agent_db;
CREATE DATABASE agent_runtime_db;
CREATE DATABASE ai_assistant_db;
CREATE DATABASE analytics_db;
CREATE DATABASE predictive_db;
CREATE DATABASE sdoh_db;
CREATE DATABASE enrichment_db;
CREATE DATABASE cdr_db;
CREATE DATABASE approval_db;
CREATE DATABASE payer_db;
CREATE DATABASE migration_db;
CREATE DATABASE ehr_connector_db;
CREATE DATABASE docs_db;
CREATE DATABASE sales_automation_db;
CREATE DATABASE notification_db;
CREATE DATABASE hcc_db;
CREATE DATABASE prior_auth_db;
CREATE DATABASE qrda_db;
CREATE DATABASE ecr_db;
CREATE DATABASE healthdata_demo;

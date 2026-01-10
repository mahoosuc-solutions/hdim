-- Minimal HDIM PostgreSQL setup for clinical stack

-- Create service databases
CREATE DATABASE gateway_db OWNER healthdata;
CREATE DATABASE fhir_db OWNER healthdata;
CREATE DATABASE cql_db OWNER healthdata;
CREATE DATABASE quality_db OWNER healthdata;
CREATE DATABASE patient_db OWNER healthdata;
CREATE DATABASE caregap_db OWNER healthdata;

-- Extensions used by some services
\c fhir_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c cql_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c quality_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

\c patient_db
CREATE EXTENSION IF NOT EXISTS pg_trgm;

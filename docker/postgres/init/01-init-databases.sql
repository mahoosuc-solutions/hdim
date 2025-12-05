-- Initialize databases for HealthData services

-- FHIR database (for HAPI FHIR server)
CREATE DATABASE healthdata_fhir;
GRANT ALL PRIVILEGES ON DATABASE healthdata_fhir TO healthdata;

-- Quality Measure database
CREATE DATABASE healthdata_quality_measure;
GRANT ALL PRIVILEGES ON DATABASE healthdata_quality_measure TO healthdata;

-- Grant schema permissions for FHIR database
\c healthdata_fhir
GRANT ALL ON SCHEMA public TO healthdata;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Grant schema permissions for Quality Measure database
\c healthdata_quality_measure
GRANT ALL ON SCHEMA public TO healthdata;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- CQL database is the default (healthdata_cql - created by environment variable)
\c healthdata_cql
GRANT ALL ON SCHEMA public TO healthdata;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";


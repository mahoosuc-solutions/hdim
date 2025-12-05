#!/bin/bash
set -e

# Create multiple databases
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create databases
    CREATE DATABASE fhir_db;
    CREATE DATABASE quality_db;
    CREATE DATABASE cql_db;
    CREATE DATABASE event_db;
    CREATE DATABASE patient_db;
    CREATE DATABASE caregap_db;

    -- Grant all privileges
    GRANT ALL PRIVILEGES ON DATABASE fhir_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE quality_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE cql_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE event_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE patient_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE caregap_db TO $POSTGRES_USER;
EOSQL

echo "Multiple databases created successfully"
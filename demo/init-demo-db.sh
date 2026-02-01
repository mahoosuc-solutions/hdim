#!/bin/bash
# Initialize Demo Databases
# This script creates the necessary databases for the HDIM demo

set -e

echo "Creating demo databases..."

# Create databases for each service
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Ensure demo user password is in sync with the container env
    ALTER USER ${POSTGRES_USER} WITH PASSWORD '${POSTGRES_PASSWORD}';

    -- Create service databases
    CREATE DATABASE gateway_db;
    CREATE DATABASE cql_db;
    CREATE DATABASE patient_db;
    CREATE DATABASE fhir_db;
    CREATE DATABASE caregap_db;
    CREATE DATABASE quality_db;

    -- Grant privileges
    GRANT ALL PRIVILEGES ON DATABASE gateway_db TO healthdata;
    GRANT ALL PRIVILEGES ON DATABASE cql_db TO healthdata;
    GRANT ALL PRIVILEGES ON DATABASE patient_db TO healthdata;
    GRANT ALL PRIVILEGES ON DATABASE fhir_db TO healthdata;
    GRANT ALL PRIVILEGES ON DATABASE caregap_db TO healthdata;
    GRANT ALL PRIVILEGES ON DATABASE quality_db TO healthdata;
EOSQL

echo "Demo databases created successfully!"

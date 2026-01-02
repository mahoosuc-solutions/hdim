#!/bin/bash
# =============================================================================
# Initialize PostgreSQL Replication
# =============================================================================
# This script runs on the primary to set up replication users and slots
# =============================================================================

set -e

# Create replication user if not exists
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${REPLICATION_USER:-replicator}') THEN
            CREATE ROLE ${REPLICATION_USER:-replicator} WITH REPLICATION LOGIN PASSWORD '${REPLICATION_PASSWORD:-replicator_password}';
        END IF;
    END
    \$\$;

    -- Create replication slots for replicas
    SELECT pg_create_physical_replication_slot('replica_1_slot', true)
    WHERE NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'replica_1_slot');

    SELECT pg_create_physical_replication_slot('replica_2_slot', true)
    WHERE NOT EXISTS (SELECT 1 FROM pg_replication_slots WHERE slot_name = 'replica_2_slot');
EOSQL

echo "Replication setup completed successfully"

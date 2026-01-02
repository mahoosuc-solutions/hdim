-- Row-Level Security (RLS) Migration for HIPAA Compliance
-- CRITICAL: This migration enables multi-tenant data isolation at the database level
-- Execute this AFTER all application migrations are complete
--
-- Purpose: Ensure that users can only access data belonging to their tenant
-- Compliance: HIPAA Security Rule - Access Controls (§164.308(a)(4))
--
-- Usage:
--   psql -U healthdata -d healthdata_production -f enable-row-level-security.sql
--
-- Rollback:
--   psql -U healthdata -d healthdata_production -f disable-row-level-security.sql

-- =============================================================================
-- SECTION 1: Enable RLS on All Multi-Tenant Tables
-- =============================================================================

DO $$
DECLARE
    r RECORD;
    table_count INTEGER := 0;
    policy_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Starting Row-Level Security enablement...';
    RAISE NOTICE 'Database: %', current_database();
    RAISE NOTICE 'Timestamp: %', now();
    RAISE NOTICE '';

    -- Find all tables with tenant_id column
    FOR r IN
        SELECT DISTINCT
            t.tablename,
            t.schemaname
        FROM pg_tables t
        INNER JOIN information_schema.columns c
            ON t.tablename = c.table_name
            AND t.schemaname = c.table_schema
        WHERE c.column_name = 'tenant_id'
        AND t.schemaname = 'public'
        AND t.tablename NOT LIKE 'pg_%'
        ORDER BY t.tablename
    LOOP
        -- Enable RLS on table
        BEGIN
            EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY',
                r.schemaname, r.tablename);

            table_count := table_count + 1;
            RAISE NOTICE '[%] Enabled RLS on: %.%',
                table_count, r.schemaname, r.tablename;

        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'Failed to enable RLS on %.%: %',
                r.schemaname, r.tablename, SQLERRM;
        END;

        -- Drop existing policy if exists
        BEGIN
            EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I.%I',
                r.schemaname, r.tablename);
        EXCEPTION WHEN OTHERS THEN
            -- Ignore if policy doesn't exist
            NULL;
        END;

        -- Create tenant isolation policy
        BEGIN
            EXECUTE format(
                'CREATE POLICY tenant_isolation ON %I.%I
                 USING (tenant_id = current_setting(''app.current_tenant'', TRUE))',
                r.schemaname, r.tablename
            );

            policy_count := policy_count + 1;
            RAISE NOTICE '  ✓ Created policy: tenant_isolation';

        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'Failed to create policy on %.%: %',
                r.schemaname, r.tablename, SQLERRM;
        END;

        RAISE NOTICE '';

    END LOOP;

    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'RLS Enablement Complete!';
    RAISE NOTICE 'Tables with RLS enabled: %', table_count;
    RAISE NOTICE 'Policies created: %', policy_count;
    RAISE NOTICE '=============================================================================';

END $$;

-- =============================================================================
-- SECTION 2: Create Tenant Context Setting Function
-- =============================================================================

CREATE OR REPLACE FUNCTION set_current_tenant(tenant_id_param TEXT)
RETURNS void AS $$
BEGIN
    PERFORM set_config('app.current_tenant', tenant_id_param, FALSE);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION set_current_tenant(TEXT) IS
    'Sets the current tenant context for row-level security.
     Call this at the beginning of each database transaction.
     Example: SELECT set_current_tenant(''tenant-001'');';

-- =============================================================================
-- SECTION 3: Grant Bypass Permissions to System Roles
-- =============================================================================

-- Create system role if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'healthdata_system') THEN
        CREATE ROLE healthdata_system;
        RAISE NOTICE 'Created role: healthdata_system';
    END IF;
END $$;

-- Grant BYPASSRLS to system role for admin operations
ALTER ROLE healthdata_system BYPASSRLS;

COMMENT ON ROLE healthdata_system IS
    'System role with row-level security bypass for administrative operations.
     Only use for data migrations, reporting, and system maintenance.';

-- =============================================================================
-- SECTION 4: Create Validation Function
-- =============================================================================

CREATE OR REPLACE FUNCTION validate_rls_enabled()
RETURNS TABLE (
    table_name TEXT,
    rls_enabled BOOLEAN,
    policy_exists BOOLEAN,
    status TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        t.tablename::TEXT,
        (SELECT relrowsecurity FROM pg_class
         WHERE oid = (t.schemaname || '.' || t.tablename)::regclass) AS rls_enabled,
        EXISTS (
            SELECT 1 FROM pg_policies
            WHERE schemaname = t.schemaname
            AND tablename = t.tablename
            AND policyname = 'tenant_isolation'
        ) AS policy_exists,
        CASE
            WHEN (SELECT relrowsecurity FROM pg_class
                  WHERE oid = (t.schemaname || '.' || t.tablename)::regclass)
                AND EXISTS (
                    SELECT 1 FROM pg_policies
                    WHERE schemaname = t.schemaname
                    AND tablename = t.tablename
                    AND policyname = 'tenant_isolation'
                )
            THEN '✓ SECURE'
            ELSE '✗ INSECURE'
        END AS status
    FROM pg_tables t
    WHERE t.schemaname = 'public'
    AND EXISTS (
        SELECT 1 FROM information_schema.columns c
        WHERE c.table_name = t.tablename
        AND c.table_schema = t.schemaname
        AND c.column_name = 'tenant_id'
    )
    ORDER BY t.tablename;
END;
$$ LANGUAGE plpgsql;

-- =============================================================================
-- SECTION 5: Validation Report
-- =============================================================================

DO $$
DECLARE
    secure_count INTEGER;
    total_count INTEGER;
    coverage_pct NUMERIC;
BEGIN
    SELECT
        COUNT(*) FILTER (WHERE status = '✓ SECURE'),
        COUNT(*)
    INTO secure_count, total_count
    FROM validate_rls_enabled();

    coverage_pct := ROUND((secure_count::NUMERIC / NULLIF(total_count, 0)) * 100, 1);

    RAISE NOTICE '';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'RLS VALIDATION REPORT';
    RAISE NOTICE '=============================================================================';
    RAISE NOTICE 'Total tenant-isolated tables: %', total_count;
    RAISE NOTICE 'Secure tables (RLS + Policy): %', secure_count;
    RAISE NOTICE 'Coverage: %%', coverage_pct;
    RAISE NOTICE '';
    RAISE NOTICE 'Run this query to see details:';
    RAISE NOTICE '  SELECT * FROM validate_rls_enabled();';
    RAISE NOTICE '=============================================================================';
END $$;

-- =============================================================================
-- SECTION 6: Usage Examples
-- =============================================================================

/*
-- Example 1: Set tenant context in application
SELECT set_current_tenant('tenant-001');

-- Example 2: Query data (automatically filtered by RLS)
SELECT * FROM patients;  -- Only returns patients for tenant-001

-- Example 3: Switch tenant context
SELECT set_current_tenant('tenant-002');
SELECT * FROM patients;  -- Now returns patients for tenant-002

-- Example 4: Validate RLS is working
SELECT * FROM validate_rls_enabled();

-- Example 5: Admin query (requires healthdata_system role)
SET ROLE healthdata_system;
SELECT COUNT(*) FROM patients;  -- Returns all patients (bypasses RLS)
RESET ROLE;

-- Example 6: Test tenant isolation
SELECT set_current_tenant('tenant-001');
INSERT INTO patients (id, tenant_id, name) VALUES (uuid_generate_v4(), 'tenant-002', 'Test');
-- This will fail! RLS prevents inserting data for other tenants

*/

-- =============================================================================
-- SECTION 7: Application Integration Notes
-- =============================================================================

/*
CRITICAL INTEGRATION STEPS:

1. Update application database connection logic:

   // Java/Spring Boot example
   @Transactional
   public void executeWithTenant(String tenantId, Runnable operation) {
       jdbcTemplate.execute("SELECT set_current_tenant('" + tenantId + "')");
       try {
           operation.run();
       } finally {
           jdbcTemplate.execute("RESET app.current_tenant");
       }
   }

2. Add tenant context to all database transactions:

   // Before any database operation
   set_current_tenant(request.getHeader("X-Tenant-ID"));

3. Update connection pool configuration:

   // Ensure tenant context is reset between connections
   spring.datasource.hikari.connection-init-sql=RESET app.current_tenant

4. Test tenant isolation:

   // Create test for cross-tenant data leakage
   @Test
   public void testTenantIsolation() {
       setTenant("tenant-A");
       Patient p1 = createPatient();

       setTenant("tenant-B");
       assertNull(patientRepository.findById(p1.getId()));  // Should not find it
   }

5. Monitor RLS in production:

   // Weekly validation query
   SELECT * FROM validate_rls_enabled()
   WHERE status = '✗ INSECURE';

*/

-- =============================================================================
-- ROLLBACK SCRIPT (Save as disable-row-level-security.sql)
-- =============================================================================

/*
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT tablename FROM pg_tables
        WHERE schemaname = 'public'
        AND tablename IN (
            SELECT table_name FROM information_schema.columns
            WHERE column_name = 'tenant_id'
        )
    LOOP
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', r.tablename);
        EXECUTE format('ALTER TABLE %I DISABLE ROW LEVEL SECURITY', r.tablename);
        RAISE NOTICE 'Disabled RLS on: %', r.tablename;
    END LOOP;
END $$;
*/

-- =============================================================================
-- END OF MIGRATION
-- =============================================================================

-- Run validation
SELECT * FROM validate_rls_enabled() ORDER BY table_name;

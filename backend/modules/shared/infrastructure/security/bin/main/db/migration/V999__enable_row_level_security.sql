-- =====================================================
-- HDIM Healthcare Platform - Row-Level Security Migration
-- =====================================================
-- CRITICAL: This migration enables PostgreSQL Row-Level Security (RLS)
-- for HIPAA compliance and multi-tenant data isolation.
--
-- Purpose:
--   - Enforce tenant isolation at the database level
--   - Prevent cross-tenant data access
--   - Ensure HIPAA compliance for PHI data
--
-- Prerequisites:
--   - All tables must have a tenant_id column
--   - Application must set app.current_tenant before queries
--
-- Usage:
--   SET app.current_tenant = 'tenant-001';
--   SELECT * FROM patients;  -- Only returns tenant-001 data
-- =====================================================

-- Create function to get current tenant
CREATE OR REPLACE FUNCTION get_current_tenant()
RETURNS TEXT AS $$
BEGIN
    RETURN current_setting('app.current_tenant', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create function to enable RLS on a table
CREATE OR REPLACE FUNCTION enable_rls_on_table(table_name TEXT)
RETURNS VOID AS $$
BEGIN
    -- Enable RLS
    EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
    
    -- Force RLS for table owner too (important for superuser connections)
    EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
    
    -- Drop existing policy if exists
    EXECUTE format('DROP POLICY IF EXISTS tenant_isolation_policy ON %I', table_name);
    
    -- Create tenant isolation policy
    EXECUTE format(
        'CREATE POLICY tenant_isolation_policy ON %I 
         FOR ALL 
         USING (tenant_id = get_current_tenant())
         WITH CHECK (tenant_id = get_current_tenant())',
        table_name
    );
    
    RAISE NOTICE 'RLS enabled on table: %', table_name;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- Enable RLS on all tables with tenant_id column
-- =====================================================

DO $$
DECLARE
    r RECORD;
    enabled_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Starting Row-Level Security migration...';
    
    -- Find all tables with tenant_id column
    FOR r IN
        SELECT DISTINCT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables t 
            ON c.table_name = t.table_name 
            AND c.table_schema = t.table_schema
        WHERE c.column_name = 'tenant_id'
        AND c.table_schema = 'public'
        AND t.table_type = 'BASE TABLE'
        ORDER BY c.table_name
    LOOP
        BEGIN
            PERFORM enable_rls_on_table(r.table_name);
            enabled_count := enabled_count + 1;
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'Failed to enable RLS on %: %', r.table_name, SQLERRM;
        END;
    END LOOP;
    
    RAISE NOTICE 'Row-Level Security enabled on % tables', enabled_count;
END $$;

-- =====================================================
-- Explicitly enable RLS on known HIPAA-sensitive tables
-- (in case they don't have tenant_id yet)
-- =====================================================

-- Patient tables
DO $$ BEGIN PERFORM enable_rls_on_table('patients'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('patient_identifiers'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('patient_demographics'); EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- Clinical data tables
DO $$ BEGIN PERFORM enable_rls_on_table('observations'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('conditions'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('medications'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('procedures'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('encounters'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('diagnostic_reports'); EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- Quality measure tables
DO $$ BEGIN PERFORM enable_rls_on_table('quality_measure_results'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('care_gaps'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('health_scores'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('risk_assessments'); EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- Event tables
DO $$ BEGIN PERFORM enable_rls_on_table('clinical_events'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('event_audit_log'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('dlq_events'); EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- Consent tables
DO $$ BEGIN PERFORM enable_rls_on_table('consents'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('consent_directives'); EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- AI/Agent tables
DO $$ BEGIN PERFORM enable_rls_on_table('agent_definitions'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('agent_executions'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('conversation_history'); EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- Payer workflow tables
DO $$ BEGIN PERFORM enable_rls_on_table('prior_authorizations'); EXCEPTION WHEN OTHERS THEN NULL; END $$;
DO $$ BEGIN PERFORM enable_rls_on_table('claims'); EXCEPTION WHEN OTHERS THEN NULL; END $$;

-- =====================================================
-- Create helper functions for application use
-- =====================================================

-- Function to set tenant context (call at start of each request)
CREATE OR REPLACE FUNCTION set_tenant_context(p_tenant_id TEXT)
RETURNS VOID AS $$
BEGIN
    IF p_tenant_id IS NULL OR p_tenant_id = '' THEN
        RAISE EXCEPTION 'tenant_id cannot be null or empty';
    END IF;
    PERFORM set_config('app.current_tenant', p_tenant_id, false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to clear tenant context (call at end of request)
CREATE OR REPLACE FUNCTION clear_tenant_context()
RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_tenant', '', false);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to verify RLS is working (for testing)
CREATE OR REPLACE FUNCTION verify_rls_enabled(table_name TEXT)
RETURNS TABLE(
    has_rls BOOLEAN,
    policy_count INTEGER,
    force_rls BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.relrowsecurity as has_rls,
        (SELECT COUNT(*)::INTEGER FROM pg_policies p WHERE p.tablename = table_name) as policy_count,
        c.relforcerowsecurity as force_rls
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE c.relname = table_name
    AND n.nspname = 'public';
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- Verification query (run after migration)
-- =====================================================
-- SELECT tablename, policyname, cmd, qual 
-- FROM pg_policies 
-- WHERE schemaname = 'public' 
-- ORDER BY tablename;

COMMENT ON FUNCTION set_tenant_context IS 'Sets the current tenant for RLS. Call at start of each request.';
COMMENT ON FUNCTION clear_tenant_context IS 'Clears tenant context. Call at end of request for security.';
COMMENT ON FUNCTION verify_rls_enabled IS 'Verifies RLS is properly configured on a table.';

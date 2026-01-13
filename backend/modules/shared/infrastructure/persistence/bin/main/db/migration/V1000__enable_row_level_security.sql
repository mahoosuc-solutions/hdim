-- Row-Level Security (RLS) for Multi-Tenant HIPAA Compliance
-- Version 1.0 - Enable RLS on all tenant-scoped tables
--
-- CRITICAL: This migration enforces tenant isolation at the database level
-- Required for HIPAA compliance and PHI protection

-- ============================================================================
-- HELPER FUNCTION: Set current tenant for RLS policies
-- ============================================================================

-- Create function to get current tenant from session
CREATE OR REPLACE FUNCTION get_current_tenant() RETURNS VARCHAR(64) AS $$
BEGIN
    RETURN current_setting('app.current_tenant', true);
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

-- Create function to set current tenant (called by application before queries)
CREATE OR REPLACE FUNCTION set_current_tenant(tenant_id VARCHAR(64)) RETURNS VOID AS $$
BEGIN
    PERFORM set_config('app.current_tenant', tenant_id, false);
END;
$$ LANGUAGE plpgsql VOLATILE SECURITY DEFINER;

COMMENT ON FUNCTION get_current_tenant() IS 'Returns the current tenant ID from session settings';
COMMENT ON FUNCTION set_current_tenant(VARCHAR) IS 'Sets the current tenant ID for RLS policy evaluation';

-- ============================================================================
-- DYNAMIC RLS ENABLEMENT: Apply to all tables with tenant_id column
-- ============================================================================

DO $$
DECLARE
    tbl_record RECORD;
    policy_name VARCHAR;
BEGIN
    -- Find all tables with a tenant_id column
    FOR tbl_record IN
        SELECT DISTINCT t.table_name, t.table_schema
        FROM information_schema.tables t
        JOIN information_schema.columns c ON t.table_name = c.table_name AND t.table_schema = c.table_schema
        WHERE t.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
          AND c.column_name = 'tenant_id'
    LOOP
        policy_name := 'tenant_isolation_' || tbl_record.table_name;

        -- Enable RLS on the table
        EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY',
                       tbl_record.table_schema, tbl_record.table_name);

        -- Force RLS for table owners (important for superuser access patterns)
        EXECUTE format('ALTER TABLE %I.%I FORCE ROW LEVEL SECURITY',
                       tbl_record.table_schema, tbl_record.table_name);

        -- Drop existing policy if it exists (idempotent)
        BEGIN
            EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I',
                           policy_name, tbl_record.table_schema, tbl_record.table_name);
        EXCEPTION WHEN undefined_object THEN
            -- Policy doesn't exist, continue
            NULL;
        END;

        -- Create RLS policy for tenant isolation
        -- This policy allows SELECT, INSERT, UPDATE, DELETE only when tenant_id matches
        EXECUTE format(
            'CREATE POLICY %I ON %I.%I FOR ALL USING (tenant_id = get_current_tenant()) WITH CHECK (tenant_id = get_current_tenant())',
            policy_name, tbl_record.table_schema, tbl_record.table_name
        );

        RAISE NOTICE 'Enabled RLS on table: %.% with policy: %',
                     tbl_record.table_schema, tbl_record.table_name, policy_name;
    END LOOP;
END $$;

-- ============================================================================
-- BYPASS POLICY: For system/admin operations
-- ============================================================================

-- Create a bypass policy for system operations (e.g., background jobs, migrations)
-- This requires a role named 'hdim_system' which can bypass RLS
DO $$
DECLARE
    tbl_record RECORD;
BEGIN
    -- Create system role if not exists
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'hdim_system') THEN
        CREATE ROLE hdim_system NOLOGIN;
        RAISE NOTICE 'Created hdim_system role';
    END IF;

    -- Grant bypass to system role for all tenant-scoped tables
    FOR tbl_record IN
        SELECT DISTINCT t.table_name, t.table_schema
        FROM information_schema.tables t
        JOIN information_schema.columns c ON t.table_name = c.table_name AND t.table_schema = c.table_schema
        WHERE t.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
          AND c.column_name = 'tenant_id'
    LOOP
        -- Create bypass policy for system role
        EXECUTE format(
            'CREATE POLICY %I ON %I.%I FOR ALL TO hdim_system USING (true) WITH CHECK (true)',
            'system_bypass_' || tbl_record.table_name,
            tbl_record.table_schema,
            tbl_record.table_name
        );
    END LOOP;
END $$;

-- ============================================================================
-- AUDIT HELPER: Log RLS policy applications
-- ============================================================================

-- Create audit table for RLS events (optional but recommended for compliance)
CREATE TABLE IF NOT EXISTS rls_audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL,
    policy_name VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,  -- 'ENABLED', 'DISABLED', 'MODIFIED'
    performed_by VARCHAR(100) DEFAULT current_user,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details JSONB
);

COMMENT ON TABLE rls_audit_log IS 'Audit trail for RLS policy changes - HIPAA compliance';

-- Log this migration
INSERT INTO rls_audit_log (table_name, policy_name, action, details)
SELECT
    t.table_name,
    'tenant_isolation_' || t.table_name,
    'ENABLED',
    jsonb_build_object(
        'migration', 'V1__enable_row_level_security.sql',
        'applied_at', CURRENT_TIMESTAMP
    )
FROM information_schema.tables t
JOIN information_schema.columns c ON t.table_name = c.table_name AND t.table_schema = c.table_schema
WHERE t.table_schema = 'public'
  AND t.table_type = 'BASE TABLE'
  AND c.column_name = 'tenant_id';

-- ============================================================================
-- VERIFICATION QUERIES (for manual verification)
-- ============================================================================

-- List all tables with RLS enabled
-- SELECT schemaname, tablename, rowsecurity
-- FROM pg_tables
-- WHERE schemaname = 'public' AND rowsecurity = true;

-- List all RLS policies
-- SELECT schemaname, tablename, policyname, cmd, qual, with_check
-- FROM pg_policies
-- WHERE schemaname = 'public';

-- ============================================================================
-- NOTES FOR APPLICATION DEVELOPERS
-- ============================================================================

-- Before executing any query, the application MUST set the current tenant:
--   SELECT set_current_tenant('tenant-123');
--
-- Example Spring Boot configuration (in a TransactionAspect or Filter):
--   @Before("execution(* com.healthdata..*.repository..*(..))")
--   public void setTenant(JoinPoint jp) {
--       String tenantId = TenantContext.getCurrentTenant();
--       jdbcTemplate.execute("SELECT set_current_tenant('" + tenantId + "')");
--   }
--
-- For background jobs without tenant context, use the hdim_system role:
--   SET ROLE hdim_system;
--   -- perform cross-tenant operations
--   RESET ROLE;

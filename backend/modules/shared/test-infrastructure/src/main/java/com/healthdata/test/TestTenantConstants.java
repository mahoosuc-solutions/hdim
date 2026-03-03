package com.healthdata.test;

/**
 * Standardized tenant and identity constants for tests.
 *
 * <p>Using consistent tenant IDs across contract, integration, and FHIR tests
 * ensures that test data represents a complete per-customer view. Each "tenant"
 * maps to a healthcare organization customer (payer, ACO, health system).
 *
 * <p>Usage: import statically or reference directly in test classes.
 *
 * <pre>{@code
 * import static com.healthdata.test.TestTenantConstants.*;
 *
 * class MyServiceIT extends AbstractFhirIntegrationTest {
 *     private static final String TENANT = PRIMARY_TENANT_ID;
 * }
 * }</pre>
 */
public final class TestTenantConstants {

    private TestTenantConstants() {}

    // ── Primary test tenant (use for all standard tests) ──────────────
    /** Primary tenant for contract, FHIR, and integration tests. */
    public static final String PRIMARY_TENANT_ID = "test-tenant-contracts";

    /** Secondary tenant for multi-tenant isolation assertions. */
    public static final String SECONDARY_TENANT_ID = "test-tenant-isolation";

    // ── Synchronized patient IDs (shared with Pact contract tests) ────
    public static final String PATIENT_JOHN_DOE_ID = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
    public static final String PATIENT_JANE_SMITH_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";

    // ── Synchronized care gap IDs (shared with Pact contract tests) ───
    public static final String CARE_GAP_HBA1C_ID = "550e8400-e29b-41d4-a716-446655440001";
    public static final String CARE_GAP_BCS_ID = "550e8400-e29b-41d4-a716-446655440002";
    public static final String CARE_GAP_COL_ID = "550e8400-e29b-41d4-a716-446655440003";

    // ── Test actor/user identity ──────────────────────────────────────
    public static final String TEST_ACTOR = "contract-test-user";
}

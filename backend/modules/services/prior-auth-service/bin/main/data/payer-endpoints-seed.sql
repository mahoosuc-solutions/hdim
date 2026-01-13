-- Payer Endpoints Seed Data
-- Major US Health Insurance Payers with CMS-0057-F Prior Authorization API configurations
-- Note: URLs and credentials are placeholders - configure with actual payer endpoints in production

-- =============================================================================
-- NATIONAL COMMERCIAL PAYERS
-- =============================================================================

-- UnitedHealthcare
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'UHC001', 'UnitedHealthcare',
    'https://api.uhc.com/fhir/r4',
    'https://api.uhc.com/prior-auth/v1/Claim/$submit',
    'https://api.uhc.com/provider-access/v1',
    'https://api.uhc.com/provider-directory/v1',
    'https://api.uhc.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "drugFormulary": true, "referral": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Anthem (Elevance Health)
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'ANTHEM001', 'Anthem Blue Cross Blue Shield',
    'https://api.anthem.com/fhir/r4',
    'https://api.anthem.com/prior-auth/v1/Claim/$submit',
    'https://api.anthem.com/provider-access/v1',
    'https://api.anthem.com/provider-directory/v1',
    'https://api.anthem.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "drugFormulary": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Aetna (CVS Health)
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'AETNA001', 'Aetna',
    'https://api.aetna.com/fhir/r4',
    'https://api.aetna.com/prior-auth/v1/Claim/$submit',
    'https://api.aetna.com/provider-access/v1',
    'https://api.aetna.com/provider-directory/v1',
    'https://api.aetna.com/oauth2/token',
    'SMART_ON_FHIR',
    'launch/patient patient/Patient.read patient/Claim.write',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "drugFormulary": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Cigna
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'CIGNA001', 'Cigna',
    'https://api.cigna.com/fhir/r4',
    'https://api.cigna.com/prior-auth/v1/Claim/$submit',
    'https://api.cigna.com/provider-access/v1',
    'https://api.cigna.com/provider-directory/v1',
    'https://api.cigna.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "drugFormulary": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Humana
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'HUMANA001', 'Humana',
    'https://api.humana.com/fhir/r4',
    'https://api.humana.com/prior-auth/v1/Claim/$submit',
    'https://api.humana.com/provider-access/v1',
    'https://api.humana.com/provider-directory/v1',
    'https://api.humana.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "drugFormulary": true, "medicareAdvantage": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Kaiser Permanente
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'KAISER001', 'Kaiser Permanente',
    'https://api.kaiserpermanente.org/fhir/r4',
    'https://api.kaiserpermanente.org/prior-auth/v1/Claim/$submit',
    'https://api.kaiserpermanente.org/provider-access/v1',
    'https://api.kaiserpermanente.org/provider-directory/v1',
    'https://api.kaiserpermanente.org/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, FALSE, TRUE,
    '{"priorAuth": true, "providerAccess": false, "integratedSystem": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- MEDICARE ADVANTAGE PLANS
-- =============================================================================

-- CMS Medicare FFS (Traditional Medicare)
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'CMS_MEDICARE', 'CMS Traditional Medicare',
    'https://api.cms.gov/ab2d/fhir/r4',
    'https://api.cms.gov/prior-auth/v1/Claim/$submit',
    'https://api.cms.gov/provider-access/v1',
    'https://data.cms.gov/provider-data',
    'https://api.cms.gov/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ExplanationOfBenefit.read',
    TRUE, FALSE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "blueButton": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- UnitedHealthcare Medicare Advantage
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'UHC_MA001', 'UnitedHealthcare Medicare Advantage',
    'https://api.uhc.com/medicare/fhir/r4',
    'https://api.uhc.com/medicare/prior-auth/v1/Claim/$submit',
    'https://api.uhc.com/medicare/provider-access/v1',
    'https://api.uhc.com/medicare/provider-directory/v1',
    'https://api.uhc.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "medicareAdvantage": true, "partD": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Humana Medicare Advantage
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'HUMANA_MA001', 'Humana Medicare Advantage',
    'https://api.humana.com/medicare/fhir/r4',
    'https://api.humana.com/medicare/prior-auth/v1/Claim/$submit',
    'https://api.humana.com/medicare/provider-access/v1',
    'https://api.humana.com/medicare/provider-directory/v1',
    'https://api.humana.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "medicareAdvantage": true, "partD": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- MEDICAID PLANS (State Examples)
-- =============================================================================

-- California Medicaid (Medi-Cal)
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'MEDI_CAL', 'California Medi-Cal',
    'https://api.dhcs.ca.gov/fhir/r4',
    'https://api.dhcs.ca.gov/prior-auth/v1/Claim/$submit',
    'https://api.dhcs.ca.gov/provider-access/v1',
    'https://api.dhcs.ca.gov/provider-directory/v1',
    'https://api.dhcs.ca.gov/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, FALSE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "medicaid": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- New York Medicaid
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'NY_MEDICAID', 'New York Medicaid',
    'https://api.health.ny.gov/fhir/r4',
    'https://api.health.ny.gov/prior-auth/v1/Claim/$submit',
    'https://api.health.ny.gov/provider-access/v1',
    'https://api.health.ny.gov/provider-directory/v1',
    'https://api.health.ny.gov/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, FALSE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "medicaid": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Texas Medicaid
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'TX_MEDICAID', 'Texas Medicaid',
    'https://api.hhsc.texas.gov/fhir/r4',
    'https://api.hhsc.texas.gov/prior-auth/v1/Claim/$submit',
    'https://api.hhsc.texas.gov/provider-access/v1',
    'https://api.hhsc.texas.gov/provider-directory/v1',
    'https://api.hhsc.texas.gov/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, FALSE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "medicaid": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- REGIONAL BCBS PLANS
-- =============================================================================

-- Blue Cross Blue Shield of Massachusetts
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'BCBS_MA', 'Blue Cross Blue Shield of Massachusetts',
    'https://api.bluecrossma.com/fhir/r4',
    'https://api.bluecrossma.com/prior-auth/v1/Claim/$submit',
    'https://api.bluecrossma.com/provider-access/v1',
    'https://api.bluecrossma.com/provider-directory/v1',
    'https://api.bluecrossma.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "drugFormulary": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Blue Cross Blue Shield of Texas
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'BCBS_TX', 'Blue Cross Blue Shield of Texas',
    'https://api.bcbstx.com/fhir/r4',
    'https://api.bcbstx.com/prior-auth/v1/Claim/$submit',
    'https://api.bcbstx.com/provider-access/v1',
    'https://api.bcbstx.com/provider-directory/v1',
    'https://api.bcbstx.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "drugFormulary": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- Blue Cross Blue Shield of Michigan
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'BCBS_MI', 'Blue Cross Blue Shield of Michigan',
    'https://api.bcbsm.com/fhir/r4',
    'https://api.bcbsm.com/prior-auth/v1/Claim/$submit',
    'https://api.bcbsm.com/provider-access/v1',
    'https://api.bcbsm.com/provider-directory/v1',
    'https://api.bcbsm.com/oauth2/token',
    'OAUTH2_CLIENT_CREDENTIALS',
    'system/Patient.read system/Claim.write system/ClaimResponse.read',
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "providerAccess": true, "drugFormulary": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- DEMO/TEST PAYERS (Sandbox environments for development)
-- =============================================================================

-- Da Vinci Reference Implementation
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services, additional_headers
) VALUES (
    'DAVINCI_RI', 'Da Vinci Reference Implementation',
    'https://prior-auth-ri.davinci.hl7.org/fhir',
    'https://prior-auth-ri.davinci.hl7.org/fhir/Claim/$submit',
    'https://prior-auth-ri.davinci.hl7.org/fhir',
    'https://prior-auth-ri.davinci.hl7.org/fhir',
    NULL,
    'API_KEY',
    NULL,
    TRUE, TRUE, TRUE,
    '{"priorAuth": true, "testEnvironment": true}'::jsonb,
    '{"X-API-Key": "demo-api-key"}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

-- HAPI FHIR Test Server
INSERT INTO prior_auth.payer_endpoints (
    payer_id, payer_name, pa_fhir_base_url, pa_endpoint_url, provider_access_endpoint_url,
    provider_directory_url, token_endpoint_url, auth_type, scope, is_active,
    supports_real_time, supports_batch, supported_services
) VALUES (
    'HAPI_TEST', 'HAPI FHIR Test Server',
    'https://hapi.fhir.org/baseR4',
    'https://hapi.fhir.org/baseR4/Claim/$submit',
    'https://hapi.fhir.org/baseR4',
    'https://hapi.fhir.org/baseR4',
    NULL,
    'API_KEY',
    NULL,
    TRUE, FALSE, TRUE,
    '{"priorAuth": true, "testEnvironment": true, "publicServer": true}'::jsonb
) ON CONFLICT (payer_id) DO UPDATE SET updated_at = CURRENT_TIMESTAMP;

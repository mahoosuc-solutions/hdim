/**
 * mTLS configuration for HDIM k6 load tests against the demo environment.
 *
 * The demo services use mutual TLS (client + server certificates signed by
 * hdim-local-ca). k6 must present a valid client certificate to connect.
 *
 * Certificate files are baked into the k6 `options.tlsAuth` array as strings
 * read from environment variables (TLS_CA_CERT, TLS_CLIENT_CERT, TLS_CLIENT_KEY).
 *
 * Usage — pass cert contents as env vars:
 *   export TLS_CA_CERT=$(cat /etc/ssl/mtls/ca-cert.pem)
 *   export TLS_CLIENT_CERT=$(cat /etc/ssl/mtls/gateway-fhir-service/certificate.pem)
 *   export TLS_CLIENT_KEY=$(cat /etc/ssl/mtls/gateway-fhir-service/private-key.pem)
 *   k6 run -e TLS_CA_CERT="$TLS_CA_CERT" -e TLS_CLIENT_CERT="$TLS_CLIENT_CERT" \
 *          -e TLS_CLIENT_KEY="$TLS_CLIENT_KEY" scenarios/patient-service.js
 *
 * When TLS_CA_CERT is not set, this config is a no-op (plain HTTP or external TLS).
 */

/**
 * Returns k6 TLS options for mTLS connections to the HDIM demo services.
 *
 * @returns {Object} k6-compatible tls options block, or {} if certs not configured
 */
export function getTlsOptions() {
  const caCert     = __ENV.TLS_CA_CERT     || '';
  const clientCert = __ENV.TLS_CLIENT_CERT || '';
  const clientKey  = __ENV.TLS_CLIENT_KEY  || '';

  if (!caCert || !clientCert || !clientKey) {
    return {};
  }

  return {
    // Trust the HDIM local CA (self-signed in demo environment)
    tlsAuth: [
      {
        cert: clientCert,
        key:  clientKey,
        // Applies to all demo service hosts on their standard ports
        domains: [
          'localhost',
          '127.0.0.1',
        ],
      },
    ],
    // Explicitly allow the self-signed HDIM CA cert
    insecureSkipTLSVerify: false,
  };
}

/**
 * Returns gateway-trust headers that bypass JWT validation in dev/demo mode.
 *
 * In demo mode (GATEWAY_AUTH_DEV_MODE=true), backend services accept any
 * X-Auth-Validated header that starts with "gateway-". They then trust
 * X-Auth-User-Id, X-Auth-Username, X-Auth-Tenant-Ids, and X-Auth-Roles.
 *
 * This is the equivalent of what the gateway-edge nginx proxy injects after
 * validating the JWT.
 *
 * @param {string} [tenantId] - Tenant to authenticate as (default: acme-health)
 * @returns {Object} HTTP headers for authenticated HDIM API requests
 */
export function getDemoAuthHeaders(tenantId) {
  const tenant = tenantId || __ENV.TENANT_ID || 'acme-health';

  return {
    'X-Auth-Validated':  'gateway-load-test',
    'X-Auth-User-Id':    '550e8400-e29b-41d4-a716-446655440010',
    'X-Auth-Username':   'demo_admin',
    'X-Auth-Tenant-Ids': tenant,
    'X-Auth-Roles':      'ADMIN,EVALUATOR',
    'X-Tenant-ID':       tenant,
    'Content-Type':      'application/json',
    'Accept':            'application/json',
  };
}

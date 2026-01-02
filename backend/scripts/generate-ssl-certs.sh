#!/bin/bash

# SSL Certificate Generation Script for HealthData-in-Motion
#
# This script generates SSL certificates for development, staging, and production environments.
#
# Usage:
#   ./generate-ssl-certs.sh [dev|staging|prod] [domain]
#
# Examples:
#   ./generate-ssl-certs.sh dev localhost
#   ./generate-ssl-certs.sh staging staging.healthdata.example.com
#   ./generate-ssl-certs.sh prod api.healthdata.example.com

set -e

ENVIRONMENT=${1:-dev}
DOMAIN=${2:-localhost}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
SSL_DIR="${PROJECT_ROOT}/ssl"
CERT_DIR="${SSL_DIR}/${ENVIRONMENT}"

echo "==================================="
echo "SSL Certificate Generation"
echo "==================================="
echo "Environment: ${ENVIRONMENT}"
echo "Domain: ${DOMAIN}"
echo "Output Directory: ${CERT_DIR}"
echo ""

# Create SSL directory structure
mkdir -p "${CERT_DIR}"

case ${ENVIRONMENT} in
  dev|development)
    echo "🔧 Generating self-signed certificate for development..."

    # Generate private key
    openssl genrsa -out "${CERT_DIR}/private-key.pem" 4096
    echo "✅ Private key generated: ${CERT_DIR}/private-key.pem"

    # Generate certificate signing request (CSR)
    openssl req -new \
      -key "${CERT_DIR}/private-key.pem" \
      -out "${CERT_DIR}/csr.pem" \
      -subj "/C=US/ST=Development/L=Local/O=HealthData/OU=Development/CN=${DOMAIN}" \
      -addext "subjectAltName=DNS:${DOMAIN},DNS:*.${DOMAIN},DNS:localhost,IP:127.0.0.1"
    echo "✅ CSR generated: ${CERT_DIR}/csr.pem"

    # Generate self-signed certificate (valid for 1 year)
    openssl x509 -req \
      -days 365 \
      -in "${CERT_DIR}/csr.pem" \
      -signkey "${CERT_DIR}/private-key.pem" \
      -out "${CERT_DIR}/certificate.pem" \
      -extensions v3_req \
      -extfile <(cat <<EOF
[v3_req]
subjectAltName = DNS:${DOMAIN},DNS:*.${DOMAIN},DNS:localhost,IP:127.0.0.1
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
EOF
)
    echo "✅ Certificate generated: ${CERT_DIR}/certificate.pem"

    # Create PKCS12 keystore for Java applications
    KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-changeit}
    openssl pkcs12 -export \
      -in "${CERT_DIR}/certificate.pem" \
      -inkey "${CERT_DIR}/private-key.pem" \
      -out "${CERT_DIR}/keystore.p12" \
      -name healthdata-${ENVIRONMENT} \
      -passout pass:${KEYSTORE_PASSWORD}
    echo "✅ PKCS12 keystore generated: ${CERT_DIR}/keystore.p12"

    # Create combined PEM file for nginx
    cat "${CERT_DIR}/certificate.pem" "${CERT_DIR}/private-key.pem" > "${CERT_DIR}/combined.pem"
    echo "✅ Combined PEM generated: ${CERT_DIR}/combined.pem"

    ;;

  staging)
    echo "🔧 Generating self-signed certificate for staging..."
    echo ""
    echo "⚠️  For staging environments, you should use a certificate from a trusted CA."
    echo "    This script will generate a self-signed certificate for testing purposes."
    echo ""

    # Generate private key
    openssl genrsa -out "${CERT_DIR}/private-key.pem" 4096
    echo "✅ Private key generated"

    # Generate CSR with SAN
    openssl req -new \
      -key "${CERT_DIR}/private-key.pem" \
      -out "${CERT_DIR}/csr.pem" \
      -subj "/C=US/ST=State/L=City/O=HealthData/OU=Staging/CN=${DOMAIN}" \
      -addext "subjectAltName=DNS:${DOMAIN},DNS:*.${DOMAIN}"
    echo "✅ CSR generated (you can submit this to a CA)"

    # Generate self-signed certificate (valid for 90 days)
    openssl x509 -req \
      -days 90 \
      -in "${CERT_DIR}/csr.pem" \
      -signkey "${CERT_DIR}/private-key.pem" \
      -out "${CERT_DIR}/certificate.pem" \
      -extensions v3_req \
      -extfile <(cat <<EOF
[v3_req]
subjectAltName = DNS:${DOMAIN},DNS:*.${DOMAIN}
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
EOF
)
    echo "✅ Self-signed certificate generated"

    # Create PKCS12 keystore
    KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-changeit}
    openssl pkcs12 -export \
      -in "${CERT_DIR}/certificate.pem" \
      -inkey "${CERT_DIR}/private-key.pem" \
      -out "${CERT_DIR}/keystore.p12" \
      -name healthdata-${ENVIRONMENT} \
      -passout pass:${KEYSTORE_PASSWORD}
    echo "✅ PKCS12 keystore generated"

    # Create combined PEM
    cat "${CERT_DIR}/certificate.pem" "${CERT_DIR}/private-key.pem" > "${CERT_DIR}/combined.pem"
    echo "✅ Combined PEM generated"

    ;;

  prod|production)
    echo "🔐 Production SSL Certificate Setup"
    echo ""
    echo "For production, you have two options:"
    echo ""
    echo "Option 1: Let's Encrypt (Recommended for public-facing services)"
    echo "  - Install certbot: sudo apt-get install certbot python3-certbot-nginx"
    echo "  - Run: sudo certbot certonly --nginx -d ${DOMAIN}"
    echo "  - Certificates will be in: /etc/letsencrypt/live/${DOMAIN}/"
    echo ""
    echo "Option 2: Commercial CA Certificate"
    echo "  1. Generate CSR (this script will do it)"
    echo "  2. Submit CSR to your CA (DigiCert, Sectigo, etc.)"
    echo "  3. Receive certificate from CA"
    echo "  4. Place certificate files in: ${CERT_DIR}"
    echo ""

    read -p "Do you want to generate a CSR for a commercial CA? (y/n) " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
      # Generate private key
      openssl genrsa -out "${CERT_DIR}/private-key.pem" 4096
      echo "✅ Private key generated"

      # Generate CSR
      openssl req -new \
        -key "${CERT_DIR}/private-key.pem" \
        -out "${CERT_DIR}/csr.pem" \
        -subj "/C=US/ST=State/L=City/O=HealthData Inc/OU=IT/CN=${DOMAIN}" \
        -addext "subjectAltName=DNS:${DOMAIN},DNS:*.${DOMAIN}"
      echo "✅ CSR generated: ${CERT_DIR}/csr.pem"
      echo ""
      echo "📋 Submit this CSR to your certificate authority:"
      cat "${CERT_DIR}/csr.pem"
      echo ""
      echo "After receiving the certificate from your CA:"
      echo "  1. Save it as: ${CERT_DIR}/certificate.pem"
      echo "  2. Save the CA bundle as: ${CERT_DIR}/ca-bundle.pem"
      echo "  3. Run this script again to create the keystore"
    else
      echo "❌ Production certificate setup cancelled"
      echo ""
      echo "For Let's Encrypt, run:"
      echo "  sudo certbot certonly --standalone -d ${DOMAIN}"
      echo ""
      echo "Then create keystore:"
      echo "  openssl pkcs12 -export \\"
      echo "    -in /etc/letsencrypt/live/${DOMAIN}/fullchain.pem \\"
      echo "    -inkey /etc/letsencrypt/live/${DOMAIN}/privkey.pem \\"
      echo "    -out ${CERT_DIR}/keystore.p12 \\"
      echo "    -name healthdata-production"
    fi

    ;;

  *)
    echo "❌ Invalid environment: ${ENVIRONMENT}"
    echo "Usage: $0 [dev|staging|prod] [domain]"
    exit 1
    ;;
esac

# Display certificate information
if [ -f "${CERT_DIR}/certificate.pem" ]; then
  echo ""
  echo "==================================="
  echo "Certificate Information"
  echo "==================================="
  openssl x509 -in "${CERT_DIR}/certificate.pem" -noout -text | grep -A 2 "Validity\|Subject:\|DNS:"
  echo ""
fi

# Set appropriate permissions
chmod 600 "${CERT_DIR}"/*.pem 2>/dev/null || true
chmod 600 "${CERT_DIR}"/*.p12 2>/dev/null || true

echo ""
echo "==================================="
echo "✅ Certificate generation complete!"
echo "==================================="
echo ""
echo "Certificate files:"
ls -lh "${CERT_DIR}/"
echo ""
echo "Next steps:"
echo "  1. Update .env.production with keystore password"
echo "  2. Configure Spring Boot to use ${CERT_DIR}/keystore.p12"
echo "  3. Configure Nginx to use ${CERT_DIR}/certificate.pem and ${CERT_DIR}/private-key.pem"
echo "  4. Test SSL configuration: openssl s_client -connect ${DOMAIN}:443"
echo ""
echo "⚠️  SECURITY REMINDER:"
echo "  - Never commit private keys to version control"
echo "  - Add ssl/ directory to .gitignore"
echo "  - Use proper file permissions (600 for private keys)"
echo "  - Rotate certificates before expiration"
echo ""

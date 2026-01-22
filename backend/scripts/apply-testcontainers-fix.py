#!/usr/bin/env python3
"""
Apply Testcontainers fix to all services with application-test.yml
Replaces Testcontainers JDBC URLs with Docker PostgreSQL connections
"""

import os
import re
from pathlib import Path

# Service name to database name mapping
SERVICE_DB_MAPPING = {
    "agent-builder-service": "agent_db",
    "agent-runtime-service": "agent_runtime_db",
    "ai-assistant-service": "ai_assistant_db",
    "analytics-service": "analytics_db",
    "approval-service": "approval_db",
    "care-gap-service": "caregap_db",
    "cdr-processor-service": "cdr_db",
    "cms-connector-service": "gateway_db",  # Shares gateway DB
    "consent-service": "consent_db",
    "cql-engine-service": "cql_db",
    "data-enrichment-service": "enrichment_db",
    "demo-seeding-service": "healthdata_db",  # Uses default DB
    "documentation-service": "docs_db",
    "ecr-service": "ecr_db",
    "ehr-connector-service": "ehr_connector_db",
    "event-processing-service": "event_db",
    "event-router-service": "event_router_db",
    "fhir-service": "fhir_db",
    "gateway-service": "gateway_db",
    "hcc-service": "hcc_db",
    "migration-workflow-service": "migration_db",
    "notification-service": "notification_db",
    "patient-service": "patient_db",
    "payer-workflows-service": "payer_db",
    "predictive-analytics-service": "predictive_db",
    "prior-auth-service": "prior_auth_db",
    "qrda-export-service": "qrda_db",
    "quality-measure-service": "quality_db",
    "sales-automation-service": "sales_automation_db",
    "sdoh-service": "sdoh_db",
}

# Docker PostgreSQL credentials
DOCKER_USER = "healthdata"
DOCKER_PASSWORD = "healthdata_password"
DOCKER_HOST = "localhost"
DOCKER_PORT = "5435"


def fix_application_test_yml(file_path: Path, db_name: str) -> bool:
    """
    Fix application-test.yml to use Docker PostgreSQL instead of Testcontainers

    Returns True if file was modified, False otherwise
    """
    try:
        with open(file_path, 'r') as f:
            content = f.read()

        original_content = content

        # Replace Testcontainers JDBC URL pattern
        # Pattern: jdbc:tc:postgresql:XX-alpine:///testdb?TC_STARTUP_TIMEOUT=XXX
        content = re.sub(
            r'jdbc:tc:postgresql:[^/]+///[^?\s]+(?:\?[^\s]*)?',
            f'jdbc:postgresql://{DOCKER_HOST}:{DOCKER_PORT}/{db_name}',
            content
        )

        # Replace driver class
        content = content.replace(
            'org.testcontainers.jdbc.ContainerDatabaseDriver',
            'org.postgresql.Driver'
        )

        # Replace username (handle both 'sa' and 'test')
        content = re.sub(
            r'username:\s+(?:sa|test)\s*$',
            f'username: {DOCKER_USER}',
            content,
            flags=re.MULTILINE
        )

        # Replace empty password
        content = re.sub(
            r'password:\s*(?:""|\'\'|\s*$)',
            f'password: {DOCKER_PASSWORD}',
            content,
            flags=re.MULTILINE
        )

        # Replace create-drop with validate
        content = re.sub(
            r'ddl-auto:\s+create-drop',
            'ddl-auto: validate',
            content
        )

        # Enable Liquibase if disabled
        content = re.sub(
            r'enabled:\s+false\s*#\s*Liquibase',
            'enabled: true\n    change-log: classpath:db/changelog/db.changelog-master.xml',
            content
        )
        content = re.sub(
            r'enabled:\s+false\s*$',
            lambda m: 'enabled: true' if 'liquibase:' in content[max(0, content.rfind('\n', 0, m.start())-50):m.start()] else m.group(0),
            content,
            flags=re.MULTILINE
        )

        # Add database-config hikari traffic-tier if healthdata section exists but no database.hikari.traffic-tier
        if 'healthdata:' in content and 'traffic-tier:' not in content:
            # Find healthdata: section and add database.hikari.traffic-tier after it
            content = re.sub(
                r'(healthdata:\s*\n)',
                r'\1  database:\n    enabled: true\n    hikari:\n      traffic-tier: LOW  # Minimal connections for tests\n',
                content,
                count=1
            )

        # Only write if content changed
        if content != original_content:
            with open(file_path, 'w') as f:
                f.write(content)
            return True
        return False

    except Exception as e:
        print(f"  ❌ Error processing {file_path}: {e}")
        return False


def main():
    backend_dir = Path(__file__).parent.parent
    services_dir = backend_dir / "modules" / "services"

    print("🔧 Applying Testcontainers Fix to All Services")
    print("=" * 60)
    print(f"Docker PostgreSQL: {DOCKER_HOST}:{DOCKER_PORT}")
    print(f"Credentials: {DOCKER_USER} / {DOCKER_PASSWORD}")
    print("=" * 60)
    print()

    fixed_count = 0
    skipped_count = 0
    error_count = 0

    for service_name, db_name in sorted(SERVICE_DB_MAPPING.items()):
        config_file = services_dir / service_name / "src" / "test" / "resources" / "application-test.yml"

        if not config_file.exists():
            print(f"⏭️  {service_name:35} - No test config found")
            skipped_count += 1
            continue

        print(f"🔄 {service_name:35} → {db_name}", end=" ... ")

        if fix_application_test_yml(config_file, db_name):
            print("✅ FIXED")
            fixed_count += 1
        else:
            print("⏭️  SKIPPED (no changes needed)")
            skipped_count += 1

    print()
    print("=" * 60)
    print(f"✅ Fixed:   {fixed_count}")
    print(f"⏭️  Skipped: {skipped_count}")
    print(f"❌ Errors:  {error_count}")
    print("=" * 60)
    print()
    print("Next steps:")
    print("1. Run comprehensive test suite: ./gradlew test --continue --no-daemon")
    print("2. Check test pass rate (target: ≥95%)")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
Comment out systemProperty() calls in build.gradle.kts files
These override application-test.yml and prevent Testcontainers fix from working
"""

import os
import re
from pathlib import Path


def fix_build_gradle(file_path: Path) -> bool:
    """
    Comment out systemProperty() calls for spring.datasource.*

    Returns True if file was modified, False otherwise
    """
    try:
        with open(file_path, 'r') as f:
            content = f.read()

        original_content = content

        # Comment out systemProperty lines for spring.datasource configuration
        # Pattern: systemProperty("spring.datasource.url", "jdbc:tc:postgresql...")
        # Pattern: systemProperty("spring.datasource.username", "test")
        # Pattern: systemProperty("spring.datasource.password", "test")
        # Pattern: systemProperty("spring.datasource.driver-class-name", "org.testcontainers...")
        # Pattern: systemProperty("spring.jpa.properties.hibernate.dialect", "...")

        lines = content.split('\n')
        modified = False
        new_lines = []

        for line in lines:
            # Check if line contains systemProperty for database config
            if ('systemProperty' in line and
                ('spring.datasource' in line or
                 'spring.jpa.properties.hibernate.dialect' in line or
                 'ContainerDatabaseDriver' in line)):
                # Don't comment out spring.profiles.active
                if 'spring.profiles.active' not in line:
                    # Comment out the line
                    stripped = line.lstrip()
                    indent = line[:len(line) - len(stripped)]
                    new_lines.append(f"{indent}// {stripped}")
                    modified = True
                else:
                    new_lines.append(line)
            else:
                new_lines.append(line)

        if modified:
            # Add comment header if not already present
            comment_header = "    // Testcontainers system properties disabled - using running Docker PostgreSQL"
            comment_note = "    // Configuration now managed in src/test/resources/application-test.yml"

            new_content = '\n'.join(new_lines)

            # Find the tasks.withType<Test> block and add comment if not present
            if 'tasks.withType<Test>' in new_content and comment_header not in new_content:
                new_content = new_content.replace(
                    'tasks.withType<Test> {',
                    f'tasks.withType<Test> {{\n{comment_header}\n{comment_note}'
                )

            with open(file_path, 'w') as f:
                f.write(new_content)
            return True

        return False

    except Exception as e:
        print(f"  ❌ Error processing {file_path}: {e}")
        return False


def main():
    backend_dir = Path(__file__).parent.parent
    services_dir = backend_dir / "modules" / "services"

    print("🔧 Fixing build.gradle.kts systemProperty() Overrides")
    print("=" * 60)
    print()

    fixed_count = 0
    skipped_count = 0

    # Get all services with build.gradle.kts
    for service_dir in sorted(services_dir.iterdir()):
        if not service_dir.is_dir():
            continue

        build_file = service_dir / "build.gradle.kts"
        if not build_file.exists():
            continue

        service_name = service_dir.name

        # Check if it has systemProperty for datasource
        with open(build_file, 'r') as f:
            content = f.read()
            if 'systemProperty' not in content or 'spring.datasource' not in content:
                continue

        print(f"🔄 {service_name:35}", end=" ... ")

        if fix_build_gradle(build_file):
            print("✅ FIXED")
            fixed_count += 1
        else:
            print("⏭️  SKIPPED (no changes needed)")
            skipped_count += 1

    print()
    print("=" * 60)
    print(f"✅ Fixed:   {fixed_count}")
    print(f"⏭️  Skipped: {skipped_count}")
    print("=" * 60)


if __name__ == "__main__":
    main()

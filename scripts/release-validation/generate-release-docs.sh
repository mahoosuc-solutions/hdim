#!/bin/bash
# Auto-generates release documentation from templates
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

VERSION="${1:-}"
if [ -z "$VERSION" ]; then
    echo -e "${RED}ERROR: VERSION required${NC}"
    echo "Usage: $0 v1.3.0"
    exit 1
fi

echo "=========================================="
echo "Generating Release Documentation"
echo "Version: $VERSION"
echo "=========================================="
echo ""

cd "$(dirname "$0")/../.." || exit 1

# Create release directory
RELEASE_DIR="docs/releases/${VERSION}"
TEMPLATE_DIR="docs/releases/templates"
mkdir -p "$RELEASE_DIR"

echo "Release directory: $RELEASE_DIR"
echo ""

# Extract version without 'v' prefix
VERSION_NUMBER="${VERSION#v}"
DATE=$(date '+%Y-%m-%d')
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Determine previous version from git tags
PREVIOUS_VERSION=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "v1.0.0")

echo "Previous version: $PREVIOUS_VERSION"
echo ""

# --- Generate RELEASE_NOTES ---
echo -e "${GREEN}Generating RELEASE_NOTES_${VERSION}.md...${NC}"

cp "$TEMPLATE_DIR/RELEASE_NOTES_TEMPLATE.md" "$RELEASE_DIR/RELEASE_NOTES_${VERSION}.md"

sed -i "s/{VERSION}/$VERSION/g" "$RELEASE_DIR/RELEASE_NOTES_${VERSION}.md"
sed -i "s/{DATE}/$DATE/g" "$RELEASE_DIR/RELEASE_NOTES_${VERSION}.md"
sed -i "s/{PREVIOUS_VERSION}/$PREVIOUS_VERSION/g" "$RELEASE_DIR/RELEASE_NOTES_${VERSION}.md"

# Auto-generate feature list from git log
GIT_FEATURES=$(git log ${PREVIOUS_VERSION}..HEAD --oneline --grep="^feat:" --format="- %s" 2>/dev/null || echo "- See git history for features")
if [ -z "$GIT_FEATURES" ]; then
    GIT_FEATURES="- See git history for features since $PREVIOUS_VERSION"
fi
# Use temporary file to avoid sed escaping issues
echo "$GIT_FEATURES" > /tmp/git-features-$$.txt
sed -i "/{AUTO_GENERATED_FROM_GIT_LOG}/{
r /tmp/git-features-$$.txt
d
}" "$RELEASE_DIR/RELEASE_NOTES_${VERSION}.md"
rm -f /tmp/git-features-$$.txt

# Auto-generate contributors
GIT_CONTRIBUTORS=$(git log ${PREVIOUS_VERSION}..HEAD --format="- %an <%ae>" 2>/dev/null | sort -u | head -10)
if [ -z "$GIT_CONTRIBUTORS" ]; then
    GIT_CONTRIBUTORS="- See git history for contributors since $PREVIOUS_VERSION"
fi
# Use temporary file to avoid sed escaping issues
echo "$GIT_CONTRIBUTORS" > /tmp/git-contributors-$$.txt
sed -i "/{AUTO_GENERATED_FROM_GIT_CONTRIBUTORS}/{
r /tmp/git-contributors-$$.txt
d
}" "$RELEASE_DIR/RELEASE_NOTES_${VERSION}.md"
rm -f /tmp/git-contributors-$$.txt

echo -e "${GREEN}✓${NC} RELEASE_NOTES_${VERSION}.md created"

# --- Generate UPGRADE_GUIDE ---
echo -e "${GREEN}Generating UPGRADE_GUIDE_${VERSION}.md...${NC}"

cp "$TEMPLATE_DIR/UPGRADE_GUIDE_TEMPLATE.md" "$RELEASE_DIR/UPGRADE_GUIDE_${VERSION}.md"

sed -i "s/{VERSION}/$VERSION/g" "$RELEASE_DIR/UPGRADE_GUIDE_${VERSION}.md"
sed -i "s/{PREVIOUS_VERSION}/$PREVIOUS_VERSION/g" "$RELEASE_DIR/UPGRADE_GUIDE_${VERSION}.md"
sed -i "s/{DATE}/$DATE/g" "$RELEASE_DIR/UPGRADE_GUIDE_${VERSION}.md"
sed -i "s/{ESTIMATED_TIME}/2-4 hours/g" "$RELEASE_DIR/UPGRADE_GUIDE_${VERSION}.md"
sed -i "s/{DOWNTIME}/30-60 minutes/g" "$RELEASE_DIR/UPGRADE_GUIDE_${VERSION}.md"

# Count Liquibase changesets
CHANGESET_COUNT=$(find backend/modules/services -name "db.changelog-master.xml" -exec grep -c "<include" {} \; | awk '{s+=$1} END {print s}' || echo "Unknown")
sed -i "s/{EXPECTED_MIGRATION_COUNT}/$CHANGESET_COUNT/g" "$RELEASE_DIR/UPGRADE_GUIDE_${VERSION}.md"

echo -e "${GREEN}✓${NC} UPGRADE_GUIDE_${VERSION}.md created"

# --- Generate VERSION_MATRIX ---
echo -e "${GREEN}Generating VERSION_MATRIX_${VERSION}.md...${NC}"

cp "$TEMPLATE_DIR/VERSION_MATRIX_TEMPLATE.md" "$RELEASE_DIR/VERSION_MATRIX_${VERSION}.md"

sed -i "s/{VERSION}/$VERSION/g" "$RELEASE_DIR/VERSION_MATRIX_${VERSION}.md"
sed -i "s/{DATE}/$DATE/g" "$RELEASE_DIR/VERSION_MATRIX_${VERSION}.md"
sed -i "s/{TIMESTAMP}/$TIMESTAMP/g" "$RELEASE_DIR/VERSION_MATRIX_${VERSION}.md"

# Extract versions from gradle/libs.versions.toml
if [ -f "backend/gradle/libs.versions.toml" ]; then
    SPRING_BOOT_VERSION=$(grep "springBoot" backend/gradle/libs.versions.toml | awk -F'"' '{print $2}' | head -1 || echo "3.3.6")
    HAPI_FHIR_VERSION=$(grep "hapiFhir" backend/gradle/libs.versions.toml | awk -F'"' '{print $2}' | head -1 || echo "7.0.0")
    LIQUIBASE_VERSION=$(grep "liquibase" backend/gradle/libs.versions.toml | awk -F'"' '{print $2}' | head -1 || echo "4.29.2")

    sed -i "s/{SPRING_BOOT_VERSION}/$SPRING_BOOT_VERSION/g" "$RELEASE_DIR/VERSION_MATRIX_${VERSION}.md"
    sed -i "s/{HAPI_FHIR_VERSION}/$HAPI_FHIR_VERSION/g" "$RELEASE_DIR/VERSION_MATRIX_${VERSION}.md"
    sed -i "s/{LIQUIBASE_VERSION}/$LIQUIBASE_VERSION/g" "$RELEASE_DIR/VERSION_MATRIX_${VERSION}.md"
fi

echo -e "${GREEN}✓${NC} VERSION_MATRIX_${VERSION}.md created"

# --- Generate PRODUCTION_DEPLOYMENT_CHECKLIST ---
echo -e "${GREEN}Generating PRODUCTION_DEPLOYMENT_CHECKLIST_${VERSION}.md...${NC}"

cp "$TEMPLATE_DIR/PRODUCTION_DEPLOYMENT_CHECKLIST_TEMPLATE.md" "$RELEASE_DIR/PRODUCTION_DEPLOYMENT_CHECKLIST_${VERSION}.md"

sed -i "s/{VERSION}/$VERSION/g" "$RELEASE_DIR/PRODUCTION_DEPLOYMENT_CHECKLIST_${VERSION}.md"
sed -i "s/{DATE}/$DATE/g" "$RELEASE_DIR/PRODUCTION_DEPLOYMENT_CHECKLIST_${VERSION}.md"
sed -i "s/{START_TIME}/02:00 AM/g" "$RELEASE_DIR/PRODUCTION_DEPLOYMENT_CHECKLIST_${VERSION}.md"
sed -i "s/{END_TIME}/06:00 AM/g" "$RELEASE_DIR/PRODUCTION_DEPLOYMENT_CHECKLIST_${VERSION}.md"
sed -i "s/{EXPECTED_MIGRATION_COUNT}/$CHANGESET_COUNT/g" "$RELEASE_DIR/PRODUCTION_DEPLOYMENT_CHECKLIST_${VERSION}.md"

echo -e "${GREEN}✓${NC} PRODUCTION_DEPLOYMENT_CHECKLIST_${VERSION}.md created"

# --- Generate KNOWN_ISSUES ---
echo -e "${GREEN}Generating KNOWN_ISSUES_${VERSION}.md...${NC}"

cp "$TEMPLATE_DIR/KNOWN_ISSUES_TEMPLATE.md" "$RELEASE_DIR/KNOWN_ISSUES_${VERSION}.md"

sed -i "s/{VERSION}/$VERSION/g" "$RELEASE_DIR/KNOWN_ISSUES_${VERSION}.md"
sed -i "s/{DATE}/$DATE/g" "$RELEASE_DIR/KNOWN_ISSUES_${VERSION}.md"

echo -e "${GREEN}✓${NC} KNOWN_ISSUES_${VERSION}.md created"

# --- Generate SCOPE ---
if [ -f "$TEMPLATE_DIR/SCOPE_TEMPLATE.md" ]; then
    echo -e "${GREEN}Generating SCOPE_${VERSION}.md...${NC}"

    cp "$TEMPLATE_DIR/SCOPE_TEMPLATE.md" "$RELEASE_DIR/SCOPE_${VERSION}.md"

    sed -i "s/{VERSION}/$VERSION/g" "$RELEASE_DIR/SCOPE_${VERSION}.md"
    sed -i "s/{DATE}/$DATE/g" "$RELEASE_DIR/SCOPE_${VERSION}.md"
    sed -i "s/{PREVIOUS_VERSION}/$PREVIOUS_VERSION/g" "$RELEASE_DIR/SCOPE_${VERSION}.md"

    echo -e "${GREEN}✓${NC} SCOPE_${VERSION}.md created"
else
    echo -e "${YELLOW}Skipping SCOPE_${VERSION}.md (missing $TEMPLATE_DIR/SCOPE_TEMPLATE.md)${NC}"
fi

# --- Summary ---
echo ""
echo "=========================================="
echo "Documentation Generation Complete"
echo "=========================================="
echo ""
echo "Generated files in $RELEASE_DIR:"
ls -lh "$RELEASE_DIR"/*.md 2>/dev/null || echo "No markdown files found"

echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Review and customize generated documentation"
echo "2. Update placeholders with version-specific content"
echo "3. Run: ./scripts/release-validation/run-release-validation.sh $VERSION"

exit 0

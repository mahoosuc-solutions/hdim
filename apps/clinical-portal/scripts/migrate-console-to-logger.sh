#!/bin/bash

# Console.log to LoggerService Migration Script
# HIPAA Compliance: Migrate console statements to LoggerService for PHI filtering

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Console.log Migration Script${NC}"
echo -e "${GREEN}=============================${NC}"
echo ""

# Target directory
TARGET_DIR="${1:-apps/clinical-portal/src/app}"

if [ ! -d "$TARGET_DIR" ]; then
    echo -e "${RED}Error: Directory $TARGET_DIR does not exist${NC}"
    exit 1
fi

echo "Target directory: $TARGET_DIR"
echo ""

# Find files with console statements (excluding tests and logger service itself)
FILES_WITH_CONSOLE=$(find "$TARGET_DIR" -name "*.ts" \
    -not -name "*.spec.ts" \
    -not -name "*test.ts" \
    -not -name "logger.service.ts" \
    -exec grep -l "console\." {} \; 2>/dev/null || true)

if [ -z "$FILES_WITH_CONSOLE" ]; then
    echo -e "${GREEN}✓${NC} No console statements found! All clean."
    exit 0
fi

# Count files
FILE_COUNT=$(echo "$FILES_WITH_CONSOLE" | wc -l)
echo -e "${YELLOW}Found $FILE_COUNT files with console statements${NC}"
echo ""

# Generate migration report
REPORT_FILE="console-migration-report.md"
echo "# Console.log Migration Report" > "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "**Generated:** $(date)" >> "$REPORT_FILE"
echo "**Files to migrate:** $FILE_COUNT" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Process each file
echo "Generating migration report..."
FILE_NUM=0

echo "$FILES_WITH_CONSOLE" | while IFS= read -r file; do
    if [ -z "$file" ]; then
        continue
    fi

    FILE_NUM=$((FILE_NUM + 1))
    echo -e "  [$FILE_NUM/$FILE_COUNT] $file"

    # Get relative path
    REL_PATH="${file#$TARGET_DIR/}"

    # Add file section to report
    echo "## $FILE_NUM. $REL_PATH" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    # Extract console statements with line numbers
    CONSOLE_LINES=$(grep -n "console\." "$file" | head -20)

    # Count console statements
    CONSOLE_COUNT=$(echo "$CONSOLE_LINES" | wc -l)
    echo "**Console statements found:** $CONSOLE_COUNT" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    # Show examples
    echo "**Examples:**" >> "$REPORT_FILE"
    echo '```typescript' >> "$REPORT_FILE"
    echo "$CONSOLE_LINES" | head -10 >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    # Check if LoggerService is already imported
    if grep -q "LoggerService" "$file"; then
        echo "✅ **LoggerService already imported**" >> "$REPORT_FILE"
    else
        echo "❌ **Needs LoggerService import**" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "Add import:" >> "$REPORT_FILE"
        echo '```typescript' >> "$REPORT_FILE"
        echo "import { LoggerService } from '@app/services/logger.service';" >> "$REPORT_FILE"
        echo '```' >> "$REPORT_FILE"
    fi

    echo "" >> "$REPORT_FILE"

    # Provide migration guidance
    echo "**Migration pattern:**" >> "$REPORT_FILE"
    echo '```typescript' >> "$REPORT_FILE"
    echo "// Before:" >> "$REPORT_FILE"
    echo "console.log('message', data);" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "// After:" >> "$REPORT_FILE"
    echo "private logger = this.loggerService.withContext('ComponentName');" >> "$REPORT_FILE"
    echo "this.logger.info('message', data);" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "---" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

done

echo ""
echo -e "${GREEN}✓${NC} Migration report generated: $REPORT_FILE"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Review $REPORT_FILE for detailed migration guidance"
echo "2. For each file:"
echo "   a. Import LoggerService (if not already imported)"
echo "   b. Inject LoggerService in constructor"
echo "   c. Create logger instance with withContext()"
echo "   d. Replace console.log → logger.info()"
echo "   e. Replace console.warn → logger.warn()"
echo "   f. Replace console.error → logger.error()"
echo "3. Run 'npm run lint' to verify no console statements remain"
echo "4. Test the application to ensure logging works"
echo ""
echo -e "${YELLOW}HIPAA Compliance Note:${NC}"
echo "LoggerService automatically filters PHI in production environments."
echo "Console statements expose PHI to browser DevTools (HIPAA violation)."
echo ""

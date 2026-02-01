#!/bin/bash
#
# Script to migrate console.log/error/warn/debug statements to LoggerService
# HIPAA Compliance: Prevents PHI exposure through browser console
#
# Usage: ./migrate-console-to-logger.sh <directory>
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Target directory (default: clinical-portal)
TARGET_DIR="${1:-apps/clinical-portal/src/app}"

echo -e "${GREEN}Console.log Migration Script${NC}"
echo -e "${GREEN}=============================${NC}"
echo ""
echo "Target directory: $TARGET_DIR"
echo ""

# Find all TypeScript files with console statements (excluding tests, spec files, and logger.service itself)
FILES_WITH_CONSOLE=$(grep -rl "console\." "$TARGET_DIR" --include="*.ts" \
  --exclude="*spec.ts" \
  --exclude="*test.ts" \
  --exclude="logger.service.ts" \
  --exclude="global-error-handler.service.ts" \
  || true)

if [ -z "$FILES_WITH_CONSOLE" ]; then
  echo -e "${GREEN}✓${NC} No console statements found! All clean."
  exit 0
fi

# Count files
FILE_COUNT=$(echo "$FILES_WITH_CONSOLE" | wc -l)
echo -e "${YELLOW}Found $FILE_COUNT files with console statements${NC}"
echo ""

# Display files (first 20)
echo "Files to migrate:"
echo "$FILES_WITH_CONSOLE" | head -20
if [ "$FILE_COUNT" -gt 20 ]; then
  echo "... and $(( FILE_COUNT - 20 )) more"
fi
echo ""

# Ask for confirmation
read -p "Proceed with migration? (y/N) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "Migration cancelled."
  exit 0
fi

# Migration function
migrate_file() {
  local file="$1"
  local modified=false

  # Check if LoggerService is already imported
  if ! grep -q "import.*LoggerService" "$file"; then
    # Add LoggerService import after other service imports
    # Find last import line
    last_import_line=$(grep -n "^import" "$file" | tail -1 | cut -d: -f1)
    if [ -n "$last_import_line" ]; then
      sed -i "${last_import_line}a import { LoggerService } from '../../services/logger.service';" "$file"
      modified=true
    fi
  fi

  # Check if logger is already in constructor
  if ! grep -q "private.*logger.*LoggerService" "$file" && ! grep -q "private.*loggerService.*LoggerService" "$file"; then
    # Add logger to constructor
    # This is complex - better to do manually for each file
    # For now, just report it
    echo "  ⚠ Manual action needed: Add LoggerService to constructor"
    return 1
  fi

  # Replace console.log with logger.info
  if grep -q "console\.log" "$file"; then
    # This is too complex for sed - requires context awareness
    echo "  ⚠ Manual action needed: Replace console.log statements"
    return 1
  fi

  if [ "$modified" = true ]; then
    echo "  ✓ Added LoggerService import"
    return 0
  fi

  return 1
}

# Report summary
echo ""
echo -e "${YELLOW}Migration Strategy:${NC}"
echo "Due to the complexity of TypeScript syntax, automatic migration is risky."
echo "This script will generate a manual migration guide instead."
echo ""

# Generate migration guide
GUIDE_FILE="console-migration-guide.md"
cat > "$GUIDE_FILE" << 'EOF'
# Console.log Migration Guide

Generated: $(date)

## Files Requiring Migration

EOF

echo "$FILES_WITH_CONSOLE" | while read -r file; do
  if [ -z "$file" ]; then
    continue
  fi

  console_count=$(grep -c "console\." "$file" || echo "0")

  cat >> "$GUIDE_FILE" << EOF

### $file

**Console statements:** $console_count

**Occurrences:**
\`\`\`
$(grep -n "console\." "$file" | head -10)
\`\`\`

**Migration steps:**
1. Add \`import { LoggerService } from '../../services/logger.service';\`
2. Add to constructor: \`private loggerService: LoggerService\`
3. Create contextual logger: \`private logger = this.loggerService.withContext('ComponentName');\`
4. Replace:
   - \`console.log(...)\` → \`this.logger.info(...)\`
   - \`console.error(...)\` → \`this.logger.error(...)\`
   - \`console.warn(...)\` → \`this.logger.warn(...)\`
   - \`console.debug(...)\` → \`this.logger.debug(...)\`

---
EOF

done

echo -e "${GREEN}✓ Migration guide generated: $GUIDE_FILE${NC}"
echo ""
echo "Next steps:"
echo "1. Review the migration guide"
echo "2. Manually update each file"
echo "3. Run 'npm run lint' to verify no console statements remain"
echo "4. Run 'npm run build:prod' to verify production build"
echo ""

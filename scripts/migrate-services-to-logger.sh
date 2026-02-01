#!/bin/bash
#
# migrate-services-to-logger.sh
# Migrates Angular service files from console.* to LoggerService
#
# Usage: ./scripts/migrate-services-to-logger.sh <service-file-path>
#
# This script:
# 1. Adds LoggerService import
# 2. Injects LoggerService in constructor
# 3. Creates contextual logger instance
# 4. Replaces all console.log/error/warn/debug calls with logger equivalents

set -euo pipefail

SERVICE_FILE="${1:-}"

if [[ -z "$SERVICE_FILE" ]]; then
    echo "Usage: $0 <service-file-path>"
    echo "Example: $0 apps/clinical-portal/src/app/services/report-builder.service.ts"
    exit 1
fi

if [[ ! -f "$SERVICE_FILE" ]]; then
    echo "Error: File not found: $SERVICE_FILE"
    exit 1
fi

echo "Migrating $SERVICE_FILE to LoggerService..."

# Extract service class name from file
SERVICE_NAME=$(basename "$SERVICE_FILE" .service.ts | sed 's/-\([a-z]\)/\U\1/g' | sed 's/^./\U&/')
SERVICE_CLASS_NAME="${SERVICE_NAME}Service"

echo "  Service class: $SERVICE_CLASS_NAME"

# Create backup
cp "$SERVICE_FILE" "${SERVICE_FILE}.backup"

# Step 1: Add LoggerService import (if not exists)
if ! grep -q "import { LoggerService }" "$SERVICE_FILE"; then
    echo "  Adding LoggerService import..."
    # Find the last import statement and add LoggerService import after it
    sed -i '/^import.*from/a\
import { LoggerService } from '"'"'./logger.service'"'"';' "$SERVICE_FILE"
fi

# Step 2: Add logger field to class (if not exists)
if ! grep -q "private readonly logger" "$SERVICE_FILE"; then
    echo "  Adding logger field..."
    # Find the constructor or first field declaration
    sed -i "/export class $SERVICE_CLASS_NAME/a\\
  private readonly logger = this.loggerService.withContext('$SERVICE_CLASS_NAME');" "$SERVICE_FILE"
fi

# Step 3: Add loggerService to constructor (if not exists)
if ! grep -q "private loggerService: LoggerService" "$SERVICE_FILE"; then
    echo "  Adding LoggerService to constructor..."
    # Find constructor and add loggerService parameter
    sed -i 's/constructor(/constructor(\n    private loggerService: LoggerService,/g' "$SERVICE_FILE"
    # If constructor has no parameters, fix the syntax
    sed -i 's/constructor(\n    private loggerService: LoggerService,)/constructor(private loggerService: LoggerService)/g' "$SERVICE_FILE"
fi

# Step 4: Replace console.error calls
echo "  Replacing console.error calls..."
perl -i -pe 's/console\.error\((.*?),\s*error\)/this.logger.error($1, { error })/g' "$SERVICE_FILE"
perl -i -pe 's/console\.error\((.*?)\)/this.logger.error($1)/g' "$SERVICE_FILE"

# Step 5: Replace console.log calls
echo "  Replacing console.log calls..."
perl -i -pe 's/console\.log\((.*?)\)/this.logger.info($1)/g' "$SERVICE_FILE"

# Step 6: Replace console.warn calls
echo "  Replacing console.warn calls..."
perl -i -pe 's/console\.warn\((.*?)\)/this.logger.warn($1)/g' "$SERVICE_FILE"

# Step 7: Replace console.debug calls
echo "  Replacing console.debug calls..."
perl -i -pe 's/console\.debug\((.*?)\)/this.logger.debug($1)/g' "$SERVICE_FILE"

# Verify no console violations remain
CONSOLE_COUNT=$(grep -c "console\." "$SERVICE_FILE" || true)

if [[ $CONSOLE_COUNT -eq 0 ]]; then
    echo "✅ Migration complete! No console violations remaining."
    rm "${SERVICE_FILE}.backup"
else
    echo "⚠️  Warning: $CONSOLE_COUNT console violations still remain. Manual review required."
    echo "  Backup saved at: ${SERVICE_FILE}.backup"
fi

echo "Done!"

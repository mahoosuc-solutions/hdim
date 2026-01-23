#!/bin/bash

# Fix logger initialization issue in all TypeScript files
# Changes: private logger = this.loggerService.withContext(...)
# To: private get logger() { return this.loggerService.withContext(...) }

cd apps/clinical-portal

find src/app -type f -name "*.ts" -print0 | while IFS= read -r -d '' file; do
  if grep -q "private logger = this\.loggerService\.withContext" "$file"; then
    echo "Fixing: $file"

    # Use perl for multi-line replacement
    perl -i -0pe "s/private logger = this\.loggerService\.withContext\(([^)]+)\);/private get logger() {\n    return this.loggerService.withContext(\1);\n  }/g" "$file"
  fi
done

echo "Done! Fixed all logger initializations."

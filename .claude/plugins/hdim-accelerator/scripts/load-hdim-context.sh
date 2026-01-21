#!/bin/bash
# SessionStart hook: Load HDIM platform context and environment configuration
set -euo pipefail

# Read hook input (contains session info, cwd, etc.)
input=$(cat)

# Extract current working directory from hook input
cwd=$(echo "$input" | jq -r '.cwd // "/mnt/wdblack/dev/projects/hdim-master"')

# Detect HDIM project root (look for backend/ and docker/ directories)
project_root="$cwd"
if [ ! -d "$project_root/backend" ] || [ ! -d "$project_root/docker" ]; then
  # Not in HDIM root, try to find it
  if [ -d "$cwd/hdim-master" ]; then
    project_root="$cwd/hdim-master"
  elif [ -d "$cwd/../hdim-master" ]; then
    project_root="$cwd/../hdim-master"
  else
    # Fallback to known location
    project_root="/mnt/wdblack/dev/projects/hdim-master"
  fi
fi

# Persist environment variables using $CLAUDE_ENV_FILE
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  echo "export HDIM_PROJECT_ROOT=\"$project_root\"" >> "$CLAUDE_ENV_FILE"
  echo "export HDIM_BACKEND_ROOT=\"$project_root/backend\"" >> "$CLAUDE_ENV_FILE"
  echo "export HDIM_DOCKER_ROOT=\"$project_root/docker\"" >> "$CLAUDE_ENV_FILE"
  echo "export HDIM_DOCS_ROOT=\"$project_root/docs\"" >> "$CLAUDE_ENV_FILE"
fi

# Detect active services (check docker-compose)
active_services=""
if command -v docker &> /dev/null && docker compose ps &> /dev/null 2>&1; then
  active_services=$(docker compose ps --format json 2>/dev/null | jq -r '.[].Service' | tr '\n' ',' | sed 's/,$//')
fi

# Detect Java/Gradle versions
java_version=""
if command -v java &> /dev/null; then
  java_version=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
fi

gradle_version=""
if [ -f "$project_root/backend/gradlew" ]; then
  gradle_version=$("$project_root/backend/gradlew" --version 2>/dev/null | grep "Gradle" | awk '{print $2}')
fi

# Build context message for Claude
context_message="
╔══════════════════════════════════════════════════════════════╗
║ HDIM Platform - Session Context Loaded                      ║
╚══════════════════════════════════════════════════════════════╝

📁 Project Structure:
   Root:    $project_root
   Backend: $project_root/backend
   Docker:  $project_root/docker
   Docs:    $project_root/docs

🔧 Environment:
   Java:    ${java_version:-Not detected}
   Gradle:  ${gradle_version:-Not detected}

🐳 Active Services:
   ${active_services:-No services running}

📋 Available HDIM Accelerator Agents:
   • spring-boot-agent      - Spring Boot configuration validation
   • spring-security-agent  - Gateway trust authentication validation
   • redis-agent            - HIPAA-compliant cache validation
   • postgres-agent         - HikariCP & entity-migration validation
   • kafka-agent            - Event streaming & CQRS validation
   • docker-agent           - Container security & orchestration
   • gcp-agent              - Infrastructure as Code generation

⚡ Proactive Hooks Enabled:
   • PreToolUse:  Auto-detect file modifications, launch agents
   • PostToolUse: Validate changes after file writes
   • Stop:        Verify HDIM quality standards before completion

📖 Key Documentation:
   • CLAUDE.md                           - Main coding guidelines
   • backend/docs/GATEWAY_TRUST_ARCHITECTURE.md
   • backend/docs/ENTITY_MIGRATION_GUIDE.md
   • backend/docs/DISTRIBUTED_TRACING_GUIDE.md
   • DATABASE_ARCHITECTURE_MIGRATION_PLAN.md

⚠️  Critical HDIM Rules:
   1. HIPAA: PHI cache TTL ≤ 5 minutes (recommend 2 min)
   2. Database: ddl-auto MUST be 'validate' (never create/update)
   3. Security: All endpoints require @PreAuthorize annotation
   4. Multi-tenant: All queries MUST filter by tenant_id
   5. Entity-Migration: JPA entities MUST sync with Liquibase
   6. Kafka: Type headers MUST be disabled (ClassNotFoundException)
   7. Docker: Containers MUST use non-root user (UID 1001)

╔══════════════════════════════════════════════════════════════╗
║ HDIM Accelerator Plugin v3.0.0 Ready                        ║
╚══════════════════════════════════════════════════════════════╝
"

# Return successful hook output with context
cat <<EOF
{
  "continue": true,
  "suppressOutput": false,
  "systemMessage": $(echo "$context_message" | jq -Rs .)
}
EOF

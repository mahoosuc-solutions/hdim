#!/bin/bash
# HDIM PostgreSQL Backup Script
#
# Creates compressed backups of all databases with:
# - Timestamped filenames
# - Configurable retention (default: 7 days)
# - Individual database backups
# - Full cluster backup
# - Error notifications
#
# Usage: ./backup.sh [OPTIONS]
#   --databases "db1,db2"  Specific databases to backup (default: all)
#   --retention DAYS       Days to keep backups (default: 7)
#   --output DIR           Output directory (default: /backups)
#   --full                 Include full pg_dumpall backup
#
# Environment Variables:
#   PGHOST, PGPORT, PGUSER, PGPASSWORD - PostgreSQL connection details
#   BACKUP_RETENTION_DAYS - Override default retention
#   BACKUP_OUTPUT_DIR - Override default output directory
#   SLACK_WEBHOOK_URL - Optional Slack notification webhook
#   BACKUP_ENCRYPTION_KEY - Optional GPG key ID for encryption

set -euo pipefail

# Configuration
BACKUP_DIR="${BACKUP_OUTPUT_DIR:-/backups}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="${BACKUP_DIR}/backup_${TIMESTAMP}.log"

# PostgreSQL connection defaults
export PGHOST="${PGHOST:-postgres}"
export PGPORT="${PGPORT:-5432}"
export PGUSER="${PGUSER:-healthdata}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() {
    local level="$1"
    local message="$2"
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo -e "${timestamp} [${level}] ${message}" | tee -a "${LOG_FILE}"
}

notify_slack() {
    local status="$1"
    local message="$2"

    if [ -n "${SLACK_WEBHOOK_URL:-}" ]; then
        local emoji="✅"
        [ "$status" = "error" ] && emoji="🚨"

        curl -s -X POST "${SLACK_WEBHOOK_URL}" \
            -H 'Content-Type: application/json' \
            -d "{
                \"text\": \"${emoji} HDIM Database Backup ${status}: ${message}\",
                \"username\": \"Backup Bot\",
                \"icon_emoji\": \":floppy_disk:\"
            }" > /dev/null 2>&1 || true
    fi
}

# Ensure backup directory exists
mkdir -p "${BACKUP_DIR}"
touch "${LOG_FILE}"

log "INFO" "Starting PostgreSQL backup process"
log "INFO" "Backup directory: ${BACKUP_DIR}"
log "INFO" "Retention policy: ${RETENTION_DAYS} days"

# Parse arguments
DATABASES=""
INCLUDE_FULL=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --databases)
            DATABASES="$2"
            shift 2
            ;;
        --retention)
            RETENTION_DAYS="$2"
            shift 2
            ;;
        --output)
            BACKUP_DIR="$2"
            mkdir -p "${BACKUP_DIR}"
            shift 2
            ;;
        --full)
            INCLUDE_FULL=true
            shift
            ;;
        *)
            log "WARN" "Unknown option: $1"
            shift
            ;;
    esac
done

# Check PostgreSQL connectivity
log "INFO" "Checking PostgreSQL connectivity..."
if ! pg_isready -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" > /dev/null 2>&1; then
    log "ERROR" "Cannot connect to PostgreSQL at ${PGHOST}:${PGPORT}"
    notify_slack "error" "Failed to connect to PostgreSQL"
    exit 1
fi
log "INFO" "PostgreSQL is ready"

# Get list of databases to backup
if [ -z "${DATABASES}" ]; then
    DATABASES=$(psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -t -c \
        "SELECT datname FROM pg_database WHERE datistemplate = false AND datname NOT IN ('postgres');" \
        | tr -d ' ' | grep -v '^$')
fi

log "INFO" "Databases to backup: ${DATABASES}"

# Create backup subdirectory for this run
BACKUP_RUN_DIR="${BACKUP_DIR}/${TIMESTAMP}"
mkdir -p "${BACKUP_RUN_DIR}"

BACKUP_SUCCESS=true
BACKUP_COUNT=0
BACKUP_SIZE=0

# Backup each database
for DB in ${DATABASES//,/ }; do
    DB=$(echo "${DB}" | tr -d ' ')
    [ -z "${DB}" ] && continue

    BACKUP_FILE="${BACKUP_RUN_DIR}/${DB}_${TIMESTAMP}.sql.gz"

    log "INFO" "Backing up database: ${DB}"

    if pg_dump -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" \
        --format=custom \
        --compress=9 \
        --file="${BACKUP_FILE%.sql.gz}.dump" \
        "${DB}" 2>> "${LOG_FILE}"; then

        FILE_SIZE=$(du -h "${BACKUP_FILE%.sql.gz}.dump" | cut -f1)
        log "INFO" "Backup of ${DB} completed successfully (${FILE_SIZE})"
        BACKUP_COUNT=$((BACKUP_COUNT + 1))

        # Optional encryption
        if [ -n "${BACKUP_ENCRYPTION_KEY:-}" ]; then
            gpg --encrypt --recipient "${BACKUP_ENCRYPTION_KEY}" \
                "${BACKUP_FILE%.sql.gz}.dump" 2>> "${LOG_FILE}" && \
            rm "${BACKUP_FILE%.sql.gz}.dump"
            log "INFO" "Encrypted backup: ${BACKUP_FILE%.sql.gz}.dump.gpg"
        fi
    else
        log "ERROR" "Backup of ${DB} failed"
        BACKUP_SUCCESS=false
    fi
done

# Full cluster backup if requested
if [ "${INCLUDE_FULL}" = true ]; then
    log "INFO" "Creating full cluster backup..."
    FULL_BACKUP="${BACKUP_RUN_DIR}/full_cluster_${TIMESTAMP}.sql.gz"

    if pg_dumpall -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" \
        | gzip > "${FULL_BACKUP}" 2>> "${LOG_FILE}"; then

        FILE_SIZE=$(du -h "${FULL_BACKUP}" | cut -f1)
        log "INFO" "Full cluster backup completed (${FILE_SIZE})"
    else
        log "ERROR" "Full cluster backup failed"
        BACKUP_SUCCESS=false
    fi
fi

# Create manifest file
MANIFEST_FILE="${BACKUP_RUN_DIR}/manifest.json"
cat > "${MANIFEST_FILE}" << EOF
{
    "timestamp": "${TIMESTAMP}",
    "host": "${PGHOST}",
    "databases": "$(echo ${DATABASES} | tr ' ' ',')",
    "backup_count": ${BACKUP_COUNT},
    "include_full": ${INCLUDE_FULL},
    "success": ${BACKUP_SUCCESS},
    "created_at": "$(date -Iseconds)"
}
EOF

# Calculate total backup size
TOTAL_SIZE=$(du -sh "${BACKUP_RUN_DIR}" | cut -f1)
log "INFO" "Total backup size: ${TOTAL_SIZE}"

# Clean up old backups
log "INFO" "Cleaning up backups older than ${RETENTION_DAYS} days..."
OLD_BACKUPS=$(find "${BACKUP_DIR}" -maxdepth 1 -type d -name "20*" -mtime +${RETENTION_DAYS} 2>/dev/null || true)

if [ -n "${OLD_BACKUPS}" ]; then
    echo "${OLD_BACKUPS}" | while read -r old_dir; do
        log "INFO" "Removing old backup: ${old_dir}"
        rm -rf "${old_dir}"
    done
fi

# Final status
if [ "${BACKUP_SUCCESS}" = true ]; then
    log "INFO" "Backup completed successfully: ${BACKUP_COUNT} database(s), ${TOTAL_SIZE} total"
    notify_slack "success" "Backed up ${BACKUP_COUNT} database(s), ${TOTAL_SIZE} total"
    exit 0
else
    log "ERROR" "Backup completed with errors"
    notify_slack "error" "Backup completed with errors - check logs"
    exit 1
fi

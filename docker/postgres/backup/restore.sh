#!/bin/bash
# HDIM PostgreSQL Restore Script
#
# Restores database backups created by backup.sh
#
# Usage: ./restore.sh [OPTIONS]
#   --backup TIMESTAMP     Backup timestamp to restore (e.g., 20240101_120000)
#   --database DB          Specific database to restore
#   --target-db NEW_DB     Restore to different database name
#   --list                 List available backups
#   --verify               Verify backup integrity without restoring
#   --dry-run              Show what would be restored
#
# Environment Variables:
#   PGHOST, PGPORT, PGUSER, PGPASSWORD - PostgreSQL connection details
#   BACKUP_INPUT_DIR - Override default backup directory

set -euo pipefail

# Configuration
BACKUP_DIR="${BACKUP_INPUT_DIR:-/backups}"
TIMESTAMP=""
DATABASE=""
TARGET_DB=""
LIST_MODE=false
VERIFY_MODE=false
DRY_RUN=false

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
    echo -e "[${level}] ${message}"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --backup)
            TIMESTAMP="$2"
            shift 2
            ;;
        --database)
            DATABASE="$2"
            shift 2
            ;;
        --target-db)
            TARGET_DB="$2"
            shift 2
            ;;
        --list)
            LIST_MODE=true
            shift
            ;;
        --verify)
            VERIFY_MODE=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --backup TIMESTAMP   Backup timestamp to restore"
            echo "  --database DB        Specific database to restore"
            echo "  --target-db NEW_DB   Restore to different database name"
            echo "  --list               List available backups"
            echo "  --verify             Verify backup integrity"
            echo "  --dry-run            Show what would be restored"
            exit 0
            ;;
        *)
            log "WARN" "Unknown option: $1"
            shift
            ;;
    esac
done

# List available backups
if [ "${LIST_MODE}" = true ]; then
    echo "Available backups:"
    echo "=================="

    for backup_dir in $(find "${BACKUP_DIR}" -maxdepth 1 -type d -name "20*" | sort -r); do
        timestamp=$(basename "${backup_dir}")
        manifest="${backup_dir}/manifest.json"

        if [ -f "${manifest}" ]; then
            dbs=$(jq -r '.databases' "${manifest}" 2>/dev/null || echo "unknown")
            success=$(jq -r '.success' "${manifest}" 2>/dev/null || echo "unknown")
            size=$(du -sh "${backup_dir}" | cut -f1)

            status_color="${GREEN}"
            [ "${success}" != "true" ] && status_color="${RED}"

            echo -e "${timestamp} | ${size} | ${status_color}${success}${NC} | ${dbs}"
        else
            echo "${timestamp} | (no manifest)"
        fi
    done
    exit 0
fi

# Validate arguments
if [ -z "${TIMESTAMP}" ]; then
    log "ERROR" "Backup timestamp required. Use --list to see available backups."
    exit 1
fi

BACKUP_RUN_DIR="${BACKUP_DIR}/${TIMESTAMP}"
if [ ! -d "${BACKUP_RUN_DIR}" ]; then
    log "ERROR" "Backup not found: ${BACKUP_RUN_DIR}"
    exit 1
fi

# Check PostgreSQL connectivity
if ! pg_isready -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" > /dev/null 2>&1; then
    log "ERROR" "Cannot connect to PostgreSQL at ${PGHOST}:${PGPORT}"
    exit 1
fi

# Find backup files
if [ -n "${DATABASE}" ]; then
    BACKUP_FILES=$(find "${BACKUP_RUN_DIR}" -name "${DATABASE}_*.dump" -o -name "${DATABASE}_*.dump.gpg" 2>/dev/null || true)
else
    BACKUP_FILES=$(find "${BACKUP_RUN_DIR}" -name "*.dump" -o -name "*.dump.gpg" 2>/dev/null | grep -v "full_cluster" || true)
fi

if [ -z "${BACKUP_FILES}" ]; then
    log "ERROR" "No backup files found in ${BACKUP_RUN_DIR}"
    exit 1
fi

# Verify mode
if [ "${VERIFY_MODE}" = true ]; then
    echo "Verifying backups in ${BACKUP_RUN_DIR}..."
    echo ""

    ALL_VALID=true
    for backup_file in ${BACKUP_FILES}; do
        filename=$(basename "${backup_file}")
        db_name=$(echo "${filename}" | sed 's/_[0-9]\{8\}_[0-9]\{6\}\.dump.*//')

        if pg_restore --list "${backup_file}" > /dev/null 2>&1; then
            echo -e "${GREEN}[VALID]${NC} ${filename}"
        else
            echo -e "${RED}[INVALID]${NC} ${filename}"
            ALL_VALID=false
        fi
    done

    if [ "${ALL_VALID}" = true ]; then
        echo ""
        echo -e "${GREEN}All backups are valid${NC}"
        exit 0
    else
        echo ""
        echo -e "${RED}Some backups are invalid${NC}"
        exit 1
    fi
fi

# Dry run mode
if [ "${DRY_RUN}" = true ]; then
    echo "Dry run - would restore the following:"
    echo ""

    for backup_file in ${BACKUP_FILES}; do
        filename=$(basename "${backup_file}")
        db_name=$(echo "${filename}" | sed 's/_[0-9]\{8\}_[0-9]\{6\}\.dump.*//')
        target="${TARGET_DB:-${db_name}}"

        echo "  ${filename} -> database '${target}'"
    done
    exit 0
fi

# Restore backups
log "INFO" "Starting restore from ${TIMESTAMP}"

for backup_file in ${BACKUP_FILES}; do
    filename=$(basename "${backup_file}")
    db_name=$(echo "${filename}" | sed 's/_[0-9]\{8\}_[0-9]\{6\}\.dump.*//')
    target="${TARGET_DB:-${db_name}}"

    log "INFO" "Restoring ${filename} to database '${target}'..."

    # Decrypt if encrypted
    if [[ "${backup_file}" == *.gpg ]]; then
        log "INFO" "Decrypting backup..."
        gpg --decrypt "${backup_file}" > "${backup_file%.gpg}"
        backup_file="${backup_file%.gpg}"
    fi

    # Create database if it doesn't exist
    psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -tc \
        "SELECT 1 FROM pg_database WHERE datname = '${target}';" | grep -q 1 || \
        psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -c \
        "CREATE DATABASE \"${target}\";"

    # Restore
    if pg_restore -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" \
        --dbname="${target}" \
        --clean \
        --if-exists \
        --no-owner \
        --no-privileges \
        "${backup_file}"; then

        log "INFO" "Successfully restored ${db_name} to ${target}"
    else
        log "ERROR" "Failed to restore ${db_name}"
    fi
done

log "INFO" "Restore completed"

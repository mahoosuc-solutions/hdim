#!/bin/bash
# HDIM PostgreSQL Backup Service Entrypoint
#
# Configures cron schedule and starts the backup service

set -e

# Default backup schedule (2 AM daily)
BACKUP_CRON="${BACKUP_CRON:-0 2 * * *}"

# Configure cron schedule
echo "${BACKUP_CRON} /usr/local/bin/backup.sh --full >> /var/log/backup.log 2>&1" > /etc/crontabs/root

# Create log file
touch /var/log/backup.log

echo "==========================================="
echo "HDIM PostgreSQL Backup Service"
echo "==========================================="
echo "Schedule: ${BACKUP_CRON}"
echo "Retention: ${BACKUP_RETENTION_DAYS:-7} days"
echo "PostgreSQL Host: ${PGHOST:-postgres}:${PGPORT:-5432}"
echo "Backup Directory: ${BACKUP_OUTPUT_DIR:-/backups}"
echo ""
echo "Commands:"
echo "  Manual backup: docker exec backup-service /usr/local/bin/backup.sh"
echo "  List backups:  docker exec backup-service /usr/local/bin/restore.sh --list"
echo "  Restore:       docker exec backup-service /usr/local/bin/restore.sh --backup TIMESTAMP"
echo "==========================================="

# Run initial backup on startup if requested
if [ "${BACKUP_ON_STARTUP:-false}" = "true" ]; then
    echo "Running initial backup..."
    /usr/local/bin/backup.sh --full
fi

# Tail log in background for docker logs visibility
tail -F /var/log/backup.log &

# Start cron daemon
exec "$@"

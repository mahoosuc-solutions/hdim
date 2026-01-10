#!/bin/bash
# =============================================================================
# HDIM Maintenance Script
# =============================================================================
# Handles log rotation, storage management, and database maintenance
#
# Can be run manually or via cron:
#   0 2 * * * /path/to/hdim-maintenance.sh rotate-logs
#   0 3 * * 0 /path/to/hdim-maintenance.sh weekly-backup
#   0 4 * * * /path/to/hdim-maintenance.sh cleanup
# =============================================================================

set -e

HDIM_DATA_DIR="${HDIM_DATA_DIR:-$HOME/.hdim-data}"
LOG_DIR="$HDIM_DATA_DIR/logs"
BACKUP_DIR="$HDIM_DATA_DIR/backups"
MAX_LOG_SIZE_MB=100
BACKUP_RETENTION_DAYS=30
DOCKER_LOG_MAX_SIZE="50m"

# Ensure directories exist
mkdir -p "$LOG_DIR" "$BACKUP_DIR"

# Logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_DIR/maintenance.log"
}

# =============================================================================
# Log Rotation
# =============================================================================
rotate_logs() {
    log "Starting log rotation..."

    # Rotate Docker container logs
    for container in $(docker ps -a --format "{{.Names}}" 2>/dev/null); do
        LOG_PATH=$(docker inspect --format='{{.LogPath}}' "$container" 2>/dev/null || echo "")
        if [ -z "$LOG_PATH" ] || [ ! -f "$LOG_PATH" ]; then
            continue
        fi

        LOG_SIZE=$(stat -c%s "$LOG_PATH" 2>/dev/null || echo 0)
        MAX_SIZE=$((MAX_LOG_SIZE_MB * 1024 * 1024))

        if [ "$LOG_SIZE" -gt "$MAX_SIZE" ]; then
            log "  Rotating logs for $container ($(numfmt --to=iec $LOG_SIZE))"

            # Archive old log
            ARCHIVE="$LOG_DIR/${container}_$(date +%Y%m%d_%H%M%S).log.gz"
            cp "$LOG_PATH" "$LOG_DIR/${container}_temp.log"
            gzip "$LOG_DIR/${container}_temp.log"
            mv "$LOG_DIR/${container}_temp.log.gz" "$ARCHIVE"

            # Truncate current log
            truncate -s 0 "$LOG_PATH" 2>/dev/null || true

            log "    Archived to: $ARCHIVE"
        fi
    done

    # Clean old archived logs (keep last 7 days)
    find "$LOG_DIR" -name "*.log.gz" -mtime +7 -delete 2>/dev/null || true

    log "Log rotation complete"
}

# =============================================================================
# Database Backup
# =============================================================================
backup_databases() {
    log "Starting database backup..."

    TIMESTAMP=$(date +%Y%m%d_%H%M%S)

    # Check if PostgreSQL is running
    if ! docker ps --format '{{.Names}}' | grep -q '^hdim-external-postgres$'; then
        log "  ERROR: PostgreSQL container not running"
        return 1
    fi

    # Full backup
    BACKUP_FILE="$BACKUP_DIR/hdim_full_$TIMESTAMP.sql.gz"
    docker exec hdim-external-postgres pg_dumpall -U healthdata 2>/dev/null | gzip > "$BACKUP_FILE"
    log "  Created: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

    # Individual database backups for quick restore
    for db in gateway_db fhir_db patient_db quality_db caregap_db; do
        DB_BACKUP="$BACKUP_DIR/${db}_$TIMESTAMP.sql.gz"
        docker exec hdim-external-postgres pg_dump -U healthdata "$db" 2>/dev/null | gzip > "$DB_BACKUP"
        log "  Created: $DB_BACKUP ($(du -h "$DB_BACKUP" | cut -f1))"
    done

    # Clean old backups
    log "  Cleaning backups older than $BACKUP_RETENTION_DAYS days..."
    find "$BACKUP_DIR" -name "*.sql.gz" -mtime +$BACKUP_RETENTION_DAYS -delete 2>/dev/null || true

    log "Database backup complete"
}

# =============================================================================
# Storage Cleanup
# =============================================================================
cleanup_storage() {
    log "Starting storage cleanup..."

    # Docker image cleanup
    log "  Removing dangling Docker images..."
    docker image prune -f 2>/dev/null || true

    # Docker volume cleanup (unused)
    log "  Checking for unused volumes..."
    UNUSED_VOLUMES=$(docker volume ls -qf dangling=true 2>/dev/null | wc -l)
    if [ "$UNUSED_VOLUMES" -gt 0 ]; then
        log "    Found $UNUSED_VOLUMES unused volumes"
        docker volume prune -f 2>/dev/null || true
    fi

    # Docker build cache (older than 24h)
    log "  Cleaning Docker build cache..."
    docker builder prune -f --filter "until=24h" 2>/dev/null || true

    # Clean temp files
    log "  Cleaning temp files..."
    find /tmp -name "hdim-*" -mtime +1 -delete 2>/dev/null || true

    # Report storage usage
    log "Current storage usage:"
    log "  PostgreSQL data: $(du -sh "$HDIM_DATA_DIR/postgres" 2>/dev/null | cut -f1 || echo 'N/A')"
    log "  Logs: $(du -sh "$LOG_DIR" 2>/dev/null | cut -f1 || echo 'N/A')"
    log "  Backups: $(du -sh "$BACKUP_DIR" 2>/dev/null | cut -f1 || echo 'N/A')"
    log "  Docker images: $(docker system df --format '{{.Size}}' 2>/dev/null | head -1 || echo 'N/A')"

    log "Storage cleanup complete"
}

# =============================================================================
# Database Vacuum
# =============================================================================
vacuum_databases() {
    log "Starting database vacuum..."

    if ! docker ps --format '{{.Names}}' | grep -q '^hdim-external-postgres$'; then
        log "  ERROR: PostgreSQL container not running"
        return 1
    fi

    for db in gateway_db fhir_db patient_db quality_db caregap_db; do
        log "  Vacuuming $db..."
        docker exec hdim-external-postgres psql -U healthdata -d "$db" -c "VACUUM ANALYZE;" 2>/dev/null || true
    done

    log "Database vacuum complete"
}

# =============================================================================
# Health Check
# =============================================================================
health_check() {
    log "Running health check..."

    HEALTHY=true

    # Check PostgreSQL
    if docker ps --format '{{.Names}}' | grep -q '^hdim-external-postgres$'; then
        if docker exec hdim-external-postgres pg_isready -U healthdata > /dev/null 2>&1; then
            log "  ✓ PostgreSQL: healthy"
        else
            log "  ✗ PostgreSQL: unhealthy"
            HEALTHY=false
        fi
    else
        log "  ✗ PostgreSQL: not running"
        HEALTHY=false
    fi

    # Check HDIM services
    for service in hdim-gateway hdim-fhir hdim-patient hdim-quality-measure hdim-care-gap; do
        if docker ps --format '{{.Names}}' | grep -q "^${service}$"; then
            STATUS=$(docker inspect --format='{{.State.Health.Status}}' "$service" 2>/dev/null || echo "unknown")
            if [ "$STATUS" == "healthy" ]; then
                log "  ✓ $service: healthy"
            else
                log "  ⚠ $service: $STATUS"
            fi
        else
            log "  - $service: not running"
        fi
    done

    # Check disk space
    DISK_USAGE=$(df -h "$HDIM_DATA_DIR" | tail -1 | awk '{print $5}' | sed 's/%//')
    if [ "$DISK_USAGE" -gt 90 ]; then
        log "  ✗ Disk usage: ${DISK_USAGE}% (CRITICAL)"
        HEALTHY=false
    elif [ "$DISK_USAGE" -gt 80 ]; then
        log "  ⚠ Disk usage: ${DISK_USAGE}% (WARNING)"
    else
        log "  ✓ Disk usage: ${DISK_USAGE}%"
    fi

    if [ "$HEALTHY" = true ]; then
        log "Health check passed"
        return 0
    else
        log "Health check failed - some issues detected"
        return 1
    fi
}

# =============================================================================
# Show Status
# =============================================================================
show_status() {
    echo ""
    echo "========================================"
    echo "HDIM Maintenance Status"
    echo "========================================"
    echo ""
    echo "Data Directory: $HDIM_DATA_DIR"
    echo ""
    echo "Storage Usage:"
    du -sh "$HDIM_DATA_DIR"/* 2>/dev/null | while read size path; do
        echo "  $(basename "$path"): $size"
    done
    echo ""
    echo "Recent Backups:"
    ls -lh "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -5 | awk '{print "  "$NF": "$5}' || echo "  No backups found"
    echo ""
    echo "Docker Storage:"
    docker system df 2>/dev/null || echo "  Docker not available"
    echo ""
}

# =============================================================================
# Weekly Maintenance
# =============================================================================
weekly_maintenance() {
    log "Starting weekly maintenance..."
    backup_databases
    vacuum_databases
    cleanup_storage
    health_check
    log "Weekly maintenance complete"
}

# =============================================================================
# Main
# =============================================================================
case "${1:-}" in
    rotate-logs)
        rotate_logs
        ;;
    backup)
        backup_databases
        ;;
    cleanup)
        cleanup_storage
        ;;
    vacuum)
        vacuum_databases
        ;;
    health)
        health_check
        ;;
    weekly)
        weekly_maintenance
        ;;
    status)
        show_status
        ;;
    *)
        echo "HDIM Maintenance Script"
        echo ""
        echo "Usage: $0 {rotate-logs|backup|cleanup|vacuum|health|weekly|status}"
        echo ""
        echo "Commands:"
        echo "  rotate-logs  - Rotate container logs"
        echo "  backup       - Backup all databases"
        echo "  cleanup      - Clean unused Docker resources"
        echo "  vacuum       - Run VACUUM ANALYZE on databases"
        echo "  health       - Run health check"
        echo "  weekly       - Run full weekly maintenance"
        echo "  status       - Show maintenance status"
        echo ""
        echo "Cron examples:"
        echo "  0 2 * * * $0 rotate-logs"
        echo "  0 3 * * 0 $0 weekly"
        exit 1
        ;;
esac

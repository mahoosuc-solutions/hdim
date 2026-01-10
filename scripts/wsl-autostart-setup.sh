#!/bin/bash
# =============================================================================
# WSL Auto-Start Setup for HDIM Customer Simulation
# =============================================================================
# This script sets up automatic startup of HDIM databases and services
# when WSL starts.
#
# Usage:
#   chmod +x scripts/wsl-autostart-setup.sh
#   ./scripts/wsl-autostart-setup.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
HDIM_DATA_DIR="${HDIM_DATA_DIR:-$HOME/.hdim-data}"

echo "========================================"
echo "HDIM WSL Auto-Start Setup"
echo "========================================"
echo ""

# =============================================================================
# Create Data Directory Structure
# =============================================================================
echo "[1/4] Creating data directory structure..."

mkdir -p "$HDIM_DATA_DIR/postgres"
mkdir -p "$HDIM_DATA_DIR/logs"
mkdir -p "$HDIM_DATA_DIR/backups"

echo "  Data directory: $HDIM_DATA_DIR"

# =============================================================================
# Create HDIM Service Management Script
# =============================================================================
echo ""
echo "[2/4] Creating service management script..."

cat > "$SCRIPT_DIR/hdim-services.sh" << 'EOFSERVICE'
#!/bin/bash
# =============================================================================
# HDIM Services Management Script
# =============================================================================
# Manages HDIM Docker containers and databases
#
# Usage:
#   hdim-services.sh start|stop|restart|status|logs|cleanup
# =============================================================================

HDIM_DATA_DIR="${HDIM_DATA_DIR:-$HOME/.hdim-data}"
PROJECT_DIR="${HDIM_PROJECT_DIR:-$(dirname "$(dirname "$(readlink -f "$0")")")}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

start_services() {
    log_info "Starting HDIM services..."

    # Start PostgreSQL container
    if ! docker ps --format '{{.Names}}' | grep -q '^hdim-external-postgres$'; then
        log_info "Starting PostgreSQL..."
        if docker ps -a --format '{{.Names}}' | grep -q '^hdim-external-postgres$'; then
            docker start hdim-external-postgres
        else
            docker run -d \
                --name hdim-external-postgres \
                -e POSTGRES_USER=healthdata \
                -e POSTGRES_PASSWORD=healthdata_password \
                -e POSTGRES_DB=healthdata_db \
                -p 5432:5432 \
                -v "$HDIM_DATA_DIR/postgres:/var/lib/postgresql/data" \
                --restart unless-stopped \
                postgres:15-alpine
        fi
    else
        log_info "PostgreSQL already running"
    fi

    # Wait for PostgreSQL
    log_info "Waiting for PostgreSQL..."
    for i in {1..30}; do
        if docker exec hdim-external-postgres pg_isready -U healthdata > /dev/null 2>&1; then
            break
        fi
        sleep 1
    done

    # Start HAPI FHIR (optional)
    if ! docker ps --format '{{.Names}}' | grep -q '^external-fhir-server$'; then
        log_info "Starting HAPI FHIR server..."
        if docker ps -a --format '{{.Names}}' | grep -q '^external-fhir-server$'; then
            docker start external-fhir-server
        else
            docker run -d \
                --name external-fhir-server \
                -p 8080:8080 \
                -e hapi.fhir.fhir_version=R4 \
                -e spring.datasource.url=jdbc:h2:mem:hapi \
                -e JAVA_TOOL_OPTIONS="-Xms512m -Xmx2g" \
                --restart unless-stopped \
                hapiproject/hapi:latest
        fi
    else
        log_info "HAPI FHIR already running"
    fi

    # Start HDIM services
    log_info "Starting HDIM clinical portal services..."
    cd "$PROJECT_DIR"
    docker compose -f docker-compose.minimal-clinical.yml up -d 2>&1 | grep -v "orphan containers"

    log_info "Services started successfully!"
}

stop_services() {
    log_info "Stopping HDIM services..."

    cd "$PROJECT_DIR"
    docker compose -f docker-compose.minimal-clinical.yml down 2>&1 | grep -v "orphan containers" || true

    docker stop external-fhir-server 2>/dev/null || true
    docker stop hdim-external-postgres 2>/dev/null || true

    log_info "Services stopped"
}

restart_services() {
    stop_services
    sleep 2
    start_services
}

show_status() {
    echo ""
    echo "========================================"
    echo "HDIM Services Status"
    echo "========================================"
    echo ""

    echo "Infrastructure:"
    docker ps --filter "name=hdim-external-postgres" --filter "name=external-fhir-server" \
        --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  No infrastructure containers"

    echo ""
    echo "HDIM Services:"
    docker ps --filter "name=hdim-" \
        --format "  {{.Names}}: {{.Status}}" 2>/dev/null || echo "  No HDIM containers"

    echo ""
    echo "Resource Usage:"
    docker stats --no-stream --format "  {{.Name}}: CPU={{.CPUPerc}}, MEM={{.MemUsage}}" \
        $(docker ps --filter "name=hdim" -q) 2>/dev/null || echo "  No stats available"

    echo ""
    echo "Data Directory: $HDIM_DATA_DIR"
    du -sh "$HDIM_DATA_DIR"/* 2>/dev/null || echo "  No data yet"
}

show_logs() {
    SERVICE="${2:-all}"
    LINES="${3:-100}"

    if [ "$SERVICE" == "all" ]; then
        log_info "Showing logs for all HDIM services (last $LINES lines each)..."
        for container in $(docker ps --filter "name=hdim" --format "{{.Names}}"); do
            echo ""
            echo "=== $container ==="
            docker logs --tail "$LINES" "$container" 2>&1 | head -50
        done
    else
        log_info "Showing logs for $SERVICE (last $LINES lines)..."
        docker logs --tail "$LINES" "$SERVICE" 2>&1
    fi
}

cleanup() {
    log_warn "This will remove old logs, unused images, and build cache."
    read -p "Continue? (y/N) " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "Cleaning up..."

        # Rotate logs
        log_info "Rotating container logs..."
        for container in $(docker ps -a --format "{{.Names}}"); do
            LOG_PATH=$(docker inspect --format='{{.LogPath}}' "$container" 2>/dev/null)
            if [ -f "$LOG_PATH" ] && [ $(stat -c%s "$LOG_PATH" 2>/dev/null || echo 0) -gt 104857600 ]; then
                log_info "  Truncating logs for $container"
                truncate -s 0 "$LOG_PATH" 2>/dev/null || true
            fi
        done

        # Remove unused images
        log_info "Removing dangling images..."
        docker image prune -f

        # Remove build cache
        log_info "Pruning build cache..."
        docker builder prune -f --filter "until=24h"

        # Clean old backups (keep last 7 days)
        log_info "Cleaning old backups..."
        find "$HDIM_DATA_DIR/backups" -name "*.sql.gz" -mtime +7 -delete 2>/dev/null || true

        log_info "Cleanup complete!"
    else
        log_info "Cleanup cancelled"
    fi
}

backup_database() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    BACKUP_FILE="$HDIM_DATA_DIR/backups/hdim_backup_$TIMESTAMP.sql.gz"

    log_info "Creating database backup..."

    docker exec hdim-external-postgres pg_dumpall -U healthdata | gzip > "$BACKUP_FILE"

    log_info "Backup created: $BACKUP_FILE"
    log_info "Size: $(du -h "$BACKUP_FILE" | cut -f1)"
}

case "${1:-}" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$@"
        ;;
    cleanup)
        cleanup
        ;;
    backup)
        backup_database
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|logs|cleanup|backup}"
        echo ""
        echo "Commands:"
        echo "  start    - Start all HDIM services"
        echo "  stop     - Stop all HDIM services"
        echo "  restart  - Restart all services"
        echo "  status   - Show service status and resource usage"
        echo "  logs     - Show service logs (logs [service] [lines])"
        echo "  cleanup  - Clean logs, images, and old backups"
        echo "  backup   - Create database backup"
        exit 1
        ;;
esac
EOFSERVICE

chmod +x "$SCRIPT_DIR/hdim-services.sh"
echo "  Created: $SCRIPT_DIR/hdim-services.sh"

# =============================================================================
# Create WSL Startup Script
# =============================================================================
echo ""
echo "[3/4] Creating WSL startup script..."

cat > "$HOME/.hdim-startup.sh" << EOFSTARTUP
#!/bin/bash
# HDIM Auto-Start Script for WSL
# This script is called when WSL starts

export HDIM_DATA_DIR="$HDIM_DATA_DIR"
export HDIM_PROJECT_DIR="$PROJECT_DIR"

# Wait for Docker to be ready
for i in {1..60}; do
    if docker info > /dev/null 2>&1; then
        break
    fi
    sleep 1
done

# Start HDIM services
"$SCRIPT_DIR/hdim-services.sh" start >> "$HDIM_DATA_DIR/logs/startup.log" 2>&1
EOFSTARTUP

chmod +x "$HOME/.hdim-startup.sh"
echo "  Created: $HOME/.hdim-startup.sh"

# =============================================================================
# Add to .bashrc for auto-start
# =============================================================================
echo ""
echo "[4/4] Configuring auto-start..."

# Add environment variables to .bashrc
if ! grep -q "HDIM_DATA_DIR" "$HOME/.bashrc" 2>/dev/null; then
    cat >> "$HOME/.bashrc" << EOFBASHRC

# HDIM Customer Simulation Environment
export HDIM_DATA_DIR="$HDIM_DATA_DIR"
export HDIM_PROJECT_DIR="$PROJECT_DIR"
alias hdim="$SCRIPT_DIR/hdim-services.sh"

# Auto-start HDIM services on first terminal (optional - uncomment to enable)
# if [ -z "\$HDIM_STARTED" ] && [ -f "\$HOME/.hdim-startup.sh" ]; then
#     export HDIM_STARTED=1
#     "\$HOME/.hdim-startup.sh" &
# fi
EOFBASHRC
    echo "  Added HDIM environment to .bashrc"
else
    echo "  .bashrc already configured"
fi

echo ""
echo "========================================"
echo "Setup Complete!"
echo "========================================"
echo ""
echo "Usage:"
echo "  hdim start    - Start all services"
echo "  hdim stop     - Stop all services"
echo "  hdim status   - Check service status"
echo "  hdim logs     - View service logs"
echo "  hdim cleanup  - Clean up resources"
echo "  hdim backup   - Backup databases"
echo ""
echo "Data stored in: $HDIM_DATA_DIR"
echo ""
echo "To enable auto-start on WSL boot:"
echo "  1. Edit ~/.bashrc"
echo "  2. Uncomment the auto-start section"
echo ""
echo "Run 'source ~/.bashrc' to load the new aliases."
echo ""

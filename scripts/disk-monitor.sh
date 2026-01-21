#!/bin/bash

################################################################################
# disk-monitor.sh
#
# Daily disk space monitoring script with alerts
# Checks C: drive and Docker storage for capacity issues
#
# Usage: ./scripts/disk-monitor.sh [--email EMAIL] [--slack-webhook URL]
# Options:
#   --email EMAIL             : Email address for alerts (requires mail command)
#   --slack-webhook URL       : Slack webhook URL for alerts
#   --threshold PERCENT       : Alert threshold (default: 85%)
#   --check-external          : Also check external drive usage
#
# Typically run daily via cron to prevent C: drive from filling up
#
################################################################################

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
THRESHOLD=85
EMAIL=""
SLACK_WEBHOOK=""
CHECK_EXTERNAL=false
HOSTNAME=$(hostname)
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --email) EMAIL="$2"; shift 2 ;;
        --slack-webhook) SLACK_WEBHOOK="$2"; shift 2 ;;
        --threshold) THRESHOLD="$2"; shift 2 ;;
        --check-external) CHECK_EXTERNAL=true; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# Function to send alert
send_alert() {
    local subject="$1"
    local message="$2"

    # Email alert
    if [ -n "$EMAIL" ]; then
        echo "$message" | mail -s "$subject" "$EMAIL" 2>/dev/null || \
            echo -e "${YELLOW}Warning: Could not send email to $EMAIL${NC}"
    fi

    # Slack alert
    if [ -n "$SLACK_WEBHOOK" ]; then
        curl -X POST "$SLACK_WEBHOOK" \
            -H 'Content-Type: application/json' \
            -d "{\"text\": \"⚠️  $subject\n$message\"}" 2>/dev/null || \
            echo -e "${YELLOW}Warning: Could not send Slack notification${NC}"
    fi
}

# Function to check drive
check_drive() {
    local mount_point="$1"
    local drive_name="$2"

    local usage=$(df "$mount_point" | tail -1 | awk '{print $5}' | sed 's/%//')
    local available=$(df -h "$mount_point" | tail -1 | awk '{print $4}')
    local total=$(df -h "$mount_point" | tail -1 | awk '{print $2}')

    echo "Checking $drive_name ($mount_point)..."
    echo "  Total: $total, Available: $available, Used: $usage%"

    if [ "$usage" -ge "$THRESHOLD" ]; then
        local alert_msg="⚠️ ALERT: $drive_name on $HOSTNAME is ${usage}% full!

Mount: $mount_point
Total: $total
Available: $available
Used: $usage%
Threshold: $THRESHOLD%

Timestamp: $TIMESTAMP

RECOMMENDATIONS:
1. Run Docker cleanup: ./scripts/docker-cleanup.sh --aggressive
2. Check Docker disk usage: docker system df
3. Consider moving volumes to external drive
4. Monitor with: df -h $mount_point"

        echo -e "${RED}✗ ALERT: $drive_name is ${usage}% full!${NC}"
        send_alert "ALERT: $drive_name at ${usage}% capacity" "$alert_msg"
        return 1
    elif [ "$usage" -ge 70 ]; then
        echo -e "${YELLOW}⚠ WARNING: $drive_name is ${usage}% full${NC}"
    else
        echo -e "${GREEN}✓ OK: $drive_name is ${usage}% full${NC}"
    fi

    echo ""
    return 0
}

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}Disk Space Monitor${NC}"
echo -e "${BLUE}================================================${NC}"
echo "Hostname: $HOSTNAME"
echo "Timestamp: $TIMESTAMP"
echo "Threshold: $THRESHOLD%"
echo ""

# Initialize status
STATUS=0

# Check C: drive (Windows host)
if [ -d "/mnt/c" ]; then
    check_drive "/mnt/c" "C: Drive (Windows)" || STATUS=1
fi

# Check WSL root filesystem
if [ -d "/" ]; then
    check_drive "/" "WSL Filesystem" || STATUS=1
fi

# Check Docker data space
if docker info > /dev/null 2>&1; then
    echo "Checking Docker storage..."
    DOCKER_USAGE=$(docker system df --format "{{json .}}" 2>/dev/null | \
        grep -o '"Images":{"Count":[0-9]*' || echo '"Count":0')
    echo "  Docker images: $DOCKER_USAGE"
    echo ""
fi

# Check external drive if requested
if [ "$CHECK_EXTERNAL" = true ] && [ -d "/mnt/wd-black" ]; then
    check_drive "/mnt/wd-black" "WD-Black External Drive" || STATUS=1
fi

# Summary
echo -e "${BLUE}================================================${NC}"
if [ $STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ All drives are healthy${NC}"
else
    echo -e "${RED}✗ Alert threshold exceeded on one or more drives${NC}"
fi
echo -e "${BLUE}================================================${NC}"

exit $STATUS

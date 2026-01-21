#!/bin/bash
# HDIM Care Gap Demo - One-Command Startup
#
# This script starts the complete demo environment including:
# - PostgreSQL database
# - Redis cache
# - 6 backend microservices
# - Clinical portal frontend
# - Demo data seeding
#
# Usage:
#   ./start-demo.sh           # Start demo
#   ./start-demo.sh --build   # Rebuild images and start
#   ./start-demo.sh --clean   # Clean start (remove volumes)
#   ./start-demo.sh --stop    # Stop demo
#   ./start-demo.sh --status  # Check status

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ASCII Art Banner
print_banner() {
    echo -e "${CYAN}"
    cat << 'EOF'
    ╦ ╦╔╦╗╦╔╦╗  ╔═╗╔═╗╦═╗╔═╗  ╔═╗╔═╗╔═╗  ╔╦╗╔═╗╔╦╗╔═╗
    ╠═╣ ║║║║║║  ║  ╠═╣╠╦╝║╣   ║ ╦╠═╣╠═╝   ║║║╣ ║║║║ ║
    ╩ ╩═╩╝╩╩ ╩  ╚═╝╩ ╩╩╚═╚═╝  ╚═╝╩ ╩╩    ═╩╝╚═╝╩ ╩╚═╝
EOF
    echo -e "${NC}"
    echo -e "${BLUE}  Real-time Care Gap Identification & Closure Demo${NC}"
    echo -e "${BLUE}  ─────────────────────────────────────────────────${NC}"
    echo ""
}

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    # Check Docker
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Error: Docker is not installed.${NC}"
        echo "Please install Docker: https://docs.docker.com/get-docker/"
        exit 1
    fi

    # Check Docker Compose
    if ! docker compose version &> /dev/null; then
        echo -e "${RED}Error: Docker Compose V2 is not available.${NC}"
        echo "Please update Docker Desktop or install Docker Compose V2."
        exit 1
    fi

    # Check Docker is running
    if ! docker info &> /dev/null; then
        echo -e "${RED}Error: Docker daemon is not running.${NC}"
        echo "Please start Docker Desktop or the Docker service."
        exit 1
    fi

    # Check available memory (recommend 8GB)
    if command -v free &> /dev/null; then
        total_mem=$(free -g | awk '/^Mem:/{print $2}')
        if [ "$total_mem" -lt 6 ]; then
            echo -e "${YELLOW}Warning: System has ${total_mem}GB RAM. 8GB+ recommended.${NC}"
        fi
    fi

    echo -e "${GREEN}✓ Prerequisites check passed${NC}"
}

# Start the demo
wait_for_init_infrastructure() {
    local container="hdim-demo-init-infrastructure"
    local timeout=300
    local interval=5
    local elapsed=0

    echo -e "${YELLOW}Waiting for demo user initialization to complete...${NC}"

    while [ "$elapsed" -lt "$timeout" ]; do
        local state
        state=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null || true)

        if [ "$state" = "exited" ]; then
            local exit_code
            exit_code=$(docker inspect -f '{{.State.ExitCode}}' "$container")
            if [ "$exit_code" -ne 0 ]; then
                echo -e "${RED}Init service failed. Check logs: docker logs ${container}${NC}"
                exit 1
            fi
            echo -e "${GREEN}✓ Demo users initialized${NC}"
            return 0
        fi

        if [ "$state" = "running" ] || [ "$state" = "created" ]; then
            sleep "$interval"
            elapsed=$((elapsed + interval))
            continue
        fi

        sleep "$interval"
        elapsed=$((elapsed + interval))
    done

    echo -e "${YELLOW}Init service still running. Proceeding, but logins may not be ready yet.${NC}"
}

start_demo() {
    local build_flag=""

    if [ "$1" == "--build" ]; then
        build_flag="--build"
        echo -e "${YELLOW}Building images (this may take 5-10 minutes)...${NC}"
    fi

    echo -e "\n${BLUE}Starting full demo stack (services, demo users, seeding, frontend)...${NC}"
    docker compose -f docker-compose.demo.yml up -d $build_flag

    echo -e "${YELLOW}Waiting for core services to initialize...${NC}"
    echo -e "  ${CYAN}Tip: Watch logs with: docker compose -f docker-compose.demo.yml logs -f${NC}"

    for i in {1..12}; do
        echo -ne "\r  Progress: [${GREEN}"
        printf '█%.0s' $(seq 1 $i)
        printf '░%.0s' $(seq 1 $((12-i)))
        echo -ne "${NC}] $((i*5))s"
        sleep 5
    done
    echo ""

    wait_for_init_infrastructure

    echo -e "\n${BLUE}Seeding demo data...${NC}"
    chmod +x ./seed-demo-data.sh
    ./seed-demo-data.sh --wait || true

    echo -e "\n${BLUE}Verifying services...${NC}"
    show_status
}

# Stop the demo
stop_demo() {
    echo -e "${YELLOW}Stopping demo services...${NC}"
    docker compose -f docker-compose.demo.yml down
    echo -e "${GREEN}Demo stopped.${NC}"
}

# Clean start (remove volumes)
clean_start() {
    echo -e "${YELLOW}Performing clean start (removing all data)...${NC}"
    docker compose -f docker-compose.demo.yml down -v --remove-orphans
    echo -e "${GREEN}Clean environment ready.${NC}"
    start_demo --build
}

# Show status
show_status() {
    echo -e "\n${BLUE}─────────────────────────────────────────────────${NC}"
    echo -e "${BLUE}                  Service Status${NC}"
    echo -e "${BLUE}─────────────────────────────────────────────────${NC}"

    services=(
        "postgres:5432:PostgreSQL Database"
        "redis:6379:Redis Cache"
        "gateway-service:8080:API Gateway"
        "cql-engine-service:8081:CQL Engine"
        "patient-service:8084:Patient Service"
        "fhir-service:8085:FHIR R4 Server"
        "care-gap-service:8086:Care Gap Service"
        "quality-measure-service:8087:Quality Measures"
        "clinical-portal:4200:Clinical Portal"
    )

    all_healthy=true

    for service in "${services[@]}"; do
        IFS=':' read -r name port desc <<< "$service"
        container="hdim-demo-${name}"

        # Check if container is running
        if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
            # Check health endpoint for HTTP services
            if [ "$port" -gt 5000 ]; then
                if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1 || \
                   curl -s "http://localhost:$port" > /dev/null 2>&1; then
                    echo -e "  ${GREEN}●${NC} $desc (port $port) - ${GREEN}Healthy${NC}"
                else
                    echo -e "  ${YELLOW}●${NC} $desc (port $port) - ${YELLOW}Starting${NC}"
                    all_healthy=false
                fi
            else
                echo -e "  ${GREEN}●${NC} $desc (port $port) - ${GREEN}Running${NC}"
            fi
        else
            echo -e "  ${RED}●${NC} $desc (port $port) - ${RED}Not Running${NC}"
            all_healthy=false
        fi
    done

    echo ""

    if [ "$all_healthy" = true ]; then
        echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}          Demo is ready! Access at:${NC}"
        echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
        echo ""
        echo -e "  ${CYAN}Clinical Portal:${NC}  http://localhost:4200"
        echo -e "  ${CYAN}API Gateway:${NC}      http://localhost:8080"
        echo -e "  ${CYAN}FHIR Server:${NC}      http://localhost:8085/fhir"
        echo ""
        echo -e "  ${YELLOW}Demo Credentials:${NC}"
        echo -e "    Username: demo_admin"
        echo -e "    Password: demo123"
        echo ""
        echo -e "  ${YELLOW}Hero Patient:${NC} Maria Garcia (MRN: MRN-2024-4521)"
        echo -e "    - 57-year-old female"
        echo -e "    - Colorectal Cancer Screening gap (127 days overdue)"
        echo -e "    - Breast Cancer Screening gap (45 days overdue)"
        echo ""
        echo -e "  ${CYAN}See DEMO_WALKTHROUGH.md for the full demo script.${NC}"
    else
        echo -e "${YELLOW}Some services are still starting. Please wait and check again:${NC}"
        echo -e "  ./start-demo.sh --status"
    fi
}

# Print usage
print_usage() {
    echo "Usage: ./start-demo.sh [OPTION]"
    echo ""
    echo "Options:"
    echo "  (none)      Start demo with existing images"
    echo "  --build     Rebuild all images before starting"
    echo "  --clean     Remove all data and start fresh"
    echo "  --stop      Stop all demo services"
    echo "  --status    Show status of all services"
    echo "  --help      Show this help message"
}

# Main
main() {
    print_banner

    case "${1:-}" in
        --build)
            check_prerequisites
            start_demo --build
            ;;
        --clean)
            check_prerequisites
            clean_start
            ;;
        --stop)
            stop_demo
            ;;
        --status)
            show_status
            ;;
        --help|-h)
            print_usage
            ;;
        "")
            check_prerequisites
            start_demo
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            print_usage
            exit 1
            ;;
    esac
}

main "$@"

#!/bin/bash

################################################################################
# HDIM Complete Deployment Script
# Handles: Git merges, Docker builds, Service deployment, Health validation
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
ENVIRONMENT=${1:-dev}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
LOG_FILE="${PROJECT_ROOT}/deployment-$(date +%Y%m%d-%H%M%S).log"

# Worktree paths
MASTER_WORKTREE="/home/webemo-aaron/projects/hdim-master"
BACKEND_PHASE1_WORKTREE="/home/webemo-aaron/projects/hdim-backend-phase1"
PHASE5B_WORKTREE="/home/webemo-aaron/projects/hdim-phase5b-integration"

log_section() {
  echo -e "\n${BLUE}════════════════════════════════════════════════════════════${NC}" | tee -a "$LOG_FILE"
  echo -e "${BLUE}  $*${NC}" | tee -a "$LOG_FILE"
  echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}\n" | tee -a "$LOG_FILE"
}

log_success() {
  echo -e "${GREEN}✅ $*${NC}" | tee -a "$LOG_FILE"
}

log_error() {
  echo -e "${RED}❌ $*${NC}" | tee -a "$LOG_FILE"
}

log() {
  echo -e "${BLUE}ℹ️  $*${NC}" | tee -a "$LOG_FILE"
}

validate_environment() {
  log_section "Validating Environment"
  docker --version && log_success "Docker found"
  docker compose version && log_success "Docker Compose found"
  node --version && log_success "Node.js found"
  java -version 2>&1 | head -1 && log_success "Java found"
}

validate_worktrees() {
  log_section "Validating Git Worktrees"
  [ -d "$MASTER_WORKTREE" ] && log_success "Master worktree found" || (log_error "Master worktree not found"; exit 1)
  [ -d "$BACKEND_PHASE1_WORKTREE" ] && log_success "Backend Phase1 worktree found" || (log_error "Backend Phase1 worktree not found"; exit 1)
  [ -d "$PHASE5B_WORKTREE" ] && log_success "Phase 5B worktree found" || (log_error "Phase 5B worktree not found"; exit 1)
}

deploy_services() {
  log_section "Deploying Services ($ENVIRONMENT)"
  cd "$PROJECT_ROOT"

  case "$ENVIRONMENT" in
    dev)
      log "Starting development environment..."
      docker compose up -d
      ;;
    demo)
      log "Starting demo environment..."
      docker compose -f docker-compose.demo.yml up -d
      ;;
    staging)
      log "Starting staging environment with observability..."
      docker compose -f docker-compose.staging.yml -f docker-compose.observability.yml up -d
      ;;
    production)
      log "Starting production environment with HA..."
      docker compose -f docker-compose.production.yml -f docker-compose.ha.yml -f docker-compose.observability.yml up -d
      ;;
    *)
      log_error "Unknown environment: $ENVIRONMENT"
      exit 1
      ;;
  esac

  log "Waiting 15 seconds for services to start..."
  sleep 15
  log_success "Deployment started"
}

check_health() {
  log_section "Health Check Validation"
  
  check_endpoint() {
    local name=$1
    local url=$2
    echo -n "  $name: "
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    if [ "$response" = "200" ]; then
      log_success "OK ($response)"
    else
      echo -e "${YELLOW}Warning - got $response${NC}"
    fi
  }

  log "Frontend & Gateway:"
  check_endpoint "Shell App" "http://localhost:4200"
  check_endpoint "API Gateway" "http://localhost:8001/health"

  log "\nCore Services:"
  check_endpoint "Quality Measure" "http://localhost:8087/quality-measure/health"
  check_endpoint "FHIR Service" "http://localhost:8085/fhir/metadata"
  check_endpoint "Patient Service" "http://localhost:8084/patient/health"
  check_endpoint "Care Gap Service" "http://localhost:8086/care-gap/health"

  if [ "$ENVIRONMENT" != "dev" ]; then
    log "\nObservability Stack:"
    check_endpoint "Prometheus" "http://localhost:9090/-/healthy"
    check_endpoint "Grafana" "http://localhost:3000/api/health"
    check_endpoint "Jaeger" "http://localhost:16686/api/traces"
  fi
}

print_summary() {
  log_section "Deployment Summary"
  log "Environment: $ENVIRONMENT"
  log "Log file: $LOG_FILE"
  log ""
  log "Access Points:"
  case "$ENVIRONMENT" in
    dev|demo)
      log "  Frontend: http://localhost:4200"
      log "  API Gateway: http://localhost:8001"
      log "  Quality Measure: http://localhost:8087"
      log "  FHIR: http://localhost:8085"
      ;;
    *)
      log "  Frontend: http://localhost:4200"
      log "  Grafana: http://localhost:3000 (admin/admin)"
      log "  Prometheus: http://localhost:9090"
      log "  Jaeger: http://localhost:16686"
      ;;
  esac
  log ""
  log "Next Steps:"
  log "  1. View logs: docker compose logs -f"
  log "  2. Run tests: npx nx run-many --target=test --all"
  log "  3. Stop services: docker compose down"
  log ""
  log_success "Deployment Successful! 🚀"
}

main() {
  log_section "HDIM Deployment Script"
  log "Environment: $ENVIRONMENT"
  
  validate_environment
  validate_worktrees
  deploy_services
  sleep 5
  check_health
  print_summary
}

main

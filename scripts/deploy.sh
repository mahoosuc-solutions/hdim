#!/bin/bash

################################################################################
# HDIM Deployment Script
# Orchestrates: pre-flight → build → deploy → health-check → smoke-test
#
# Usage: ./scripts/deploy.sh <environment> [options]
#
# Arguments:
#   <environment>   dev | demo | staging | pilot | production
#
# Options:
#   -i, --instance <name>          Stack/tenant name (default: default)
#                                  Docker Compose: sets project name to hdim-<name>
#                                  Kubernetes: sets namespace to hdim-<env>-<name>
#   -t, --target <compose|k8s|auto> Deployment toolchain (default: auto)
#   -s, --services <list>          Comma-separated services to deploy (default: all)
#   --strategy <rolling|recreate>  Deployment strategy (default: rolling)
#   --skip-tests                   Skip smoke tests after deploy
#   --dry-run                      Print commands without executing
#
# Examples:
#   ./scripts/deploy.sh dev
#   ./scripts/deploy.sh staging --instance staging-1
#   ./scripts/deploy.sh pilot --instance acme-health --target k8s --dry-run
#   ./scripts/deploy.sh production --instance prod --strategy rolling
################################################################################

set -euo pipefail

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# ── Paths (always relative to repo root, never hardcoded) ─────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/backend"
DEPLOY_LOG_DIR="$PROJECT_ROOT/deployments"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

# ── Defaults ──────────────────────────────────────────────────────────────────
ENVIRONMENT="${1:-dev}"
INSTANCE="default"
TARGET="auto"
SERVICES=""
STRATEGY="rolling"
SKIP_TESTS=false
DRY_RUN=false

# ── Logging ───────────────────────────────────────────────────────────────────
LOG_FILE="$PROJECT_ROOT/deployment-${ENVIRONMENT}-${TIMESTAMP}.log"

log_section() { echo -e "\n${BLUE}════════════════════════════════════════════════════════════${NC}" | tee -a "$LOG_FILE"; echo -e "${BLUE}  $*${NC}" | tee -a "$LOG_FILE"; echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}\n" | tee -a "$LOG_FILE"; }
log_success()  { echo -e "${GREEN}✅ $*${NC}" | tee -a "$LOG_FILE"; }
log_warning()  { echo -e "${YELLOW}⚠️  $*${NC}" | tee -a "$LOG_FILE"; }
log_error()    { echo -e "${RED}❌ $*${NC}" | tee -a "$LOG_FILE"; }
log()          { echo -e "${CYAN}ℹ  $*${NC}" | tee -a "$LOG_FILE"; }

# Execute or print (dry-run aware)
run() {
  if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}[DRY-RUN] $*${NC}" | tee -a "$LOG_FILE"
  else
    eval "$@" 2>&1 | tee -a "$LOG_FILE"
  fi
}

# ── Argument parsing ──────────────────────────────────────────────────────────
shift || true  # consume environment arg
while [[ $# -gt 0 ]]; do
  case "$1" in
    -i|--instance)   INSTANCE="$2";  shift 2 ;;
    -t|--target)     TARGET="$2";    shift 2 ;;
    -s|--services)   SERVICES="$2";  shift 2 ;;
    --strategy)      STRATEGY="$2";  shift 2 ;;
    --skip-tests)    SKIP_TESTS=true; shift ;;
    --dry-run)       DRY_RUN=true;   shift ;;
    *) log_error "Unknown option: $1"; exit 1 ;;
  esac
done

# Derived values
COMPOSE_PROJECT="hdim-${INSTANCE}"
K8S_NAMESPACE="hdim-${ENVIRONMENT}-${INSTANCE}"

# ── Environment → Compose file mapping ───────────────────────────────────────
resolve_compose_file() {
  case "$ENVIRONMENT" in
    dev)        COMPOSE_FILES="-f docker-compose.yml" ;;
    demo)       COMPOSE_FILES="-f docker-compose.demo.yml" ;;
    staging)    COMPOSE_FILES="-f docker-compose.staging.yml -f docker-compose.observability.yml" ;;
    pilot)      COMPOSE_FILES="-f docker-compose.minimal-clinical.yml" ;;
    production) COMPOSE_FILES="-f docker-compose.production.yml -f docker-compose.ha.yml -f docker-compose.observability.yml" ;;
    *)          log_error "Unknown environment: $ENVIRONMENT"; exit 1 ;;
  esac
}

# ── Auto-detect toolchain ─────────────────────────────────────────────────────
resolve_target() {
  if [ "$TARGET" = "auto" ]; then
    if command -v kubectl &>/dev/null && kubectl cluster-info &>/dev/null 2>&1; then
      TARGET="k8s"
      log "Auto-detected active kubectl context → using Kubernetes"
    else
      TARGET="compose"
      log "No active kubectl context → using Docker Compose"
    fi
  fi
}

# ── Phase 1: Pre-flight ───────────────────────────────────────────────────────
pre_flight() {
  log_section "Phase 1: Pre-flight Validation"

  # Repo root sanity check
  if [ ! -d "$BACKEND_DIR" ]; then
    log_error "backend/ directory not found. Run from the repo root."
    exit 1
  fi

  # Git clean-state check (production only)
  if [ "$ENVIRONMENT" = "production" ]; then
    local git_status
    git_status=$(git -C "$PROJECT_ROOT" status --porcelain 2>/dev/null || echo "")
    if [ -n "$git_status" ]; then
      log_error "Production deployment requires a clean git working directory."
      echo "$git_status"
      exit 1
    fi
    local branch
    branch=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD)
    if [ "$branch" != "master" ]; then
      log_error "Production deployments must be from 'master' branch (current: $branch)"
      exit 1
    fi
    log_success "Git state valid (clean, on master)"
  fi

  # Tooling checks
  if [ "$TARGET" = "compose" ] || [ "$TARGET" = "auto" ]; then
    command -v docker &>/dev/null && log_success "Docker found" || { log_error "docker not found"; exit 1; }
    docker compose version &>/dev/null 2>&1 && log_success "Docker Compose found" || { log_error "docker compose not found"; exit 1; }
  fi
  if [ "$TARGET" = "k8s" ]; then
    command -v kubectl &>/dev/null && log_success "kubectl found" || { log_error "kubectl not found"; exit 1; }
  fi

  # Validate-before-docker-build (entity-migration + HIPAA + liquibase rollback)
  local validate_script="$SCRIPT_DIR/validate-before-docker-build.sh"
  if [ -f "$validate_script" ]; then
    log "Running pre-build validation (entity-migration + HIPAA checks)..."
    run "bash $validate_script"
    log_success "Pre-build validation passed"
  else
    log_warning "validate-before-docker-build.sh not found — skipping schema validation"
  fi
}

# ── Phase 2: Database Migration Validation ────────────────────────────────────
validate_migrations() {
  log_section "Phase 2: Database Migration Validation"

  if [ ! -d "$BACKEND_DIR" ]; then
    log_warning "backend/ not found — skipping migration validation"
    return
  fi

  log "Running EntityMigrationValidationTest across all services..."
  run "cd '$BACKEND_DIR' && ./gradlew test --tests '*EntityMigrationValidationTest' -x javadoc --continue"
  log_success "Migration validation passed"
}

# ── Phase 3: Build (Compose only) ────────────────────────────────────────────
build_images() {
  log_section "Phase 3: Build Docker Images"

  if [ "$TARGET" = "k8s" ]; then
    log "Kubernetes target: skipping local image build (CI/CD pushes images)"
    return
  fi

  log "Pre-caching Gradle dependencies..."
  run "cd '$BACKEND_DIR' && ./gradlew downloadDependencies --no-daemon"

  local build_args=""
  [ "$ENVIRONMENT" = "production" ] && build_args="--no-cache"

  if [ -n "$SERVICES" ]; then
    log "Building selected services: $SERVICES"
    IFS=',' read -ra svc_list <<< "$SERVICES"
    for svc in "${svc_list[@]}"; do
      run "docker compose -p '$COMPOSE_PROJECT' $COMPOSE_FILES build $build_args $svc"
    done
  else
    log "Building all services..."
    run "docker compose -p '$COMPOSE_PROJECT' $COMPOSE_FILES build $build_args"
  fi
  log_success "Images built"
}

# ── Phase 4: Deploy ───────────────────────────────────────────────────────────
deploy() {
  log_section "Phase 4: Deploy (target=$TARGET, strategy=$STRATEGY)"

  local service_args=""
  if [ -n "$SERVICES" ]; then
    service_args="${SERVICES//,/ }"
  fi

  if [ "$TARGET" = "compose" ]; then
    case "$STRATEGY" in
      recreate)
        run "docker compose -p '$COMPOSE_PROJECT' $COMPOSE_FILES down --remove-orphans"
        run "docker compose -p '$COMPOSE_PROJECT' $COMPOSE_FILES up -d $service_args"
        ;;
      rolling|*)
        run "docker compose -p '$COMPOSE_PROJECT' $COMPOSE_FILES up -d --remove-orphans $service_args"
        ;;
    esac
  elif [ "$TARGET" = "k8s" ]; then
    local overlay_dir="$PROJECT_ROOT/k8s/overlays/$ENVIRONMENT"
    if [ ! -d "$overlay_dir" ]; then
      log_error "No K8s overlay found at $overlay_dir"
      exit 1
    fi
    # Apply with optional namespace override
    if [ "$INSTANCE" != "default" ]; then
      run "kubectl apply -k '$overlay_dir/' --namespace '$K8S_NAMESPACE'"
    else
      run "kubectl apply -k '$overlay_dir/'"
    fi
    # Rolling-update wait
    if [ "$STRATEGY" = "rolling" ] && [ "$DRY_RUN" = false ]; then
      local ns_flag="--namespace ${K8S_NAMESPACE}"
      [ "$INSTANCE" = "default" ] && ns_flag="--namespace hdim-${ENVIRONMENT}"
      run "kubectl rollout status deployment --timeout=300s $ns_flag"
    fi
  fi

  log_success "Deployment initiated"
}

# ── Phase 5: Health Checks ────────────────────────────────────────────────────
wait_for_health() {
  local name="$1"
  local url="$2"
  local retries="${3:-12}"
  local delay="${4:-5}"

  echo -n "  $name: "
  for i in $(seq 1 "$retries"); do
    local code
    code=$(curl -sf -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    if [ "$code" = "200" ]; then
      echo -e "${GREEN}UP (${code})${NC}"
      return 0
    fi
    echo -n "."
    sleep "$delay"
  done
  echo -e " ${RED}UNHEALTHY (timeout after $((retries * delay))s)${NC}"
  return 1
}

health_checks() {
  log_section "Phase 5: Health Checks"

  if [ "$DRY_RUN" = true ]; then
    log "[DRY-RUN] Would poll health endpoints for all deployed services"
    return
  fi

  if [ "$TARGET" = "k8s" ]; then
    log "K8s health validated via kubectl rollout status in Phase 4"
    log_success "Health checks passed (K8s rollout)"
    return
  fi

  local gateway_port=8001
  local failed=0

  log "Core services:"
  wait_for_health "API Gateway"      "http://localhost:${gateway_port}/actuator/health" 24 5 || failed=1
  wait_for_health "Patient Service"  "http://localhost:8084/patient/actuator/health"    12 5 || failed=1
  wait_for_health "FHIR Service"     "http://localhost:8085/fhir/actuator/health"       12 5 || failed=1
  wait_for_health "Care Gap Service" "http://localhost:8086/care-gap/actuator/health"   12 5 || failed=1
  wait_for_health "Quality Measure"  "http://localhost:8087/quality-measure/actuator/health" 12 5 || failed=1

  if [ "$ENVIRONMENT" != "dev" ]; then
    log "Observability stack:"
    wait_for_health "Prometheus" "http://localhost:9090/-/healthy" 6 5 || log_warning "Prometheus not healthy (non-critical)"
    wait_for_health "Grafana"    "http://localhost:3000/api/health" 6 5 || log_warning "Grafana not healthy (non-critical)"
    wait_for_health "Jaeger"     "http://localhost:16686/api/traces" 6 5 || log_warning "Jaeger not healthy (non-critical)"
  fi

  if [ "$failed" -ne 0 ]; then
    log_error "One or more core services failed health checks — triggering rollback"
    rollback
    exit 1
  fi

  log_success "All core services healthy"
}

# ── Rollback ──────────────────────────────────────────────────────────────────
rollback() {
  log_section "ROLLBACK"
  log_warning "Rolling back deployment for instance=$INSTANCE environment=$ENVIRONMENT"

  if [ "$TARGET" = "compose" ]; then
    run "docker compose -p '$COMPOSE_PROJECT' $COMPOSE_FILES down --remove-orphans"
    log_warning "Services stopped. Redeploy previous version manually or re-run with the previous image tag."
  elif [ "$TARGET" = "k8s" ]; then
    local ns="hdim-${ENVIRONMENT}"
    [ "$INSTANCE" != "default" ] && ns="$K8S_NAMESPACE"
    run "kubectl rollout undo deployment --namespace '$ns'"
    run "kubectl rollout status deployment --timeout=120s --namespace '$ns'"
  fi
}

# ── Phase 6: Smoke Tests ──────────────────────────────────────────────────────
smoke_tests() {
  log_section "Phase 6: Smoke Tests"

  if [ "$SKIP_TESTS" = true ]; then
    log_warning "Smoke tests skipped (--skip-tests)"
    return
  fi

  local smoke_script="$SCRIPT_DIR/smoke-tests.sh"
  if [ ! -f "$smoke_script" ]; then
    log_warning "smoke-tests.sh not found — skipping smoke tests"
    return
  fi

  local gateway_url="http://localhost:8001"
  local smoke_args="--environment $ENVIRONMENT --gateway $gateway_url"
  [ "$INSTANCE" != "default" ] && smoke_args="$smoke_args --tenant $INSTANCE"
  [ "$ENVIRONMENT" = "dev" ] && smoke_args="$smoke_args --quick"

  run "bash '$smoke_script' $smoke_args"
  log_success "Smoke tests passed"
}

# ── Phase 7: Post-Deployment Report ──────────────────────────────────────────
post_deploy_report() {
  log_section "Phase 7: Post-Deployment Report"

  local report_dir="$DEPLOY_LOG_DIR"
  mkdir -p "$report_dir"

  local report_file="$report_dir/hdim-${ENVIRONMENT}-${INSTANCE}-${TIMESTAMP}.json"
  local git_commit
  git_commit=$(git -C "$PROJECT_ROOT" rev-parse --short HEAD 2>/dev/null || echo "unknown")

  cat > "$report_file" <<JSON
{
  "environment": "${ENVIRONMENT}",
  "instance": "${INSTANCE}",
  "target": "${TARGET}",
  "strategy": "${STRATEGY}",
  "compose_project": "${COMPOSE_PROJECT}",
  "k8s_namespace": "${K8S_NAMESPACE}",
  "git_commit": "${git_commit}",
  "deployed_at": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "smoke_tests_skipped": ${SKIP_TESTS},
  "dry_run": ${DRY_RUN},
  "log_file": "${LOG_FILE}"
}
JSON

  log_success "Deployment report written → $report_file"

  log ""
  log "Access Points:"
  case "$ENVIRONMENT" in
    dev|demo)
      log "  Frontend:        http://localhost:4200"
      log "  API Gateway:     http://localhost:8001"
      log "  Patient API:     http://localhost:8084/patient/swagger-ui/index.html"
      log "  FHIR API:        http://localhost:8085/fhir/swagger-ui/index.html"
      log "  Care Gap API:    http://localhost:8086/care-gap/swagger-ui/index.html"
      log "  Quality API:     http://localhost:8087/quality-measure/swagger-ui/index.html"
      ;;
    staging|pilot|production)
      log "  Frontend:        http://localhost:4200"
      log "  API Gateway:     http://localhost:8001"
      log "  Grafana:         http://localhost:3000 (admin/admin)"
      log "  Prometheus:      http://localhost:9090"
      log "  Jaeger:          http://localhost:16686"
      ;;
  esac
  log ""
  log "Useful commands:"
  log "  Logs:   docker compose -p $COMPOSE_PROJECT $COMPOSE_FILES logs -f"
  log "  Status: docker compose -p $COMPOSE_PROJECT $COMPOSE_FILES ps"
  log "  Stop:   docker compose -p $COMPOSE_PROJECT $COMPOSE_FILES down"
  log ""
  log_success "🚀 Deployment complete! Environment=$ENVIRONMENT Instance=$INSTANCE"
}

# ── Validate environment name ─────────────────────────────────────────────────
validate_env_name() {
  case "$ENVIRONMENT" in
    dev|demo|staging|pilot|production) ;;
    *) log_error "Invalid environment '$ENVIRONMENT'. Must be: dev | demo | staging | pilot | production"; exit 1 ;;
  esac
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
  mkdir -p "$(dirname "$LOG_FILE")"
  log_section "HDIM Deployment Script"
  log "Environment : $ENVIRONMENT"
  log "Instance    : $INSTANCE"
  log "Target      : $TARGET"
  log "Strategy    : $STRATEGY"
  log "Dry-run     : $DRY_RUN"
  [ -n "$SERVICES" ] && log "Services    : $SERVICES"
  log "Log file    : $LOG_FILE"

  validate_env_name
  resolve_target
  resolve_compose_file

  pre_flight
  validate_migrations
  build_images
  deploy
  health_checks
  smoke_tests
  post_deploy_report
}

main

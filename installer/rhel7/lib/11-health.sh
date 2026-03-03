#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Health Checks
# ==============================================================================
# Delegates to existing scripts/wait-for-services.sh and scripts/smoke-tests.sh.
# ==============================================================================

run_health() {
    log_step 9 "Health verification"

    local source_dir="${CURRENT_RELEASE_DIR}/source"

    # --- Wait for services to be healthy ---
    if [[ -x "${source_dir}/scripts/wait-for-services.sh" ]]; then
        log_info "Waiting for services to become healthy (timeout: ${HEALTH_TIMEOUT}s)..."
        if "${source_dir}/scripts/wait-for-services.sh" "$HEALTH_TIMEOUT"; then
            log_success "All services healthy"
        else
            log_warn "Some services did not become healthy within ${HEALTH_TIMEOUT}s"
            log_warn "Check: docker compose logs"
        fi
    else
        log_warn "wait-for-services.sh not found — performing basic checks"
        _basic_health_check
    fi

    # --- Smoke tests ---
    if [[ -x "${source_dir}/scripts/smoke-tests.sh" ]]; then
        log_info "Running smoke tests..."
        if "${source_dir}/scripts/smoke-tests.sh" "http://localhost:8080" $SMOKE_TEST_MODE; then
            log_success "Smoke tests passed"
        else
            log_warn "Some smoke tests failed — review output above"
        fi
    else
        log_info "smoke-tests.sh not found — skipping smoke tests"
    fi
}

_basic_health_check() {
    local timeout=120
    local elapsed=0
    local interval=5

    while (( elapsed < timeout )); do
        local healthy=0
        local total=0

        # Check infrastructure ports
        for port in 5435 6380 9094; do
            ((total++))
            if nc -z localhost "$port" 2>/dev/null; then
                ((healthy++))
            fi
        done

        # Check gateway
        ((total++))
        if curl -sf http://localhost:8080/actuator/health &>/dev/null; then
            ((healthy++))
        fi

        if (( healthy == total )); then
            log_success "Basic health check: ${healthy}/${total} services responding"
            return 0
        fi

        log_info "  ${healthy}/${total} healthy (${elapsed}s / ${timeout}s)..."
        sleep "$interval"
        elapsed=$((elapsed + interval))
    done

    log_warn "Basic health check: not all services responded within ${timeout}s"
}

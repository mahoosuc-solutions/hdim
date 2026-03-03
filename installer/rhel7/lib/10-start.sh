#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Start Services
# ==============================================================================
# Starts Docker Compose with the selected profile.
# ==============================================================================

run_start() {
    log_step 8 "Starting services"

    local source_dir="${CURRENT_RELEASE_DIR}/source"
    local env_file="${CURRENT_RELEASE_DIR}/.env"

    cd "$source_dir"

    # Build compose command — layer overlays based on profile
    local compose_cmd=(docker compose -f docker-compose.yml)

    # Add production overlay if it exists
    if [[ -f docker-compose.prod.yml ]]; then
        compose_cmd+=(-f docker-compose.prod.yml)
    fi

    # Add resource limits overlay
    if [[ -f docker-compose.resources.yml ]]; then
        compose_cmd+=(-f docker-compose.resources.yml)
    fi

    # Healthix profile: add HA + Redis HA + Healthix-scale overlays
    local effective_profile="$PROFILE"
    if [[ "$PROFILE" == "healthix" ]]; then
        effective_profile="full"  # healthix deploys all services
        if [[ -f docker-compose.ha.yml ]]; then
            compose_cmd+=(-f docker-compose.ha.yml)
        fi
        if [[ -f docker-compose.redis-ha.yml ]]; then
            compose_cmd+=(-f docker-compose.redis-ha.yml)
        fi
        if [[ -f docker-compose.healthix.yml ]]; then
            compose_cmd+=(-f docker-compose.healthix.yml)
        fi
        log_info "Healthix profile: layering HA + Redis Sentinel + scaled resources"
    fi

    compose_cmd+=(--env-file "$env_file")
    compose_cmd+=(--profile "$effective_profile")

    log_info "Starting containers with profile '$PROFILE'..."
    log_info "Command: ${compose_cmd[*]} up -d"

    "${compose_cmd[@]}" up -d 2>&1 | tail -20 || \
        die "Failed to start containers"

    # Brief pause for containers to initialize
    sleep 5

    # Show running containers
    log_info "Running containers:"
    docker compose ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null || true

    log_success "Containers started"
}

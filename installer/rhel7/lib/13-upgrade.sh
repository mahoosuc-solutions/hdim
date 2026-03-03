#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Upgrade
# ==============================================================================
# Backs up databases, creates new release, swaps symlink, restarts.
# Auto-rolls back if smoke tests fail.
# ==============================================================================

run_upgrade() {
    local version="${1:-}"

    # Validate current installation
    if [[ ! -L "$HDIM_CURRENT" ]]; then
        die "No active installation found at ${HDIM_CURRENT}. Run 'install' first."
    fi

    local previous_release
    previous_release="$(readlink -f "$HDIM_CURRENT")"
    log_info "Current release: $(basename "$previous_release")"

    # Load current profile
    if [[ -f "$HDIM_CONF" ]]; then
        source "$HDIM_CONF"
        PROFILE="${HDIM_PROFILE:-core}"
    else
        die "Configuration not found at ${HDIM_CONF}"
    fi

    # --- Step 1: Backup ---
    log_step 1 "Database backup"
    local backup_ts
    backup_ts="$(date '+%Y%m%d-%H%M%S')"
    local backup_dir="${HDIM_BACKUPS}/${backup_ts}"
    mkdir -p "$backup_dir"

    log_info "Backing up PostgreSQL databases to ${backup_dir}..."
    local pg_container
    pg_container="$(docker ps --filter 'name=postgres' --format '{{.Names}}' | head -1)"

    if [[ -n "$pg_container" ]]; then
        # Get list of databases
        local databases
        databases="$(docker exec "$pg_container" psql -U healthdata -t -c \
            "SELECT datname FROM pg_database WHERE datistemplate = false AND datname != 'postgres';" 2>/dev/null | tr -d ' ')"

        for db in $databases; do
            if [[ -n "$db" ]]; then
                log_info "  Dumping ${db}..."
                docker exec "$pg_container" pg_dump -U healthdata -Fc "$db" \
                    > "${backup_dir}/${db}.dump" 2>/dev/null || \
                    log_warn "  Failed to dump ${db}"
            fi
        done
        log_success "Backup complete: ${backup_dir}"
    else
        log_warn "PostgreSQL container not found — skipping database backup"
    fi

    # --- Step 2: New release ---
    log_step 2 "Preparing new release"
    source "${INSTALLER_DIR}/lib/08-release.sh"
    run_release "$version"

    # --- Step 3: Reuse secrets ---
    log_info "Copying secrets from previous release..."
    if [[ -f "${previous_release}/.env" ]]; then
        cp "${previous_release}/.env" "${CURRENT_RELEASE_DIR}/.env"
        chmod 600 "${CURRENT_RELEASE_DIR}/.env"
        chown "${HDIM_USER}:${HDIM_GROUP}" "${CURRENT_RELEASE_DIR}/.env"
        log_success "Secrets copied from previous release"
    else
        log_warn "No .env in previous release — generating new secrets"
        source "${INSTALLER_DIR}/lib/07-secrets.sh"
        run_secrets
    fi

    # --- Step 4: Build ---
    log_step 3 "Building updated services"
    source "${INSTALLER_DIR}/lib/09-build.sh"
    run_build

    # --- Step 5: Cutover ---
    log_step 4 "Performing cutover"
    local source_dir="${previous_release}/source"
    cd "$source_dir"

    log_info "Stopping current services..."
    docker compose down --timeout 30 2>&1 | tail -5 || true

    log_info "Swapping symlink to new release..."
    ln -sfn "$CURRENT_RELEASE_DIR" "$HDIM_CURRENT"

    log_info "Starting updated services..."
    systemctl restart hdim || {
        log_error "Failed to start new release — initiating auto-rollback"
        ln -sfn "$previous_release" "$HDIM_CURRENT"
        systemctl restart hdim
        die "Upgrade failed. Rolled back to $(basename "$previous_release")"
    }

    # --- Step 6: Validate ---
    log_step 5 "Post-upgrade validation"
    source "${INSTALLER_DIR}/lib/11-health.sh"
    local new_source="${CURRENT_RELEASE_DIR}/source"

    if [[ -x "${new_source}/scripts/smoke-tests.sh" ]]; then
        if "${new_source}/scripts/smoke-tests.sh" "http://localhost:8080" --quick; then
            log_success "Smoke tests passed — upgrade successful"
        else
            log_error "Smoke tests failed — initiating auto-rollback"
            ln -sfn "$previous_release" "$HDIM_CURRENT"
            systemctl restart hdim
            die "Upgrade failed smoke tests. Rolled back to $(basename "$previous_release")"
        fi
    else
        log_warn "Smoke tests not available — please verify manually"
    fi

    log_banner "Upgrade Complete"
    log_info "Previous: $(basename "$previous_release")"
    log_info "Current:  $(basename "$CURRENT_RELEASE_DIR")"
    log_info "Backup:   ${backup_dir}"
    log_info "Rollback: hdim-install.sh rollback --to ${previous_release}"
}

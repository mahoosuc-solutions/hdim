#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Rollback
# ==============================================================================
# Restores a previous release and optionally restores database backups.
# ==============================================================================

run_rollback() {
    local target="${1:-}"

    # Validate current installation
    if [[ ! -L "$HDIM_CURRENT" ]]; then
        die "No active installation found at ${HDIM_CURRENT}"
    fi

    local current_release
    current_release="$(readlink -f "$HDIM_CURRENT")"
    log_info "Current release: $(basename "$current_release")"

    # --- Determine rollback target ---
    if [[ -n "$target" ]]; then
        # Explicit target
        if [[ ! -d "$target" ]]; then
            # Try as a release name under HDIM_RELEASES
            if [[ -d "${HDIM_RELEASES}/${target}" ]]; then
                target="${HDIM_RELEASES}/${target}"
            else
                die "Rollback target not found: $target"
            fi
        fi
    else
        # Auto-detect previous release (most recent before current)
        log_info "Available releases:"
        local releases=()
        while IFS= read -r d; do
            local name
            name="$(basename "$d")"
            if [[ "$d" != "$current_release" ]]; then
                releases+=("$d")
                echo "    $(basename "$d")"
            fi
        done < <(find "$HDIM_RELEASES" -maxdepth 1 -mindepth 1 -type d | sort -r)

        if (( ${#releases[@]} == 0 )); then
            die "No previous releases found to roll back to"
        fi

        target="${releases[0]}"
        log_info "Rolling back to most recent previous release: $(basename "$target")"
    fi

    log_info "Rollback target: $(basename "$target")"

    # Confirm
    read -rp "  Proceed with rollback? [y/N] " confirm
    if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
        log_info "Rollback cancelled"
        return 0
    fi

    # --- Stop current services ---
    log_info "Stopping current services..."
    cd "${current_release}/source" 2>/dev/null || true
    docker compose down --timeout 30 2>&1 | tail -5 || true

    # --- Restore symlink ---
    log_info "Restoring symlink to previous release..."
    ln -sfn "$target" "$HDIM_CURRENT"

    # --- Optional: Database restore ---
    local latest_backup
    latest_backup="$(find "$HDIM_BACKUPS" -maxdepth 1 -mindepth 1 -type d | sort -r | head -1)"

    if [[ -n "$latest_backup" ]]; then
        echo ""
        read -rp "  Restore database backup from $(basename "$latest_backup")? [y/N] " restore_db
        if [[ "$restore_db" == "y" || "$restore_db" == "Y" ]]; then
            log_info "Starting PostgreSQL for restore..."
            cd "${target}/source"
            docker compose up -d postgres
            sleep 10

            local pg_container
            pg_container="$(docker ps --filter 'name=postgres' --format '{{.Names}}' | head -1)"

            if [[ -n "$pg_container" ]]; then
                for dump_file in "${latest_backup}"/*.dump; do
                    local db_name
                    db_name="$(basename "$dump_file" .dump)"
                    log_info "  Restoring ${db_name}..."
                    docker exec -i "$pg_container" pg_restore -U healthdata -d "$db_name" \
                        --clean --if-exists < "$dump_file" 2>/dev/null || \
                        log_warn "  Restore warning for ${db_name} (may be OK if schema unchanged)"
                done
                log_success "Database restore complete"
            else
                log_warn "PostgreSQL container not available for restore"
            fi
        fi
    fi

    # --- Restart ---
    log_info "Starting restored release..."
    systemctl restart hdim || die "Failed to start restored release"

    # --- Validate ---
    source "${INSTALLER_DIR}/lib/11-health.sh"
    CURRENT_RELEASE_DIR="$target"
    run_health 2>/dev/null || true

    log_banner "Rollback Complete"
    log_info "Restored to: $(basename "$target")"
    log_info "Previous (now inactive): $(basename "$current_release")"
}

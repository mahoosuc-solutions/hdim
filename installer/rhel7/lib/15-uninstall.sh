#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Uninstall
# ==============================================================================
# Stops services, removes systemd unit, optionally preserves data.
# ==============================================================================

run_uninstall() {
    local keep_data="${1:-false}"

    log_warn "This will remove the HDIM platform from this system."
    if [[ "$keep_data" == "true" ]]; then
        log_info "Database volumes and backups will be PRESERVED."
    else
        log_warn "ALL DATA including databases will be PERMANENTLY DELETED."
    fi

    echo ""
    read -rp "  Type 'UNINSTALL' to confirm: " confirm
    if [[ "$confirm" != "UNINSTALL" ]]; then
        log_info "Uninstall cancelled"
        return 0
    fi

    # --- Stop services ---
    log_info "Stopping HDIM services..."
    systemctl stop hdim 2>/dev/null || true

    if [[ -L "$HDIM_CURRENT" ]]; then
        local source_dir
        source_dir="$(readlink -f "$HDIM_CURRENT")/source"
        if [[ -d "$source_dir" ]]; then
            cd "$source_dir"
            docker compose down --volumes --remove-orphans --timeout 30 2>&1 | tail -5 || true
        fi
    fi

    # --- Remove systemd unit ---
    log_info "Removing systemd unit..."
    systemctl disable hdim 2>/dev/null || true
    rm -f "$HDIM_SYSTEMD_UNIT"
    systemctl daemon-reload

    # --- Remove releases ---
    log_info "Removing release directories..."
    rm -rf "$HDIM_RELEASES"
    rm -f "$HDIM_CURRENT"

    # --- Optionally remove data ---
    if [[ "$keep_data" != "true" ]]; then
        log_info "Removing data volumes and backups..."
        rm -rf "$HDIM_DATA"
        rm -rf "$HDIM_BACKUPS"
        rm -rf "$HDIM_LOGS"
        rm -rf "$HDIM_SSL"
        # Remove base directory if empty
        rmdir "$HDIM_SHARED" 2>/dev/null || true
        rmdir "$HDIM_BASE" 2>/dev/null || true
    else
        log_info "Preserving data at ${HDIM_DATA} and backups at ${HDIM_BACKUPS}"
    fi

    # --- Remove config files ---
    log_info "Removing configuration files..."
    rm -f "$HDIM_CONF"
    rmdir "$(dirname "$HDIM_CONF")" 2>/dev/null || true
    rm -f "$HDIM_LOGROTATE_CONF"
    rm -f "$HDIM_JAVA_PROFILE"
    rm -f "$HDIM_OPS_PROFILE"

    # --- Remove hdim user ---
    if id "$HDIM_USER" &>/dev/null; then
        log_info "Removing system user '${HDIM_USER}'..."
        userdel "$HDIM_USER" 2>/dev/null || true
    fi

    log_banner "Uninstall Complete"
    if [[ "$keep_data" == "true" ]]; then
        log_info "Data preserved at: ${HDIM_DATA}"
        log_info "Backups preserved at: ${HDIM_BACKUPS}"
    fi
    log_info "Java (${JAVA_INSTALL_DIR}) and Docker were NOT removed."
    log_info "Remove manually if no longer needed."
}

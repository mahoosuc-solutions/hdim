#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Systemd, Firewall & Finalization
# ==============================================================================
# Installs systemd unit, configures firewall, creates convenience aliases.
# ==============================================================================

run_systemd() {
    log_step 9 "Systemd & finalization"

    # --- Render systemd unit ---
    log_info "Installing systemd unit..."
    sed -e "s|\${HDIM_USER}|${HDIM_USER}|g" \
        -e "s|\${HDIM_GROUP}|${HDIM_GROUP}|g" \
        -e "s|\${HDIM_CURRENT}|${HDIM_CURRENT}|g" \
        -e "s|\${HDIM_CONF}|${HDIM_CONF}|g" \
        -e "s|\${HDIM_PROFILE}|${PROFILE}|g" \
        "${INSTALLER_DIR}/templates/hdim.service.tmpl" > "$HDIM_SYSTEMD_UNIT"

    chmod 644 "$HDIM_SYSTEMD_UNIT"
    systemctl daemon-reload
    systemctl enable hdim.service
    log_success "hdim.service installed and enabled"

    # --- Create current symlink ---
    log_info "Creating 'current' symlink..."
    ln -sfn "$CURRENT_RELEASE_DIR" "$HDIM_CURRENT"
    log_success "${HDIM_CURRENT} -> ${CURRENT_RELEASE_DIR}"

    # --- Firewall ---
    if command -v firewall-cmd &>/dev/null && systemctl is-active firewalld &>/dev/null; then
        log_info "Configuring firewalld..."
        # External-facing ports
        for port in 80 443 8080 4200; do
            firewall-cmd --permanent --add-port="${port}/tcp" 2>/dev/null || true
        done
        firewall-cmd --reload 2>/dev/null || true
        log_success "Firewall: ports 80, 443, 8080, 4200 opened"
        log_info "Internal ports (5435, 6380, 9094) remain blocked from external access"
    elif command -v iptables &>/dev/null; then
        log_info "Configuring iptables..."
        for port in 80 443 8080 4200; do
            iptables -I INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null || true
        done
        service iptables save 2>/dev/null || true
        log_success "iptables: ports 80, 443, 8080, 4200 opened"
    else
        log_warn "No firewall manager detected — ensure ports 80, 443, 8080, 4200 are accessible"
    fi

    # --- Logrotate ---
    log_info "Installing logrotate configuration..."
    sed -e "s|\${HDIM_LOGS}|${HDIM_LOGS}|g" \
        -e "s|\${HDIM_USER}|${HDIM_USER}|g" \
        -e "s|\${HDIM_GROUP}|${HDIM_GROUP}|g" \
        -e "s|\${HDIM_CURRENT}|${HDIM_CURRENT}|g" \
        "${INSTALLER_DIR}/templates/hdim-logrotate.tmpl" > "$HDIM_LOGROTATE_CONF"
    chmod 644 "$HDIM_LOGROTATE_CONF"
    log_success "Logrotate configured at ${HDIM_LOGROTATE_CONF}"

    # --- Convenience aliases ---
    cat > "$HDIM_OPS_PROFILE" <<'OPSEOF'
# HDIM Platform Operations Aliases
alias hdim-status='sudo /opt/hdim/current/source/installer/rhel7/hdim-install.sh status'
alias hdim-logs='cd /opt/hdim/current/source && docker compose logs -f'
alias hdim-restart='sudo systemctl restart hdim'
alias hdim-stop='sudo systemctl stop hdim'
alias hdim-start='sudo systemctl start hdim'
OPSEOF
    chmod 644 "$HDIM_OPS_PROFILE"
    log_success "Operations aliases installed (hdim-status, hdim-logs, hdim-restart)"

    # --- Access summary ---
    echo ""
    echo "  ┌──────────────────────────────────────────────────────────┐"
    echo "  │  ACCESS POINTS                                          │"
    echo "  ├──────────────────────────────────────────────────────────┤"
    echo "  │  Clinical Portal:  http://$(hostname -f):4200           │"
    echo "  │  API Gateway:      http://$(hostname -f):8080           │"
    echo "  │  Health Check:     http://$(hostname -f):8080/actuator/health │"
    echo "  ├──────────────────────────────────────────────────────────┤"
    echo "  │  Systemd:          systemctl status hdim                │"
    echo "  │  Logs:             journalctl -u hdim -f               │"
    echo "  │  Install log:      ${INSTALL_LOG}                      │"
    echo "  └──────────────────────────────────────────────────────────┘"
    echo ""
}

#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Secrets Generation
# ==============================================================================
# Reads .env.example, generates secrets for CHANGE_ME values, writes .env.
# ==============================================================================

run_secrets() {
    log_step 5 "Generating secrets"

    local release_dir="${CURRENT_RELEASE_DIR:-${HDIM_RELEASES}/pending}"
    local env_file="${release_dir}/.env"
    local env_example="${release_dir}/source/.env.example"

    # If .env already exists (upgrade scenario), skip
    if [[ -f "$env_file" ]]; then
        log_info "Existing .env found — preserving secrets"
        return 0
    fi

    if [[ ! -f "$env_example" ]]; then
        log_warn ".env.example not found at ${env_example} — generating minimal .env"
        env_example=""
    fi

    mkdir -p "$release_dir"

    # Start with the example as base (or empty)
    if [[ -n "$env_example" ]]; then
        cp "$env_example" "$env_file"
    else
        touch "$env_file"
    fi

    log_info "Generating cryptographic secrets..."
    echo ""
    echo "  ┌──────────────────────────────────────────────────────────┐"
    echo "  │  GENERATED SECRETS — SAVE THESE IN A SECURE LOCATION    │"
    echo "  └──────────────────────────────────────────────────────────┘"
    echo ""

    for entry in "${GENERATED_SECRETS[@]}"; do
        local var_name="${entry%%:*}"
        local byte_length="${entry##*:}"
        local secret
        secret="$(openssl rand -base64 "$byte_length" | tr -d '\n')"

        # Replace in .env file — handle both CHANGE_ME patterns
        sed -i "s|${var_name}=CHANGE_ME[A-Z_]*|${var_name}=${secret}|g" "$env_file"

        # Also ensure the variable exists if it wasn't in the template
        if ! grep -q "^${var_name}=" "$env_file"; then
            echo "${var_name}=${secret}" >> "$env_file"
        fi

        # Display to operator (truncated for safety)
        printf "  %-35s %s...\n" "$var_name" "${secret:0:20}"
    done

    # Production hardening: disable dev mode
    sed -i 's|GATEWAY_AUTH_DEV_MODE=true|GATEWAY_AUTH_DEV_MODE=false|g' "$env_file"
    if ! grep -q "^GATEWAY_AUTH_DEV_MODE=" "$env_file"; then
        echo "GATEWAY_AUTH_DEV_MODE=false" >> "$env_file"
    fi

    echo ""
    log_warn "The secrets above are shown ONCE. Store them securely."
    echo ""

    # Lock down permissions
    chmod 600 "$env_file"
    chown "${HDIM_USER}:${HDIM_GROUP}" "$env_file"

    log_success "Secrets written to ${env_file} (mode 0600, owner ${HDIM_USER})"
}

#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Profile Selection
# ==============================================================================
# Interactive menu or --profile flag for deployment profile selection.
# Writes /etc/hdim/hdim.conf.
# ==============================================================================

# Profile descriptions and resource estimates
declare -A PROFILE_DESC=(
    [light]="Infrastructure only (PostgreSQL, Redis, Kafka)"
    [core]="Core services + 4 gateways (~16 containers)"
    [ai]="Core + AI/ML services (~20 containers)"
    [analytics]="Core + analytics/reporting (~22 containers)"
    [full]="All 51+ services (~55 containers)"
    [healthix]="Healthix HIE scale (16M patients, full + HA + tuned resources)"
)

declare -A PROFILE_RAM=(
    [light]="8GB"
    [core]="16GB"
    [ai]="24GB"
    [analytics]="24GB"
    [full]="32GB+"
    [healthix]="128GB"
)

declare -A PROFILE_DISK=(
    [light]="20GB"
    [core]="40GB"
    [ai]="50GB"
    [analytics]="50GB"
    [full]="60GB+"
    [healthix]="2TB NVMe SSD"
)

run_profile() {
    log_step 4 "Profile selection"

    local selected_profile="${1:-}"

    # Validate if provided via flag
    if [[ -n "$selected_profile" ]]; then
        if ! echo "$VALID_PROFILES" | grep -qw "$selected_profile"; then
            die "Invalid profile: $selected_profile (valid: $VALID_PROFILES)"
        fi
        log_info "Profile selected via flag: $selected_profile"
    else
        # Interactive selection
        echo ""
        echo "  Select a deployment profile:"
        echo ""
        local i=1
        for p in light core ai analytics full healthix; do
            printf "    %d) %-12s %s\n" "$i" "$p" "${PROFILE_DESC[$p]}"
            printf "       %s\n" "RAM: ${PROFILE_RAM[$p]} | Disk: ${PROFILE_DISK[$p]}"
            echo ""
            ((i++))
        done

        local choice
        while true; do
            read -rp "  Enter choice [1-6] (recommended: 2 for core): " choice
            case "$choice" in
                1) selected_profile="light"; break ;;
                2) selected_profile="core"; break ;;
                3) selected_profile="ai"; break ;;
                4) selected_profile="analytics"; break ;;
                5) selected_profile="full"; break ;;
                6) selected_profile="healthix"; break ;;
                *) echo "  Invalid choice. Enter 1-6." ;;
            esac
        done
    fi

    # Export for other modules
    PROFILE="$selected_profile"
    export PROFILE

    # Write /etc/hdim/hdim.conf
    mkdir -p "$(dirname "$HDIM_CONF")"

    local install_date
    install_date="$(date '+%Y-%m-%d %H:%M:%S')"
    local hdim_version="${VERSION:-$(git -C "$INSTALLER_DIR" rev-parse --short HEAD 2>/dev/null || echo 'local')}"

    sed -e "s|\${INSTALL_DATE}|${install_date}|g" \
        -e "s|\${HDIM_PROFILE}|${selected_profile}|g" \
        -e "s|\${HDIM_BASE}|${HDIM_BASE}|g" \
        -e "s|\${HDIM_VERSION}|${hdim_version}|g" \
        -e "s|\${JAVA_INSTALL_DIR}|${JAVA_INSTALL_DIR}|g" \
        "${INSTALLER_DIR}/templates/hdim.conf.tmpl" > "$HDIM_CONF"

    chmod 644 "$HDIM_CONF"
    log_success "Profile '${selected_profile}' written to ${HDIM_CONF}"

    # Print resource estimate
    log_info "Resource estimate for '${selected_profile}':"
    log_info "  RAM: ${PROFILE_RAM[$selected_profile]}"
    log_info "  Disk: ${PROFILE_DISK[$selected_profile]}"
}

#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Pre-flight Checks
# ==============================================================================
# Validates the target system before installation begins.
# ==============================================================================

run_preflight() {
    log_step 0 "Pre-flight system checks"

    local errors=0

    # --- 1. RHEL 7 / CentOS 7 detection ---
    if [[ -f /etc/redhat-release ]]; then
        local release
        release="$(cat /etc/redhat-release)"
        if echo "$release" | grep -qE '(Red Hat Enterprise Linux|CentOS).* 7\.'; then
            log_success "OS: $release"
        else
            log_warn "Detected: $release (not RHEL/CentOS 7 — proceed with caution)"
        fi
    else
        log_warn "Cannot detect /etc/redhat-release — this installer targets RHEL 7"
    fi

    # --- 2. Kernel version ---
    local kernel
    kernel="$(uname -r)"
    if [[ "$(printf '%s\n' "$MIN_KERNEL" "$kernel" | sort -V | head -1)" == "$MIN_KERNEL" ]]; then
        log_success "Kernel: $kernel (>= $MIN_KERNEL)"
    else
        log_fail "Kernel $kernel is below minimum $MIN_KERNEL"
        ((errors++))
    fi

    # --- 3. glibc version ---
    local glibc
    glibc="$(ldd --version 2>&1 | head -1 | grep -oE '[0-9]+\.[0-9]+$' || echo "0.0")"
    if [[ "$(printf '%s\n' "$MIN_GLIBC" "$glibc" | sort -V | head -1)" == "$MIN_GLIBC" ]]; then
        log_success "glibc: $glibc (>= $MIN_GLIBC)"
    else
        log_fail "glibc $glibc is below minimum $MIN_GLIBC"
        ((errors++))
    fi

    # --- 4. SELinux mode ---
    if command -v getenforce &>/dev/null; then
        local selinux_mode
        selinux_mode="$(getenforce 2>/dev/null || echo "Unknown")"
        log_info "SELinux: $selinux_mode"
        if [[ "$selinux_mode" == "Enforcing" ]]; then
            log_info "SELinux enforcing — will configure container_var_lib_t contexts"
        fi
    else
        log_info "SELinux: not detected"
    fi

    # --- 5. Disk space ---
    local avail_gb
    avail_gb="$(df -BG /opt 2>/dev/null | tail -1 | awk '{print $4}' | tr -d 'G')"
    if [[ -z "$avail_gb" ]]; then
        avail_gb=0
    fi
    local min_disk="$MIN_DISK_CORE_GB"
    if [[ "$PROFILE" == "light" ]]; then
        min_disk="$MIN_DISK_LIGHT_GB"
    elif [[ "$PROFILE" == "healthix" ]]; then
        min_disk=2000  # 2 TB
    elif [[ "$PROFILE" == "full" || "$PROFILE" == "analytics" || "$PROFILE" == "ai" ]]; then
        min_disk="$MIN_DISK_FULL_GB"
    fi
    if (( avail_gb >= min_disk )); then
        log_success "Disk: ${avail_gb}GB available on /opt (>= ${min_disk}GB)"
    else
        log_fail "Disk: ${avail_gb}GB available on /opt (need >= ${min_disk}GB)"
        ((errors++))
    fi

    # --- 6. RAM ---
    local total_ram_gb
    total_ram_gb="$(free -g | awk '/^Mem:/{print $2}')"
    local min_ram="$MIN_RAM_CORE_GB"
    if [[ "$PROFILE" == "light" ]]; then
        min_ram="$MIN_RAM_LIGHT_GB"
    elif [[ "$PROFILE" == "healthix" ]]; then
        min_ram=128
    elif [[ "$PROFILE" == "full" || "$PROFILE" == "analytics" || "$PROFILE" == "ai" ]]; then
        min_ram="$MIN_RAM_FULL_GB"
    fi
    if (( total_ram_gb >= min_ram )); then
        log_success "RAM: ${total_ram_gb}GB (>= ${min_ram}GB)"
    else
        log_fail "RAM: ${total_ram_gb}GB (need >= ${min_ram}GB)"
        ((errors++))
    fi

    # --- 7. CPU cores ---
    local cpu_cores
    cpu_cores="$(nproc 2>/dev/null || echo 1)"
    if (( cpu_cores >= MIN_CPU_CORES )); then
        log_success "CPU: ${cpu_cores} cores (>= ${MIN_CPU_CORES})"
    else
        log_fail "CPU: ${cpu_cores} cores (need >= ${MIN_CPU_CORES})"
        ((errors++))
    fi

    # --- 8. Internet connectivity ---
    log_info "Checking internet connectivity..."
    local connectivity_ok=true
    for url in "https://github.com" "https://download.docker.com" "https://rpm.nodesource.com"; do
        if curl -sfL --connect-timeout 10 --max-time 15 "$url" -o /dev/null 2>/dev/null; then
            log_success "Reachable: $url"
        else
            log_fail "Unreachable: $url"
            connectivity_ok=false
        fi
    done
    if [[ "$connectivity_ok" != "true" ]]; then
        log_error "Internet connectivity issues detected — connected install requires network access"
        ((errors++))
    fi

    # --- 9. Port conflicts ---
    log_info "Checking for port conflicts..."
    local all_ports=("${INFRA_PORTS[@]}" "${SERVICE_PORTS[@]}" "${FRONTEND_PORTS[@]}")
    for port in "${all_ports[@]}"; do
        if ss -tlnp 2>/dev/null | grep -q ":${port} " || \
           netstat -tlnp 2>/dev/null | grep -q ":${port} "; then
            log_fail "Port $port is already in use"
            ((errors++))
        fi
    done
    if (( errors == 0 )); then
        log_success "No port conflicts detected"
    fi

    # --- 10. NTP synchronization ---
    if command -v timedatectl &>/dev/null; then
        if timedatectl status 2>/dev/null | grep -q "NTP synchronized: yes\|System clock synchronized: yes"; then
            log_success "NTP: synchronized (HIPAA time accuracy requirement)"
        else
            log_warn "NTP not synchronized — HIPAA requires accurate timestamps"
            log_warn "  Fix: timedatectl set-ntp true"
        fi
    elif command -v ntpstat &>/dev/null; then
        if ntpstat &>/dev/null; then
            log_success "NTP: synchronized"
        else
            log_warn "NTP not synchronized"
        fi
    else
        log_warn "Cannot verify NTP status — ensure time synchronization is configured"
    fi

    # --- 11. ulimit nofile ---
    local current_nofile
    current_nofile="$(ulimit -n 2>/dev/null || echo 0)"
    if (( current_nofile >= MIN_ULIMIT_NOFILE )); then
        log_success "ulimit nofile: $current_nofile (>= $MIN_ULIMIT_NOFILE)"
    else
        log_warn "ulimit nofile: $current_nofile (Kafka needs >= $MIN_ULIMIT_NOFILE)"
        log_warn "  Fix: Add to /etc/security/limits.conf:"
        log_warn "    * soft nofile $MIN_ULIMIT_NOFILE"
        log_warn "    * hard nofile $MIN_ULIMIT_NOFILE"
    fi

    # --- Summary ---
    echo ""
    if (( errors > 0 )); then
        die "Pre-flight failed with $errors error(s). Fix the issues above and re-run."
    fi
    log_success "All pre-flight checks passed"
}

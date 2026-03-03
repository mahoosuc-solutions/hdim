#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — System Package Installation
# ==============================================================================
# Adds Docker CE and NodeSource repos, installs required packages.
# ==============================================================================

run_packages() {
    log_step 1 "Installing system packages"

    # --- Docker CE repository ---
    if [[ ! -f /etc/yum.repos.d/docker-ce.repo ]]; then
        log_info "Adding Docker CE repository..."
        yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo || \
            die "Failed to add Docker CE repository"
        log_success "Docker CE repository added"
    else
        log_info "Docker CE repository already configured"
    fi

    # --- NodeSource repository ---
    if ! yum repolist 2>/dev/null | grep -q nodesource; then
        log_info "Adding NodeSource ${NODEJS_MAJOR}.x repository..."
        curl -fsSL "https://rpm.nodesource.com/setup_${NODEJS_MAJOR}.x" | bash - || \
            die "Failed to add NodeSource repository"
        log_success "NodeSource repository added"
    else
        log_info "NodeSource repository already configured"
    fi

    # --- Install packages ---
    local packages=(
        # Docker
        docker-ce
        docker-ce-cli
        containerd.io
        # Node.js
        nodejs
        # Utilities
        curl
        wget
        tar
        git
        openssl
        # Network
        iptables-services
        net-tools
        nc
        lsof
        # Process
        psmisc
        # PostgreSQL client (for pg_dump during upgrades)
        postgresql
    )

    log_info "Installing packages: ${packages[*]}"
    yum install -y "${packages[@]}" || die "Package installation failed"

    # Verify critical binaries
    for bin in docker git node npm curl openssl nc lsof psql; do
        if command -v "$bin" &>/dev/null; then
            log_success "$bin: $(command -v "$bin")"
        else
            log_warn "$bin: not found after install"
        fi
    done

    log_success "System packages installed"
}

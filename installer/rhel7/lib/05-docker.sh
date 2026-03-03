#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Docker Configuration
# ==============================================================================
# Configures Docker daemon, installs Compose v2 plugin, creates hdim user.
# ==============================================================================

run_docker() {
    log_step 3 "Configuring Docker"

    # --- Docker daemon.json ---
    log_info "Writing /etc/docker/daemon.json..."
    mkdir -p /etc/docker
    cat > /etc/docker/daemon.json <<DAEMONJSON
{
    "storage-driver": "overlay2",
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "${DOCKER_LOG_MAX_SIZE}",
        "max-file": "${DOCKER_LOG_MAX_FILE}"
    },
    "default-ulimits": {
        "nofile": {
            "Name": "nofile",
            "Hard": ${MIN_ULIMIT_NOFILE},
            "Soft": ${MIN_ULIMIT_NOFILE}
        }
    },
    "live-restore": true
}
DAEMONJSON
    log_success "Docker daemon.json configured"

    # --- Docker Compose v2 plugin ---
    if docker compose version &>/dev/null; then
        log_info "Docker Compose plugin already installed: $(docker compose version)"
    else
        log_info "Installing Docker Compose v${DOCKER_COMPOSE_VERSION} plugin..."
        mkdir -p "$DOCKER_COMPOSE_PLUGIN_DIR"
        curl -fSL --connect-timeout 30 --max-time 300 \
            -o "${DOCKER_COMPOSE_PLUGIN_DIR}/docker-compose" \
            "$DOCKER_COMPOSE_URL" || die "Failed to download Docker Compose"
        chmod +x "${DOCKER_COMPOSE_PLUGIN_DIR}/docker-compose"
        log_success "Docker Compose $(docker compose version) installed"
    fi

    # --- Enable and start Docker ---
    systemctl enable docker
    systemctl restart docker
    log_success "Docker service enabled and started"

    # Verify Docker is working
    docker info &>/dev/null || die "Docker daemon failed to start"

    # --- SELinux contexts (if enforcing) ---
    if command -v getenforce &>/dev/null && [[ "$(getenforce)" == "Enforcing" ]]; then
        log_info "Configuring SELinux contexts for Docker volumes..."
        for dir in "${HDIM_DATA}/postgres" "${HDIM_DATA}/redis" "${HDIM_DATA}/kafka"; do
            mkdir -p "$dir"
            chcon -Rt container_var_lib_t "$dir" 2>/dev/null || \
                log_warn "Could not set SELinux context on $dir"
        done
        log_success "SELinux contexts configured"
    fi

    # --- Create hdim system user ---
    if id "$HDIM_USER" &>/dev/null; then
        log_info "User '$HDIM_USER' already exists"
    else
        log_info "Creating system user '$HDIM_USER'..."
        useradd -r -m -s /sbin/nologin -d "$HDIM_BASE" "$HDIM_USER" || \
            die "Failed to create user $HDIM_USER"
        log_success "User '$HDIM_USER' created"
    fi

    # Add hdim user to docker group
    usermod -aG docker "$HDIM_USER" 2>/dev/null || true
    log_success "User '$HDIM_USER' added to docker group"

    # --- Create shared directories ---
    for dir in "$HDIM_RELEASES" "$HDIM_SHARED" "$HDIM_DATA/postgres" \
               "$HDIM_DATA/redis" "$HDIM_DATA/kafka" "$HDIM_SSL" \
               "$HDIM_LOGS" "$HDIM_BACKUPS"; do
        mkdir -p "$dir"
    done
    chown -R "${HDIM_USER}:${HDIM_GROUP}" "$HDIM_BASE"
    log_success "Directory structure created under ${HDIM_BASE}"
}

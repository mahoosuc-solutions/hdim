#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Release Setup
# ==============================================================================
# Creates a versioned release directory with project source.
# ==============================================================================

run_release() {
    log_step 6 "Setting up release"

    local version="${1:-}"

    # Determine release tag
    local release_date
    release_date="$(date '+%Y.%m.%d')"
    local short_sha

    if [[ -n "$version" ]]; then
        short_sha="$(echo "$version" | cut -c1-7)"
    elif git -C "$INSTALLER_DIR" rev-parse HEAD &>/dev/null; then
        short_sha="$(git -C "$INSTALLER_DIR" rev-parse --short HEAD)"
    else
        short_sha="local"
    fi

    local release_tag="${release_date}-${short_sha}"
    local release_dir="${HDIM_RELEASES}/${release_tag}"

    if [[ -d "$release_dir" ]]; then
        log_warn "Release directory already exists: $release_dir"
        log_info "Reusing existing release"
    else
        mkdir -p "${release_dir}/source"

        # Determine project root (installer is in installer/rhel7/)
        local project_root
        project_root="$(cd "$INSTALLER_DIR/../.." && pwd)"

        if [[ -n "$version" && -d "$project_root/.git" ]]; then
            # Clone specific version
            log_info "Cloning version ${version} into release directory..."
            git clone --depth 1 --branch "$version" "$project_root" "${release_dir}/source" 2>/dev/null || \
            git clone --depth 1 "$project_root" "${release_dir}/source"
        elif [[ -d "$project_root" ]]; then
            # Rsync from local source
            log_info "Syncing project source to release directory..."
            rsync -a --exclude='.git' \
                     --exclude='node_modules' \
                     --exclude='build' \
                     --exclude='dist' \
                     --exclude='.gradle' \
                     --exclude='.env' \
                     --exclude='.env.local' \
                     "$project_root/" "${release_dir}/source/"
        else
            die "Cannot find project source at $project_root"
        fi
    fi

    # Record profile
    echo "$PROFILE" > "${release_dir}/compose-profile.txt"

    # Set ownership
    chown -R "${HDIM_USER}:${HDIM_GROUP}" "$release_dir"

    # Export for other modules
    CURRENT_RELEASE_DIR="$release_dir"
    export CURRENT_RELEASE_DIR

    log_success "Release ${release_tag} prepared at ${release_dir}"
    log_info "Source: ${release_dir}/source/"
}

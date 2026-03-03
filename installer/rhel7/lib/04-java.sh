#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Temurin JDK 21 Installation
# ==============================================================================
# Downloads, verifies, and extracts Eclipse Temurin JDK 21 to /opt/java.
# Creates /etc/profile.d/hdim-java.sh for JAVA_HOME.
# ==============================================================================

run_java() {
    log_step 2 "Installing Eclipse Temurin JDK ${TEMURIN_MAJOR}"

    # Skip if already installed and correct version
    if [[ -x "${JAVA_INSTALL_DIR}/bin/java" ]]; then
        local installed_version
        installed_version="$("${JAVA_INSTALL_DIR}/bin/java" -version 2>&1 | head -1)"
        log_info "Temurin JDK already installed: $installed_version"
        _write_java_profile
        return 0
    fi

    local tmp_dir
    tmp_dir="$(mktemp -d)"
    trap "rm -rf '$tmp_dir'" RETURN

    local tarball="${tmp_dir}/${TEMURIN_TARBALL}"

    # --- Download ---
    log_info "Downloading Temurin JDK ${TEMURIN_VERSION}..."
    log_info "URL: ${TEMURIN_URL}"
    curl -fSL --progress-bar --connect-timeout 30 --max-time 600 \
        -o "$tarball" "$TEMURIN_URL" || die "Failed to download Temurin JDK"
    log_success "Download complete: $(du -h "$tarball" | cut -f1)"

    # --- SHA256 verification ---
    log_info "Verifying SHA256 checksum..."
    local actual_sha
    actual_sha="$(sha256sum "$tarball" | awk '{print $1}')"
    if [[ "$actual_sha" == "$TEMURIN_SHA256" ]]; then
        log_success "SHA256 verified: ${actual_sha:0:16}..."
    else
        log_error "SHA256 mismatch!"
        log_error "  Expected: $TEMURIN_SHA256"
        log_error "  Actual:   $actual_sha"
        die "Checksum verification failed — download may be corrupted or tampered with"
    fi

    # --- Extract ---
    log_info "Extracting to ${JAVA_INSTALL_DIR}..."
    mkdir -p "$(dirname "$JAVA_INSTALL_DIR")"
    # Extract to temp first, then move (atomic-ish)
    local extract_dir="${tmp_dir}/extract"
    mkdir -p "$extract_dir"
    tar xzf "$tarball" -C "$extract_dir" || die "Failed to extract Temurin tarball"

    # Find the extracted directory (named like jdk-21.0.6+7)
    local jdk_dir
    jdk_dir="$(find "$extract_dir" -maxdepth 1 -type d -name 'jdk-*' | head -1)"
    if [[ -z "$jdk_dir" ]]; then
        die "Could not find extracted JDK directory"
    fi

    # Move into place
    rm -rf "$JAVA_INSTALL_DIR"
    mv "$jdk_dir" "$JAVA_INSTALL_DIR"

    # Verify
    "${JAVA_INSTALL_DIR}/bin/java" -version 2>&1 | head -1 || die "Java binary not functional"
    log_success "Temurin JDK ${TEMURIN_MAJOR} installed to ${JAVA_INSTALL_DIR}"

    # --- Profile script ---
    _write_java_profile
}

_write_java_profile() {
    cat > "$HDIM_JAVA_PROFILE" <<JAVAEOF
# HDIM — Eclipse Temurin JDK ${TEMURIN_MAJOR}
export JAVA_HOME="${JAVA_INSTALL_DIR}"
export PATH="\${JAVA_HOME}/bin:\${PATH}"
JAVAEOF
    chmod 644 "$HDIM_JAVA_PROFILE"

    # Source immediately for this session
    export JAVA_HOME="${JAVA_INSTALL_DIR}"
    export PATH="${JAVA_HOME}/bin:${PATH}"

    log_success "JAVA_HOME=${JAVA_INSTALL_DIR} (written to ${HDIM_JAVA_PROFILE})"
}

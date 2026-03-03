#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Build Services
# ==============================================================================
# Builds backend JARs and Docker images for the selected profile.
# ==============================================================================

# Service lists by profile
_get_profile_services() {
    local profile="$1"
    case "$profile" in
        light)
            # Infrastructure only — no services to build
            echo ""
            ;;
        core)
            echo "gateway-service clinical-gateway fhir-gateway admin-gateway"
            echo "patient-service care-gap-service fhir-service cql-engine-service"
            echo "quality-measure-service auth-service notification-service"
            echo "audit-service"
            ;;
        ai)
            _get_profile_services "core"
            echo "ai-analytics-service"
            ;;
        analytics)
            _get_profile_services "core"
            echo "analytics-service reporting-service"
            ;;
        full|healthix)
            # All services — discover from filesystem
            find "${CURRENT_RELEASE_DIR}/source/backend/modules/services" \
                -maxdepth 1 -mindepth 1 -type d -printf '%f\n' 2>/dev/null | sort
            ;;
    esac
}

run_build() {
    log_step 7 "Building services"

    local source_dir="${CURRENT_RELEASE_DIR}/source"
    local backend_dir="${source_dir}/backend"

    # Ensure JAVA_HOME is set
    if [[ -z "${JAVA_HOME:-}" ]]; then
        source "$HDIM_JAVA_PROFILE" 2>/dev/null || true
    fi
    log_info "JAVA_HOME=${JAVA_HOME}"

    # --- Download Gradle dependencies ---
    if [[ -d "$backend_dir" ]]; then
        log_info "Pre-caching Gradle dependencies..."
        cd "$backend_dir"
        sudo -u "$HDIM_USER" ./gradlew downloadDependencies --no-daemon 2>&1 | tail -5 || \
            log_warn "Dependency download had issues (may be OK if cached)"

        # --- Build service JARs ---
        local services
        services="$(_get_profile_services "$PROFILE" | tr '\n' ' ')"

        if [[ -z "${services// /}" ]]; then
            log_info "Profile '$PROFILE' has no backend services to build"
        else
            log_info "Building services for profile '$PROFILE'..."
            for svc in $services; do
                local module_path=":modules:services:${svc}"
                if [[ -d "modules/services/${svc}" ]]; then
                    log_info "  Building ${svc}..."
                    sudo -u "$HDIM_USER" ./gradlew "${module_path}:bootJar" -x test --no-daemon 2>&1 | tail -3 || \
                        log_warn "  Build warning for ${svc} (continuing)"
                fi
            done
        fi
        cd "$source_dir"
    fi

    # --- Build Docker images ---
    log_info "Building Docker images..."
    cd "$source_dir"
    if [[ "$PROFILE" == "light" ]]; then
        # Light profile only needs infra — pre-built images
        log_info "Light profile uses pre-built infrastructure images"
    else
        docker compose build 2>&1 | tail -20 || \
            log_warn "Docker build had warnings (check logs)"
    fi

    # --- Build Angular clinical-portal ---
    if [[ -d "${source_dir}/apps/clinical-portal" && "$PROFILE" != "light" ]]; then
        log_info "Building Angular Clinical Portal..."
        cd "$source_dir"
        npm ci --prefer-offline 2>&1 | tail -5 || log_warn "npm ci had warnings"
        npx nx build clinical-portal --configuration production 2>&1 | tail -10 || \
            log_warn "Angular build had warnings"
    fi

    cd "$source_dir"
    log_success "Build complete for profile '$PROFILE'"
}

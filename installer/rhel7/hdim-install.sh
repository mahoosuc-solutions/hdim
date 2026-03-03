#!/bin/bash
# ==============================================================================
# HDIM Production Installer for RHEL 7 / CentOS 7
# ==============================================================================
#
# Usage:
#   hdim-install.sh install  [--profile light|core|ai|analytics|full] [--version <git-ref>]
#   hdim-install.sh upgrade  [--version <git-ref>]
#   hdim-install.sh rollback [--to <release-dir>]
#   hdim-install.sh status
#   hdim-install.sh uninstall [--keep-data]
#
# See docs/deployment/RHEL7-DEPLOYMENT-GUIDE.md for full documentation.
# ==============================================================================

set -euo pipefail

# Resolve installer directory (follow symlinks)
INSTALLER_DIR="$(cd "$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")" && pwd)"
readonly INSTALLER_DIR

# --- Source library modules (order matters) ---
source "${INSTALLER_DIR}/lib/00-constants.sh"
source "${INSTALLER_DIR}/lib/01-logging.sh"

# --- Parse global options ---
COMMAND=""
PROFILE=""
VERSION=""
ROLLBACK_TARGET=""
KEEP_DATA=false

usage() {
    cat <<'EOF'
HDIM Production Installer for RHEL 7 / CentOS 7

Usage:
  hdim-install.sh <command> [options]

Commands:
  install    Full installation (first time)
  upgrade    Upgrade to a new version
  rollback   Roll back to a previous release
  status     Show platform status
  uninstall  Remove HDIM platform

Options for 'install':
  --profile <name>   Deployment profile: light, core, ai, analytics, full (default: interactive)
  --version <ref>    Git ref to deploy (default: current directory)

Options for 'upgrade':
  --version <ref>    Git ref to upgrade to (default: latest)

Options for 'rollback':
  --to <dir>         Release directory to roll back to (default: previous)

Options for 'uninstall':
  --keep-data        Preserve database volumes and backups

General:
  --help, -h         Show this help message

EOF
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        install|upgrade|rollback|status|uninstall)
            COMMAND="$1"; shift ;;
        --profile)
            PROFILE="$2"; shift 2 ;;
        --version)
            VERSION="$2"; shift 2 ;;
        --to)
            ROLLBACK_TARGET="$2"; shift 2 ;;
        --keep-data)
            KEEP_DATA=true; shift ;;
        --help|-h)
            usage ;;
        *)
            log_error "Unknown argument: $1"
            usage ;;
    esac
done

if [[ -z "$COMMAND" ]]; then
    log_error "No command specified."
    usage
fi

# --- Root check ---
if [[ $EUID -ne 0 ]]; then
    die "This installer must be run as root (use sudo)."
fi

# --- Dispatch commands ---
case "$COMMAND" in
    install)
        log_banner "HDIM Platform Installer v${HDIM_INSTALLER_VERSION}"
        log_info "Command: install | Profile: ${PROFILE:-interactive} | Version: ${VERSION:-local}"

        source "${INSTALLER_DIR}/lib/02-preflight.sh"
        source "${INSTALLER_DIR}/lib/03-packages.sh"
        source "${INSTALLER_DIR}/lib/04-java.sh"
        source "${INSTALLER_DIR}/lib/05-docker.sh"
        source "${INSTALLER_DIR}/lib/06-profile.sh"
        source "${INSTALLER_DIR}/lib/07-secrets.sh"
        source "${INSTALLER_DIR}/lib/08-release.sh"
        source "${INSTALLER_DIR}/lib/09-build.sh"
        source "${INSTALLER_DIR}/lib/10-start.sh"
        source "${INSTALLER_DIR}/lib/11-health.sh"
        source "${INSTALLER_DIR}/lib/12-systemd.sh"

        run_preflight
        run_packages
        run_java
        run_docker
        run_profile "$PROFILE"
        run_secrets
        run_release "$VERSION"
        run_build
        run_start
        run_health
        run_systemd

        log_banner "Installation Complete"
        log_info "HDIM platform is running with profile: $(cat "$HDIM_CONF" 2>/dev/null | grep HDIM_PROFILE | cut -d= -f2)"
        log_info "Access the Clinical Portal at: http://$(hostname -f):4200"
        log_info "Platform status: hdim-install.sh status"
        log_info "Install log: ${INSTALL_LOG}"
        ;;

    upgrade)
        log_banner "HDIM Platform Upgrade v${HDIM_INSTALLER_VERSION}"
        source "${INSTALLER_DIR}/lib/13-upgrade.sh"
        run_upgrade "$VERSION"
        ;;

    rollback)
        log_banner "HDIM Platform Rollback v${HDIM_INSTALLER_VERSION}"
        source "${INSTALLER_DIR}/lib/14-rollback.sh"
        run_rollback "$ROLLBACK_TARGET"
        ;;

    status)
        source "${INSTALLER_DIR}/lib/11-health.sh"
        echo ""
        log_banner "HDIM Platform Status"

        # Show installed version
        if [[ -L "$HDIM_CURRENT" ]]; then
            local_release="$(readlink -f "$HDIM_CURRENT")"
            log_info "Active release: $(basename "$local_release")"
        else
            log_warn "No active release symlink found at ${HDIM_CURRENT}"
        fi

        # Show profile
        if [[ -f "$HDIM_CONF" ]]; then
            source "$HDIM_CONF"
            log_info "Profile: ${HDIM_PROFILE:-unknown}"
        fi

        # Show systemd status
        if systemctl is-active hdim.service &>/dev/null; then
            log_success "systemd unit: active"
        else
            log_fail "systemd unit: inactive"
        fi

        # Show container status
        if [[ -L "$HDIM_CURRENT" ]]; then
            cd "$(readlink -f "$HDIM_CURRENT")/source" 2>/dev/null || true
            docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || \
                log_warn "Could not query Docker Compose"
        fi
        ;;

    uninstall)
        log_banner "HDIM Platform Uninstall"
        source "${INSTALLER_DIR}/lib/15-uninstall.sh"
        run_uninstall "$KEEP_DATA"
        ;;

    *)
        die "Unknown command: $COMMAND"
        ;;
esac

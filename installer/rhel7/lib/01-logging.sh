#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Logging
# ==============================================================================
# Colored terminal output with simultaneous tee to install.log.
# ==============================================================================

# Colors (disabled when stdout is not a terminal)
if [[ -t 1 ]]; then
    readonly _RED='\033[0;31m'
    readonly _GREEN='\033[0;32m'
    readonly _YELLOW='\033[1;33m'
    readonly _BLUE='\033[0;34m'
    readonly _CYAN='\033[0;36m'
    readonly _BOLD='\033[1m'
    readonly _NC='\033[0m'
else
    readonly _RED='' _GREEN='' _YELLOW='' _BLUE='' _CYAN='' _BOLD='' _NC=''
fi

# Log file — set early so all modules can use it
INSTALL_LOG="${HDIM_LOGS:-/opt/hdim/shared/logs}/install.log"

_ensure_log_dir() {
    local log_dir
    log_dir="$(dirname "$INSTALL_LOG")"
    if [[ ! -d "$log_dir" ]]; then
        mkdir -p "$log_dir" 2>/dev/null || true
    fi
}

_log() {
    local level="$1" color="$2"
    shift 2
    local ts
    ts="$(date '+%Y-%m-%d %H:%M:%S')"
    local msg="[${ts}] [${level}] $*"
    # Terminal output with color
    echo -e "${color}[${level}]${_NC} $*" >&2
    # File output without color
    _ensure_log_dir
    echo "$msg" >> "$INSTALL_LOG" 2>/dev/null || true
}

log_info()  { _log "INFO"  "$_GREEN"  "$@"; }
log_warn()  { _log "WARN"  "$_YELLOW" "$@"; }
log_error() { _log "ERROR" "$_RED"    "$@"; }
log_debug() { _log "DEBUG" "$_CYAN"   "$@"; }

log_step() {
    local step_num="$1"
    shift
    echo "" >&2
    echo -e "${_BOLD}${_BLUE}==> Phase ${step_num}: $*${_NC}" >&2
    _ensure_log_dir
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [PHASE] Phase ${step_num}: $*" >> "$INSTALL_LOG" 2>/dev/null || true
    echo "" >&2
}

log_banner() {
    local width=60
    local border
    border=$(printf '=%.0s' $(seq 1 $width))
    echo "" >&2
    echo -e "${_BOLD}${border}${_NC}" >&2
    echo -e "${_BOLD}  $*${_NC}" >&2
    echo -e "${_BOLD}${border}${_NC}" >&2
    echo "" >&2
}

log_success() {
    echo -e "  ${_GREEN}✓${_NC} $*" >&2
    _ensure_log_dir
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [OK] $*" >> "$INSTALL_LOG" 2>/dev/null || true
}

log_fail() {
    echo -e "  ${_RED}✗${_NC} $*" >&2
    _ensure_log_dir
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [FAIL] $*" >> "$INSTALL_LOG" 2>/dev/null || true
}

# Fatal error — log, print, and exit
die() {
    log_error "$@"
    log_error "Installation aborted. See ${INSTALL_LOG} for details."
    exit 1
}

# Run a command, logging stdout/stderr to the install log
run_logged() {
    local desc="$1"
    shift
    log_info "$desc"
    _ensure_log_dir
    if "$@" >> "$INSTALL_LOG" 2>&1; then
        log_success "$desc"
        return 0
    else
        log_fail "$desc (exit code $?)"
        return 1
    fi
}

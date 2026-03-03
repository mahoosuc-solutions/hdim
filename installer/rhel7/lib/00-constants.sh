#!/bin/bash
# ==============================================================================
# HDIM RHEL 7 Installer — Constants & Version Pins
# ==============================================================================
# All external versions, URLs, and checksums in one place.
# Update this file when upgrading dependencies.
# ==============================================================================

# --- Installer version ---
readonly HDIM_INSTALLER_VERSION="1.0.0"

# --- Java (Eclipse Temurin 21 LTS) ---
readonly TEMURIN_VERSION="21.0.6+7"
readonly TEMURIN_MAJOR="21"
readonly TEMURIN_TARBALL="OpenJDK21U-jdk_x64_linux_hotspot_${TEMURIN_VERSION//+/_}.tar.gz"
readonly TEMURIN_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-${TEMURIN_VERSION}/${TEMURIN_TARBALL}"
readonly TEMURIN_SHA256="d89b90e63028e3f997b953cf6a3b4506d3bb8cf20fce8b47c81ee0ca2c98de3b"
readonly JAVA_INSTALL_DIR="/opt/java/temurin-21"

# --- Docker ---
readonly DOCKER_COMPOSE_VERSION="2.32.4"
readonly DOCKER_COMPOSE_URL="https://github.com/docker/compose/releases/download/v${DOCKER_COMPOSE_VERSION}/docker-compose-linux-x86_64"
readonly DOCKER_COMPOSE_PLUGIN_DIR="/usr/local/lib/docker/cli-plugins"

# --- Node.js ---
readonly NODEJS_MAJOR="20"

# --- Filesystem layout ---
readonly HDIM_BASE="/opt/hdim"
readonly HDIM_RELEASES="${HDIM_BASE}/releases"
readonly HDIM_CURRENT="${HDIM_BASE}/current"
readonly HDIM_SHARED="${HDIM_BASE}/shared"
readonly HDIM_BACKUPS="${HDIM_BASE}/backups"
readonly HDIM_DATA="${HDIM_SHARED}/data"
readonly HDIM_LOGS="${HDIM_SHARED}/logs"
readonly HDIM_SSL="${HDIM_SHARED}/ssl"
readonly HDIM_CONF="/etc/hdim/hdim.conf"
readonly HDIM_SYSTEMD_UNIT="/etc/systemd/system/hdim.service"
readonly HDIM_LOGROTATE_CONF="/etc/logrotate.d/hdim"
readonly HDIM_JAVA_PROFILE="/etc/profile.d/hdim-java.sh"
readonly HDIM_OPS_PROFILE="/etc/profile.d/hdim-ops.sh"

# --- System user ---
readonly HDIM_USER="hdim"
readonly HDIM_GROUP="hdim"

# --- Minimum system requirements ---
readonly MIN_KERNEL="3.10.0-1062"
readonly MIN_GLIBC="2.17"
readonly MIN_DISK_LIGHT_GB=20
readonly MIN_DISK_CORE_GB=40
readonly MIN_DISK_FULL_GB=60
readonly MIN_RAM_LIGHT_GB=8
readonly MIN_RAM_CORE_GB=16
readonly MIN_RAM_FULL_GB=32
readonly MIN_CPU_CORES=4
readonly MIN_ULIMIT_NOFILE=65536

# --- Ports to check ---
readonly INFRA_PORTS=(5435 6380 9094)
readonly SERVICE_PORTS=(8080 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091 8092 8093 8094 8095 8096 8097 8098 8099)
readonly FRONTEND_PORTS=(4200)

# --- Profiles ---
# Each profile is an array of docker compose service names.
# "light"     — infrastructure only (db, cache, broker)
# "core"      — infra + primary services + gateways
# "ai"        — core + AI/ML services
# "analytics" — core + analytics/reporting services
# "full"      — everything
readonly VALID_PROFILES="light core ai analytics full healthix"

# --- Secrets to generate (variable name : byte length) ---
# These are parsed from .env.example lines containing CHANGE_ME
readonly GENERATED_SECRETS=(
    "POSTGRES_PASSWORD:32"
    "SPRING_DATASOURCE_PASSWORD:32"
    "REDIS_PASSWORD:32"
    "JWT_SECRET:64"
    "GATEWAY_AUTH_SIGNING_SECRET:32"
    "AUDIT_ENCRYPTION_KEY:32"
    "GRAFANA_ADMIN_PASSWORD:32"
)

# --- Health check ---
readonly HEALTH_TIMEOUT=300
readonly SMOKE_TEST_MODE="--quick"

# --- Docker daemon config ---
readonly DOCKER_LOG_MAX_SIZE="50m"
readonly DOCKER_LOG_MAX_FILE="5"

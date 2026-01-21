#!/bin/bash
# Deployment validation wrapper for on-prem and cloud hosted environments.

set -e

log_info() { echo "[deployment-validate] $1"; }

log_info "Running system validation..."
bash ./validate-system.sh

log_info "Running FHIR UUID/reference validation..."
bash ./validate-fhir-data.sh

log_info "Running service data validation..."
bash ./scripts/validate-all-services-data.sh

if [ "$RUN_SCREENSHOTS" = "true" ]; then
    log_info "Capturing demo screenshots..."
    bash ./scripts/run-demo-screenshots.sh
fi

if [ "$RUN_E2E" = "true" ]; then
    log_info "Running clinical portal E2E suite..."
    npm run e2e:clinical-portal:cli
fi

if [ "$RUN_DEMO_FLOW" = "true" ]; then
    log_info "Launching interactive demo walkthrough..."
    bash ./demo-full-system.sh
fi

log_info "Deployment validation complete."

#!/bin/bash
# Quick launcher for release validation workflow
# This is equivalent to /release-validation command

set -euo pipefail

VERSION="${1:-}"

if [ -z "$VERSION" ]; then
    echo "Usage: ./validate-release.sh v1.3.0"
    echo ""
    echo "Examples:"
    echo "  ./validate-release.sh v1.3.0-test    # Test run"
    echo "  ./validate-release.sh v1.3.0          # Production run"
    exit 1
fi

# Forward to the actual workflow launcher
exec ./scripts/release-validation/run-release-validation.sh "$VERSION"

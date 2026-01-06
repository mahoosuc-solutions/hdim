#!/bin/bash
# HDIM Demo CLI Wrapper Script
#
# Usage:
#   ./demo-cli.sh reset                    # Reset all demo data
#   ./demo-cli.sh load-scenario <name>     # Load specific scenario
#   ./demo-cli.sh list-scenarios           # List available scenarios
#   ./demo-cli.sh generate-patients        # Generate synthetic patients
#   ./demo-cli.sh status                   # Check demo system status
#   ./demo-cli.sh snapshot create <name>   # Create database snapshot
#   ./demo-cli.sh snapshot restore <name>  # Restore from snapshot
#   ./demo-cli.sh initialize               # Initialize demo environment

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_FILE="$SCRIPT_DIR/build/libs/demo-cli.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Demo CLI JAR not found. Building..."
    echo
    cd "$SCRIPT_DIR/../.."  # Go to backend root
    ./gradlew :tools:demo-cli:bootJar --quiet
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to build demo-cli"
        exit 1
    fi
    echo "Build complete."
    echo
fi

# Run the CLI
java -jar "$JAR_FILE" "$@"

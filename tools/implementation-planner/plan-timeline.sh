#!/bin/bash
# HDIM Implementation Timeline Planner
# Quick launcher script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if ts-node is available
if ! command -v npx &> /dev/null; then
    echo "Error: npx not found. Please install Node.js."
    exit 1
fi

case "$1" in
    "interactive"|"-i"|"")
        echo "Starting interactive planner..."
        npx ts-node --project tsconfig.json interactive-planner.ts
        ;;
    "demo"|"-d")
        profile="${2:-small-practice}"
        echo "Running demo with profile: $profile"
        npx ts-node --project tsconfig.json timeline-calculator.ts --demo "$profile"
        ;;
    "help"|"-h"|"--help")
        echo ""
        echo "HDIM Implementation Timeline Planner"
        echo ""
        echo "Usage:"
        echo "  ./plan-timeline.sh                    Run interactive planner"
        echo "  ./plan-timeline.sh demo [profile]    Run demo with example profile"
        echo "  ./plan-timeline.sh help              Show this help"
        echo ""
        echo "Example Profiles:"
        echo "  solo-practice      - Dr. Martinez (1,200 patients, CSV)"
        echo "  small-practice     - Riverside Primary Care (4,500 patients, FHIR)"
        echo "  fqhc               - Community Health Partners (22,000 patients, n8n)"
        echo "  midsize-aco        - Metro Health Alliance (42,000 lives, SMART)"
        echo "  large-health-system - Regional Medical (180,000 patients, Private Cloud)"
        echo ""
        ;;
    *)
        echo "Unknown command: $1"
        echo "Run './plan-timeline.sh help' for usage"
        exit 1
        ;;
esac

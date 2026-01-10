#!/bin/bash
# HDIM Development Tools - Quick Access Menu
# Central hub for all HDIM custom tools and workflows

set -e

show_menu() {
    echo ""
    echo "🔧 HDIM Development Tools"
    echo "========================="
    echo ""
    echo "Validation Tools:"
    echo "  1) Check HIPAA Compliance"
    echo "  2) Check Multi-Tenant Queries"
    echo "  3) Validate Entity-Migration Sync"
    echo "  4) Check Service Health"
    echo "  5) Run Pre-Commit Checks (ALL)"
    echo ""
    echo "Workflows:"
    echo "  6) Create New Service"
    echo "  7) Run All Services"
    echo "  8) Stop All Services"
    echo "  9) View Service Logs"
    echo ""
    echo "Quick References:"
    echo "  h) HIPAA Compliance Checklist"
    echo "  a) Gateway Trust Auth Guide"
    echo "  e) Entity-Migration Sync Guide"
    echo "  s) Service Registry"
    echo ""
    echo "  q) Quit"
    echo ""
}

run_tool() {
    case $1 in
        1)
            echo "Running HIPAA compliance check..."
            bash .serena/tools/check-hipaa-compliance.sh
            ;;
        2)
            echo "Running multi-tenant query check..."
            bash .serena/tools/check-multitenant-queries.sh
            ;;
        3)
            echo "Running entity-migration validation..."
            bash .serena/tools/validate-entity-migration-sync.sh
            ;;
        4)
            echo "Checking service health..."
            bash .serena/tools/check-service-health.sh
            ;;
        5)
            echo "Running all pre-commit checks..."
            bash .serena/workflows/pre-commit-check.sh
            ;;
        6)
            echo ""
            read -p "Service name (e.g., prescription-service): " service_name
            read -p "Port number (e.g., 8091): " port
            bash .serena/workflows/new-service-setup.sh "$service_name" "$port"
            ;;
        7)
            echo "Starting all services..."
            docker compose up -d
            echo "✅ Services started. Check status with option 4."
            ;;
        8)
            echo "Stopping all services..."
            docker compose down
            echo "✅ Services stopped."
            ;;
        9)
            echo ""
            read -p "Service name (e.g., quality-measure-service): " service_name
            docker compose logs -f "$service_name"
            ;;
        h)
            cat .serena/memories/hipaa-compliance-checklist.md | less
            ;;
        a)
            cat .serena/memories/gateway-trust-auth.md | less
            ;;
        e)
            cat .serena/memories/entity-migration-sync.md | less
            ;;
        s)
            cat .serena/memories/service-registry.md | less
            ;;
        q)
            echo "Goodbye!"
            exit 0
            ;;
        *)
            echo "Invalid option. Please try again."
            ;;
    esac
}

# Main loop
while true; do
    show_menu
    read -p "Select option: " option
    echo ""
    run_tool "$option"
    echo ""
    read -p "Press Enter to continue..."
done

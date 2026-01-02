#!/bin/bash
set -e

# HealthData in Motion - GCP Demo Quick Start
# Project: healthdatainmotion

cat << "EOF"
╔════════════════════════════════════════════════════════════╗
║                                                            ║
║         HealthData in Motion - GCP Demo Setup             ║
║                                                            ║
╚════════════════════════════════════════════════════════════╝
EOF

echo ""
echo "This will set up a cost-optimized GCP demo environment."
echo "Cost: ~\$20/month for 2-4 demos (when stopped: $0/hour)"
echo ""

# Check prerequisites
echo "Checking prerequisites..."
echo ""

# Check gcloud
if ! command -v gcloud &> /dev/null; then
    echo "❌ ERROR: gcloud CLI not found"
    echo ""
    echo "Install gcloud CLI:"
    echo "  https://cloud.google.com/sdk/docs/install"
    echo ""
    exit 1
fi
echo "✅ gcloud CLI installed"

# Check if logged in
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    echo "⚠️  Not logged into gcloud"
    echo ""
    read -p "Login now? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        gcloud auth login
    else
        echo "Please run: gcloud auth login"
        exit 1
    fi
fi
echo "✅ Logged into gcloud"

# Set project
PROJECT_ID="healthcare-data-in-motion"
echo ""
echo "Setting GCP project: $PROJECT_ID"

if ! gcloud projects describe $PROJECT_ID &> /dev/null; then
    echo "❌ ERROR: Project '$PROJECT_ID' not found"
    echo ""
    echo "Options:"
    echo "  1. Create project in GCP Console: https://console.cloud.google.com"
    echo "  2. Use a different project (set PROJECT_ID env variable)"
    echo ""
    exit 1
fi

gcloud config set project $PROJECT_ID
echo "✅ Project set: $PROJECT_ID"
echo ""

# Configuration
ZONE="us-central1-a"
VM_NAME="healthdata-demo"

echo "Configuration:"
echo "  Project:      $PROJECT_ID"
echo "  Zone:         $ZONE"
echo "  VM Name:      $VM_NAME"
echo "  Machine Type: e2-standard-4 (4 vCPU, 16GB RAM)"
echo "  Disk:         100GB SSD"
echo ""

read -p "Continue with VM creation? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Setup cancelled. You can run individual scripts:"
    echo "  ./scripts/gcp-create-demo-vm.sh    # Create VM"
    echo "  ./scripts/gcp-start-demo.sh        # Start demo"
    echo "  ./scripts/gcp-stop-demo.sh         # Stop demo"
    echo "  ./scripts/gcp-demo-status.sh       # Check status"
    exit 0
fi

echo ""
echo "════════════════════════════════════════════════════════"
echo "  Creating VM (this takes ~15 minutes)"
echo "════════════════════════════════════════════════════════"
echo ""

# Run creation script
export PROJECT_ID=$PROJECT_ID
export ZONE=$ZONE
export VM_NAME=$VM_NAME

./scripts/gcp-create-demo-vm.sh

echo ""
echo "════════════════════════════════════════════════════════"
echo "  ✅ VM Created Successfully!"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Next Steps:"
echo ""
echo "1️⃣  Deploy Application to VM:"
echo ""
echo "   # SSH into VM"
echo "   gcloud compute ssh $VM_NAME --zone=$ZONE"
echo ""
echo "   # Inside VM, run:"
echo "   cd /opt/healthdata"
echo "   git clone https://github.com/YOUR_ORG/healthdata-in-motion.git"
echo "   cd healthdata-in-motion/backend"
echo "   ./gradlew build -x test"
echo "   cd .."
echo "   docker-compose up -d"
echo "   ./load-demo-data.sh"
echo "   exit"
echo ""
echo "2️⃣  Test the demo:"
echo ""
echo "   ./scripts/gcp-demo-status.sh"
echo ""
echo "3️⃣  Stop VM to save costs:"
echo ""
echo "   ./scripts/gcp-stop-demo.sh"
echo ""
echo "════════════════════════════════════════════════════════"
echo "  Cost Optimization"
echo "════════════════════════════════════════════════════════"
echo ""
echo "💰 Running:  \$0.15/hour (~\$110/month if left on)"
echo "💰 Stopped:  \$0/hour (only \$17/month for storage)"
echo ""
echo "💡 TIP: Always stop VM after demos to save ~\$100/month!"
echo ""
echo "Quick Commands:"
echo "  Start:  ./scripts/gcp-start-demo.sh"
echo "  Stop:   ./scripts/gcp-stop-demo.sh"
echo "  Status: ./scripts/gcp-demo-status.sh"
echo ""

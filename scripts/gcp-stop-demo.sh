#!/bin/bash
set -e

# HealthData Demo Shutdown Script
# Stops Docker services and GCP VM to minimize costs

# Configuration
PROJECT_ID=${PROJECT_ID:-"healthcare-data-in-motion"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}

echo "================================================"
echo "  🛑 Stopping HealthData Demo"
echo "================================================"
echo ""

# Check if VM exists
if ! gcloud compute instances describe $VM_NAME --project=$PROJECT_ID --zone=$ZONE &>/dev/null; then
    echo "❌ ERROR: VM '$VM_NAME' not found in zone $ZONE"
    exit 1
fi

# Check current status
STATUS=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(status)")

if [ "$STATUS" == "TERMINATED" ]; then
    echo "ℹ️  VM is already STOPPED"
    echo "💾 Data preserved for next startup"
    echo "💰 Current cost: $0/hour (only storage: ~$17/month)"
    exit 0
fi

echo "Step 1/2: Stopping Docker services (preserving data)..."

# Stop Docker containers gracefully
gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
    set -e
    cd /opt/healthdata/healthdata-in-motion

    echo "🐳 Stopping Docker containers..."
    docker-compose stop

    echo ""
    echo "📊 Container Status:"
    docker-compose ps

    echo ""
    echo "💾 Data volumes preserved for next startup"
' 2>/dev/null

echo "✅ Docker services stopped"
echo ""

echo "Step 2/2: Stopping VM..."
gcloud compute instances stop $VM_NAME --project=$PROJECT_ID --zone=$ZONE

echo "⏳ Waiting for VM to stop..."
sleep 10

# Verify stopped
STATUS=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(status)")

echo ""
echo "================================================"
echo "  ✅ Demo Stopped Successfully"
echo "================================================"
echo ""
echo "VM Status:      $STATUS"
echo "Data:           ✅ Preserved"
echo "Docker Images:  ✅ Preserved"
echo "Database:       ✅ Preserved"
echo ""
echo "💰 Cost Savings:"
echo "   Before: $0.15/hour (~$110/month)"
echo "   After:  $0/hour (only $17/month for storage)"
echo ""
echo "🚀 To restart:"
echo "   ./scripts/gcp-start-demo.sh"
echo ""

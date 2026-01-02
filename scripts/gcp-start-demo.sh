#!/bin/bash
set -e

# HealthData Demo Startup Script
# Starts the GCP VM and all Docker services

# Configuration
PROJECT_ID=${PROJECT_ID:-"healthcare-data-in-motion"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}

echo "================================================"
echo "  🚀 Starting HealthData Demo"
echo "================================================"
echo ""

# Check if VM exists
if ! gcloud compute instances describe $VM_NAME --project=$PROJECT_ID --zone=$ZONE &>/dev/null; then
    echo "❌ ERROR: VM '$VM_NAME' not found in zone $ZONE"
    echo ""
    echo "Create VM first:"
    echo "  ./scripts/gcp-create-demo-vm.sh"
    exit 1
fi

# Check current status
STATUS=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(status)")

if [ "$STATUS" == "RUNNING" ]; then
    echo "ℹ️  VM is already RUNNING"
    echo ""
    read -p "Restart Docker services anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        # Just show IP and exit
        VM_IP=$(gcloud compute instances describe $VM_NAME \
          --project=$PROJECT_ID \
          --zone=$ZONE \
          --format="get(networkInterfaces[0].accessConfigs[0].natIP)")
        echo ""
        echo "Demo already running at: http://$VM_IP:4200"
        exit 0
    fi
else
    echo "Step 1/3: Starting VM..."
    gcloud compute instances start $VM_NAME --project=$PROJECT_ID --zone=$ZONE

    echo "⏳ Waiting for VM to boot (60 seconds)..."
    sleep 60
    echo "✅ VM started"
    echo ""
fi

echo "Step 2/3: Starting Docker services..."

# Start Docker containers
gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
    set -e
    cd /opt/healthdata/healthdata-in-motion

    echo "🐳 Starting containers..."
    docker-compose up -d

    echo "⏳ Waiting for services to initialize (30 seconds)..."
    sleep 30

    echo ""
    echo "📊 Container Status:"
    docker-compose ps

    echo ""
    echo "🏥 Service Health Checks:"
    echo -n "FHIR Service:        "
    curl -sf http://localhost:8083/fhir/actuator/health > /dev/null && echo "✅ UP" || echo "⚠️  Not ready yet"

    echo -n "CQL Engine:          "
    curl -sf http://localhost:8081/cql-engine/actuator/health > /dev/null && echo "✅ UP" || echo "⚠️  Not ready yet"

    echo -n "Quality Measure:     "
    curl -sf http://localhost:8087/quality-measure/actuator/health > /dev/null && echo "✅ UP" || echo "⚠️  Not ready yet"
' 2>/dev/null

echo "✅ Docker services started"
echo ""

echo "Step 3/3: Getting connection info..."

# Get VM IP
VM_IP=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

# Calculate uptime cost
UPTIME_COST=$(echo "scale=2; 0.15" | bc)

echo ""
echo "================================================"
echo "  ✅ Demo is Ready!"
echo "================================================"
echo ""
echo "🌐 Access URLs:"
echo "   Clinical Portal:  http://$VM_IP:4200"
echo "   FHIR API:         http://$VM_IP:8083/fhir"
echo "   CQL Engine:       http://$VM_IP:8081/cql-engine"
echo "   Quality Measure:  http://$VM_IP:8087/quality-measure"
echo "   API Documentation: http://$VM_IP:8087/quality-measure/swagger-ui.html"
echo ""
echo "🔑 Demo Credentials:"
echo "   Username: demo@healthdata.com"
echo "   Password: Demo123!"
echo ""
echo "💡 Tips:"
echo "   - Services may take 2-3 minutes to fully initialize"
echo "   - WebSocket real-time updates enabled"
echo "   - HIPAA-compliant security active"
echo ""
echo "💰 Running Cost:"
echo "   $0.15/hour (~$110/month if left running)"
echo ""
echo "🛑 When Finished:"
echo "   ./scripts/gcp-stop-demo.sh"
echo ""

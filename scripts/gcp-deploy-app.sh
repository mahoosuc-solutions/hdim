#!/bin/bash
set -e

# HealthData Demo Deployment Script
# Clones/updates repo and starts demo stack on GCP VM

# Configuration
PROJECT_ID=${PROJECT_ID:-"healthcare-data-in-motion"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}
REPO_URL=${REPO_URL:-"https://github.com/mahoosuc-solutions/hdim.git"}
BRANCH=${BRANCH:-"main"}

echo "================================================"
echo "  🚀 Deploying HealthData Demo to GCP"
echo "================================================"
echo ""
echo "VM:       $VM_NAME"
echo "Zone:     $ZONE"
echo "Repo:     $REPO_URL"
echo "Branch:   $BRANCH"
echo ""

# Step 1: Verify VM is running and SSH-accessible
echo "Step 1/5: Verifying VM is running..."

if ! gcloud compute instances describe $VM_NAME --project=$PROJECT_ID --zone=$ZONE &>/dev/null; then
    echo "❌ ERROR: VM '$VM_NAME' not found in zone $ZONE"
    echo "Create VM first: ./scripts/gcp-create-demo-vm.sh"
    exit 1
fi

STATUS=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(status)")

if [ "$STATUS" != "RUNNING" ]; then
    echo "VM is $STATUS — starting it..."
    gcloud compute instances start $VM_NAME --project=$PROJECT_ID --zone=$ZONE
    echo "⏳ Waiting for VM to boot (60 seconds)..."
    sleep 60
fi

# Quick SSH test
if ! gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='echo ok' &>/dev/null; then
    echo "❌ ERROR: Cannot SSH into VM. Wait a moment and retry."
    exit 1
fi

echo "✅ VM is running and SSH-accessible"
echo ""

# Step 2: Clone or update repository
echo "Step 2/5: Syncing repository..."

gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command="
    set -e
    if [ -d /opt/healthdata/hdim ]; then
        echo 'Repository exists — pulling latest...'
        cd /opt/healthdata/hdim
        git fetch origin
        git checkout $BRANCH
        git reset --hard origin/$BRANCH
    else
        echo 'Cloning repository...'
        cd /opt/healthdata
        git clone --branch $BRANCH $REPO_URL hdim
    fi
    echo 'Repository synced.'
" 2>/dev/null

echo "✅ Repository synced"
echo ""

# Step 3: Create Docker network
echo "Step 3/5: Preparing Docker environment..."

gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
    docker network create demo_hdim-demo-network 2>/dev/null || true
' 2>/dev/null

echo "✅ Docker environment ready"
echo ""

# Step 4: Build and start containers
echo "Step 4/5: Starting demo stack (this takes several minutes)..."

gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
    set -e
    cd /opt/healthdata/hdim
    docker compose -f docker-compose.demo.yml up -d --build
' 2>/dev/null

echo "✅ Docker compose started"
echo ""

# Step 5: Poll health checks
echo "Step 5/5: Waiting for services to become healthy..."
echo "   (polling every 30s for up to 15 minutes)"
echo ""

MAX_ATTEMPTS=30
ATTEMPT=0
ALL_HEALTHY=false

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))

    HEALTHY_COUNT=$(gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
        set +e
        count=0
        # Check core services
        curl -sf http://localhost:8085/fhir/actuator/health > /dev/null 2>&1 && count=$((count+1))
        curl -sf http://localhost:8084/patient/actuator/health > /dev/null 2>&1 && count=$((count+1))
        curl -sf http://localhost:8086/care-gap/actuator/health > /dev/null 2>&1 && count=$((count+1))
        curl -sf http://localhost:8081/cql-engine/actuator/health > /dev/null 2>&1 && count=$((count+1))
        curl -sf http://localhost:8087/quality-measure/actuator/health > /dev/null 2>&1 && count=$((count+1))
        echo $count
    ' 2>/dev/null)

    echo "   [$ATTEMPT/$MAX_ATTEMPTS] Healthy services: ${HEALTHY_COUNT:-0}/5"

    if [ "${HEALTHY_COUNT:-0}" -ge 5 ]; then
        ALL_HEALTHY=true
        break
    fi

    sleep 30
done

# Get VM IP
VM_IP=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

echo ""
if [ "$ALL_HEALTHY" = true ]; then
    echo "================================================"
    echo "  ✅ Deployment Complete — All Services Healthy!"
    echo "================================================"
else
    echo "================================================"
    echo "  ⚠️  Deployment Complete — Some Services Still Starting"
    echo "================================================"
    echo ""
    echo "Some services may need more time. Check status with:"
    echo "  ./scripts/gcp-demo-status.sh"
fi

echo ""
echo "🌐 Access URLs:"
echo "   Clinical Portal:  http://$VM_IP:4200"
echo "   Gateway Edge:     http://$VM_IP:18080"
echo "   FHIR API:         http://$VM_IP:8085/fhir"
echo "   Patient Service:  http://$VM_IP:8084/patient"
echo "   Care Gap Service: http://$VM_IP:8086/care-gap"
echo "   CQL Engine:       http://$VM_IP:8081/cql-engine"
echo "   Quality Measure:  http://$VM_IP:8087/quality-measure"
echo "   Jaeger Tracing:   http://$VM_IP:16686"
echo ""
echo "🔑 Demo Credentials:"
echo "   Username: demo_admin@hdim.ai"
echo "   Password: demo123"
echo ""
echo "🛑 When Finished:"
echo "   ./scripts/gcp-stop-demo.sh"
echo ""

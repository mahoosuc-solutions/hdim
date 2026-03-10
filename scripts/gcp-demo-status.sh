#!/bin/bash
set -e

# HealthData Demo Status Script
# Checks VM and service status

# Configuration
PROJECT_ID=${PROJECT_ID:-"healthcare-data-in-motion"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}

echo "================================================"
echo "  HealthData Demo Status"
echo "================================================"
echo ""

# Check if VM exists
if ! gcloud compute instances describe $VM_NAME --project=$PROJECT_ID --zone=$ZONE &>/dev/null; then
    echo "❌ VM '$VM_NAME' not found in zone $ZONE"
    echo ""
    echo "Create VM first:"
    echo "  ./scripts/gcp-create-demo-vm.sh"
    exit 1
fi

# Get VM status
STATUS=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(status)")

MACHINE_TYPE=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(machineType)")

MACHINE_TYPE=$(basename $MACHINE_TYPE)

echo "VM Information:"
echo "  Name:         $VM_NAME"
echo "  Status:       $STATUS"
echo "  Machine Type: $MACHINE_TYPE"
echo "  Zone:         $ZONE"
echo ""

if [ "$STATUS" == "RUNNING" ]; then
    VM_IP=$(gcloud compute instances describe $VM_NAME \
      --project=$PROJECT_ID \
      --zone=$ZONE \
      --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

    echo "  External IP:  $VM_IP"
    echo ""

    echo "Checking Docker services..."
    echo ""

    # Check Docker services
    gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
        set +e

        if [ ! -d "/opt/healthdata/hdim" ]; then
            echo "⚠️  Application not deployed yet"
            echo ""
            echo "Deploy application:"
            echo "  ./scripts/gcp-deploy-app.sh"
            exit 0
        fi

        cd /opt/healthdata/hdim

        echo "Docker Containers:"
        docker compose -f docker-compose.demo.yml ps 2>/dev/null || echo "  Docker Compose not running"
        echo ""

        echo "Service Health:"
        echo -n "  Gateway Edge:        "
        if curl -sf http://localhost:18080/ > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi

        echo -n "  FHIR Service:        "
        if curl -sf http://localhost:8085/fhir/actuator/health > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi

        echo -n "  Patient Service:     "
        if curl -sf http://localhost:8084/patient/actuator/health > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi

        echo -n "  CQL Engine:          "
        if curl -sf http://localhost:8081/cql-engine/actuator/health > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi

        echo -n "  Care Gap Service:    "
        if curl -sf http://localhost:8086/care-gap/actuator/health > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi

        echo -n "  Quality Measure:     "
        if curl -sf http://localhost:8087/quality-measure/actuator/health > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi

        echo -n "  PostgreSQL:          "
        if docker compose -f docker-compose.demo.yml exec -T postgres pg_isready -U healthdata > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi

        echo -n "  Redis:               "
        if docker compose -f docker-compose.demo.yml exec -T redis redis-cli ping > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi

        echo -n "  Jaeger:              "
        if curl -sf http://localhost:16686/ > /dev/null 2>&1; then
            echo "✅ UP"
        else
            echo "❌ DOWN"
        fi
    ' 2>/dev/null

    echo ""
    echo "Access URLs:"
    echo "  Clinical Portal:  http://$VM_IP:4200"
    echo "  Gateway Edge:     http://$VM_IP:18080"
    echo "  FHIR API:         http://$VM_IP:8085/fhir"
    echo "  Patient Service:  http://$VM_IP:8084/patient"
    echo "  Care Gap Service: http://$VM_IP:8086/care-gap"
    echo "  CQL Engine:       http://$VM_IP:8081/cql-engine"
    echo "  Quality Measure:  http://$VM_IP:8087/quality-measure"
    echo "  Jaeger Tracing:   http://$VM_IP:16686"
    echo ""

    echo "💰 Current Cost: \$0.15/hour (~\$110/month)"
    echo ""
    echo "To stop and save costs:"
    echo "  ./scripts/gcp-stop-demo.sh"

elif [ "$STATUS" == "TERMINATED" ]; then
    echo "💾 Data Status:    ✅ Preserved"
    echo "💰 Current Cost:   \$0/hour (only storage: ~\$17/month)"
    echo ""
    echo "To start demo:"
    echo "  ./scripts/gcp-start-demo.sh"

else
    echo "Status: $STATUS"
    echo ""
    echo "Possible actions:"
    echo "  Start: ./scripts/gcp-start-demo.sh"
    echo "  Stop:  ./scripts/gcp-stop-demo.sh"
fi

echo ""

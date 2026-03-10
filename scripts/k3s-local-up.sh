#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
K3D_CONFIG="$PROJECT_ROOT/k8s/demo/local/k3d-cluster.yaml"
HOSTS_FILE="$PROJECT_ROOT/k8s/demo/local/hosts-entries.txt"
NAMESPACE="hdim-demo"

echo "================================================"
echo "  HDIM Demo — K3d Local Setup"
echo "================================================"
echo ""

# Step 1: Check prerequisites
echo "Step 1/5: Checking prerequisites..."

if ! command -v docker &>/dev/null; then
    echo "ERROR: Docker not found. Install Docker first."
    exit 1
fi

if ! docker info &>/dev/null; then
    echo "ERROR: Docker daemon not running."
    exit 1
fi

if ! command -v k3d &>/dev/null; then
    echo "ERROR: k3d not found. Install it first."
    exit 1
fi

if ! command -v kubectl &>/dev/null; then
    echo "ERROR: kubectl not found. Install it first."
    exit 1
fi

echo "  docker:  $(docker version --format '{{.Server.Version}}')"
echo "  k3d:     $(k3d version -s | head -1)"
echo "  kubectl: $(kubectl version --client -o json 2>/dev/null | grep -o '"gitVersion":"[^"]*"' | cut -d'"' -f4)"
echo ""

# Step 2: Create cluster (or reuse existing)
echo "Step 2/5: Creating K3d cluster..."

if k3d cluster list | grep -q hdim-demo; then
    echo "  Cluster 'hdim-demo' already exists."
    read -p "  Delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        k3d cluster delete hdim-demo
    else
        echo "  Reusing existing cluster."
    fi
fi

if ! k3d cluster list | grep -q hdim-demo; then
    k3d cluster create --config "$K3D_CONFIG"
fi

echo "  Cluster ready."
echo ""

# Step 3: Apply manifests
echo "Step 3/5: Applying K8s manifests..."
kubectl apply -k "$PROJECT_ROOT/k8s/demo/"
echo ""

# Step 4: Add /etc/hosts entries
echo "Step 4/5: Configuring /etc/hosts..."

if grep -q "hdim-demo" /etc/hosts 2>/dev/null; then
    echo "  /etc/hosts entries already present."
else
    echo "  Adding subdomain entries (requires sudo)..."
    cat "$HOSTS_FILE" | sudo tee -a /etc/hosts > /dev/null
    echo "  Done."
fi
echo ""

# Step 5: Wait for pods
echo "Step 5/5: Waiting for pods to become ready..."
echo "  (this may take 5-10 minutes on first run)"
echo ""

# Wait for infrastructure first
echo "  Infrastructure:"
kubectl -n "$NAMESPACE" wait --for=condition=ready pod -l tier=infrastructure --timeout=300s 2>/dev/null || true

# Then wait for everything
kubectl -n "$NAMESPACE" wait --for=condition=ready pod --all --timeout=600s 2>/dev/null || true

echo ""
echo "================================================"
echo "  HDIM Demo — Local Environment Ready"
echo "================================================"
echo ""
echo "  Clinical Portal:  http://demo.healthdatainmotion.com"
echo "  API Gateway:      http://api.healthdatainmotion.com"
echo "  FHIR Metadata:    http://fhir.healthdatainmotion.com/metadata"
echo "  Jaeger Tracing:   http://traces.healthdatainmotion.com  (admin / hdim-demo-2026)"
echo "  API Docs:         http://docs.healthdatainmotion.com/swagger-ui/"
echo "  Ops Console:      http://ops.healthdatainmotion.com    (admin / hdim-demo-2026)"
echo ""
echo "  Demo login:       demo_admin@hdim.ai / demo123"
echo ""
echo "  kubectl -n $NAMESPACE get pods"
echo "  kubectl -n $NAMESPACE logs -f deployment/<service>"
echo ""
echo "  To tear down:     ./scripts/k3s-local-down.sh"
echo ""

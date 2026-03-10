#!/bin/bash
set -euo pipefail

# HDIM K3s — Hetzner Provisioning
# Creates CCX33, installs K3s, bootstraps FluxCD

SERVER_NAME="${SERVER_NAME:-hdim-demo}"
SERVER_TYPE="${SERVER_TYPE:-ccx33}"
LOCATION="${LOCATION:-fsn1}"
SSH_KEY="${SSH_KEY:-default}"
GITHUB_OWNER="${GITHUB_OWNER:-mahoosuc-solutions}"
GITHUB_REPO="${GITHUB_REPO:-hdim}"
FLUX_PATH="${FLUX_PATH:-k8s/demo}"

echo "================================================"
echo "  HDIM K3s — Hetzner Setup"
echo "================================================"
echo ""
echo "  Server:   $SERVER_NAME ($SERVER_TYPE)"
echo "  Location: $LOCATION"
echo "  Repo:     $GITHUB_OWNER/$GITHUB_REPO"
echo ""

# Prerequisites
for cmd in hcloud flux kubectl; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "ERROR: $cmd not found. Install it first."
        exit 1
    fi
done

# Step 1: Create server
echo "Step 1/5: Creating Hetzner server..."
SERVER_IP=$(hcloud server create \
    --name "$SERVER_NAME" \
    --type "$SERVER_TYPE" \
    --image ubuntu-22.04 \
    --location "$LOCATION" \
    --ssh-key "$SSH_KEY" \
    --format json 2>/dev/null | grep -o '"ipv4_address":"[^"]*"' | cut -d'"' -f4)

if [ -z "$SERVER_IP" ]; then
    SERVER_IP=$(hcloud server ip "$SERVER_NAME" 2>/dev/null)
fi

echo "  Server IP: $SERVER_IP"
echo ""

# Step 2: Firewall
echo "Step 2/5: Configuring firewall..."
hcloud firewall create --name hdim-demo-fw 2>/dev/null || true
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 80 --source-ips 0.0.0.0/0 2>/dev/null || true
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 443 --source-ips 0.0.0.0/0 2>/dev/null || true
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 6443 --source-ips 0.0.0.0/0 2>/dev/null || true
hcloud firewall apply-to-resource hdim-demo-fw --type server --server "$SERVER_NAME" 2>/dev/null || true
echo "  Firewall configured."
echo ""

# Step 3: Install K3s
echo "Step 3/5: Installing K3s (this takes ~2 minutes)..."
ssh -o StrictHostKeyChecking=no "root@$SERVER_IP" <<'REMOTE'
    curl -sfL https://get.k3s.io | sh -
    # Wait for K3s to be ready
    until kubectl get nodes &>/dev/null; do sleep 2; done
    echo "K3s installed."
REMOTE
echo "  K3s running."
echo ""

# Step 4: Copy kubeconfig
echo "Step 4/5: Fetching kubeconfig..."
scp "root@$SERVER_IP:/etc/rancher/k3s/k3s.yaml" /tmp/k3s-kubeconfig.yaml
sed -i "s|127.0.0.1|$SERVER_IP|g" /tmp/k3s-kubeconfig.yaml
export KUBECONFIG=/tmp/k3s-kubeconfig.yaml
kubectl get nodes
echo ""

# Step 5: Bootstrap FluxCD
echo "Step 5/5: Bootstrapping FluxCD..."
echo "  Requires GITHUB_TOKEN env var with repo access."

if [ -z "${GITHUB_TOKEN:-}" ]; then
    echo "  WARNING: GITHUB_TOKEN not set. Set it and run:"
    echo "    export GITHUB_TOKEN=ghp_..."
    echo "    flux bootstrap github --owner=$GITHUB_OWNER --repository=$GITHUB_REPO --path=$FLUX_PATH --branch=main"
    echo ""
    echo "  Skipping FluxCD bootstrap. K3s is ready for manual kubectl apply."
else
    flux bootstrap github \
        --owner="$GITHUB_OWNER" \
        --repository="$GITHUB_REPO" \
        --path="$FLUX_PATH" \
        --branch=main
    echo "  FluxCD bootstrapped. Cluster will auto-sync from git."
fi

echo ""
echo "================================================"
echo "  Hetzner K3s Setup Complete"
echo "================================================"
echo ""
echo "  Server:     $SERVER_NAME"
echo "  IP:         $SERVER_IP"
echo "  Kubeconfig: /tmp/k3s-kubeconfig.yaml"
echo ""
echo "  DNS: Add A record *.healthdatainmotion.com -> $SERVER_IP"
echo ""
echo "  Validate:   KUBECONFIG=/tmp/k3s-kubeconfig.yaml ./scripts/k3s-validate-demo.sh"
echo ""
echo "  Monthly cost: ~\$35 (CCX33)"
echo ""

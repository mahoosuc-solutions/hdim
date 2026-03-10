#!/bin/bash
set -euo pipefail

echo "================================================"
echo "  HDIM Demo — K3d Teardown"
echo "================================================"
echo ""

# Delete cluster
if k3d cluster list 2>/dev/null | grep -q hdim-demo; then
    echo "Deleting K3d cluster..."
    k3d cluster delete hdim-demo
    echo "  Cluster deleted."
else
    echo "  No hdim-demo cluster found."
fi

# Remove /etc/hosts entries
if grep -q "hdim-demo" /etc/hosts 2>/dev/null; then
    echo "Removing /etc/hosts entries (requires sudo)..."
    sudo sed -i '/# HDIM Demo (k3d local)/d' /etc/hosts
    sudo sed -i '/healthdatainmotion\.com/d' /etc/hosts
    echo "  Entries removed."
else
    echo "  No /etc/hosts entries to remove."
fi

echo ""
echo "  Teardown complete. Docker images are preserved for fast restart."
echo ""

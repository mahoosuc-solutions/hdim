#!/bin/bash
# Docker Auto-Cleanup Script
# Run this weekly or when disk space is low

set -e

echo "=== Docker Auto-Cleanup ==="
echo ""

# Check current usage
echo "Current Docker disk usage:"
docker system df
echo ""

# Remove stopped containers
echo "Removing stopped containers..."
docker container prune -f
echo ""

# Remove dangling images
echo "Removing dangling images..."
docker image prune -f
echo ""

# Remove unused volumes (careful - keeps named volumes in use)
echo "Removing unused volumes..."
docker volume prune -f
echo ""

# Prune build cache (keep 2GB for faster rebuilds)
echo "Pruning build cache (keeping 2GB)..."
docker builder prune -f --keep-storage=2gb 2>/dev/null || docker builder prune -f
echo ""

# Final status
echo "=== Cleanup Complete ==="
echo ""
echo "Final Docker disk usage:"
docker system df
echo ""

# Calculate reclaimable from VHDX compaction
echo "NOTE: Docker files within WSL still consume Windows C: drive space."
echo "If C: drive is still low on space, run the VHDX compaction script from PowerShell."

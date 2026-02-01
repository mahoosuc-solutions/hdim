#!/bin/bash
# Deploy split Gateway services to Docker

set -e

echo "🚀 Deploying Gateway services..."
echo "================================"

# Stop any running local Gateway services
echo ""
echo "🛑 Stopping local Gateway instances..."
pkill -f "gateway-.*-service:bootRun" || echo "No local Gateway instances running"
sleep 2

# Stop existing Docker containers
echo ""
echo "🐳 Stopping existing Docker Gateways..."
docker compose stop gateway-admin-service gateway-fhir-service gateway-clinical-service gateway-edge 2>/dev/null || echo "No Docker Gateways running"
docker compose rm -f gateway-admin-service gateway-fhir-service gateway-clinical-service gateway-edge 2>/dev/null || echo "No containers to remove"

# Start Gateway services with Docker Compose
echo ""
echo "▶️  Starting Gateways in Docker..."
docker compose up -d gateway-admin-service gateway-fhir-service gateway-clinical-service gateway-edge

echo ""
echo "⏳ Waiting for Gateway services to stabilize..."
sleep 15

echo ""
echo "================================"
echo "✅ Deployment complete!"
echo ""
echo "Gateway Edge:"
echo "  URL: http://localhost:18080"
echo ""
echo "Useful commands:"
echo "  Logs: docker compose logs -f gateway-admin-service gateway-fhir-service gateway-clinical-service"
echo "  Stop: docker compose stop gateway-admin-service gateway-fhir-service gateway-clinical-service gateway-edge"
echo "  Restart: docker compose restart gateway-admin-service gateway-fhir-service gateway-clinical-service gateway-edge"
echo "  Status: docker compose ps gateway-admin-service gateway-fhir-service gateway-clinical-service gateway-edge"
echo ""

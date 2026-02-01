#!/bin/bash
# Demo Startup Script for HealthData In Motion Platform
# Starts all services with Gateway authentication

set -e

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"

echo "🚀 Starting HealthData In Motion Demo..."

# Start demo stack
echo "📦 Starting demo stack..."
docker compose -f "$COMPOSE_FILE" up -d

echo "⏳ Waiting for services to stabilize..."
sleep 20

echo ""
echo "✅ All services started!"
echo ""
echo "📝 Service URLs:"
echo "  Gateway Edge:            ${GATEWAY_URL}"
echo "  Clinical Portal:         http://localhost:4200"
echo ""
echo "🔐 Test Login:"
echo "  POST ${GATEWAY_URL}/api/v1/auth/login"
echo '  Body: {"username":"admin","password":"admin123"}'
echo ""
echo "📊 Service Logs:"
echo "  Gateways:                docker compose -f \"$COMPOSE_FILE\" logs -f gateway-admin-service gateway-fhir-service gateway-clinical-service"
echo "  Edge:                    docker compose -f \"$COMPOSE_FILE\" logs -f gateway-edge"
echo "  Clinical Portal:         docker compose -f \"$COMPOSE_FILE\" logs -f clinical-portal"
echo ""
echo "🛑 To stop all services:"
echo "  docker compose -f \"$COMPOSE_FILE\" down"
echo ""

echo "Done."

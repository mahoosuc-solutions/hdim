#!/bin/bash
# Deploy Gateway Service to Docker

set -e

echo "🚀 Deploying Gateway Service..."
echo "================================"

# Stop any running local Gateway
echo ""
echo "🛑 Stopping local Gateway instance..."
pkill -f "gateway-service:bootRun" || echo "No local Gateway running"
sleep 2

# Stop existing Docker container
echo ""
echo "🐳 Stopping existing Docker Gateway..."
docker compose stop gateway-service 2>/dev/null || echo "No Docker Gateway running"
docker compose rm -f gateway-service 2>/dev/null || echo "No container to remove"

# Start Gateway with Docker Compose
echo ""
echo "▶️  Starting Gateway in Docker..."
docker compose up -d gateway-service

# Wait for health check
echo ""
echo "⏳ Waiting for Gateway to be healthy..."
for i in {1..30}; do
  sleep 2
  if curl -s http://localhost:9000/actuator/health | grep -q "UP"; then
    echo ""
    echo "✅ Gateway is UP and healthy!"
    break
  fi
  echo -n "."
  if [ $i -eq 30 ]; then
    echo ""
    echo "⚠️  Gateway didn't become healthy after 60s"
    echo "Check logs: docker compose logs gateway-service"
    exit 1
  fi
done

# Test authentication
echo ""
echo "🔐 Testing authentication..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

if echo "$LOGIN_RESPONSE" | grep -q "accessToken"; then
  echo "✅ Authentication working!"
  echo ""
  echo "Access Token: $(echo $LOGIN_RESPONSE | jq -r .accessToken | head -c 50)..."
else
  echo "⚠️  Authentication test failed"
  echo "Response: $LOGIN_RESPONSE"
fi

echo ""
echo "================================"
echo "✅ Deployment complete!"
echo ""
echo "Gateway Service:"
echo "  URL: http://localhost:9000"
echo "  Health: http://localhost:9000/actuator/health"
echo "  Login: POST http://localhost:9000/api/v1/auth/login"
echo ""
echo "Useful commands:"
echo "  Logs: docker compose logs -f gateway-service"
echo "  Stop: docker compose stop gateway-service"
echo "  Restart: docker compose restart gateway-service"
echo "  Status: docker compose ps gateway-service"
echo ""

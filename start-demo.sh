#!/bin/bash
# Demo Startup Script for HealthData In Motion Platform
# Starts all services with Gateway authentication

set -e

echo "🚀 Starting HealthData In Motion Demo..."

# Start infrastructure services (PostgreSQL, Redis, Kafka)
echo "📦 Starting infrastructure services..."
docker-compose up -d postgres redis kafka zookeeper

# Wait for database
echo "⏳ Waiting for PostgreSQL..."
sleep 5

# Start backend services
echo "🔧 Starting backend microservices..."

# Terminal 1: Gateway Service (Port 9000)
echo "  → Gateway Service (port 9000)"
cd backend
./gradlew :modules:services:gateway-service:bootRun --args='--spring.profiles.active=dev' > ../logs/gateway.log 2>&1 &
GATEWAY_PID=$!

# Wait for Gateway to start
sleep 10

# Terminal 2: CQL Engine Service (Port 8081)
echo "  → CQL Engine Service (port 8081)"
./gradlew :modules:services:cql-engine-service:bootRun --args='--spring.profiles.active=dev' > ../logs/cql-engine.log 2>&1 &
CQL_PID=$!

# Terminal 3: Quality Measure Service (Port 8087)
echo "  → Quality Measure Service (port 8087)"
./gradlew :modules:services:quality-measure-service:bootRun --args='--spring.profiles.active=dev' > ../logs/quality-measure.log 2>&1 &
QUALITY_PID=$!

# Wait for services to start
sleep 15

# Start frontend
echo "🎨 Starting Clinical Portal..."
cd ../apps/clinical-portal
npm start > ../../logs/clinical-portal.log 2>&1 &
PORTAL_PID=$!

echo ""
echo "✅ All services started!"
echo ""
echo "📝 Service URLs:"
echo "  Gateway (Auth):          http://localhost:9000"
echo "  CQL Engine:              http://localhost:8081"
echo "  Quality Measure:         http://localhost:8087"
echo "  Clinical Portal:         http://localhost:4202"
echo ""
echo "🔐 Test Login:"
echo "  POST http://localhost:9000/api/v1/auth/login"
echo '  Body: {"username":"admin","password":"admin123"}'
echo ""
echo "📊 Service Logs:"
echo "  Gateway:                 tail -f logs/gateway.log"
echo "  CQL Engine:              tail -f logs/cql-engine.log"
echo "  Quality Measure:         tail -f logs/quality-measure.log"
echo "  Clinical Portal:         tail -f logs/clinical-portal.log"
echo ""
echo "🛑 To stop all services:"
echo "  kill $GATEWAY_PID $CQL_PID $QUALITY_PID $PORTAL_PID"
echo "  docker-compose down"
echo ""

# Save PIDs
echo "$GATEWAY_PID $CQL_PID $QUALITY_PID $PORTAL_PID" > .demo-pids

echo "Press Ctrl+C to stop all services..."
wait

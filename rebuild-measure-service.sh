#!/bin/bash
set -e

echo "Building JAR..."
cd backend
./gradlew :modules:services:quality-measure-service:build -x test
cd ..

echo "Building Docker Image..."
docker compose -f docker-compose.demo.yml build quality-measure-service

echo "Restarting Service..."
echo "Cleaning up conflicting containers on port 8087..."
docker ps -q --filter expose=8087 | xargs -r docker rm -f

docker compose -f docker-compose.demo.yml up -d --no-deps quality-measure-service
docker compose -f docker-compose.demo.yml ps quality-measure-service

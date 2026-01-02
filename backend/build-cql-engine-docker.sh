#!/bin/bash
set -e

SERVICE_NAME="cql-engine-service"
IMAGE_NAME="healthdata/cql-engine-service"
VERSION="1.0.11"

echo "=========================================="
echo "Building CQL Engine Service Docker Image"
echo "=========================================="

echo "Step 1: Clean and build JAR..."
./gradlew clean :modules:services:$SERVICE_NAME:bootJar

echo "Step 2: Copy JAR to build context..."
cp modules/services/$SERVICE_NAME/build/libs/$SERVICE_NAME.jar ./app.jar

echo "Step 3: Build Docker image..."
docker build -t $IMAGE_NAME:$VERSION --build-arg SERVICE_NAME=$SERVICE_NAME -f Dockerfile .

echo "Step 4: Tag as latest..."
docker tag $IMAGE_NAME:$VERSION $IMAGE_NAME:latest

echo "Step 5: Cleanup..."
rm -f ./app.jar

echo ""
echo "=========================================="
echo "✓ Build Complete!"
echo "=========================================="
echo "Image: $IMAGE_NAME:latest"
echo "Version: $VERSION"
echo ""
echo "Next steps:"
echo "  docker compose up -d cql-engine-service"
echo "  docker logs -f cql-engine-service"

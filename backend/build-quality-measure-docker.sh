#!/bin/bash

# Build script for quality-measure-service Docker image
# This script builds the JAR and creates a Docker image

set -e

SERVICE_NAME="quality-measure-service"
IMAGE_NAME="healthdata/quality-measure-service"
VERSION="1.0.25"

echo "========================================="
echo "Building $SERVICE_NAME Docker Image"
echo "========================================="
echo ""

# Navigate to backend directory
cd "$(dirname "$0")"

echo "Step 1: Clean and build JAR..."
./gradlew clean :modules:services:$SERVICE_NAME:bootJar

echo ""
echo "Step 2: Copy JAR to backend directory..."
cp modules/services/$SERVICE_NAME/build/libs/$SERVICE_NAME.jar ./app.jar

echo ""
echo "Step 3: Build Docker image..."
docker build -t $IMAGE_NAME:$VERSION \
  --build-arg SERVICE_NAME=$SERVICE_NAME \
  -f Dockerfile .

echo ""
echo "Step 4: Tag as latest..."
docker tag $IMAGE_NAME:$VERSION $IMAGE_NAME:latest

echo ""
echo "Step 5: Clean up temporary JAR..."
rm -f ./app.jar

echo ""
echo "========================================="
echo "Build Complete!"
echo "========================================="
echo "Image: $IMAGE_NAME:$VERSION"
echo "Image: $IMAGE_NAME:latest"
echo ""
echo "To run the service:"
echo "  docker compose up -d $SERVICE_NAME"

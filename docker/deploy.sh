#!/bin/bash
#
# HDIM Docker Deployment Script
# Deploy HDIM services to container registry with multi-platform support
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
VERSION=${1:-$(git describe --tags --always --dirty 2>/dev/null || echo "latest")}
REGISTRY=${REGISTRY:-"dockerhub"}
REGISTRY_URL=${REGISTRY_URL:-""}
IMAGE_PREFIX=${IMAGE_PREFIX:-"hdim"}
MULTI_PLATFORM=${MULTI_PLATFORM:-"true"}
SIGN_IMAGE=${SIGN_IMAGE:-"false"}
BUILD_ONLY=${BUILD_ONLY:-"false"}
SKIP_GRADLE_BUILD=${SKIP_GRADLE_BUILD:-"false"}

# Services to deploy (all 22 microservices)
SERVICES=(
    "gateway-service:8080"
    "cql-engine-service:8081"
    "consent-service:8082"
    "event-processing-service:8083"
    "patient-service:8084"
    "fhir-service:8085"
    "care-gap-service:8086"
    "quality-measure-service:8087"
    "agent-runtime-service:8088"
    "data-enrichment-service:8089"
    "ai-assistant-service:8090"
    "documentation-service:8091"
    "analytics-service:8092"
    "predictive-analytics-service:8093"
    "sdoh-service:8094"
    "event-router-service:8095"
    "agent-builder-service:8096"
    "approval-service:8097"
    "payer-workflows-service:8098"
    "cdr-processor-service:8099"
    "ehr-connector-service:8100"
)

SERVICES_OVERRIDE=${SERVICES_OVERRIDE:-""}

if [ -n "$SERVICES_OVERRIDE" ]; then
    IFS=',' read -r -a requested_services <<< "$SERVICES_OVERRIDE"
    filtered_services=()

    for requested in "${requested_services[@]}"; do
        match_found=false
        for service_info in "${SERVICES[@]}"; do
            service_name=$(echo "$service_info" | cut -d: -f1)
            if [ "$service_name" = "$requested" ]; then
                filtered_services+=("$service_info")
                match_found=true
                break
            fi
        done

        if [ "$match_found" = false ]; then
            echo -e "${RED}❌ Unknown service in SERVICES_OVERRIDE: ${requested}${NC}"
            exit 1
        fi
    done

    SERVICES=("${filtered_services[@]}")
fi

# Architectures to build for
PLATFORMS="linux/amd64,linux/arm64"

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   HDIM Docker Deployment Script      ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Configuration:${NC}"
echo -e "  Version:      ${YELLOW}$VERSION${NC}"
echo -e "  Registry:     ${YELLOW}$REGISTRY${NC}"
echo -e "  Multi-arch:   ${YELLOW}$MULTI_PLATFORM${NC}"
echo -e "  Sign images:  ${YELLOW}$SIGN_IMAGE${NC}"
echo -e "  Services:     ${YELLOW}${#SERVICES[@]}${NC}"
echo ""

# Function to get registry URL
get_registry_url() {
    case "$REGISTRY" in
        "dockerhub")
            echo ""
            ;;
        "gcr")
            echo "gcr.io/${GCP_PROJECT_ID}"
            ;;
        "gar")
            echo "${GAR_LOCATION}-docker.pkg.dev/${GCP_PROJECT_ID}/${GAR_REPO}"
            ;;
        "ecr")
            echo "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
            ;;
        "acr")
            echo "${AZURE_REGISTRY_NAME}.azurecr.io"
            ;;
        "ghcr")
            echo "ghcr.io/${GITHUB_OWNER}"
            ;;
        "gitlab")
            echo "registry.gitlab.com/${GITLAB_GROUP}/${GITLAB_PROJECT}"
            ;;
        *)
            echo "$REGISTRY_URL"
            ;;
    esac
}

# Function to authenticate with registry
authenticate_registry() {
    echo -e "${BLUE}🔐 Authenticating with registry...${NC}"

    case "$REGISTRY" in
        "dockerhub")
            if [ -n "$DOCKER_PASSWORD" ]; then
                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
            else
                echo -e "${YELLOW}⚠️  DOCKER_PASSWORD not set, assuming already logged in${NC}"
            fi
            ;;
        "gcr"|"gar")
            gcloud auth configure-docker $([ "$REGISTRY" = "gar" ] && echo "$GAR_LOCATION-docker.pkg.dev" || echo "gcr.io")
            ;;
        "ecr")
            aws ecr get-login-password --region "$AWS_REGION" | \
                docker login --username AWS --password-stdin "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
            ;;
        "acr")
            az acr login --name "$AZURE_REGISTRY_NAME"
            ;;
        "ghcr")
            echo "$GITHUB_TOKEN" | docker login ghcr.io -u "$GITHUB_OWNER" --password-stdin
            ;;
        "gitlab")
            echo "$GITLAB_TOKEN" | docker login registry.gitlab.com -u "$GITLAB_USERNAME" --password-stdin
            ;;
    esac

    echo -e "${GREEN}✅ Registry authentication successful${NC}"
}

# Function to setup buildx for multi-platform
setup_buildx() {
    if [ "$MULTI_PLATFORM" = "true" ]; then
        echo -e "${BLUE}🏗️  Setting up Docker Buildx for multi-platform builds...${NC}"

        # Create builder if it doesn't exist
        if ! docker buildx inspect hdim-multiplatform > /dev/null 2>&1; then
            docker buildx create --name hdim-multiplatform --use
        else
            docker buildx use hdim-multiplatform
        fi

        # Bootstrap builder
        docker buildx inspect --bootstrap

        echo -e "${GREEN}✅ Buildx configured for: $PLATFORMS${NC}"
    fi
}

# Function to build and push a service
build_and_push_service() {
    local service_info=$1
    local service_name=$(echo "$service_info" | cut -d: -f1)
    local service_port=$(echo "$service_info" | cut -d: -f2)

    local registry_url=$(get_registry_url)
    local full_image_name="${registry_url:+$registry_url/}${IMAGE_PREFIX}/${service_name}"
    local dockerfile_path="backend/modules/services/${service_name}/Dockerfile"

    echo ""
    echo -e "${BLUE}═══════════════════════════════════════${NC}"
    echo -e "${BLUE}Building: ${YELLOW}$service_name${NC}"
    echo -e "${BLUE}═══════════════════════════════════════${NC}"

    # Check if Dockerfile exists
    if [ ! -f "$dockerfile_path" ]; then
        echo -e "${RED}❌ Dockerfile not found: $dockerfile_path${NC}"
        return 1
    fi

    # Build arguments
    local build_args=""
    if [ "$MULTI_PLATFORM" = "true" ]; then
        build_args="--platform $PLATFORMS"
    fi

    # Tags
    local tags=(
        "${full_image_name}:${VERSION}"
        "${full_image_name}:latest"
    )

    # Add semantic version tags if VERSION matches semver pattern
    if [[ $VERSION =~ ^v?([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
        local major="${BASH_REMATCH[1]}"
        local minor="${BASH_REMATCH[2]}"
        tags+=("${full_image_name}:v${major}")
        tags+=("${full_image_name}:v${major}.${minor}")
    fi

    # Build tag arguments
    local tag_args=""
    for tag in "${tags[@]}"; do
        tag_args="$tag_args --tag $tag"
    done

    # Build and push command
    if [ "$MULTI_PLATFORM" = "true" ]; then
        # Multi-platform build with buildx
        docker buildx build \
            $build_args \
            $tag_args \
            --file "$dockerfile_path" \
            --cache-from type=registry,ref=${full_image_name}:buildcache \
            --cache-to type=registry,ref=${full_image_name}:buildcache,mode=max \
            --provenance=true \
            --sbom=true \
            $([ "$BUILD_ONLY" != "true" ] && echo "--push" || echo "--load") \
            backend/
    else
        # Single platform build
        docker build \
            $tag_args \
            --file "$dockerfile_path" \
            backend/

        if [ "$BUILD_ONLY" != "true" ]; then
            for tag in "${tags[@]}"; do
                docker push "$tag"
            done
        fi
    fi

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Successfully built and pushed: $service_name${NC}"

        # Sign image if requested
        if [ "$SIGN_IMAGE" = "true" ] && [ "$BUILD_ONLY" != "true" ]; then
            sign_image "${full_image_name}:${VERSION}"
        fi
    else
        echo -e "${RED}❌ Failed to build: $service_name${NC}"
        return 1
    fi
}

# Function to sign image with cosign
sign_image() {
    local image=$1

    if command -v cosign &> /dev/null; then
        echo -e "${BLUE}🔏 Signing image: $image${NC}"

        # Sign with keyless (OIDC) if available, otherwise skip
        if [ -n "$COSIGN_KEY" ]; then
            cosign sign --key "$COSIGN_KEY" "$image"
        else
            echo -e "${YELLOW}⚠️  COSIGN_KEY not set, using keyless signing${NC}"
            cosign sign --yes "$image" 2>/dev/null || echo -e "${YELLOW}⚠️  Keyless signing not available, skipping${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  cosign not installed, skipping image signing${NC}"
    fi
}

# Function to verify build prerequisites
check_prerequisites() {
    echo -e "${BLUE}🔍 Checking prerequisites...${NC}"

    local missing=()

    # Check Docker
    if ! command -v docker &> /dev/null; then
        missing+=("docker")
    fi

    # Check buildx for multi-platform
    if [ "$MULTI_PLATFORM" = "true" ]; then
        if ! docker buildx version &> /dev/null; then
            missing+=("docker buildx")
        fi
    fi

    # Check registry-specific tools
    case "$REGISTRY" in
        "gcr"|"gar")
            if ! command -v gcloud &> /dev/null; then
                missing+=("gcloud")
            fi
            ;;
        "ecr")
            if ! command -v aws &> /dev/null; then
                missing+=("aws-cli")
            fi
            ;;
        "acr")
            if ! command -v az &> /dev/null; then
                missing+=("azure-cli")
            fi
            ;;
    esac

    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${RED}❌ Missing prerequisites: ${missing[*]}${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ All prerequisites met${NC}"
}

# Function to generate deployment report
generate_report() {
    local start_time=$1
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║     Deployment Complete!              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}Summary:${NC}"
    echo -e "  Services:     ${GREEN}${#SERVICES[@]}${NC}"
    echo -e "  Version:      ${GREEN}$VERSION${NC}"
    echo -e "  Duration:     ${GREEN}${duration}s${NC}"
    echo -e "  Registry:     ${GREEN}$(get_registry_url || echo $REGISTRY)${NC}"
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo -e "  1. Verify images:"
    echo -e "     ${YELLOW}docker images | grep $IMAGE_PREFIX${NC}"
    echo ""
    echo -e "  2. Deploy to Kubernetes:"
    echo -e "     ${YELLOW}kubectl apply -f k8s/${NC}"
    echo ""
    echo -e "  3. Monitor deployment:"
    echo -e "     ${YELLOW}kubectl get pods -n hdim${NC}"
    echo ""
}

# Ensure backend artifacts exist before building images
prepare_backend_artifacts() {
    if [ "$SKIP_GRADLE_BUILD" = "true" ]; then
        echo -e "${YELLOW}⚠️  SKIP_GRADLE_BUILD=true, skipping Gradle build${NC}"
        return 0
    fi

    local missing=()
    for service_info in "${SERVICES[@]}"; do
        local service_name=$(echo "$service_info" | cut -d: -f1)
        if ! ls "backend/modules/services/${service_name}/build/libs/"*.jar >/dev/null 2>&1; then
            missing+=("$service_name")
        fi
    done

    if [ ${#missing[@]} -gt 0 ]; then
        echo -e "${BLUE}🔧 Building backend artifacts with Gradle...${NC}"
        echo -e "${YELLOW}Missing artifacts for: ${missing[*]}${NC}"
        (cd backend && ./gradlew build -x test --parallel)
    else
        echo -e "${GREEN}✅ Backend artifacts already present${NC}"
    fi
}

# Main execution
main() {
    local start_time=$(date +%s)

    # Check prerequisites
    check_prerequisites

    # Build backend artifacts if needed
    prepare_backend_artifacts

    # Authenticate with registry
    if [ "$BUILD_ONLY" != "true" ]; then
        authenticate_registry
    fi

    # Setup buildx for multi-platform
    setup_buildx

    # Build and push each service
    local failed_services=()
    for service_info in "${SERVICES[@]}"; do
        if ! build_and_push_service "$service_info"; then
            failed_services+=("$service_info")
        fi
    done

    # Report results
    if [ ${#failed_services[@]} -gt 0 ]; then
        echo ""
        echo -e "${RED}❌ Failed to build ${#failed_services[@]} service(s):${NC}"
        for service in "${failed_services[@]}"; do
            echo -e "  ${RED}✗${NC} $service"
        done
        exit 1
    fi

    generate_report "$start_time"
}

# Show usage
usage() {
    echo "Usage: $0 [VERSION]"
    echo ""
    echo "Environment Variables:"
    echo "  REGISTRY           - Registry type (dockerhub, gcr, gar, ecr, acr, ghcr, gitlab)"
    echo "  REGISTRY_URL       - Custom registry URL"
    echo "  IMAGE_PREFIX       - Image name prefix (default: hdim)"
    echo "  MULTI_PLATFORM     - Build for multiple platforms (default: true)"
    echo "  SIGN_IMAGE         - Sign images with cosign (default: false)"
    echo "  BUILD_ONLY         - Only build, don't push (default: false)"
    echo "  SKIP_GRADLE_BUILD  - Skip Gradle build step (default: false)"
    echo "  SERVICES_OVERRIDE  - Comma-separated service list to build (default: all)"
    echo ""
    echo "Registry-specific variables:"
    echo "  Docker Hub:    DOCKER_USERNAME, DOCKER_PASSWORD"
    echo "  GCR/GAR:       GCP_PROJECT_ID, GAR_LOCATION, GAR_REPO"
    echo "  ECR:           AWS_ACCOUNT_ID, AWS_REGION"
    echo "  ACR:           AZURE_REGISTRY_NAME"
    echo "  GHCR:          GITHUB_OWNER, GITHUB_TOKEN"
    echo "  GitLab:        GITLAB_GROUP, GITLAB_PROJECT, GITLAB_TOKEN"
    echo ""
    echo "Examples:"
    echo "  $0 v1.0.0                    # Deploy version 1.0.0"
    echo "  REGISTRY=gcr $0 v1.0.0       # Deploy to Google Container Registry"
    echo "  BUILD_ONLY=true $0 latest    # Build only, don't push"
    exit 1
}

# Handle arguments
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    usage
fi

# Run main
main

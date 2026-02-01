# OCR Docker Deployment Plan - Local Testing

**Purpose:** Step-by-step guide for deploying the OCR-enabled Documentation Service to Docker for local testing.

**Date:** January 24, 2026
**Status:** Ready for Deployment
**Related:** Issue #245 (OCR Document Processing), Option 3 Completion

---

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Deployment Strategy](#deployment-strategy)
3. [Step-by-Step Deployment](#step-by-step-deployment)
4. [Verification & Testing](#verification--testing)
5. [Troubleshooting](#troubleshooting)
6. [Rollback Plan](#rollback-plan)

---

## Pre-Deployment Checklist

### System Requirements

**✅ Prerequisites:**
- [ ] Docker Desktop running (version 24.0+)
- [ ] Java 21 installed and configured
- [ ] Gradle 8.11+ available
- [ ] 8GB+ RAM available for Docker
- [ ] 10GB+ free disk space
- [ ] PostgreSQL container running (port 5435)

**✅ Code Status:**
- [x] OCR integration tests passing locally
- [x] All files committed to git
- [x] Dockerfile updated with Tesseract OCR
- [x] Application.yml configured for OCR
- [x] Database migrations up to date

**✅ Dependencies:**
```bash
# Verify Docker
docker --version
# Should output: Docker version 24.x.x

# Verify PostgreSQL is running
docker ps | grep postgres
# Should show: hdim-postgres container

# Verify Java version
java -version
# Should output: openjdk version "21"

# Verify Gradle
./gradlew --version
# Should output: Gradle 8.11 or later
```

---

## Deployment Strategy

### Architecture Overview

```
┌─────────────────────────────────────────────┐
│         Docker Compose Network              │
│                                             │
│  ┌──────────────┐    ┌──────────────┐      │
│  │ PostgreSQL   │◄───│ Documentation│      │
│  │ Port: 5435   │    │ Service      │      │
│  │              │    │ Port: 8091   │      │
│  │ + GIN Index  │    │              │      │
│  │ + ts_vector  │    │ + Tesseract  │      │
│  └──────────────┘    │ + PDFBox     │      │
│                      │ + File Store │      │
│                      └──────────────┘      │
│                                             │
│  ┌──────────────┐                          │
│  │ Redis        │                          │
│  │ Port: 6380   │                          │
│  └──────────────┘                          │
│                                             │
└─────────────────────────────────────────────┘

Volume: document-storage (persistent)
```

### Deployment Phases

**Phase 1: Pre-cache Dependencies** (5-10 minutes)
- Resolve and download Gradle dependencies locally
- Uses `./gradlew dependencies` task (not `downloadDependencies`)
- Prevents Docker TLS timeout issues
- Reduces build time

**Phase 2: Build Service JAR** (2-3 minutes)
- Build documentation-service bootJar
- Run without tests (already validated)
- Verify JAR creation

**Phase 3: Build Docker Image** (3-5 minutes)
- Build multi-stage Dockerfile
- Install Tesseract OCR
- Verify language data
- Create storage directories

**Phase 4: Start Service** (1-2 minutes)
- Start documentation-service container
- Wait for startup completion
- Verify health endpoint

**Phase 5: Smoke Testing** (5-10 minutes)
- Upload test PDF document
- Verify OCR processing
- Test full-text search
- Check multi-tenant isolation

**Total Estimated Time:** 16-30 minutes (first deployment)
**Subsequent Deployments:** 5-10 minutes (with cached dependencies)

---

## Step-by-Step Deployment

### Phase 1: Pre-cache Dependencies

**Why:** Prevent Docker TLS timeout issues during build.

```bash
# Navigate to backend directory
cd /mnt/wdblack/dev/projects/hdim-master/backend

# Download all dependencies to local Gradle cache
./gradlew downloadDependencies --no-daemon

# Verify dependency download
ls ~/.gradle/caches/modules-2/files-2.1/ | head -20

# Expected output: List of dependency packages
```

**Success Criteria:**
- ✅ Command completes without errors
- ✅ Dependencies cached in `~/.gradle/caches/`
- ✅ No TLS or network errors

**Troubleshooting:**
```bash
# If downloadDependencies fails, try individual module
./gradlew :modules:services:documentation-service:dependencies --no-daemon

# Clear corrupted cache if needed
rm -rf ~/.gradle/caches/modules-2/files-2.1/*
./gradlew downloadDependencies --no-daemon --refresh-dependencies
```

---

### Phase 2: Build Service JAR

**Why:** Create the application JAR that will be packaged in Docker.

```bash
# Build documentation-service bootJar (skip tests - already validated)
./gradlew :modules:services:documentation-service:bootJar -x test --no-daemon

# Expected duration: 2-3 minutes

# Verify JAR was created
ls -lh modules/services/documentation-service/build/libs/

# Expected output:
# documentation-service-1.0.0-SNAPSHOT.jar (approximately 80-100MB)
```

**Success Criteria:**
- ✅ Build completes with "BUILD SUCCESSFUL"
- ✅ JAR file exists in `build/libs/`
- ✅ JAR size is 80-100MB (includes dependencies)

**Troubleshooting:**
```bash
# If build fails, check for compilation errors
./gradlew :modules:services:documentation-service:compileJava --info

# Clean and rebuild if needed
./gradlew :modules:services:documentation-service:clean bootJar -x test
```

---

### Phase 3: Build Docker Image

**Why:** Package the service with Tesseract OCR in a container.

```bash
# Navigate to project root
cd /mnt/wdblack/dev/projects/hdim-master

# Build Docker image for documentation-service
docker compose build documentation-service

# Expected duration: 3-5 minutes (first build)
# Subsequent builds: 1-2 minutes (cached layers)

# Verify image was created
docker images | grep documentation-service

# Expected output:
# hdim-master-documentation-service   latest   <image-id>   <timestamp>   500-600MB
```

**Build Process:**
1. **Stage 1: Builder** (gradle:8.5-jdk21)
   - Copy Gradle files
   - Copy shared modules
   - Copy service source
   - Build bootJar

2. **Stage 2: Runtime** (eclipse-temurin:21-jre-alpine)
   - Install Tesseract OCR
   - Install English language data
   - Create healthdata user
   - Copy JAR from builder
   - Create document storage directories
   - Set environment variables

**Verify Tesseract Installation:**
```bash
# Check Tesseract version in image
docker run --rm hdim-master-documentation-service tesseract --version

# Expected output:
# tesseract 5.x.x
#  leptonica-1.x.x
#  libjpeg 9e : libpng 1.6.x : libtiff 4.x.x

# Verify language data
docker run --rm hdim-master-documentation-service ls -la /usr/share/tessdata/

# Expected output:
# -rw-r--r--   1 root  root  4.x MB  eng.traineddata
```

**Success Criteria:**
- ✅ Image builds without errors
- ✅ Image size is 500-600MB
- ✅ Tesseract version 5.x.x installed
- ✅ eng.traineddata exists in /usr/share/tessdata/

**Troubleshooting:**
```bash
# Build with verbose output
docker compose build --progress=plain documentation-service

# Build without cache (if layers are corrupted)
docker compose build --no-cache documentation-service

# Check Dockerfile syntax
docker build --check backend/modules/services/documentation-service/
```

---

### Phase 4: Start Service

**Why:** Launch the documentation-service container with all dependencies.

```bash
# Ensure PostgreSQL is running
docker compose up -d postgres

# Wait for PostgreSQL to be ready
docker compose logs postgres | grep "database system is ready to accept connections"

# Start documentation-service
docker compose up -d documentation-service

# Expected duration: 1-2 minutes for startup

# Monitor startup logs
docker compose logs -f documentation-service

# Look for these indicators:
# ✅ "Started DocumentationServiceApplication in X.xxx seconds"
# ✅ "Tomcat started on port 8091"
# ✅ "Liquibase: Successfully acquired change log lock"
# ✅ "OCR Configuration initialized: enabled=true"
```

**Startup Checklist:**
```bash
# 1. Check container is running
docker compose ps documentation-service

# Expected output:
# NAME                    STATUS          PORTS
# documentation-service   Up 30 seconds   0.0.0.0:8091->8091/tcp

# 2. Check health endpoint
curl -s http://localhost:8091/actuator/health | jq '.'

# Expected output:
# {
#   "status": "UP",
#   "components": {
#     "db": { "status": "UP" },
#     "diskSpace": { "status": "UP" },
#     "ping": { "status": "UP" }
#   }
# }

# 3. Verify OCR configuration
docker compose exec documentation-service env | grep OCR

# Expected output:
# OCR_ENABLED=true
# OCR_ASYNC=true
# TESSERACT_LANGUAGE=eng
# TESSERACT_OEM=1
# TESSERACT_PSM=3
# TESSERACT_DPI=300

# 4. Check Tesseract accessibility
docker compose exec documentation-service tesseract --version

# Expected output:
# tesseract 5.x.x
```

**Success Criteria:**
- ✅ Container status: "Up" (not "Restarting")
- ✅ Health endpoint returns `{"status":"UP"}`
- ✅ Startup logs show "Started DocumentationServiceApplication"
- ✅ No errors in logs (check last 100 lines)
- ✅ Tesseract accessible from within container

**Troubleshooting:**
```bash
# If container exits immediately, check logs
docker compose logs --tail=100 documentation-service

# Common issues:
# - Database connection failure → Check PostgreSQL is running
# - Port conflict (8091) → Check if another service uses port 8091
# - Tesseract not found → Rebuild image with --no-cache
# - Permission denied on /var/lib/healthdata → Check Dockerfile USER directive

# Restart service with fresh logs
docker compose restart documentation-service
docker compose logs -f documentation-service

# Execute shell in container for debugging
docker compose exec documentation-service sh
```

---

### Phase 5: Smoke Testing

**Why:** Verify OCR functionality works end-to-end in Docker.

#### Test 1: Create Clinical Document

```bash
# Create a clinical document to attach files to
curl -X POST http://localhost:8091/api/documents/clinical \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -d '{
    "patientId": "patient-123",
    "documentType": "LAB_RESULT",
    "title": "OCR Test Document",
    "status": "FINAL"
  }' | jq '.'

# Save the document ID from response
# Example: DOCUMENT_ID="550e8400-e29b-41d4-a716-446655440000"
```

**Expected Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "patientId": "patient-123",
  "documentType": "LAB_RESULT",
  "title": "OCR Test Document",
  "status": "FINAL",
  "createdAt": "2026-01-24T10:30:00Z"
}
```

#### Test 2: Upload PDF with OCR

**Create a test PDF:**
```bash
# Create a simple test PDF with text (requires pandoc or similar)
echo "Lab Result: Hemoglobin A1c 7.2%" > test-lab-result.txt
pandoc test-lab-result.txt -o test-lab-result.pdf

# Alternatively, use a pre-existing PDF document
```

**Upload the PDF:**
```bash
# Upload PDF file with OCR processing
curl -X POST http://localhost:8091/api/documents/clinical/$DOCUMENT_ID/upload \
  -H "X-Tenant-ID: test-tenant" \
  -F "file=@test-lab-result.pdf" \
  -F "title=Lab Result PDF" | jq '.'

# Save the attachment ID from response
# Example: ATTACHMENT_ID="660e8400-e29b-41d4-a716-446655440001"
```

**Expected Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "clinicalDocumentId": "550e8400-e29b-41d4-a716-446655440000",
  "fileName": "test-lab-result.pdf",
  "contentType": "application/pdf",
  "fileSize": 12345,
  "ocrStatus": "PENDING",
  "ocrText": null,
  "uploadedAt": "2026-01-24T10:35:00Z"
}
```

#### Test 3: Monitor OCR Processing

```bash
# Poll OCR status (every 2 seconds for up to 30 seconds)
for i in {1..15}; do
  echo "Poll attempt $i:"
  curl -s http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID/ocr-status \
    -H "X-Tenant-ID: test-tenant" | jq '.'

  # Check if completed
  STATUS=$(curl -s http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID/ocr-status \
    -H "X-Tenant-ID: test-tenant" | jq -r '.status')

  if [ "$STATUS" = "COMPLETED" ]; then
    echo "OCR processing completed!"
    break
  fi

  sleep 2
done
```

**Expected Status Progression:**
1. `PENDING` → OCR queued for processing
2. `PROCESSING` → Tesseract is extracting text (may be brief)
3. `COMPLETED` → Text extraction successful

**Final Status Response:**
```json
{
  "status": "COMPLETED",
  "processedAt": "2026-01-24T10:35:15Z",
  "errorMessage": null,
  "hasText": true
}
```

#### Test 4: Verify OCR Text Extraction

```bash
# Get attachment details to see extracted text
curl -s http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID \
  -H "X-Tenant-ID: test-tenant" | jq '.ocrText'

# Expected output (should contain text from PDF):
# "Lab Result: Hemoglobin A1c 7.2%"
```

#### Test 5: Test Full-Text Search

```bash
# Search for "hemoglobin" in OCR'd documents
curl -s "http://localhost:8091/api/documents/clinical/search-ocr?query=hemoglobin&page=0&size=20" \
  -H "X-Tenant-ID: test-tenant" | jq '.'

# Expected response:
# {
#   "content": [
#     {
#       "id": "660e8400-e29b-41d4-a716-446655440001",
#       "fileName": "test-lab-result.pdf",
#       "ocrText": "Lab Result: Hemoglobin A1c 7.2%",
#       "ocrStatus": "COMPLETED",
#       ...
#     }
#   ],
#   "totalElements": 1,
#   "totalPages": 1,
#   "size": 20,
#   "number": 0
# }
```

#### Test 6: Multi-Tenant Isolation

```bash
# Try to access document with different tenant ID (should fail)
curl -s http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID \
  -H "X-Tenant-ID: different-tenant" | jq '.'

# Expected response: HTTP 404 Not Found
# {
#   "timestamp": "2026-01-24T10:40:00Z",
#   "status": 404,
#   "error": "Not Found",
#   "message": "Attachment not found",
#   "path": "/api/documents/clinical/attachments/..."
# }
```

---

## Verification & Testing

### Quick Verification Script

**File:** `scripts/verify-ocr-deployment.sh`

```bash
#!/bin/bash
set -e

echo "=== OCR Deployment Verification Script ==="
echo ""

# Configuration
BASE_URL="http://localhost:8091"
TENANT_ID="test-tenant"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Health Check
echo -n "Test 1: Health Check... "
HEALTH=$(curl -s "$BASE_URL/actuator/health" | jq -r '.status')
if [ "$HEALTH" = "UP" ]; then
    echo -e "${GREEN}PASS${NC}"
else
    echo -e "${RED}FAIL${NC} (Status: $HEALTH)"
    exit 1
fi

# Test 2: Create Clinical Document
echo -n "Test 2: Create Clinical Document... "
RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/clinical" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -d '{
    "patientId": "patient-test",
    "documentType": "LAB_RESULT",
    "title": "Verification Test",
    "status": "FINAL"
  }')

DOCUMENT_ID=$(echo "$RESPONSE" | jq -r '.id')
if [ -n "$DOCUMENT_ID" ] && [ "$DOCUMENT_ID" != "null" ]; then
    echo -e "${GREEN}PASS${NC} (ID: $DOCUMENT_ID)"
else
    echo -e "${RED}FAIL${NC}"
    echo "Response: $RESPONSE"
    exit 1
fi

# Test 3: Upload Test File
echo -n "Test 3: Upload Test PDF... "
# Create simple test PDF (requires pandoc)
echo "Test Content: Diabetes Type 2 Diagnosis" > /tmp/test-ocr.txt
if command -v pandoc &> /dev/null; then
    pandoc /tmp/test-ocr.txt -o /tmp/test-ocr.pdf

    UPLOAD_RESPONSE=$(curl -s -X POST "$BASE_URL/api/documents/clinical/$DOCUMENT_ID/upload" \
      -H "X-Tenant-ID: $TENANT_ID" \
      -F "file=@/tmp/test-ocr.pdf" \
      -F "title=Test PDF")

    ATTACHMENT_ID=$(echo "$UPLOAD_RESPONSE" | jq -r '.id')
    if [ -n "$ATTACHMENT_ID" ] && [ "$ATTACHMENT_ID" != "null" ]; then
        echo -e "${GREEN}PASS${NC} (ID: $ATTACHMENT_ID)"
    else
        echo -e "${RED}FAIL${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}SKIP${NC} (pandoc not installed)"
    ATTACHMENT_ID=""
fi

# Test 4: Monitor OCR Processing
if [ -n "$ATTACHMENT_ID" ]; then
    echo -n "Test 4: OCR Processing... "
    for i in {1..15}; do
        STATUS=$(curl -s "$BASE_URL/api/documents/clinical/attachments/$ATTACHMENT_ID/ocr-status" \
          -H "X-Tenant-ID: $TENANT_ID" | jq -r '.status')

        if [ "$STATUS" = "COMPLETED" ]; then
            echo -e "${GREEN}PASS${NC} (Completed in ${i}x2 seconds)"
            break
        elif [ "$STATUS" = "FAILED" ]; then
            echo -e "${RED}FAIL${NC} (OCR processing failed)"
            exit 1
        fi

        sleep 2
    done

    if [ "$STATUS" != "COMPLETED" ]; then
        echo -e "${YELLOW}TIMEOUT${NC} (Still processing after 30s)"
    fi
fi

# Test 5: Full-Text Search
if [ -n "$ATTACHMENT_ID" ] && [ "$STATUS" = "COMPLETED" ]; then
    echo -n "Test 5: Full-Text Search... "
    SEARCH_RESULTS=$(curl -s "$BASE_URL/api/documents/clinical/search-ocr?query=diabetes&page=0&size=20" \
      -H "X-Tenant-ID: $TENANT_ID" | jq -r '.totalElements')

    if [ "$SEARCH_RESULTS" -ge 1 ]; then
        echo -e "${GREEN}PASS${NC} (Found $SEARCH_RESULTS results)"
    else
        echo -e "${RED}FAIL${NC} (No results found)"
    fi
fi

# Test 6: Multi-Tenant Isolation
if [ -n "$ATTACHMENT_ID" ]; then
    echo -n "Test 6: Multi-Tenant Isolation... "
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
      "$BASE_URL/api/documents/clinical/attachments/$ATTACHMENT_ID" \
      -H "X-Tenant-ID: different-tenant")

    if [ "$HTTP_CODE" = "404" ]; then
        echo -e "${GREEN}PASS${NC} (Access denied for different tenant)"
    else
        echo -e "${RED}FAIL${NC} (HTTP $HTTP_CODE - tenant isolation broken)"
    fi
fi

echo ""
echo "=== Verification Complete ==="
```

**Make executable and run:**
```bash
chmod +x scripts/verify-ocr-deployment.sh
./scripts/verify-ocr-deployment.sh
```

---

## Troubleshooting

### Issue 1: Container Exits Immediately

**Symptoms:**
```bash
docker compose ps documentation-service
# Status: Exited (1)
```

**Diagnosis:**
```bash
docker compose logs documentation-service | tail -50
```

**Common Causes:**

1. **Database Connection Failure**
   ```
   Error: Could not connect to database
   ```
   **Fix:**
   ```bash
   # Ensure PostgreSQL is running
   docker compose up -d postgres

   # Check PostgreSQL logs
   docker compose logs postgres
   ```

2. **Missing Environment Variables**
   ```
   Error: Required property 'TESSDATA_PREFIX' not found
   ```
   **Fix:** Check `docker-compose.yml` has proper environment section

3. **Port Conflict**
   ```
   Error: Address already in use: bind
   ```
   **Fix:**
   ```bash
   # Find process using port 8091
   lsof -ti:8091 | xargs kill -9

   # Or change port in docker-compose.yml
   ports:
     - "8092:8091"  # Use different external port
   ```

---

### Issue 2: Tesseract Not Found

**Symptoms:**
```
TesseractException: Error opening data file
```

**Diagnosis:**
```bash
# Check if Tesseract is installed in container
docker compose exec documentation-service which tesseract

# Check language data
docker compose exec documentation-service ls -la /usr/share/tessdata/
```

**Fix:**
```bash
# Rebuild Docker image with --no-cache
docker compose build --no-cache documentation-service

# Verify installation during build
docker compose build --progress=plain documentation-service | grep tesseract
```

---

### Issue 3: OCR Processing Stuck in PENDING

**Symptoms:**
```json
{
  "status": "PENDING",
  "processedAt": null
}
```

**Diagnosis:**
```bash
# Check service logs for OCR processing
docker compose logs documentation-service | grep OCR

# Check for async executor configuration
docker compose exec documentation-service env | grep ASYNC
```

**Possible Causes:**

1. **Async processing disabled in test mode**
   - Check `application.yml`: `ocr.async: false`
   - Wait longer (synchronous processing may take 5-10s)

2. **Thread pool exhausted**
   - Check logs for "RejectedExecutionException"
   - Restart service to reset thread pool

3. **File storage permission issue**
   - Check logs for "Permission denied"
   - Verify `/var/lib/healthdata/documents` permissions

**Fix:**
```bash
# Restart service
docker compose restart documentation-service

# Trigger reprocessing
curl -X POST http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID/reprocess-ocr \
  -H "X-Tenant-ID: test-tenant"
```

---

### Issue 4: Low OCR Accuracy

**Symptoms:**
- Extracted text has many errors
- Missing words or garbled text

**Diagnosis:**
```bash
# Check PDF rendering DPI
docker compose exec documentation-service env | grep DPI

# Check Tesseract engine mode
docker compose exec documentation-service env | grep OEM
```

**Optimization:**
```yaml
# In docker-compose.yml, increase DPI for better accuracy
environment:
  - TESSERACT_DPI=600  # Higher DPI = better accuracy, slower processing
  - TESSERACT_OEM=1    # LSTM engine (best accuracy)
  - TESSERACT_PSM=3    # Automatic page segmentation
```

**Restart after changes:**
```bash
docker compose down documentation-service
docker compose up -d documentation-service
```

---

### Issue 5: Full-Text Search Returns No Results

**Symptoms:**
```json
{
  "content": [],
  "totalElements": 0
}
```

**Diagnosis:**
```bash
# Check if OCR text was saved
curl -s http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID \
  -H "X-Tenant-ID: test-tenant" | jq '.ocrText'

# Check database directly
docker compose exec postgres psql -U healthdata -d docs_db \
  -c "SELECT id, file_name, ocr_status, length(ocr_text) FROM document_attachments;"
```

**Possible Causes:**

1. **GIN index not created**
   ```sql
   -- Check for index
   docker compose exec postgres psql -U healthdata -d docs_db \
     -c "\d document_attachments"
   ```

2. **Search query syntax issue**
   - Use simple words: "diabetes" not "diabetes mellitus type 2"
   - PostgreSQL full-text search uses stemming: "diabetes" matches "diabetic"

3. **Tenant ID mismatch**
   - Ensure same `X-Tenant-ID` used for upload and search

---

## Rollback Plan

### When to Rollback

Rollback if:
- Service fails to start after 3 restart attempts
- OCR processing fails for all document types
- Database corruption detected
- Critical performance degradation (>30s response times)

### Rollback Steps

**1. Stop Current Service**
```bash
docker compose down documentation-service
```

**2. Restore Previous Image (if available)**
```bash
# List available images
docker images | grep documentation-service

# Tag previous working image as latest
docker tag documentation-service:previous documentation-service:latest
```

**3. Revert Code Changes**
```bash
# If needed, revert to previous commit
git log --oneline -5
git reset --hard <previous-commit-hash>

# Rebuild from previous code
./gradlew :modules:services:documentation-service:bootJar -x test
docker compose build documentation-service
```

**4. Restart Service**
```bash
docker compose up -d documentation-service
docker compose logs -f documentation-service
```

**5. Verify Rollback**
```bash
curl http://localhost:8091/actuator/health
```

---

## Next Steps After Successful Deployment

### 1. Performance Testing
```bash
# Install Apache Bench (if not installed)
# macOS: brew install httpd
# Ubuntu: sudo apt-get install apache2-utils

# Test concurrent uploads (10 concurrent, 100 total)
ab -n 100 -c 10 -p test-file.pdf -T application/pdf \
  -H "X-Tenant-ID: test-tenant" \
  http://localhost:8091/api/documents/clinical/$DOCUMENT_ID/upload
```

### 2. Monitoring Setup
```bash
# View metrics endpoint
curl http://localhost:8091/actuator/metrics

# Monitor OCR processing times
curl http://localhost:8091/actuator/metrics/ocr.processing.time

# Monitor file upload sizes
curl http://localhost:8091/actuator/metrics/ocr.file.size
```

### 3. Log Analysis
```bash
# Export logs for analysis
docker compose logs documentation-service > ocr-deployment-logs.txt

# Analyze OCR processing times
grep "OCR processing completed" ocr-deployment-logs.txt | \
  awk '{print $NF}' | sort -n
```

### 4. Prepare for Staging Deployment
- Document any configuration changes made during local testing
- Create deployment checklist for staging environment
- Update monitoring dashboards with OCR metrics

---

## Summary

This deployment plan provides a comprehensive, step-by-step approach to deploying the OCR-enabled Documentation Service to Docker for local testing.

**Key Points:**
- ✅ Pre-cache dependencies to avoid build timeouts
- ✅ Verify Tesseract installation in Docker image
- ✅ Comprehensive smoke testing with real documents
- ✅ Troubleshooting guide for common issues
- ✅ Rollback plan for emergency recovery

**Estimated Total Time:** 16-30 minutes (first deployment)

**Ready to proceed?** Start with [Phase 1: Pre-cache Dependencies](#phase-1-pre-cache-dependencies)

---

_Last Updated: January 24, 2026_
_Version: 1.0 - Initial Docker Deployment Plan for OCR_

# OCR Integration Test Guide

**Document Purpose:** Comprehensive guide for running and understanding the OCR integration tests for the HDIM Documentation Service.

**Last Updated:** January 24, 2026
**Related Issues:** #245 (OCR Document Processing Implementation)

---

## Table of Contents

1. [Overview](#overview)
2. [Test Coverage](#test-coverage)
3. [Prerequisites](#prerequisites)
4. [Running Tests](#running-tests)
5. [Test Architecture](#test-architecture)
6. [Test Scenarios](#test-scenarios)
7. [Troubleshooting](#troubleshooting)
8. [CI/CD Integration](#cicd-integration)

---

## Overview

The OCR integration tests validate the complete end-to-end workflow for document text extraction:

```
File Upload → OCR Processing → Text Extraction → Full-Text Search
```

These tests use **real components** (not mocks):
- PostgreSQL database with Testcontainers
- Tesseract OCR engine
- PDFBox for PDF processing
- Spring Boot application context

**Test Duration:** ~2-3 minutes (includes Docker container startup)

---

## Test Coverage

### Features Tested

| Feature | Test Method | Coverage |
|---------|------------|----------|
| PDF upload with OCR | `testPdfUploadWithOcrExtraction()` | ✅ Complete |
| Image upload with OCR | `testImageUploadWithOcrExtraction()` | ✅ Complete |
| OCR status polling | `testOcrStatusPolling()` | ✅ Complete |
| OCR reprocessing | `testOcrReprocessing()` | ✅ Complete |
| Full-text search | `testFullTextSearchOnOcrDocuments()` | ✅ Complete |
| Unsupported file rejection | `testUnsupportedFileTypeRejection()` | ✅ Complete |
| Oversized file rejection | `testOversizedFileRejection()` | ✅ Complete |
| Multi-tenant isolation | `testMultiTenantOcrSearchIsolation()` | ✅ Complete |

### Code Coverage Targets

- **Line Coverage:** 90%+
- **Branch Coverage:** 85%+
- **Method Coverage:** 95%+

---

## Prerequisites

### System Requirements

**Required:**
- Java 21 (LTS)
- Docker Desktop (for Testcontainers)
- Tesseract OCR installed locally (for development)
- Gradle 8.11+

**Recommended:**
- 8GB+ RAM (for Docker containers)
- 10GB+ free disk space

### Tesseract Installation

**macOS:**
```bash
brew install tesseract
brew install tesseract-lang  # Optional: additional languages
```

**Ubuntu/WSL2:**
```bash
sudo apt-get update
sudo apt-get install -y tesseract-ocr tesseract-ocr-eng
```

**Verify Installation:**
```bash
tesseract --version
# Should output: tesseract 5.x.x

ls /usr/share/tessdata/
# Should include: eng.traineddata
```

### Docker Setup

**Verify Docker is running:**
```bash
docker ps
# Should show running containers or empty list (no errors)
```

**Pull PostgreSQL image (optional - Testcontainers will do this):**
```bash
docker pull postgres:16-alpine
```

---

## Running Tests

### Quick Start

**Run all OCR integration tests:**
```bash
cd backend
./gradlew :modules:services:documentation-service:test --tests "*OcrIntegrationTest"
```

**Run specific test method:**
```bash
./gradlew :modules:services:documentation-service:test \
  --tests "OcrIntegrationTest.testPdfUploadWithOcrExtraction"
```

### Gradle Task Options

**Run with detailed output:**
```bash
./gradlew test --tests "*OcrIntegrationTest" --info
```

**Run with test report:**
```bash
./gradlew test --tests "*OcrIntegrationTest"
open backend/modules/services/documentation-service/build/reports/tests/test/index.html
```

**Run with coverage report:**
```bash
./gradlew test --tests "*OcrIntegrationTest" jacocoTestReport
open backend/modules/services/documentation-service/build/reports/jacoco/test/html/index.html
```

**Clean and re-run:**
```bash
./gradlew clean test --tests "*OcrIntegrationTest" --rerun-tasks
```

### IDE Execution

**IntelliJ IDEA:**
1. Open `OcrIntegrationTest.java`
2. Click green arrow next to class name
3. Select "Run 'OcrIntegrationTest'"

**VS Code:**
1. Install "Test Runner for Java" extension
2. Open Testing sidebar
3. Click play button next to test class

---

## Test Architecture

### Test Infrastructure

```
┌─────────────────────────────────────────┐
│         Spring Boot Test Context       │
│  @SpringBootTest                        │
│  @AutoConfigureMockMvc                  │
└─────────────────────────────────────────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
┌──────────────┐      ┌──────────────────┐
│ Testcontainers│      │   MockMvc        │
│ PostgreSQL    │      │   (REST API)     │
│ 16-alpine     │      └──────────────────┘
└──────────────┘                │
        │                       │
        ▼                       ▼
┌──────────────────────────────────────────┐
│    ClinicalDocumentService               │
│    ├── OcrService (Tesseract)            │
│    ├── DocumentAttachmentRepository      │
│    └── File Storage (temp directory)     │
└──────────────────────────────────────────┘
```

### Key Components

**1. Testcontainers PostgreSQL**
- Runs real PostgreSQL 16-alpine in Docker
- Automatic lifecycle management
- Isolated database per test run
- Full-text search capabilities enabled

**2. TestFileGenerator Utility**
- Creates real PDF documents with PDFBox
- Generates PNG/JPEG/TIFF images with Java AWT
- High-quality images (300 DPI equivalent) for OCR accuracy
- Multi-page PDF support for performance testing

**3. Async Testing with Awaitility**
- Polls for OCR completion (max 30 seconds)
- Handles async processing gracefully
- Configurable polling interval (2 seconds)

**4. MockMvc for API Testing**
- Tests REST endpoints without HTTP server
- Validates request/response contracts
- Verifies security annotations

### Test Configuration

**application-test.yml highlights:**
```yaml
healthdata:
  document:
    storage:
      base-path: ${java.io.tmpdir}/test-documents  # Temp directory
      max-file-size: 10485760  # 10MB limit
    ocr:
      enabled: true
      async: false  # Synchronous for easier testing
      tesseract:
        language: eng
        oem: 1  # LSTM engine (best accuracy)
        psm: 3  # Automatic page segmentation
```

---

## Test Scenarios

### Scenario 1: PDF Upload with OCR Extraction

**Test:** `testPdfUploadWithOcrExtraction()`

**Flow:**
1. Create PDF with text: "Lab Result: Hemoglobin A1c 7.2%"
2. Upload to `/api/documents/clinical/{id}/upload`
3. Verify initial status: `PENDING`
4. Wait for async processing (Awaitility)
5. Verify final status: `COMPLETED`
6. Assert OCR text contains "Hemoglobin" and "7.2"

**Expected Duration:** 5-10 seconds

**Success Criteria:**
- ✅ File uploaded successfully (HTTP 201)
- ✅ OCR status transitions: PENDING → PROCESSING → COMPLETED
- ✅ Extracted text matches input text (case-insensitive)
- ✅ `ocrProcessedAt` timestamp is set
- ✅ No error messages

---

### Scenario 2: Image Upload with OCR Extraction

**Test:** `testImageUploadWithOcrExtraction()`

**Flow:**
1. Create PNG image with text: "Patient Name: John Doe\nDOB: 01/15/1980"
2. Upload to endpoint
3. Wait for OCR processing
4. Verify text extraction (partial match allowed due to OCR variability)

**OCR Accuracy Note:**
- High-quality images: 95%+ accuracy
- Lower quality: 70-90% accuracy
- Test uses partial string matching: "john", "doe", "patient"

**Expected Duration:** 5-8 seconds

---

### Scenario 3: Full-Text Search on OCR'd Documents

**Test:** `testFullTextSearchOnOcrDocuments()`

**Flow:**
1. Upload 3 documents:
   - `diabetes-diagnosis.pdf`: "Diagnosis: Type 2 Diabetes Mellitus. HbA1c: 8.5%"
   - `hypertension-note.pdf`: "Blood Pressure: 150/95 mmHg. Diagnosis: Essential Hypertension"
   - `cholesterol-lab.pdf`: "Total Cholesterol: 240 mg/dL. LDL: 160 mg/dL. High cholesterol"
2. Search for "diabetes" → Expect: diabetes-diagnosis.pdf first
3. Search for "hypertension blood pressure" → Expect: hypertension-note.pdf first
4. Search for "cholesterol" → Expect: cholesterol-lab.pdf first
5. Test pagination (page=0, size=2)

**PostgreSQL Full-Text Search:**
```sql
to_tsvector('english', ocr_text) @@ plainto_tsquery('english', 'diabetes')
ORDER BY ts_rank(to_tsvector('english', ocr_text), plainto_tsquery('english', 'diabetes')) DESC
```

**Expected Duration:** 15-20 seconds (3 uploads + 3 searches)

---

### Scenario 4: OCR Reprocessing

**Test:** `testOcrReprocessing()`

**Flow:**
1. Upload document → Wait for completion
2. Trigger reprocessing: `POST /api/documents/clinical/attachments/{id}/reprocess-ocr`
3. Verify status reset to `PENDING`
4. Wait for reprocessing completion
5. Verify final status: `COMPLETED`

**Use Cases:**
- Failed OCR processing retry
- Configuration change (e.g., language switch)
- Tesseract upgrade

**Expected Duration:** 10-15 seconds (2 processing cycles)

---

### Scenario 5: Multi-Tenant Isolation

**Test:** `testMultiTenantOcrSearchIsolation()`

**Flow:**
1. Upload document for tenant1: "Tenant 1 confidential data"
2. Upload document for tenant2: "Tenant 2 confidential data"
3. Search as tenant1 → Should only see tenant1 data
4. Search as tenant2 → Should only see tenant2 data

**Security Validation:**
- ✅ Tenant ID filtering in repository queries
- ✅ No cross-tenant data leakage
- ✅ HIPAA compliance (tenant isolation)

**Expected Duration:** 15-20 seconds

---

### Scenario 6: Error Handling

**Test:** `testUnsupportedFileTypeRejection()`

**Flow:**
1. Attempt to upload `.docx` file
2. Expect: HTTP 400 (Bad Request)
3. Verify: File not saved, no database record

**Test:** `testOversizedFileRejection()`

**Flow:**
1. Attempt to upload 11MB file (limit: 10MB)
2. Expect: HTTP 400 (Bad Request)

**Expected Duration:** <1 second each

---

## Troubleshooting

### Common Issues

#### Issue 1: Docker Not Running

**Error:**
```
org.testcontainers.containers.ContainerLaunchException:
  Container startup failed
```

**Solution:**
```bash
# Start Docker Desktop
# Verify Docker is running
docker ps

# Check Docker resources
docker system df
```

---

#### Issue 2: Tesseract Not Found

**Error:**
```
net.sourceforge.tess4j.TesseractException:
  Error opening data file /usr/share/tessdata/eng.traineddata
```

**Solution (macOS):**
```bash
brew install tesseract tesseract-lang

# Verify installation
tesseract --version
ls /usr/local/share/tessdata/

# Set environment variable (if needed)
export TESSDATA_PREFIX=/usr/local/share/tessdata
```

**Solution (Ubuntu/WSL2):**
```bash
sudo apt-get install -y tesseract-ocr tesseract-ocr-eng

# Verify installation
tesseract --version
ls /usr/share/tessdata/

# For tests, set in application-test.yml or environment
export TESSDATA_PREFIX=/usr/share/tessdata
```

---

#### Issue 3: Port Conflict

**Error:**
```
Address already in use: bind
```

**Solution:**
```bash
# Tests use random port (server.port=0)
# If issue persists, kill process on port 5435 (PostgreSQL)
lsof -ti:5435 | xargs kill -9
```

---

#### Issue 4: Slow Test Execution

**Symptoms:**
- Tests take > 5 minutes
- High CPU usage

**Solutions:**
1. **Increase Docker resources:**
   - Docker Desktop → Settings → Resources
   - RAM: 8GB+ recommended
   - CPU: 4+ cores

2. **Disable unnecessary services:**
   - Tests already disable Redis, Kafka in application-test.yml
   - Verify no other services running

3. **Run single test method:**
   ```bash
   ./gradlew test --tests "OcrIntegrationTest.testPdfUploadWithOcrExtraction"
   ```

---

#### Issue 5: OCR Accuracy Too Low

**Symptoms:**
- Tests fail on text extraction assertions
- OCR text doesn't match expected content

**Solutions:**
1. **Verify Tesseract language data:**
   ```bash
   ls /usr/share/tessdata/
   # Should include: eng.traineddata
   ```

2. **Check test image quality:**
   - TestFileGenerator creates 800x600px images
   - Font size: 18pt (large for clarity)
   - High DPI for PDFs: 300 DPI

3. **Adjust test assertions:**
   - Use partial string matching
   - Case-insensitive comparisons
   - Allow for OCR variability (e.g., "I" vs "l")

---

#### Issue 6: Testcontainers Network Issues

**Error:**
```
Could not find a valid Docker environment
```

**Solutions:**
1. **Check Docker socket permissions (Linux/WSL2):**
   ```bash
   sudo chmod 666 /var/run/docker.sock
   ```

2. **Verify Testcontainers configuration:**
   ```bash
   # ~/.testcontainers.properties (optional)
   docker.client.strategy=org.testcontainers.dockerclient.UnixSocketClientProviderStrategy
   ```

---

## CI/CD Integration

### GitHub Actions Example

**File:** `.github/workflows/ocr-tests.yml`

```yaml
name: OCR Integration Tests

on:
  push:
    branches: [main, develop]
    paths:
      - 'backend/modules/services/documentation-service/**'
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Install Tesseract OCR
        run: |
          sudo apt-get update
          sudo apt-get install -y tesseract-ocr tesseract-ocr-eng
          tesseract --version

      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}

      - name: Run OCR Integration Tests
        run: |
          cd backend
          ./gradlew :modules:services:documentation-service:test \
            --tests "*OcrIntegrationTest" \
            --info

      - name: Publish Test Report
        uses: mikepenz/action-junit-report@v4
        if: always()
        with:
          report_paths: '**/build/test-results/test/TEST-*.xml'

      - name: Upload Test Coverage
        uses: codecov/codecov-action@v4
        with:
          files: ./backend/modules/services/documentation-service/build/reports/jacoco/test/jacocoTestReport.xml
```

### Expected CI/CD Duration

| Stage | Duration |
|-------|----------|
| Docker image pull (first run) | 2-3 minutes |
| Gradle dependency download | 1-2 minutes |
| Test execution | 2-3 minutes |
| **Total** | **5-8 minutes** |

**Subsequent runs (cached):** 2-3 minutes

---

## Performance Benchmarks

### Test Execution Times

| Test Method | Expected Duration | Max Duration |
|-------------|-------------------|--------------|
| PDF upload + OCR | 5-10 seconds | 30 seconds |
| Image upload + OCR | 5-8 seconds | 30 seconds |
| OCR status polling | 5-10 seconds | 30 seconds |
| OCR reprocessing | 10-15 seconds | 60 seconds |
| Full-text search (3 docs) | 15-20 seconds | 90 seconds |
| Unsupported file rejection | <1 second | 5 seconds |
| Oversized file rejection | <1 second | 5 seconds |
| Multi-tenant isolation | 15-20 seconds | 90 seconds |
| **Total Suite** | **60-90 seconds** | **5 minutes** |

**Note:** First run may be slower due to Docker image pull and Testcontainers initialization.

---

## Test Maintenance

### When to Update Tests

**1. OCR Library Upgrade**
- Update Tesseract version → Re-test accuracy
- Update Tess4j version → Verify compatibility
- Update PDFBox version → Regenerate test PDFs

**2. Database Schema Changes**
- Update Liquibase migrations → Run entity-migration validation
- Add new OCR fields → Update test assertions
- Modify indexes → Test full-text search performance

**3. API Changes**
- New endpoints → Add test methods
- Changed request/response contracts → Update MockMvc assertions
- New security rules → Update @WithMockUser annotations

### Test Data Refresh

**Regenerate test files annually:**
```bash
# Run TestFileGenerator main method
cd backend/modules/services/documentation-service
./gradlew test --tests "TestFileGenerator" --info
```

---

## Additional Resources

**Documentation:**
- [OCR Implementation Summary](./ISSUE_245_OCR_COMPLETION_SUMMARY.md)
- [Database Architecture Guide](../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)
- [Liquibase Workflow](../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md)

**External Links:**
- [Tesseract OCR Documentation](https://tesseract-ocr.github.io/)
- [PDFBox Documentation](https://pdfbox.apache.org/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Awaitility Documentation](https://github.com/awaitility/awaitility)

---

## Support

**Questions or Issues:**
- **Internal:** Contact HDIM Platform Team
- **GitHub:** Open issue with label `ocr` and `testing`
- **Email:** healthdata-support@example.com

---

_Last Updated: January 24, 2026_
_Version: 1.0 - Initial OCR Integration Test Guide_

# OCR Deployment Quick Reference

**Purpose:** Quick command reference for deploying and testing OCR functionality
**Last Updated:** January 24, 2026

---

## 🚀 Quick Deployment

### Automated Deployment (Recommended)

```bash
# Full deployment with verification
./scripts/deploy-ocr-service.sh --verify

# Fast deployment (skip dependency cache)
./scripts/deploy-ocr-service.sh --skip-deps

# Fresh build (no cache, slower but thorough)
./scripts/deploy-ocr-service.sh --no-cache
```

**Duration:** 16-30 minutes (first run), 5-10 minutes (subsequent)

---

## 📋 Manual Deployment Steps

### 1. Pre-cache Dependencies (5-10 min)
```bash
cd backend
./gradlew downloadDependencies --no-daemon
```

### 2. Build Service JAR (2-3 min)
```bash
./gradlew :modules:services:documentation-service:bootJar -x test --no-daemon
```

### 3. Build Docker Image (3-5 min)
```bash
cd ..
docker compose build documentation-service

# Verify Tesseract
docker run --rm hdim-master-documentation-service tesseract --version
```

### 4. Start Service (1-2 min)
```bash
docker compose up -d documentation-service
docker compose logs -f documentation-service
```

### 5. Verify Health
```bash
curl http://localhost:8091/actuator/health | jq '.'
```

---

## 🧪 Quick Smoke Tests

### Create Test Document
```bash
# Save document ID from response
DOCUMENT_ID=$(curl -s -X POST http://localhost:8091/api/documents/clinical \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -d '{
    "patientId": "patient-123",
    "documentType": "LAB_RESULT",
    "title": "OCR Test",
    "status": "FINAL"
  }' | jq -r '.id')

echo "Document ID: $DOCUMENT_ID"
```

### Upload PDF (Create test file first)
```bash
# Create simple test PDF
echo "Lab Result: Hemoglobin A1c 7.2%" > /tmp/test.txt
pandoc /tmp/test.txt -o /tmp/test.pdf  # Requires pandoc

# Upload and save attachment ID
ATTACHMENT_ID=$(curl -s -X POST \
  "http://localhost:8091/api/documents/clinical/$DOCUMENT_ID/upload" \
  -H "X-Tenant-ID: test-tenant" \
  -F "file=@/tmp/test.pdf" \
  -F "title=Test PDF" | jq -r '.id')

echo "Attachment ID: $ATTACHMENT_ID"
```

### Monitor OCR Processing
```bash
# Poll status every 2 seconds
for i in {1..15}; do
  curl -s "http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID/ocr-status" \
    -H "X-Tenant-ID: test-tenant" | jq '.'
  sleep 2
done
```

### Verify Text Extraction
```bash
curl -s "http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID" \
  -H "X-Tenant-ID: test-tenant" | jq '.ocrText'
```

### Test Full-Text Search
```bash
curl -s "http://localhost:8091/api/documents/clinical/search-ocr?query=hemoglobin" \
  -H "X-Tenant-ID: test-tenant" | jq '.content[0].ocrText'
```

---

## 🔍 Troubleshooting Commands

### Check Service Status
```bash
docker compose ps documentation-service
docker compose logs --tail=100 documentation-service
```

### Verify Tesseract in Container
```bash
docker compose exec documentation-service tesseract --version
docker compose exec documentation-service ls -la /usr/share/tessdata/
```

### Check OCR Configuration
```bash
docker compose exec documentation-service env | grep -E "OCR|TESSERACT"
```

### Restart Service
```bash
docker compose restart documentation-service
docker compose logs -f documentation-service
```

### Rebuild from Scratch
```bash
docker compose down documentation-service
docker compose build --no-cache documentation-service
docker compose up -d documentation-service
```

### Check Database
```bash
docker compose exec postgres psql -U healthdata -d docs_db \
  -c "SELECT id, file_name, ocr_status FROM document_attachments ORDER BY created_at DESC LIMIT 5;"
```

---

## 📊 Monitoring Commands

### Health Check
```bash
curl http://localhost:8091/actuator/health | jq '.'
```

### Metrics
```bash
# All metrics
curl http://localhost:8091/actuator/metrics | jq '.'

# OCR-specific metrics
curl http://localhost:8091/actuator/metrics/ocr.processing.time
```

### Logs
```bash
# Real-time logs
docker compose logs -f documentation-service

# Last 100 lines
docker compose logs --tail=100 documentation-service

# Filter for OCR
docker compose logs documentation-service | grep OCR

# Export logs
docker compose logs documentation-service > ocr-logs.txt
```

### Container Stats
```bash
docker stats documentation-service
```

---

## 🧹 Cleanup Commands

### Stop Service
```bash
docker compose down documentation-service
```

### Remove Volumes (⚠️ Data Loss)
```bash
docker compose down -v documentation-service
```

### Clean Build Cache
```bash
# Gradle cache
cd backend
./gradlew clean

# Docker cache
docker builder prune -a
```

### Remove All Test Data
```bash
docker compose exec postgres psql -U healthdata -d docs_db \
  -c "DELETE FROM document_attachments WHERE tenant_id = 'test-tenant';"
```

---

## 📂 Key Files

### Documentation
- `docs/OCR_DOCKER_DEPLOYMENT_PLAN.md` - Full deployment guide
- `docs/ISSUE_245_OCR_COMPLETION_SUMMARY.md` - API reference
- `docs/OCR_INTEGRATION_TEST_GUIDE.md` - Testing guide

### Scripts
- `scripts/deploy-ocr-service.sh` - Automated deployment
- `scripts/verify-ocr-deployment.sh` - Verification tests

### Configuration
- `backend/modules/services/documentation-service/Dockerfile` - Container definition
- `backend/modules/services/documentation-service/src/main/resources/application.yml` - Service config
- `docker-compose.yml` - Service orchestration

---

## ⚡ Performance Tips

### Optimize Build Time
```bash
# Skip tests (already validated)
./gradlew bootJar -x test

# Use build cache
docker compose build  # Uses cache by default

# Parallel Gradle builds
./gradlew --parallel bootJar
```

### Optimize OCR Processing
```bash
# Increase DPI for better accuracy (slower)
docker compose exec documentation-service sh -c 'export TESSERACT_DPI=600'

# Use faster engine mode (lower accuracy)
docker compose exec documentation-service sh -c 'export TESSERACT_OEM=0'
```

### Optimize Database
```bash
# Verify GIN index exists
docker compose exec postgres psql -U healthdata -d docs_db \
  -c "\d document_attachments"

# Analyze full-text search performance
docker compose exec postgres psql -U healthdata -d docs_db \
  -c "EXPLAIN ANALYZE SELECT * FROM document_attachments WHERE to_tsvector('english', ocr_text) @@ plainto_tsquery('english', 'diabetes');"
```

---

## 🎯 Success Criteria

✅ **Deployment Success:**
- Container status: "Up"
- Health endpoint: `{"status":"UP"}`
- Tesseract version: 5.x.x
- No errors in logs (last 100 lines)

✅ **OCR Functionality:**
- PDF upload returns HTTP 201
- OCR status transitions: PENDING → COMPLETED
- Text extraction accuracy ≥ 95%
- Search returns relevant results

✅ **Performance:**
- OCR processing: 2-10s for 1-5 page PDF
- Search query: <100ms
- Service startup: <60s

---

## 🆘 Emergency Commands

### Service Won't Start
```bash
# View full error logs
docker compose logs --tail=200 documentation-service

# Check if port 8091 is in use
lsof -ti:8091 | xargs kill -9

# Restart with clean state
docker compose down documentation-service
docker compose up -d documentation-service
```

### OCR Stuck in PENDING
```bash
# Trigger reprocessing
curl -X POST "http://localhost:8091/api/documents/clinical/attachments/$ATTACHMENT_ID/reprocess-ocr" \
  -H "X-Tenant-ID: test-tenant"

# Restart service
docker compose restart documentation-service
```

### Database Connection Issues
```bash
# Check PostgreSQL is running
docker compose ps postgres

# Restart PostgreSQL
docker compose restart postgres

# Wait for database ready
docker compose logs postgres | grep "ready to accept connections"
```

### Complete Reset (⚠️ Nuclear Option)
```bash
# Stop all services
docker compose down

# Remove all volumes (data loss!)
docker volume prune -f

# Rebuild and restart
docker compose build --no-cache documentation-service
docker compose up -d postgres
sleep 10
docker compose up -d documentation-service
```

---

## 📞 Support

**Documentation:** See `docs/OCR_DOCKER_DEPLOYMENT_PLAN.md` for detailed troubleshooting

**Logs Location:**
- Container: `docker compose logs documentation-service`
- Local: `backend/modules/services/documentation-service/build/logs/`

**Common Issues:** See "Troubleshooting" section in deployment plan

---

_Quick Reference v1.0 - January 24, 2026_

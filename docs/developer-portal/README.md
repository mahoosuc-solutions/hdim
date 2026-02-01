# HDIM Platform - Developer Portal

**Status:** ✅ Production Ready
**Last Updated:** January 24, 2026
**Version:** 1.0

## Overview

Interactive API documentation portal with embedded Swagger UI for exploring and testing HDIM Platform APIs.

**Features:**
- 🚀 Interactive API explorer with "Try it out" functionality
- 📖 Live OpenAPI 3.0 specifications for all services
- 🔐 Built-in authentication testing
- 📝 Request/response examples
- 🎨 Modern, responsive UI
- 🔍 Search and filter endpoints

---

## Quick Start

### Option 1: Open Locally (Recommended)

```bash
# From project root
open docs/developer-portal/index.html

# Or using a web browser
# macOS
open -a "Google Chrome" docs/developer-portal/index.html

# Linux
xdg-open docs/developer-portal/index.html

# Windows
start docs/developer-portal/index.html
```

### Option 2: Serve with Python

```bash
# From project root
cd docs/developer-portal
python3 -m http.server 8000

# Open browser
open http://localhost:8000
```

### Option 3: Serve with Node.js

```bash
# Install http-server globally
npm install -g http-server

# From project root
cd docs/developer-portal
http-server -p 8000

# Open browser
open http://localhost:8000
```

---

## Available API Services

The developer portal provides interactive documentation for 6 core HDIM services:

| Service | Description | Endpoints | Port |
|---------|-------------|-----------|------|
| **Quality Measures** | HEDIS measure evaluation, assignments, overrides | 84 | 8087 |
| **Patient Management** | Patient demographics, search, CRUD operations | 15 | 8084 |
| **FHIR R4** | FHIR resource management (HAPI FHIR) | 120+ | 8085 |
| **Care Gaps** | Care gap detection, closure, recommendations | 12 | 8086 |
| **CQL Engine** | CQL expression evaluation, logic processing | 8 | 8081 |
| **API Gateway** | Authentication, routing, rate limiting | 6 | 8001 |

---

## Using the Developer Portal

### 1. Select a Service

Click one of the service buttons in the navigation bar:

- **Quality Measures** - HEDIS measure evaluation (default)
- **Patient Management** - Patient CRUD operations
- **FHIR R4** - FHIR resource management
- **Care Gaps** - Care gap workflows
- **CQL Engine** - CQL expression evaluation
- **API Gateway** - Authentication and routing

### 2. Authenticate

**Step 1: Get JWT Token**

1. Switch to **API Gateway** service
2. Find `POST /auth/login` endpoint
3. Click "Try it out"
4. Use test credentials:
   ```json
   {
     "username": "test_evaluator",
     "password": "password123"
   }
   ```
5. Click "Execute"
6. Copy the `token` from the response

**Step 2: Authorize Swagger UI**

1. Click the **"Authorize"** button (top-right)
2. Enter: `Bearer YOUR_JWT_TOKEN`
3. Click "Authorize"
4. Click "Close"

**Step 3: Set Tenant ID**

The portal automatically adds `X-Tenant-ID: TENANT-001` header to all requests.

### 3. Explore Endpoints

**Search Endpoints:**
- Use the search box to filter endpoints by name or tag
- Example: Search for "patient" to find patient-related endpoints

**Expand Endpoint:**
- Click an endpoint to view details
- See request parameters, response schemas, and examples

**Try It Out:**
1. Click "Try it out" button
2. Fill in required parameters
3. Click "Execute"
4. View request and response details

---

## Example Workflows

### Workflow 1: Search for a Patient

1. **Authenticate** (see above)
2. **Switch to:** Patient Management service
3. **Find:** `GET /api/v1/patients`
4. **Click:** "Try it out"
5. **Enter Query Params:**
   - `lastName`: Smith
   - `dateOfBirth`: 1980-05-15
6. **Click:** "Execute"
7. **View Response:** Patient list with IDs

### Workflow 2: Run Quality Measure Evaluation

1. **Prerequisites:** Have a valid `patientId` (from patient search)
2. **Switch to:** Quality Measures service
3. **Find:** `POST /api/v1/evaluations`
4. **Click:** "Try it out"
5. **Enter Request Body:**
   ```json
   {
     "patientId": "PAT-12345",
     "measureId": "COL-001",
     "evaluationDate": "2026-01-24T00:00:00Z"
   }
   ```
6. **Click:** "Execute"
7. **View Response:** Evaluation result (MET, NOT_MET, EXCLUDED)

### Workflow 3: Identify Care Gaps

1. **Prerequisites:** Have a valid `patientId`
2. **Switch to:** Care Gaps service
3. **Find:** `GET /api/v1/patients/{patientId}/care-gaps`
4. **Click:** "Try it out"
5. **Enter Path Param:** `patientId` (e.g., PAT-12345)
6. **Enter Query Params:**
   - `status`: OPEN
   - `priority`: HIGH
7. **Click:** "Execute"
8. **View Response:** List of open care gaps

---

## Features

### Auto-Added Headers

The portal automatically adds required headers to all requests:

```
X-Tenant-ID: TENANT-001
```

You only need to add the JWT token via the "Authorize" button.

### Persistent Authorization

Your JWT token is saved in browser localStorage. It persists across:
- Service switches
- Page refreshes
- Browser sessions (until you clear browser data)

### Request Interceptor

The portal includes a request interceptor that:
- Adds `X-Tenant-ID` header if missing
- Validates JWT token format
- Logs requests for debugging

### Deep Linking

Share specific endpoints with team members:
```
http://localhost:8000/#/Quality%20Measures/post_api_v1_evaluations
```

The URL updates as you navigate, allowing bookmarking and sharing.

### Filter and Search

**Search by endpoint name:**
- Type "patient" to find all patient-related endpoints
- Type "measure" to find measure-related endpoints
- Search is case-insensitive

**Filter by tag:**
- Endpoints are grouped by tags (e.g., "Patient Management", "Care Gaps")
- Use tag dropdowns to filter

---

## Updating OpenAPI Specifications

### When to Update

Update OpenAPI specs when:
- Adding new API endpoints
- Modifying request/response schemas
- Changing endpoint paths or methods
- Updating API descriptions or examples

### Generate Specifications

**Prerequisites:**
1. Services must be running: `docker compose up -d`
2. Services must expose `/v3/api-docs` endpoint

**Generate All Services:**
```bash
cd docs/api
./generate-openapi-specs.sh
```

**Generate Specific Service:**
```bash
cd docs/api
./generate-openapi-specs.sh --service quality-measure-service
```

**Output:**
```
docs/api/
├── openapi-gateway-service-v1.2.0.json
├── openapi-cql-engine-service-v1.2.0.json
├── openapi-fhir-service-v1.2.0.json
├── openapi-patient-service-v1.2.0.json
├── openapi-quality-measure-service-v1.2.0.json
└── openapi-care-gap-service-v1.2.0.json
```

### Refresh Portal

After generating new specs:

1. **Refresh browser** - Portal will load updated specs
2. **Clear cache** (if needed):
   - Chrome: Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)
   - Firefox: Ctrl+F5 (Windows/Linux) or Cmd+Shift+R (Mac)

---

## Troubleshooting

### "Error Loading API Specification"

**Problem:** Portal shows error when loading a service

**Solutions:**

1. **Check if OpenAPI spec exists:**
   ```bash
   ls -lh docs/api/openapi-quality-measure-service-v1.2.0.json
   ```

2. **Generate missing spec:**
   ```bash
   cd docs/api
   ./generate-openapi-specs.sh --service quality-measure-service
   ```

3. **Verify service is running:**
   ```bash
   docker compose ps quality-measure-service
   docker compose logs -f quality-measure-service
   ```

4. **Check SpringDoc is enabled:**
   ```yaml
   # In application.yml
   springdoc:
     api-docs:
       enabled: true
       path: /v3/api-docs
   ```

### "401 Unauthorized" When Testing Endpoints

**Problem:** API returns 401 error

**Solutions:**

1. **Get fresh JWT token:**
   - Switch to API Gateway service
   - Run `POST /auth/login`
   - Copy new token

2. **Re-authorize Swagger UI:**
   - Click "Authorize" button
   - Enter: `Bearer YOUR_NEW_TOKEN`
   - Click "Authorize"

3. **Check token expiry:**
   - JWT tokens expire after 60 minutes
   - Use refresh token or re-login

### "403 Forbidden" Error

**Problem:** API returns 403 error

**Solutions:**

1. **Verify user role:**
   - `test_evaluator` has EVALUATOR role
   - Some endpoints require ADMIN role

2. **Use appropriate test user:**
   - `test_admin` / `password123` - ADMIN role
   - `test_evaluator` / `password123` - EVALUATOR role
   - `test_analyst` / `password123` - ANALYST role

### Portal Not Loading

**Problem:** Blank page or infinite loading

**Solutions:**

1. **Check browser console:**
   - Press F12 to open DevTools
   - Check Console tab for errors

2. **Verify file paths:**
   - Ensure `docs/api/` directory exists
   - Ensure OpenAPI JSON files are present

3. **Disable browser extensions:**
   - Ad blockers may interfere with Swagger UI
   - Try in incognito/private mode

---

## Customization

### Change Default Service

Edit `index.html`:

```javascript
// Change from quality-measure-service to patient-service
window.addEventListener('load', () => {
    loadSpec('patient-service');
});
```

### Customize Swagger UI Theme

Add to `<style>` section in `index.html`:

```css
/* Dark theme example */
.swagger-ui {
    background: #1e1e1e;
    color: #d4d4d4;
}

.swagger-ui .opblock {
    background: #2d2d2d;
    border-color: #3e3e3e;
}
```

### Add Custom Headers

Edit request interceptor in `index.html`:

```javascript
requestInterceptor: (request) => {
    // Auto-add X-Tenant-ID
    if (!request.headers['X-Tenant-ID']) {
        request.headers['X-Tenant-ID'] = 'TENANT-001';
    }

    // Add custom headers
    request.headers['X-Custom-Header'] = 'value';

    return request;
}
```

---

## Production Deployment

### Option 1: Nginx

```nginx
server {
    listen 80;
    server_name api-docs.hdim.example.com;

    root /var/www/hdim/docs/developer-portal;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # Serve OpenAPI specs
    location /api/ {
        alias /var/www/hdim/docs/api/;
        add_header Access-Control-Allow-Origin *;
    }
}
```

### Option 2: Docker

Create `Dockerfile`:

```dockerfile
FROM nginx:alpine
COPY docs/developer-portal /usr/share/nginx/html
COPY docs/api /usr/share/nginx/html/api
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

Build and run:

```bash
docker build -t hdim-api-docs .
docker run -d -p 8080:80 hdim-api-docs
```

### Option 3: Static Site Hosting

Deploy to:
- **GitHub Pages** - Free for public repos
- **Netlify** - Auto-deploy from Git
- **Vercel** - Serverless deployment
- **AWS S3 + CloudFront** - Enterprise CDN

---

## Related Documentation

- **[API Getting Started Guide](../api/GETTING_STARTED.md)** - Authentication, workflows, error handling
- **[OpenAPI Specifications](../api/README.md)** - Complete API reference
- **[Postman Collections](./postman/README.md)** - Pre-configured API requests
- **[Service Catalog](../services/SERVICE_CATALOG.md)** - All 50+ services
- **[CLAUDE.md](../../CLAUDE.md)** - HDIM development quick reference

---

## Support

**Issues:**
- Check [Troubleshooting](#troubleshooting) section
- View service logs: `docker compose logs -f [SERVICE_NAME]`
- Check [Monitoring Dashboard](http://localhost:3000) (Grafana)

**Contact:**
- Email: support@hdim-platform.com
- Slack: #hdim-api-support

---

**Last Updated:** January 24, 2026
**Document Version:** 1.0
**Status:** ✅ Production Ready

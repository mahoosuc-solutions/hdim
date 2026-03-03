# Screenshot Requirements for Marketing Materials

## Issue Identified
The current `evaluations.png` screenshot shows an error state:
> "Unable to process quality measures. The CQL Engine service may be temporarily unavailable. Please try again later."

This needs to be replaced with a screenshot showing the successful evaluation workflow.

## Fix Applied
- Fixed context-path mismatch in CQL Engine service
- Docker and Kubernetes profiles now correctly use `/cql-engine` context path
- Code compiles successfully

## Screenshots Needed

### 1. Quality Measure Evaluations (REPLACE evaluations.png)
**What to capture:**
- The 3-step wizard showing Step 1 "Select Measure"
- NO error messages
- Show available HEDIS measures in the dropdown
- Categories populated with actual measure categories

**Steps to capture:**
1. Start backend services with `docker-compose up`
2. Navigate to Clinical Portal → Evaluations
3. Ensure CQL Engine is running and connected
4. Screenshot should show clean UI with measures available

### 2. Evaluation Results (NEW - evaluation-results.png)
**What to capture:**
- Step 3 "View Results" showing a successful evaluation
- Patient compliance status displayed
- Measure criteria shown with pass/fail indicators

### 3. Live Monitor (NEW - live-monitor.png)
**What to capture:**
- System health dashboard
- All services showing green/healthy status
- Integration connection status

### 4. AI Assistant (NEW - ai-assistant.png)
**What to capture:**
- AI chat interface
- Example query and response about a patient or measure
- Shows the natural language capability

### 5. Measure Builder (NEW - measure-builder.png)
**What to capture:**
- Custom measure creation interface
- CQL expression builder
- Measure definition fields

## How to Capture Screenshots

```bash
# 1. Start the backend
cd backend
docker-compose up -d

# 2. Start the frontend
cd apps/clinical-portal
npx nx serve clinical-portal

# 3. Navigate to http://localhost:4200

# 4. Use browser developer tools or screenshot tool
# Recommended resolution: 1920x1080 or 1440x900
```

## Screenshot Specifications
- **Format:** PNG
- **Resolution:** High-res (2x for retina)
- **Dimensions:** Consistent with existing screenshots (~1200px wide)
- **Browser:** Chrome (clean, no extensions visible)
- **Theme:** Light mode (default)
- **Data:** Use realistic test data (Main Street Clinic)

## Files to Update After Screenshots
1. `vercel-deploy/assets/screenshots/evaluations.png` - REPLACE
2. `vercel-deploy/gallery.html` - Update descriptions if needed
3. `content/platform-features/` - Reference new screenshots

# HDIM Demo Environment & Screenshot Scripts

This directory contains scripts for preparing the demo environment and capturing screenshots for product documentation.

## Scripts Overview

### 1. `prepare-demo-environment.sh`

**Purpose**: Fully automated script to prepare the complete demo environment for screenshot capture.

**What it does**:
- Cleans existing Docker environment
- Builds all backend services (36 services)
- Builds all frontend applications (5 apps)
- Starts infrastructure (PostgreSQL, Redis, Kafka, Zookeeper)
- Initializes database with migrations
- Seeds demo data (patients, users, care gaps)
- Performs health checks
- Starts all services

**Usage**:
```bash
cd /home/webemo-aaron/projects/hdim-master
./scripts/prepare-demo-environment.sh
```

**Duration**: ~30-45 minutes (depending on system)

**Output**: Fully running system with:
- All backend services healthy
- All frontend applications running
- 5 demo patients with clinical data
- 7 demo users for different roles
- Generated care gaps and quality measures
- AI conversation history

### 2. `capture-screenshots.js`

**Purpose**: Automated screenshot capture using Playwright for all user types and pages.

**What it does**:
- Logs in as each user type
- Navigates through all key pages
- Captures high-resolution screenshots
- Organizes screenshots by user type
- Generates an index file

**Prerequisites**:
```bash
cd /home/webemo-aaron/projects/hdim-master/scripts
npm install
```

**Usage**:
```bash
# Make sure all services are running first
./prepare-demo-environment.sh

# Then capture screenshots
cd scripts
npm run capture
```

**Duration**: ~15-20 minutes

**Output**: 70+ screenshots in `docs/screenshots/`

### 3. `validate-dockerfiles.sh`

**Purpose**: Enforces Dockerfile runtime consistency across services.

**What it checks**:
- `USER` is set in the runtime stage
- `HEALTHCHECK` is defined
- `JAVA_OPTS` is defined
- `wget` is installed if used by healthchecks

**Usage**:
```bash
./scripts/validate-dockerfiles.sh
```

### 4. `complete-open-prs.sh`

**Purpose**: Batch-validate and merge open PRs using the GitHub CLI.

**What it does**:
- Lists open PRs
- Optionally waits for checks
- Validates status checks and review state
- Merges PRs when conditions are met

**Usage**:
```bash
# Dry-run (default)
./scripts/complete-open-prs.sh

# Merge ready PRs
CONFIRM=1 ./scripts/complete-open-prs.sh

# Customize behavior
REPO=owner/repo LIMIT=100 MERGE_METHOD=merge REQUIRE_APPROVAL=0 WATCH_CHECKS=1 CONFIRM=1 ./scripts/complete-open-prs.sh
```

### 5. `capture-compose-logs.sh`

**Purpose**: Snapshot Docker Compose logs for troubleshooting.

**What it does**:
- Captures `docker compose ps`
- Captures `docker compose logs --since`
- Writes a timestamped log file under `logs/compose/`

**Usage**:
```bash
./scripts/capture-compose-logs.sh

# Custom compose file and time window
COMPOSE_FILE=docker-compose.demo.yml SINCE=2h ./scripts/capture-compose-logs.sh
```

### 6. `dev-shell-deployment.sh`

**Purpose**: Start only the shell + deployment console MFE with the demo stack.

**What it does**:
- Starts the demo Docker stack
- Starts `mfeDeployment` on port `4210`
- Starts `shell-app` on port `4300` with only the deployment remote

**Usage**:
```bash
./scripts/dev-shell-deployment.sh
```

**Notes**:
- Use this when you only need the deployment portal.
- Other MFEs are intentionally not started to avoid `remoteEntry` errors.

### 7. `dev-shell-all.sh`

**Purpose**: Start the shell with all core MFEs for full navigation.

**What it does**:
- Starts the demo Docker stack
- Starts `mfeDeployment` on port `4210`
- Starts `mfePatients` on port `4201`
- Starts `mfeMeasureBuilder` on port `4202`
- Starts `shell-app` on port `4300` with all remotes

**Usage**:
```bash
./scripts/dev-shell-all.sh
```

### 8. `shell-app-e2e` (Deployment Console UI Tests)

**Purpose**: Exercise the deployment console UI and local ops backend.

**What it does**:
- Launches the demo stack + shell + deployment MFE (via `dev-shell-deployment.sh`)
- Verifies the deployment console renders and connects to the ops service
- Validates `/ops/status` returns services

**Usage**:
```bash
BASE_URL=http://localhost:4300 OPS_BASE_URL=http://localhost:4710 npx nx e2e shell-app-e2e
```

**Convenience**:
```bash
npm run e2e:deployment-console
```

### 9. `verify-seeding-counts.sh`

**Purpose**: Validate seeded record counts per tenant across FHIR and care-gap services.

**What it does**:
- Queries FHIR `/Patient?_summary=count`
- Queries care-gap `/api/v1/care-gaps?page=0&size=1` and reads `totalElements`

**Usage**:
```bash
./scripts/verify-seeding-counts.sh

# With expectations (exit non-zero on mismatch)
EXPECTED_PATIENTS_PER_TENANT=100 ./scripts/verify-seeding-counts.sh

# Custom tenants
TENANTS=summit-care-2026,valley-health-2026 ./scripts/verify-seeding-counts.sh
```

---

## Quick Start

### Step 1: Prepare Environment

```bash
cd /home/webemo-aaron/projects/hdim-master
./scripts/prepare-demo-environment.sh
```

Wait for completion. You should see:
```
[SUCCESS] ==========================================
[SUCCESS] Demo environment ready!
[SUCCESS] ==========================================
```

### Step 2: Verify Services

Access these URLs to verify services are running:

**Frontend Applications**:
- Clinical Dashboard: http://localhost:3000
- Admin Portal: http://localhost:3001
- AI Assistant: http://localhost:3002
- Patient Portal: http://localhost:3003
- Analytics: http://localhost:3004

**Backend Health Checks**:
- Gateway: http://localhost:8080/actuator/health
- CQL Engine: http://localhost:8100/actuator/health
- Care Gap: http://localhost:8101/actuator/health
- Agent Runtime: http://localhost:8088/actuator/health

### Step 3: Install Screenshot Dependencies

```bash
cd /home/webemo-aaron/projects/hdim-master/scripts
npm install
```

### Step 4: Capture Screenshots

```bash
npm run capture
```

Screenshots will be saved to `../docs/screenshots/`

### Step 5: Review Screenshots

```bash
cd ../docs/screenshots
ls -R
cat INDEX.md
```

---

## Demo Credentials

| Role | Email | Password | Application URL |
|------|-------|----------|----------------|
| Care Manager | care.manager@demo.com | Demo2026! | http://localhost:3000 |
| Physician | dr.smith@demo.com | Demo2026! | http://localhost:3000 |
| System Admin | admin@demo.com | Demo2026! | http://localhost:3001 |
| AI User | ai.user@demo.com | Demo2026! | http://localhost:3002 |
| Patient | patient@demo.com | Demo2026! | http://localhost:3003 |
| Quality Manager | quality.manager@demo.com | Demo2026! | http://localhost:3000 |
| Billing Specialist | billing.specialist@demo.com | Demo2026! | http://localhost:3000 |

---

## Demo Patients

| MRN | Name | Age | Conditions | Purpose |
|-----|------|-----|------------|---------|
| MRN001 | John Diabetes | 58 | Type 2 Diabetes | Quality measure testing |
| MRN002 | Sarah Heart | 65 | CHF, HTN | Complex care example |
| MRN003 | Michael CKD | 51 | CKD Stage 3 | Specialty care example |
| MRN004 | Emma Healthy | 33 | None | Well patient example |
| MRN005 | Robert Complex | 78 | Multiple conditions | High-risk patient example |

---

## Troubleshooting

### Script fails during build

**Issue**: `./gradlew build` fails with compilation errors

**Solution**:
1. Check Java version: `java -version` (should be 17+)
2. Clean Gradle cache: `./gradlew clean --no-daemon`
3. Check for syntax errors in recent code changes
4. Review build output for specific errors

### Docker containers fail to start

**Issue**: Services fail to start with port binding errors

**Solution**:
1. Check for port conflicts: `netstat -tulpn | grep LISTEN`
2. Stop conflicting services
3. Verify Docker has enough resources (4GB+ RAM)
4. Check Docker logs: `docker-compose logs [service-name]`

### Frontend build fails

**Issue**: `npm install` or `npm run build` fails

**Solution**:
1. Check Node.js version: `node -v` (should be 18+)
2. Clear npm cache: `npm cache clean --force`
3. Remove node_modules: `rm -rf node_modules`
4. Reinstall: `npm install --legacy-peer-deps`

### Screenshot capture fails

**Issue**: Playwright cannot connect to services

**Solution**:
1. Verify all services are running and healthy
2. Check browser installation: `npx playwright install chromium`
3. Verify login URLs are accessible
4. Check credentials in script match database

### Database initialization fails

**Issue**: Flyway migrations fail

**Solution**:
1. Check PostgreSQL is running: `docker-compose ps postgres`
2. Verify database credentials in docker-compose.yml
3. Check migration scripts for syntax errors
4. Reset database: `docker-compose down -v postgres` then restart

---

## Manual Screenshot Capture

If automated capture fails, you can capture screenshots manually:

### Using Browser DevTools

1. Open Developer Tools (F12)
2. Toggle device toolbar (Ctrl+Shift+M)
3. Set resolution to 1920x1080
4. Capture screenshot:
   - Chrome: Ctrl+Shift+P → "Capture full size screenshot"
   - Firefox: Right-click → "Take a Screenshot"

### Using External Tools

**Recommended Tools**:
- **Linux**: Flameshot, GNOME Screenshot
- **Windows**: Snipping Tool, ShareX
- **Mac**: CMD+Shift+4
- **Cross-platform**: Greenshot, Snagit

### Naming Convention

Use this format: `{user-type}-{page-name}.png`

Examples:
- `care-manager-dashboard-overview.png`
- `physician-patient-clinical-summary.png`
- `admin-user-management.png`

---

## Customization

### Adding New Screenshots

Edit `capture-screenshots.js` and add to the `SCENARIOS` array:

```javascript
{
  userType: 'your-user-type',
  credentials: {
    email: 'user@demo.com',
    password: 'Demo2026!',
  },
  baseUrl: 'http://localhost:3000',
  pages: [
    { path: '/your-path', name: 'your-page-name', wait: 2000 },
  ],
}
```

### Changing Screenshot Resolution

Edit `CONFIG.viewport` in `capture-screenshots.js`:

```javascript
const CONFIG = {
  viewport: { width: 1920, height: 1080 }, // Change here
  // ...
};
```

### Adding Demo Data

Edit `prepare-demo-environment.sh` in the `seed_demo_data()` function to add more patients, users, or other data.

---

## Maintenance

### Updating Demo Data

To refresh demo data without rebuilding everything:

```bash
# Reset database
docker-compose down postgres
docker-compose up -d postgres

# Re-run migrations and seed
cd backend
./gradlew flywayMigrate
./gradlew seedDemoData
```

### Updating Screenshots

When UI changes, regenerate screenshots:

```bash
cd /home/webemo-aaron/projects/hdim-master/scripts
npm run capture
```

### Cleaning Up

To stop all services and clean up:

```bash
cd /home/webemo-aaron/projects/hdim-master
docker-compose down -v
```

---

## Log Artifacts

Scripts that generate log artifacts store them under `logs/`:
- `logs/test-runs/` for `test-all-local.sh`
- `logs/seed-runs/` for `seed-all-demo-data.sh`
- `logs/compose/` for `capture-compose-logs.sh`

---

## Documentation

- **Comprehensive Plan**: `../DOCUMENTATION_AND_DEMO_PLAN.md`
- **Checklist**: `../DOCUMENTATION_CHECKLIST.md`
- **Quick Start Guide**: `../docs/QUICK_START_GUIDE.md`
- **User Guides**: `../docs/user-guides/`

---

## Support

For issues with these scripts:
- Check logs: `./prepare-demo-environment.sh` outputs detailed logs
- Review Docker logs: `docker-compose logs [service-name]`
- Check service health: `curl http://localhost:PORT/actuator/health`

---

**Last Updated**: January 14, 2026  
**Version**: 1.0

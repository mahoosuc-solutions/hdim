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

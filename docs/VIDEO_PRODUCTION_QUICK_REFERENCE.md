# Video Production - Quick Reference

## 🎬 5-Minute Quick Start

```bash
# 1. Start Docker
sudo service docker start  # WSL2/Linux
# OR open Docker Desktop

# 2. Start services
cd /home/webemo-aaron/projects/hdim-master
docker compose -f demo/docker-compose.demo.yml up -d

# 3. Wait 90 seconds for services
sleep 90

# 4. Start screen recording
# Windows: Windows + G, then Windows + Alt + R
# macOS: QuickTime > File > New Screen Recording
# Linux: Start OBS Studio

# 5. Capture screenshots and execute demo
node scripts/capture-screenshots.js --phase BEFORE --scenario hedis-evaluation
# ... execute demo in browser ...
node scripts/capture-screenshots.js --phase DURING --scenario hedis-evaluation
# ... wait for data ...
node scripts/capture-screenshots.js --phase AFTER --scenario hedis-evaluation

# 6. Stop recording
```

## 📋 Pre-Recording Checklist

- [ ] Docker running
- [ ] Services healthy (`docker ps`)
- [ ] Data validated (`curl http://localhost:8090/api/v1/devops/fhir-validation/validate`)
- [ ] Browser ready (logged in, correct size)
- [ ] Recording tool configured
- [ ] Desktop clean

## 🎥 Recording Tools

| Platform | Tool | Shortcut |
|----------|------|----------|
| Windows | Game Bar | Windows + G, then Windows + Alt + R |
| macOS | QuickTime | Cmd + Shift + 5 |
| Linux | OBS Studio | `obs` command |

## 📸 Screenshot Phases

```bash
# BEFORE - Initial state
node scripts/capture-screenshots.js --phase BEFORE --scenario <scenario>

# DURING - Loading/processing
node scripts/capture-screenshots.js --phase DURING --scenario <scenario>

# AFTER - Final results
node scripts/capture-screenshots.js --phase AFTER --scenario <scenario>
```

## ✅ Validation

```bash
# Check FHIR data authenticity
curl -X POST http://localhost:8090/api/v1/devops/fhir-validation/validate | jq

# Expected: "overallStatus": "PASS"
```

## 📁 Output Locations

- **Videos**: `docs/demo-recordings/`
- **Screenshots**: `docs/screenshots/scenarios/`
- **Validation**: DevOps Agent UI (when available)

## 🔧 Troubleshooting

```bash
# Services not starting?
docker compose -f demo/docker-compose.demo.yml restart

# Screenshots failing?
curl http://localhost:4200  # Check portal is up

# Validation failing?
curl http://localhost:8085/fhir/metadata  # Check FHIR service
```

## 📖 Full Guide

See `docs/VIDEO_PRODUCTION_GUIDE.md` for complete documentation.

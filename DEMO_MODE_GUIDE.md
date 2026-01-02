# Demo Mode Guide 🎬

Quick setup for video demonstrations with easy-to-remember accounts.

## 🚀 Quick Setup

```bash
# 1. Setup demo accounts (one-time)
./setup-demo-mode.sh

# 2. Test all accounts
./test-demo-accounts.sh
```

## 👥 Demo Accounts

**All accounts use password: `demo123`**

### 1. 👨‍⚕️ Clinical Doctor (Evaluator)
```
Username: demo.doctor
Password: demo123
Name:     Dr. Sarah Chen
Role:     EVALUATOR
```
**Use for:** Patient care workflows, quality measure evaluation, clinical assessments

### 2. 📊 Data Analyst
```
Username: demo.analyst
Password: demo123
Name:     Michael Rodriguez
Role:     ANALYST
```
**Use for:** Data analysis, reporting, quality measure calculations, performance metrics

### 3. 🤝 Care Evaluator
```
Username: demo.care
Password: demo123
Name:     Jennifer Thompson
Role:     EVALUATOR
```
**Use for:** Care gap evaluation, patient assessment, care coordination workflows

### 4. 👑 System Administrator
```
Username: demo.admin
Password: demo123
Name:     David Johnson
Role:     ADMIN
```
**Use for:** Full system access, configuration, user management

### 5. 👁️ Stakeholder Viewer
```
Username: demo.viewer
Password: demo123
Name:     Emily Martinez
Role:     VIEWER
```
**Use for:** Read-only demos for stakeholders, executive dashboards

## 🎥 Video Recording Tips

### Login Sequence
```bash
# Use this curl command in your video (looks professional)
curl -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.doctor","password":"demo123"}'
```

### Demo Flow Suggestions

#### Option 1: Clinical Workflow (5 minutes)
1. Login as `demo.doctor`
2. View patient dashboard
3. Check quality measures for patients
4. Review care gaps
5. Show depression screening improvement

#### Option 2: Quality Manager Workflow (5 minutes)
1. Login as `demo.quality`
2. View quality measure dashboard
3. Show CMS reporting
4. Display performance metrics
5. Highlight 38% improvement statistic

#### Option 3: Care Coordination Workflow (5 minutes)
1. Login as `demo.care`
2. View care gap dashboard
3. Filter high-risk patients
4. Show intervention tracking
5. Display outcome improvements

#### Option 4: Executive Overview (3 minutes)
1. Login as `demo.viewer`
2. High-level dashboard
3. Key metrics and outcomes
4. ROI visualization
5. Integration highlights

## 🔄 Resetting Demo Accounts

If you need to reset the demo accounts:

```bash
# Re-run setup script
./setup-demo-mode.sh
```

This will:
- ✓ Clear existing demo accounts
- ✓ Recreate all 5 demo users
- ✓ Reset passwords to `demo123`
- ✓ Assign correct roles and tenants

## 🧪 Testing Before Recording

**Always test before recording!**

```bash
# Test all accounts
./test-demo-accounts.sh

# Or test individual account
curl -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.doctor","password":"demo123"}' | jq
```

## 📝 Screen Recording Checklist

- [ ] Run `./setup-demo-mode.sh` to ensure accounts are ready
- [ ] Run `./test-demo-accounts.sh` to verify all accounts work
- [ ] Clear browser cache/cookies
- [ ] Close unnecessary browser tabs
- [ ] Prepare demo script with timing
- [ ] Check audio/video recording settings
- [ ] Have login credentials visible on second monitor
- [ ] Test screen recording software first

## 🎯 Demo Scenarios by Audience

### For Hospital CIOs/CTOs
**Use:** `demo.admin`
- Show system architecture
- Highlight integration capabilities
- Display security features
- Show scalability metrics

### For Chief Medical Officers
**Use:** `demo.doctor`
- Patient care workflows
- Clinical decision support
- Quality measure tracking
- Care gap identification

### For Quality Directors
**Use:** `demo.quality`
- CMS measure reporting
- Performance dashboards
- Improvement trends
- Benchmark comparisons

### For Care Management Teams
**Use:** `demo.care`
- Care gap dashboard
- Patient outreach tools
- Intervention tracking
- Outcome measurements

### For Board Members/Executives
**Use:** `demo.viewer`
- High-level metrics
- ROI visualization
- Outcome improvements
- Strategic value proposition

## 🔐 Security Note

**These are DEMO accounts only!**
- ✗ Never use in production
- ✗ Never expose in public demos
- ✗ Never include in documentation sent to clients
- ✓ Use only in controlled demo environments
- ✓ Reset/remove before production deployment

## 📊 Demo Data

Demo accounts work with:
- ✓ Existing patient data in database
- ✓ Quality measures (CMS134, CMS2, etc.)
- ✓ Care gap calculations
- ✓ All backend services

## 🎬 Recording Workflow

```bash
# 1. Prepare environment
docker compose up -d
./setup-demo-mode.sh

# 2. Verify everything works
./test-demo-accounts.sh

# 3. Start recording
# ... record your demo ...

# 4. Cleanup (optional)
# Demo accounts persist until you reset them
```

## 💡 Pro Tips

1. **Keep it simple**: Use memorable usernames (demo.doctor, demo.quality)
2. **Use same password**: All accounts use `demo123` for easy memory
3. **Test first**: Always run test script before recording
4. **Multiple takes**: Demo accounts persist, so you can record multiple times
5. **Clear tokens**: Clear browser storage between takes for fresh logins
6. **Script it**: Write a demo script with exact timing and talking points

## 🆘 Troubleshooting

### "Login failed" error
```bash
# Check Gateway is running
docker compose ps gateway-service

# Check Gateway logs
docker compose logs gateway-service

# Restart Gateway
docker compose restart gateway-service
```

### "Account not found" error
```bash
# Re-run setup
./setup-demo-mode.sh

# Verify accounts in database
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT username, first_name, last_name FROM users WHERE username LIKE 'demo.%';"
```

### Token expires during recording
- Access tokens last 15 minutes
- If recording longer demos, refresh token or re-login
- Or increase JWT expiration in application-prod.yml for demo environment

---

**Ready to record?** 🎥  
Run: `./setup-demo-mode.sh && ./test-demo-accounts.sh`

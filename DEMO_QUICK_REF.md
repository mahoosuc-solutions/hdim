# 🎬 Demo Mode - Quick Reference Card

## ✅ Status: READY FOR VIDEO RECORDING

All 5 demo accounts are created, tested, and working!

---

## 🔐 Login Credentials

**All passwords: `demo123`**

| Username        | Name              | Role      | Use Case                          |
|-----------------|-------------------|-----------|-----------------------------------|
| `demo.doctor`   | Dr. Sarah Chen    | EVALUATOR | Clinical workflows, evaluations   |
| `demo.analyst`  | Michael Rodriguez | ANALYST   | Data analysis, reporting          |
| `demo.care`     | Jennifer Thompson | EVALUATOR | Care gap evaluation               |
| `demo.admin`    | David Johnson     | ADMIN     | Full system access                |
| `demo.viewer`   | Emily Martinez    | VIEWER    | Read-only, stakeholder view       |

---

## 🚀 Quick Commands

```bash
# Setup (already done!)
./create-demo-users-v2.sh

# Test all accounts
./test-demo-accounts.sh

# Manual login test
curl -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.doctor","password":"demo123"}' | jq
```

---

## 📹 For Your Video

### Quick Login (show in terminal)
```bash
curl -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.doctor","password":"demo123"}'
```

### Web Login (show in browser)
```
URL:      http://localhost:4200
Username: demo.doctor
Password: demo123
```

---

## ✅ Verified Working

- ✅ All 5 users created in database
- ✅ BCrypt password hashes working
- ✅ All roles assigned correctly
- ✅ Tenant assignments working (demo-clinic)
- ✅ JWT tokens generating successfully
- ✅ Login tested for all accounts
- ✅ Email verified flag set to true
- ✅ All accounts active

---

## 🎯 Demo Scenarios

### Scenario 1: Clinical Workflow (demo.doctor)
1. Login as Dr. Sarah Chen
2. View patient dashboard
3. Check quality measures
4. Show care gaps

### Scenario 2: Analytics Dashboard (demo.analyst)
1. Login as Michael Rodriguez
2. View data analytics
3. Generate reports
4. Show metrics

### Scenario 3: Care Management (demo.care)
1. Login as Jennifer Thompson
2. Review care gaps
3. Evaluate patient needs
4. Track interventions

### Scenario 4: Admin Features (demo.admin)
1. Login as David Johnson
2. Show system configuration
3. User management
4. Full access demo

### Scenario 5: Stakeholder View (demo.viewer)
1. Login as Emily Martinez
2. High-level dashboard
3. Read-only metrics
4. Executive summary

---

## 🔄 Reset Demo Users

If you need to recreate:
```bash
./create-demo-users-v2.sh
```

This will:
- Delete existing demo.* users
- Generate new BCrypt hash
- Create fresh accounts
- Test login automatically

---

## 📊 Database Verification

```bash
# Check users exist
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT username, first_name, last_name, active FROM users WHERE username LIKE 'demo.%';"

# Check roles
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT u.username, r.role FROM users u JOIN user_roles r ON u.id = r.user_id WHERE u.username LIKE 'demo.%';"
```

---

## 🎥 Recording Checklist

- [x] Demo accounts created
- [x] All logins tested
- [x] Database persistence verified
- [x] JWT tokens working
- [ ] Clear browser cache before recording
- [ ] Close unnecessary tabs
- [ ] Prepare demo script
- [ ] Test screen recording software
- [ ] Check audio levels
- [ ] Have this quick reference visible

---

**Ready to record! 🎬**

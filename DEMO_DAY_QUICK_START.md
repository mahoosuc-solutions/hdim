# 🚀 DEMO DAY QUICK START

## ✅ System Ready!

Everything is configured and loaded for your full demo today.

---

## 🎯 Quick Demo Commands

### Option 1: Automated Interactive Demo (Recommended)
```bash
./demo-full-system.sh
```
**10-part interactive demonstration with pause points**

### Option 2: Manual API Testing
```bash
# Login
curl -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.doctor","password":"demo123"}' | jq

# Get quality measures (use token from login)
curl http://localhost:9000/api/quality/quality-measure/results \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: demo-clinic" | jq
```

### Option 3: Quick System Test
```bash
./test-demo-accounts.sh        # Test all 5 user accounts
./test-e2e-all-roles.sh        # Full validation (40 tests)
```

---

## 📊 Demo Data Loaded

✅ **8 Quality Measure Results**
- CMS2: Depression Screening (67% compliance)
- CMS134: Diabetes Nephropathy (50% compliance)
- CMS165: Blood Pressure Control (50% compliance)
- CMS122: HbA1c Control

✅ **5 Care Gaps Identified**
- 3 HIGH priority (depression screening, diabetes, HbA1c)
- 1 MEDIUM priority (blood pressure)
- 1 LOW priority (flu vaccination)

✅ **5 Demo Patients**
- Mix of compliant and non-compliant
- Realistic clinical scenarios

✅ **5 Demo Users** (password: demo123)
- demo.doctor (EVALUATOR)
- demo.analyst (ANALYST)
- demo.care (EVALUATOR)
- demo.admin (ADMIN)
- demo.viewer (VIEWER)

---

## 🎬 Demo Flow Suggestion (15 minutes)

### Part 1: Introduction (2 min)
- Show system architecture diagram
- Explain microservices approach
- Highlight 38% improvement statistic

### Part 2: Authentication (2 min)
- Login as demo.doctor
- Show JWT token structure
- Demonstrate role-based access

### Part 3: Quality Measures (3 min)
- View quality measure results
- Show aggregate quality score
- Highlight compliance rates

### Part 4: Care Gaps (3 min)
- Display identified care gaps
- Explain priority system
- Show clinical recommendations

### Part 5: CQL Engine (2 min)
- Show CQL libraries
- Explain FHIR compliance
- Demonstrate value sets

### Part 6: User Roles (2 min)
- Switch between user accounts
- Show different permissions
- Demonstrate access control

### Part 7: Q&A (1 min)
- Address questions
- Discuss integration
- Next steps

---

## 🔥 Key Talking Points

1. **Clinical Outcome**
   > "38% improvement in depression remission rates through systematic screening"

2. **Automation**
   > "Automated CMS quality measure calculation saves 20+ hours per month"

3. **Care Gaps**
   > "Real-time identification of care gaps prevents missed interventions"

4. **Integration**
   > "FHIR-compliant, integrates with any EMR system"

5. **Security**
   > "Enterprise-grade security with JWT authentication and role-based access"

6. **Scalability**
   > "Microservices architecture scales to millions of patients"

---

## 📱 URLs to Have Open

```
Gateway:          http://localhost:9000
Health Check:     http://localhost:9000/actuator/health
CQL Engine:       http://localhost:8081/actuator/health
Quality Measure:  http://localhost:8087/actuator/health
```

---

## 🧪 Pre-Demo Checklist

- [ ] Run: `docker compose ps` (verify all services UP)
- [ ] Run: `./test-demo-accounts.sh` (verify all 5 logins work)
- [ ] Open browser to http://localhost:4200 (if frontend ready)
- [ ] Have DEMO_CHEAT_SHEET.txt visible on second monitor
- [ ] Close unnecessary tabs and windows
- [ ] Test screen sharing/recording
- [ ] Have demo script ready: `./demo-full-system.sh`
- [ ] Review talking points above

---

## 🆘 Quick Troubleshooting

### If services aren't running:
```bash
docker compose up -d
sleep 30  # Wait for services to start
```

### If demo data is missing:
```bash
./load-demo-clinical-data.sh
```

### If authentication fails:
```bash
./create-demo-users-v2.sh
```

### Check logs:
```bash
docker compose logs gateway-service --tail=50
docker compose logs quality-measure-service --tail=50
```

---

## 📊 Live Demo Data Queries

### Show quality measure breakdown:
```bash
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql -c \
  "SELECT measure_id, measure_name, 
   COUNT(*) as patients,
   SUM(CASE WHEN numerator_compliant THEN 1 ELSE 0 END) as compliant
   FROM quality_measure_results 
   WHERE tenant_id = 'demo-clinic' 
   GROUP BY measure_id, measure_name;"
```

### Show care gap summary:
```bash
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql -c \
  "SELECT category, priority, COUNT(*) 
   FROM care_gaps 
   WHERE tenant_id = 'demo-clinic' 
   GROUP BY category, priority 
   ORDER BY priority;"
```

---

## 🎯 Post-Demo Actions

1. **Capture recording** for follow-up emails
2. **Note questions** that came up
3. **Document** specific integration requirements discussed
4. **Schedule** technical deep-dive if needed
5. **Send** personalized follow-up with:
   - Demo recording
   - Case study PDF
   - Technical architecture doc
   - Pricing proposal

---

## 📧 Follow-Up Email Template

```
Subject: Health Data in Motion Demo - [Organization Name]

Hi [Name],

Thank you for attending today's demo! Here are the key highlights:

✅ 38% improvement in depression remission rates
✅ Automated CMS quality measure tracking
✅ Real-time care gap identification
✅ FHIR-compliant integration

Attached:
- Demo recording (15 minutes)
- Technical architecture document
- Case study: Depression screening program
- ROI calculator

Next steps:
1. Review materials
2. Schedule technical deep-dive (45 min)
3. Discuss integration requirements
4. Plan pilot deployment

Looking forward to helping improve patient outcomes at [Organization]!

Best regards,
[Your Name]
```

---

**You're ready! Run `./demo-full-system.sh` to begin! 🚀**

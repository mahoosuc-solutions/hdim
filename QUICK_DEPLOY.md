# Quick Deploy - HealthData In Motion

**⏱️ Time**: 30 minutes total
**💰 Cost**: $60/month (Railway + Vercel)

---

## 🚀 Deploy in 5 Steps

### Step 1: Deploy Backend (Railway) - 15 min

```bash
# Install CLI
npm install -g @railway/cli

# Login
railway login

# Initialize project
railway init

# Add database
railway add postgresql

# Deploy service
cd backend/modules/services/quality-measure-service
railway up

# Get URL
railway status
# Copy: quality-measure-service-production.up.railway.app
```

---

### Step 2: Deploy Frontend (Vercel) - 10 min

```bash
# Install CLI
npm install -g vercel

# Login
vercel login

# Deploy
vercel

# Set environment variables
vercel env add API_GATEWAY_URL production
# Enter: https://quality-measure-service-production.up.railway.app

vercel env add QUALITY_MEASURE_URL production
# Enter: https://quality-measure-service-production.up.railway.app

# Deploy to production
vercel --prod
```

---

### Step 3: Test Backend - 2 min

```bash
# Health check
curl https://quality-measure-service-production.up.railway.app/actuator/health

# Expected: {"status":"UP"}
```

---

### Step 4: Test Frontend - 2 min

```bash
# Open in browser
open https://your-project.vercel.app

# Navigate to Patient Health Overview
# Try submitting PHQ-9 assessment
```

---

### Step 5: Verify Integration - 1 min

```bash
# Open browser DevTools → Network
# Submit PHQ-9 assessment
# Verify API calls go to Railway backend
# Check for 200 OK responses
```

---

## ✅ Done!

Your app is now live at:
- **Frontend**: https://your-project.vercel.app
- **Backend**: https://quality-measure-service-production.up.railway.app

---

## 🔧 Common Issues

### Backend health check fails
```bash
# Check Railway logs
railway logs

# Common issue: Database not connected
# Fix: Verify DATABASE_URL environment variable
```

### Frontend shows "localhost" errors
```bash
# Environment variables not set
# Fix: Run vercel env commands again
vercel env add API_GATEWAY_URL production
```

### CORS errors
```bash
# Backend needs to allow your Vercel domain
# Add to QualityMeasureSecurityConfig.java:
configuration.setAllowedOrigins(Arrays.asList(
    "https://your-project.vercel.app",
    "https://*.vercel.app"
));
```

---

## 📚 Full Documentation

- [Complete Deployment Guide](DEPLOYMENT_COMPLETE_SUMMARY.md)
- [Vercel Details](VERCEL_DEPLOYMENT_GUIDE.md)
- [Backend Options](BACKEND_DEPLOYMENT_OPTIONS.md)

---

## 💰 Costs

| Service | Monthly Cost |
|---------|--------------|
| Railway (Backend + DB) | $40 |
| Vercel (Frontend) | $20 |
| **Total** | **$60** |

---

## 📞 Help

- Railway: https://railway.app/discord
- Vercel: https://vercel.com/discord
- Docs: See guides above

---

**Ready?** Run `railway login` to start!

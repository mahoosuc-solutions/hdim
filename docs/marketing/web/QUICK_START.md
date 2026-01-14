# Quick Start Guide - AI Solutioning Journey Web Content

## 🚀 Get Started in 5 Minutes

### 1. View Locally
```bash
cd docs/marketing/web
open ai-solutioning-index.html
```
Or just double-click `ai-solutioning-index.html` in your file browser.

### 2. Deploy (Choose One)

#### Option A: Vercel (Easiest)
```bash
cd docs/marketing/web
vercel --prod
```

#### Option B: GitHub Pages
1. Go to repository Settings → Pages
2. Source: `master` branch, folder: `docs/marketing/web`
3. Done! Site will be at `https://your-username.github.io/hdim/`

#### Option C: Netlify
1. Drag `docs/marketing/web` folder to https://app.netlify.com/drop
2. Done!

### 3. Update Configuration (After Deployment)

1. **Update BASE_URL** in `convert-to-html-enhanced.py`:
   ```python
   BASE_URL = "https://your-actual-domain.com"
   ```

2. **Update sitemap.xml** with your domain

3. **Configure Google Analytics** (optional):
   - Replace `GA_MEASUREMENT_ID` in HTML files with your GA4 ID

### 4. Regenerate HTML (If Needed)
```bash
cd docs/marketing/web
python3 convert-to-html-enhanced.py
```

---

## 📋 What's Included

- ✅ 9 HTML pages (index + 7 documents + executive summary)
- ✅ Social sharing buttons
- ✅ PDF download support
- ✅ Analytics ready
- ✅ SEO optimized
- ✅ Fully responsive
- ✅ Deployment configs

---

## 📚 Documentation

- **README.md** - Full usage guide
- **DEPLOYMENT.md** - Detailed deployment instructions
- **NEXT_STEPS.md** - Enhancement ideas
- **CONTENT_CREATION_COMPLETE.md** - Complete deliverables list

---

## 🎯 Key Files

- `ai-solutioning-index.html` - Start here!
- `executive-summary.html` - Quick overview
- `convert-to-html-enhanced.py` - Regeneration script
- `vercel.json` - Vercel config
- `sitemap.xml` - SEO sitemap

---

**Ready to deploy!** 🎉

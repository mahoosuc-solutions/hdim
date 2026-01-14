# Vercel Deployment Guide

## Quick Deploy

### Option 1: Using Deployment Script (Recommended)
```bash
cd docs/marketing/web
./deploy-vercel.sh
```

### Option 2: Using Vercel CLI
```bash
cd docs/marketing/web
vercel --prod
```

### Option 3: Using Vercel Dashboard
1. Go to https://vercel.com
2. Click "Add New Project"
3. Import your GitHub repository
4. Configure:
   - **Root Directory:** `docs/marketing/web`
   - **Framework Preset:** Other
   - **Build Command:** (leave empty)
   - **Output Directory:** `.` (current directory)
5. Click "Deploy"

---

## Prerequisites

### Install Vercel CLI (if needed)
```bash
npm install -g vercel
```

### Login to Vercel
```bash
vercel login
```

---

## Configuration

The `vercel.json` file is already configured with:
- Static file serving
- Security headers
- Root redirect to `ai-solutioning-index.html`

### Custom Domain Setup

After deployment:

1. **Get your Vercel URL:**
   - Check deployment output or Vercel dashboard
   - Format: `https://your-project.vercel.app`

2. **Update BASE_URL:**
   ```bash
   # Edit convert-to-html-enhanced.py
   BASE_URL = "https://your-project.vercel.app"
   ```

3. **Regenerate HTML files:**
   ```bash
   python3 convert-to-html-enhanced.py
   ```

4. **Redeploy:**
   ```bash
   vercel --prod
   ```

5. **Add Custom Domain (Optional):**
   - Go to Vercel Dashboard → Project Settings → Domains
   - Add your custom domain
   - Follow DNS configuration instructions

---

## Post-Deployment Checklist

- [ ] Deployment successful
- [ ] Site accessible at Vercel URL
- [ ] All pages load correctly
- [ ] Navigation works
- [ ] Social sharing works
- [ ] PDF download works
- [ ] Update BASE_URL in conversion script
- [ ] Update sitemap.xml with actual domain
- [ ] Regenerate HTML files with correct URLs
- [ ] Configure Google Analytics ID (if using)
- [ ] Test on mobile devices
- [ ] Add custom domain (optional)

---

## Troubleshooting

### Deployment Fails
- Check that all HTML files are in the directory
- Verify `vercel.json` is valid JSON
- Check Vercel CLI version: `vercel --version`

### Links Not Working
- Verify BASE_URL is correct
- Check that relative paths are correct
- Ensure all files are deployed

### Social Sharing Not Working
- Verify Open Graph meta tags are present
- Check that `og:url` matches actual URL
- Test with Facebook Debugger: https://developers.facebook.com/tools/debug/

### Analytics Not Tracking
- Verify Google Analytics ID is configured
- Check browser console for errors
- Ensure ad blockers aren't blocking GA

---

## Environment Variables

If you need to set environment variables (e.g., for analytics):

1. Go to Vercel Dashboard → Project Settings → Environment Variables
2. Add variables:
   - `GA_MEASUREMENT_ID` = `G-XXXXXXXXXX`
3. Update HTML files to use environment variable (requires build step)

Or manually replace `GA_MEASUREMENT_ID` in HTML files with your actual ID.

---

## Continuous Deployment

Vercel automatically deploys when you push to your connected branch:

1. Connect GitHub repository in Vercel dashboard
2. Set root directory to `docs/marketing/web`
3. Every push to `master` branch will auto-deploy

---

## Rollback

If you need to rollback:

1. Go to Vercel Dashboard → Deployments
2. Find previous deployment
3. Click "..." → "Promote to Production"

---

*Last Updated: January 14, 2026*

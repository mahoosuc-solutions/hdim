# Deployment Guide - AI Solutioning Journey Web Content

## Quick Start

The web content is ready to deploy. Choose your preferred hosting option below.

---

## Option 1: Vercel (Recommended)

### Prerequisites
- Vercel account (free tier available)
- GitHub repository connected

### Steps

1. **Install Vercel CLI** (optional, can use web interface):
```bash
npm i -g vercel
```

2. **Deploy from web/ directory**:
```bash
cd docs/marketing/web
vercel --prod
```

3. **Or use Vercel Dashboard**:
   - Go to https://vercel.com
   - Click "New Project"
   - Import your GitHub repository
   - Set root directory to `docs/marketing/web`
   - Deploy

### Configuration
- `vercel.json` is already configured
- Custom domain can be added in Vercel dashboard
- Environment variables not needed for static site

### Update BASE_URL
After deployment, update `BASE_URL` in `convert-to-html-enhanced.py`:
```python
BASE_URL = "https://your-vercel-domain.vercel.app"
```

Then regenerate HTML files:
```bash
python3 convert-to-html-enhanced.py
```

---

## Option 2: GitHub Pages

### Prerequisites
- GitHub repository
- GitHub Pages enabled

### Steps

1. **Enable GitHub Pages**:
   - Go to repository Settings → Pages
   - Source: Deploy from a branch
   - Branch: `master` (or `gh-pages`)
   - Folder: `/docs/marketing/web`

2. **Or use GitHub Actions** (already configured):
   - The workflow file `.github/workflows/deploy-pages.yml` is ready
   - Push to master branch
   - GitHub Actions will deploy automatically

3. **Access your site**:
   - URL: `https://your-username.github.io/hdim/ai-solutioning/`
   - Or custom domain if configured

### Update BASE_URL
After deployment, update `BASE_URL` in `convert-to-html-enhanced.py`:
```python
BASE_URL = "https://your-username.github.io/hdim/ai-solutioning"
```

---

## Option 3: Netlify

### Prerequisites
- Netlify account (free tier available)

### Steps

1. **Deploy via Netlify Dashboard**:
   - Go to https://app.netlify.com
   - Click "Add new site" → "Import an existing project"
   - Connect GitHub repository
   - Build settings:
     - Base directory: `docs/marketing/web`
     - Build command: (leave empty, static site)
     - Publish directory: `docs/marketing/web`

2. **Or use Netlify CLI**:
```bash
npm i -g netlify-cli
cd docs/marketing/web
netlify deploy --prod
```

### Configuration
Create `netlify.toml`:
```toml
[build]
  publish = "docs/marketing/web"
  base = "docs/marketing/web"

[[redirects]]
  from = "/*"
  to = "/index.html"
  status = 200
```

---

## Option 4: Any Web Server

### Steps

1. **Upload files**:
   - Upload all files from `docs/marketing/web/` to your web server
   - Maintain directory structure

2. **Configure web server**:
   - Ensure `.html` files are served correctly
   - Set up redirects if needed:
     - `/` → `/ai-solutioning-index.html`

3. **Update BASE_URL**:
   - Update `BASE_URL` in `convert-to-html-enhanced.py`
   - Regenerate HTML files if needed

---

## Post-Deployment Checklist

- [ ] Update `BASE_URL` in `convert-to-html-enhanced.py`
- [ ] Regenerate HTML files with correct URLs
- [ ] Update `sitemap.xml` with actual domain
- [ ] Test all links
- [ ] Verify social sharing works
- [ ] Check Google Analytics (if configured)
- [ ] Test PDF download functionality
- [ ] Verify responsive design on mobile
- [ ] Check SEO meta tags

---

## Custom Domain Setup

### Vercel
1. Go to Project Settings → Domains
2. Add your custom domain
3. Follow DNS configuration instructions

### GitHub Pages
1. Go to repository Settings → Pages
2. Add custom domain
3. Configure DNS records (CNAME or A records)

### Netlify
1. Go to Site Settings → Domain Management
2. Add custom domain
3. Configure DNS as instructed

---

## Environment Variables

### Google Analytics
If using Google Analytics, update the tracking ID:

1. Get your GA4 Measurement ID from Google Analytics
2. Replace `GA_MEASUREMENT_ID` in HTML files:
   - Search for `GA_MEASUREMENT_ID` in all HTML files
   - Replace with your actual ID (e.g., `G-XXXXXXXXXX`)

Or use environment variable in deployment platform:
- Vercel: Add `GA_MEASUREMENT_ID` in project settings
- Netlify: Add in Site Settings → Environment Variables

---

## Troubleshooting

### Links Not Working
- Check that `BASE_URL` is correct
- Verify relative paths are correct
- Ensure all files are uploaded

### Social Sharing Not Working
- Verify Open Graph meta tags are present
- Check that `og:url` matches actual URL
- Test with Facebook Debugger: https://developers.facebook.com/tools/debug/

### PDF Download Not Working
- Ensure browser supports print-to-PDF
- Check that print CSS is included
- Test on different browsers

### Analytics Not Tracking
- Verify Google Analytics ID is correct
- Check browser console for errors
- Ensure ad blockers aren't blocking GA

---

## Maintenance

### Updating Content
1. Edit markdown files in `docs/marketing/`
2. Run conversion script:
   ```bash
   cd docs/marketing/web
   python3 convert-to-html-enhanced.py
   ```
3. Commit and push changes
4. Deployment happens automatically (if using CI/CD)

### Adding New Documents
1. Add document to `DOCUMENTS` mapping in `convert-to-html-enhanced.py`
2. Add to `ai-solutioning-index.html` documents grid
3. Update `sitemap.xml`
4. Regenerate HTML files

---

## Support

For issues or questions:
- Check `README.md` for usage instructions
- Review `NEXT_STEPS.md` for enhancement ideas
- Check deployment platform documentation

---

*Last Updated: January 14, 2026*

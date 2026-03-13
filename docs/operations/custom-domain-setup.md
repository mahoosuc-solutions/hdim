# Custom Domain Setup — healthdatainmotion.com

**Status:** DNS configuration required (manual step)
**Date:** March 13, 2026

## Current State

- **Domain:** healthdatainmotion.com (registered at Namecheap)
- **Nameservers:** dns1.registrar-servers.com, dns2.registrar-servers.com
- **Current A Record:** 216.150.1.1 (Namecheap parking page)
- **Vercel Project:** `hdim-himss` (prj_zxJr3GxK2jSZGa0QOZf0FbaukJns) under `mahooosuc-solutions` team
- **Vercel URL:** landing-page-ecru-five-65.vercel.app
- **Issue:** Domain already assigned to another Vercel project (likely `prj_UJqXYIX3lzypVOK1nlPYijSCDvkq`)

## Steps to Complete

### Step 1: Resolve Vercel Project Assignment

Go to [Vercel Dashboard](https://vercel.com/dashboard) → find the project that owns `healthdatainmotion.com`:

1. Check **Settings → Domains** on both projects:
   - `hdim-himss` (current landing page)
   - The original `landing-page` project (prj_UJqXYIX3lzypVOK1nlPYijSCDvkq)
2. Remove the domain from the old project if it's there
3. Add it to `hdim-himss`:
   ```bash
   vercel domains add healthdatainmotion.com --project hdim-himss
   ```

### Step 2: Configure DNS at Namecheap

Once Vercel accepts the domain, set these records in [Namecheap DNS](https://ap.www.namecheap.com/domains/dns):

| Type | Host | Value | TTL |
|------|------|-------|-----|
| A | @ | 76.76.21.21 | Auto |
| CNAME | www | cname.vercel-dns.com | Auto |

Delete the existing parking page A record (216.150.1.1) first.

### Step 3: Verify

```bash
# DNS propagation (may take 5-30 minutes)
dig healthdatainmotion.com A +short
# Expected: 76.76.21.21

dig www.healthdatainmotion.com CNAME +short
# Expected: cname.vercel-dns.com

# HTTPS verification
curl -sI https://healthdatainmotion.com | head -5
# Expected: HTTP/2 200, served by Vercel
```

### Step 4: Update Canonical References

After DNS is live, update:
- Landing page `<head>` canonical URL
- `sitemap.xml` — base URL
- `robots.txt` — sitemap URL
- Google Search Console — add property
- LinkedIn company page — website URL

### Additional: Email (Optional)

If setting up email at healthdatainmotion.com:
- Add MX records for your email provider
- Add SPF TXT record: `v=spf1 include:<provider> ~all`
- Add DKIM TXT record from provider

## Estimated Time

- Vercel reassignment: 5 minutes (dashboard)
- DNS changes: 5 minutes (Namecheap)
- DNS propagation: 5-30 minutes (wait)
- Verification: 5 minutes
- **Total: ~15-45 minutes** (mostly waiting for propagation)

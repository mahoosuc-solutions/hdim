# healthdatainmotion.com Domain Migration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Serve the HDIM landing page at `https://healthdatainmotion.com` and `https://www.healthdatainmotion.com` with valid SSL, correct redirects, and updated metadata/SEO.

**Architecture:** The domain is already registered and present in Vercel, but currently assigned to the wrong project (`technical-product-site`). Nameservers are at Namecheap (not Vercel), so DNS changes must be made at Namecheap. We reassign the domain to `hdim-landing-page`, point DNS correctly, update all hardcoded URLs in the Next.js app to the new domain, update reCAPTCHA and Search Console, and verify end-to-end.

**Tech Stack:** Vercel CLI, Namecheap DNS (A + CNAME records), Next.js 16 (metadata, sitemap, robots), Google reCAPTCHA v3 admin, Google Search Console.

---

## Current State (as of 2026-02-18)

| Item | Current value |
|------|--------------|
| Landing page URL | `https://hdim-landing-page.vercel.app` |
| Domain in Vercel | `healthdatainmotion.com` → assigned to `technical-product-site` (wrong project) |
| Nameservers | Namecheap (`registrar-servers.com`) — NOT Vercel DNS |
| `www` A record | `76.76.21.21` (Vercel's anycast IP, already set) |
| reCAPTCHA allowed domains | `hdim-landing-page.vercel.app` only |
| `metadataBase` in layout.tsx | `https://hdim-landing-page.vercel.app` |
| Sitemap base URL | `hdim-landing-page.vercel.app` |

## What Needs To Happen

1. **Vercel CLI:** Remove domain from `technical-product-site`, add to `hdim-landing-page`
2. **Namecheap DNS:** Add/confirm A record (`@` → `76.76.21.21`) + CNAME (`www` → `cname.vercel-dns.com`)
3. **Code:** Update `metadataBase`, `sitemap.ts`, `robots.ts`, and any hardcoded URLs to `healthdatainmotion.com`
4. **reCAPTCHA admin:** Add `healthdatainmotion.com` as allowed domain
5. **Search Console:** Add new property for `healthdatainmotion.com`, submit sitemap
6. **Verify:** SSL, redirects, forms, GA4, reCAPTCHA all working on new domain

---

## Task 1: Reassign Domain in Vercel

**Files:** None (CLI only)

**Context:** `healthdatainmotion.com` is currently assigned to `technical-product-site`. We must remove it from that project and add it to `hdim-landing-page`. Both `healthdatainmotion.com` (apex) and `www.healthdatainmotion.com` need to be added.

**Step 1: Remove domain from wrong project**

```bash
vercel domains rm healthdatainmotion.com --project technical-product-site
```

Expected output: `> Domain healthdatainmotion.com removed from technical-product-site`

If that fails (project name mismatch), list projects first:
```bash
vercel project ls
```

**Step 2: Add apex domain to hdim-landing-page**

Run from `landing-page-v0/` directory:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/landing-page-v0
vercel domains add healthdatainmotion.com
```

Expected: Vercel will show required DNS records to add (A record for apex, CNAME for www).

**Step 3: Add www subdomain**

```bash
vercel domains add www.healthdatainmotion.com
```

**Step 4: Verify both are assigned**

```bash
vercel domains inspect healthdatainmotion.com
```

Expected: `Projects` section shows `hdim-landing-page`.

**Step 5: Commit** — no code changes in this task, nothing to commit.

---

## Task 2: Update DNS at Namecheap

**Files:** None (Namecheap dashboard — manual)

**Context:** Nameservers are `dns1.registrar-servers.com` / `dns2.registrar-servers.com` (Namecheap). We do NOT switch to Vercel nameservers (that would break existing SPF/Google verification TXT records). Instead we add A + CNAME records directly in Namecheap's Advanced DNS panel.

**Step 1: Log in to Namecheap**

Go to https://ap.www.namecheap.com → Domain List → `healthdatainmotion.com` → Manage → Advanced DNS

**Step 2: Add/confirm A record for apex**

| Type | Host | Value | TTL |
|------|------|-------|-----|
| A Record | `@` | `76.76.21.21` | Automatic |

> Note: `www.healthdatainmotion.com` already resolves to `76.76.21.21` — check if the apex (`@`) also has this record. If not, add it.

**Step 3: Add CNAME for www**

| Type | Host | Value | TTL |
|------|------|-------|-----|
| CNAME Record | `www` | `cname.vercel-dns.com` | Automatic |

> Vercel prefers a CNAME for `www` pointing to `cname.vercel-dns.com` rather than an A record, for proper edge routing.

**Step 4: Verify DNS propagation** (wait 5-15 minutes, then):

```bash
dig healthdatainmotion.com A +short
# Expected: 76.76.21.21

dig www.healthdatainmotion.com CNAME +short
# Expected: cname.vercel-dns.com.
```

**Step 5: Verify SSL is provisioned by Vercel**

```bash
curl -I https://healthdatainmotion.com
# Expected: HTTP/2 200 (or 308 redirect to www)

curl -I https://www.healthdatainmotion.com
# Expected: HTTP/2 200
```

---

## Task 3: Update Hardcoded URLs in Next.js App

**Files:**
- Modify: `landing-page-v0/app/layout.tsx` (line 18 — `metadataBase`)
- Modify: `landing-page-v0/app/sitemap.ts` (if exists — base URL)
- Modify: `landing-page-v0/app/robots.ts` (if exists — sitemap URL)

**Step 1: Check what files reference the old domain**

```bash
grep -r "hdim-landing-page.vercel.app" landing-page-v0/app/ --include="*.ts" --include="*.tsx" -l
```

**Step 2: Update `metadataBase` in layout.tsx**

In `landing-page-v0/app/layout.tsx`, change:
```tsx
// Before
metadataBase: new URL('https://hdim-landing-page.vercel.app'),

// After
metadataBase: new URL('https://www.healthdatainmotion.com'),
```

Also update the `openGraph.url`:
```tsx
// Before
url: 'https://hdim-landing-page.vercel.app',

// After
url: 'https://www.healthdatainmotion.com',
```

**Step 3: Update sitemap.ts if it exists**

```bash
cat landing-page-v0/app/sitemap.ts 2>/dev/null || echo "no sitemap.ts"
```

If it has hardcoded URLs, replace `hdim-landing-page.vercel.app` with `www.healthdatainmotion.com`.

**Step 4: Update robots.ts if it exists**

```bash
cat landing-page-v0/app/robots.ts 2>/dev/null || echo "no robots.ts"
```

If it has a `host` or sitemap URL pointing to the old domain, update it.

**Step 5: Build locally to verify no TypeScript errors**

```bash
cd landing-page-v0 && npm run build
```

Expected: `✓ Compiled successfully`

**Step 6: Commit**

```bash
git add landing-page-v0/app/layout.tsx landing-page-v0/app/sitemap.ts landing-page-v0/app/robots.ts
git commit -m "feat(landing-page): update canonical domain to healthdatainmotion.com"
```

---

## Task 4: Configure www → apex Redirect (or apex → www)

**Files:**
- Modify: `landing-page-v0/vercel.json`

**Context:** Vercel needs to know the canonical domain. Standard practice: redirect apex (`healthdatainmotion.com`) to `www`, OR redirect `www` to apex. Either works — pick one and be consistent with `metadataBase`.

Recommended: **`www` is canonical** (easier for Vercel SSL + CDN routing).

**Step 1: Add redirect in vercel.json**

In `landing-page-v0/vercel.json`, add to the `"redirects"` array (create it if missing):

```json
{
  "redirects": [
    {
      "source": "/(.*)",
      "has": [{ "type": "host", "value": "healthdatainmotion.com" }],
      "destination": "https://www.healthdatainmotion.com/$1",
      "permanent": true
    }
  ]
}
```

**Step 2: Verify redirect logic doesn't conflict with existing rewrites**

Check existing `"rewrites"` in `vercel.json` — ensure `/demo` and `/calculator` rewrites still work after adding redirects.

**Step 3: Deploy**

```bash
cd landing-page-v0 && vercel --prod
```

**Step 4: Test redirect**

```bash
curl -I https://healthdatainmotion.com
# Expected: HTTP/2 308 (or 301) Location: https://www.healthdatainmotion.com/

curl -I https://www.healthdatainmotion.com
# Expected: HTTP/2 200
```

**Step 5: Commit**

```bash
git add landing-page-v0/vercel.json
git commit -m "feat(landing-page): add apex→www redirect for healthdatainmotion.com"
```

---

## Task 5: Update reCAPTCHA Allowed Domains

**Files:** None (Google reCAPTCHA admin console — manual)

**Context:** reCAPTCHA v3 keys are scoped to specific domains. The current keys only allow `hdim-landing-page.vercel.app`. Adding the new domain prevents `reCAPTCHA token invalid` errors in production.

**Step 1: Go to reCAPTCHA admin**

https://www.google.com/recaptcha/admin → select the HDIM site key (`6LfeKnAs...`)

**Step 2: Add new domain**

Under "Domains" → Add:
- `healthdatainmotion.com`
- `www.healthdatainmotion.com`

Keep `hdim-landing-page.vercel.app` — useful for staging/testing.

**Step 3: Save**

**Step 4: Verify** — submit a form at `https://www.healthdatainmotion.com/contact` and check Vercel Logs for `recaptchaScore: "(verified)"` (not rejected).

---

## Task 6: Update Google Analytics 4 Property

**Files:** None (GA4 admin console — manual)

**Context:** GA4 properties accept data from any domain by default, but the property should list authorized domains for clarity.

**Step 1: Go to GA4**

https://analytics.google.com → Admin → Property → Data Streams → Web stream

**Step 2: Verify stream URL**

Check the stream URL is (or add) `https://www.healthdatainmotion.com`. The `G-GY8EY3LBYJ` measurement ID is already wired into the code — no code change needed.

**Step 3: (Optional) Update stream URL** if it still says `hdim-landing-page.vercel.app`.

---

## Task 7: Google Search Console — New Property

**Files:** None (Search Console — manual)

**Context:** Search Console properties are domain-specific. The existing property (if any) was for `hdim-landing-page.vercel.app`. We need a new property for `healthdatainmotion.com`.

**Step 1: Add property**

https://search.google.com/search-console → Add property → **Domain** type → `healthdatainmotion.com`

Domain-type verification uses a DNS TXT record.

**Step 2: Verify ownership via DNS TXT**

Search Console will give a TXT record value like `google-site-verification=XXXX`.

Add it in Namecheap → Advanced DNS:
| Type | Host | Value |
|------|------|-------|
| TXT Record | `@` | `google-site-verification=XXXX` |

Note: `healthdatainmotion.com` already has a Google verification TXT — it may already be verified. Check first.

**Step 3: Submit sitemap**

Sitemaps → Submit: `https://www.healthdatainmotion.com/sitemap.xml`

---

## Task 8: End-to-End Verification

**Step 1: DNS**
```bash
dig healthdatainmotion.com A +short      # → 76.76.21.21
dig www.healthdatainmotion.com A +short  # → 76.76.21.21 (via Vercel anycast)
```

**Step 2: SSL**
```bash
curl -vI https://www.healthdatainmotion.com 2>&1 | grep -E "SSL|issuer|HTTP"
# Expected: valid cert, HTTP/2 200
```

**Step 3: Apex redirect**
```bash
curl -IL https://healthdatainmotion.com 2>&1 | grep -E "HTTP|Location"
# Expected: 308 → https://www.healthdatainmotion.com/ → 200
```

**Step 4: Bot protection**
```bash
curl -X POST https://www.healthdatainmotion.com/api/leads \
  -H "Content-Type: application/json" \
  -d '{"source":"demo_modal","firstName":"Bot","email":"bot@spam.com","company":"Spam"}'
# Expected: {"error":"Spam detected"}
```

**Step 5: Real form submission**

- Open `https://www.healthdatainmotion.com`
- Submit demo request form with real data
- Verify: email arrives at `sales@mahoosuc.solutions` under "Sales Leads" label
- Verify: GA4 Realtime shows `generate_lead` event
- Verify: Vercel Logs show `recaptchaScore: "(verified)"`

**Step 6: Open Graph / social preview**

Check https://www.opengraph.xyz and enter `https://www.healthdatainmotion.com` — verify title, description, and image resolve correctly.

---

## Task 9: Final Commit + Push

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git add landing-page-v0/
git commit -m "feat(landing-page): migrate canonical domain to healthdatainmotion.com

- metadataBase → https://www.healthdatainmotion.com
- openGraph.url → https://www.healthdatainmotion.com
- sitemap/robots base URLs updated
- apex → www redirect in vercel.json
- reCAPTCHA + Search Console updated (manual steps in plan)"
git push origin master
```

---

## Summary: What Requires Manual Action (No CLI)

| Step | Where | What |
|------|-------|------|
| DNS records | Namecheap Advanced DNS | A record `@` → `76.76.21.21`, CNAME `www` → `cname.vercel-dns.com` |
| reCAPTCHA domains | Google reCAPTCHA admin | Add `healthdatainmotion.com` + `www.healthdatainmotion.com` |
| GA4 stream URL | Google Analytics admin | Update or add stream for `www.healthdatainmotion.com` |
| Search Console | Google Search Console | Add domain property, verify DNS TXT, submit sitemap |

## Summary: What Is Automated (CLI + Code)

| Step | How |
|------|-----|
| Reassign domain in Vercel | `vercel domains add/rm` CLI |
| `metadataBase` + OG URL | Edit `layout.tsx` |
| Sitemap/robots base URL | Edit `sitemap.ts` / `robots.ts` |
| Apex → www redirect | Edit `vercel.json` |
| Deploy | `vercel --prod` |

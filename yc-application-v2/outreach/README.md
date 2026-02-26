# Investor Outreach Automation

This module builds a review-first investor outreach workflow:

1. Enrich target firms with contact/team URLs.
2. Generate recipient candidates and validate domain MX.
3. Generate personalized email drafts for review.
4. Optionally create Gmail drafts (not sent).

## Quick Start

```bash
cd yc-application-v2/outreach
node scripts/enrich-investor-sites.mjs
node scripts/validate-email-candidates.mjs
node scripts/generate-email-drafts.mjs
```

Draft outputs:
- `drafts/investor-drafts.json`
- `drafts/*.md`
- `AI_ARCHITECT_WORKFLOW.md` (narrative + command flow)

## Optional: Create Gmail drafts in aaron@mahoosuc.solutions

1. Obtain OAuth access token for Gmail API scope: `https://www.googleapis.com/auth/gmail.compose`
2. Export token:

```bash
export GMAIL_ACCESS_TOKEN="<token>"
```

3. Create drafts from reviewed JSON:

```bash
node scripts/create-gmail-drafts.mjs
```

Notes:
- Script creates Gmail drafts only, never sends.
- Add/approve recipients in `drafts/investor-drafts.json` before creating drafts.
- Use this with your account context (`aaron@mahoosuc.solutions`) when generating OAuth token.

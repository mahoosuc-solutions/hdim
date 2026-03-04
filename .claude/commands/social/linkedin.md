# /social:linkedin — Human-in-the-Loop LinkedIn Publisher

Publish a LinkedIn post from HDIM context. Drafts with AI, requires your approval before publishing.

## Usage

```
/social:linkedin --topic "your topic" [--mode thought-leadership|product-announce|customer-milestone] [--tag v1.2-rhel7]
```

**Arguments:**
- `--topic` (required) — What to post about. Primary signal for the draft.
- `--mode` (optional, default: `thought-leadership`) — Controls voice and rules.
- `--tag` (optional) — Git tag for enrichment context (e.g., `v1.2-rhel7`).

---

## Step 1: Validate environment

Check `.env.linkedin` exists and token is valid:

```bash
if [ ! -f .env.linkedin ]; then
  echo "ERROR: .env.linkedin not found."
  echo "Run: scripts/linkedin-auth.sh"
  exit 1
fi

source .env.linkedin

TODAY=$(date +%Y-%m-%d)
if [[ "$LINKEDIN_TOKEN_EXPIRY" < "$TODAY" ]]; then
  echo "Token expired ($LINKEDIN_TOKEN_EXPIRY). Attempting refresh..."
  # Refresh using refresh token
  REFRESH_RESPONSE=$(curl -s -X POST https://www.linkedin.com/oauth/v2/accessToken \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=refresh_token&refresh_token=${LINKEDIN_REFRESH_TOKEN}&client_id=${LINKEDIN_CLIENT_ID}&client_secret=${LINKEDIN_CLIENT_SECRET}")

  NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('access_token',''))")
  EXPIRES_IN=$(echo "$REFRESH_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('expires_in',0))")

  if [ -z "$NEW_TOKEN" ]; then
    echo "ERROR: Refresh token also expired or invalid."
    echo "Re-run: scripts/linkedin-auth.sh"
    echo "LinkedIn tokens expire every 60 days and require re-authorization."
    exit 1
  fi

  # Calculate new expiry (expires_in is in seconds)
  NEW_EXPIRY=$(date -d "+${EXPIRES_IN} seconds" +%Y-%m-%d 2>/dev/null || date -v "+${EXPIRES_IN}S" +%Y-%m-%d)
  sed -i "s/LINKEDIN_ACCESS_TOKEN=.*/LINKEDIN_ACCESS_TOKEN=${NEW_TOKEN}/" .env.linkedin
  sed -i "s/LINKEDIN_TOKEN_EXPIRY=.*/LINKEDIN_TOKEN_EXPIRY=${NEW_EXPIRY}/" .env.linkedin
  echo "Token refreshed. Valid until: $NEW_EXPIRY"
  LINKEDIN_ACCESS_TOKEN="$NEW_TOKEN"
fi

echo "LinkedIn token valid until: $LINKEDIN_TOKEN_EXPIRY"
```

---

## Step 2: Parse arguments

Extract `--topic`, `--mode`, and `--tag` from the command arguments. Defaults:
- `MODE=thought-leadership`
- `TAG=""` (enrichment only, not required)

---

## Step 3: Read last 5 posts for dedup check

Read `docs/outreach/linkedin-posts.md`. Extract the last 5 rows from the tracking table. Note the topics and themes to avoid repetition in the draft. If the file doesn't exist yet, skip this step.

Format a brief summary: "Last post: N days ago on [topic]" or "No prior posts found."

---

## Step 4: Draft the post

Apply mode-specific rules below. Generate ONE draft (not two). Keep it in the LinkedIn voice: no em-dashes, no bullet lists unless quoting data, no hollow phrases like "excited to share" or "thrilled to announce."

### Mode rules

**`thought-leadership`** (default)
- Voice: practitioner insight, no HDIM product mentions, no pitch
- Length: 150–220 words
- Structure: 1 opening observation, 2–3 insight sentences, 1 closing provocation or question
- No CTAs ("DM me", "link in bio")
- Examples of good opening lines:
  - "Most quality measure failures aren't technical problems."
  - "The gap between HEDIS reporting and actual care delivery is wider than most payers realize."

**`product-announce`**
- Voice: founder, specific proof points, technical substance
- Length: 120–180 words
- Structure: what was built (1 sentence), why it matters (2 sentences), specific detail (1–2 sentences), implicit CTA
- No buzzwords: "game-changing", "revolutionary", "AI-powered" (unless AI is genuinely the feature)
- Include the git tag or version if `--tag` was provided
- Technical facts over marketing claims

**`customer-milestone`**
- Voice: relationship-aware, restrained, focused on the problem solved
- Length: 100–160 words
- Do NOT name the customer unless `--topic` explicitly includes permission (e.g., "Mahoosuc Regional Health approved mention")
- Focus on the problem solved and outcome, not the relationship or the win
- No "proud to partner" language

### Dedup check
If the last post was on a similar topic (within last 7 days), note this in the draft header: `[NOTE: Similar topic posted N days ago — consider differentiating angle]`

---

## Step 5: Approval loop

Display the draft in this exact terminal format:

```
─────────────────────────────────────────────────
LINKEDIN DRAFT  [MODE]  tag: TAG_OR_NONE
─────────────────────────────────────────────────
[draft text]
─────────────────────────────────────────────────
Word count: NNN  |  Last post: N days ago (TOPIC or "none")
[A]pprove  [E]dit  [R]egenerate  [X]Reject
>
```

Wait for input:

- **[A]pprove** → proceed to Step 6 (publish)
- **[E]dit** → open `$EDITOR` (fallback: `nano`) with draft text in a temp file. After editor closes, re-display the edited text and ask "Publish this? [Y/N]". If Y → publish. If N → return to approval loop.
- **[R]egenerate** → generate a new draft using a different angle or opening line. Re-display the approval loop.
- **[X]Reject** → print "Post discarded." and exit.

---

## Step 6: Publish to LinkedIn

Source `.env.linkedin` and execute using the **Posts API** (`/rest/posts`):

```bash
source .env.linkedin

POST_TEXT="$APPROVED_DRAFT"

# LinkedIn Posts API (replaces deprecated ugcPosts API)
# Requires: LinkedIn-Version header, author as urn:li:person:{id}
# .env.linkedin must contain LINKEDIN_PERSON_ID (numeric ID, not URN)
AUTHOR_URN="urn:li:person:${LINKEDIN_PERSON_ID}"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST https://api.linkedin.com/rest/posts \
  -H "Authorization: Bearer $LINKEDIN_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -H "LinkedIn-Version: 202401" \
  -H "X-Restli-Protocol-Version: 2.0.0" \
  -d "{
    \"author\": \"${AUTHOR_URN}\",
    \"commentary\": $(echo "$POST_TEXT" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read()))'),
    \"visibility\": \"PUBLIC\",
    \"distribution\": {
      \"feedDistribution\": \"MAIN_FEED\",
      \"targetEntities\": [],
      \"thirdPartyDistributionChannels\": []
    },
    \"lifecycleState\": \"PUBLISHED\",
    \"isReshareDisabledByAuthor\": false
  }")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "201" ]; then
  # Posts API returns the URN in the x-restli-id header or response body
  POST_URN=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id', d.get('urn', '')))" 2>/dev/null)
  if [ -z "$POST_URN" ]; then
    # Fallback: extract from Location header if body is empty (201 with header-only response)
    POST_URN="(check LinkedIn activity feed for published post)"
  fi
  echo "✓ Published! URN: $POST_URN"
  POST_URL="https://www.linkedin.com/feed/update/${POST_URN}/"
else
  echo "ERROR publishing (HTTP $HTTP_CODE):"
  echo "$BODY"
  exit 1
fi
```

---

## Step 7: Log to tracking file

Append a row to `docs/outreach/linkedin-posts.md`.

If the file doesn't exist, create it with this header first:

```markdown
# LinkedIn Posts Tracking

Logs all published posts. Phase 1: impressions/comments filled manually.
Phase 2 (planned): nightly sync via `scripts/linkedin-engagement-sync.sh`.

| Date | Tag/Topic | Mode | Post URL | Impressions | Comments | Notes |
|------|-----------|------|----------|-------------|----------|-------|
```

Then append:

```
| YYYY-MM-DD | TAG_OR_TOPIC | MODE | POST_URL | — | — | |
```

Print confirmation: `Logged to docs/outreach/linkedin-posts.md`

---

## Error handling

| Condition | Behavior |
|-----------|----------|
| `.env.linkedin` missing | Print setup instructions, exit |
| Token expired, refresh succeeds | Update `.env.linkedin`, continue |
| Token expired, refresh fails | Print re-auth instructions, exit |
| LinkedIn API returns non-201 | Print error body, exit (no log entry) |
| `$EDITOR` not set | Default to `nano` |
| `docs/outreach/` dir missing | Create it before writing log |

---

## Notes

- **API Version:** This skill uses the LinkedIn **Posts API** (`/rest/posts`) which replaced the deprecated UGC Posts API (`/v2/ugcPosts`) in 2024. The `LinkedIn-Version: 202401` header is required.
- **OAuth Scope:** The Posts API requires the `w_member_social` OAuth scope. Ensure this scope was granted during `scripts/linkedin-auth.sh`.
- **Scheduling Limitation:** LinkedIn's API does **not** support scheduled publishing for personal profiles. There is no `scheduledPublishAt` field. To schedule posts, use one of:
  1. LinkedIn's native post scheduler (compose → clock icon → set date/time)
  2. A local cron job that calls this command at the desired time
  3. Manual copy-paste from draft files at the target time
- **`.env.linkedin` required fields:** `LINKEDIN_ACCESS_TOKEN`, `LINKEDIN_PERSON_ID` (numeric, e.g., `A1b2C3d4E5`), `LINKEDIN_TOKEN_EXPIRY`, `LINKEDIN_REFRESH_TOKEN`, `LINKEDIN_CLIENT_ID`, `LINKEDIN_CLIENT_SECRET`
- Phase 2 engagement sync (`scripts/linkedin-engagement-sync.sh`) is out of scope for this command. Fill Impressions and Comments columns manually until Phase 2 is built.
- Posts cannot be edited via API after publishing. Reject and re-run if corrections needed.

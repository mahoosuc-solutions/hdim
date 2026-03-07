# HIMSS Open-Source Launch Punch List

**Entity:** Grateful House Incorporated (C-Corp, ROBS-funded)
**Product:** HealthData-in-Motion (HDIM)
**Domain:** healthdatainmotion.com (product) | gratefulhouse.com/io/dev (corporate TBD)
**Target:** HIMSS 2026 (March 10, Las Vegas)
**Created:** 2026-03-07

---

## Execution Timeline

### DAY 1 — Friday March 7 (TODAY)
**Theme: Legal foundation + security audit + ROBS protection**

| ID | Task | Owner | Track | Priority | Status |
|----|------|-------|-------|----------|--------|
| 1 | Draft BSL 1.1 LICENSE file (Grateful House Inc. as licensor) | Claude | Legal | P0-BLOCKER | [x] |
| 4 | Draft IP Assignment Agreement template (personal -> C-Corp) | Claude | Legal | P0-BLOCKER | [x] |
| 5 | Install gitleaks + run full 1,947-commit history scan | Claude | Security | P0-BLOCKER | [x] |
| 6 | Create SECURITY.md (responsible disclosure policy) | Claude | Security | P1 | [x] |
| 7 | Audit .env.example files for safe defaults | Claude | Security | P1 | [x] |
| 21 | Document open-source business rationale (ROBS protection) | Claude | ROBS | P0-BLOCKER | [x] |
| 18 | Set up Grateful House email addresses | Aaron | Infra | P1 | [ ] |
| 19 | Register gratefulhouse domain | Aaron | Infra | P1 | [ ] |
| 20 | Identify ROBS-experienced attorney | Aaron | ROBS | P0-BLOCKER | [ ] |

**Day 1 Decision Gate:** If gitleaks finds secrets in history, we STOP and decide:
- Option A: BFG Repo-Cleaner on a copy (preserves history minus secrets)
- Option B: Clean fork with squashed/curated history (fresh start)
- Option C: Secrets are only in .env files already gitignored (proceed)

---

### DAY 2 — Saturday March 8
**Theme: Repository preparation + commercial framework + pitch**

| ID | Task | Owner | Track | Blocked By | Status |
|----|------|-------|-------|------------|--------|
| 2 | Add copyright headers on key files (minimum launch scope) | Claude | Legal | #1 | [x] |
| 3 | Create NOTICE file (third-party attributions) | Claude | Legal | #1 | [x] |
| 8 | Create GitHub Organization for Grateful House | Aaron | Repo | #1 | [ ] |
| 9 | Rewrite README.md for public/open-source audience | Claude | Repo | #1 | [x] |
| 10 | Update CONTRIBUTING.md for external contributors + CLA | Claude | Repo | — | [x] |
| 11 | Create CODE_OF_CONDUCT.md (Contributor Covenant v2.1) | Claude | Repo | — | [x] |
| 12 | Create GitHub issue/PR templates | Claude | Repo | — | [x] |
| 13 | Draft commercial license terms outline | Claude | Commercial | — | [x] |
| 15 | Prepare elevator pitch (15s / 30s / 60s) | Claude | HIMSS | — | [x] |

---

### DAY 3 — Sunday March 9 (Day before HIMSS)
**Theme: Launch assets + go/no-go gate**

| ID | Task | Owner | Track | Blocked By | Status |
|----|------|-------|-------|------------|--------|
| 14 | Create pricing page content for healthdatainmotion.com | Claude | Commercial | #13 | [x] |
| 16 | Draft LinkedIn launch announcement post | Claude | HIMSS | #8 | [x] |
| 17 | Create one-page digital leave-behind (PDF) | Claude | HIMSS | — | [x] |

---

### DAY 3 — GO/NO-GO GATE (Sunday evening)

Before ANY public action, ALL of these must be true:

- [x] **LICENSE file committed** (BSL 1.1, Grateful House Inc.)
- [x] **Gitleaks scan CLEAN** (zero secrets in history)
- [x] **SECURITY.md committed** (responsible disclosure)
- [x] **NOTICE file committed** (third-party attributions)
- [x] **Copyright headers on key files** (at minimum: root files + build files)
- [x] **IP Assignment drafted** (doesn't need to be executed yet, but must exist)
- [x] **ROBS business rationale documented** (IRS audit protection)
- [x] **README rewritten** for public audience
- [ ] **Attorney identified** (doesn't need to be retained yet, but name + contact)
- [ ] **Email addresses live** (at least security@ and info@)

### LAUNCH SEQUENCE (post-gate, timing TBD)

**Option A: Launch AT HIMSS (March 10-12)**
- Flip repo to public during keynote/peak traffic
- Post LinkedIn announcement
- Share in FHIR community (chat.fhir.org)
- Have leave-behind ready for conversations

**Option B: Launch AFTER HIMSS (March 13-14)**
- Use HIMSS conversations to refine messaging
- Launch with more polish
- Less dramatic but lower risk

**Recommendation: Option A — launch during HIMSS Day 1 (March 10)**
- Maximum visibility (40,000+ attendees, massive social media activity)
- "We just went open source" is a conversation starter
- The GitHub link IS the demo — people can run it immediately

---

## Critical Path (Dependency Chain)

```
LICENSE (1) ──┬──> Copyright Headers (2)
              ├──> NOTICE (3)
              ├──> GitHub Org (8) ──> LinkedIn Post (16)
              └──> Public README (9)

Gitleaks (5) ──> GO/NO-GO GATE

Commercial Terms (13) ──> Pricing Page (14)

IP Assignment (4) ─┐
ROBS Rationale (21) ├──> Attorney Review (20)
LICENSE (1) ────────┘
```

---

## What We Are NOT Doing Before HIMSS

These are important but not launch-blocking:

- [ ] CLA infrastructure (can use a simple "I agree" checkbox in PR template initially)
- [ ] Custom domain on GitHub org (can add later)
- [ ] Automated CI/CD on public repo (can mirror later)
- [ ] Marketing website redesign (current Vercel landing page is fine)
- [ ] Pricing payment processing (conversations first, Stripe/invoicing later)
- [ ] PGP key for security reports (email is sufficient to start)

---

## ROBS-Specific Safeguards

1. **IP Assignment MUST happen** before revenue or public launch
2. **Business rationale document** protects against IRS "hobby" classification
3. **Commercial license revenue** is the primary justification for the ROBS structure
4. **Annual stock valuation** — open-source traction (stars, forks, contributors) increases company value, benefiting the retirement plan
5. **Reasonable compensation** — factor into pricing once revenue starts
6. **Board minutes** — document the decision to open-source as a board resolution (even if you're the sole director)

---

## Contact Information (To Be Finalized)

| Purpose | Current | Target |
|---------|---------|--------|
| General info | info@mahoosuc.solutions | info@gratefulhouse.com |
| Sales | sales@mahoosuc.solutions | sales@gratefulhouse.com |
| Security | (none) | security@gratefulhouse.com |
| Legal | (none) | legal@gratefulhouse.com |
| Product site | healthdatainmotion.com | healthdatainmotion.com (no change) |
| Corporate site | (none) | gratefulhouse.com |
| GitHub (private) | webemo-aaron/hdim | webemo-aaron/hdim (stays private) |
| GitHub (public) | (none) | grateful-house/hdim |

---

## Risk Register

| Risk | Impact | Mitigation |
|------|--------|------------|
| Secrets in git history | CRITICAL — public exposure of credentials | Gitleaks scan (Task #5) BLOCKS launch |
| IP not assigned to C-Corp | HIGH — ROBS compliance issue | IP Assignment template (Task #4) + attorney (Task #20) |
| BSL 1.1 wording error | MEDIUM — unintended license grant | Attorney review post-launch; BSL is well-precedented |
| No CLA for contributors | LOW — IP ambiguity on contributions | PR template checkbox for now; formal CLA later |
| Competitor forks code | LOW — BSL prevents production use without license | BSL 1.1 explicitly covers this; 4-year change date |
| IRS ROBS audit | MEDIUM — must show legitimate business purpose | Business rationale doc (Task #21) |

---

*This document is the single source of truth for the HIMSS launch. Update status here as tasks complete.*

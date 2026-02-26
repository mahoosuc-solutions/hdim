# Investor Outreach Dashboard — Design

**Date:** 2026-02-25
**Status:** Approved
**Location:** `tools/investor-dashboard/`

## Purpose

Internal web server for managing investor outreach: pipeline dashboard, contact management, form-based email composition with copy-to-clipboard, and partnership tracking. Polished enough to demo to investors/advisors.

## Architecture

- **Runtime:** Node/Express, server-rendered EJS templates
- **Database:** SQLite (better-sqlite3) for contacts, pipeline, activity log
- **UI:** Bootstrap 5, Chart.js for pipeline funnel
- **Data seeding:** Parse existing markdown docs on first run

## Data Model

### contacts
`id`, `name`, `organization`, `type` (VC/Angel/Strategic), `tier` (1/2/3), `role`, `check_size`, `intro_path`, `portfolio_fit`, `notes`, `status` (Research/Intro Requested/Intro Sent/Meeting Scheduled/First Meeting/Partner Meeting/Diligence/Term Sheet/Passed), `last_contact_date`, `next_action`, `next_action_date`

### templates
`id`, `name`, `category` (Customer/Investor/Follow-up/LOI), `subject`, `body` (with `{{name}}`, `{{organization}}` placeholders)

### activity_log
`id`, `contact_id`, `action` (email_sent/call/meeting/note), `details`, `created_at`

### partnerships
`id`, `company`, `category`, `customer_base`, `partnership_angle`, `status`, `notes`

## Pages

| Route | Purpose |
|-------|---------|
| `/` | Dashboard — funnel, follow-ups due, recent activity |
| `/contacts` | Filterable contact list |
| `/contacts/:id` | Contact detail + activity history |
| `/compose` | Pick contact + template, fill fields, copy email |
| `/templates` | Browse/edit templates |
| `/partnerships` | Partnership targets with status |

## Compose Flow

1. Select contact (or arrive from contact detail)
2. Select template category → template
3. Auto-fill `{{name}}`, `{{organization}}`, `{{fund}}` from contact
4. User fills remaining placeholders
5. Preview final email
6. Copy subject + body to clipboard
7. Optionally log activity

## Data Seeding

Parse on first run:
- `investor-target-list.md` → 50 contacts
- `angel-outreach-list.md` → 15 contacts
- `outreach-templates.md` → ~10 templates
- `healthtech-partnership-outreach.md` → partners

## Project Structure

```
tools/investor-dashboard/
├── server.js
├── package.json
├── db/
│   ├── schema.sql
│   └── seed.js
├── views/
│   ├── layout.ejs
│   ├── dashboard.ejs
│   ├── contacts.ejs
│   ├── contact-detail.ejs
│   ├── compose.ejs
│   ├── templates.ejs
│   └── partnerships.ejs
└── public/
    ├── css/style.css
    └── js/app.js
```

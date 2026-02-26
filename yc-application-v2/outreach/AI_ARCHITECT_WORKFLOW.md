# AI Architect Workflow: End-to-End Investor Outreach by Prompting

This workflow demonstrates how an AI Architect can direct a full sequence of work with clear language and constraints.

## Objective

Build an outreach system that:
- expands investor contact sites,
- creates recipient candidates,
- validates domains,
- generates personalized drafts,
- stores drafts for human review,
- optionally creates Gmail drafts.

## Command Sequence

```bash
cd yc-application-v2/outreach
npm run run:all
```

Outputs:
- `data/investor-firms.enriched.json`
- `data/investor-firms.enriched.csv`
- `data/investor-email-validation.json`
- `drafts/investor-drafts.json`
- `drafts/*.md`

## Human-in-the-loop review step

1. Open `drafts/investor-drafts.json`.
2. Update `toEmail` where needed.
3. Set `reviewStatus` to `approved` for drafts you want created in Gmail.

## Optional Gmail draft creation

```bash
export GMAIL_ACCESS_TOKEN="<oauth_access_token_with_gmail.compose_scope>"
npm run gmail:drafts
```

Result:
- Creates drafts in Gmail account context for approved entries only.
- Never sends emails.

## Why this is AI-Architect grade

- The workflow is reproducible and auditable.
- Every automated step writes reviewable artifacts.
- Approval gates prevent accidental outbound messaging.
- It scales from 10 targets to hundreds with the same commands.

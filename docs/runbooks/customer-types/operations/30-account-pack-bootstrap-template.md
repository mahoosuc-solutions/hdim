# Account Pack Bootstrap Template

Purpose: quickly instantiate a new Week 1 execution pack for any MA/ACO/IPA customer.

## New Pack Naming Convention

`<account-slug>-week1-execution-pack`

Examples:
- `houston-ma-week1-execution-pack`
- `metro-aco-week1-execution-pack`
- `valley-ipa-week1-execution-pack`

## Bootstrap Procedure

1. Copy source pack:
- Source: `houston-ma-week1-execution-pack/`
- Destination: `<new-account-slug>-week1-execution-pack/`

2. Update core identity fields in all files:
- Customer name
- Customer type
- Date fields
- Owner names and contacts

3. Reinitialize operational files:
- `04-action-log.md`
- `05-risk-register.md`
- `11-decision-log.md`
- `18-open-items-resolution-log.md`
- `24-go-no-go-dashboard-week1.md`

4. Validate links and references.

## Minimum Deliverables Before First Send

- [ ] Owner roster completed
- [ ] Gate status snapshot completed
- [ ] Workshop invite and pre-read templates updated
- [ ] Day 1 send set reviewed
- [ ] Command-center checklist ready

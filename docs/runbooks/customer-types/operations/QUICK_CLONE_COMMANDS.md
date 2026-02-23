# Quick Clone Commands

Use this to duplicate an account pack quickly.

```bash
# Example: clone Houston MA pack to new IPA account
cp -R docs/runbooks/customer-types/operations/houston-ma-week1-execution-pack \
  docs/runbooks/customer-types/operations/valley-ipa-week1-execution-pack

# Then globally replace account name markers manually and reset statuses.
```

Post-clone required actions:

1. Reset owner roster.
2. Reset action/risk/decision/open-item logs.
3. Set workshop date and send artifacts.

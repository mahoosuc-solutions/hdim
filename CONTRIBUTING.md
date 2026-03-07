# Contributing to HDIM

Thanks for contributing to HealthData-in-Motion.

## Before You Start

- Read the [LICENSE](./LICENSE) to understand source-available terms.
- Review our [Code of Conduct](./CODE_OF_CONDUCT.md).
- For security issues, follow [SECURITY.md](./SECURITY.md) and do not file public issues.

## Workflow

1. Fork the repository and create a focused branch.
2. Keep changes scoped to one objective.
3. Add or update tests where relevant.
4. Open a pull request using the PR template.

## Lightweight CLA Acknowledgement

By opening a pull request, you confirm:

- You have the right to submit the contribution.
- You grant Grateful House Incorporated the rights needed to use,
  modify, and relicense your contribution under the project license model.
- You understand this project is source-available under BSL 1.1 and may
  include commercial licensing.

To make this explicit, check the CLA checkbox in the PR template.

## Development Validation

Run validation relevant to your changes.

Examples:

- Backend tests: `cd backend && ./gradlew test`
- Frontend tests: `npm test`
- Docker/local stack: `docker compose --profile core up -d`

## Pull Request Expectations

- Clear summary and rationale
- Risk and rollout notes when applicable
- Evidence of testing
- No unrelated changes bundled in the same PR

## Issue Types

Use issue templates for:

- Bug reports
- Feature requests
- Incomplete feature tracking

## Questions

If you are unsure where to contribute, open a feature request issue with context,
proposed scope, and acceptance criteria.

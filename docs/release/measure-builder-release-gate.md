# Measure Builder Release Gate

Use this checklist before cutting a release that includes `measure-builder` changes.

## Required Commands

Run the consolidated gate:

```bash
npm run release:measure-builder:gate
```

Equivalent explicit sequence:

```bash
npx tsc -p apps/clinical-portal/tsconfig.app.json --noEmit
npm run test:measure-builder:focused
npm run e2e:measure-builder:retry
```

## Required CI Signal

The `Measure Builder Focused CI` workflow must pass on the release branch/PR.

## Manual QA Checklist

1. Navigate to `/measure-builder/new` and create a draft.
2. Open `Edit Details` for an existing measure.
3. Confirm client validation appears after field interaction for invalid values.
4. Confirm `400` field errors render inline in the dialog.
5. Confirm `500` error shows alert banner and `Retry` succeeds.
6. Verify table row updates after successful metadata save.
7. Verify behavior on desktop and mobile viewport widths.

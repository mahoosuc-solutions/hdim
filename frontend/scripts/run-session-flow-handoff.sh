#!/usr/bin/env bash
set -euo pipefail

WITH_E2E=false

for arg in "$@"; do
  case "$arg" in
    --with-e2e)
      WITH_E2E=true
      ;;
    *)
      echo "Unknown option: $arg"
      echo "Usage: ./scripts/run-session-flow-handoff.sh [--with-e2e]"
      exit 1
      ;;
  esac
done

echo "==> Predicting expected CI gate execution"
npm run detect:session-flow-checks

echo "==> Running targeted session/auth suite"
npm run test:session-flow

if [ "$WITH_E2E" = "true" ]; then
  echo "==> Running session flow e2e"
  npm run e2e:session-flow

  echo "==> Running session flow external-auth e2e"
  npm run e2e:session-flow:external-auth

  echo "==> Running auth callback e2e"
  npm run e2e:auth-callback
else
  echo "==> Skipping e2e (pass --with-e2e to include browser validation)"
fi

echo "==> Session flow handoff run complete"

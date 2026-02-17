#!/usr/bin/env bash
set -euo pipefail

python3 - <<'EOF'
import glob
import re
from pathlib import Path

paths = sorted(glob.glob('backend/modules/services/*/Dockerfile'))

issues = []

def last_from_index(lines):
    idx = -1
    for i, line in enumerate(lines):
        if line.strip().startswith('FROM '):
            idx = i
    return idx

def has_token(lines, token):
    return any(token in line for line in lines)

def is_java_dockerfile(lines):
    # Heuristic: require JAVA_OPTS only for Java runtime images.
    # This repo also contains non-Java services (e.g., Python) that should not be forced to carry JAVA_OPTS.
    from_lines = [l.strip() for l in lines if l.strip().startswith('FROM ')]
    if any(re.search(r'\b(temurin|openjdk|corretto|eclipse-temurin|java|jre|jdk)\b', l, re.IGNORECASE) for l in from_lines):
        return True
    if any(re.search(r'^\s*(CMD|ENTRYPOINT)\b.*\bjava\b', l) for l in lines):
        return True
    return False

def uses_wget_healthcheck(lines):
    for i, line in enumerate(lines):
        if line.strip().startswith('HEALTHCHECK'):
            for j in range(i, min(i + 10, len(lines))):
                if 'CMD' in lines[j] and 'wget' in lines[j]:
                    return True
    return False

def installs_wget_before_healthcheck(lines):
    # Consider only the runtime stage before HEALTHCHECK
    end = len(lines)
    for i, line in enumerate(lines):
        if line.strip().startswith('HEALTHCHECK'):
            end = i
            break
    pre = lines[:end]
    # Detect wget in apk add blocks (single-line or multi-line)
    in_apk_block = False
    for line in pre:
        stripped = line.strip()
        if 'apk add' in stripped:
            in_apk_block = True
            if 'wget' in stripped:
                return True
            continue
        if in_apk_block:
            if 'wget' in stripped:
                return True
            if not stripped.endswith('\\'):
                in_apk_block = False
    return False

for p in paths:
    lines = Path(p).read_text().splitlines()
    runtime_start = last_from_index(lines)
    runtime = lines[runtime_start + 1:] if runtime_start >= 0 else lines
    java = is_java_dockerfile(lines)

    if not has_token(runtime, 'USER '):
        issues.append((p, 'missing USER in runtime stage'))
    if not has_token(runtime, 'HEALTHCHECK'):
        issues.append((p, 'missing HEALTHCHECK in runtime stage'))
    if java and not has_token(runtime, 'ENV JAVA_OPTS'):
        issues.append((p, 'missing JAVA_OPTS in runtime stage'))
    if uses_wget_healthcheck(runtime) and not installs_wget_before_healthcheck(runtime):
        issues.append((p, 'uses wget in HEALTHCHECK but does not install wget in runtime stage'))

if issues:
    print('Dockerfile validation failed:\n')
    for path, msg in issues:
        print(f'- {path}: {msg}')
    raise SystemExit(1)

print('Dockerfile validation passed for', len(paths), 'services.')
EOF

#!/usr/bin/env node
/**
 * Nx runner that scopes targets to app/lib projects (and optionally e2e).
 *
 * Rationale:
 * - This repo contains mixed Nx project types (apps/libs + backend services + legacy).
 * - `nx run-many -t <target> --all` is too broad for local CI and may include projects
 *   without the requested target.
 */

import { execFileSync, spawnSync } from 'node:child_process';

function parseArgs(argv) {
  const out = { target: null, exclude: [], includeE2e: false, extra: [] };
  const [target, ...rest] = argv;
  out.target = target ?? null;
  for (const a of rest) {
    if (a === '--include-e2e') {
      out.includeE2e = true;
      continue;
    }
    if (a.startsWith('--exclude=')) {
      const raw = a.slice('--exclude='.length);
      out.exclude = raw
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
      continue;
    }
    out.extra.push(a);
  }
  return out;
}

function runNxCapture(args) {
  return execFileSync('npm', ['run', '-s', 'nx', '--', ...args], {
    encoding: 'utf8',
    maxBuffer: 50 * 1024 * 1024,
  }).trim();
}

function runNxInherit(args) {
  const res = spawnSync('npm', ['run', '-s', 'nx', '--', ...args], { stdio: 'inherit' });
  if (res.status !== 0) {
    process.exit(res.status ?? 1);
  }
}

function showProjectsByPattern(pattern) {
  const raw = runNxCapture(['show', 'projects', '--projects', pattern, '--json']);
  if (!raw) return [];
  return JSON.parse(raw);
}

function uniq(xs) {
  return [...new Set(xs)];
}

function main() {
  const opts = parseArgs(process.argv.slice(2));
  if (!opts.target) {
    console.error('Usage: node scripts/nx/run-many.mjs <target> [--exclude=a,b] [--include-e2e]');
    process.exit(2);
  }

  const projects = uniq([
    ...showProjectsByPattern('apps/*'),
    ...showProjectsByPattern('libs/*'),
    ...(opts.includeE2e ? showProjectsByPattern('apps/*-e2e') : []),
  ]).filter((p) => !opts.exclude.includes(p));

  if (projects.length === 0) {
    console.log(`No projects selected for target "${opts.target}".`);
    return;
  }

  runNxInherit(['run-many', '-t', opts.target, '--projects', projects.join(','), ...opts.extra]);
}

main();

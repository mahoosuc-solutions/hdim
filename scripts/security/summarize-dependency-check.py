#!/usr/bin/env python3
import json
from collections import defaultdict
from pathlib import Path

report = Path('backend/build/reports/dependency-check-report.json')
if not report.exists():
    raise SystemExit(f'Missing report: {report}')

data = json.loads(report.read_text())

by_package = defaultdict(lambda: {"max_cvss": 0.0, "cves": set(), "files": set(), "projects": set(), "count": 0})
critical = []

for dep in data.get('dependencies', []):
    vulns = dep.get('vulnerabilities') or []
    if not vulns:
        continue
    pkg = ''
    pkgs = dep.get('packages') or []
    if pkgs:
        pkg = pkgs[0].get('id') or ''
    if not pkg:
        pkg = dep.get('fileName', 'unknown')

    refs = dep.get('projectReferences') or []
    file_name = dep.get('fileName', 'unknown')

    for v in vulns:
        name = v.get('name', 'UNKNOWN-CVE')
        cvss = (v.get('cvssv3') or {}).get('baseScore')
        if cvss is None:
            cvss = (v.get('cvssv2') or {}).get('score')
        cvss = float(cvss or 0)
        rec = by_package[pkg]
        rec['max_cvss'] = max(rec['max_cvss'], cvss)
        rec['cves'].add(name)
        rec['files'].add(file_name)
        rec['projects'].update(refs)
        rec['count'] += 1
        if cvss >= 9.0:
            critical.append((name, cvss, pkg, file_name, tuple(refs)))

rows = []
for pkg, rec in by_package.items():
    rows.append({
        'package': pkg,
        'max_cvss': rec['max_cvss'],
        'cve_count': len(rec['cves']),
        'occurrences': rec['count'],
        'top_cves': sorted(rec['cves'])[:8],
        'files': sorted(rec['files'])[:4],
        'projects': sorted(rec['projects'])[:8],
    })

rows.sort(key=lambda r: (-r['max_cvss'], -r['cve_count'], r['package']))
critical.sort(key=lambda r: (-r[1], r[0], r[2]))

out = Path('docs/compliance/CVE_REMEDIATION_WAVE1_2026-02-27.md')
out.parent.mkdir(parents=True, exist_ok=True)

with out.open('w') as f:
    f.write('# CVE Remediation Wave 1\n\n')
    f.write('**Date:** 2026-02-27\n')
    f.write('**Source:** `backend/build/reports/dependency-check-report.json`\n')
    f.write('\n')
    f.write('## Summary\n\n')
    f.write(f'- Packages with vulnerabilities: **{len(rows)}**\n')
    f.write(f'- Vulnerability occurrences: **{sum(r["occurrences"] for r in rows)}**\n')
    f.write(f'- Findings with CVSS >= 9.0: **{len(critical)}**\n\n')

    f.write('## Priority Buckets\n\n')
    f.write('### P0 (CVSS >= 9.0)\n\n')
    f.write('| Package | Max CVSS | CVE Count | Example CVEs | Project Refs |\n')
    f.write('|---|---:|---:|---|---|\n')
    for r in rows:
        if r['max_cvss'] < 9.0:
            continue
        cves = ', '.join(r['top_cves'][:4])
        projects = ', '.join(r['projects'][:3])
        f.write(f"| `{r['package']}` | {r['max_cvss']:.1f} | {r['cve_count']} | {cves} | {projects} |\n")
    f.write('\n')

    f.write('### P1 (7.0 <= CVSS < 9.0)\n\n')
    f.write('| Package | Max CVSS | CVE Count | Example CVEs |\n')
    f.write('|---|---:|---:|---|\n')
    shown = 0
    for r in rows:
        if not (7.0 <= r['max_cvss'] < 9.0):
            continue
        cves = ', '.join(r['top_cves'][:4])
        f.write(f"| `{r['package']}` | {r['max_cvss']:.1f} | {r['cve_count']} | {cves} |\n")
        shown += 1
        if shown >= 30:
            break
    f.write('\n')

    f.write('## Recommended Execution Order\n\n')
    f.write('1. Spring/Tomcat stack remediation (critical web stack CVEs).\n')
    f.write('2. Quartz upgrade/removal for schedulers and transitive chains.\n')
    f.write('3. SQLite/Tika/PlantUML tooling dependencies remediation.\n')
    f.write('4. gRPC/Kafka/Netty stream stack updates and regression tests.\n')
    f.write('5. iText and document-processing dependency upgrades.\n\n')

    f.write('## Notes\n\n')
    f.write('- Run `./gradlew dependencyCheckAggregate --no-daemon` after each wave.\n')
    f.write('- Keep `scripts/validation/validate-compliance-evidence-gate.sh` in strict mode for go/no-go.\n')

print(f'Wrote {out}')

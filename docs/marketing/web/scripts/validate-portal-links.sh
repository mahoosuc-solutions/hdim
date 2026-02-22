#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-https://web-gamma-snowy-38.vercel.app}"
OUT_DIR="${OUT_DIR:-docs/marketing/web/evidence}"
AUTH_HEADER_NAME="${AUTH_HEADER_NAME:-}"
AUTH_HEADER_VALUE="${AUTH_HEADER_VALUE:-}"
AUTH_SET_BYPASS_COOKIE="${AUTH_SET_BYPASS_COOKIE:-}"
mkdir -p "$OUT_DIR"

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TS_UTC="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
HOST_SLUG="$(echo "$BASE_URL" | sed -E 's#https?://##; s#[^A-Za-z0-9]+#-#g; s#-+$##')"
CSV_OUT="$OUT_DIR/portal-link-audit-$HOST_SLUG-$STAMP.csv"
MD_OUT="$OUT_DIR/portal-link-audit-$HOST_SLUG-$STAMP.md"

python3 - "$BASE_URL" "$CSV_OUT" "$MD_OUT" "$TS_UTC" "$AUTH_HEADER_NAME" "$AUTH_HEADER_VALUE" "$AUTH_SET_BYPASS_COOKIE" <<'PY'
import csv
import pathlib
import re
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request

base_url = sys.argv[1].rstrip('/')
csv_out = pathlib.Path(sys.argv[2])
md_out = pathlib.Path(sys.argv[3])
ts_utc = sys.argv[4]
auth_header_name = sys.argv[5].strip()
auth_header_value = sys.argv[6].strip()
auth_set_bypass_cookie = sys.argv[7].strip()

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

root = pathlib.Path('docs/marketing/web')
html_files = sorted([p.name for p in root.glob('*.html') if p.name != 'shared-nav.html'])

sitemap_text = (root / 'sitemap.xml').read_text(errors='ignore')
sitemap_urls = re.findall(r'<loc>(.*?)</loc>', sitemap_text)
sitemap_paths = []
for u in sitemap_urls:
    p = urllib.parse.urlparse(u)
    sitemap_paths.append(p.path if p.path else '/')

seed_pages = ['/ai-solutioning-index.html', '/race-track-fhir-pipeline', '/race-track-fhir-evidence.html']

href_re = re.compile(r'href=["\']([^"\']+)["\']', re.I)

internal_links = set()
external_links = set()
source_map = {}

def add_source(target, source):
    source_map.setdefault(target, set()).add(source)

def classify_href(href, source):
    if href.startswith('#') or href.startswith('mailto:') or href.startswith('tel:') or href.startswith('javascript:'):
        return
    absu = urllib.parse.urljoin(f'{base_url}{source}', href)
    p = urllib.parse.urlparse(absu)
    if p.netloc and p.netloc != urllib.parse.urlparse(base_url).netloc:
        external_links.add(absu)
        add_source(absu, source)
        return
    path = p.path or '/'
    if p.query:
        path = f'{path}?{p.query}'
    internal_links.add(path)
    add_source(path, source)

# Parse local key source files for link inventory
for src in ['ai-solutioning-index.html', 'shared-nav.html'] + html_files:
    t = (root / src).read_text(errors='ignore')
    source = '/' + src
    for h in href_re.findall(t):
        classify_href(h, source)

# Ensure sitemap urls are checked even if not linked
for p in sitemap_paths:
    internal_links.add(p)
    add_source(p, '/sitemap.xml')

# Add root and sitemap
internal_links.add('/')
internal_links.add('/sitemap.xml')
add_source('/', '/seed')
add_source('/sitemap.xml', '/seed')

rows = []

request_headers = {'User-Agent': 'Mozilla/5.0'}
if auth_header_name and auth_header_value:
    request_headers[auth_header_name] = auth_header_value
if auth_set_bypass_cookie:
    request_headers['x-vercel-set-bypass-cookie'] = auth_set_bypass_cookie

def fetch_status(url):
    req = urllib.request.Request(url, headers=request_headers)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=25) as r:
            return r.getcode(), r.geturl(), ''
    except urllib.error.HTTPError as e:
        return e.code, url, e.reason
    except Exception as e:
        return 'ERR', url, str(e)

for path in sorted(internal_links):
    url = base_url + path
    code, final_url, note = fetch_status(url)
    rows.append({
        'kind': 'internal',
        'target': path,
        'status': code,
        'final_url': final_url,
        'sources': ';'.join(sorted(source_map.get(path, []))),
        'note': note,
    })

for url in sorted(external_links):
    code, final_url, note = fetch_status(url)
    rows.append({
        'kind': 'external',
        'target': url,
        'status': code,
        'final_url': final_url,
        'sources': ';'.join(sorted(source_map.get(url, []))),
        'note': note,
    })

# Discoverability checks
linked_html = set()
for link in internal_links:
    p = link.split('?', 1)[0]
    if p.startswith('/'):
        p = p[1:]
    if p in html_files:
        linked_html.add(p)

sitemap_html = set()
for path in sitemap_paths:
    p = path[1:] if path.startswith('/') else path
    if p in html_files:
        sitemap_html.add(p)

key_should_be_sitemap = {
    'cms-vision.html',
    'platform-architecture.html',
    'performance-benchmarking.html',
    'vision-deck.html',
    'origin-story.html',
    'ai-solutioning-whitepaper.html',
    'sales-narrative.html',
    'blog-post-ai-solutioning.html',
    'race-track-fhir-pipeline-print.html',
    'race-track-fhir-evidence.html',
    'kill-tony-vision-deck.html',
    'speech-final.html',
    'validation-report.html',
}

missing_from_sitemap = sorted([p for p in key_should_be_sitemap if p not in sitemap_html])

missing_from_links = sorted([p for p in html_files if p not in linked_html and p not in {'race-track-fhir-experiment.html', 'race-track-fhir-pipeline-b.html', 'race-track-fhir-pipeline.html'}])

# Write CSV
with csv_out.open('w', newline='') as f:
    w = csv.DictWriter(f, fieldnames=['kind', 'target', 'status', 'final_url', 'sources', 'note'])
    w.writeheader()
    for r in rows:
        w.writerow(r)

internal_fail = [r for r in rows if r['kind'] == 'internal' and r['status'] != 200]

def is_expected_external_status(row):
    target = row['target']
    status = row['status']
    if target in ('https://fonts.googleapis.com', 'https://fonts.gstatic.com'):
        return True
    if '/issues/' in target and 'github.com/webemo-aaron/hdim/' in target and status in (401, 403, 404):
        return True
    return False

external_fail = [r for r in rows if r['kind'] == 'external' and r['status'] not in (200, 301, 302) and not is_expected_external_status(r)]

summary = []
summary.append('# Portal Deployment Link Audit')
summary.append('')
summary.append(f'- Timestamp (UTC): {ts_utc}')
summary.append(f'- Base URL: {base_url}')
summary.append(f'- Internal links checked: {len([r for r in rows if r["kind"] == "internal"])}')
summary.append(f'- External links checked: {len([r for r in rows if r["kind"] == "external"])}')
summary.append('')
summary.append('## Internal Non-200')
if not internal_fail:
    summary.append('- None')
else:
    for r in internal_fail:
        summary.append(f"- {r['status']} `{r['target']}` (sources: {r['sources'] or 'n/a'})")

summary.append('')
summary.append('## External Non-200/3xx')
if not external_fail:
    summary.append('- None')
else:
    for r in external_fail:
        summary.append(f"- {r['status']} `{r['target']}`")

summary.append('')
summary.append('## Discoverability Gaps')
summary.append(f"- Missing from sitemap (expected set): {', '.join(missing_from_sitemap) if missing_from_sitemap else 'none'}")
summary.append(f"- HTML pages not linked from index/nav/page graph: {', '.join(missing_from_links) if missing_from_links else 'none'}")
summary.append('')
summary.append('## Artifacts')
summary.append(f'- CSV: {csv_out.name}')
summary.append(f'- Report: {md_out.name}')

md_out.write_text('\n'.join(summary) + '\n')
PY

echo "Wrote: $CSV_OUT"
echo "Wrote: $MD_OUT"

# Portal Deployment Link Audit

- Timestamp (UTC): 2026-02-23T00:56:07Z
- Base URL: http://localhost:4174
- Internal links checked: 44
- External links checked: 22

## Internal Non-200
- 404 `/race-track-fhir-pipeline` (sources: /ai-solutioning-index.html;/platform-architecture.html;/shared-nav.html;/sitemap.xml)
- 404 `/race-track-fhir-pipeline?ab=a` (sources: /race-track-fhir-pipeline-b.html;/race-track-fhir-pipeline.html)
- 404 `/race-track-fhir-pipeline?ab=b` (sources: /race-track-fhir-pipeline-b.html;/race-track-fhir-pipeline.html)

## External Non-200/3xx
- 404 `https://github.com/webemo-aaron/hdim/actions/runs/22283112071`
- 404 `https://github.com/webemo-aaron/hdim/actions/runs/22284191276`
- 404 `https://github.com/webemo-aaron/hdim/actions/runs/22286089694`

## Discoverability Gaps
- Missing from sitemap (expected set): none
- HTML pages not linked from index/nav/page graph: none

## Artifacts
- CSV: portal-link-audit-localhost-4174-20260223T005607Z.csv
- Report: portal-link-audit-localhost-4174-20260223T005607Z.md

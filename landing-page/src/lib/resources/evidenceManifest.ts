export type EvidenceDataStatus = 'Observed' | 'Modeled';

export type EvidenceArtifact = {
  key: string;
  label: string;
  href: string;
  dataStatus: EvidenceDataStatus;
  lastUpdated: string;
};

export const evidenceManifest = {
  releaseEvidence: {
    key: 'releaseEvidence',
    label: 'Release Evidence Hub',
    href: '/resources/build-evidence',
    dataStatus: 'Observed',
    lastUpdated: '2026-03-08',
  },
  technicalHub: {
    key: 'technicalHub',
    label: 'Technical Evaluation Hub',
    href: '/resources/technical',
    dataStatus: 'Observed',
    lastUpdated: '2026-03-08',
  },
  executiveHub: {
    key: 'executiveHub',
    label: 'Executive & Compliance Hub',
    href: '/resources/executive',
    dataStatus: 'Observed',
    lastUpdated: '2026-03-08',
  },
  fhirEvidence: {
    key: 'fhirEvidence',
    label: 'FHIR Validation Evidence',
    href: '/resources/fhir-evidence',
    dataStatus: 'Observed',
    lastUpdated: '2026-03-08',
  },
  clinicalHub: {
    key: 'clinicalHub',
    label: 'Clinical Leadership Hub',
    href: '/resources/clinical',
    dataStatus: 'Observed',
    lastUpdated: '2026-03-08',
  },
  executiveSummary: {
    key: 'executiveSummary',
    label: 'Executive Summary',
    href: '/resources/executive-summary',
    dataStatus: 'Modeled',
    lastUpdated: '2026-03-01',
  },
  architectureHub: {
    key: 'architectureHub',
    label: 'Architecture Hub',
    href: '/resources/architecture',
    dataStatus: 'Observed',
    lastUpdated: '2026-03-05',
  },
  architectureEvolution: {
    key: 'architectureEvolution',
    label: 'Architecture Evolution',
    href: '/resources/architecture-evolution',
    dataStatus: 'Observed',
    lastUpdated: '2026-03-05',
  },
  aiMetrics: {
    key: 'aiMetrics',
    label: 'AI Metrics',
    href: '/resources/ai-metrics',
    dataStatus: 'Observed',
    lastUpdated: '2026-03-07',
  },
  performance: {
    key: 'performance',
    label: 'Performance',
    href: '/resources/performance',
    dataStatus: 'Modeled',
    lastUpdated: '2026-02-27',
  },
  aiJourney: {
    key: 'aiJourney',
    label: 'AI Journey',
    href: '/resources/ai-journey',
    dataStatus: 'Modeled',
    lastUpdated: '2026-02-24',
  },
} as const satisfies Record<string, EvidenceArtifact>;

export type EvidenceArtifactKey = keyof typeof evidenceManifest;

function freshnessLabel(lastUpdated: string): string {
  const updatedAt = new Date(`${lastUpdated}T00:00:00Z`);
  const now = new Date();
  const diffMs = now.getTime() - updatedAt.getTime();
  const days = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)));

  if (days <= 1) return 'Updated today';
  if (days <= 7) return `Updated ${days}d ago`;
  if (days <= 30) return `Updated ${Math.ceil(days / 7)}w ago`;
  return `Updated ${Math.ceil(days / 30)}mo ago`;
}

export function getEvidenceArtifact(key: EvidenceArtifactKey): EvidenceArtifact & { freshness: string } {
  const artifact = evidenceManifest[key];
  return {
    ...artifact,
    freshness: freshnessLabel(artifact.lastUpdated),
  };
}

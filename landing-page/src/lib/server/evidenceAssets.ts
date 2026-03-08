export type EvidenceAssetConfig = {
  key: string;
  label: string;
  notes: string;
  envVar: string;
};

const packetAssets: Record<string, EvidenceAssetConfig[]> = {
  security: [
    {
      key: 'soc2-summary',
      label: 'SOC 2 Control Summary (PDF)',
      notes: 'Control mapping and control owner matrix.',
      envVar: 'EVIDENCE_PACKET_SECURITY_SOC2_URL',
    },
    {
      key: 'incident-response',
      label: 'Incident Response Summary (PDF)',
      notes: 'Incident process and escalation timeline.',
      envVar: 'EVIDENCE_PACKET_SECURITY_IR_URL',
    },
  ],
  reliability: [
    {
      key: 'release-scorecard',
      label: 'Release Scorecard Export (PDF)',
      notes: 'Latest go/no-go and validator summary.',
      envVar: 'EVIDENCE_PACKET_RELIABILITY_SCORECARD_URL',
    },
    {
      key: 'validation-logs',
      label: 'Validation Evidence Bundle (ZIP)',
      notes: 'Release evidence logs and artifact index.',
      envVar: 'EVIDENCE_PACKET_RELIABILITY_VALIDATION_URL',
    },
  ],
  procurement: [
    {
      key: 'sow-template',
      label: 'Implementation SOW Template (DOCX)',
      notes: 'Baseline implementation structure and milestones.',
      envVar: 'EVIDENCE_PACKET_PROCUREMENT_SOW_URL',
    },
    {
      key: 'sla-summary',
      label: 'Service Level Summary (PDF)',
      notes: 'Support model, service levels, and escalation terms.',
      envVar: 'EVIDENCE_PACKET_PROCUREMENT_SLA_URL',
    },
  ],
};

export function getPacketAssets(packet: string): Array<EvidenceAssetConfig & { url: string }> {
  const assets = packetAssets[packet] ?? packetAssets.security;
  return assets
    .map((asset) => {
      const url = process.env[asset.envVar];
      if (!url) return null;
      return {
        ...asset,
        url,
      };
    })
    .filter((asset): asset is EvidenceAssetConfig & { url: string } => Boolean(asset));
}

export function getAssetForPacket(packet: string, assetKey: string): (EvidenceAssetConfig & { url: string }) | null {
  const assets = getPacketAssets(packet);
  return assets.find((asset) => asset.key === assetKey) ?? null;
}

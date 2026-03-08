'use client';

import { useEffect } from 'react';
import { trackEvent } from '@/lib/analytics';

type EvidenceAccessTelemetryProps = {
  packet: string;
  role: string;
};

export default function EvidenceAccessTelemetry({ packet, role }: EvidenceAccessTelemetryProps) {
  useEffect(() => {
    trackEvent('evidence_access_opened', { packet, role });
  }, [packet, role]);

  return null;
}

import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Oracle Health (Cerner) Integration | HDIM',
  description:
    'Connect HDIM to Oracle Health Millennium platform via FHIR R4. Enterprise quality measurement with CDS Hooks support.',
  keywords: ['Oracle Health', 'Cerner', 'Millennium', 'CDS Hooks', 'FHIR R4', 'HEDIS measures', 'EHR integration'],
};

export default function OracleHealthLayout({ children }: { children: React.ReactNode }) {
  return children;
}

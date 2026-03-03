import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'InterSystems HealthShare Integration | HDIM',
  description:
    'Connect HDIM to InterSystems HealthShare and IRIS for Health for HEDIS quality measurement at HIE scale. Architected for workloads of 16M+ patients.',
  keywords: [
    'InterSystems HealthShare',
    'IRIS for Health',
    'HIE integration',
    'FHIR R4',
    'HEDIS measures',
    'health information exchange',
  ],
};

export default function InterSystemsLayout({ children }: { children: React.ReactNode }) {
  return children;
}

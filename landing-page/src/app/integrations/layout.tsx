import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'EHR Integrations | HDIM',
  description:
    'HDIM integrates with all major EHR systems via FHIR R4. Connect Epic, Oracle Health, Athenahealth, InterSystems, Meditech, and more.',
  keywords: [
    'EHR integration',
    'FHIR R4',
    'Epic',
    'Cerner',
    'Oracle Health',
    'Athenahealth',
    'InterSystems',
    'Meditech',
    'healthcare interoperability',
  ],
};

export default function IntegrationsLayout({ children }: { children: React.ReactNode }) {
  return children;
}

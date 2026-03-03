import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'NextGen Healthcare Integration | HDIM',
  description:
    'Connect HDIM to NextGen Healthcare via FHIR R4. Quality measurement for specialty and ambulatory practices with Mirth Connect support.',
  keywords: ['NextGen Healthcare', 'Mirth Connect', 'specialty EHR', 'FHIR R4', 'ambulatory', 'HEDIS measures'],
};

export default function NextGenLayout({ children }: { children: React.ReactNode }) {
  return children;
}

import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'eClinicalWorks Integration | HDIM',
  description:
    'Connect HDIM to eClinicalWorks cloud EHR via FHIR R4. Quality measurement for large ambulatory networks and health centers.',
  keywords: ['eClinicalWorks', 'eCW', 'cloud EHR', 'FHIR R4', 'ambulatory', 'HEDIS measures'],
};

export default function EClinicalWorksLayout({ children }: { children: React.ReactNode }) {
  return children;
}

import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Athenahealth Integration | HDIM',
  description:
    'Connect HDIM to Athenahealth via FHIR R4 with simplified OAuth2. Cloud-native quality measurement for modern practices.',
  keywords: ['Athenahealth', 'athenaClinicals', 'cloud EHR', 'FHIR R4', 'HEDIS measures', 'ambulatory EHR'],
};

export default function AthenahealthLayout({ children }: { children: React.ReactNode }) {
  return children;
}

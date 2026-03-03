import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Meditech Integration | HDIM',
  description:
    'Connect HDIM to Meditech Expanse via FHIR R4. Quality measurement optimized for community hospitals and rural health systems.',
  keywords: ['Meditech', 'Meditech Expanse', 'community hospital', 'FHIR R4', 'HEDIS measures', 'rural health'],
};

export default function MeditechLayout({ children }: { children: React.ReactNode }) {
  return children;
}

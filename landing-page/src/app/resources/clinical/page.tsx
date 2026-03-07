import type { Metadata } from 'next';
import ClinicalHub from './ClinicalHub';

export const metadata: Metadata = {
  title: 'Clinical Leadership Hub | HDIM Resources',
  description:
    'Clinical leadership view of care gap automation, quality outcomes, and intervention explainability.',
  openGraph: {
    title: 'HDIM Clinical Leadership Hub',
    description:
      'From fragmented records to prioritized interventions and measurable care-gap outcomes.',
  },
  alternates: {
    canonical: 'https://healthdatainmotion.com/resources/clinical',
  },
};

export default function ClinicalPage() {
  return <ClinicalHub />;
}

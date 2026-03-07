import type { Metadata } from 'next';
import ResourcesHub from './ResourcesHub';

export const metadata: Metadata = {
  title: 'Resources | HDIM - Healthcare Quality Measurement Platform',
  description:
    'HDIM resources for healthcare automation, implementation architecture, and release-grade evidence.',
  openGraph: {
    title: 'HDIM Resources | Event-Driven Healthcare Intelligence',
    description:
      'Healthcare automation that is event-driven, explainable, and release-verifiable.',
  },
  alternates: {
    canonical: 'https://healthdatainmotion.com/resources',
  },
};

export default function ResourcesPage() {
  return <ResourcesHub />;
}

import type { Metadata } from 'next';
import TechnicalHub from './TechnicalHub';

export const metadata: Metadata = {
  title: 'Technical Evaluator Hub | HDIM Resources',
  description:
    'Technical evaluator view of event architecture, tests, runtime controls, and release gates.',
  openGraph: {
    title: 'HDIM Technical Evaluator Hub',
    description:
      'Architecture, contracts, runtime orchestration, and evidence-driven release policy.',
  },
  alternates: {
    canonical: 'https://healthdatainmotion.com/resources/technical',
  },
};

export default function TechnicalPage() {
  return <TechnicalHub />;
}

import type { Metadata } from 'next';
import ExecutiveHub from './ExecutiveHub';

export const metadata: Metadata = {
  title: 'Executive & Compliance Hub | HDIM Resources',
  description:
    'Executive and compliance view of HDIM platform controls, evidence, and release operations.',
  openGraph: {
    title: 'HDIM Executive + Compliance Hub',
    description:
      'Control posture, audit traceability, and release confidence in one operational view.',
  },
  alternates: {
    canonical: 'https://healthdatainmotion.com/resources/executive',
  },
};

export default function ExecutivePage() {
  return <ExecutiveHub />;
}

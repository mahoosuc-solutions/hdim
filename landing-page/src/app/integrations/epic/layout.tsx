import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Epic Systems Integration | HDIM',
  description:
    'Connect HDIM to Epic Systems via FHIR R4 with RS384 JWT authentication. Enterprise HEDIS quality measurement for the largest US EHR.',
  keywords: ['Epic Systems', 'Epic FHIR', 'RS384 JWT', 'App Orchard', 'MyChart', 'HEDIS measures', 'EHR integration'],
};

export default function EpicLayout({ children }: { children: React.ReactNode }) {
  return children;
}

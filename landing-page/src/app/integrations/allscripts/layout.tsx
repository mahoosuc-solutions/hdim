import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Allscripts / Veradigm Integration | HDIM',
  description:
    'Connect HDIM to Allscripts Sunrise and TouchWorks via FHIR R4. Quality measurement across ambulatory and acute care.',
  keywords: ['Allscripts', 'Veradigm', 'Sunrise Clinical Manager', 'TouchWorks', 'FHIR R4', 'HEDIS measures'],
};

export default function AllscriptsLayout({ children }: { children: React.ReactNode }) {
  return children;
}

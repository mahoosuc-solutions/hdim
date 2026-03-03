import IntegrationPageLayout from '@/components/integrations/IntegrationPageLayout';
import { ALLSCRIPTS_INTEGRATION } from '@/lib/data/allscripts-integration';

export default function AllscriptsPage() {
  return <IntegrationPageLayout data={ALLSCRIPTS_INTEGRATION} />;
}

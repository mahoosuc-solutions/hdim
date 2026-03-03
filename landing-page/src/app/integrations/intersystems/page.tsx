import IntegrationPageLayout from '@/components/integrations/IntegrationPageLayout';
import { INTERSYSTEMS_INTEGRATION } from '@/lib/data/intersystems-integration';

export default function InterSystemsPage() {
  return <IntegrationPageLayout data={INTERSYSTEMS_INTEGRATION} />;
}

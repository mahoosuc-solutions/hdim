import IntegrationPageLayout from '@/components/integrations/IntegrationPageLayout';
import { ATHENAHEALTH_INTEGRATION } from '@/lib/data/athenahealth-integration';

export default function AthenahealthPage() {
  return <IntegrationPageLayout data={ATHENAHEALTH_INTEGRATION} />;
}

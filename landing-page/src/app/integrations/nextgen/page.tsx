import IntegrationPageLayout from '@/components/integrations/IntegrationPageLayout';
import { NEXTGEN_INTEGRATION } from '@/lib/data/nextgen-integration';

export default function NextGenPage() {
  return <IntegrationPageLayout data={NEXTGEN_INTEGRATION} />;
}

import { ApiClient } from './client';

export interface IntegrationsHealthResponse {
  status: string;
  details?: Record<string, unknown>;
  [key: string]: unknown;
}

export async function getHealth(client: ApiClient): Promise<IntegrationsHealthResponse> {
  return client.get<IntegrationsHealthResponse>('/health/integrations');
}

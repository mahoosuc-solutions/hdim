export type ScheduleSource = 'appointment-task' | 'encounter' | 'hybrid';

export const DEFAULT_SCHEDULE_SOURCE: ScheduleSource = 'appointment-task';

export const SCHEDULE_SOURCE_BY_TENANT: Record<string, ScheduleSource> = {
  'acme-health': 'appointment-task',
  'demo-clinic': 'encounter',
};

export function getScheduleSourceForTenant(tenantId?: string | null): ScheduleSource {
  if (!tenantId) {
    return DEFAULT_SCHEDULE_SOURCE;
  }
  return SCHEDULE_SOURCE_BY_TENANT[tenantId] || DEFAULT_SCHEDULE_SOURCE;
}

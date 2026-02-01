/**
 * Service Dependency Graph Models
 *
 * Defines the structure of service dependencies for visualization.
 * Used by the Service Dependency Graph component to show how services interact.
 */

/**
 * Service Dependency Node
 * Represents a single service in the dependency graph
 */
export interface ServiceNode {
  id: string;
  displayName: string;
  category: string;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  port: number | null;
}

/**
 * Service Dependency Link
 * Represents a dependency relationship between two services
 */
export interface ServiceLink {
  source: string; // Service ID that depends on another
  target: string; // Service ID being depended upon
  type: DependencyType;
}

/**
 * Dependency Type
 * Types of dependencies between services
 */
export type DependencyType = 'HTTP' | 'KAFKA' | 'DATABASE' | 'CACHE';

/**
 * Service Dependencies Configuration
 * Hard-coded dependency graph for HDIM services
 *
 * Based on architectural design:
 * - Services call each other via HTTP/REST
 * - Services publish/consume Kafka events
 * - Services share PostgreSQL databases
 * - Services use Redis cache
 */
export const SERVICE_DEPENDENCIES: ServiceLink[] = [
  // Quality Measure Service dependencies
  { source: 'quality-measure-service', target: 'patient-service', type: 'HTTP' },
  { source: 'quality-measure-service', target: 'fhir-service', type: 'HTTP' },
  { source: 'quality-measure-service', target: 'cql-engine-service', type: 'HTTP' },

  // Care Gap Service dependencies
  { source: 'care-gap-service', target: 'patient-service', type: 'HTTP' },
  { source: 'care-gap-service', target: 'quality-measure-service', type: 'HTTP' },
  { source: 'care-gap-service', target: 'fhir-service', type: 'HTTP' },

  // Patient Service dependencies
  { source: 'patient-service', target: 'fhir-service', type: 'HTTP' },
  { source: 'patient-service', target: 'consent-service', type: 'HTTP' },

  // CQL Engine dependencies
  { source: 'cql-engine-service', target: 'fhir-service', type: 'HTTP' },
  { source: 'cql-engine-service', target: 'patient-service', type: 'HTTP' },

  // Analytics Service dependencies
  { source: 'analytics-service', target: 'quality-measure-service', type: 'HTTP' },
  { source: 'analytics-service', target: 'patient-service', type: 'HTTP' },
  { source: 'analytics-service', target: 'care-gap-service', type: 'HTTP' },

  // HCC Service dependencies
  { source: 'hcc-service', target: 'patient-service', type: 'HTTP' },
  { source: 'hcc-service', target: 'fhir-service', type: 'HTTP' },

  // Prior Authorization dependencies
  { source: 'prior-auth-service', target: 'patient-service', type: 'HTTP' },
  { source: 'prior-auth-service', target: 'fhir-service', type: 'HTTP' },
  { source: 'prior-auth-service', target: 'approval-service', type: 'HTTP' },

  // EHR Connector dependencies
  { source: 'ehr-connector-service', target: 'fhir-service', type: 'HTTP' },
  { source: 'ehr-connector-service', target: 'patient-service', type: 'HTTP' },

  // QRDA Export dependencies
  { source: 'qrda-export-service', target: 'quality-measure-service', type: 'HTTP' },
  { source: 'qrda-export-service', target: 'patient-service', type: 'HTTP' },
  { source: 'qrda-export-service', target: 'fhir-service', type: 'HTTP' },

  // AI Assistant dependencies
  { source: 'ai-assistant-service', target: 'patient-service', type: 'HTTP' },
  { source: 'ai-assistant-service', target: 'quality-measure-service', type: 'HTTP' },
  { source: 'ai-assistant-service', target: 'care-gap-service', type: 'HTTP' },

  // Event Services (Kafka)
  { source: 'patient-event-service', target: 'patient-service', type: 'KAFKA' },
  { source: 'quality-measure-event-service', target: 'quality-measure-service', type: 'KAFKA' },
  { source: 'care-gap-event-service', target: 'care-gap-service', type: 'KAFKA' },
  { source: 'clinical-workflow-event-service', target: 'patient-service', type: 'KAFKA' },

  // Notification Service (Kafka consumer)
  { source: 'notification-service', target: 'patient-event-service', type: 'KAFKA' },
  { source: 'notification-service', target: 'care-gap-event-service', type: 'KAFKA' },

  // Audit Service (Kafka consumer)
  { source: 'audit-service', target: 'patient-event-service', type: 'KAFKA' },
  { source: 'audit-service', target: 'quality-measure-event-service', type: 'KAFKA' },
  { source: 'audit-service', target: 'care-gap-event-service', type: 'KAFKA' },

  // Documentation Service dependencies
  { source: 'documentation-service', target: 'patient-service', type: 'HTTP' },

  // Consent Service dependencies
  { source: 'consent-service', target: 'patient-service', type: 'HTTP' },

  // Data Enrichment dependencies
  { source: 'data-enrichment-service', target: 'patient-service', type: 'HTTP' },
  { source: 'data-enrichment-service', target: 'fhir-service', type: 'HTTP' },

  // SDOH Service dependencies
  { source: 'sdoh-service', target: 'patient-service', type: 'HTTP' },
  { source: 'sdoh-service', target: 'fhir-service', type: 'HTTP' },

  // Electronic Case Reporting dependencies
  { source: 'ecr-service', target: 'patient-service', type: 'HTTP' },
  { source: 'ecr-service', target: 'fhir-service', type: 'HTTP' },

  // Predictive Analytics dependencies
  { source: 'predictive-analytics-service', target: 'patient-service', type: 'HTTP' },
  { source: 'predictive-analytics-service', target: 'analytics-service', type: 'HTTP' },

  // Cost Monitoring dependencies
  { source: 'cost-monitoring-service', target: 'patient-service', type: 'HTTP' },
  { source: 'cost-monitoring-service', target: 'hcc-service', type: 'HTTP' },

  // Risk Stratification dependencies
  { source: 'risk-stratification-service', target: 'patient-service', type: 'HTTP' },
  { source: 'risk-stratification-service', target: 'hcc-service', type: 'HTTP' },

  // API Gateway (routes to all services)
  { source: 'api-gateway', target: 'patient-service', type: 'HTTP' },
  { source: 'api-gateway', target: 'quality-measure-service', type: 'HTTP' },
  { source: 'api-gateway', target: 'care-gap-service', type: 'HTTP' },
  { source: 'api-gateway', target: 'fhir-service', type: 'HTTP' },
  { source: 'api-gateway', target: 'analytics-service', type: 'HTTP' },
];

/**
 * Get all unique service IDs from dependencies
 */
export function getServiceIdsFromDependencies(): string[] {
  const ids = new Set<string>();
  SERVICE_DEPENDENCIES.forEach((link) => {
    ids.add(link.source);
    ids.add(link.target);
  });
  return Array.from(ids);
}

/**
 * Get dependencies for a specific service
 * Returns services that this service depends on
 */
export function getDependenciesForService(serviceId: string): ServiceLink[] {
  return SERVICE_DEPENDENCIES.filter((link) => link.source === serviceId);
}

/**
 * Get dependents of a specific service
 * Returns services that depend on this service
 */
export function getDependentsOfService(serviceId: string): ServiceLink[] {
  return SERVICE_DEPENDENCIES.filter((link) => link.target === serviceId);
}

/**
 * Calculate impact if a service goes down
 * Returns all services that would be affected (directly or indirectly)
 */
export function calculateImpact(serviceId: string): Set<string> {
  const affected = new Set<string>();
  const queue = [serviceId];

  while (queue.length > 0) {
    const currentId = queue.shift()!;
    const dependents = getDependentsOfService(currentId);

    dependents.forEach((link) => {
      if (!affected.has(link.source)) {
        affected.add(link.source);
        queue.push(link.source);
      }
    });
  }

  return affected;
}

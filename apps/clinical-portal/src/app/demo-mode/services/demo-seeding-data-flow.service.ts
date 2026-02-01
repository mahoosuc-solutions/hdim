import { Injectable } from '@angular/core';
import { DemoProgress, DemoModeService } from './demo-mode.service';
import { PipelineState, PipelineNode, PipelineConnection } from '../../models/system-event.model';

/**
 * Demo Seeding Data Flow Service
 * 
 * Transforms demo seeding progress data into pipeline state for visualization.
 * Maps progress stages to service states and calculates throughput.
 */
@Injectable({
  providedIn: 'root'
})
export class DemoSeedingDataFlowService {

  /**
   * Map demo progress to pipeline state for visualization
   */
  mapProgressToPipelineState(progress: DemoProgress | null): PipelineState {
    if (!progress) {
      return this.getInitialPipelineState();
    }

    const nodes = this.createServiceNodes(progress);
    const connections = this.createServiceConnections(progress);
    
    return {
      nodes,
      connections,
      lastUpdated: progress.updatedAt || new Date().toISOString()
    };
  }

  /**
   * Get initial pipeline state (all services idle)
   */
  private getInitialPipelineState(): PipelineState {
    return {
      nodes: [
        {
          id: 'demo-seeding',
          name: 'Demo Seeding',
          status: 'idle',
          throughput: 0,
          lastActivity: new Date().toISOString(),
          description: 'Generates synthetic patient data',
          errorCount: 0
        },
        {
          id: 'fhir',
          name: 'FHIR Service',
          status: 'idle',
          throughput: 0,
          lastActivity: new Date().toISOString(),
          description: 'Stores FHIR resources',
          errorCount: 0
        },
        {
          id: 'patient',
          name: 'Patient Service',
          status: 'idle',
          throughput: 0,
          lastActivity: new Date().toISOString(),
          description: 'Manages patient demographics',
          errorCount: 0
        },
        {
          id: 'care-gap',
          name: 'Care Gap Service',
          status: 'idle',
          throughput: 0,
          lastActivity: new Date().toISOString(),
          description: 'Identifies care gaps',
          errorCount: 0
        },
        {
          id: 'quality-measure',
          name: 'Quality Measure',
          status: 'idle',
          throughput: 0,
          lastActivity: new Date().toISOString(),
          description: 'Calculates quality measures',
          errorCount: 0
        }
      ],
      connections: [
        { id: 'conn1', from: 'demo-seeding', to: 'fhir', isActive: false, throughput: 0 },
        { id: 'conn2', from: 'fhir', to: 'patient', isActive: false, throughput: 0 },
        { id: 'conn3', from: 'patient', to: 'care-gap', isActive: false, throughput: 0 },
        { id: 'conn4', from: 'care-gap', to: 'quality-measure', isActive: false, throughput: 0 }
      ],
      lastUpdated: new Date().toISOString()
    };
  }

  /**
   * Create service nodes based on progress stage
   */
  private createServiceNodes(progress: DemoProgress): PipelineNode[] {
    const stage = progress.stage;
    
    return [
      {
        id: 'demo-seeding',
        name: 'Demo Seeding',
        status: this.getServiceStatus(stage, 'demo-seeding'),
        throughput: this.calculateThroughput(progress, 'demo-seeding'),
        lastActivity: progress.updatedAt || new Date().toISOString(),
        description: 'Generates synthetic patient data',
        errorCount: stage === 'FAILED' ? 1 : 0
      },
      {
        id: 'fhir',
        name: 'FHIR Service',
        status: this.getServiceStatus(stage, 'fhir'),
        throughput: this.calculateThroughput(progress, 'fhir'),
        lastActivity: progress.updatedAt || new Date().toISOString(),
        description: 'Stores FHIR resources',
        errorCount: 0
      },
      {
        id: 'patient',
        name: 'Patient Service',
        status: this.getServiceStatus(stage, 'patient'),
        throughput: this.calculateThroughput(progress, 'patient'),
        lastActivity: progress.updatedAt || new Date().toISOString(),
        description: 'Manages patient demographics',
        errorCount: 0
      },
      {
        id: 'care-gap',
        name: 'Care Gap Service',
        status: this.getServiceStatus(stage, 'care-gap'),
        throughput: this.calculateThroughput(progress, 'care-gap'),
        lastActivity: progress.updatedAt || new Date().toISOString(),
        description: 'Identifies care gaps',
        errorCount: 0
      },
      {
        id: 'quality-measure',
        name: 'Quality Measure',
        status: this.getServiceStatus(stage, 'quality-measure'),
        throughput: this.calculateThroughput(progress, 'quality-measure'),
        lastActivity: progress.updatedAt || new Date().toISOString(),
        description: 'Calculates quality measures',
        errorCount: 0
      }
    ];
  }

  /**
   * Create service connections based on progress stage
   */
  private createServiceConnections(progress: DemoProgress): PipelineConnection[] {
    const stage = progress.stage;
    
    // Determine which connections should be active based on stage
    const conn1Active = ['GENERATING_PATIENTS', 'PERSISTING_FHIR', 'CREATING_CARE_GAPS', 'SEEDING_MEASURES'].includes(stage);
    const conn2Active = ['PERSISTING_FHIR', 'CREATING_CARE_GAPS', 'SEEDING_MEASURES'].includes(stage);
    const conn3Active = ['CREATING_CARE_GAPS', 'SEEDING_MEASURES'].includes(stage);
    const conn4Active = ['SEEDING_MEASURES'].includes(stage);

    return [
      {
        from: 'demo-seeding',
        to: 'fhir',
        isActive: conn1Active,
        throughput: conn1Active ? this.calculateConnectionThroughput(progress, 'demo-seeding', 'fhir') : 0
      },
      {
        from: 'fhir',
        to: 'patient',
        isActive: conn2Active,
        throughput: conn2Active ? this.calculateConnectionThroughput(progress, 'fhir', 'patient') : 0
      },
      {
        from: 'patient',
        to: 'care-gap',
        isActive: conn3Active,
        throughput: conn3Active ? this.calculateConnectionThroughput(progress, 'patient', 'care-gap') : 0
      },
      {
        from: 'care-gap',
        to: 'quality-measure',
        isActive: conn4Active,
        throughput: conn4Active ? this.calculateConnectionThroughput(progress, 'care-gap', 'quality-measure') : 0
      }
    ];
  }

  /**
   * Get service status based on current stage
   */
  getServiceStatus(stage: string, serviceId: string): 'active' | 'processing' | 'idle' | 'error' {
    if (stage === 'FAILED') {
      return 'error';
    }
    
    if (stage === 'COMPLETE' || stage === 'CANCELLED') {
      return 'idle';
    }

    // Map stages to active services
    const stageServiceMap: Record<string, string[]> = {
      'INITIALIZING': ['demo-seeding'],
      'RESETTING': ['demo-seeding'],
      'GENERATING_PATIENTS': ['demo-seeding'],
      'PERSISTING_FHIR': ['fhir'],
      'CREATING_CARE_GAPS': ['care-gap'],
      'SEEDING_MEASURES': ['quality-measure']
    };

    const activeServices = stageServiceMap[stage] || [];
    
    if (activeServices.includes(serviceId)) {
      return 'processing';
    }
    
    // Check if service should be active (upstream services)
    const serviceOrder = ['demo-seeding', 'fhir', 'patient', 'care-gap', 'quality-measure'];
    const currentServiceIndex = serviceOrder.indexOf(serviceId);
    const activeServiceIndex = activeServices.length > 0 
      ? serviceOrder.indexOf(activeServices[0])
      : -1;
    
    if (activeServiceIndex >= 0 && currentServiceIndex < activeServiceIndex) {
      return 'active'; // Upstream service that has completed
    }
    
    return 'idle';
  }

  /**
   * Calculate throughput for a service
   */
  calculateThroughput(progress: DemoProgress, serviceId: string): number {
    const stage = progress.stage;
    
    // Calculate throughput based on stage and counts
    switch (serviceId) {
      case 'demo-seeding':
        if (stage === 'GENERATING_PATIENTS' && progress.patientsGenerated) {
          // Estimate: if we have progress percent, calculate rate
          // Assume 5 seconds per 100 patients
          return progress.patientsGenerated / 5;
        }
        return 0;
        
      case 'fhir':
        if (stage === 'PERSISTING_FHIR' && progress.patientsPersisted) {
          // Estimate: patients per second
          return progress.patientsPersisted / 3;
        }
        return 0;
        
      case 'care-gap':
        if (stage === 'CREATING_CARE_GAPS' && progress.careGapsCreated) {
          // Estimate: care gaps per second
          return progress.careGapsCreated / 2;
        }
        return 0;
        
      case 'quality-measure':
        if (stage === 'SEEDING_MEASURES' && progress.measuresSeeded) {
          // Estimate: measures per second
          return progress.measuresSeeded / 1;
        }
        return 0;
        
      default:
        return 0;
    }
  }

  /**
   * Calculate connection throughput between two services
   */
  private calculateConnectionThroughput(progress: DemoProgress, from: string, to: string): number {
    // Use the throughput of the source service
    return this.calculateThroughput(progress, from);
  }
}

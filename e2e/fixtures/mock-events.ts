/**
 * Mock Events Fixture for Event-Driven Testing
 *
 * Provides mock event data for WebSocket and Kafka event testing.
 * These events mirror the real event structures used in the HDIM platform.
 */

// Event types matching Kafka topics and WebSocket messages
export enum EventType {
  // Evaluation Events
  EVALUATION_STARTED = 'EVALUATION_STARTED',
  EVALUATION_PROGRESS = 'EVALUATION_PROGRESS',
  EVALUATION_COMPLETE = 'EVALUATION_COMPLETE',
  EVALUATION_FAILED = 'EVALUATION_FAILED',

  // Batch Evaluation Events
  BATCH_STARTED = 'BATCH_STARTED',
  BATCH_PROGRESS = 'BATCH_PROGRESS',
  BATCH_COMPLETE = 'BATCH_COMPLETE',
  BATCH_FAILED = 'BATCH_FAILED',

  // Care Gap Events
  CARE_GAP_CREATED = 'CARE_GAP_CREATED',
  CARE_GAP_UPDATED = 'CARE_GAP_UPDATED',
  CARE_GAP_CLOSED = 'CARE_GAP_CLOSED',
  INTERVENTION_RECORDED = 'INTERVENTION_RECORDED',

  // Patient Events
  PATIENT_DATA_SYNCED = 'PATIENT_DATA_SYNCED',
  PATIENT_UPDATED = 'PATIENT_UPDATED',
  PATIENT_MERGED = 'PATIENT_MERGED',

  // Clinical Alert Events
  ALERT_TRIGGERED = 'ALERT_TRIGGERED',
  ALERT_ACKNOWLEDGED = 'ALERT_ACKNOWLEDGED',
  ALERT_RESOLVED = 'ALERT_RESOLVED',
  ALERT_ESCALATED = 'ALERT_ESCALATED',

  // Report Events
  REPORT_GENERATED = 'REPORT_GENERATED',
  REPORT_FAILED = 'REPORT_FAILED',
  EXPORT_COMPLETE = 'EXPORT_COMPLETE',

  // System Events
  SYSTEM_HEALTH = 'SYSTEM_HEALTH',
  SERVICE_STATUS = 'SERVICE_STATUS',
  CONNECTION_STATUS = 'CONNECTION_STATUS',
}

// Alert severity levels
export enum AlertSeverity {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
  INFO = 'INFO',
}

// Alert types
export enum AlertType {
  A1C_CRITICAL = 'A1C_CRITICAL',
  BP_CRITICAL = 'BP_CRITICAL',
  MEDICATION_INTERACTION = 'MEDICATION_INTERACTION',
  CARE_GAP_OVERDUE = 'CARE_GAP_OVERDUE',
  LAB_ABNORMAL = 'LAB_ABNORMAL',
  FALL_RISK = 'FALL_RISK',
  READMISSION_RISK = 'READMISSION_RISK',
}

// Base event interface
export interface BaseEvent {
  type: EventType;
  timestamp: number;
  correlationId: string;
  tenantId: string;
  source: string;
}

// Evaluation event payloads
export interface EvaluationStartedEvent extends BaseEvent {
  type: EventType.EVALUATION_STARTED;
  payload: {
    evaluationId: string;
    patientId: string;
    measureCode: string;
    measureName: string;
    initiatedBy: string;
  };
}

export interface EvaluationProgressEvent extends BaseEvent {
  type: EventType.EVALUATION_PROGRESS;
  payload: {
    evaluationId: string;
    patientId: string;
    progress: number;
    stage: 'LOADING_DATA' | 'EXECUTING_CQL' | 'CALCULATING_RESULT';
    message: string;
  };
}

export interface EvaluationCompleteEvent extends BaseEvent {
  type: EventType.EVALUATION_COMPLETE;
  payload: {
    evaluationId: string;
    patientId: string;
    measureCode: string;
    result: 'COMPLIANT' | 'NON_COMPLIANT' | 'NOT_ELIGIBLE';
    numerator: boolean;
    denominator: boolean;
    duration: number;
    careGapCreated?: string;
  };
}

// Batch event payloads
export interface BatchProgressEvent extends BaseEvent {
  type: EventType.BATCH_PROGRESS;
  payload: {
    batchId: string;
    totalPatients: number;
    completedPatients: number;
    progress: number;
    estimatedTimeRemaining: number;
    currentPatient?: string;
  };
}

export interface BatchCompleteEvent extends BaseEvent {
  type: EventType.BATCH_COMPLETE;
  payload: {
    batchId: string;
    totalPatients: number;
    compliant: number;
    nonCompliant: number;
    notEligible: number;
    failed: number;
    duration: number;
    careGapsCreated: number;
  };
}

// Care gap event payloads
export interface CareGapCreatedEvent extends BaseEvent {
  type: EventType.CARE_GAP_CREATED;
  payload: {
    gapId: string;
    patientId: string;
    measureCode: string;
    gapType: string;
    urgency: 'HIGH' | 'MEDIUM' | 'LOW';
    dueDate: string;
    recommendation: string;
  };
}

export interface CareGapClosedEvent extends BaseEvent {
  type: EventType.CARE_GAP_CLOSED;
  payload: {
    gapId: string;
    patientId: string;
    closedBy: string;
    reason: string;
    notes?: string;
  };
}

// Alert event payloads
export interface AlertTriggeredEvent extends BaseEvent {
  type: EventType.ALERT_TRIGGERED;
  payload: {
    alertId: string;
    patientId: string;
    alertType: AlertType;
    severity: AlertSeverity;
    title: string;
    message: string;
    sourceData: {
      value?: number;
      threshold?: number;
      unit?: string;
    };
    requiredAction?: string;
    assignedTo?: string;
  };
}

export interface AlertAcknowledgedEvent extends BaseEvent {
  type: EventType.ALERT_ACKNOWLEDGED;
  payload: {
    alertId: string;
    acknowledgedBy: string;
    notes?: string;
  };
}

// Report event payloads
export interface ReportGeneratedEvent extends BaseEvent {
  type: EventType.REPORT_GENERATED;
  payload: {
    reportId: string;
    reportType: string;
    format: 'PDF' | 'CSV' | 'XLSX' | 'JSON';
    downloadUrl: string;
    expiresAt: string;
    recordCount: number;
  };
}

// Union type for all events
export type HDIMEvent =
  | EvaluationStartedEvent
  | EvaluationProgressEvent
  | EvaluationCompleteEvent
  | BatchProgressEvent
  | BatchCompleteEvent
  | CareGapCreatedEvent
  | CareGapClosedEvent
  | AlertTriggeredEvent
  | AlertAcknowledgedEvent
  | ReportGeneratedEvent;

/**
 * Mock Event Factory
 *
 * Creates mock events for testing purposes.
 */
export class MockEventFactory {
  private idCounter = 0;
  private tenantId: string;

  constructor(tenantId: string = 'TENANT001') {
    this.tenantId = tenantId;
  }

  private generateId(prefix: string): string {
    return `${prefix}_${Date.now()}_${++this.idCounter}`;
  }

  private baseEvent(type: EventType, correlationId?: string): BaseEvent {
    return {
      type,
      timestamp: Date.now(),
      correlationId: correlationId || this.generateId('CORR'),
      tenantId: this.tenantId,
      source: 'mock-event-factory',
    };
  }

  // Evaluation Events
  evaluationStarted(patientId: string, measureCode: string): EvaluationStartedEvent {
    const correlationId = this.generateId('EVAL');
    return {
      ...this.baseEvent(EventType.EVALUATION_STARTED, correlationId),
      type: EventType.EVALUATION_STARTED,
      payload: {
        evaluationId: correlationId,
        patientId,
        measureCode,
        measureName: `Quality Measure ${measureCode}`,
        initiatedBy: 'test_user',
      },
    };
  }

  evaluationProgress(evaluationId: string, patientId: string, progress: number): EvaluationProgressEvent {
    const stages = ['LOADING_DATA', 'EXECUTING_CQL', 'CALCULATING_RESULT'] as const;
    const stageIndex = Math.floor((progress / 100) * 3);
    const stage = stages[Math.min(stageIndex, 2)];

    return {
      ...this.baseEvent(EventType.EVALUATION_PROGRESS, evaluationId),
      type: EventType.EVALUATION_PROGRESS,
      payload: {
        evaluationId,
        patientId,
        progress,
        stage,
        message: `${stage.replace(/_/g, ' ').toLowerCase()}...`,
      },
    };
  }

  evaluationComplete(
    evaluationId: string,
    patientId: string,
    measureCode: string,
    result: 'COMPLIANT' | 'NON_COMPLIANT' | 'NOT_ELIGIBLE'
  ): EvaluationCompleteEvent {
    return {
      ...this.baseEvent(EventType.EVALUATION_COMPLETE, evaluationId),
      type: EventType.EVALUATION_COMPLETE,
      payload: {
        evaluationId,
        patientId,
        measureCode,
        result,
        numerator: result === 'COMPLIANT',
        denominator: result !== 'NOT_ELIGIBLE',
        duration: Math.floor(Math.random() * 3000) + 1000,
        careGapCreated: result === 'NON_COMPLIANT' ? this.generateId('GAP') : undefined,
      },
    };
  }

  // Batch Events
  batchProgress(batchId: string, completed: number, total: number): BatchProgressEvent {
    return {
      ...this.baseEvent(EventType.BATCH_PROGRESS, batchId),
      type: EventType.BATCH_PROGRESS,
      payload: {
        batchId,
        totalPatients: total,
        completedPatients: completed,
        progress: Math.round((completed / total) * 100),
        estimatedTimeRemaining: (total - completed) * 2000, // 2s per patient
        currentPatient: `PATIENT_${completed + 1}`,
      },
    };
  }

  batchComplete(batchId: string, results: { total: number; compliant: number; nonCompliant: number; notEligible: number }): BatchCompleteEvent {
    return {
      ...this.baseEvent(EventType.BATCH_COMPLETE, batchId),
      type: EventType.BATCH_COMPLETE,
      payload: {
        batchId,
        totalPatients: results.total,
        compliant: results.compliant,
        nonCompliant: results.nonCompliant,
        notEligible: results.notEligible,
        failed: 0,
        duration: results.total * 2000,
        careGapsCreated: results.nonCompliant,
      },
    };
  }

  // Care Gap Events
  careGapCreated(patientId: string, measureCode: string, urgency: 'HIGH' | 'MEDIUM' | 'LOW' = 'MEDIUM'): CareGapCreatedEvent {
    const gapId = this.generateId('GAP');
    return {
      ...this.baseEvent(EventType.CARE_GAP_CREATED, gapId),
      type: EventType.CARE_GAP_CREATED,
      payload: {
        gapId,
        patientId,
        measureCode,
        gapType: 'SCREENING',
        urgency,
        dueDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString(),
        recommendation: `Complete ${measureCode} screening for patient`,
      },
    };
  }

  careGapClosed(gapId: string, patientId: string, reason: string): CareGapClosedEvent {
    return {
      ...this.baseEvent(EventType.CARE_GAP_CLOSED, gapId),
      type: EventType.CARE_GAP_CLOSED,
      payload: {
        gapId,
        patientId,
        closedBy: 'test_user',
        reason,
        notes: 'Closed via E2E test',
      },
    };
  }

  // Alert Events
  alertTriggered(
    patientId: string,
    alertType: AlertType,
    severity: AlertSeverity,
    sourceData?: { value?: number; threshold?: number; unit?: string }
  ): AlertTriggeredEvent {
    const alertId = this.generateId('ALERT');
    const alertMessages: Record<AlertType, { title: string; message: string }> = {
      [AlertType.A1C_CRITICAL]: { title: 'Critical A1C Level', message: 'Patient A1C level is critically high' },
      [AlertType.BP_CRITICAL]: { title: 'Critical Blood Pressure', message: 'Patient blood pressure is critically elevated' },
      [AlertType.MEDICATION_INTERACTION]: { title: 'Medication Interaction', message: 'Potential drug interaction detected' },
      [AlertType.CARE_GAP_OVERDUE]: { title: 'Overdue Care Gap', message: 'Care gap is significantly overdue' },
      [AlertType.LAB_ABNORMAL]: { title: 'Abnormal Lab Result', message: 'Lab result outside normal range' },
      [AlertType.FALL_RISK]: { title: 'Fall Risk Alert', message: 'Patient has elevated fall risk' },
      [AlertType.READMISSION_RISK]: { title: 'Readmission Risk', message: 'Patient at high risk for readmission' },
    };

    const { title, message } = alertMessages[alertType];

    return {
      ...this.baseEvent(EventType.ALERT_TRIGGERED, alertId),
      type: EventType.ALERT_TRIGGERED,
      payload: {
        alertId,
        patientId,
        alertType,
        severity,
        title,
        message,
        sourceData: sourceData || {},
        requiredAction: 'Review and acknowledge within 24 hours',
      },
    };
  }

  alertAcknowledged(alertId: string, acknowledgedBy: string): AlertAcknowledgedEvent {
    return {
      ...this.baseEvent(EventType.ALERT_ACKNOWLEDGED, alertId),
      type: EventType.ALERT_ACKNOWLEDGED,
      payload: {
        alertId,
        acknowledgedBy,
        notes: 'Acknowledged via E2E test',
      },
    };
  }

  // Report Events
  reportGenerated(reportType: string, format: 'PDF' | 'CSV' | 'XLSX' | 'JSON' = 'PDF'): ReportGeneratedEvent {
    const reportId = this.generateId('RPT');
    return {
      ...this.baseEvent(EventType.REPORT_GENERATED, reportId),
      type: EventType.REPORT_GENERATED,
      payload: {
        reportId,
        reportType,
        format,
        downloadUrl: `/api/v1/reports/${reportId}/download`,
        expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
        recordCount: Math.floor(Math.random() * 1000) + 100,
      },
    };
  }

  // Sequence generators for testing event flows
  evaluationSequence(patientId: string, measureCode: string, result: 'COMPLIANT' | 'NON_COMPLIANT' | 'NOT_ELIGIBLE'): HDIMEvent[] {
    const started = this.evaluationStarted(patientId, measureCode);
    const evalId = started.payload.evaluationId;

    return [
      started,
      this.evaluationProgress(evalId, patientId, 25),
      this.evaluationProgress(evalId, patientId, 50),
      this.evaluationProgress(evalId, patientId, 75),
      this.evaluationComplete(evalId, patientId, measureCode, result),
    ];
  }

  batchSequence(batchId: string, patientCount: number): HDIMEvent[] {
    const events: HDIMEvent[] = [];

    // Progress events
    for (let i = 1; i <= patientCount; i++) {
      events.push(this.batchProgress(batchId, i, patientCount));
    }

    // Complete event
    const compliant = Math.floor(patientCount * 0.6);
    const nonCompliant = Math.floor(patientCount * 0.3);
    const notEligible = patientCount - compliant - nonCompliant;

    events.push(this.batchComplete(batchId, {
      total: patientCount,
      compliant,
      nonCompliant,
      notEligible,
    }));

    return events;
  }
}

// Default factory instance
export const mockEventFactory = new MockEventFactory();

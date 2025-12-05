/**
 * Test suite for EventDetailsModal component
 * Following TDD approach - tests written BEFORE implementation
 */

import { describe, it, expect, vi } from 'vitest';
import { renderWithTheme, screen, userEvent, waitFor } from '../../test/test-utils';
import EventDetailsModal from '../EventDetailsModal';
import {
  EventType,
  FailureCategory,
  EvaluationCompletedEvent,
  EvaluationFailedEvent,
  BatchProgressEvent,
  EvaluationStartedEvent,
} from '../../types/events';

// Mock events for testing
const mockCompletedEvent: EvaluationCompletedEvent = {
  eventId: 'evt-001',
  eventType: EventType.EVALUATION_COMPLETED,
  tenantId: 'TENANT001',
  timestamp: 1699564800000, // Nov 10, 2023
  evaluationId: 'eval-001',
  measureId: 'HEDIS-CDC',
  measureName: 'Comprehensive Diabetes Care',
  patientId: 'PAT-12345',
  batchId: 'batch-001',
  inDenominator: true,
  inNumerator: true,
  complianceRate: 0.85,
  score: 100,
  durationMs: 125,
  evidence: {
    diagnoses: ['E11.9 - Type 2 diabetes'],
    medications: ['Metformin 500mg'],
    labs: [{ test: 'HbA1c', value: 6.5, date: '2023-10-01' }],
  },
  careGapCount: 0,
};

const mockFailedEvent: EvaluationFailedEvent = {
  eventId: 'evt-002',
  eventType: EventType.EVALUATION_FAILED,
  tenantId: 'TENANT001',
  timestamp: 1699564900000,
  evaluationId: 'eval-002',
  measureId: 'HEDIS-CDC',
  measureName: 'Comprehensive Diabetes Care',
  patientId: 'PAT-67890',
  batchId: 'batch-001',
  errorMessage: 'FHIR resource not found',
  errorCategory: FailureCategory.FHIR_FETCH_ERROR,
  stackTrace: 'at FhirClient.fetch(FhirClient.java:45)\nat EvaluationService.evaluate(EvaluationService.java:123)',
  durationMs: 50,
};

const mockBatchProgressEvent: BatchProgressEvent = {
  eventType: EventType.BATCH_PROGRESS,
  batchId: 'batch-001',
  tenantId: 'TENANT001',
  measureId: 'HEDIS-CDC',
  measureName: 'Comprehensive Diabetes Care',
  totalPatients: 100,
  completedCount: 45,
  successCount: 43,
  failedCount: 2,
  pendingCount: 55,
  percentComplete: 45.0,
  avgDurationMs: 125.5,
  currentThroughput: 3.6,
  elapsedTimeMs: 12500,
  estimatedTimeRemainingMs: 15278,
  denominatorCount: 38,
  numeratorCount: 28,
  cumulativeComplianceRate: 73.7,
  timestamp: 1699564800000,
};

const mockStartedEvent: EvaluationStartedEvent = {
  eventId: 'evt-003',
  eventType: EventType.EVALUATION_STARTED,
  tenantId: 'TENANT001',
  timestamp: 1699564700000,
  evaluationId: 'eval-003',
  measureId: 'HEDIS-CDC',
  measureName: 'Comprehensive Diabetes Care',
  patientId: 'PAT-11111',
  batchId: 'batch-001',
};

describe('EventDetailsModal', () => {
  it('renders with closed state (not visible)', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={false} onClose={vi.fn()} />
    );

    // Dialog should not be visible when open=false
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('renders with open state (visible)', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    // Dialog should be visible when open=true
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('displays event type correctly', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    // Event type should be displayed in the title or content
    expect(screen.getByText(/EVALUATION_COMPLETED/i)).toBeInTheDocument();
  });

  it('displays timestamp formatted', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    // Timestamp should be formatted as a readable date
    const formattedDate = new Date(mockCompletedEvent.timestamp).toLocaleString();
    expect(screen.getByText(formattedDate)).toBeInTheDocument();
  });

  it('displays patient ID for patient events', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    expect(screen.getByText(/PAT-12345/)).toBeInTheDocument();
  });

  it('displays clinical data for completed events', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    // Should display denominator, numerator, compliance rate
    expect(screen.getByText(/Denominator/i)).toBeInTheDocument();
    expect(screen.getByText(/Numerator/i)).toBeInTheDocument();
    expect(screen.getByText(/85%/)).toBeInTheDocument(); // compliance rate
  });

  it('displays error details for failed events', () => {
    renderWithTheme(
      <EventDetailsModal event={mockFailedEvent} open={true} onClose={vi.fn()} />
    );

    // Should display error message and category
    expect(screen.getByText(/FHIR resource not found/i)).toBeInTheDocument();
    expect(screen.getByText(/FHIR_FETCH_ERROR/i)).toBeInTheDocument();
  });

  it('displays stack trace for failed events', () => {
    renderWithTheme(
      <EventDetailsModal event={mockFailedEvent} open={true} onClose={vi.fn()} />
    );

    // Should display stack trace
    expect(screen.getByText(/FhirClient.fetch/)).toBeInTheDocument();
  });

  it('displays batch stats for batch progress events', () => {
    renderWithTheme(
      <EventDetailsModal event={mockBatchProgressEvent} open={true} onClose={vi.fn()} />
    );

    // Should display batch statistics
    expect(screen.getByText(/Total Patients/i)).toBeInTheDocument();
    expect(screen.getByText(/Batch Statistics/i)).toBeInTheDocument();
    expect(screen.getByText(/Percent Complete/i)).toBeInTheDocument();
    expect(screen.getByText(/Cumulative Compliance Rate/i)).toBeInTheDocument();
  });

  it('copy to clipboard works', async () => {
    const user = userEvent.setup();

    // Mock clipboard API
    const writeTextMock = vi.fn(() => Promise.resolve());
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: writeTextMock,
      },
      writable: true,
      configurable: true,
    });

    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    // Find and click copy button (by text since aria-label is "copy")
    const copyButton = screen.getByText(/Copy JSON/i);
    await user.click(copyButton);

    // Verify clipboard.writeText was called
    expect(writeTextMock).toHaveBeenCalled();
  });

  it('close button calls onClose', async () => {
    const user = userEvent.setup();
    const onCloseMock = vi.fn();

    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={onCloseMock} />
    );

    // Find and click close button in the dialog actions (by text)
    const closeButton = screen.getByText(/^Close$/i);
    await user.click(closeButton);

    expect(onCloseMock).toHaveBeenCalledTimes(1);
  });

  it('ESC key calls onClose', async () => {
    const user = userEvent.setup();
    const onCloseMock = vi.fn();

    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={onCloseMock} />
    );

    // Press ESC key
    await user.keyboard('{Escape}');

    expect(onCloseMock).toHaveBeenCalledTimes(1);
  });

  it('does not render when event is null', () => {
    renderWithTheme(
      <EventDetailsModal event={null} open={true} onClose={vi.fn()} />
    );

    // Dialog should not show content when event is null
    expect(screen.queryByText(/Event Details/i)).not.toBeInTheDocument();
  });

  it('handles missing optional fields gracefully', () => {
    const eventWithoutOptionalFields: EvaluationCompletedEvent = {
      ...mockCompletedEvent,
      measureName: undefined,
      batchId: undefined,
    };

    renderWithTheme(
      <EventDetailsModal event={eventWithoutOptionalFields} open={true} onClose={vi.fn()} />
    );

    // Should render without crashing
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/PAT-12345/)).toBeInTheDocument();
  });

  it('expands and collapses sections', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    // Should have accordion sections
    expect(screen.getByText(/Overview/i)).toBeInTheDocument();
    expect(screen.getByText(/Patient Info/i)).toBeInTheDocument();
    expect(screen.getByText(/Clinical Data/i)).toBeInTheDocument();
    expect(screen.getByText(/^Evidence$/i)).toBeInTheDocument();

    // Overview section is expanded by default, so timestamp should be visible
    expect(screen.getByText(new Date(mockCompletedEvent.timestamp).toLocaleString())).toBeInTheDocument();
  });

  it('JSON syntax highlighting present', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    // Expand evidence section to see JSON
    const evidenceSection = screen.getByText(/Evidence/i);
    evidenceSection.click();

    // Should have a <pre> tag for JSON display
    const preElement = screen.getByText(/diagnoses/).closest('pre');
    expect(preElement).toBeInTheDocument();
    expect(preElement).toHaveStyle({ fontFamily: /monospace/ });
  });

  it('displays duration for completed events', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    // Should display duration in ms
    expect(screen.getByText(/125ms/i)).toBeInTheDocument();
  });

  it('displays measure name when available', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    expect(screen.getByText(/Comprehensive Diabetes Care/i)).toBeInTheDocument();
  });

  it('displays batch ID when available', () => {
    renderWithTheme(
      <EventDetailsModal event={mockCompletedEvent} open={true} onClose={vi.fn()} />
    );

    expect(screen.getByText(/batch-001/i)).toBeInTheDocument();
  });

  it('displays throughput for batch progress events', () => {
    renderWithTheme(
      <EventDetailsModal event={mockBatchProgressEvent} open={true} onClose={vi.fn()} />
    );

    // Should display throughput (3.6 evals/sec)
    expect(screen.getByText(/3.6/)).toBeInTheDocument();
    expect(screen.getByText(/throughput/i)).toBeInTheDocument();
  });

  it('handles evaluation started events', () => {
    renderWithTheme(
      <EventDetailsModal event={mockStartedEvent} open={true} onClose={vi.fn()} />
    );

    // Should render basic event info
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText(/EVALUATION_STARTED/i)).toBeInTheDocument();
    expect(screen.getByText(/PAT-11111/)).toBeInTheDocument();
  });
});

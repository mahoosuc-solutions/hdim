/**
 * EventDetailsModal Component
 * Modal dialog to display full event details with expandable sections
 */

import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Typography,
  Box,
  IconButton,
  useTheme,
  useMediaQuery,
  Chip,
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  Close as CloseIcon,
  ContentCopy as ContentCopyIcon,
} from '@mui/icons-material';
import type { AnyEvaluationEvent } from '../types/events';
import {
  isEvaluationCompletedEvent,
  isEvaluationFailedEvent,
  isBatchProgressEvent,
  EventType,
} from '../types/events';

export interface EventDetailsModalProps {
  event: AnyEvaluationEvent | null;
  open: boolean;
  onClose: () => void;
}

const EventDetailsModal: React.FC<EventDetailsModalProps> = ({ event, open, onClose }) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const [copySuccess, setCopySuccess] = useState(false);

  // Don't render if event is null
  if (!event) {
    return null;
  }

  const handleCopyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(JSON.stringify(event, null, 2));
      setCopySuccess(true);
      setTimeout(() => setCopySuccess(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const formatTimestamp = (timestamp: number) => {
    return new Date(timestamp).toLocaleString();
  };

  const formatDuration = (durationMs: number) => {
    return `${durationMs}ms`;
  };

  const formatPercentage = (value: number) => {
    return `${Math.round(value * 100)}%`;
  };

  const renderOverviewSection = () => {
    const isCompleted = isEvaluationCompletedEvent(event);
    const isFailed = isEvaluationFailedEvent(event);
    const isBatch = isBatchProgressEvent(event);

    return (
      <Accordion defaultExpanded>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Overview</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Event Type
              </Typography>
              <Typography>{event.eventType}</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Timestamp
              </Typography>
              <Typography>{formatTimestamp(event.timestamp)}</Typography>
            </Box>
            {(isCompleted || isFailed) && 'durationMs' in event && (
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Duration
                </Typography>
                <Typography>{formatDuration(event.durationMs)}</Typography>
              </Box>
            )}
            {isBatch && (
              <>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Percent Complete
                  </Typography>
                  <Typography>{event.percentComplete}%</Typography>
                </Box>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <Typography>
                    {event.completedCount} / {event.totalPatients} patients
                  </Typography>
                </Box>
              </>
            )}
          </Box>
        </AccordionDetails>
      </Accordion>
    );
  };

  const renderPatientInfoSection = () => {
    const isCompleted = isEvaluationCompletedEvent(event);
    const isFailed = isEvaluationFailedEvent(event);
    const isBatch = isBatchProgressEvent(event);

    // Only show for patient-level events
    if (!isCompleted && !isFailed && !('patientId' in event)) {
      return null;
    }

    return (
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Patient Info</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {'patientId' in event && (
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Patient ID
                </Typography>
                <Typography>{event.patientId}</Typography>
              </Box>
            )}
            {'batchId' in event && event.batchId && (
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Batch ID
                </Typography>
                <Typography>{event.batchId}</Typography>
              </Box>
            )}
            {'measureId' in event && (
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Measure ID
                </Typography>
                <Typography>{event.measureId}</Typography>
              </Box>
            )}
            {'measureName' in event && event.measureName && (
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Measure Name
                </Typography>
                <Typography>{event.measureName}</Typography>
              </Box>
            )}
          </Box>
        </AccordionDetails>
      </Accordion>
    );
  };

  const renderClinicalDataSection = () => {
    if (!isEvaluationCompletedEvent(event)) {
      return null;
    }

    return (
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Clinical Data</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Denominator
              </Typography>
              <Typography>{event.inDenominator ? 'Yes' : 'No'}</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Numerator
              </Typography>
              <Typography>{event.inNumerator ? 'Yes' : 'No'}</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Compliance Rate
              </Typography>
              <Typography>{formatPercentage(event.complianceRate)}</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Score
              </Typography>
              <Typography>{event.score}</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Care Gap Count
              </Typography>
              <Typography>{event.careGapCount}</Typography>
            </Box>
          </Box>
        </AccordionDetails>
      </Accordion>
    );
  };

  const renderEvidenceSection = () => {
    if (!isEvaluationCompletedEvent(event)) {
      return null;
    }

    return (
      <Accordion>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Evidence</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box sx={{ maxHeight: 400, overflow: 'auto' }}>
            <pre
              style={{
                fontFamily: 'monospace',
                fontSize: '0.875rem',
                backgroundColor: theme.palette.mode === 'dark' ? '#1e1e1e' : '#f5f5f5',
                padding: theme.spacing(2),
                borderRadius: theme.shape.borderRadius,
                overflow: 'auto',
                margin: 0,
              }}
            >
              {JSON.stringify(event.evidence, null, 2)}
            </pre>
          </Box>
        </AccordionDetails>
      </Accordion>
    );
  };

  const renderErrorDetailsSection = () => {
    if (!isEvaluationFailedEvent(event)) {
      return null;
    }

    return (
      <Accordion defaultExpanded>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6" color="error">
            Error Details
          </Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Error Message
              </Typography>
              <Typography color="error">{event.errorMessage}</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Error Category
              </Typography>
              <Chip label={event.errorCategory} color="error" size="small" />
            </Box>
            {event.stackTrace && (
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Stack Trace
                </Typography>
                <pre
                  style={{
                    fontFamily: 'monospace',
                    fontSize: '0.75rem',
                    backgroundColor: theme.palette.mode === 'dark' ? '#1e1e1e' : '#f5f5f5',
                    padding: theme.spacing(2),
                    borderRadius: theme.shape.borderRadius,
                    overflow: 'auto',
                    margin: 0,
                    maxHeight: 300,
                  }}
                >
                  {event.stackTrace}
                </pre>
              </Box>
            )}
          </Box>
        </AccordionDetails>
      </Accordion>
    );
  };

  const renderBatchStatsSection = () => {
    if (!isBatchProgressEvent(event)) {
      return null;
    }

    return (
      <Accordion defaultExpanded>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="h6">Batch Statistics</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Total Patients
              </Typography>
              <Typography>{event.totalPatients}</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Completed
              </Typography>
              <Typography>
                {event.completedCount} ({event.percentComplete}%)
              </Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Success / Failed
              </Typography>
              <Typography>
                {event.successCount} / {event.failedCount}
              </Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Throughput
              </Typography>
              <Typography>{event.currentThroughput} evals/sec</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Average Duration
              </Typography>
              <Typography>{formatDuration(event.avgDurationMs)}</Typography>
            </Box>
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Cumulative Compliance Rate
              </Typography>
              <Typography>{event.cumulativeComplianceRate}%</Typography>
            </Box>
          </Box>
        </AccordionDetails>
      </Accordion>
    );
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullScreen={isMobile}
      maxWidth="md"
      fullWidth
      aria-labelledby="event-details-dialog-title"
    >
      <DialogTitle id="event-details-dialog-title">
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h5">Event Details</Typography>
          <IconButton onClick={onClose} size="small" aria-label="close">
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent dividers>
        {renderOverviewSection()}
        {renderPatientInfoSection()}
        {renderClinicalDataSection()}
        {renderEvidenceSection()}
        {renderErrorDetailsSection()}
        {renderBatchStatsSection()}
      </DialogContent>
      <DialogActions>
        <Button
          startIcon={<ContentCopyIcon />}
          onClick={handleCopyToClipboard}
          variant="outlined"
          aria-label="copy"
        >
          {copySuccess ? 'Copied!' : 'Copy JSON'}
        </Button>
        <Button onClick={onClose} variant="contained" aria-label="close">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default EventDetailsModal;

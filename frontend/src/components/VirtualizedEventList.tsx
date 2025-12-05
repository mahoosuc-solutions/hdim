/**
 * VirtualizedEventList Component
 * Efficiently renders large lists of events (>1000 items) using virtual scrolling
 * Implements react-window v2 List for performance optimization
 */

import { ComponentProps } from 'react';
import { List } from 'react-window';
import { Box, Typography, Paper } from '@mui/material';
import { format } from 'date-fns';
import type { AnyEvaluationEvent } from '../types/events';
import { isBatchProgressEvent } from '../types/events';
import EventNoteIcon from '@mui/icons-material/EventNote';

export interface VirtualizedEventListProps {
  events: AnyEvaluationEvent[];
  onEventClick: (event: AnyEvaluationEvent) => void;
  height?: number;
  itemHeight?: number;
}

/**
 * Row props passed to each row component
 */
interface EventRowProps {
  events: AnyEvaluationEvent[];
  onClick: (event: AnyEvaluationEvent) => void;
}

/**
 * Extract patientId from event if available
 */
const getPatientId = (event: AnyEvaluationEvent): string => {
  if (isBatchProgressEvent(event)) {
    return 'N/A (Batch)';
  }

  // EvaluationStartedEvent, EvaluationCompletedEvent, EvaluationFailedEvent all have patientId
  if ('patientId' in event) {
    return event.patientId || 'N/A';
  }

  return 'N/A';
};

/**
 * Extract measureId from event
 */
const getMeasureId = (event: AnyEvaluationEvent): string => {
  if ('measureId' in event) {
    return event.measureId;
  }
  return 'N/A';
};

/**
 * Format timestamp to readable date string
 */
const formatTimestamp = (timestamp: number): string => {
  try {
    return format(new Date(timestamp), 'yyyy-MM-dd HH:mm:ss');
  } catch {
    return 'Invalid Date';
  }
};

/**
 * Row component for each virtualized item
 */
const EventRow = ({
  index,
  events,
  onClick,
  style,
}: {
  index: number;
  ariaAttributes: {
    'aria-posinset': number;
    'aria-setsize': number;
    role: 'listitem';
  };
  style: React.CSSProperties;
} & EventRowProps) => {
  const event = events[index];

  const handleClick = () => {
    onClick(event);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onClick(event);
    }
  };

  return (
    <Box
      style={style}
      role="button"
      tabIndex={0}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      sx={{
        display: 'flex',
        alignItems: 'center',
        padding: 2,
        cursor: 'pointer',
        borderBottom: '1px solid',
        borderColor: 'divider',
        '&:hover': {
          backgroundColor: 'action.hover',
        },
        '&:focus': {
          outline: '2px solid',
          outlineColor: 'primary.main',
          outlineOffset: '-2px',
        },
      }}
    >
      <Box sx={{ flex: '0 0 180px', mr: 2 }}>
        <Typography variant="body2" color="text.secondary">
          {formatTimestamp(event.timestamp)}
        </Typography>
      </Box>

      <Box sx={{ flex: '0 0 200px', mr: 2 }}>
        <Typography
          variant="body2"
          sx={{
            fontWeight: 500,
            color:
              event.eventType === 'EVALUATION_FAILED'
                ? 'error.main'
                : event.eventType === 'EVALUATION_COMPLETED'
                ? 'success.main'
                : event.eventType === 'BATCH_PROGRESS'
                ? 'info.main'
                : 'text.primary',
          }}
        >
          {event.eventType}
        </Typography>
      </Box>

      <Box sx={{ flex: '0 0 150px', mr: 2 }}>
        <Typography variant="body2" color="text.secondary">
          {getMeasureId(event)}
        </Typography>
      </Box>

      <Box sx={{ flex: '1' }}>
        <Typography variant="body2" color="text.secondary">
          {getPatientId(event)}
        </Typography>
      </Box>
    </Box>
  );
};

/**
 * VirtualizedEventList component
 * Uses react-window v2 for efficient rendering of large lists
 */
export const VirtualizedEventList = ({
  events,
  onEventClick,
  height = 400,
  itemHeight = 60,
}: VirtualizedEventListProps) => {
  // Handle empty state
  if (events.length === 0) {
    return (
      <Paper
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          height,
          padding: 4,
          backgroundColor: 'background.default',
        }}
      >
        <EventNoteIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          No events to display
        </Typography>
        <Typography variant="body2" color="text.disabled" sx={{ mt: 1 }}>
          Events will appear here when available
        </Typography>
      </Paper>
    );
  }

  return (
    <Box sx={{ width: '100%', height: `${height}px`, backgroundColor: 'background.paper' }}>
      <List
        defaultHeight={height}
        rowCount={events.length}
        rowHeight={itemHeight}
        rowComponent={EventRow}
        rowProps={{
          events,
          onClick: onEventClick,
        }}
        role="list"
        aria-label="Event list"
      />
    </Box>
  );
};

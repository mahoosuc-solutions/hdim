/**
 * SimpleEventFilter component
 * Simplified filtering UI without MUI Select to avoid infinite loop issues
 * Uses buttons and checkboxes instead of complex Select components
 */

import React, { useState } from 'react';
import {
  Box,
  Chip,
  Button,
  Typography,
  Badge,
  Collapse,
  IconButton,
  Paper,
  Stack,
  FormGroup,
  FormControlLabel,
  Checkbox,
  Divider,
} from '@mui/material';
import {
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  FilterList as FilterListIcon,
  Clear as ClearIcon,
  EventBusy as EventBusyIcon,
} from '@mui/icons-material';
import { EventType } from '../types/events';
import { useEvaluationStore } from '../store/evaluationStore';
import type { EventFilters } from '../store/evaluationStore';

// Re-export for backward compatibility
export type { EventFilters };

interface SimpleEventFilterProps {
  availableEventTypes: EventType[];
  availableMeasures: string[];
}

const SimpleEventFilterComponent: React.FC<SimpleEventFilterProps> = ({
  availableEventTypes,
  availableMeasures,
}) => {
  const [expanded, setExpanded] = useState(true);
  const [showMeasures, setShowMeasures] = useState(false);

  // Get filters from store (single source of truth)
  const filters = useEvaluationStore(state => state.eventFilters);
  const setFilters = useEvaluationStore(state => state.setEventFilters);
  const updateFilter = useEvaluationStore(state => state.updateEventFilter);

  /**
   * Toggle an event type in the selection
   */
  const handleEventTypeToggle = (eventType: EventType) => {
    const isSelected = filters.eventTypes.includes(eventType);
    const newEventTypes = isSelected
      ? filters.eventTypes.filter((type) => type !== eventType)
      : [...filters.eventTypes, eventType];

    updateFilter('eventTypes', newEventTypes);
  };

  /**
   * Handle measure selection (checkbox)
   */
  const handleMeasureToggle = (measureId: string) => {
    if (filters.measureId === measureId) {
      // Deselect if already selected
      updateFilter('measureId', null);
    } else {
      // Select this measure
      updateFilter('measureId', measureId);
    }
  };

  /**
   * Quick filter: All
   */
  const handleAllFilter = () => {
    setFilters({
      eventTypes: [],
      measureId: null,
      statusFilter: 'all',
    });
  };

  /**
   * Quick filter: Errors Only
   */
  const handleErrorsOnlyFilter = () => {
    setFilters({
      ...filters,
      eventTypes: [EventType.EVALUATION_FAILED],
      statusFilter: 'errors',
    });
  };

  /**
   * Quick filter: Success Only
   */
  const handleSuccessOnlyFilter = () => {
    setFilters({
      ...filters,
      eventTypes: [EventType.EVALUATION_COMPLETED],
      statusFilter: 'success',
    });
  };

  /**
   * Clear all filters
   */
  const handleClearAll = () => {
    setFilters({
      eventTypes: [],
      measureId: null,
      statusFilter: 'all',
    });
  };

  /**
   * Calculate active filter count
   */
  const activeFilterCount =
    filters.eventTypes.length + (filters.measureId ? 1 : 0);

  return (
    <Paper elevation={2} sx={{ p: 2, mb: 2 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
        <FilterListIcon sx={{ mr: 1 }} />
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          Event Filters
        </Typography>
        <Badge badgeContent={activeFilterCount} color="primary" sx={{ mr: 2 }}>
          <Box sx={{ width: 24, height: 24 }} />
        </Badge>
        <IconButton
          onClick={() => setExpanded(!expanded)}
          aria-label={expanded ? 'Collapse filters' : 'Expand filters'}
          size="small"
        >
          {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
        </IconButton>
      </Box>

      <Collapse in={expanded}>
        <Stack spacing={2}>
          {/* Quick Filter Buttons */}
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button
              size="small"
              variant={filters.statusFilter === 'all' ? 'contained' : 'outlined'}
              onClick={handleAllFilter}
            >
              All
            </Button>
            <Button
              size="small"
              variant={filters.statusFilter === 'errors' ? 'contained' : 'outlined'}
              color="error"
              onClick={handleErrorsOnlyFilter}
            >
              Errors Only
            </Button>
            <Button
              size="small"
              variant={filters.statusFilter === 'success' ? 'contained' : 'outlined'}
              color="success"
              onClick={handleSuccessOnlyFilter}
            >
              Success Only
            </Button>
            <Button
              size="small"
              variant="outlined"
              color="inherit"
              onClick={handleClearAll}
              disabled={activeFilterCount === 0}
              startIcon={<ClearIcon />}
            >
              Clear All
            </Button>
          </Box>

          <Divider />

          {/* Event Type Chips */}
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Event Types
            </Typography>
            {availableEventTypes.length === 0 ? (
              <Stack direction="row" spacing={1} alignItems="center" color="text.secondary">
                <EventBusyIcon fontSize="small" />
                <Typography variant="body2">No event types available</Typography>
              </Stack>
            ) : (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {availableEventTypes.map((eventType) => (
                  <Chip
                    key={eventType}
                    label={eventType}
                    onClick={() => handleEventTypeToggle(eventType)}
                    color={
                      filters.eventTypes.includes(eventType) ? 'primary' : 'default'
                    }
                    variant={
                      filters.eventTypes.includes(eventType) ? 'filled' : 'outlined'
                    }
                    size="small"
                  />
                ))}
              </Box>
            )}
          </Box>

          {/* Measure Filter - Expandable Checkboxes */}
          {availableMeasures.length > 0 && (
            <>
              <Divider />
              <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Typography variant="subtitle2">
                    Measures {filters.measureId && `(1 selected)`}
                  </Typography>
                  <Button
                    size="small"
                    onClick={() => setShowMeasures(!showMeasures)}
                    endIcon={showMeasures ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                  >
                    {showMeasures ? 'Hide' : 'Show'}
                  </Button>
                </Box>
                <Collapse in={showMeasures}>
                  <FormGroup sx={{ mt: 1, ml: 2 }}>
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={filters.measureId === null}
                          onChange={() => updateFilter('measureId', null)}
                          size="small"
                        />
                      }
                      label="All Measures"
                    />
                    {availableMeasures.map((measure) => (
                      <FormControlLabel
                        key={measure}
                        control={
                          <Checkbox
                            checked={filters.measureId === measure}
                            onChange={() => handleMeasureToggle(measure)}
                            size="small"
                          />
                        }
                        label={measure}
                      />
                    ))}
                  </FormGroup>
                </Collapse>
              </Box>
            </>
          )}

          {/* Active Filters Summary */}
          {activeFilterCount > 0 && (
            <>
              <Divider />
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Active Filters: {activeFilterCount}
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 0.5 }}>
                  {filters.eventTypes.map((type) => (
                    <Chip
                      key={type}
                      label={type}
                      size="small"
                      onDelete={() => handleEventTypeToggle(type)}
                      color="primary"
                      variant="outlined"
                    />
                  ))}
                  {filters.measureId && (
                    <Chip
                      label={`Measure: ${filters.measureId}`}
                      size="small"
                      onDelete={() => updateFilter('measureId', null)}
                      color="secondary"
                      variant="outlined"
                    />
                  )}
                </Box>
              </Box>
            </>
          )}
        </Stack>
      </Collapse>
    </Paper>
  );
};

// Memoize the component to prevent unnecessary re-renders when props haven't changed
export const SimpleEventFilter = React.memo(SimpleEventFilterComponent);
SimpleEventFilter.displayName = 'SimpleEventFilter';

export default SimpleEventFilter;

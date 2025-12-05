import React, { useEffect, useState } from 'react';
import { Box, Button, Chip, Typography, Badge, IconButton, Paper, Stack, Divider } from '@mui/material';
import { ExpandMore, ExpandLess, FilterList, Clear } from '@mui/icons-material';
import { EventType } from '../types/events';

export interface EventFilters {
  eventTypes: EventType[];
  measureId: string | null;
  statusFilter: 'all' | 'errors' | 'success';
}

export interface EventFilterProps {
  availableEventTypes: EventType[];
  availableMeasures: string[];
  onFilterChange: (filters: EventFilters) => void;
}

const DEFAULT_FILTERS: EventFilters = {
  eventTypes: [],
  measureId: null,
  statusFilter: 'all',
};

const STORAGE_KEY = 'eventFilters';

export const EventFilter: React.FC<EventFilterProps> = ({
  availableEventTypes,
  availableMeasures,
  onFilterChange,
}) => {
  const [expanded, setExpanded] = useState(true);
  const [filters, setFilters] = useState<EventFilters>(DEFAULT_FILTERS);

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        setFilters({ ...DEFAULT_FILTERS, ...JSON.parse(stored) });
      }
    } catch {
      // ignore bad data
    }
  }, []);

  useEffect(() => {
    onFilterChange(filters);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(filters));
  }, [filters, onFilterChange]);

  const toggleEventType = (eventType: EventType) => {
    setFilters((prev) => {
      const exists = prev.eventTypes.includes(eventType);
      const nextTypes = exists ? prev.eventTypes.filter((t) => t !== eventType) : [...prev.eventTypes, eventType];
      return { ...prev, eventTypes: nextTypes };
    });
  };

  const handleMeasureChange = (value: string) => {
    const nextFilters = { ...filters, measureId: value || null };
    setFilters(nextFilters);
    onFilterChange(nextFilters);
  };

  const quickFilter = (status: EventFilters['statusFilter'], eventTypes: EventType[] = []) => {
    setFilters({ ...filters, statusFilter: status, eventTypes });
  };

  const clearAll = () => setFilters(DEFAULT_FILTERS);

  const activeFilterCount = filters.eventTypes.length + (filters.measureId ? 1 : 0);

  return (
    <Paper elevation={2} sx={{ p: 2 }} data-testid="event-filter">
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
        <FilterList sx={{ mr: 1 }} />
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          Event Filters
        </Typography>
        <Badge badgeContent={activeFilterCount} color="primary" sx={{ mr: 2 }}>
          <Box sx={{ width: 24, height: 24 }} />
        </Badge>
        <IconButton aria-label={expanded ? 'Collapse filters' : 'Expand filters'} onClick={() => setExpanded(!expanded)} size="small">
          {expanded ? <ExpandLess /> : <ExpandMore />}
        </IconButton>
      </Box>

      {expanded && (
        <Stack spacing={2}>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button size="small" variant={filters.statusFilter === 'all' ? 'contained' : 'outlined'} onClick={() => quickFilter('all', [])}>
              All
            </Button>
            <Button size="small" variant={filters.statusFilter === 'errors' ? 'contained' : 'outlined'} color="error" onClick={() => quickFilter('errors', [EventType.EVALUATION_FAILED])}>
              Errors Only
            </Button>
            <Button size="small" variant={filters.statusFilter === 'success' ? 'contained' : 'outlined'} color="success" onClick={() => quickFilter('success', [EventType.EVALUATION_COMPLETED])}>
              Success Only
            </Button>
            <Button size="small" variant="outlined" color="inherit" onClick={clearAll} startIcon={<Clear />} disabled={activeFilterCount === 0 && filters.statusFilter === 'all'}>
              Clear All Filters
            </Button>
          </Box>

          <Divider />

          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Event Types
            </Typography>
            {availableEventTypes.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No event types available
              </Typography>
            ) : (
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                {availableEventTypes.map((type) => {
                  const selected = filters.eventTypes.includes(type);
                  return (
                    <Chip
                      key={type}
                      label={type}
                      color={selected ? 'primary' : 'default'}
                      onClick={() => toggleEventType(type)}
                      variant={selected ? 'filled' : 'outlined'}
                    />
                  );
                })}
              </Box>
            )}
          </Box>

          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Measure
            </Typography>
            <select aria-label="Measure" value={filters.measureId ?? ''} onChange={(e) => handleMeasureChange(e.target.value)}>
              <option value="">All Measures</option>
              {availableMeasures.map((measure) => (
                <option key={measure} value={measure} onClick={() => handleMeasureChange(measure)}>
                  {measure}
                </option>
              ))}
            </select>
            {availableMeasures.length === 0 && (
              <Typography variant="body2" color="text.secondary">
                No measures available
              </Typography>
            )}
          </Box>
        </Stack>
      )}
    </Paper>
  );
};

export default EventFilter;

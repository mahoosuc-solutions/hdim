/**
 * Sequence List Component
 * Displays email sequences with performance metrics
 */

import { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  GridLegacy as Grid,
  Typography,
  Chip,
  IconButton,
  Button,
  CircularProgress,
  Alert,
  Tooltip,
  Switch,
  LinearProgress,
} from '@mui/material';
import {
  Add as AddIcon,
  Email as EmailIcon,
  PlayArrow as PlayIcon,
  Pause as PauseIcon,
  Edit as EditIcon,
  Analytics as AnalyticsIcon,
  OpenInNew as OpenIcon,
} from '@mui/icons-material';
import { salesService } from '../../services/sales.service';
import type { EmailSequence, SequenceAnalytics, SequenceType, PageResponse } from '../../types/sales';

interface SequenceListProps {
  tenantId: string;
}

const typeColors: Record<SequenceType, 'primary' | 'secondary' | 'success' | 'warning' | 'info'> = {
  NURTURE: 'primary',
  FOLLOW_UP: 'info',
  ONBOARDING: 'success',
  RE_ENGAGEMENT: 'warning',
  PROMOTION: 'secondary',
  EVENT: 'info',
  TRIAL: 'warning',
  WELCOME: 'success',
  CUSTOM: 'primary',
};

interface SequenceCardProps {
  sequence: EmailSequence;
  analytics?: SequenceAnalytics;
  onToggleActive: (sequence: EmailSequence) => void;
  onEdit: (sequence: EmailSequence) => void;
}

function SequenceCard({ sequence, analytics, onToggleActive, onEdit }: SequenceCardProps) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <EmailIcon color="primary" />
            <Box>
              <Typography variant="subtitle1" fontWeight="medium">
                {sequence.name}
              </Typography>
              <Chip
                label={sequence.type.replace('_', ' ')}
                size="small"
                color={typeColors[sequence.type]}
                sx={{ mt: 0.5 }}
              />
            </Box>
          </Box>
          <Switch
            checked={sequence.active}
            onChange={() => onToggleActive(sequence)}
            size="small"
          />
        </Box>

        {sequence.description && (
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {sequence.description}
          </Typography>
        )}

        <Box sx={{ mb: 2 }}>
          <Typography variant="caption" color="text.secondary">
            {sequence.steps.length} steps
          </Typography>
        </Box>

        {analytics && (
          <Box sx={{ mb: 2 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption">Open Rate</Typography>
              <Typography variant="caption" fontWeight="medium">
                {analytics.openRate.toFixed(1)}%
              </Typography>
            </Box>
            <LinearProgress
              variant="determinate"
              value={analytics.openRate}
              sx={{ height: 6, borderRadius: 3, mb: 1 }}
            />

            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
              <Typography variant="caption">Click Rate</Typography>
              <Typography variant="caption" fontWeight="medium">
                {analytics.clickRate.toFixed(1)}%
              </Typography>
            </Box>
            <LinearProgress
              variant="determinate"
              value={analytics.clickRate}
              color="success"
              sx={{ height: 6, borderRadius: 3 }}
            />

            <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Enrolled
                </Typography>
                <Typography variant="body2" fontWeight="medium">
                  {analytics.totalEnrollments}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Active
                </Typography>
                <Typography variant="body2" fontWeight="medium">
                  {analytics.activeEnrollments}
                </Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Sent
                </Typography>
                <Typography variant="body2" fontWeight="medium">
                  {analytics.totalEmailsSent}
                </Typography>
              </Box>
            </Box>
          </Box>
        )}

        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
          <Tooltip title="View Analytics">
            <IconButton size="small">
              <AnalyticsIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="Edit Sequence">
            <IconButton size="small" onClick={() => onEdit(sequence)}>
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      </CardContent>
    </Card>
  );
}

export function SequenceList({ tenantId }: SequenceListProps) {
  const [sequences, setSequences] = useState<EmailSequence[]>([]);
  const [analytics, setAnalytics] = useState<Record<string, SequenceAnalytics>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadSequences();
  }, [tenantId]);

  const loadSequences = async () => {
    try {
      setLoading(true);
      setError(null);
      const response: PageResponse<EmailSequence> = await salesService.getSequences(
        { tenantId },
        0,
        50
      );
      setSequences(response.content);

      // Load analytics for each sequence
      const analyticsMap: Record<string, SequenceAnalytics> = {};
      for (const seq of response.content) {
        if (seq.id) {
          try {
            const seqAnalytics = await salesService.getSequenceAnalytics({ tenantId }, seq.id);
            analyticsMap[seq.id] = seqAnalytics;
          } catch {
            // Ignore analytics errors
          }
        }
      }
      setAnalytics(analyticsMap);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load sequences');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleActive = async (sequence: EmailSequence) => {
    if (!sequence.id) return;
    try {
      if (sequence.active) {
        // Deactivate - would need deactivate endpoint
        await salesService.updateSequence({ tenantId }, sequence.id, { active: false });
      } else {
        await salesService.activateSequence({ tenantId }, sequence.id);
      }
      loadSequences();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update sequence');
    }
  };

  const handleEdit = (sequence: EmailSequence) => {
    // Would open edit dialog
    console.log('Edit sequence:', sequence);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h5">Email Sequences</Typography>
          <Typography variant="body2" color="text.secondary">
            Automate email outreach with sequences
          </Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />}>
          Create Sequence
        </Button>
      </Box>

      {/* Error */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Sequence Grid */}
      {sequences.length === 0 ? (
        <Card>
          <CardContent sx={{ textAlign: 'center', py: 6 }}>
            <EmailIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" gutterBottom>
              No sequences yet
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Create your first email sequence to automate outreach
            </Typography>
            <Button variant="contained" startIcon={<AddIcon />}>
              Create Sequence
            </Button>
          </CardContent>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {sequences.map((sequence) => (
            <Grid item xs={12} sm={6} md={4} key={sequence.id}>
              <SequenceCard
                sequence={sequence}
                analytics={sequence.id ? analytics[sequence.id] : undefined}
                onToggleActive={handleToggleActive}
                onEdit={handleEdit}
              />
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  );
}

export default SequenceList;

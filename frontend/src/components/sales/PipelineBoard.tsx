/**
 * Pipeline Board Component
 * Kanban-style view of opportunities by stage
 */

import { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  CircularProgress,
  Alert,
  Paper,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
} from '@mui/material';
import {
  Warning as WarningIcon,
  AttachMoney as MoneyIcon,
  CalendarToday as CalendarIcon,
  Business as BusinessIcon,
  TrendingUp as TrendingUpIcon,
} from '@mui/icons-material';
import { salesService } from '../../services/sales.service';
import type { PipelineKanbanDTO, KanbanColumn, KanbanCard, OpportunityStage, StageTransitionRequest } from '../../types/sales';

interface PipelineBoardProps {
  tenantId: string;
}

const stageColors: Record<OpportunityStage, string> = {
  DISCOVERY: '#2196f3',
  DEMO: '#9c27b0',
  PROPOSAL: '#ff9800',
  NEGOTIATION: '#f44336',
  CLOSED_WON: '#4caf50',
  CLOSED_LOST: '#9e9e9e',
};

interface OpportunityCardProps {
  card: KanbanCard;
  onStageChange: (card: KanbanCard) => void;
}

function OpportunityCard({ card, onStageChange }: OpportunityCardProps) {
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
    }).format(value);
  };

  return (
    <Paper
      elevation={1}
      sx={{
        p: 2,
        mb: 1.5,
        cursor: 'pointer',
        transition: 'all 0.2s',
        border: card.isAtRisk ? '2px solid' : 'none',
        borderColor: card.isAtRisk ? 'warning.main' : 'transparent',
        '&:hover': {
          elevation: 4,
          transform: 'translateY(-2px)',
        },
      }}
      onClick={() => onStageChange(card)}
    >
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
        <Typography variant="subtitle2" fontWeight="medium" sx={{ pr: 1 }}>
          {card.opportunityName}
        </Typography>
        {card.isAtRisk && (
          <Tooltip title={card.riskReason || 'At risk'}>
            <WarningIcon color="warning" fontSize="small" />
          </Tooltip>
        )}
      </Box>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 1 }}>
        <BusinessIcon fontSize="small" color="action" />
        <Typography variant="caption" color="text.secondary">
          {card.accountName}
        </Typography>
      </Box>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1.5 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <MoneyIcon fontSize="small" color="success" />
          <Typography variant="body2" fontWeight="medium" color="success.main">
            {formatCurrency(card.amount)}
          </Typography>
        </Box>
        <Chip
          label={`${card.probability}%`}
          size="small"
          color={card.probability >= 70 ? 'success' : card.probability >= 40 ? 'warning' : 'default'}
        />
      </Box>

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <CalendarIcon fontSize="small" color="action" />
          <Typography variant="caption" color="text.secondary">
            {new Date(card.expectedCloseDate).toLocaleDateString()}
          </Typography>
        </Box>
        <Typography variant="caption" color="text.secondary">
          {card.daysInStage}d in stage
        </Typography>
      </Box>
    </Paper>
  );
}

interface PipelineColumnProps {
  column: KanbanColumn;
  onCardClick: (card: KanbanCard) => void;
}

function PipelineColumn({ column, onCardClick }: PipelineColumnProps) {
  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
    }).format(value);
  };

  return (
    <Box
      sx={{
        minWidth: 280,
        maxWidth: 320,
        flex: '1 0 auto',
        display: 'flex',
        flexDirection: 'column',
        bgcolor: 'grey.50',
        borderRadius: 2,
        p: 1.5,
        height: 'fit-content',
        maxHeight: '100%',
      }}
    >
      <Box sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
          <Typography variant="subtitle1" fontWeight="bold">
            {column.stageLabel}
          </Typography>
          <Chip
            label={column.count}
            size="small"
            sx={{ bgcolor: stageColors[column.stage], color: 'white' }}
          />
        </Box>
        <Typography variant="caption" color="text.secondary">
          {formatCurrency(column.totalValue)}
        </Typography>
      </Box>

      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {column.cards.map((card) => (
          <OpportunityCard key={card.opportunityId} card={card} onStageChange={onCardClick} />
        ))}
        {column.cards.length === 0 && (
          <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 2 }}>
            No opportunities
          </Typography>
        )}
      </Box>
    </Box>
  );
}

export function PipelineBoard({ tenantId }: PipelineBoardProps) {
  const [pipeline, setPipeline] = useState<PipelineKanbanDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCard, setSelectedCard] = useState<KanbanCard | null>(null);
  const [moveDialogOpen, setMoveDialogOpen] = useState(false);
  const [newStage, setNewStage] = useState<OpportunityStage | ''>('');
  const [lostReason, setLostReason] = useState('');

  useEffect(() => {
    loadPipeline();
  }, [tenantId]);

  const loadPipeline = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await salesService.getPipelineKanban({ tenantId });
      setPipeline(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load pipeline');
    } finally {
      setLoading(false);
    }
  };

  const handleCardClick = (card: KanbanCard) => {
    setSelectedCard(card);
    setMoveDialogOpen(true);
    setNewStage('');
    setLostReason('');
  };

  const handleMoveConfirm = async () => {
    if (!selectedCard || !newStage) return;

    try {
      const request: StageTransitionRequest = {
        newStage: newStage as OpportunityStage,
        lostReason: newStage === 'CLOSED_LOST' ? lostReason : undefined,
        createFollowUp: true,
        followUpDays: 3,
      };
      await salesService.moveOpportunityStage({ tenantId }, selectedCard.opportunityId, request);
      loadPipeline();
      setMoveDialogOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to move opportunity');
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
    }).format(value);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }}>
        {error}
      </Alert>
    );
  }

  if (!pipeline) {
    return (
      <Alert severity="info">
        No pipeline data available
      </Alert>
    );
  }

  return (
    <Box>
      {/* Summary */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Total Pipeline Value
              </Typography>
              <Typography variant="h5" color="primary.main">
                {formatCurrency(pipeline.summary.totalValue)}
              </Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Weighted Value
              </Typography>
              <Typography variant="h5" color="success.main">
                {formatCurrency(pipeline.summary.weightedValue)}
              </Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Opportunities
              </Typography>
              <Typography variant="h5">
                {pipeline.summary.opportunityCount}
              </Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Avg Deal Size
              </Typography>
              <Typography variant="h5">
                {formatCurrency(pipeline.summary.avgDealSize)}
              </Typography>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Avg Probability
              </Typography>
              <Typography variant="h5">
                {pipeline.summary.avgProbability.toFixed(0)}%
              </Typography>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Kanban Board */}
      <Box
        sx={{
          display: 'flex',
          gap: 2,
          overflowX: 'auto',
          pb: 2,
          minHeight: 500,
        }}
      >
        {pipeline.columns.map((column) => (
          <PipelineColumn
            key={column.stage}
            column={column}
            onCardClick={handleCardClick}
          />
        ))}
      </Box>

      {/* Move Stage Dialog */}
      <Dialog open={moveDialogOpen} onClose={() => setMoveDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Move Opportunity</DialogTitle>
        <DialogContent>
          {selectedCard && (
            <Box sx={{ pt: 1 }}>
              <Typography variant="subtitle2" gutterBottom>
                {selectedCard.opportunityName}
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {selectedCard.accountName} - {formatCurrency(selectedCard.amount)}
              </Typography>

              <FormControl fullWidth sx={{ mt: 2 }}>
                <InputLabel>New Stage</InputLabel>
                <Select
                  value={newStage}
                  label="New Stage"
                  onChange={(e) => setNewStage(e.target.value as OpportunityStage)}
                >
                  <MenuItem value="DISCOVERY">Discovery</MenuItem>
                  <MenuItem value="DEMO">Demo</MenuItem>
                  <MenuItem value="PROPOSAL">Proposal</MenuItem>
                  <MenuItem value="NEGOTIATION">Negotiation</MenuItem>
                  <MenuItem value="CLOSED_WON">Closed Won</MenuItem>
                  <MenuItem value="CLOSED_LOST">Closed Lost</MenuItem>
                </Select>
              </FormControl>

              {newStage === 'CLOSED_LOST' && (
                <TextField
                  fullWidth
                  label="Lost Reason"
                  value={lostReason}
                  onChange={(e) => setLostReason(e.target.value)}
                  sx={{ mt: 2 }}
                  multiline
                  rows={2}
                />
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMoveDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleMoveConfirm}
            disabled={!newStage || (newStage === 'CLOSED_LOST' && !lostReason)}
          >
            Move
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default PipelineBoard;

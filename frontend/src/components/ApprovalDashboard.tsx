/**
 * ApprovalDashboard Component
 * Human-in-the-Loop approval interface for reviewing and managing pending approvals
 */

import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  Stack,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  IconButton,
  Tooltip,
  CircularProgress,
  Divider,
  Tabs,
  Tab,
  Badge,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  GridLegacy as Grid,
} from '@mui/material';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined';
import EscalatorWarningIcon from '@mui/icons-material/EscalatorWarning';
import VisibilityIcon from '@mui/icons-material/Visibility';
import RefreshIcon from '@mui/icons-material/Refresh';
import AssignmentIndIcon from '@mui/icons-material/AssignmentInd';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

import { approvalService } from '../services/approval.service';
import { ApprovalAnalytics } from './ApprovalAnalytics';
import type {
  ApprovalRequest,
  ApprovalStats,
  ApprovalHistory,
  RiskLevel,
  ApprovalStatus,
  RequestType,
} from '../types/approval';

interface ApprovalDashboardProps {
  tenantId: string;
  userId: string;
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ p: 2 }}>{children}</Box>}
    </div>
  );
}

function getRiskLevelColor(level: RiskLevel): 'error' | 'warning' | 'info' | 'success' {
  switch (level) {
    case 'CRITICAL':
      return 'error';
    case 'HIGH':
      return 'error';
    case 'MEDIUM':
      return 'warning';
    case 'LOW':
      return 'success';
    default:
      return 'info';
  }
}

function getRiskLevelIcon(level: RiskLevel) {
  switch (level) {
    case 'CRITICAL':
      return <ErrorOutlineIcon />;
    case 'HIGH':
      return <WarningAmberIcon />;
    case 'MEDIUM':
      return <InfoOutlinedIcon />;
    default:
      return null;
  }
}

function getStatusColor(
  status: ApprovalStatus
): 'default' | 'primary' | 'success' | 'error' | 'warning' | 'info' {
  switch (status) {
    case 'APPROVED':
      return 'success';
    case 'REJECTED':
      return 'error';
    case 'PENDING':
      return 'warning';
    case 'ASSIGNED':
      return 'primary';
    case 'EXPIRED':
      return 'default';
    case 'ESCALATED':
      return 'info';
    default:
      return 'default';
  }
}

function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return 'just now';
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  return `${diffDays}d ago`;
}

function formatTimeRemaining(expiresAt: string | null): string | null {
  if (!expiresAt) return null;
  const expires = new Date(expiresAt);
  const now = new Date();
  const diffMs = expires.getTime() - now.getTime();
  if (diffMs <= 0) return 'Expired';
  const diffHours = Math.floor(diffMs / 3600000);
  const diffMins = Math.floor((diffMs % 3600000) / 60000);
  if (diffHours > 24) return `${Math.floor(diffHours / 24)}d ${diffHours % 24}h`;
  return `${diffHours}h ${diffMins}m`;
}

export default function ApprovalDashboard({ tenantId, userId }: ApprovalDashboardProps) {
  const [tabValue, setTabValue] = useState(0);
  const [approvals, setApprovals] = useState<ApprovalRequest[]>([]);
  const [stats, setStats] = useState<ApprovalStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);

  // Dialog states
  const [selectedApproval, setSelectedApproval] = useState<ApprovalRequest | null>(null);
  const [detailsOpen, setDetailsOpen] = useState(false);
  const [decisionOpen, setDecisionOpen] = useState(false);
  const [decisionType, setDecisionType] = useState<'approve' | 'reject' | 'escalate'>('approve');
  const [decisionReason, setDecisionReason] = useState('');
  const [escalateTo, setEscalateTo] = useState('');
  const [historyData, setHistoryData] = useState<ApprovalHistory[]>([]);
  const [processingAction, setProcessingAction] = useState(false);

  const apiOptions = { tenantId, userId };

  const fetchApprovals = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response =
        tabValue === 0
          ? await approvalService.getMyApprovals(apiOptions, page, rowsPerPage)
          : await approvalService.getPendingApprovals(apiOptions, page, rowsPerPage);

      setApprovals(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch approvals');
    } finally {
      setLoading(false);
    }
  }, [tenantId, userId, tabValue, page, rowsPerPage]);

  const fetchStats = useCallback(async () => {
    try {
      const data = await approvalService.getStatistics(apiOptions);
      setStats(data);
    } catch (err) {
      console.error('Failed to fetch stats:', err);
    }
  }, [tenantId, userId]);

  useEffect(() => {
    fetchApprovals();
    fetchStats();
  }, [fetchApprovals, fetchStats]);

  const handleViewDetails = async (approval: ApprovalRequest) => {
    setSelectedApproval(approval);
    try {
      const history = await approvalService.getApprovalHistory(apiOptions, approval.id);
      setHistoryData(history);
    } catch (err) {
      console.error('Failed to fetch history:', err);
      setHistoryData([]);
    }
    setDetailsOpen(true);
  };

  const handleOpenDecision = (approval: ApprovalRequest, type: 'approve' | 'reject' | 'escalate') => {
    setSelectedApproval(approval);
    setDecisionType(type);
    setDecisionReason('');
    setEscalateTo('');
    setDecisionOpen(true);
  };

  const handleSubmitDecision = async () => {
    if (!selectedApproval) return;
    setProcessingAction(true);
    try {
      if (decisionType === 'approve') {
        await approvalService.approve(apiOptions, selectedApproval.id, { reason: decisionReason });
      } else if (decisionType === 'reject') {
        await approvalService.reject(apiOptions, selectedApproval.id, { reason: decisionReason });
      } else if (decisionType === 'escalate') {
        await approvalService.escalate(apiOptions, selectedApproval.id, {
          escalatedTo: escalateTo,
          reason: decisionReason,
        });
      }
      setDecisionOpen(false);
      fetchApprovals();
      fetchStats();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process decision');
    } finally {
      setProcessingAction(false);
    }
  };

  const handleChangePage = (_event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  return (
    <Box sx={{ width: '100%', p: 2 }}>
      {/* Stats Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Pending
              </Typography>
              <Typography variant="h4">{stats?.pending ?? '-'}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Approval Rate
              </Typography>
              <Typography variant="h4">
                {stats ? `${(stats.approvalRate * 100).toFixed(0)}%` : '-'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Avg Response Time
              </Typography>
              <Typography variant="h4">
                {stats ? `${stats.avgResponseTimeHours.toFixed(1)}h` : '-'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Escalations
              </Typography>
              <Typography variant="h4">{stats?.escalated ?? '-'}</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs and Table */}
      <Paper sx={{ width: '100%', mb: 2 }}>
        <Box sx={{ borderBottom: 1, borderColor: 'divider', display: 'flex', alignItems: 'center' }}>
          <Tabs value={tabValue} onChange={(_e, v) => setTabValue(v)} sx={{ flexGrow: 1 }}>
            <Tab
              label={
                <Badge badgeContent={stats?.assigned ?? 0} color="primary">
                  My Approvals
                </Badge>
              }
            />
            <Tab
              label={
                <Badge badgeContent={stats?.pending ?? 0} color="warning">
                  Pending Queue
                </Badge>
              }
            />
            <Tab label="Analytics" />
          </Tabs>
          <IconButton onClick={() => { fetchApprovals(); fetchStats(); }} sx={{ mr: 2 }}>
            <RefreshIcon />
          </IconButton>
        </Box>

        {error && (
          <Alert severity="error" sx={{ m: 2 }}>
            {error}
          </Alert>
        )}

        {tabValue === 2 ? (
          <Box sx={{ p: 2 }}>
            <ApprovalAnalytics tenantId={tenantId} />
          </Box>
        ) : loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Risk</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Entity</TableCell>
                    <TableCell>Requested</TableCell>
                    <TableCell>Expires</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {approvals.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        <Typography color="textSecondary" sx={{ py: 4 }}>
                          No approvals found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    approvals.map((approval) => (
                      <TableRow key={approval.id} hover>
                        <TableCell>
                          <Chip
                            icon={getRiskLevelIcon(approval.riskLevel) || undefined}
                            label={approval.riskLevel}
                            color={getRiskLevelColor(approval.riskLevel)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">{approval.requestType}</Typography>
                          <Typography variant="caption" color="textSecondary">
                            {approval.actionRequested}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">{approval.entityType}</Typography>
                          {approval.entityId && (
                            <Typography variant="caption" color="textSecondary">
                              {approval.entityId}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {formatTimeAgo(approval.requestedAt)}
                          </Typography>
                          <Typography variant="caption" color="textSecondary">
                            by {approval.requestedBy}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          {approval.expiresAt && (
                            <Typography
                              variant="body2"
                              color={
                                formatTimeRemaining(approval.expiresAt) === 'Expired'
                                  ? 'error'
                                  : 'textPrimary'
                              }
                            >
                              {formatTimeRemaining(approval.expiresAt)}
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={approval.status}
                            color={getStatusColor(approval.status)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell align="right">
                          <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                            <Tooltip title="View Details">
                              <IconButton size="small" onClick={() => handleViewDetails(approval)}>
                                <VisibilityIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            {(approval.status === 'PENDING' || approval.status === 'ASSIGNED') && (
                              <>
                                <Tooltip title="Approve">
                                  <IconButton
                                    size="small"
                                    color="success"
                                    onClick={() => handleOpenDecision(approval, 'approve')}
                                  >
                                    <CheckCircleOutlineIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                                <Tooltip title="Reject">
                                  <IconButton
                                    size="small"
                                    color="error"
                                    onClick={() => handleOpenDecision(approval, 'reject')}
                                  >
                                    <CancelOutlinedIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                                <Tooltip title="Escalate">
                                  <IconButton
                                    size="small"
                                    color="warning"
                                    onClick={() => handleOpenDecision(approval, 'escalate')}
                                  >
                                    <EscalatorWarningIcon fontSize="small" />
                                  </IconButton>
                                </Tooltip>
                              </>
                            )}
                          </Stack>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
            <TablePagination
              rowsPerPageOptions={[5, 10, 25]}
              component="div"
              count={totalElements}
              rowsPerPage={rowsPerPage}
              page={page}
              onPageChange={handleChangePage}
              onRowsPerPageChange={handleChangeRowsPerPage}
            />
          </>
        )}
      </Paper>

      {/* Details Dialog */}
      <Dialog open={detailsOpen} onClose={() => setDetailsOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          Approval Request Details
          {selectedApproval && (
            <Chip
              label={selectedApproval.riskLevel}
              color={getRiskLevelColor(selectedApproval.riskLevel)}
              size="small"
              sx={{ ml: 2 }}
            />
          )}
        </DialogTitle>
        <DialogContent dividers>
          {selectedApproval && (
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  Request Type
                </Typography>
                <Typography>{selectedApproval.requestType}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  Action Requested
                </Typography>
                <Typography>{selectedApproval.actionRequested}</Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  Entity
                </Typography>
                <Typography>
                  {selectedApproval.entityType}
                  {selectedApproval.entityId && ` (${selectedApproval.entityId})`}
                </Typography>
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" color="textSecondary">
                  Requested By
                </Typography>
                <Typography>{selectedApproval.requestedBy}</Typography>
              </Grid>
              {selectedApproval.confidenceScore !== null && (
                <Grid item xs={12} md={6}>
                  <Typography variant="subtitle2" color="textSecondary">
                    AI Confidence
                  </Typography>
                  <Typography>
                    {(selectedApproval.confidenceScore * 100).toFixed(0)}%
                  </Typography>
                </Grid>
              )}
              <Grid item xs={12}>
                <Divider sx={{ my: 1 }} />
                <Typography variant="subtitle2" color="textSecondary">
                  Payload
                </Typography>
                <Paper variant="outlined" sx={{ p: 1, mt: 1, bgcolor: 'grey.50', maxHeight: 200, overflow: 'auto' }}>
                  <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word', fontSize: '0.85rem' }}>
                    {JSON.stringify(selectedApproval.payload, null, 2)}
                  </pre>
                </Paper>
              </Grid>
              {historyData.length > 0 && (
                <Grid item xs={12}>
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="subtitle2" color="textSecondary" sx={{ mb: 1 }}>
                    History
                  </Typography>
                  {historyData.map((h) => (
                    <Box key={h.id} sx={{ display: 'flex', gap: 2, mb: 1 }}>
                      <Typography variant="caption" color="textSecondary">
                        {formatTimeAgo(h.createdAt)}
                      </Typography>
                      <Typography variant="body2">
                        {h.action} by {h.actor}
                      </Typography>
                    </Box>
                  ))}
                </Grid>
              )}
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailsOpen(false)}>Close</Button>
          {selectedApproval &&
            (selectedApproval.status === 'PENDING' || selectedApproval.status === 'ASSIGNED') && (
              <>
                <Button
                  color="error"
                  onClick={() => {
                    setDetailsOpen(false);
                    handleOpenDecision(selectedApproval, 'reject');
                  }}
                >
                  Reject
                </Button>
                <Button
                  color="success"
                  variant="contained"
                  onClick={() => {
                    setDetailsOpen(false);
                    handleOpenDecision(selectedApproval, 'approve');
                  }}
                >
                  Approve
                </Button>
              </>
            )}
        </DialogActions>
      </Dialog>

      {/* Decision Dialog */}
      <Dialog open={decisionOpen} onClose={() => setDecisionOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {decisionType === 'approve' && 'Approve Request'}
          {decisionType === 'reject' && 'Reject Request'}
          {decisionType === 'escalate' && 'Escalate Request'}
        </DialogTitle>
        <DialogContent>
          {selectedApproval && (
            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="textSecondary">
                {selectedApproval.requestType}: {selectedApproval.entityType}
              </Typography>
            </Box>
          )}
          {decisionType === 'escalate' && (
            <TextField
              fullWidth
              label="Escalate To"
              value={escalateTo}
              onChange={(e) => setEscalateTo(e.target.value)}
              sx={{ mb: 2 }}
              required
            />
          )}
          <TextField
            fullWidth
            label="Reason"
            value={decisionReason}
            onChange={(e) => setDecisionReason(e.target.value)}
            multiline
            rows={3}
            required={decisionType === 'reject'}
            helperText={
              decisionType === 'reject'
                ? 'Required for rejections'
                : 'Optional justification for audit trail'
            }
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDecisionOpen(false)} disabled={processingAction}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmitDecision}
            variant="contained"
            color={
              decisionType === 'approve' ? 'success' : decisionType === 'reject' ? 'error' : 'warning'
            }
            disabled={
              processingAction ||
              (decisionType === 'reject' && !decisionReason) ||
              (decisionType === 'escalate' && !escalateTo)
            }
          >
            {processingAction ? (
              <CircularProgress size={24} />
            ) : decisionType === 'approve' ? (
              'Approve'
            ) : decisionType === 'reject' ? (
              'Reject'
            ) : (
              'Escalate'
            )}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

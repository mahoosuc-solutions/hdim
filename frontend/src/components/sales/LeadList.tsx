/**
 * Lead List Component
 * Displays leads in a table with filtering and actions
 */

import { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Typography,
  Chip,
  IconButton,
  TextField,
  InputAdornment,
  Button,
  Menu,
  MenuItem,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  Tooltip,
} from '@mui/material';
import {
  Search as SearchIcon,
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  MoreVert as MoreIcon,
  SwapHoriz as ConvertIcon,
  Email as EmailIcon,
  FilterList as FilterIcon,
} from '@mui/icons-material';
import { salesService } from '../../services/sales.service';
import type { Lead, LeadStatus, LeadSource, PageResponse } from '../../types/sales';

interface LeadListProps {
  tenantId: string;
}

const statusColors: Record<LeadStatus, 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'> = {
  NEW: 'info',
  CONTACTED: 'primary',
  QUALIFIED: 'success',
  UNQUALIFIED: 'warning',
  CONVERTED: 'success',
  LOST: 'error',
};

export function LeadList({ tenantId }: LeadListProps) {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [sourceFilter, setSourceFilter] = useState<string>('');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [selectedLead, setSelectedLead] = useState<Lead | null>(null);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [filterMenuOpen, setFilterMenuOpen] = useState(false);
  const [filterAnchorEl, setFilterAnchorEl] = useState<null | HTMLElement>(null);

  useEffect(() => {
    loadLeads();
  }, [tenantId, page, rowsPerPage, searchQuery, statusFilter, sourceFilter]);

  const loadLeads = async () => {
    try {
      setLoading(true);
      setError(null);
      const response: PageResponse<Lead> = await salesService.getLeads(
        { tenantId },
        { search: searchQuery, status: statusFilter, source: sourceFilter },
        page,
        rowsPerPage
      );
      setLeads(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load leads');
    } finally {
      setLoading(false);
    }
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, lead: Lead) => {
    setAnchorEl(event.currentTarget);
    setSelectedLead(lead);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleEditClick = () => {
    handleMenuClose();
    setEditDialogOpen(true);
  };

  const handleConvertClick = async () => {
    if (!selectedLead) return;
    try {
      await salesService.convertLead({ tenantId }, selectedLead.id);
      loadLeads();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to convert lead');
    }
    handleMenuClose();
  };

  const handleDeleteClick = async () => {
    if (!selectedLead) return;
    try {
      await salesService.deleteLead({ tenantId }, selectedLead.id);
      loadLeads();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete lead');
    }
    handleMenuClose();
  };

  const handleChangePage = (_event: unknown, newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(event.target.value);
    setPage(0);
  };

  const getScoreColor = (score: number): 'success' | 'warning' | 'error' | 'default' => {
    if (score >= 70) return 'success';
    if (score >= 40) return 'warning';
    if (score > 0) return 'error';
    return 'default';
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5">Leads</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditDialogOpen(true)}>
          Add Lead
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
            <TextField
              size="small"
              placeholder="Search leads..."
              value={searchQuery}
              onChange={handleSearchChange}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              sx={{ minWidth: 250 }}
            />
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Status</InputLabel>
              <Select
                value={statusFilter}
                label="Status"
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0);
                }}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="NEW">New</MenuItem>
                <MenuItem value="CONTACTED">Contacted</MenuItem>
                <MenuItem value="QUALIFIED">Qualified</MenuItem>
                <MenuItem value="UNQUALIFIED">Unqualified</MenuItem>
                <MenuItem value="CONVERTED">Converted</MenuItem>
                <MenuItem value="LOST">Lost</MenuItem>
              </Select>
            </FormControl>
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Source</InputLabel>
              <Select
                value={sourceFilter}
                label="Source"
                onChange={(e) => {
                  setSourceFilter(e.target.value);
                  setPage(0);
                }}
              >
                <MenuItem value="">All</MenuItem>
                <MenuItem value="WEBSITE">Website</MenuItem>
                <MenuItem value="ROI_CALCULATOR">ROI Calculator</MenuItem>
                <MenuItem value="REFERRAL">Referral</MenuItem>
                <MenuItem value="CONFERENCE">Conference</MenuItem>
                <MenuItem value="COLD_OUTREACH">Cold Outreach</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </CardContent>
      </Card>

      {/* Error */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Table */}
      <Card>
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Company</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Source</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="center">Score</TableCell>
                <TableCell>Created</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                    <CircularProgress />
                  </TableCell>
                </TableRow>
              ) : leads.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No leads found</Typography>
                  </TableCell>
                </TableRow>
              ) : (
                leads.map((lead) => (
                  <TableRow key={lead.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {lead.firstName} {lead.lastName}
                      </Typography>
                      {lead.title && (
                        <Typography variant="caption" color="text.secondary">
                          {lead.title}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell>{lead.company || '-'}</TableCell>
                    <TableCell>
                      <Typography variant="body2">{lead.email}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={lead.source.replace('_', ' ')}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={lead.status}
                        size="small"
                        color={statusColors[lead.status]}
                      />
                    </TableCell>
                    <TableCell align="center">
                      <Chip
                        label={lead.score}
                        size="small"
                        color={getScoreColor(lead.score)}
                      />
                    </TableCell>
                    <TableCell>
                      {new Date(lead.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="Actions">
                        <IconButton size="small" onClick={(e) => handleMenuOpen(e, lead)}>
                          <MoreIcon />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={handleChangePage}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          rowsPerPageOptions={[5, 10, 25, 50]}
        />
      </Card>

      {/* Actions Menu */}
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={handleMenuClose}>
        <MenuItem onClick={handleEditClick}>
          <EditIcon fontSize="small" sx={{ mr: 1 }} />
          Edit
        </MenuItem>
        <MenuItem onClick={handleConvertClick} disabled={selectedLead?.status === 'CONVERTED'}>
          <ConvertIcon fontSize="small" sx={{ mr: 1 }} />
          Convert to Opportunity
        </MenuItem>
        <MenuItem onClick={() => handleMenuClose()}>
          <EmailIcon fontSize="small" sx={{ mr: 1 }} />
          Send Email
        </MenuItem>
        <MenuItem onClick={handleDeleteClick} sx={{ color: 'error.main' }}>
          <DeleteIcon fontSize="small" sx={{ mr: 1 }} />
          Delete
        </MenuItem>
      </Menu>

      {/* Edit Dialog (simplified placeholder) */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedLead ? 'Edit Lead' : 'Add Lead'}</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="First Name"
              fullWidth
              defaultValue={selectedLead?.firstName}
            />
            <TextField
              label="Last Name"
              fullWidth
              defaultValue={selectedLead?.lastName}
            />
            <TextField
              label="Email"
              fullWidth
              defaultValue={selectedLead?.email}
            />
            <TextField
              label="Company"
              fullWidth
              defaultValue={selectedLead?.company}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={() => setEditDialogOpen(false)}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default LeadList;

/**
 * Account List Component
 * Displays accounts in a table with filtering and actions
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
  CircularProgress,
  Alert,
  Tooltip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  Search as SearchIcon,
  Add as AddIcon,
  Business as BusinessIcon,
  OpenInNew as OpenIcon,
} from '@mui/icons-material';
import { salesService } from '../../services/sales.service';
import type { Account, AccountStage, AccountType, PageResponse } from '../../types/sales';

interface AccountListProps {
  tenantId: string;
}

const stageColors: Record<AccountStage, 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'> = {
  PROSPECT: 'default',
  QUALIFIED: 'info',
  DEMO: 'primary',
  PROPOSAL: 'warning',
  NEGOTIATION: 'warning',
  CLOSED_WON: 'success',
  CLOSED_LOST: 'error',
};

const typeLabels: Record<AccountType, string> = {
  ACO: 'ACO',
  HEALTH_SYSTEM: 'Health System',
  PAYER: 'Payer',
  HIE: 'HIE',
  FQHC: 'FQHC',
  CLINIC: 'Clinic',
  OTHER: 'Other',
};

export function AccountList({ tenantId }: AccountListProps) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [stageFilter, setStageFilter] = useState<string>('');

  useEffect(() => {
    loadAccounts();
  }, [tenantId, page, rowsPerPage, searchQuery, stageFilter]);

  const loadAccounts = async () => {
    try {
      setLoading(true);
      setError(null);
      const response: PageResponse<Account> = await salesService.getAccounts(
        { tenantId },
        { search: searchQuery, stage: stageFilter },
        page,
        rowsPerPage
      );
      setAccounts(response.content);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load accounts');
    } finally {
      setLoading(false);
    }
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

  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h5">Accounts</Typography>
        <Button variant="contained" startIcon={<AddIcon />}>
          Add Account
        </Button>
      </Box>

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
            <TextField
              size="small"
              placeholder="Search accounts..."
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
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Stage</InputLabel>
              <Select
                value={stageFilter}
                label="Stage"
                onChange={(e) => {
                  setStageFilter(e.target.value);
                  setPage(0);
                }}
              >
                <MenuItem value="">All Stages</MenuItem>
                <MenuItem value="PROSPECT">Prospect</MenuItem>
                <MenuItem value="QUALIFIED">Qualified</MenuItem>
                <MenuItem value="DEMO">Demo</MenuItem>
                <MenuItem value="PROPOSAL">Proposal</MenuItem>
                <MenuItem value="NEGOTIATION">Negotiation</MenuItem>
                <MenuItem value="CLOSED_WON">Closed Won</MenuItem>
                <MenuItem value="CLOSED_LOST">Closed Lost</MenuItem>
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
                <TableCell>Account Name</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Stage</TableCell>
                <TableCell>Location</TableCell>
                <TableCell align="right">Patients</TableCell>
                <TableCell align="right">EHRs</TableCell>
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
              ) : accounts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                    <Typography color="text.secondary">No accounts found</Typography>
                  </TableCell>
                </TableRow>
              ) : (
                accounts.map((account) => (
                  <TableRow key={account.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <BusinessIcon color="action" fontSize="small" />
                        <Box>
                          <Typography variant="body2" fontWeight="medium">
                            {account.name}
                          </Typography>
                          {account.website && (
                            <Typography variant="caption" color="text.secondary">
                              {account.website}
                            </Typography>
                          )}
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={typeLabels[account.type]}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={account.stage.replace('_', ' ')}
                        size="small"
                        color={stageColors[account.stage]}
                      />
                    </TableCell>
                    <TableCell>
                      {account.city && account.state
                        ? `${account.city}, ${account.state}`
                        : account.state || '-'}
                    </TableCell>
                    <TableCell align="right">
                      {account.patientCount?.toLocaleString() || '-'}
                    </TableCell>
                    <TableCell align="right">
                      {account.ehrCount || '-'}
                    </TableCell>
                    <TableCell>
                      {new Date(account.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="View Details">
                        <IconButton size="small">
                          <OpenIcon fontSize="small" />
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
    </Box>
  );
}

export default AccountList;

/**
 * Sales Dashboard Component
 * Displays key sales metrics and charts
 */

import { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  GridLegacy as Grid,
  Typography,
  CircularProgress,
  Alert,
  Chip,
  LinearProgress,
} from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  People as PeopleIcon,
  AttachMoney as MoneyIcon,
  Speed as SpeedIcon,
  CheckCircle as CheckIcon,
} from '@mui/icons-material';
import { salesService } from '../../services/sales.service';
import type { SalesDashboardDTO } from '../../types/sales';

interface SalesDashboardProps {
  tenantId: string;
}

interface MetricCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  trend?: number;
  color?: 'primary' | 'success' | 'warning' | 'error' | 'info';
}

function MetricCard({ title, value, subtitle, icon, trend, color = 'primary' }: MetricCardProps) {
  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
          <Box>
            <Typography color="text.secondary" variant="body2" gutterBottom>
              {title}
            </Typography>
            <Typography variant="h4" component="div" color={`${color}.main`}>
              {value}
            </Typography>
            {subtitle && (
              <Typography variant="caption" color="text.secondary">
                {subtitle}
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              p: 1,
              borderRadius: 1,
              bgcolor: `${color}.lighter`,
              color: `${color}.main`,
            }}
          >
            {icon}
          </Box>
        </Box>
        {trend !== undefined && (
          <Box sx={{ display: 'flex', alignItems: 'center', mt: 1 }}>
            {trend >= 0 ? (
              <TrendingUpIcon color="success" fontSize="small" />
            ) : (
              <TrendingDownIcon color="error" fontSize="small" />
            )}
            <Typography
              variant="caption"
              color={trend >= 0 ? 'success.main' : 'error.main'}
              sx={{ ml: 0.5 }}
            >
              {Math.abs(trend)}% vs last month
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
}

export function SalesDashboard({ tenantId }: SalesDashboardProps) {
  const [dashboard, setDashboard] = useState<SalesDashboardDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDashboard();
  }, [tenantId]);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await salesService.getDashboard({ tenantId });
      setDashboard(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
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

  if (!dashboard) {
    return (
      <Alert severity="info">
        No dashboard data available
      </Alert>
    );
  }

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  };

  return (
    <Box>
      {/* Key Metrics */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Total Leads"
            value={dashboard.totalLeads}
            subtitle={`${dashboard.newLeadsThisMonth} new this month`}
            icon={<PeopleIcon />}
            color="primary"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Pipeline Value"
            value={formatCurrency(dashboard.pipelineValue)}
            subtitle={`${formatCurrency(dashboard.weightedPipelineValue)} weighted`}
            icon={<MoneyIcon />}
            color="success"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Win Rate"
            value={`${dashboard.winRate.toFixed(1)}%`}
            subtitle={`${dashboard.closedWonThisMonth} closed this month`}
            icon={<CheckIcon />}
            color="info"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <MetricCard
            title="Avg Deal Size"
            value={formatCurrency(dashboard.avgDealSize)}
            subtitle={`${dashboard.avgSalesCycleInDays} days avg cycle`}
            icon={<SpeedIcon />}
            color="warning"
          />
        </Grid>
      </Grid>

      {/* Conversion Funnel */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Lead Conversion
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">Conversion Rate</Typography>
                  <Typography variant="body2" fontWeight="bold">
                    {dashboard.conversionRate.toFixed(1)}%
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={dashboard.conversionRate}
                  sx={{ height: 10, borderRadius: 5 }}
                />
              </Box>
              <Box sx={{ mt: 3 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                  <Typography variant="body2">Qualified Leads</Typography>
                  <Typography variant="body2">
                    {dashboard.qualifiedLeads} / {dashboard.totalLeads}
                  </Typography>
                </Box>
                <LinearProgress
                  variant="determinate"
                  value={(dashboard.qualifiedLeads / dashboard.totalLeads) * 100 || 0}
                  color="success"
                  sx={{ height: 10, borderRadius: 5 }}
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Activity Summary
              </Typography>
              <Box sx={{ mt: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="body2">Activities This Week</Typography>
                  <Chip label={dashboard.activitiesThisWeek} color="primary" size="small" />
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="body2">Overdue Activities</Typography>
                  <Chip
                    label={dashboard.overdueActivities}
                    color={dashboard.overdueActivities > 0 ? 'error' : 'default'}
                    size="small"
                  />
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Typography variant="body2">Closed Lost This Month</Typography>
                  <Chip label={dashboard.closedLostThisMonth} color="default" size="small" />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Leads by Source */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Leads by Source
              </Typography>
              <Box sx={{ mt: 2 }}>
                {Object.entries(dashboard.leadsBySource).map(([source, count]) => (
                  <Box
                    key={source}
                    sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}
                  >
                    <Typography variant="body2">{source.replace('_', ' ')}</Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <LinearProgress
                        variant="determinate"
                        value={(count / dashboard.totalLeads) * 100}
                        sx={{ width: 100, height: 6, borderRadius: 3 }}
                      />
                      <Typography variant="body2" sx={{ minWidth: 30 }}>
                        {count}
                      </Typography>
                    </Box>
                  </Box>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Opportunities by Stage
              </Typography>
              <Box sx={{ mt: 2 }}>
                {Object.entries(dashboard.opportunitiesByStage).map(([stage, count]) => (
                  <Box
                    key={stage}
                    sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}
                  >
                    <Typography variant="body2">{stage.replace('_', ' ')}</Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <LinearProgress
                        variant="determinate"
                        value={(count / dashboard.totalOpportunities) * 100}
                        color={stage.includes('WON') ? 'success' : stage.includes('LOST') ? 'error' : 'primary'}
                        sx={{ width: 100, height: 6, borderRadius: 3 }}
                      />
                      <Typography variant="body2" sx={{ minWidth: 30 }}>
                        {count}
                      </Typography>
                    </Box>
                  </Box>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
}

export default SalesDashboard;

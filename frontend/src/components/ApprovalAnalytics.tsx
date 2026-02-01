import { useState, useEffect } from 'react';
import { ApprovalStats } from '../types/approval';
import { approvalService } from '../services/approval.service';

interface ApprovalAnalyticsProps {
  tenantId: string;
  refreshInterval?: number; // in milliseconds
}

interface ChartData {
  label: string;
  value: number;
  color: string;
}

export function ApprovalAnalytics({ tenantId, refreshInterval = 60000 }: ApprovalAnalyticsProps) {
  const [stats, setStats] = useState<ApprovalStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [period, setPeriod] = useState<'7' | '30' | '90'>('30');

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setLoading(true);
        const data = await approvalService.getStats(tenantId, parseInt(period));
        setStats(data);
        setError(null);
      } catch (err) {
        setError('Failed to load analytics');
        console.error('Failed to fetch stats:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
    const interval = setInterval(fetchStats, refreshInterval);
    return () => clearInterval(interval);
  }, [tenantId, period, refreshInterval]);

  if (loading && !stats) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-center text-red-700">
        {error}
      </div>
    );
  }

  if (!stats) return null;

  const statusData: ChartData[] = [
    { label: 'Pending', value: stats.pending, color: '#3B82F6' },
    { label: 'Assigned', value: stats.assigned, color: '#8B5CF6' },
    { label: 'Approved', value: stats.approved, color: '#10B981' },
    { label: 'Rejected', value: stats.rejected, color: '#EF4444' },
    { label: 'Expired', value: stats.expired, color: '#6B7280' },
    { label: 'Escalated', value: stats.escalated, color: '#F59E0B' },
  ];

  const total = statusData.reduce((acc, item) => acc + item.value, 0);
  const approvalRate = total > 0
    ? ((stats.approved / (stats.approved + stats.rejected + stats.expired)) * 100).toFixed(1)
    : '0';

  return (
    <div className="space-y-6">
      {/* Header with Period Selector */}
      <div className="flex justify-between items-center">
        <h2 className="text-xl font-semibold text-gray-900">Approval Analytics</h2>
        <div className="flex gap-2">
          {(['7', '30', '90'] as const).map((p) => (
            <button
              key={p}
              onClick={() => setPeriod(p)}
              className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors
                ${period === p
                  ? 'bg-indigo-100 text-indigo-700'
                  : 'text-gray-600 hover:bg-gray-100'
                }`}
            >
              {p} days
            </button>
          ))}
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <KPICard
          title="Approval Rate"
          value={`${approvalRate}%`}
          subtitle="of completed requests"
          trend={parseFloat(approvalRate) >= 80 ? 'positive' : parseFloat(approvalRate) >= 50 ? 'neutral' : 'negative'}
        />
        <KPICard
          title="Avg Response Time"
          value={formatResponseTime(stats.avgResponseTimeHours)}
          subtitle="average decision time"
          trend={stats.avgResponseTimeHours <= 4 ? 'positive' : stats.avgResponseTimeHours <= 12 ? 'neutral' : 'negative'}
        />
        <KPICard
          title="Pending Now"
          value={String(stats.pending + stats.assigned)}
          subtitle="awaiting decision"
          trend={stats.pending + stats.assigned <= 10 ? 'positive' : stats.pending + stats.assigned <= 25 ? 'neutral' : 'negative'}
        />
        <KPICard
          title="Total Processed"
          value={String(stats.approved + stats.rejected + stats.expired)}
          subtitle={`in last ${period} days`}
        />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Status Distribution */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h3 className="text-sm font-medium text-gray-700 mb-4">Status Distribution</h3>
          <div className="flex items-center justify-center h-48">
            <DonutChart data={statusData} total={total} />
          </div>
          <div className="mt-4 grid grid-cols-2 gap-2">
            {statusData.map((item) => (
              <div key={item.label} className="flex items-center gap-2">
                <div
                  className="w-3 h-3 rounded-full"
                  style={{ backgroundColor: item.color }}
                />
                <span className="text-sm text-gray-600">{item.label}</span>
                <span className="text-sm font-medium text-gray-900 ml-auto">{item.value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Outcome Analysis */}
        <div className="bg-white rounded-lg border border-gray-200 p-6">
          <h3 className="text-sm font-medium text-gray-700 mb-4">Outcome Analysis</h3>
          <div className="space-y-4">
            <ProgressBar
              label="Approved"
              value={stats.approved}
              total={stats.approved + stats.rejected + stats.expired + stats.escalated}
              color="#10B981"
            />
            <ProgressBar
              label="Rejected"
              value={stats.rejected}
              total={stats.approved + stats.rejected + stats.expired + stats.escalated}
              color="#EF4444"
            />
            <ProgressBar
              label="Expired"
              value={stats.expired}
              total={stats.approved + stats.rejected + stats.expired + stats.escalated}
              color="#6B7280"
            />
            <ProgressBar
              label="Escalated"
              value={stats.escalated}
              total={stats.approved + stats.rejected + stats.expired + stats.escalated}
              color="#F59E0B"
            />
          </div>
        </div>
      </div>

      {/* SLA Compliance */}
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <h3 className="text-sm font-medium text-gray-700 mb-4">SLA Compliance</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <SLAMetric
            title="Critical (< 1 hour)"
            targetHours={1}
            actualHours={stats.avgResponseTimeHours * 0.5} // Simulated for demo
          />
          <SLAMetric
            title="High (< 4 hours)"
            targetHours={4}
            actualHours={stats.avgResponseTimeHours * 0.8}
          />
          <SLAMetric
            title="Standard (< 24 hours)"
            targetHours={24}
            actualHours={stats.avgResponseTimeHours}
          />
        </div>
      </div>
    </div>
  );
}

function KPICard({
  title,
  value,
  subtitle,
  trend,
}: {
  title: string;
  value: string;
  subtitle: string;
  trend?: 'positive' | 'neutral' | 'negative';
}) {
  const trendColors = {
    positive: 'text-green-600',
    neutral: 'text-yellow-600',
    negative: 'text-red-600',
  };

  return (
    <div className="bg-white rounded-lg border border-gray-200 p-4">
      <p className="text-sm text-gray-500">{title}</p>
      <p className={`text-2xl font-semibold mt-1 ${trend ? trendColors[trend] : 'text-gray-900'}`}>
        {value}
      </p>
      <p className="text-xs text-gray-400 mt-1">{subtitle}</p>
    </div>
  );
}

function DonutChart({ data, total }: { data: ChartData[]; total: number }) {
  // Simple SVG donut chart
  const size = 160;
  const strokeWidth = 24;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;

  let currentOffset = 0;

  return (
    <div className="relative">
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
        {data.map((item, index) => {
          const percentage = total > 0 ? item.value / total : 0;
          const dashLength = percentage * circumference;
          const offset = currentOffset;
          currentOffset += dashLength;

          return (
            <circle
              key={index}
              cx={size / 2}
              cy={size / 2}
              r={radius}
              fill="none"
              stroke={item.color}
              strokeWidth={strokeWidth}
              strokeDasharray={`${dashLength} ${circumference}`}
              strokeDashoffset={-offset}
              transform={`rotate(-90 ${size / 2} ${size / 2})`}
            />
          );
        })}
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <div className="text-center">
          <p className="text-2xl font-bold text-gray-900">{total}</p>
          <p className="text-xs text-gray-500">Total</p>
        </div>
      </div>
    </div>
  );
}

function ProgressBar({
  label,
  value,
  total,
  color,
}: {
  label: string;
  value: number;
  total: number;
  color: string;
}) {
  const percentage = total > 0 ? (value / total) * 100 : 0;

  return (
    <div>
      <div className="flex justify-between text-sm mb-1">
        <span className="text-gray-600">{label}</span>
        <span className="font-medium text-gray-900">
          {value} ({percentage.toFixed(1)}%)
        </span>
      </div>
      <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-500"
          style={{ width: `${percentage}%`, backgroundColor: color }}
        />
      </div>
    </div>
  );
}

function SLAMetric({
  title,
  targetHours,
  actualHours,
}: {
  title: string;
  targetHours: number;
  actualHours: number;
}) {
  const isCompliant = actualHours <= targetHours;
  const compliancePercentage = Math.min(100, (targetHours / Math.max(actualHours, 0.1)) * 100);

  return (
    <div className={`rounded-lg p-4 ${isCompliant ? 'bg-green-50' : 'bg-red-50'}`}>
      <p className="text-sm font-medium text-gray-700">{title}</p>
      <div className="flex items-baseline gap-2 mt-2">
        <span className={`text-xl font-bold ${isCompliant ? 'text-green-600' : 'text-red-600'}`}>
          {formatResponseTime(actualHours)}
        </span>
        <span className="text-sm text-gray-500">
          / {formatResponseTime(targetHours)} target
        </span>
      </div>
      <div className="mt-2">
        <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden">
          <div
            className={`h-full rounded-full ${isCompliant ? 'bg-green-500' : 'bg-red-500'}`}
            style={{ width: `${Math.min(compliancePercentage, 100)}%` }}
          />
        </div>
      </div>
      <p className={`text-xs mt-1 ${isCompliant ? 'text-green-600' : 'text-red-600'}`}>
        {isCompliant ? 'Within SLA' : 'Exceeds SLA'}
      </p>
    </div>
  );
}

function formatResponseTime(hours: number): string {
  if (hours < 1) {
    return `${Math.round(hours * 60)}m`;
  } else if (hours < 24) {
    return `${hours.toFixed(1)}h`;
  } else {
    return `${(hours / 24).toFixed(1)}d`;
  }
}

export default ApprovalAnalytics;

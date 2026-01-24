# Admin Portal - Audit Log Viewer Implementation Guide

**Status:** 📋 Implementation Guide
**Last Updated:** January 24, 2026
**Version:** 1.0

---

## Overview

This guide provides comprehensive documentation for implementing the **Audit Log Viewer** in the HDIM Admin Portal. The backend API is **production-ready**; this guide focuses on implementing the React/TypeScript frontend component.

### Purpose

The Audit Log Viewer enables compliance officers and administrators to:
- Search and filter HIPAA-compliant audit events
- Investigate PHI access and user activities
- Generate compliance reports (CSV/JSON export)
- Monitor real-time security events
- Meet HIPAA audit requirements (§164.312(b))

---

## Backend API Status

### ✅ Production-Ready Implementation

The audit logging infrastructure is **fully implemented** with the following components:

**Core Components:**
- ✅ `AuditEventEntity.java` - JPA entity with HIPAA-compliant fields
- ✅ `AuditEventRepository.java` - Database access with indexed queries
- ✅ `AuditQueryService.java` - Multi-criteria search and statistics
- ✅ `AuditQueryController.java` - REST API endpoints
- ✅ `AuditExportService.java` - CSV/JSON export functionality

**API Endpoints:**

| Endpoint | Method | Description | Permission |
|----------|--------|-------------|------------|
| `/api/v1/audit/logs/search` | POST | Multi-criteria search | AUDIT_READ |
| `/api/v1/audit/logs/{eventId}` | GET | Get event details | AUDIT_READ |
| `/api/v1/audit/logs/statistics` | GET | Aggregated statistics | AUDIT_READ |
| `/api/v1/audit/logs/export` | GET | Export to CSV/JSON/PDF | AUDIT_EXPORT |

---

## Architecture

### Component Structure

```
frontend/src/
├── components/
│   └── audit/
│       ├── AuditLogViewer.tsx           # Main container component
│       ├── AuditSearchFilters.tsx       # Search filter form
│       ├── AuditEventTable.tsx          # Results table
│       ├── AuditEventDetails.tsx        # Event details modal
│       ├── AuditStatistics.tsx          # Statistics dashboard
│       └── AuditExport.tsx              # Export functionality
├── services/
│   └── audit/
│       └── auditService.ts              # API client
├── types/
│   └── audit.ts                         # TypeScript interfaces
└── hooks/
    └── useAuditLogs.ts                  # Custom React hook
```

### Data Flow

```
┌──────────────────┐
│  AuditLogViewer  │  (Container Component)
│                  │
│  State:          │
│  - filters       │
│  - results       │
│  - pagination    │
│  - loading       │
│  - error         │
└────────┬─────────┘
         │
    ┌────┴────┬──────────────┬────────────────┐
    │         │              │                │
    ▼         ▼              ▼                ▼
┌──────┐  ┌──────┐      ┌──────┐       ┌──────────┐
│Filter│  │Table │      │Details│       │Statistics│
└──┬───┘  └──┬───┘      └───────┘       └──────────┘
   │         │
   │         │
   ▼         ▼
┌────────────────────────────┐
│    auditService.ts         │
│                            │
│  searchAuditLogs()         │
│  getAuditEvent()           │
│  getStatistics()           │
│  exportAuditLogs()         │
└──────────┬─────────────────┘
           │
           ▼
┌────────────────────────────┐
│  Backend API               │
│  /api/v1/audit/logs/*      │
└────────────────────────────┘
```

---

## Implementation

### Phase 1: TypeScript Interfaces (30 minutes)

#### File: `frontend/src/types/audit.ts`

```typescript
/**
 * TypeScript interfaces for HIPAA-compliant audit events.
 *
 * Matches backend AuditEventEntity and DTOs.
 */

/**
 * Audit action types (matches backend AuditAction enum)
 */
export enum AuditAction {
  CREATE = 'CREATE',
  READ = 'READ',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  EXPORT = 'EXPORT',
  SEARCH = 'SEARCH',
  EXECUTE = 'EXECUTE'
}

/**
 * Audit outcome types (matches backend AuditOutcome enum)
 */
export enum AuditOutcome {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
  PARTIAL = 'PARTIAL'
}

/**
 * Audit event interface
 * Represents a single HIPAA-compliant audit event
 */
export interface AuditEvent {
  id: string;
  timestamp: string;  // ISO 8601 format
  tenantId: string;

  // Who
  userId: string;
  username: string;
  role: string;
  ipAddress: string;
  userAgent: string;

  // What
  action: AuditAction;
  resourceType: string;
  resourceId?: string;
  outcome: AuditOutcome;

  // Where
  serviceName: string;
  methodName?: string;
  requestPath?: string;

  // Why (HIPAA requirement)
  purposeOfUse?: string;

  // Additional context
  requestPayload?: Record<string, any>;
  responsePayload?: Record<string, any>;
  errorMessage?: string;
  durationMs?: number;

  // FHIR AuditEvent reference (optional)
  fhirAuditEventId?: string;
}

/**
 * Search request interface
 */
export interface AuditSearchRequest {
  // User filters
  userId?: string;
  username?: string;
  role?: string;

  // Resource filters
  resourceType?: string;
  resourceId?: string;

  // Action filters
  actions?: AuditAction[];
  outcomes?: AuditOutcome[];

  // Service filters
  serviceName?: string;

  // Time range
  startTime?: string;  // ISO 8601
  endTime?: string;    // ISO 8601

  // Text search
  searchText?: string;

  // Pagination
  page: number;
  size: number;

  // Sorting
  sortBy: string;
  sortDirection: 'ASC' | 'DESC';
}

/**
 * Paginated search response
 */
export interface AuditSearchResponse {
  content: AuditEvent[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;  // Current page
}

/**
 * Statistics response interface
 */
export interface AuditStatistics {
  totalEvents: number;
  timeRange: {
    startTime: string;
    endTime: string;
  };

  // By action
  byAction: Record<AuditAction, number>;

  // By outcome
  byOutcome: Record<AuditOutcome, number>;

  // By resource type
  byResourceType: Record<string, number>;

  // By service
  byService: Record<string, number>;

  // Top users
  topUsers: Array<{
    userId: string;
    username: string;
    count: number;
  }>;

  // Top resources
  topResources: Array<{
    resourceType: string;
    resourceId: string;
    count: number;
  }>;

  // Failure rate
  failureRate: number;

  // Average response time
  averageResponseTimeMs: number;
}

/**
 * Export format options
 */
export type ExportFormat = 'CSV' | 'JSON' | 'PDF';

/**
 * Filter state for UI
 */
export interface AuditFilterState {
  userId: string;
  username: string;
  role: string;
  resourceType: string;
  resourceId: string;
  actions: AuditAction[];
  outcomes: AuditOutcome[];
  serviceName: string;
  startDate: Date | null;
  endDate: Date | null;
  searchText: string;
}
```

---

### Phase 2: API Service (1 hour)

#### File: `frontend/src/services/audit/auditService.ts`

```typescript
/**
 * Audit Log API Service
 *
 * Provides methods for querying HIPAA-compliant audit events.
 * All methods automatically include X-Tenant-ID header from auth context.
 */

import {
  AuditEvent,
  AuditSearchRequest,
  AuditSearchResponse,
  AuditStatistics,
  ExportFormat
} from '../../types/audit';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8001';
const AUDIT_API_URL = `${API_BASE_URL}/api/v1/audit/logs`;

/**
 * Get auth headers from localStorage/auth context
 */
function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('access_token');
  const tenantId = localStorage.getItem('tenant_id') || 'TENANT-001';

  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-Tenant-ID': tenantId
  };
}

/**
 * Handle API errors with user-friendly messages
 */
function handleApiError(error: any): never {
  if (error.response?.status === 403) {
    throw new Error('Insufficient permissions to access audit logs. AUDIT_READ permission required.');
  }
  if (error.response?.status === 401) {
    throw new Error('Authentication required. Please log in again.');
  }
  throw new Error(error.message || 'Failed to fetch audit logs');
}

/**
 * Search audit logs with multi-criteria filtering
 *
 * @param request - Search criteria
 * @returns Paginated audit events
 */
export async function searchAuditLogs(
  request: AuditSearchRequest
): Promise<AuditSearchResponse> {
  try {
    const response = await fetch(`${AUDIT_API_URL}/search`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(request)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    return handleApiError(error);
  }
}

/**
 * Get a specific audit event by ID
 *
 * @param eventId - Audit event ID
 * @returns Audit event details
 */
export async function getAuditEvent(eventId: string): Promise<AuditEvent> {
  try {
    const response = await fetch(`${AUDIT_API_URL}/${eventId}`, {
      method: 'GET',
      headers: getAuthHeaders()
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Audit event not found');
      }
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    return handleApiError(error);
  }
}

/**
 * Get aggregated audit log statistics
 *
 * @param startTime - Start of time range (ISO 8601)
 * @param endTime - End of time range (ISO 8601)
 * @returns Aggregated statistics
 */
export async function getAuditStatistics(
  startTime?: string,
  endTime?: string
): Promise<AuditStatistics> {
  try {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);

    const response = await fetch(
      `${AUDIT_API_URL}/statistics?${params.toString()}`,
      {
        method: 'GET',
        headers: getAuthHeaders()
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    return handleApiError(error);
  }
}

/**
 * Export audit logs to CSV, JSON, or PDF
 *
 * @param request - Search criteria
 * @param format - Export format
 * @returns Blob for download
 */
export async function exportAuditLogs(
  request: AuditSearchRequest,
  format: ExportFormat
): Promise<Blob> {
  try {
    const params = new URLSearchParams({
      format: format.toLowerCase(),
      ...Object.fromEntries(
        Object.entries(request)
          .filter(([_, value]) => value !== undefined && value !== null)
          .map(([key, value]) => [key, String(value)])
      )
    });

    const response = await fetch(
      `${AUDIT_API_URL}/export?${params.toString()}`,
      {
        method: 'GET',
        headers: getAuthHeaders()
      }
    );

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return await response.blob();
  } catch (error) {
    return handleApiError(error);
  }
}

/**
 * Download exported file
 *
 * @param blob - File blob
 * @param filename - Download filename
 */
export function downloadFile(blob: Blob, filename: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/**
 * Format export filename with timestamp
 *
 * @param format - Export format
 * @returns Formatted filename
 */
export function getExportFilename(format: ExportFormat): string {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
  return `audit-logs-${timestamp}.${format.toLowerCase()}`;
}
```

---

### Phase 3: Custom React Hook (30 minutes)

#### File: `frontend/src/hooks/useAuditLogs.ts`

```typescript
/**
 * Custom React hook for audit log management
 *
 * Provides state management and API integration for audit log viewer.
 */

import { useState, useCallback, useEffect } from 'react';
import {
  AuditEvent,
  AuditSearchRequest,
  AuditSearchResponse,
  AuditStatistics,
  ExportFormat,
  AuditFilterState
} from '../types/audit';
import {
  searchAuditLogs,
  getAuditEvent,
  getAuditStatistics,
  exportAuditLogs,
  downloadFile,
  getExportFilename
} from '../services/audit/auditService';

interface UseAuditLogsResult {
  // State
  events: AuditEvent[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  loading: boolean;
  error: string | null;
  selectedEvent: AuditEvent | null;
  statistics: AuditStatistics | null;

  // Actions
  search: (filters: AuditFilterState, page?: number, size?: number) => Promise<void>;
  selectEvent: (eventId: string) => Promise<void>;
  clearSelection: () => void;
  loadStatistics: (startDate?: Date, endDate?: Date) => Promise<void>;
  exportLogs: (filters: AuditFilterState, format: ExportFormat) => Promise<void>;
  refresh: () => Promise<void>;
}

/**
 * Hook for managing audit log state and operations
 */
export function useAuditLogs(): UseAuditLogsResult {
  const [events, setEvents] = useState<AuditEvent[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<AuditEvent | null>(null);
  const [statistics, setStatistics] = useState<AuditStatistics | null>(null);
  const [lastRequest, setLastRequest] = useState<AuditSearchRequest | null>(null);

  /**
   * Convert filter state to search request
   */
  const filtersToRequest = useCallback(
    (filters: AuditFilterState, page = 0, size = 20): AuditSearchRequest => ({
      userId: filters.userId || undefined,
      username: filters.username || undefined,
      role: filters.role || undefined,
      resourceType: filters.resourceType || undefined,
      resourceId: filters.resourceId || undefined,
      actions: filters.actions.length > 0 ? filters.actions : undefined,
      outcomes: filters.outcomes.length > 0 ? filters.outcomes : undefined,
      serviceName: filters.serviceName || undefined,
      startTime: filters.startDate?.toISOString() || undefined,
      endTime: filters.endDate?.toISOString() || undefined,
      searchText: filters.searchText || undefined,
      page,
      size,
      sortBy: 'timestamp',
      sortDirection: 'DESC'
    }),
    []
  );

  /**
   * Search audit logs
   */
  const search = useCallback(
    async (filters: AuditFilterState, page = 0, size = 20) => {
      setLoading(true);
      setError(null);

      try {
        const request = filtersToRequest(filters, page, size);
        setLastRequest(request);

        const response = await searchAuditLogs(request);

        setEvents(response.content);
        setTotalElements(response.totalElements);
        setTotalPages(response.totalPages);
        setCurrentPage(response.number);
      } catch (err: any) {
        setError(err.message);
        setEvents([]);
      } finally {
        setLoading(false);
      }
    },
    [filtersToRequest]
  );

  /**
   * Select and load event details
   */
  const selectEvent = useCallback(async (eventId: string) => {
    setLoading(true);
    setError(null);

    try {
      const event = await getAuditEvent(eventId);
      setSelectedEvent(event);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Clear selected event
   */
  const clearSelection = useCallback(() => {
    setSelectedEvent(null);
  }, []);

  /**
   * Load statistics
   */
  const loadStatistics = useCallback(
    async (startDate?: Date, endDate?: Date) => {
      setLoading(true);
      setError(null);

      try {
        const stats = await getAuditStatistics(
          startDate?.toISOString(),
          endDate?.toISOString()
        );
        setStatistics(stats);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    },
    []
  );

  /**
   * Export audit logs
   */
  const exportLogs = useCallback(
    async (filters: AuditFilterState, format: ExportFormat) => {
      setLoading(true);
      setError(null);

      try {
        const request = filtersToRequest(filters, 0, 100000); // Export all matching records
        const blob = await exportAuditLogs(request, format);
        const filename = getExportFilename(format);
        downloadFile(blob, filename);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    },
    [filtersToRequest]
  );

  /**
   * Refresh current search
   */
  const refresh = useCallback(async () => {
    if (lastRequest) {
      setLoading(true);
      setError(null);

      try {
        const response = await searchAuditLogs(lastRequest);
        setEvents(response.content);
        setTotalElements(response.totalElements);
        setTotalPages(response.totalPages);
        setCurrentPage(response.number);
      } catch (err: any) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    }
  }, [lastRequest]);

  return {
    events,
    totalElements,
    totalPages,
    currentPage,
    loading,
    error,
    selectedEvent,
    statistics,
    search,
    selectEvent,
    clearSelection,
    loadStatistics,
    exportLogs,
    refresh
  };
}
```

---

### Phase 4: Search Filters Component (2 hours)

#### File: `frontend/src/components/audit/AuditSearchFilters.tsx`

```typescript
/**
 * Audit Search Filters Component
 *
 * Provides comprehensive search filters for audit log viewer.
 */

import React, { useState } from 'react';
import { AuditFilterState, AuditAction, AuditOutcome } from '../../types/audit';

interface AuditSearchFiltersProps {
  onSearch: (filters: AuditFilterState) => void;
  loading: boolean;
}

export const AuditSearchFilters: React.FC<AuditSearchFiltersProps> = ({
  onSearch,
  loading
}) => {
  const [filters, setFilters] = useState<AuditFilterState>({
    userId: '',
    username: '',
    role: '',
    resourceType: '',
    resourceId: '',
    actions: [],
    outcomes: [],
    serviceName: '',
    startDate: null,
    endDate: null,
    searchText: ''
  });

  const handleChange = (field: keyof AuditFilterState, value: any) => {
    setFilters(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch(filters);
  };

  const handleReset = () => {
    const emptyFilters: AuditFilterState = {
      userId: '',
      username: '',
      role: '',
      resourceType: '',
      resourceId: '',
      actions: [],
      outcomes: [],
      serviceName: '',
      startDate: null,
      endDate: null,
      searchText: ''
    };
    setFilters(emptyFilters);
    onSearch(emptyFilters);
  };

  return (
    <div className="audit-search-filters">
      <form onSubmit={handleSubmit} className="filters-form">
        {/* Text Search */}
        <div className="filter-row">
          <div className="filter-group full-width">
            <label htmlFor="searchText">Search</label>
            <input
              id="searchText"
              type="text"
              placeholder="Search by user, resource, or action..."
              value={filters.searchText}
              onChange={(e) => handleChange('searchText', e.target.value)}
              disabled={loading}
            />
          </div>
        </div>

        {/* User Filters */}
        <div className="filter-row">
          <div className="filter-group">
            <label htmlFor="userId">User ID</label>
            <input
              id="userId"
              type="text"
              placeholder="User ID"
              value={filters.userId}
              onChange={(e) => handleChange('userId', e.target.value)}
              disabled={loading}
            />
          </div>

          <div className="filter-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              placeholder="Email or username"
              value={filters.username}
              onChange={(e) => handleChange('username', e.target.value)}
              disabled={loading}
            />
          </div>

          <div className="filter-group">
            <label htmlFor="role">Role</label>
            <select
              id="role"
              value={filters.role}
              onChange={(e) => handleChange('role', e.target.value)}
              disabled={loading}
            >
              <option value="">All Roles</option>
              <option value="SUPER_ADMIN">Super Admin</option>
              <option value="ADMIN">Admin</option>
              <option value="CLINICIAN">Clinician</option>
              <option value="EVALUATOR">Evaluator</option>
              <option value="AUDITOR">Auditor</option>
              <option value="ANALYST">Analyst</option>
              <option value="VIEWER">Viewer</option>
            </select>
          </div>
        </div>

        {/* Resource Filters */}
        <div className="filter-row">
          <div className="filter-group">
            <label htmlFor="resourceType">Resource Type</label>
            <select
              id="resourceType"
              value={filters.resourceType}
              onChange={(e) => handleChange('resourceType', e.target.value)}
              disabled={loading}
            >
              <option value="">All Resource Types</option>
              <option value="Patient">Patient</option>
              <option value="QualityMeasure">Quality Measure</option>
              <option value="Evaluation">Evaluation</option>
              <option value="CareGap">Care Gap</option>
              <option value="User">User</option>
              <option value="Configuration">Configuration</option>
            </select>
          </div>

          <div className="filter-group">
            <label htmlFor="resourceId">Resource ID</label>
            <input
              id="resourceId"
              type="text"
              placeholder="Resource ID"
              value={filters.resourceId}
              onChange={(e) => handleChange('resourceId', e.target.value)}
              disabled={loading}
            />
          </div>

          <div className="filter-group">
            <label htmlFor="serviceName">Service</label>
            <select
              id="serviceName"
              value={filters.serviceName}
              onChange={(e) => handleChange('serviceName', e.target.value)}
              disabled={loading}
            >
              <option value="">All Services</option>
              <option value="gateway-service">Gateway</option>
              <option value="patient-service">Patient Service</option>
              <option value="quality-measure-service">Quality Measure</option>
              <option value="care-gap-service">Care Gap</option>
              <option value="fhir-service">FHIR</option>
            </select>
          </div>
        </div>

        {/* Action & Outcome Filters */}
        <div className="filter-row">
          <div className="filter-group">
            <label>Actions</label>
            <div className="checkbox-group">
              {Object.values(AuditAction).map(action => (
                <label key={action} className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={filters.actions.includes(action)}
                    onChange={(e) => {
                      const newActions = e.target.checked
                        ? [...filters.actions, action]
                        : filters.actions.filter(a => a !== action);
                      handleChange('actions', newActions);
                    }}
                    disabled={loading}
                  />
                  <span>{action}</span>
                </label>
              ))}
            </div>
          </div>

          <div className="filter-group">
            <label>Outcomes</label>
            <div className="checkbox-group">
              {Object.values(AuditOutcome).map(outcome => (
                <label key={outcome} className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={filters.outcomes.includes(outcome)}
                    onChange={(e) => {
                      const newOutcomes = e.target.checked
                        ? [...filters.outcomes, outcome]
                        : filters.outcomes.filter(o => o !== outcome);
                      handleChange('outcomes', newOutcomes);
                    }}
                    disabled={loading}
                  />
                  <span>{outcome}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        {/* Date Range */}
        <div className="filter-row">
          <div className="filter-group">
            <label htmlFor="startDate">Start Date</label>
            <input
              id="startDate"
              type="datetime-local"
              value={filters.startDate?.toISOString().slice(0, 16) || ''}
              onChange={(e) => handleChange('startDate', e.target.value ? new Date(e.target.value) : null)}
              disabled={loading}
            />
          </div>

          <div className="filter-group">
            <label htmlFor="endDate">End Date</label>
            <input
              id="endDate"
              type="datetime-local"
              value={filters.endDate?.toISOString().slice(0, 16) || ''}
              onChange={(e) => handleChange('endDate', e.target.value ? new Date(e.target.value) : null)}
              disabled={loading}
            />
          </div>
        </div>

        {/* Actions */}
        <div className="filter-actions">
          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Searching...' : 'Search'}
          </button>
          <button type="button" onClick={handleReset} disabled={loading} className="btn-secondary">
            Reset
          </button>
        </div>
      </form>
    </div>
  );
};
```

---

## Styling

### CSS File: `frontend/src/components/audit/AuditLogViewer.css`

```css
/**
 * Audit Log Viewer Styles
 *
 * HIPAA-compliant styling with accessibility features.
 */

.audit-log-viewer {
  padding: 2rem;
  max-width: 1600px;
  margin: 0 auto;
}

.audit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.audit-header h1 {
  font-size: 2rem;
  color: #333;
  margin: 0;
}

.audit-actions {
  display: flex;
  gap: 1rem;
}

/* Search Filters */
.audit-search-filters {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.filters-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.filter-row {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.filter-group {
  flex: 1;
  min-width: 200px;
  display: flex;
  flex-direction: column;
}

.filter-group.full-width {
  flex-basis: 100%;
}

.filter-group label {
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: #555;
}

.filter-group input,
.filter-group select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.filter-group input:focus,
.filter-group select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
}

.filter-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1rem;
}

/* Audit Event Table */
.audit-event-table {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.audit-event-table table {
  width: 100%;
  border-collapse: collapse;
}

.audit-event-table th {
  background: #f5f5f5;
  padding: 1rem;
  text-align: left;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #ddd;
}

.audit-event-table td {
  padding: 1rem;
  border-bottom: 1px solid #eee;
}

.audit-event-table tr:hover {
  background: #f9f9f9;
  cursor: pointer;
}

/* Outcome Badges */
.outcome-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
  text-transform: uppercase;
}

.outcome-badge.success {
  background: #d4edda;
  color: #155724;
}

.outcome-badge.failure {
  background: #f8d7da;
  color: #721c24;
}

.outcome-badge.partial {
  background: #fff3cd;
  color: #856404;
}

/* Action Badges */
.action-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
  background: #e3f2fd;
  color: #1565c0;
}

/* Pagination */
.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-top: 1px solid #eee;
}

.pagination-info {
  color: #666;
  font-size: 0.9rem;
}

.pagination-controls {
  display: flex;
  gap: 0.5rem;
}

.pagination-controls button {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}

.pagination-controls button:hover:not(:disabled) {
  background: #f5f5f5;
}

.pagination-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Event Details Modal */
.event-details-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.event-details-content {
  background: white;
  border-radius: 8px;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 2rem;
}

.event-details-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.event-details-header h2 {
  margin: 0;
  color: #333;
}

.close-button {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
}

.event-field {
  margin-bottom: 1rem;
}

.event-field label {
  display: block;
  font-weight: 600;
  color: #555;
  margin-bottom: 0.25rem;
}

.event-field value {
  display: block;
  padding: 0.5rem;
  background: #f5f5f5;
  border-radius: 4px;
  font-family: monospace;
}

/* Loading State */
.loading-spinner {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 3rem;
}

.loading-spinner::after {
  content: "";
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Error State */
.error-message {
  background: #f8d7da;
  border-left: 4px solid #dc3545;
  padding: 1rem;
  border-radius: 4px;
  color: #721c24;
  margin-bottom: 1rem;
}

/* Buttons */
.btn-primary {
  background: #667eea;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  font-weight: 600;
}

.btn-primary:hover:not(:disabled) {
  background: #5568d3;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  font-weight: 600;
}

.btn-secondary:hover:not(:disabled) {
  background: #f0f4ff;
}

.btn-icon {
  background: none;
  border: 1px solid #ddd;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-icon:hover {
  background: #f5f5f5;
}

/* Responsive Design */
@media (max-width: 768px) {
  .audit-log-viewer {
    padding: 1rem;
  }

  .filter-row {
    flex-direction: column;
  }

  .filter-group {
    min-width: 100%;
  }

  .audit-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }

  .audit-event-table {
    overflow-x: auto;
  }
}
```

---

## Security Considerations

### HIPAA Compliance

**Audit Log Viewer Requirements:**

1. **Access Control (§164.312(a)(1))**
   - ✅ AUDIT_READ permission required for all endpoints
   - ✅ Multi-tenant isolation enforced by X-Tenant-ID
   - ✅ Role-based access (AUDITOR or ADMIN only)

2. **Audit Controls (§164.312(b))**
   - ✅ All audit log accesses are themselves audited
   - ✅ Viewing audit logs creates audit events
   - ✅ Export actions are logged

3. **Data Integrity (§164.312(c)(1))**
   - ✅ Audit events are immutable (no update/delete endpoints)
   - ✅ Payload encryption for sensitive data
   - ✅ Tamper-evident logging

4. **Transmission Security (§164.312(e)(1))**
   - ✅ HTTPS required for all API calls
   - ✅ JWT tokens for authentication
   - ✅ Secure headers (X-Tenant-ID validated)

### Frontend Security

**Best Practices:**

```typescript
// 1. Never log PHI to browser console
// ❌ BAD
console.log('Audit event:', event);

// ✅ GOOD
console.log('Audit event loaded:', event.id);

// 2. Sanitize display values
function sanitizeValue(value: any): string {
  if (typeof value === 'string' && value.includes('password')) {
    return '[REDACTED]';
  }
  return String(value);
}

// 3. Implement auto-logout on inactivity
useEffect(() => {
  let timeout: NodeJS.Timeout;

  const resetTimeout = () => {
    clearTimeout(timeout);
    timeout = setTimeout(() => {
      // Logout user after 15 minutes of inactivity
      handleLogout();
    }, 15 * 60 * 1000);
  };

  // Reset timeout on user activity
  window.addEventListener('mousemove', resetTimeout);
  window.addEventListener('keypress', resetTimeout);

  return () => {
    clearTimeout(timeout);
    window.removeEventListener('mousemove', resetTimeout);
    window.removeEventListener('keypress', resetTimeout);
  };
}, []);

// 4. Clear sensitive data on unmount
useEffect(() => {
  return () => {
    setEvents([]);
    setSelectedEvent(null);
  };
}, []);
```

---

## Testing

### Unit Tests

**File:** `frontend/src/components/audit/__tests__/AuditLogViewer.test.tsx`

```typescript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AuditLogViewer } from '../AuditLogViewer';
import * as auditService from '../../../services/audit/auditService';

// Mock audit service
jest.mock('../../../services/audit/auditService');

describe('AuditLogViewer', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders search filters', () => {
    render(<AuditLogViewer />);
    expect(screen.getByPlaceholderText(/search by user/i)).toBeInTheDocument();
  });

  test('performs search on form submit', async () => {
    const mockSearch = jest.spyOn(auditService, 'searchAuditLogs');
    mockSearch.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      number: 0
    });

    render(<AuditLogViewer />);

    const searchButton = screen.getByRole('button', { name: /search/i });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(mockSearch).toHaveBeenCalled();
    });
  });

  test('displays audit events in table', async () => {
    const mockSearch = jest.spyOn(auditService, 'searchAuditLogs');
    mockSearch.mockResolvedValue({
      content: [
        {
          id: '123',
          timestamp: '2026-01-24T10:00:00Z',
          username: 'john.doe@example.com',
          action: 'READ',
          resourceType: 'Patient',
          resourceId: 'PAT-123',
          outcome: 'SUCCESS'
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0
    });

    render(<AuditLogViewer />);

    const searchButton = screen.getByRole('button', { name: /search/i });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(screen.getByText('john.doe@example.com')).toBeInTheDocument();
      expect(screen.getByText('READ')).toBeInTheDocument();
      expect(screen.getByText('Patient')).toBeInTheDocument();
    });
  });

  test('handles API errors gracefully', async () => {
    const mockSearch = jest.spyOn(auditService, 'searchAuditLogs');
    mockSearch.mockRejectedValue(new Error('Network error'));

    render(<AuditLogViewer />);

    const searchButton = screen.getByRole('button', { name: /search/i });
    fireEvent.click(searchButton);

    await waitFor(() => {
      expect(screen.getByText(/network error/i)).toBeInTheDocument();
    });
  });

  test('exports to CSV', async () => {
    const mockExport = jest.spyOn(auditService, 'exportAuditLogs');
    const mockBlob = new Blob(['csv data'], { type: 'text/csv' });
    mockExport.mockResolvedValue(mockBlob);

    render(<AuditLogViewer />);

    const exportButton = screen.getByRole('button', { name: /export csv/i });
    fireEvent.click(exportButton);

    await waitFor(() => {
      expect(mockExport).toHaveBeenCalledWith(expect.anything(), 'CSV');
    });
  });
});
```

---

## Troubleshooting

### Common Issues

#### Issue 1: "Insufficient permissions to access audit logs"

**Cause:** User lacks AUDIT_READ permission

**Solution:**
1. Verify user has AUDITOR or ADMIN role
2. Check RBAC permission mapping:
   ```java
   // RolePermissions.java
   AUDITOR: EnumSet.of(Permission.AUDIT_READ, Permission.AUDIT_EXPORT)
   ```
3. Re-login to refresh JWT token

#### Issue 2: "No audit events found" (but events exist)

**Cause:** Tenant isolation issue or incorrect filters

**Solution:**
1. Verify X-Tenant-ID header is set correctly
2. Check browser DevTools Network tab for tenant ID
3. Clear all filters and try again
4. Verify database has events for this tenant:
   ```sql
   SELECT COUNT(*) FROM audit_events WHERE tenant_id = 'TENANT-001';
   ```

#### Issue 3: "Export download fails"

**Cause:** CORS or file download blocking

**Solution:**
1. Verify AUDIT_EXPORT permission
2. Check browser allows downloads from localhost
3. Verify export endpoint in DevTools Network tab
4. Check blob size is not too large

---

## Related Documentation

- **[RBAC Implementation Guide](../backend/docs/RBAC_IMPLEMENTATION_GUIDE.md)** - Permission system
- **[HIPAA Compliance Guide](../backend/HIPAA-CACHE-COMPLIANCE.md)** - PHI handling
- **[Gateway Trust Architecture](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)** - Authentication
- **[Distributed Tracing Guide](../backend/docs/DISTRIBUTED_TRACING_GUIDE.md)** - Request tracking

---

## Next Steps

### Immediate Implementation

1. **Create TypeScript interfaces** (`frontend/src/types/audit.ts`)
2. **Implement API service** (`frontend/src/services/audit/auditService.ts`)
3. **Create custom hook** (`frontend/src/hooks/useAuditLogs.ts`)
4. **Build components** (AuditSearchFilters, AuditEventTable, AuditEventDetails)
5. **Add routing** to Admin Portal navigation
6. **Test with backend API** (already running on port 8001)

### Future Enhancements

1. **Real-time log streaming** via WebSocket
2. **Advanced analytics** (charts, graphs, trends)
3. **Anomaly detection** (suspicious patterns)
4. **Compliance report templates** (HIPAA audit reports)
5. **Automated alerts** (security incidents)

---

**Last Updated:** January 24, 2026
**Document Version:** 1.0
**Status:** 📋 Implementation Guide

# Administrator Guide

This guide is designed for **System Administrators** managing the HDIM Clinical Portal deployment, user access, and system configuration.

## Overview

As an Administrator, you are responsible for:
- Managing users and permissions
- Configuring quality measures
- Monitoring system health
- Managing data imports
- Generating compliance reports
- Maintaining system security

---

## Your Dashboard

When you log in, you'll see the **Admin Dashboard** with these widgets:

### System Health Widget
Real-time system status:
- API service status
- Database connectivity
- CQL engine status
- FHIR server status

**Status Indicators:**
- 🟢 Healthy - All systems operational
- 🟡 Degraded - Partial functionality
- 🔴 Down - Service unavailable

### User Activity Widget
Recent user actions:
- Active users
- Recent logins
- Failed login attempts
- High-volume activities

### Data Sync Status Widget
Data synchronization status:
- Last sync time
- Records processed
- Errors/warnings
- Pending syncs

### Compliance Overview Widget
Organization-wide compliance:
- Overall compliance rate
- By department/provider
- Trend vs previous period

---

## User Management

### View Users

1. Navigate to **Admin** → **Users**
2. User list displays:
   - Username
   - Full name
   - Role
   - Status (Active/Inactive)
   - Last login

### Create New User

1. Navigate to **Admin** → **Users**
2. Click **"Add User"**
3. Complete form:
   - Username (unique)
   - Email address
   - Full name
   - Role assignment
   - Organization/department
4. Click **"Create User"**
5. System sends welcome email with password reset

### Edit User

1. Click user row to select
2. Click **"Edit"**
3. Modify fields as needed
4. Click **"Save Changes"**

### Deactivate User

1. Select user
2. Click **"Deactivate"**
3. Confirm action
4. User can no longer log in

::: warning
Deactivating a user does not delete their historical activity. Use this for departed employees.
:::

### Reset User Password

1. Select user
2. Click **"Reset Password"**
3. User receives password reset email
4. Must change password on next login

---

## Role Management

### Available Roles

| Role | Description | Key Permissions |
|------|-------------|-----------------|
| **Provider** | Physicians, NPs, PAs | Full clinical access, evaluations, gap closure |
| **RN** | Registered Nurses | Outreach, care coordination, gap closure |
| **MA** | Medical Assistants | Patient prep, basic documentation |
| **Quality Analyst** | Quality improvement staff | Reports, batch evaluations, measure config |
| **Administrator** | System administrators | Full system access |
| **Read-Only** | View-only users | View data, no modifications |

### Assign Role to User

1. Navigate to user edit screen
2. Select role from dropdown
3. Save changes
4. User receives updated permissions immediately

### Custom Role Permissions

Contact system support to configure custom roles with specific permissions.

---

## Quality Measure Configuration

### View Measures

1. Navigate to **Admin** → **Quality Measures**
2. Measure library displays:
   - Measure ID
   - Name
   - Category
   - Version
   - Status (Active/Inactive)
   - Evaluable (Yes/No)

### Activate/Deactivate Measure

1. Select measure
2. Click **"Activate"** or **"Deactivate"**
3. Confirm action

::: tip
Inactive measures won't appear in evaluation dropdowns but historical data is retained.
:::

### Measure Settings

For each measure, configure:

**Basic Settings:**
- Display name
- Description
- Category assignment
- Target compliance rate

**Evaluation Settings:**
- Evaluation period (e.g., "measurement year")
- Eligible population criteria
- Exclusion handling

### Import New Measures

1. Navigate to **Admin** → **Quality Measures**
2. Click **"Import Measure"**
3. Upload measure package (CQL + FHIR resources)
4. Review validation results
5. Confirm import
6. Activate when ready

---

## Data Management

### Data Import

#### Manual Import

1. Navigate to **Admin** → **Data Import**
2. Select import type:
   - Patient demographics
   - Clinical data
   - Claims data
3. Upload file (CSV, JSON, or FHIR bundle)
4. Map fields to system fields
5. Review preview
6. Execute import
7. Review results

#### Scheduled Imports

1. Navigate to **Admin** → **Data Import** → **Schedules**
2. Click **"Create Schedule"**
3. Configure:
   - Import type
   - Source connection
   - Frequency (hourly, daily, weekly)
   - Time of day
   - Error notifications
4. Save and activate

### Data Export

1. Navigate to **Admin** → **Data Export**
2. Select export type:
   - Patient data
   - Evaluation results
   - Care gap summary
   - Compliance reports
3. Configure filters (date range, measures, etc.)
4. Select format (CSV, Excel, JSON)
5. Execute export
6. Download file

### Data Quality Monitoring

1. Navigate to **Admin** → **Data Quality**
2. Review:
   - Missing data alerts
   - Data validation failures
   - Duplicate records
   - Stale data warnings
3. Take corrective action as needed

---

## Report Scheduling

### Create Scheduled Report

1. Navigate to **Admin** → **Report Scheduling**
2. Click **"Create Schedule"**
3. Configure:
   - Report type (Patient, Population, Trend)
   - Frequency (Daily, Weekly, Monthly)
   - Day and time
   - Recipients (email addresses)
   - Format (PDF, Excel)
4. Save schedule

### Manage Scheduled Reports

1. View all scheduled reports
2. Edit schedule parameters
3. Pause/resume schedules
4. View delivery history
5. Delete schedules

---

## System Configuration

### General Settings

1. Navigate to **Admin** → **Settings**
2. Configure:
   - Organization name
   - Default time zone
   - Date format
   - Session timeout
   - Pagination defaults

### Security Settings

1. Navigate to **Admin** → **Security**
2. Configure:
   - Password policy
   - Multi-factor authentication
   - Session management
   - IP restrictions
   - Audit logging level

### Integration Settings

1. Navigate to **Admin** → **Integrations**
2. Configure connections:
   - EHR integration
   - FHIR server
   - Authentication provider
   - Email service

---

## System Monitoring

### Health Dashboard

1. Navigate to **Admin** → **System Health**
2. View:
   - Service status
   - Response times
   - Error rates
   - Resource utilization

### Audit Logs

1. Navigate to **Admin** → **Audit Logs**
2. View:
   - User actions
   - System events
   - Security events
   - Data changes
3. Filter by:
   - Date range
   - User
   - Action type
   - Severity

### Performance Monitoring

1. Navigate to **Admin** → **Performance**
2. View:
   - API response times
   - Database query performance
   - CQL evaluation times
   - Batch processing metrics

---

## Troubleshooting

### User Can't Log In

1. Verify user is active
2. Check account lockout status
3. Reset password if needed
4. Verify role assignment
5. Check for IP restrictions

### Data Not Syncing

1. Check sync schedule status
2. Review error logs
3. Verify source connectivity
4. Check data format compliance
5. Manual retry if needed

### Slow Performance

1. Check system health dashboard
2. Review resource utilization
3. Check for large batch operations
4. Review database performance
5. Contact support if persists

### Evaluation Failures

1. Check CQL engine status
2. Verify measure has CQL library
3. Check patient data completeness
4. Review error message details
5. Test with known-good patient

---

## Security Best Practices

### User Management

1. **Principle of least privilege** - Assign minimum necessary role
2. **Regular access review** - Audit user list quarterly
3. **Prompt deactivation** - Remove access same day as departure
4. **Strong passwords** - Enforce complexity requirements

### Data Protection

1. **Access logging** - Monitor all data access
2. **Export controls** - Limit bulk exports
3. **Encryption** - Ensure data encrypted at rest and in transit
4. **Backup verification** - Test restore procedures

### System Security

1. **Regular updates** - Apply security patches promptly
2. **Monitoring** - Review security alerts daily
3. **Incident response** - Document and follow procedures
4. **Vendor management** - Review third-party security

---

## Backup and Recovery

### Backup Schedule

| Data Type | Frequency | Retention |
|-----------|-----------|-----------|
| Database | Daily | 30 days |
| Configuration | Weekly | 90 days |
| Audit logs | Daily | 1 year |
| User data | Daily | 30 days |

### Recovery Procedures

For data recovery:
1. Contact system support
2. Specify recovery point
3. Coordinate downtime
4. Verify restoration
5. Document incident

---

## Support and Escalation

### Internal Support

1. First-line: Help desk tickets
2. Second-line: IT administrator
3. Third-line: Vendor support

### Vendor Support

Contact vendor support for:
- System outages
- Security incidents
- Feature requests
- License issues

### Emergency Contacts

Maintain current contact list for:
- Vendor support hotline
- Security incident response
- Executive escalation

---

## Related Workflows

- [Report Generation](/workflows/report-generation)

## Related User Stories

- [US-DB-018: View System Health](/user-stories/dashboards#us-db-018)
- [US-RP-010: Schedule Automated Reports](/user-stories/reports#us-rp-010)

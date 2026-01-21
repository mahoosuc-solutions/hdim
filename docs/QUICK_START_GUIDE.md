# HDIM Platform - Quick Start Guide

## Welcome to the Healthcare Data Integration & Management Platform

This guide will help you quickly get started with the HDIM platform based on your role.

## Choose Your Role

- [Care Manager](#care-manager-quick-start)
- [Physician/Clinician](#physician-quick-start)
- [System Administrator](#administrator-quick-start)
- [AI Assistant User](#ai-assistant-quick-start)
- [Patient](#patient-quick-start)

---

## Care Manager Quick Start

**Access**: http://localhost:3000

### Demo Credentials
- **Email**: care.manager@demo.com
- **Password**: Demo2026!

### Your First 5 Minutes

1. **Login** to the Clinical Dashboard
2. **Review Dashboard** - See open care gaps and high-risk patients
3. **Click "Care Gaps"** - View all open gaps
4. **Select a Patient** - Click on "John Diabetes"
5. **Close a Gap** - Mark a gap as addressed

### Quick Links
- 📊 [Dashboard](http://localhost:3000/dashboard)
- 🔍 [Care Gaps](http://localhost:3000/care-gaps)
- 👥 [Patients](http://localhost:3000/patients)
- 📈 [Quality Measures](http://localhost:3000/quality-measures)

### Need Help?
📖 [Full Care Manager Guide](user-guides/care-manager-guide.md)

---

## Physician Quick Start

**Access**: http://localhost:3000

### Demo Credentials
- **Email**: dr.smith@demo.com
- **Password**: Demo2026!

### Your First 5 Minutes

1. **Login** to the Clinical Dashboard
2. **Search Patient** - Type "Sarah Heart"
3. **View Clinical Summary** - Review vitals, labs, medications
4. **Open AI Assistant** - Click AI icon in top right
5. **Ask a Question** - "What are the treatment recommendations for this patient?"

### Quick Links
- 🏥 [Clinical Dashboard](http://localhost:3000/dashboard)
- 👤 [Patient Search](http://localhost:3000/patients)
- 🤖 [AI Assistant](http://localhost:3000/ai-assistant)
- 📋 [CQL Results](http://localhost:3000/cql-results)

### Need Help?
📖 [Full Physician Guide](user-guides/physician-guide.md)

---

## Administrator Quick Start

**Access**: http://localhost:3001

### Demo Credentials
- **Email**: admin@demo.com
- **Password**: Demo2026!

### Your First 5 Minutes

1. **Login** to Admin Portal
2. **View System Health** - Check all services are running
3. **Review Users** - Navigate to User Management
4. **Check Audit Logs** - View recent system activity
5. **Review Integrations** - Check FHIR and EHR connections

### Quick Links
- 🎛️ [Admin Dashboard](http://localhost:3001/dashboard)
- 👥 [User Management](http://localhost:3001/users)
- 📜 [Audit Logs](http://localhost:3001/audit-logs)
- 🔌 [Integrations](http://localhost:3001/integrations)
- 💊 [System Health](http://localhost:3001/system-health)

### Need Help?
📖 [Full Administrator Guide](user-guides/admin-guide.md)

---

## AI Assistant Quick Start

**Access**: http://localhost:3002

### Demo Credentials
- **Email**: ai.user@demo.com
- **Password**: Demo2026!

### Your First 5 Minutes

1. **Login** to AI Assistant Interface
2. **Select Agent** - Choose "Clinical Assistant"
3. **Start Conversation** - Type your clinical question
4. **Watch Tools Execute** - See AI query FHIR, run CQL, analyze data
5. **Review Reasoning** - Understand how AI made decisions

### Example Questions to Try

```
"What are the care gaps for patient John Diabetes?"

"Analyze risk factors for patient Sarah Heart and recommend interventions"

"Run CQL evaluation for all diabetes quality measures for patient MRN001"

"What are the HCC codes for patient Robert Complex?"
```

### Quick Links
- 💬 [Chat Interface](http://localhost:3002/chat)
- 🤖 [Available Agents](http://localhost:3002/agents)
- 🔧 [Tool Library](http://localhost:3002/tools)
- 📊 [Audit Trail](http://localhost:3002/audit)

### Need Help?
📖 [Full AI Assistant Guide](user-guides/ai-assistant-guide.md)

---

## Patient Quick Start

**Access**: http://localhost:3003

### Demo Credentials
- **Email**: patient@demo.com
- **Password**: Demo2026!

### Your First 5 Minutes

1. **Login** to Patient Portal
2. **View Health Summary** - See your current health status
3. **Review Care Gaps** - Understand recommended preventive care
4. **Check Appointments** - View upcoming visits
5. **Send Message** - Contact your care team securely

### Quick Links
- 🏠 [Home](http://localhost:3003/home)
- 💊 [Health Summary](http://localhost:3003/health-summary)
- 📋 [My Care Gaps](http://localhost:3003/care-gaps)
- 📅 [Appointments](http://localhost:3003/appointments)
- 💬 [Messages](http://localhost:3003/messages)

### Need Help?
📖 [Full Patient Portal Guide](user-guides/patient-portal-guide.md)

---

## Common Tasks

### For All Users

#### Change Password
1. Click your profile icon (top right)
2. Select "Settings"
3. Click "Change Password"
4. Enter old and new password
5. Click "Save"

#### Update Profile
1. Click your profile icon
2. Select "Profile"
3. Update your information
4. Click "Save Changes"

#### Get Help
1. Click "Help" in the main menu
2. Options:
   - **User Guides** - Detailed documentation
   - **Training Videos** - Video tutorials
   - **Report Issue** - Submit a bug report
   - **Contact Support** - Email support@hdim.com

---

## System Architecture Overview

### Frontend Applications

| Application | Port | Users | Purpose |
|-------------|------|-------|---------|
| Clinical Dashboard | 3000 | Care Managers, Physicians | Care gap and patient management |
| Admin Portal | 3001 | Administrators | System administration |
| AI Assistant | 3002 | AI Users, Clinicians | AI-powered clinical assistance |
| Patient Portal | 3003 | Patients | Personal health management |
| Analytics | 3004 | Data Analysts | Advanced analytics and reporting |

### Backend Services

| Service | Port | Purpose |
|---------|------|---------|
| Gateway | 8080 | Main API gateway |
| Admin Gateway | 8081 | Admin API gateway |
| Clinical Gateway | 8082 | Clinical API gateway |
| FHIR Gateway | 8083 | FHIR API gateway |
| CQL Engine | 8100 | Quality measure evaluation |
| Care Gap Service | 8101 | Care gap identification |
| Agent Runtime | 8088 | AI agent execution |
| Predictive Analytics | 8105 | Risk prediction models |

---

## Demo Data

The system includes pre-populated demo data:

### Demo Patients

| MRN | Name | Age | Conditions | Purpose |
|-----|------|-----|------------|---------|
| MRN001 | John Diabetes | 58 | Type 2 Diabetes | Quality measure testing |
| MRN002 | Sarah Heart | 65 | CHF, HTN | Complex care example |
| MRN003 | Michael CKD | 51 | CKD Stage 3 | Specialty care example |
| MRN004 | Emma Healthy | 33 | None | Well patient example |
| MRN005 | Robert Complex | 78 | Multiple conditions | High-risk patient example |

### Demo Users

| Email | Role | Password | Access |
|-------|------|----------|--------|
| care.manager@demo.com | Care Manager | Demo2026! | Clinical Dashboard |
| dr.smith@demo.com | Physician | Demo2026! | Clinical Dashboard |
| admin@demo.com | System Admin | Demo2026! | Admin Portal |
| ai.user@demo.com | AI User | Demo2026! | AI Assistant |
| patient@demo.com | Patient | Demo2026! | Patient Portal |

---

## Troubleshooting

### Cannot Login

**Problem**: Login fails or redirects back to login page

**Solutions**:
1. Verify you're using the correct URL for your role
2. Check credentials (email and password are case-sensitive)
3. Clear browser cache and cookies
4. Try a different browser or incognito/private mode
5. Contact support if issue persists

### Page Not Loading

**Problem**: Application shows loading spinner indefinitely

**Solutions**:
1. Check your internet connection
2. Verify the service is running (admin can check system health)
3. Refresh the page (F5 or Ctrl+R)
4. Clear browser cache
5. Check browser console for errors (F12)

### Data Not Appearing

**Problem**: Expected data (patients, gaps, etc.) not showing

**Solutions**:
1. Check filters - you may have active filters hiding data
2. Verify date range selection
3. Ensure data sync has completed (may take up to 24 hours)
4. Check your permissions (some data may be role-restricted)
5. Contact administrator to verify data access

---

## Best Practices

### Security

- ✅ **DO**: Log out when finished
- ✅ **DO**: Use strong, unique passwords
- ✅ **DO**: Lock your computer when away
- ❌ **DON'T**: Share your login credentials
- ❌ **DON'T**: Write down passwords
- ❌ **DON'T**: Access PHI on public computers

### Data Entry

- ✅ **DO**: Document all patient interactions
- ✅ **DO**: Use standardized codes when available
- ✅ **DO**: Review data before saving
- ❌ **DON'T**: Enter fake or test data in production
- ❌ **DON'T**: Copy/paste without verification
- ❌ **DON'T**: Leave incomplete records

### Performance

- ✅ **DO**: Use filters to narrow results
- ✅ **DO**: Close unused tabs and windows
- ✅ **DO**: Export large datasets rather than viewing in browser
- ❌ **DON'T**: Run multiple large reports simultaneously
- ❌ **DON'T**: Keep dozens of tabs open
- ❌ **DON'T**: Attempt to view all patients at once

---

## Next Steps

### After Your First Login

1. **Complete Your Profile**
   - Add your photo
   - Update contact information
   - Set notification preferences

2. **Explore Training Resources**
   - Watch introductory videos
   - Read your role-specific guide
   - Try practice scenarios

3. **Customize Your Dashboard**
   - Arrange widgets to your preference
   - Set up favorite reports
   - Configure alerts

4. **Join a Training Session**
   - Check training calendar
   - Register for role-specific training
   - Attend Q&A sessions

### Learning Paths by Role

**Care Manager**: Dashboard → Care Gaps → Patient Management → Quality Measures → Reports

**Physician**: Patient Search → Clinical Summary → AI Assistant → CQL Results → Documentation

**Administrator**: System Health → User Management → Audit Logs → Integrations → Configuration

**AI User**: Agent Selection → Conversation → Tool Execution → Audit Trail → Advanced Queries

**Patient**: Health Summary → Care Gaps → Appointments → Messages → Documents

---

## Support and Resources

### Contact Information

- **Technical Support**: support@hdim.com
- **Training**: training@hdim.com
- **Billing**: billing@hdim.com
- **General Inquiries**: info@hdim.com

### Support Hours

- **Monday - Friday**: 8:00 AM - 8:00 PM EST
- **Saturday**: 9:00 AM - 5:00 PM EST
- **Sunday**: Closed
- **Emergency**: 24/7 for critical issues

### Documentation Links

- 📚 [Full Documentation](../README.md)
- 🎥 [Video Tutorials](https://training.hdim.com/videos)
- 💡 [FAQ](https://support.hdim.com/faq)
- 🐛 [Known Issues](https://support.hdim.com/known-issues)
- 🔄 [Release Notes](../CHANGELOG.md)

---

**Welcome to HDIM!** We're here to help you provide better care through better data.

---

**Last Updated**: January 14, 2026  
**Version**: 1.0  
**For Support**: support@hdim.com

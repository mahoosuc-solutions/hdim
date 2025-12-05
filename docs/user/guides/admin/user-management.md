---
id: "user-user-management"
title: "User Guide: User Management"
portalType: "user"
path: "user/guides/admin/user-management.md"
category: "user-guide"
subcategory: "feature"
tags: ["user-management", "access-control", "roles", "permissions", "user-provisioning"]
summary: "Add, remove, and manage user access and permissions for platform users."
estimatedReadTime: 6
difficulty: "intermediate"
targetAudience: ["administrator", "it-support"]
prerequisites: ["platform-navigation"]
relatedGuides: ["security-privacy"]
lastUpdated: "2025-12-02"
---

# User Management

Administrator user management maintains secure, appropriate access to the platform.

## User Roles and Permissions

### Available Roles
- **Physician/Provider**: Full clinical access
- **Care Manager**: Patient outreach, care planning
- **Nurse/Clinical Staff**: Clinical documentation, patient care
- **Administrator**: System administration, reporting
- **IT Support**: Technical support, system maintenance
- **Analyst**: Reporting and analytics only
- **Viewer**: Read-only access for compliance

### Role Permissions
Each role has specific permissions:
- What patients they can access
- What actions they can perform
- What reports they can generate
- What settings they can change

System prevents unauthorized actions.

## Adding New Users

### Step 1: Access User Management
1. Click **Admin** in navigation
2. Select **User Management**
3. Click **Add User**

### Step 2: Enter User Information
- **Name**: First and last name
- **Email**: Work email (login credential)
- **Username**: Optional unique identifier
- **Department**: Which department
- **Title**: Job title
- **Credentials**: License numbers (if applicable)

### Step 3: Assign Role
1. Select primary role (Physician, Care Manager, etc.)
2. Can assign multiple roles if needed
3. Role determines default permissions

### Step 4: Set Permissions
1. Confirm role-based permissions are appropriate
2. Can add additional specific permissions:
   - **Patient Access**: Which patients can see (all, specific panel, etc.)
   - **Module Access**: Which features they can use
   - **Reporting**: Which reports they can generate
   - **Administrative**: Can they manage other users, settings, etc.
3. Review before saving

### Step 5: Activation
1. Choose activation date (usually immediate)
2. System sends user welcome email with login instructions
3. User receives temporary password
4. User completes initial setup

## Managing Existing Users

### Updating User Information
1. Find user in list
2. Click **Edit**
3. Update:
   - Contact information
   - Title
   - Department
4. Save changes

### Changing Roles/Permissions
1. Select user
2. Click **Change Role** or **Manage Permissions**
3. Update role and/or specific permissions
4. System applies changes (may take minutes to propagate)
5. Document reason for change

### Deactivating Users
1. Select user
2. Click **Deactivate**
3. Reason (optional but recommended):
   - "Terminated employment"
   - "Moved to different system"
   - "Medical leave"
4. Set deactivation date
5. User loses system access on that date

**Important**: Deactivated users' data remains; only access is removed

### Reactivating Users
1. Find deactivated user
2. Click **Reactivate**
3. Confirm new start date
4. User regains access

## Password Management

### Initial Password
- New users receive temporary password via email
- Password must be changed on first login
- Temporary password expires after 24 hours

### Password Resets
If user forgets password:
1. Click **Forgot Password** on login
2. Enter email
3. Receive password reset link
4. Create new password
5. Login with new password

### Admin Password Reset
If user locked out:
1. Admin goes to User Management
2. Select user
3. Click **Reset Password**
4. System sends user password reset email
5. User follows reset process

### Password Policies
System enforces:
- Minimum length (usually 12 characters)
- Complexity (uppercase, lowercase, numbers, symbols)
- Expiration (every 90 days)
- History (can't reuse recent passwords)

## Multi-Factor Authentication (MFA)

### Enabling MFA
For security, MFA can be required:
1. User logs in
2. Enters username/password
3. Receives code via phone/email
4. Enters code to complete login

### MFA Methods
- **SMS**: Text message with code
- **Email**: Code sent to email
- **Authenticator App**: Code from app (Google Authenticator, Microsoft Authenticator)

### MFA Enforcement
Admin can:
- Require MFA for all users
- Require for specific roles (high-risk)
- Make optional
- Set grace period for adoption

## Access Auditing

### Viewing User Activity
1. Click **Audit Log**
2. View user access history:
   - Who logged in
   - When they accessed system
   - How long they stayed
   - What actions they performed
3. Filter by user, date, action
4. Export for compliance review

### Suspicious Activity
If suspicious activity detected:
1. Review audit log details
2. Verify with user if intended
3. If unauthorized: deactivate user immediately
4. Change passwords if account compromised
5. Document incident

## Security Practices

### User Access Best Practices
1. ✅ Least privilege: Only access needed for role
2. ✅ Remove access when role changes
3. ✅ Deactivate promptly when person leaves
4. ✅ Regular audit of active users
5. ✅ Verify credentials match role
6. ✅ Require MFA for high-risk roles
7. ✅ Monitor access patterns
8. ✅ Document all access changes

### Common Security Issues
❌ Sharing login credentials
❌ Generic accounts (not tied to person)
❌ Inactive accounts never deactivated
❌ Excessive permissions for role
❌ No regular access review
❌ Unchanged default passwords

## Compliance

### Access Documentation
Keep records of:
- Who has access
- When access granted
- When access removed
- Permission changes
- Reason for changes

Required for:
- HIPAA compliance
- Security audits
- Regulatory requirements

### Regular Audits
Periodically review:
- **Quarterly**: Active users list
- **Quarterly**: Inactive accounts (deactivate if needed)
- **Annually**: User permissions appropriateness
- **Annually**: Terminated employee accounts

## Troubleshooting

### "User Can't Login"
**Causes**: Account not activated, wrong credentials, account deactivated
**Solution**: Check user status, reset password, verify activation date

### "User Doesn't Have Access to Patient"
**Causes**: Permissions not set, patient not on user's panel
**Solution**: Review user permissions, verify patient assignment

### "Need to Recover Access to Deactivated Account"
**Solution**: Reactivate user (user data remains intact)

## See Also

- [Security & Privacy](../reference/security.md)
- [Platform Navigation](../getting-started/platform-navigation.md)

## Need Help?

**Support**: IT Administrator, Help Desk

---

**Last Updated**: December 2, 2025
**Document Version**: 1.0

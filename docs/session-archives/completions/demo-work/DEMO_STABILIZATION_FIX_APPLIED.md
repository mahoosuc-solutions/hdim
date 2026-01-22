# Demo Stabilization - Fix Applied

**Date**: January 15, 2026  
**Status**: ✅ **FIX APPLIED - REBUILDING**

---

## Issue Identified

**Error**: `Schema-validation: missing column [account_locked_until] in table [users]`

**Root Cause**: Gateway service migration file (`0001-create-auth-tables.xml`) was missing two columns that the User entity expects:
- `last_login_at` (TIMESTAMP WITH TIME ZONE)
- `account_locked_until` (TIMESTAMP WITH TIME ZONE)

---

## Fix Applied

**File**: `backend/modules/services/gateway-service/src/main/resources/db/changelog/0001-create-auth-tables.xml`

**Change**: Added missing columns to users table:
```sql
last_login_at TIMESTAMP WITH TIME ZONE,
failed_login_attempts INTEGER NOT NULL DEFAULT 0,
account_locked_until TIMESTAMP WITH TIME ZONE,
```

**Status**: ✅ File updated

---

## Next Steps

1. **Rebuild gateway service**: Image needs to be rebuilt with updated migration
2. **Restart service**: Service will recreate schema with new columns
3. **Verify**: Check logs to confirm service starts successfully
4. **Continue demo setup**: Proceed with seeding demo data

---

## User Entity Expected Columns

Based on `User.java` entity:
- ✅ `id` (UUID)
- ✅ `username` (VARCHAR(50))
- ✅ `email` (VARCHAR(100))
- ✅ `password_hash` (VARCHAR(255))
- ✅ `first_name` (VARCHAR(100))
- ✅ `last_name` (VARCHAR(100))
- ✅ `active` (BOOLEAN)
- ✅ `email_verified` (BOOLEAN)
- ✅ `mfa_enabled` (BOOLEAN) - Gateway specific
- ✅ `last_login_at` (TIMESTAMP) - **ADDED**
- ✅ `failed_login_attempts` (INTEGER)
- ✅ `account_locked_until` (TIMESTAMP) - **ADDED**
- ✅ `created_at` (TIMESTAMP)
- ✅ `updated_at` (TIMESTAMP)

---

**Status**: ✅ **FIX APPLIED - REBUILDING SERVICE**

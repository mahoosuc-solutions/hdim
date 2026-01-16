# Test Harness Access Fix

## Issue

Accessing `http://localhost:4200/testing` redirected to the main landing page due to authentication requirements.

## Solution

Created a `DevGuard` that allows access to the testing dashboard in development mode without requiring authentication.

### Changes Made

1. **Created DevGuard** (`apps/clinical-portal/src/app/guards/dev.guard.ts`)
   - Allows access in development mode (`!environment.production`)
   - Requires authentication in production mode
   - Provides security while enabling easy development access

2. **Updated Route Configuration** (`apps/clinical-portal/src/app/app.routes.ts`)
   - Changed `/testing` route to use `DevGuard` instead of `AuthGuard + RoleGuard`
   - Allows direct access in development
   - Still protected in production builds

## Access

### Development Mode
- **URL**: http://localhost:4200/testing
- **Auth**: Not required
- **Status**: ✅ Accessible immediately

### Production Mode
- **URL**: `/testing`
- **Auth**: Requires DEVELOPER or ADMIN role
- **Status**: Protected (as intended)

## Verification

After the fix:
1. Navigate to http://localhost:4200/testing
2. Should load the testing dashboard directly
3. No redirect to login page
4. All features accessible

## Notes

- The development server should hot-reload automatically
- If not, refresh the browser page
- In production builds, authentication will still be required

---

**Fix Applied**: January 15, 2026

# SMS MFA Implementation Guide

## Overview

SMS-based Multi-Factor Authentication (MFA) for the HDIM platform, implemented alongside existing TOTP MFA.

**Issue**: #262 - MFA - SMS Support (P1-High)
**Completion Date**: January 24, 2026

---

## Features

✅ **SMS Code Generation** - 6-digit codes with 5-minute expiration
✅ **Twilio Integration** - Industry-standard SMS delivery via Twilio API
✅ **Rate Limiting** - Max 5 SMS codes per hour per user (prevents abuse)
✅ **Multi-Method Support** - Users can choose TOTP, SMS, or both
✅ **Graceful Fallback** - SMS disabled if Twilio credentials not configured
✅ **HIPAA Compliant** - BCrypt hashing for stored codes, PHI-safe logging

---

## Architecture

### MFA Methods

| Method | Description | User Experience |
|--------|-------------|-----------------|
| `TOTP` | App-based (Google Authenticator, Authy) | Scan QR code, enter 6-digit code from app |
| `SMS` | SMS-based (text message) | Enter phone number, receive code via SMS |
| `BOTH` | Dual MFA (highest security) | User can use either TOTP or SMS |

### Database Schema

**New User Entity Fields**:
```java
private MfaMethod mfaMethod;           // TOTP, SMS, or BOTH
private String mfaPhoneNumber;         // E.164 format: +15555551234
private String smsCode;                // BCrypt hashed
private Instant smsCodeExpiry;         // 5-minute TTL
private Integer smsCodeSentCount;      // Rate limit counter
private Instant smsCodeLastReset;      // Hourly reset timestamp
```

**Liquibase Migration**: `0013-add-sms-mfa-fields.xml`
- Location: `gateway-service/src/main/resources/db/changelog/`
- Rollback included: Full rollback support with column drops

---

## API Endpoints

### 1. Enable SMS MFA
```http
POST /api/v1/auth/mfa/sms/setup
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "phoneNumber": "+15555551234"
}
```

**Response** (200 OK):
```json
{
  "phoneNumber": "****1234",
  "message": "Verification code sent to your phone. Code expires in 5 minutes."
}
```

---

### 2. Confirm SMS MFA Setup
```http
POST /api/v1/auth/mfa/sms/confirm
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "code": "123456"
}
```

**Response** (200 OK):
```json
{}
```

**Effect**: SMS MFA enabled for user. If user already has TOTP, method becomes `BOTH`.

---

### 3. Login (MFA Required Response)
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password123"
}
```

**Response** (200 OK) when MFA enabled:
```json
{
  "mfaRequired": true,
  "mfaToken": "eyJhbGciOiJIUzUxMiJ9...",
  "availableMethods": ["TOTP", "SMS"],
  "smsPhoneNumber": "****1234",
  "message": "MFA verification required. Choose your preferred method."
}
```

---

### 4. Request SMS Code During Login
```http
POST /api/v1/auth/mfa/sms/send
Content-Type: application/json

{
  "mfaToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response** (200 OK):
```json
{
  "phoneNumber": "****1234",
  "message": "Verification code sent. Code expires in 5 minutes."
}
```

**Rate Limit**: Returns 400 if > 5 codes sent in past hour.

---

### 5. Verify MFA Code (SMS or TOTP)
```http
POST /api/v1/auth/mfa/verify
Content-Type: application/json

{
  "mfaToken": "eyJhbGciOiJIUzUxMiJ9...",
  "code": "123456",
  "useRecoveryCode": false
}
```

**Response** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "user@example.com",
  "email": "user@example.com",
  "roles": ["ROLE_USER"],
  "tenantIds": ["tenant-123"],
  "mfaEnabled": true,
  "message": "MFA verification successful"
}
```

**Logic**:
- If `mfaMethod == SMS`: Verify SMS code only
- If `mfaMethod == TOTP`: Verify TOTP code only
- If `mfaMethod == BOTH`: Accept either SMS or TOTP code

---

### 6. Disable SMS MFA
```http
POST /api/v1/auth/mfa/sms/disable
Authorization: Bearer {access_token}
```

**Response** (200 OK):
```json
{}
```

**Effect**:
- If `mfaMethod == SMS`: MFA disabled entirely
- If `mfaMethod == BOTH`: Switches to `TOTP` only

---

## Configuration

### Environment Variables

**Required for SMS MFA**:
```bash
# Enable SMS MFA feature
MFA_SMS_ENABLED=true

# Twilio Configuration
TWILIO_ENABLED=true
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=...
TWILIO_FROM_PHONE_NUMBER=+15555551234
```

**Get Twilio Credentials**:
1. Sign up at https://www.twilio.com
2. Get Account SID and Auth Token from dashboard
3. Purchase a phone number (+1 format recommended)

### Application Properties

**File**: `backend/modules/shared/infrastructure/authentication/src/main/resources/application.yml`

```yaml
# MFA Configuration
mfa:
  sms:
    enabled: ${MFA_SMS_ENABLED:false}
  issuer: ${MFA_ISSUER:HDIM}

# Twilio SMS Configuration
twilio:
  enabled: ${TWILIO_ENABLED:false}
  account-sid: ${TWILIO_ACCOUNT_SID:}
  auth-token: ${TWILIO_AUTH_TOKEN:}
  from-phone-number: ${TWILIO_FROM_PHONE_NUMBER:}
```

---

## Security Features

### 1. Rate Limiting
- **Max 5 SMS codes per hour per user**
- Counter resets automatically after 1 hour
- Prevents SMS abuse and cost explosion
- Stored in database (persists across service restarts)

### 2. Code Expiration
- **5-minute TTL** for all SMS codes
- Expired codes rejected automatically
- Stored as `Instant` in database

### 3. Code Hashing
- SMS codes hashed with BCrypt before storage
- Plaintext code never stored in database
- Verification uses `PasswordEncoder.matches()`

### 4. PHI-Safe Logging
- Phone numbers masked in logs (`****1234`)
- Codes never logged (except in test mode)
- HIPAA-compliant audit trail

### 5. Phone Number Validation
- E.164 format required (`+15555551234`)
- Regex validation: `^\+[1-9]\d{1,14}$`
- Invalid formats rejected with clear error message

---

## Testing

### Unit Tests

**TODO** (not implemented yet):
- `SmsMfaServiceTest.java` - Service layer tests
- `MfaControllerTest.java` - API endpoint tests
- `TwilioSmsClientTest.java` - Twilio integration tests

**Test Coverage Goals**:
- SMS code generation (valid format, uniqueness)
- Code expiration (5-minute TTL)
- Rate limiting (max 5 codes/hour)
- Phone number validation (E.164 format)
- Twilio fallback (graceful degradation)

### Manual Testing

**Prerequisites**:
1. Valid Twilio account with credits
2. Test phone number verified in Twilio console
3. Environment variables configured

**Test Flow**:
```bash
# 1. Enable SMS MFA
curl -X POST http://localhost:8001/api/v1/auth/mfa/sms/setup \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber": "+15555551234"}'

# 2. Check SMS inbox for code

# 3. Confirm setup
curl -X POST http://localhost:8001/api/v1/auth/mfa/sms/confirm \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"code": "123456"}'

# 4. Logout

# 5. Login again (should return mfaRequired=true)
curl -X POST http://localhost:8001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user@example.com", "password": "password123"}'

# 6. Request SMS code
curl -X POST http://localhost:8001/api/v1/auth/mfa/sms/send \
  -H "Content-Type: application/json" \
  -d '{"mfaToken": "..."}'

# 7. Check SMS inbox for new code

# 8. Verify code
curl -X POST http://localhost:8001/api/v1/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -d '{"mfaToken": "...", "code": "123456"}'

# 9. Verify JWT tokens returned
```

---

## Implementation Files

### Core Services
- `SmsMfaService.java` - SMS MFA business logic
- `TwilioSmsClient.java` - Twilio API integration

### Controllers
- `MfaController.java` - SMS MFA endpoints (modified)
- `AuthController.java` - Login flow (modified for availableMethods)

### DTOs
- `SmsMfaSetupRequest.java` - Phone number validation
- `MfaSmsSetupResponse.java` - Setup confirmation
- `SmsCodeVerifyRequest.java` - Code verification
- `MfaSmsSendRequest.java` - SMS send request
- `MfaSmsSendResponse.java` - SMS send confirmation
- `MfaRequiredResponse.java` - Modified for multi-method support

### Database
- `User.java` - Added SMS MFA fields
- `0013-add-sms-mfa-fields.xml` - Liquibase migration

### Configuration
- `build.gradle.kts` - Added Twilio SDK dependency
- `application.yml` - Added Twilio configuration

---

## Dependencies

**New Dependency Added**:
```gradle
// Twilio SDK for SMS MFA
implementation("com.twilio.sdk:twilio:9.14.1")
```

**Transitive Dependencies**:
- Jackson for JSON (already included via Spring)
- Apache HttpClient (included with Twilio SDK)

---

## Troubleshooting

### "Twilio SMS client disabled or not configured"
**Cause**: Twilio credentials not set or `TWILIO_ENABLED=false`
**Fix**: Set environment variables or update `application.yml`

### "SMS rate limit exceeded"
**Cause**: User sent > 5 SMS codes in past hour
**Fix**: Wait for rate limit reset (1 hour) or manually reset in database

### "Invalid phone number format"
**Cause**: Phone number not in E.164 format
**Fix**: Use format `+15555551234` (country code + number)

### "Invalid or expired SMS code"
**Cause**: Code older than 5 minutes or typo
**Fix**: Request new code via `/sms/send` endpoint

### "Failed to send SMS"
**Cause**: Twilio API error (invalid credentials, no credits, unverified number)
**Fix**: Check Twilio console logs, verify phone number, add credits

---

## Cost Considerations

**Twilio Pricing** (as of January 2026):
- **US SMS**: ~$0.0075 per message
- **International SMS**: $0.05 - $0.25 per message (varies by country)
- **Free Trial**: $15 credit (can send ~2,000 US SMS)

**Cost Mitigation**:
- Rate limiting (5 codes/hour) prevents abuse
- Code expiration (5 minutes) reduces resend requests
- Consider alternative: TOTP apps are free (recommend for admins)

---

## Future Enhancements

1. **Voice Calls** - Add voice-based MFA for accessibility
2. **Push Notifications** - Mobile app push notifications
3. **Backup Codes for SMS** - Generate backup codes for SMS users
4. **SMS Template Customization** - Allow tenants to customize SMS message
5. **Multi-Language Support** - Send SMS in user's preferred language
6. **Analytics Dashboard** - Track SMS usage, costs, success rates

---

## Compliance

✅ **HIPAA §164.312(d)** - Multi-factor authentication for administrative access
✅ **HIPAA §164.312(a)(2)(i)** - Unique user identification
✅ **HIPAA §164.312(b)** - Audit trail (all SMS sends logged)
✅ **NIST SP 800-63B** - Multi-factor authentication best practices

---

## Migration Guide

**For Existing Deployments**:

1. **Add Environment Variables**:
   ```bash
   export MFA_SMS_ENABLED=true
   export TWILIO_ENABLED=true
   export TWILIO_ACCOUNT_SID=AC...
   export TWILIO_AUTH_TOKEN=...
   export TWILIO_FROM_PHONE_NUMBER=+15555551234
   ```

2. **Restart Services**:
   ```bash
   docker compose restart gateway-service
   ```

3. **Verify Migration**:
   ```bash
   docker exec -it hdim-postgres psql -U healthdata -d gateway_db -c "\d users"
   # Should show new columns: mfa_method, mfa_phone_number, sms_code, etc.
   ```

4. **Test SMS MFA**:
   - Follow manual testing guide above

---

## Contact

**Issue**: #262 - MFA - SMS Support
**Implementation**: January 24, 2026
**Developer**: Claude Code (via human oversight)
**Review Status**: Pending code review and testing

---

_Last Updated: January 24, 2026_

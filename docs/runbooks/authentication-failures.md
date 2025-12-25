# Runbook: Authentication Failures

**Severity:** High
**Response Time:** < 30 min
**Alert Names:** `HighAuthFailureRate`, `JwtValidationErrors`, `GatewayAuthErrors`

## Symptoms

- Users unable to log in
- 401/403 errors from gateway
- JWT validation failures in logs
- "Invalid token" errors

## Impact Assessment

Authentication issues block all authenticated operations:
- Clinical portal inaccessible
- API calls failing
- Batch processing blocked

## Diagnosis

### 1. Check Gateway Logs
```bash
kubectl logs deployment/gateway-service -n healthdata-prod --since=10m | grep -i "auth\|jwt\|token\|401\|403"
```

### 2. Check Auth Service Logs
```bash
kubectl logs deployment/cql-engine-service -n healthdata-prod --since=10m | grep -i "jwt\|token\|authentication"
```

### 3. Common Error Patterns

| Error | Cause | Solution |
|-------|-------|----------|
| `JWT expired` | Token TTL exceeded | Check token refresh; adjust TTL if needed |
| `Invalid signature` | Secret mismatch | Verify GATEWAY_AUTH_SIGNING_SECRET |
| `Token not found` | Cookie not sent | Check CORS, withCredentials |
| `HMAC validation failed` | Signature mismatch | Check signing secret sync |
| `User not found` | Token references deleted user | Clear user session |

### 4. Verify JWT Configuration
```bash
# Check if secrets are set
kubectl get secret hdim-secrets -n healthdata-prod -o jsonpath='{.data.GATEWAY_AUTH_SIGNING_SECRET}' | base64 -d | wc -c
# Should be >= 32 characters

# Verify same secret across services
for svc in gateway-service cql-engine-service patient-service; do
  echo -n "$svc: "
  kubectl exec deployment/$svc -n healthdata-prod -- printenv GATEWAY_AUTH_SIGNING_SECRET | md5sum
done
# All should have same hash
```

### 5. Check Clock Sync
```bash
# JWT validation is time-sensitive
for pod in $(kubectl get pods -n healthdata-prod -o name | head -5); do
  echo "$pod: $(kubectl exec $pod -n healthdata-prod -- date -u)"
done
```

## Mitigation Steps

### JWT Secret Mismatch

**Step 1: Verify secret in all services**
```bash
kubectl get deployment -n healthdata-prod -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.template.spec.containers[0].env[?(@.name=="GATEWAY_AUTH_SIGNING_SECRET")].valueFrom.secretKeyRef.key}{"\n"}{end}'
```

**Step 2: Update secret if needed**
```bash
kubectl create secret generic hdim-secrets \
  --from-literal=GATEWAY_AUTH_SIGNING_SECRET="$(openssl rand -base64 32)" \
  -n healthdata-prod --dry-run=client -o yaml | kubectl apply -f -
```

**Step 3: Restart all services to pick up new secret**
```bash
kubectl rollout restart deployment -n healthdata-prod
```

### Cookie/CORS Issues

**Step 1: Check gateway CORS config**
```bash
kubectl exec deployment/gateway-service -n healthdata-prod -- cat /app/config/application.yml | grep -A10 cors
```

**Step 2: Verify cookie settings**
- SameSite should be `Strict` or `Lax`
- Secure flag should be `true` for HTTPS
- Domain should match

### Token Refresh Failing

**Step 1: Check refresh token endpoint**
```bash
curl -X POST https://api.healthdata-platform.io/auth/refresh \
  -H "Cookie: hdim_refresh_token=<token>" -v
```

**Step 2: Check if refresh tokens are being issued**
```bash
kubectl logs deployment/gateway-service -n healthdata-prod --since=10m | grep -i refresh
```

### Mass Session Invalidation

If secret was rotated, all existing sessions are invalid:

**Step 1: Communicate to users** - Announce need to re-login

**Step 2: Verify new logins work**
```bash
# Test login endpoint
curl -X POST https://api.healthdata-platform.io/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}' -v
```

## Recovery Verification

1. Login flow works:
```bash
# Test authentication
curl -X POST https://api.healthdata-platform.io/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"<test-user>","password":"<password>"}' \
  -c cookies.txt

# Test authenticated request
curl https://api.healthdata-platform.io/api/patients \
  -b cookies.txt
```

2. Error rate returns to baseline:
   - Check Grafana auth error panel
   - 401/403 rate < 1%

3. Gateway health check:
```bash
curl -s https://api.healthdata-platform.io/actuator/health | jq .status
```

## Escalation

| Condition | Action |
|-----------|--------|
| Cannot identify root cause | Escalate to security team |
| Secret rotation required | Coordinate with team lead |
| CORS/cookie config change needed | Escalate to DevOps |
| Suspected security breach | Escalate to security team immediately |

## Post-Incident

- [ ] Verify no unauthorized access during incident
- [ ] Review authentication logs for anomalies
- [ ] Update secret rotation schedule if needed
- [ ] Document any configuration changes

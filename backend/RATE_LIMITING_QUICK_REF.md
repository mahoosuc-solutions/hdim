# Rate Limiting Quick Reference

## Rate Limit Values

```yaml
Login:      5/minute, 20/hour per IP
Register:   3/hour per IP
API:        100/minute per IP
```

## Key Files

```
Filter:     authentication/filter/RateLimitingFilter.java
Config:     authentication/config/RateLimitConfig.java
Tests:      authentication/filter/RateLimitingFilterTest.java
```

## Configuration

```yaml
rate-limiting:
  enabled: true
  login:
    per-minute: 5
    per-hour: 20
  register:
    per-hour: 3
  api:
    per-minute: 100
```

## Testing

```bash
# Test login (6th request gets 429)
for i in {1..6}; do
  curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}'
done
```

## Monitoring

```bash
# Watch for rate limit hits
tail -f logs/app.log | grep "Rate limit exceeded"

# Count rate limits by IP
grep "Rate limit exceeded" logs/app.log | \
  grep -oP 'IP: \K[0-9.]+' | \
  sort | uniq -c | sort -rn
```

## 429 Response

```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "status": 429,
  "path": "/api/v1/auth/login"
}
```

## Troubleshooting

**Legitimate users blocked?**
- Increase per-minute limit
- Check shared IP (corporate network)
- Add IP whitelist

**Rate limits not working?**
- Check `rate-limiting.enabled=true`
- Verify filter registered in SecurityConfig
- Check Bucket4j dependency present

**Load balancer issues?**
- Ensure X-Forwarded-For header set
- Verify IP extraction in logs

## Filter Chain Order

```
1. RateLimitingFilter (BEFORE auth)
2. UsernamePasswordAuthenticationFilter
3. BasicAuthenticationFilter
4. TenantAccessFilter
```

## Status: PRODUCTION READY

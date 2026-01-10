#!/bin/bash
# Test /auth/me endpoint

echo "Getting fresh token..."
curl -s -X POST http://localhost:8001/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test_admin","password":"password123"}' > /tmp/fresh_login.json

sleep 1

TOKEN=$(grep -o '"accessToken":"[^"]*"' /tmp/fresh_login.json | cut -d'"' -f4)
echo "Token length: ${#TOKEN}"

if [ -z "$TOKEN" ]; then
    echo "Failed to get token!"
    cat /tmp/fresh_login.json
    exit 1
fi

echo ""
echo "Testing /auth/me with fresh token..."
curl -v -H "Authorization: Bearer $TOKEN" http://localhost:8001/api/v1/auth/me 2>&1

echo ""
echo "Gateway logs for this request:"
docker logs hdim-gateway 2>&1 | grep -i "auth/me\|JWT\|validated" | tail -10

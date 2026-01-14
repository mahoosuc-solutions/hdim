#!/bin/bash
#
# Audit Metrics Check Script
# Checks various metrics related to audit integration
#

set -e

echo "=========================================="
echo "Audit Integration Metrics"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check Kafka topic message count
echo -e "${YELLOW}Kafka Topic Statistics:${NC}"
docker exec healthdata-kafka kafka-run-class kafka.tools.GetOffsetShell \
    --broker-list localhost:29092 \
    --topic ai.agent.decisions 2>/dev/null | \
    awk -F: '{sum += $3} END {print "  Total messages: " sum}' || \
    echo "  Topic may not have messages yet"

echo ""

# Check service health
echo -e "${YELLOW}Service Health:${NC}"
CARE_GAP=$(curl -s http://localhost:8086/care-gap/actuator/health 2>/dev/null | jq -r '.status' || echo "UNKNOWN")
CQL_ENGINE=$(curl -s http://localhost:8081/cql-engine/actuator/health 2>/dev/null | jq -r '.status' || echo "UNKNOWN")

if [ "$CARE_GAP" = "UP" ]; then
    echo -e "  Care Gap Service: ${GREEN}✓ UP${NC}"
else
    echo -e "  Care Gap Service: ${YELLOW}⚠ $CARE_GAP${NC}"
fi

if [ "$CQL_ENGINE" = "UP" ]; then
    echo -e "  CQL Engine Service: ${GREEN}✓ UP${NC}"
else
    echo -e "  CQL Engine Service: ${YELLOW}⚠ $CQL_ENGINE${NC}"
fi

echo ""

# Check Kafka connectivity from services
echo -e "${YELLOW}Kafka Connectivity:${NC}"
KAFKA_HEALTH=$(docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:29092 --list > /dev/null 2>&1 && echo "OK" || echo "FAILED")
if [ "$KAFKA_HEALTH" = "OK" ]; then
    echo -e "  Kafka: ${GREEN}✓ Accessible${NC}"
else
    echo -e "  Kafka: ${YELLOW}⚠ Not accessible${NC}"
fi

echo ""

# Check recent audit-related logs
echo -e "${YELLOW}Recent Audit Activity:${NC}"
CARE_GAP_LOGS=$(docker logs healthdata-care-gap-service 2>&1 | grep -i "audit\|agentId" | wc -l)
CQL_ENGINE_LOGS=$(docker logs healthdata-cql-engine-service 2>&1 | grep -i "audit\|agentId" | wc -l)

echo "  Care Gap Service audit log entries: $CARE_GAP_LOGS"
echo "  CQL Engine Service audit log entries: $CQL_ENGINE_LOGS"

echo ""

# Check topic partitions
echo -e "${YELLOW}Topic Configuration:${NC}"
docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:29092 \
    --describe --topic ai.agent.decisions 2>/dev/null | \
    grep -E "PartitionCount|ReplicationFactor" | \
    sed 's/^/  /' || echo "  Topic details not available"

echo ""
echo "=========================================="


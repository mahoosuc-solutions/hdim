#!/bin/bash
#
# Audit Event Monitoring Script
# Monitors Kafka for audit events and displays them in real-time
#

set -e

KAFKA_CONTAINER="healthdata-kafka"
KAFKA_BOOTSTRAP="localhost:29092"
TOPIC="ai.agent.decisions"

echo "=========================================="
echo "Audit Event Monitor"
echo "=========================================="
echo ""
echo "Monitoring topic: $TOPIC"
echo "Press Ctrl+C to stop"
echo ""

# Check if Kafka is accessible
if ! docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $KAFKA_BOOTSTRAP --list > /dev/null 2>&1; then
    echo "❌ Error: Kafka is not accessible"
    exit 1
fi

# Check if topic exists
if ! docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $KAFKA_BOOTSTRAP --list | grep -q "^${TOPIC}$"; then
    echo "⚠️  Topic '$TOPIC' does not exist yet. It will be created when first event is published."
    echo "Waiting for events..."
    echo ""
fi

# Monitor Kafka for audit events
echo "Listening for audit events..."
echo "Format: [Partition Key] | [Timestamp] | [Event JSON]"
echo "----------------------------------------"
echo ""

docker exec -it $KAFKA_CONTAINER kafka-console-consumer \
    --bootstrap-server $KAFKA_BOOTSTRAP \
    --topic $TOPIC \
    --from-beginning \
    --property print.key=true \
    --property print.timestamp=true \
    --property print.partition=true \
    --property key.separator=" | " \
    --property timestamp.separator=" | " \
    2>&1 | while IFS= read -r line; do
    # Color code based on agent type
    if [[ $line == *"care-gap-identifier"* ]]; then
        echo -e "\033[0;32m[Care Gap] $line\033[0m"
    elif [[ $line == *"cql-engine"* ]]; then
        echo -e "\033[0;34m[CQL Engine] $line\033[0m"
    else
        echo "$line"
    fi
done


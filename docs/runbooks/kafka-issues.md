# Runbook: Kafka Issues

**Severity:** High
**Response Time:** < 30 min
**Alert Names:** `KafkaDown`, `HighKafkaConsumerLag`, `KafkaProducerErrors`

## Symptoms

- Event processing stopped
- Consumer lag increasing
- Producer timeout errors
- Care gap notifications delayed

## Impact Assessment

Kafka issues affect async processing:
- CQL evaluation events not processed
- Care gap notifications delayed
- Audit events not persisted
- Analytics data pipeline stalled

## Diagnosis

### 1. Check Kafka Broker Status
```bash
# Kubernetes
kubectl exec -it statefulset/kafka -n healthdata-prod -- \
  kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Docker
docker compose exec kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### 2. Check Topic Health
```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Describe topic
kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic evaluation-events

# Check under-replicated partitions
kafka-topics.sh --bootstrap-server localhost:9092 --describe --under-replicated-partitions
```

### 3. Check Consumer Lag
```bash
# List consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Check lag for specific group
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group cql-engine-consumer

# Key columns: CURRENT-OFFSET, LOG-END-OFFSET, LAG
```

### 4. Check Service Connectivity
```bash
# From affected service
kubectl exec -it deployment/event-processing-service -n healthdata-prod -- \
  nc -zv kafka:9092
```

### 5. Check Logs
```bash
# Kafka broker logs
kubectl logs statefulset/kafka -n healthdata-prod --tail=100

# Consumer service logs
kubectl logs deployment/event-processing-service -n healthdata-prod --since=10m | grep -i kafka
```

## Mitigation Steps

### Kafka Broker Down

**Step 1: Check ZooKeeper (if using)**
```bash
kubectl exec -it statefulset/zookeeper -n healthdata-prod -- \
  zkCli.sh -server localhost:2181 stat
```

**Step 2: Restart Kafka**
```bash
# Kubernetes
kubectl rollout restart statefulset/kafka -n healthdata-prod

# Docker
docker compose restart kafka
```

**Step 3: Verify broker is up**
```bash
kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

### High Consumer Lag

**Step 1: Check consumer is running**
```bash
kubectl get pods -n healthdata-prod -l app=event-processing-service
```

**Step 2: Scale up consumers**
```bash
kubectl scale deployment/event-processing-service --replicas=5 -n healthdata-prod
```

**Step 3: Check for processing errors**
```bash
kubectl logs deployment/event-processing-service -n healthdata-prod --since=10m | grep -i error
```

**Step 4: Reset consumer offset (if needed, CAUTION)**
```bash
# Stop consumers first
kubectl scale deployment/event-processing-service --replicas=0 -n healthdata-prod

# Reset to latest (skip backlog)
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group cql-engine-consumer \
  --topic evaluation-events \
  --reset-offsets --to-latest --execute

# Restart consumers
kubectl scale deployment/event-processing-service --replicas=3 -n healthdata-prod
```

### Producer Errors

**Step 1: Check producer logs**
```bash
kubectl logs deployment/cql-engine-service -n healthdata-prod --since=10m | grep -i "kafka\|producer"
```

**Step 2: Check broker availability**
```bash
kubectl exec -it deployment/cql-engine-service -n healthdata-prod -- \
  nc -zv kafka:9092
```

**Step 3: Restart producer service**
```bash
kubectl rollout restart deployment/cql-engine-service -n healthdata-prod
```

### Disk Space Issues

**Step 1: Check Kafka disk usage**
```bash
kubectl exec -it statefulset/kafka -n healthdata-prod -- df -h /var/lib/kafka
```

**Step 2: Reduce retention**
```bash
kafka-configs.sh --bootstrap-server localhost:9092 \
  --entity-type topics --entity-name evaluation-events \
  --alter --add-config retention.ms=86400000  # 1 day
```

**Step 3: Force log cleanup**
```bash
kafka-log-dirs.sh --bootstrap-server localhost:9092 --describe
```

## Recovery Verification

1. Consumer lag decreasing:
```bash
# Run multiple times, LAG should decrease
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --describe --group cql-engine-consumer | tail -5
```

2. Producers can send:
```bash
# Test produce
echo "test" | kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-topic
```

3. No errors in service logs:
```bash
kubectl logs deployment/event-processing-service -n healthdata-prod --since=5m | grep -c ERROR
# Should be 0 or near 0
```

4. Grafana Kafka dashboard shows healthy metrics

## Escalation

| Condition | Action |
|-----------|--------|
| Broker won't start | Escalate to infrastructure team |
| Data loss suspected | Escalate to team lead + data team |
| ZooKeeper issues | Escalate to infrastructure team |
| Consumer lag > 1 hour | Escalate for capacity review |

## Post-Incident

- [ ] Review retention policies
- [ ] Check partition count for high-volume topics
- [ ] Review consumer scaling settings
- [ ] Document any configuration changes

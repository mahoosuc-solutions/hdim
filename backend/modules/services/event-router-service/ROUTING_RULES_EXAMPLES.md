# Event Router - Routing Rules Examples

## Table of Contents
1. [Basic Routing](#basic-routing)
2. [Filtered Routing](#filtered-routing)
3. [Transformation Routing](#transformation-routing)
4. [Advanced Examples](#advanced-examples)

## Basic Routing

### Example 1: Simple Topic Routing
Route all patient creation events to processing queue.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    priority, enabled
) VALUES (
    'tenant1',
    'Patient Creation Routing',
    'Route all patient creations to processing queue',
    'fhir.patient.created',
    'patient.processing.queue',
    'MEDIUM',
    true
);
```

### Example 2: High Priority Observations
Route observation events with high priority.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    priority, enabled
) VALUES (
    'tenant1',
    'Observation Routing',
    'Route observations to analytics pipeline',
    'fhir.observation.created',
    'analytics.observations',
    'HIGH',
    true
);
```

## Filtered Routing

### Example 3: Critical Patients Only
Route only critical/urgent patient updates.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    filter_expression, priority, enabled
) VALUES (
    'tenant1',
    'Critical Patients Fast Track',
    'Route only urgent patient updates to critical queue',
    'fhir.patient.updated',
    'critical.patient.processing',
    '{"urgent": true}',
    'CRITICAL',
    true
);
```

### Example 4: Age-Based Routing
Route patients over 65 to geriatric care queue.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    filter_expression, priority, enabled
) VALUES (
    'tenant1',
    'Geriatric Care Routing',
    'Route patients 65+ to geriatric care',
    'fhir.patient.created',
    'geriatric.care.queue',
    '{"age": {"$gte": 65}}',
    'HIGH',
    true
);
```

### Example 5: Geographic Routing
Route patients from specific states to regional queues.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    filter_expression, priority, enabled
) VALUES (
    'tenant1',
    'Northeast Region Routing',
    'Route patients from MA, NY, CT to northeast queue',
    'fhir.patient.created',
    'northeast.regional.queue',
    '{"address.state": {"$in": ["MA", "NY", "CT"]}}',
    'MEDIUM',
    true
);
```

### Example 6: Lab Result Routing
Route abnormal lab results to review queue.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    filter_expression, priority, enabled
) VALUES (
    'tenant1',
    'Abnormal Labs Routing',
    'Route abnormal lab results to physician review',
    'fhir.observation.created',
    'lab.review.queue',
    '{"valueQuantity.value": {"$gt": 100}, "status": "final"}',
    'HIGH',
    true
);
```

## Transformation Routing

### Example 7: Add Timestamps
Enrich events with processing timestamps.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    transformation_script, priority, enabled
) VALUES (
    'tenant1',
    'Timestamp Enrichment',
    'Add processing timestamp to all events',
    'fhir.patient.created',
    'enriched.patient.queue',
    'enrichment:add-timestamp,add-source',
    'MEDIUM',
    true
);
```

### Example 8: Mask Sensitive Data
Mask SSN and email before routing to analytics.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    transformation_script, priority, enabled
) VALUES (
    'tenant1',
    'Anonymized Analytics',
    'Mask PII before sending to analytics',
    'fhir.patient.updated',
    'analytics.patient.queue',
    'mask:ssn,email,phone',
    'MEDIUM',
    true
);
```

### Example 9: Field Renaming
Rename fields for downstream compatibility.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    transformation_script, priority, enabled
) VALUES (
    'tenant1',
    'Legacy System Compatibility',
    'Rename fields for legacy system',
    'fhir.patient.created',
    'legacy.patient.queue',
    'rename:patientId->patient_id,createdAt->created_at',
    'LOW',
    true
);
```

### Example 10: Complex Transformation Chain
Multiple transformations in sequence.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    transformation_script, priority, enabled
) VALUES (
    'tenant1',
    'Full Processing Pipeline',
    'Complete transformation pipeline',
    'fhir.patient.created',
    'processed.patient.queue',
    'enrichment:add-timestamp|mask:ssn|rename:old_field->new_field|remove:temp_field',
    'MEDIUM',
    true
);
```

## Advanced Examples

### Example 11: Multi-Criteria Filtering
Complex filter with multiple conditions.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    filter_expression, priority, enabled
) VALUES (
    'tenant1',
    'High-Risk Patient Identification',
    'Identify high-risk patients for proactive care',
    'fhir.patient.updated',
    'risk.assessment.queue',
    '{
        "age": {"$gte": 65},
        "conditions": {"$exists": true},
        "medications.count": {"$gte": 5},
        "address.state": {"$in": ["MA", "NY"]}
    }',
    'CRITICAL',
    true
);
```

### Example 12: Regex Pattern Matching
Route based on email domain.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    filter_expression, priority, enabled
) VALUES (
    'tenant1',
    'Internal Patients',
    'Route employees to internal queue',
    'fhir.patient.created',
    'internal.employee.queue',
    '{"email": {"$regex": ".*@healthdata\\\\.com$"}}',
    'HIGH',
    true
);
```

### Example 13: Conditional Transformation
Transform only if conditions are met.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    filter_expression, transformation_script, priority, enabled
) VALUES (
    'tenant1',
    'Conditional Masking',
    'Mask data only for external reporting',
    'fhir.patient.updated',
    'external.reporting.queue',
    '{"reportType": "external"}',
    'mask:ssn,email,phone|enrichment:add-timestamp',
    'MEDIUM',
    true
);
```

### Example 14: Type Conversion
Convert data types for downstream systems.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    transformation_script, priority, enabled
) VALUES (
    'tenant1',
    'Type Conversion for Analytics',
    'Convert string numbers to integers',
    'fhir.observation.created',
    'analytics.typed.queue',
    'convert:age->integer,value->double,active->boolean',
    'MEDIUM',
    true
);
```

### Example 15: Object Flattening
Flatten nested JSON for relational databases.

```sql
INSERT INTO event_routing_rules (
    tenant_id, rule_name, description,
    source_topic, target_topic,
    transformation_script, priority, enabled
) VALUES (
    'tenant1',
    'Flatten for SQL Storage',
    'Flatten nested patient object for SQL',
    'fhir.patient.created',
    'sql.storage.queue',
    'flatten:patient|flatten:address',
    'LOW',
    true
);
```

## Filter Expression Reference

### Operators Supported

| Operator | Description | Example |
|----------|-------------|---------|
| `=` | Equality | `{"status": "active"}` |
| `$gte` | Greater than or equal | `{"age": {"$gte": 18}}` |
| `$gt` | Greater than | `{"score": {"$gt": 75}}` |
| `$lte` | Less than or equal | `{"age": {"$lte": 65}}` |
| `$lt` | Less than | `{"value": {"$lt": 100}}` |
| `$ne` | Not equal | `{"status": {"$ne": "deleted"}}` |
| `$in` | In array | `{"state": {"$in": ["MA", "NY"]}}` |
| `$exists` | Field exists | `{"email": {"$exists": true}}` |
| `$regex` | Regex pattern | `{"email": {"$regex": ".*@example\\\\.com"}}` |

### Transformation Commands

| Command | Description | Example |
|---------|-------------|---------|
| `enrichment` | Add metadata | `enrichment:add-timestamp,add-source` |
| `rename` | Rename fields | `rename:oldName->newName` |
| `remove` | Remove fields | `remove:field1,field2` |
| `convert` | Type conversion | `convert:age->integer,score->double` |
| `mask` | Mask sensitive data | `mask:ssn,email,phone` |
| `flatten` | Flatten nested objects | `flatten:patient,address` |
| `js` | JavaScript transform | `js:payload.put('full', payload.get('first'))` |

## Testing Rules

### Test a Filter Expression
```javascript
// JavaScript test
const filter = {"age": {"$gte": 65}};
const event = {"age": 70};
// Should match: true
```

### Test a Transformation
```javascript
// Before: {"ssn": "123-45-6789"}
// Transform: "mask:ssn"
// After: {"ssn": "***-**-****"}
```

## Best Practices

1. **Priority Assignment**
   - CRITICAL: System failures, critical patient alerts
   - HIGH: Patient care events, urgent updates
   - MEDIUM: Standard processing, routine updates
   - LOW: Analytics, reporting, batch jobs

2. **Filter Complexity**
   - Keep filters simple for better performance
   - Use indexed fields when possible
   - Test filters with sample data first

3. **Transformation Order**
   - Enrichment → Masking → Renaming → Removal
   - Most expensive operations last
   - Test chained transformations incrementally

4. **Rule Naming**
   - Use descriptive names
   - Include tenant/region if applicable
   - Document purpose in description field

5. **Testing**
   - Test rules in dev environment first
   - Monitor DLQ for filtered/failed events
   - Review metrics regularly

---

For complete documentation, see [EVENT_ROUTER_SERVICE_GUIDE.md](../../../EVENT_ROUTER_SERVICE_GUIDE.md)

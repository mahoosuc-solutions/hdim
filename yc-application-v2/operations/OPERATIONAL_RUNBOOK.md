# HDIM Operational Runbook

> Day-to-day operational procedures, incident response, monitoring, and disaster recovery.

---

## Overview

This runbook provides operational procedures for maintaining HDIM platform reliability, security, and performance. All team members with production access must be familiar with these procedures.

**On-Call Rotation:** 24/7 coverage via PagerDuty
**Primary Escalation:** Slack #ops-alerts
**Secondary Escalation:** Phone tree

---

## Daily Operations Checklist

### Morning Health Check (9:00 AM ET)

**Automated Checks (Dashboard Review):**
- [ ] All services showing green in monitoring dashboard
- [ ] No critical alerts in past 12 hours
- [ ] API response times within SLA (<500ms p95)
- [ ] Error rate below threshold (<0.1%)
- [ ] Queue depths normal (< 1000 pending jobs)

**Manual Checks:**
- [ ] Review overnight alerts and resolutions
- [ ] Check customer-reported issues in support queue
- [ ] Verify scheduled jobs completed successfully
- [ ] Review upcoming maintenance windows
- [ ] Check certificate expiration (30-day warning)

### System Status Updates

| Frequency | Action |
|-----------|--------|
| Hourly | Automated health checks |
| Daily | Manual review, status page update |
| Weekly | Performance review, capacity planning |
| Monthly | Security patches, compliance review |

---

## Monitoring & Alerting

### Monitoring Stack

| Component | Tool | Purpose |
|-----------|------|---------|
| Infrastructure | AWS CloudWatch | EC2, RDS, ELB metrics |
| Application | Datadog | APM, custom metrics |
| Logs | CloudWatch Logs + Datadog | Centralized logging |
| Uptime | Pingdom | External availability |
| Errors | Sentry | Error tracking |
| On-call | PagerDuty | Alert routing |

### Key Metrics to Monitor

**Availability:**
| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| API uptime | 99.9% | < 99.5% (15 min) |
| Web app uptime | 99.9% | < 99.5% (15 min) |
| Database uptime | 99.99% | Any outage |

**Performance:**
| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| API p50 latency | < 100ms | > 200ms (5 min avg) |
| API p95 latency | < 500ms | > 1000ms (5 min avg) |
| API p99 latency | < 1000ms | > 2000ms (5 min avg) |
| CQL evaluation | < 200ms | > 500ms (5 min avg) |

**Errors:**
| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| 5xx error rate | < 0.1% | > 0.5% (5 min) |
| 4xx error rate | < 5% | > 10% (5 min) |
| Unhandled exceptions | 0 | Any occurrence |

**Resources:**
| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| CPU utilization | < 70% | > 85% (10 min avg) |
| Memory utilization | < 80% | > 90% |
| Disk utilization | < 70% | > 85% |
| Database connections | < 80% max | > 90% max |

### Alert Severity Levels

| Severity | Response Time | Examples |
|----------|---------------|----------|
| **P1 - Critical** | 15 minutes | Service down, data breach, security incident |
| **P2 - High** | 1 hour | Degraded performance, partial outage |
| **P3 - Medium** | 4 hours | Non-critical feature broken, elevated errors |
| **P4 - Low** | 24 hours | Cosmetic issues, minor degradation |

---

## Incident Response

### Incident Classification

| Category | Description | Examples |
|----------|-------------|----------|
| **Availability** | Service unavailable | API down, web app unreachable |
| **Performance** | Degraded response times | Slow queries, high latency |
| **Data** | Data integrity or access issues | Calculation errors, missing data |
| **Security** | Security-related incidents | Unauthorized access, breach attempt |
| **Integration** | Third-party integration failure | EHR connection down, FHIR errors |

### Incident Response Process

**1. Detection (0-5 min)**
- Alert received via PagerDuty
- On-call engineer acknowledges
- Initial assessment of impact

**2. Triage (5-15 min)**
- Confirm incident is real (not false positive)
- Determine severity level
- Identify affected customers
- Begin incident channel (#incident-YYYYMMDD-N)

**3. Response (15 min - resolution)**
- Engage additional team members if needed
- Implement fix or workaround
- Communicate status to stakeholders
- Document actions taken

**4. Resolution**
- Confirm service restored
- Verify fix with monitoring
- Close incident channel
- Update status page

**5. Post-Incident (within 48 hours)**
- Schedule post-mortem for P1/P2 incidents
- Document root cause
- Identify preventive measures
- Update runbooks if needed

### Incident Commander Responsibilities

- Coordinate response efforts
- Make decisions on response actions
- Manage communications
- Escalate as needed
- Ensure documentation

### Communication Templates

**Internal Alert:**
```
🚨 INCIDENT DETECTED
Severity: P1/P2/P3/P4
Service: [affected service]
Impact: [customer impact description]
Status: Investigating/Mitigating/Resolved
Lead: [name]
Channel: #incident-YYYYMMDD-N
```

**Customer Communication (P1/P2):**
```
Subject: HDIM Service Disruption - [Brief Description]

We are currently experiencing [description of issue].

Impact: [what customers are experiencing]
Status: Our team is actively working on resolution.
ETA: [if known, otherwise "We will provide updates every 30 minutes"]

We apologize for any inconvenience and will notify you when service is restored.
```

**Resolution Communication:**
```
Subject: RESOLVED - HDIM Service Disruption

The service disruption reported earlier has been resolved.

Duration: [start time] to [end time] ([duration])
Root Cause: [brief description]
Resolution: [what was done]

We apologize for the inconvenience. If you continue to experience issues, please contact support.
```

---

## Common Operational Procedures

### Procedure 1: Deploying a Hotfix

**When to Use:** Critical bug fix requiring immediate deployment

**Prerequisites:**
- Code reviewed and approved
- Tests passing
- Rollback plan ready

**Steps:**
1. Announce deployment in #ops-deployments
2. Create deployment branch from main
3. Merge hotfix to deployment branch
4. Run automated tests
5. Deploy to staging, verify fix
6. Deploy to production (rolling deployment)
7. Monitor for 15 minutes
8. Confirm successful deployment
9. Update deployment log

**Rollback:**
If issues detected, immediately:
1. Announce rollback in #ops-deployments
2. Run rollback command: `kubectl rollout undo deployment/[service]`
3. Verify rollback successful
4. Investigate issue

---

### Procedure 2: Database Maintenance

**When to Use:** Scheduled maintenance, schema migrations

**Prerequisites:**
- Maintenance window scheduled (typically Sunday 2-6 AM ET)
- Customer notification sent (24 hours prior)
- Backup verified

**Steps:**
1. Enable maintenance mode (read-only for writes)
2. Take fresh database snapshot
3. Verify snapshot completion
4. Execute maintenance/migration
5. Verify data integrity
6. Disable maintenance mode
7. Monitor application metrics
8. Update maintenance log

---

### Procedure 3: Certificate Renewal

**When to Use:** TLS certificates expiring within 30 days

**Prerequisites:**
- New certificate obtained from CA
- Certificate validated

**Steps:**
1. Upload new certificate to AWS Certificate Manager
2. Update load balancer configuration
3. Verify HTTPS still working
4. Update certificate inventory
5. Set next renewal reminder

**Automated Alternative:**
- AWS ACM provides auto-renewal for managed certificates
- Verify auto-renewal is enabled for all certificates

---

### Procedure 4: Scaling Services

**When to Use:** Increased load, capacity planning

**Horizontal Scaling (Add Instances):**
```bash
# Scale API service
kubectl scale deployment/api-service --replicas=N

# Verify scaling
kubectl get pods -l app=api-service
```

**Vertical Scaling (Larger Instances):**
1. Update instance type in Terraform
2. Apply changes during maintenance window
3. Verify service stability

**Database Scaling:**
1. Create read replicas for read-heavy loads
2. Upgrade instance class during maintenance window
3. Consider Aurora Serverless for variable workloads

---

### Procedure 5: Log Analysis

**When to Use:** Investigating issues, auditing

**Access Logs:**
```bash
# CloudWatch Logs via AWS CLI
aws logs filter-log-events \
  --log-group-name /hdim/api \
  --start-time [epoch_ms] \
  --end-time [epoch_ms] \
  --filter-pattern "[pattern]"
```

**Datadog Queries:**
- Service logs: `service:hdim-api status:error`
- User activity: `@usr.id:[user_id]`
- API calls: `@http.url:/api/v1/*`

**Retention:**
| Log Type | Retention |
|----------|-----------|
| Application logs | 90 days |
| Security logs | 7 years |
| Audit logs | 7 years |
| Debug logs | 7 days |

---

## Escalation Matrix

### Technical Escalation

| Level | Who | When to Escalate |
|-------|-----|------------------|
| L1 | On-call engineer | First responder, initial triage |
| L2 | Senior engineer | L1 cannot resolve in 30 min |
| L3 | Tech lead/architect | Complex issues, architecture decisions |
| L4 | CTO | Major outages, security incidents |

### Business Escalation

| Impact | Who to Notify | When |
|--------|---------------|------|
| Single customer affected | Customer Success Manager | Within 1 hour |
| Multiple customers affected | VP Customer Success | Within 30 min |
| All customers affected | CEO, Executive team | Immediately |
| Security incident | CEO, Legal | Immediately |

### Contact Information

| Role | Primary | Backup |
|------|---------|--------|
| On-call Engineer | PagerDuty rotation | - |
| Engineering Lead | [Name] | [Name] |
| CTO | [Name] | [Name] |
| VP Customer Success | [Name] | [Name] |
| CEO | [Name] | [Name] |

---

## Disaster Recovery

### Recovery Objectives

| Metric | Target | Notes |
|--------|--------|-------|
| RTO (Recovery Time Objective) | 4 hours | Time to restore service |
| RPO (Recovery Point Objective) | 1 hour | Maximum data loss |

### Backup Strategy

| Data Type | Frequency | Retention | Location |
|-----------|-----------|-----------|----------|
| Database (RDS) | Continuous (point-in-time) | 35 days | AWS us-east-1 + us-west-2 |
| Database snapshots | Daily | 90 days | AWS us-east-1 + us-west-2 |
| File storage (S3) | Cross-region replication | Indefinite | AWS us-east-1 + us-west-2 |
| Configuration | Git + encrypted backup | Indefinite | GitHub + S3 |
| Secrets | Vault replication | - | Primary + DR region |

### Disaster Scenarios

**Scenario 1: Single Service Failure**
- Impact: One microservice unavailable
- Response: Auto-scaling, manual restart
- RTO: 5-15 minutes

**Scenario 2: Availability Zone Failure**
- Impact: Degraded performance, potential brief outage
- Response: Traffic shifts to other AZs automatically
- RTO: 5-30 minutes

**Scenario 3: Region Failure**
- Impact: Full service outage
- Response: Failover to DR region
- RTO: 2-4 hours

**Scenario 4: Data Corruption**
- Impact: Incorrect data in production
- Response: Point-in-time recovery from backup
- RTO: 1-4 hours (depending on extent)

### DR Failover Procedure

**Initiate Failover:**
1. Confirm primary region is unavailable
2. Get approval from Incident Commander
3. Update DNS to point to DR region
4. Verify DR services are healthy
5. Communicate to customers

**Failover Steps:**
```
1. Assess primary region status
2. Promote DR database to primary
3. Update application configuration
4. Route 53 failover (if not automatic)
5. Verify all services operational
6. Monitor closely for issues
7. Communicate resolution
```

**Failback Procedure:**
1. Confirm primary region restored
2. Replicate data from DR to primary
3. Verify data integrity
4. Schedule maintenance window
5. Failback during maintenance
6. Monitor for issues
7. Return to normal operations

---

## Security Operations

### Daily Security Checks

- [ ] Review security alerts from AWS GuardDuty
- [ ] Check for failed login attempts
- [ ] Review privileged access logs
- [ ] Verify no unauthorized API keys created
- [ ] Check vulnerability scan results

### Access Management

**Production Access:**
- Requires MFA
- Limited to on-call rotation
- All access logged
- Just-in-time access (temporary)

**Access Request:**
1. Submit request via internal ticketing
2. Manager approval required
3. Security team review (production)
4. Access granted with expiration
5. Audit log created

**Access Revocation:**
- Immediate for security concerns
- End of day for terminations
- Quarterly access review for all

### Security Incident Response

**Classification:**
| Severity | Examples | Response |
|----------|----------|----------|
| Critical | Active breach, data exfiltration | Immediate containment, executive notification |
| High | Unauthorized access attempt, vulnerability exploited | 1-hour response, investigation |
| Medium | Suspicious activity, policy violation | 4-hour response, investigation |
| Low | Failed attacks, minor policy issues | 24-hour response |

**Response Steps:**
1. Contain the incident (isolate affected systems)
2. Preserve evidence (logs, snapshots)
3. Investigate scope and impact
4. Eradicate threat
5. Recover systems
6. Conduct post-incident review
7. Report (to customers, regulators if required)

---

## Maintenance Windows

### Standard Maintenance

| Type | Window | Notice |
|------|--------|--------|
| Routine maintenance | Sunday 2-6 AM ET | 72 hours |
| Security patches | Sunday 2-6 AM ET | 72 hours |
| Emergency patches | ASAP | As much as possible |
| Database maintenance | Sunday 2-6 AM ET | 1 week |

### Maintenance Notification Template

```
Subject: Scheduled Maintenance - [Date]

We will be performing scheduled maintenance on [date] from [time] to [time] ET.

What to expect:
- [Brief description of work]
- [Expected impact, if any]
- [Duration]

During this window, you may experience [specific impacts, e.g., brief API delays].

No action is required on your part. We will notify you when maintenance is complete.

Questions? Contact support@healthdatainmotion.com
```

---

## Appendix: Quick Reference

### Useful Commands

**Kubernetes:**
```bash
# Get pod status
kubectl get pods -n production

# View logs
kubectl logs -f deployment/api-service -n production

# Restart deployment
kubectl rollout restart deployment/api-service -n production

# Scale deployment
kubectl scale deployment/api-service --replicas=N -n production
```

**Database:**
```bash
# Connect to RDS
psql -h [hostname] -U [user] -d hdim_production

# Check replication lag
SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;

# Active connections
SELECT count(*) FROM pg_stat_activity;
```

**AWS:**
```bash
# Check EC2 status
aws ec2 describe-instance-status --region us-east-1

# RDS status
aws rds describe-db-instances --region us-east-1

# Clear CloudWatch alarm
aws cloudwatch set-alarm-state --alarm-name [name] --state-value OK
```

### Key URLs

| Resource | URL |
|----------|-----|
| Monitoring Dashboard | monitoring.internal.hdim.com |
| Status Page | status.healthdatainmotion.com |
| PagerDuty | hdim.pagerduty.com |
| AWS Console | aws.amazon.com/console |
| Datadog | app.datadoghq.com |

### Emergency Contacts

| Role | Contact |
|------|---------|
| AWS Support | aws.amazon.com/support (Enterprise) |
| Datadog Support | support.datadoghq.com |
| PagerDuty | support@pagerduty.com |
| Security Emergency | security@healthdatainmotion.com |

---

*Last Updated: December 2025*
*Review Cadence: Quarterly*
*Owner: Engineering Operations*

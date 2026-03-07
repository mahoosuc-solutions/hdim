# Release Preflight Stability Report

- Version: `v0.0.0-test`
- Compose file: `docker-compose.demo.yml`
- Generated at: `2026-03-07T06:24:01Z`

## Required Containers

| Container | Status | Gate |
|---|---|---|
| `hdim-demo-postgres` | `healthy` | PASS |
| `hdim-demo-redis` | `healthy` | PASS |
| `hdim-demo-kafka` | `healthy` | PASS |
| `hdim-demo-fhir` | `healthy` | PASS |
| `hdim-demo-patient` | `healthy` | PASS |
| `hdim-demo-care-gap` | `healthy` | PASS |
| `hdim-demo-quality-measure` | `healthy` | PASS |
| `hdim-demo-events` | `healthy` | PASS |
| `hdim-demo-gateway-fhir` | `healthy` | PASS |
| `hdim-demo-gateway-admin` | `healthy` | PASS |
| `hdim-demo-gateway-clinical` | `healthy` | PASS |
| `hdim-demo-gateway-edge` | `healthy` | PASS |
| `hdim-demo-seeding` | `healthy` | PASS |
| `hdim-demo-ops` | `running` | PASS |

## docker compose ps Snapshot

```
NAME                               IMAGE                                  COMMAND                  SERVICE                    CREATED        STATUS                             PORTS
hdim-demo-audit-query              hdim-master-audit-query-service        "sh -c 'java $JAVA_O…"   audit-query-service        15 hours ago   Up 15 hours (healthy)              0.0.0.0:8088->8088/tcp, [::]:8088->8088/tcp
hdim-demo-care-gap                 hdim-master-care-gap-service           "sh -c 'java $JAVA_O…"   care-gap-service           15 hours ago   Up 15 hours (healthy)              0.0.0.0:8086->8086/tcp, [::]:8086->8086/tcp
hdim-demo-clinical-portal          hdim-master-clinical-portal            "/docker-entrypoint.…"   clinical-portal            8 days ago     Up 14 hours (healthy)              0.0.0.0:4200->80/tcp, [::]:4200->80/tcp
hdim-demo-cql-engine               hdim-master-cql-engine-service         "sh -c 'java $JAVA_O…"   cql-engine-service         15 hours ago   Up 15 hours (healthy)              0.0.0.0:8081->8081/tcp, [::]:8081->8081/tcp
hdim-demo-events                   hdim-master-event-processing-service   "sh -c 'java $JAVA_O…"   event-processing-service   15 hours ago   Up 15 hours (healthy)              0.0.0.0:8083->8083/tcp, [::]:8083->8083/tcp
hdim-demo-fhir                     hdim-master-fhir-service               "sh -c 'java $JAVA_O…"   fhir-service               15 hours ago   Up About a minute (healthy)        0.0.0.0:8085->8085/tcp, [::]:8085->8085/tcp
hdim-demo-gateway-admin            hdim-master-gateway-admin-service      "sh -c 'java $JAVA_O…"   gateway-admin-service      15 hours ago   Up 15 hours (healthy)              8080/tcp
hdim-demo-gateway-clinical         hdim-master-gateway-clinical-service   "sh -c 'java $JAVA_O…"   gateway-clinical-service   15 hours ago   Up 15 hours (healthy)              8080/tcp
hdim-demo-gateway-edge             nginx:1.27-alpine                      "/docker-entrypoint.…"   gateway-edge               15 hours ago   Up 15 hours (healthy)              80/tcp, 0.0.0.0:18080->8080/tcp, [::]:18080->8080/tcp
hdim-demo-gateway-fhir             hdim-master-gateway-fhir-service       "sh -c 'java $JAVA_O…"   gateway-fhir-service       15 hours ago   Up 15 hours (healthy)              8080/tcp
hdim-demo-hcc                      hdim-master-hcc-service                "sh -c 'java $JAVA_O…"   hcc-service                15 hours ago   Up 15 hours (healthy)              0.0.0.0:8105->8105/tcp, [::]:8105->8105/tcp
hdim-demo-jaeger                   jaegertracing/all-in-one:1.53          "/go/bin/all-in-one-…"   jaeger                     15 hours ago   Up 4 hours                         4317/tcp, 5775/udp, 5778/tcp, 0.0.0.0:4318->4318/tcp, [::]:4318->4318/tcp, 0.0.0.0:14250->14250/tcp, [::]:14250->14250/tcp, 0.0.0.0:14268->14268/tcp, [::]:14268->14268/tcp, 9411/tcp, 0.0.0.0:16686->16686/tcp, [::]:16686->16686/tcp, 6831-6832/udp
hdim-demo-kafka                    confluentinc/cp-kafka:7.5.0            "/etc/confluent/dock…"   kafka                      15 hours ago   Up 15 hours (healthy)              0.0.0.0:9094->9092/tcp, [::]:9094->9092/tcp
hdim-demo-ops                      hdim-master-ops-service                "docker-entrypoint.s…"   ops-service                3 hours ago    Up 3 hours                         0.0.0.0:4710->4710/tcp, [::]:4710->4710/tcp
hdim-demo-patient                  hdim-master-patient-service            "sh -c 'java $JAVA_O…"   patient-service            15 hours ago   Up 15 hours (healthy)              0.0.0.0:8084->8084/tcp, [::]:8084->8084/tcp
hdim-demo-postgres                 postgres:16-alpine                     "docker-entrypoint.s…"   postgres                   15 hours ago   Up 15 hours (healthy)              0.0.0.0:5435->5432/tcp, [::]:5435->5432/tcp
hdim-demo-quality-measure          hdim-master-quality-measure-service    "sh -c 'java $JAVA_O…"   quality-measure-service    15 hours ago   Up 15 hours (healthy)              0.0.0.0:8087->8087/tcp, [::]:8087->8087/tcp
hdim-demo-redis                    redis:7-alpine                         "docker-entrypoint.s…"   redis                      15 hours ago   Up 15 hours (healthy)              0.0.0.0:6380->6379/tcp, [::]:6380->6379/tcp
hdim-demo-seeding                  hdim-master-demo-seeding-service       "sh -c 'java $JAVA_O…"   demo-seeding-service       8 days ago     Up 15 hours (healthy)              0.0.0.0:8098->8098/tcp, [::]:8098->8098/tcp
healthdata-consent-service         hdim-master-consent-service            "sh -c 'java $JAVA_O…"   consent-service            4 days ago     Up 28 seconds (health: starting)   0.0.0.0:8082->8082/tcp, [::]:8082->8082/tcp
healthdata-ecr-service             hdim-master-ecr-service                "sh -c 'java $JAVA_O…"   ecr-service                4 days ago     Restarting (1) 1 second ago        
healthdata-event-router-service    hdim-master-event-router-service       "sh -c 'java $JAVA_O…"   event-router-service       4 days ago     Restarting (1) 1 second ago        
healthdata-event-store-service     hdim-master-event-store-service        "sh -c 'java $JAVA_O…"   event-store-service        4 days ago     Up 29 seconds (health: starting)   0.0.0.0:8090->8090/tcp, [::]:8090->8090/tcp
healthdata-patient-event-service   hdim-master-patient-event-service      "sh -c 'java $JAVA_O…"   patient-event-service      4 days ago     Up 23 seconds (health: starting)   0.0.0.0:8110->8110/tcp, [::]:8110->8110/tcp
healthdata-prior-auth-service      hdim-master-prior-auth-service         "sh -c 'java $JAVA_O…"   prior-auth-service         4 days ago     Up 27 seconds (health: starting)   0.0.0.0:8102->8102/tcp, [::]:8102->8102/tcp
healthdata-qrda-export-service     hdim-master-qrda-export-service        "sh -c 'java $JAVA_O…"   qrda-export-service        4 days ago     Restarting (1) 1 second ago        
healthdata-zookeeper               confluentinc/cp-zookeeper:7.5.0        "/etc/confluent/dock…"   zookeeper                  4 days ago     Up 2 days (healthy)                2888/tcp, 3888/tcp, 0.0.0.0:2182->2181/tcp, [::]:2182->2181/tcp
```

## ✅ Result: PASS

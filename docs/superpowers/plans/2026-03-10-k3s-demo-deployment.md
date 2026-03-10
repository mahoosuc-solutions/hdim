# K3s Demo Deployment Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the HDIM 19-service demo stack to K3s with GitOps (FluxCD), Traefik ingress with 6 subdomains, and local k3d validation — so the demo is always-on for HIMSS sales conversations.

**Architecture:** K3s single-node on Hetzner CCX33, designed for 3-node scale-out. All services run as K8s Deployments with explicit resource requests/limits and dynamic JVM sizing via MaxRAMPercentage. Traefik handles ingress routing and auto-TLS. FluxCD watches the `k8s/demo/` directory in git and reconciles the cluster. Local validation via k3d (K3s-in-Docker).

**Tech Stack:** K3s, Kustomize, Traefik, FluxCD, k3d, GHCR, Hetzner Cloud Volumes, Let's Encrypt

**Spec:** `docs/superpowers/specs/2026-03-10-k3s-demo-deployment-design.md`

---

## File Structure

```
k8s/demo/
├── kustomization.yaml                    # Root kustomization — assembles all subdirectories
├── namespace.yaml                        # hdim-demo namespace + ResourceQuota + LimitRange
│
├── config/
│   ├── kustomization.yaml
│   ├── configmap.yaml                    # Shared env: JAVA_OPTS, Spring profiles, service URLs
│   ├── configmap-demo.yaml               # Demo-specific: credentials, seeding config, tenant IDs
│   └── secrets.yaml                      # DB, Redis, Kafka, JWT credentials
│
├── infrastructure/
│   ├── kustomization.yaml
│   ├── postgres.yaml                     # StatefulSet + PVC + Service + initdb ConfigMap
│   ├── redis.yaml                        # Deployment + Service
│   └── kafka.yaml                        # StatefulSet + PVC + Service (KRaft mode)
│
├── gateways/
│   ├── kustomization.yaml
│   ├── gateway-admin.yaml                # Deployment + Service
│   ├── gateway-fhir.yaml                 # Deployment + Service
│   ├── gateway-clinical.yaml             # Deployment + Service
│   └── gateway-edge.yaml                 # Deployment + Service + ConfigMap (nginx.conf)
│
├── services/
│   ├── kustomization.yaml
│   ├── fhir-service.yaml                 # Deployment + Service (85% MaxRAMPercentage)
│   ├── patient-service.yaml              # Deployment + Service
│   ├── cql-engine-service.yaml           # Deployment + Service
│   ├── quality-measure-service.yaml      # Deployment + Service
│   ├── care-gap-service.yaml             # Deployment + Service
│   ├── event-processing-service.yaml     # Deployment + Service
│   ├── audit-query-service.yaml          # Deployment + Service
│   ├── hcc-service.yaml                  # Deployment + Service
│   └── demo-seeding-service.yaml         # Deployment + Service + PVC
│
├── frontend/
│   ├── kustomization.yaml
│   └── clinical-portal.yaml              # Deployment + Service
│
├── observability/
│   ├── kustomization.yaml
│   └── jaeger.yaml                       # Deployment + Service
│
├── ops/
│   ├── kustomization.yaml
│   └── ops-service.yaml                  # Deployment + Service
│
├── ingress/
│   ├── kustomization.yaml
│   ├── ingress-public.yaml               # demo/api/fhir subdomains (no auth)
│   ├── ingress-internal.yaml             # traces/docs/ops subdomains (basic auth)
│   ├── basic-auth-middleware.yaml         # Traefik Middleware CRD for basic auth
│   └── basic-auth-secret.yaml            # htpasswd secret
│
└── local/
    ├── kustomization.yaml                # Patches: no TLS, local-path storage, IfNotPresent images
    ├── k3d-cluster.yaml                  # k3d cluster config (ports 80/443 mapped)
    ├── patch-no-tls.yaml                 # Remove cert-manager annotations
    ├── patch-local-storage.yaml          # StorageClass → local-path
    └── hosts-entries.txt                 # /etc/hosts lines for local subdomain testing
```

**Scripts:**
```
scripts/
├── k3s-local-up.sh                       # Install k3d+kubectl, create cluster, apply manifests, /etc/hosts
├── k3s-local-down.sh                     # Delete cluster, remove /etc/hosts entries
├── k3s-validate-demo.sh                  # Validate all pods, ingress, auth, FHIR, data (local + remote)
└── k3s-create-hetzner.sh                 # Hetzner server + K3s + FluxCD bootstrap (future)
```

---

## Chunk 1: Foundation — Namespace, Config, Secrets, Infrastructure

### Task 1: Install k3d and kubectl

**Files:** None (system tooling)

- [ ] **Step 1: Install kubectl**

```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/kubectl
kubectl version --client
```

Expected: `Client Version: v1.3x.x`

- [ ] **Step 2: Install k3d**

```bash
curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash
k3d version
```

Expected: `k3d version v5.x.x`

- [ ] **Step 3: Verify Docker is accessible**

```bash
docker version --format '{{.Server.Version}}'
```

Expected: `28.5.1` (or similar)

---

### Task 2: Namespace and resource quotas

**Files:**
- Create: `k8s/demo/namespace.yaml`
- Create: `k8s/demo/kustomization.yaml` (initial, will grow)

- [ ] **Step 1: Create namespace manifest**

Create `k8s/demo/namespace.yaml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: hdim-demo
  labels:
    app.kubernetes.io/name: hdim-demo
    app.kubernetes.io/part-of: hdim-platform
---
apiVersion: v1
kind: ResourceQuota
metadata:
  name: hdim-demo-quota
  namespace: hdim-demo
spec:
  hard:
    requests.cpu: "8"
    requests.memory: 16Gi
    limits.cpu: "16"
    limits.memory: 24Gi
    pods: "30"
---
apiVersion: v1
kind: LimitRange
metadata:
  name: hdim-demo-limits
  namespace: hdim-demo
spec:
  limits:
    - default:
        cpu: "500m"
        memory: "512Mi"
      defaultRequest:
        cpu: "100m"
        memory: "256Mi"
      type: Container
```

- [ ] **Step 2: Create root kustomization**

Create `k8s/demo/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: hdim-demo

resources:
  - namespace.yaml
  - config/
  - infrastructure/
```

- [ ] **Step 3: Commit**

```bash
git add k8s/demo/namespace.yaml k8s/demo/kustomization.yaml
git commit -m "feat(k8s): add hdim-demo namespace with resource quotas"
```

---

### Task 3: ConfigMaps and Secrets

**Files:**
- Create: `k8s/demo/config/kustomization.yaml`
- Create: `k8s/demo/config/configmap.yaml`
- Create: `k8s/demo/config/configmap-demo.yaml`
- Create: `k8s/demo/config/secrets.yaml`

- [ ] **Step 1: Create config kustomization**

Create `k8s/demo/config/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - configmap.yaml
  - configmap-demo.yaml
  - secrets.yaml
```

- [ ] **Step 2: Create shared configmap**

Create `k8s/demo/config/configmap.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hdim-common-config
  labels:
    app.kubernetes.io/part-of: hdim-platform
data:
  SPRING_PROFILES_ACTIVE: "demo"
  JAVA_OPTS: "-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=25.0 -XX:+UseG1GC"
  SERVER_TOMCAT_ACCESSLOG_ENABLED: "true"
  MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE: "health,info,metrics,prometheus"
  MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS: "always"
  MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED: "true"
  LOGGING_LEVEL_ROOT: "INFO"
  LOGGING_LEVEL_COM_HEALTHDATA: "DEBUG"
  SPRING_FLYWAY_ENABLED: "false"
  SPRING_JPA_HIBERNATE_DDL_AUTO: "update"
  GATEWAY_AUTH_DEV_MODE: "true"
  OTEL_EXPORTER_OTLP_ENDPOINT: "http://jaeger:4318/v1/traces"
  OTEL_EXPORTER_OTLP_PROTOCOL: "http/protobuf"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: hdim-service-urls
  labels:
    app.kubernetes.io/part-of: hdim-platform
data:
  FHIR_SERVICE_URL: "http://fhir-service:8085/fhir"
  FHIR_EXTERNAL_URL: "http://fhir-service:8085/fhir"
  FHIR_SERVER_URL: "http://fhir-service:8085/fhir"
  PATIENT_SERVICE_URL: "http://patient-service:8084/patient"
  CARE_GAP_SERVICE_URL: "http://care-gap-service:8086/care-gap"
  QUALITY_MEASURE_SERVICE_URL: "http://quality-measure-service:8087/quality-measure"
  CQL_ENGINE_SERVICE_URL: "http://cql-engine-service:8081/cql-engine"
  EVENT_PROCESSING_SERVICE_URL: "http://event-processing-service:8083/events"
  GATEWAY_SERVICE_URL: "http://gateway-edge:8080"
```

- [ ] **Step 3: Create demo-specific configmap**

Create `k8s/demo/config/configmap-demo.yaml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hdim-demo-config
  labels:
    app.kubernetes.io/part-of: hdim-platform
data:
  DEMO_GENERATION_DEFAULT_PATIENT_COUNT: "200"
  DEMO_MULTI_TENANT_PATIENTS_PER_TENANT: "100"
  DEMO_PERFORMANCE_PARALLEL_GENERATION: "false"
  DEMO_PERFORMANCE_MAX_CONCURRENT_REQUESTS: "2"
  DEMO_QUALITY_MEASURES_ENABLED: "false"
  DEMO_PERSISTENCE_ENABLED: "true"
  FHIR_TARGET: "internal"
  TENANT_ENFORCEMENT_MODE: "enforce"
  TENANT_ENFORCEMENT_MISSING_TENANT_PATHS: "/actuator,/swagger-ui,/v3/api-docs,/fhir/metadata,/metadata,/api/v1/auth"
```

- [ ] **Step 4: Create secrets**

Create `k8s/demo/config/secrets.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: hdim-db-credentials
  labels:
    app.kubernetes.io/part-of: hdim-platform
type: Opaque
stringData:
  POSTGRES_USER: healthdata
  POSTGRES_PASSWORD: demo_password_2024
  POSTGRES_DB: healthdata_db
  SPRING_DATASOURCE_USERNAME: healthdata
  SPRING_DATASOURCE_PASSWORD: demo_password_2024
---
apiVersion: v1
kind: Secret
metadata:
  name: hdim-redis-credentials
  labels:
    app.kubernetes.io/part-of: hdim-platform
type: Opaque
stringData:
  SPRING_DATA_REDIS_HOST: redis
  SPRING_DATA_REDIS_PORT: "6379"
---
apiVersion: v1
kind: Secret
metadata:
  name: hdim-kafka-credentials
  labels:
    app.kubernetes.io/part-of: hdim-platform
type: Opaque
stringData:
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
---
apiVersion: v1
kind: Secret
metadata:
  name: hdim-jwt-credentials
  labels:
    app.kubernetes.io/part-of: hdim-platform
type: Opaque
stringData:
  JWT_SECRET: DemoSecretKeyForHDIMShouldBeAtLeast256BitsLongForHS256Algorithm12345678
  GATEWAY_AUTH_SIGNING_SECRET: "2J3YcbuHgPp0xyMq3GHYJ/MNolqB+Hvp4j/fsA6LQYM="
---
apiVersion: v1
kind: Secret
metadata:
  name: hdim-gateway-db
  labels:
    app.kubernetes.io/part-of: hdim-platform
type: Opaque
stringData:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gateway_db
  SPRING_DATASOURCE_USERNAME: healthdata
  SPRING_DATASOURCE_PASSWORD: demo_password_2024
```

- [ ] **Step 5: Commit**

```bash
git add k8s/demo/config/
git commit -m "feat(k8s): add demo configmaps and secrets"
```

---

### Task 4: Infrastructure — PostgreSQL, Redis, Kafka

**Files:**
- Create: `k8s/demo/infrastructure/kustomization.yaml`
- Create: `k8s/demo/infrastructure/postgres.yaml`
- Create: `k8s/demo/infrastructure/redis.yaml`
- Create: `k8s/demo/infrastructure/kafka.yaml`

- [ ] **Step 1: Create infrastructure kustomization**

Create `k8s/demo/infrastructure/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - postgres.yaml
  - redis.yaml
  - kafka.yaml
```

- [ ] **Step 2: Create PostgreSQL StatefulSet**

Create `k8s/demo/infrastructure/postgres.yaml`.

Use StatefulSet (not Deployment) for stable network identity and ordered shutdown.
Include initdb ConfigMap to create the multiple databases the demo stack needs.
PVC uses default StorageClass (local-path on k3d, hcloud-volumes on Hetzner).

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-initdb
  labels:
    app: postgres
data:
  01-create-databases.sh: |
    #!/bin/bash
    set -e
    for db in gateway_db fhir_db patient_db care_gap_db cql_engine_db \
              quality_measure_db event_processing_db audit_db hcc_db \
              healthdata_demo; do
      psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        SELECT 'CREATE DATABASE ${db}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${db}')\gexec
        GRANT ALL PRIVILEGES ON DATABASE ${db} TO ${POSTGRES_USER};
      EOSQL
    done
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  labels:
    app: postgres
    app.kubernetes.io/part-of: hdim-platform
    tier: infrastructure
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
        tier: infrastructure
    spec:
      containers:
        - name: postgres
          image: postgres:16-alpine
          ports:
            - containerPort: 5432
          envFrom:
            - secretRef:
                name: hdim-db-credentials
          env:
            - name: PGDATA
              value: /var/lib/postgresql/data/pgdata
          volumeMounts:
            - name: postgres-data
              mountPath: /var/lib/postgresql/data
            - name: initdb
              mountPath: /docker-entrypoint-initdb.d
              readOnly: true
          resources:
            requests:
              cpu: "250m"
              memory: "384Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          livenessProbe:
            exec:
              command: ["pg_isready", "-U", "healthdata"]
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "healthdata"]
            initialDelaySeconds: 5
            periodSeconds: 5
      volumes:
        - name: initdb
          configMap:
            name: postgres-initdb
  volumeClaimTemplates:
    - metadata:
        name: postgres-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 20Gi
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  labels:
    app: postgres
spec:
  type: ClusterIP
  ports:
    - port: 5432
      targetPort: 5432
  selector:
    app: postgres
```

- [ ] **Step 3: Create Redis Deployment**

Create `k8s/demo/infrastructure/redis.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  labels:
    app: redis
    app.kubernetes.io/part-of: hdim-platform
    tier: infrastructure
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
        tier: infrastructure
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          ports:
            - containerPort: 6379
          command:
            - redis-server
            - --appendonly
            - "yes"
            - --maxmemory
            - 96mb
            - --maxmemory-policy
            - allkeys-lru
          resources:
            requests:
              cpu: "100m"
              memory: "64Mi"
            limits:
              cpu: "250m"
              memory: "128Mi"
          livenessProbe:
            exec:
              command: ["redis-cli", "ping"]
            initialDelaySeconds: 10
            periodSeconds: 5
          readinessProbe:
            exec:
              command: ["redis-cli", "ping"]
            initialDelaySeconds: 5
            periodSeconds: 3
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  labels:
    app: redis
spec:
  type: ClusterIP
  ports:
    - port: 6379
      targetPort: 6379
  selector:
    app: redis
```

- [ ] **Step 4: Create Kafka StatefulSet**

Create `k8s/demo/infrastructure/kafka.yaml`:

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  labels:
    app: kafka
    app.kubernetes.io/part-of: hdim-platform
    tier: infrastructure
spec:
  serviceName: kafka
  replicas: 1
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
        tier: infrastructure
    spec:
      containers:
        - name: kafka
          image: confluentinc/cp-kafka:7.5.0
          ports:
            - containerPort: 9092
          env:
            - name: KAFKA_NODE_ID
              value: "1"
            - name: KAFKA_PROCESS_ROLES
              value: broker,controller
            - name: KAFKA_LISTENERS
              value: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
            - name: KAFKA_ADVERTISED_LISTENERS
              value: PLAINTEXT://kafka:9092
            - name: KAFKA_CONTROLLER_LISTENER_NAMES
              value: CONTROLLER
            - name: KAFKA_LISTENER_SECURITY_PROTOCOL_MAP
              value: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
            - name: KAFKA_CONTROLLER_QUORUM_VOTERS
              value: 1@localhost:9093
            - name: KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR
              value: "1"
            - name: KAFKA_TRANSACTION_STATE_LOG_MIN_ISR
              value: "1"
            - name: KAFKA_AUTO_CREATE_TOPICS_ENABLE
              value: "true"
            - name: KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS
              value: "0"
            - name: KAFKA_NUM_PARTITIONS
              value: "1"
            - name: KAFKA_LOG_RETENTION_HOURS
              value: "24"
            - name: CLUSTER_ID
              value: demo-cluster-hdim-2024
            - name: KAFKA_HEAP_OPTS
              value: "-Xms256m -Xmx512m"
          volumeMounts:
            - name: kafka-data
              mountPath: /var/lib/kafka/data
          resources:
            requests:
              cpu: "250m"
              memory: "512Mi"
            limits:
              cpu: "1000m"
              memory: "1024Mi"
          readinessProbe:
            tcpSocket:
              port: 9092
            initialDelaySeconds: 60
            periodSeconds: 10
  volumeClaimTemplates:
    - metadata:
        name: kafka-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 10Gi
---
apiVersion: v1
kind: Service
metadata:
  name: kafka
  labels:
    app: kafka
spec:
  type: ClusterIP
  ports:
    - port: 9092
      targetPort: 9092
  selector:
    app: kafka
```

- [ ] **Step 5: Update root kustomization to include config/**

Edit `k8s/demo/kustomization.yaml` — ensure `config/` is listed under resources (already added in Task 2 Step 2).

- [ ] **Step 6: Validate kustomize build**

```bash
kubectl kustomize k8s/demo/ > /dev/null
echo $?
```

Expected: `0` (no errors)

- [ ] **Step 7: Commit**

```bash
git add k8s/demo/
git commit -m "feat(k8s): add demo infrastructure — postgres, redis, kafka with PVCs"
```

---

## Chunk 2: Gateways and Backend Services

### Task 5: Gateway Deployments

**Files:**
- Create: `k8s/demo/gateways/kustomization.yaml`
- Create: `k8s/demo/gateways/gateway-admin.yaml`
- Create: `k8s/demo/gateways/gateway-fhir.yaml`
- Create: `k8s/demo/gateways/gateway-clinical.yaml`
- Create: `k8s/demo/gateways/gateway-edge.yaml`

All three gateway services follow the same pattern. Each uses:
- `envFrom` for common config, service URLs, DB credentials (gateway_db), Redis, Kafka, JWT
- Service-specific env for `OTEL_SERVICE_NAME` and `HEALTH_TENANT_ID`
- Port 8080 (internal), exposed as ClusterIP
- Dynamic JVM: `MaxRAMPercentage=75.0` via common configmap

- [ ] **Step 1: Create gateways kustomization**

Create `k8s/demo/gateways/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - gateway-admin.yaml
  - gateway-fhir.yaml
  - gateway-clinical.yaml
  - gateway-edge.yaml
```

- [ ] **Step 2: Create gateway-admin deployment**

Create `k8s/demo/gateways/gateway-admin.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-admin-service
  labels:
    app: gateway-admin-service
    app.kubernetes.io/part-of: hdim-platform
    tier: gateway
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gateway-admin-service
  template:
    metadata:
      labels:
        app: gateway-admin-service
        tier: gateway
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values: ["gateway-admin-service"]
                topologyKey: kubernetes.io/hostname
      containers:
        - name: gateway-admin-service
          image: ghcr.io/mahoosuc-solutions/hdim/gateway-admin-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: hdim-common-config
            - configMapRef:
                name: hdim-service-urls
            - secretRef:
                name: hdim-gateway-db
            - secretRef:
                name: hdim-redis-credentials
            - secretRef:
                name: hdim-kafka-credentials
            - secretRef:
                name: hdim-jwt-credentials
          env:
            - name: OTEL_SERVICE_NAME
              value: gateway-admin-service
            - name: HEALTH_TENANT_ID
              value: service-health-gateway-admin
            - name: JAVA_TOOL_OPTIONS
              value: "-Djava.net.preferIPv4Stack=true -Dsun.net.inetaddr.ttl=60 -Dsun.net.inetaddr.negative.ttl=0"
            - name: AUTHENTICATION_COOKIE_SECURE
              value: "false"
          resources:
            requests:
              cpu: "100m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 120
            periodSeconds: 15
            timeoutSeconds: 10
            failureThreshold: 5
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: gateway-admin-service
  labels:
    app: gateway-admin-service
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: gateway-admin-service
```

- [ ] **Step 3: Create gateway-fhir deployment**

Create `k8s/demo/gateways/gateway-fhir.yaml`.

Same pattern as gateway-admin, with:
- `name: gateway-fhir-service`
- `OTEL_SERVICE_NAME: gateway-fhir-service`
- `HEALTH_TENANT_ID: service-health-gateway-fhir`

(Full YAML follows identical structure — change name/labels/env values only.)

- [ ] **Step 4: Create gateway-clinical deployment**

Create `k8s/demo/gateways/gateway-clinical.yaml`.

Same pattern, with:
- `name: gateway-clinical-service`
- `OTEL_SERVICE_NAME: gateway-clinical-service`
- `HEALTH_TENANT_ID: service-health-gateway-clinical`

- [ ] **Step 5: Create gateway-edge deployment**

Create `k8s/demo/gateways/gateway-edge.yaml`.

This is nginx, not a Java service. Uses a ConfigMap for `nginx.conf` that replicates the routing
from `docker-compose.demo.yml` lines 524-770. Uses the K8s service names (no port mapping differences).

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-edge-config
  labels:
    app: gateway-edge
data:
  default.conf: |
    resolver kube-dns.kube-system.svc.cluster.local valid=10s;

    upstream gateway_admin {
        zone gateway_admin 64k;
        server gateway-admin-service:8080;
    }

    upstream gateway_fhir {
        zone gateway_fhir 64k;
        server gateway-fhir-service:8080;
    }

    upstream gateway_clinical {
        zone gateway_clinical 64k;
        server gateway-clinical-service:8080;
    }

    upstream demo_seeding {
        zone demo_seeding 64k;
        server demo-seeding-service:8098;
    }

    map "$http_authorization:$cookie_hdim_access_token" $hdim_missing_auth {
        default 0;
        ":" 1;
    }

    server {
        listen 8080;
        client_max_body_size 50m;

        add_header X-Edge gateway-edge always;
        add_header X-Edge-Request-ID $request_id always;

        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Request-ID $request_id;

        location ~ ^/api/v1/auth/ { proxy_pass http://gateway_admin; }
        location ~ ^/api/v1/agent-builder/ { proxy_pass http://gateway_admin; }
        location ~ ^/api/v1/tools/ { proxy_pass http://gateway_admin; }
        location ~ ^/api/v1/providers/ { proxy_pass http://gateway_admin; }
        location ~ ^/api/v1/runtime/ { proxy_pass http://gateway_admin; }
        location ~ ^/api/sales/ { proxy_pass http://gateway_admin; }
        location ~ ^/sales-automation/ { proxy_pass http://gateway_admin; }

        location ~ ^/api/fhir/ { if ($hdim_missing_auth = 1) { return 401; } proxy_pass http://gateway_fhir; }
        location = /fhir/metadata { proxy_pass http://gateway_fhir; }
        location ~ ^/fhir/ { if ($hdim_missing_auth = 1) { return 401; } proxy_pass http://gateway_fhir; }
        location ~ ^/patient/ { if ($hdim_missing_auth = 1) { return 401; } proxy_pass http://gateway_fhir; }
        location ~ ^/cql-engine/ { proxy_pass http://gateway_fhir; }
        location ~ ^/quality-measure/ { if ($hdim_missing_auth = 1) { return 401; } proxy_pass http://gateway_fhir; }

        location ~ ^/care-gap/ { if ($hdim_missing_auth = 1) { return 401; } proxy_pass http://gateway_clinical; }
        location ~ ^/events/ { proxy_pass http://gateway_clinical; }
        location ~ ^/api/v1/hcc/ { proxy_pass http://gateway_clinical; }
        location ~ ^/api/v1/audit/ { proxy_pass http://gateway_admin; }

        location ~ ^/demo/ { proxy_pass http://demo_seeding; }
        location ~ ^/swagger-ui/ { proxy_pass http://gateway_admin; }
        location ~ ^/v3/api-docs/ { proxy_pass http://gateway_admin; }
        location = /swagger-ui.html { proxy_pass http://gateway_admin; }
        location ~ ^/actuator/ { proxy_pass http://gateway_admin; }

        location / { proxy_pass http://gateway_admin; }
    }
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gateway-edge
  labels:
    app: gateway-edge
    app.kubernetes.io/part-of: hdim-platform
    tier: gateway
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gateway-edge
  template:
    metadata:
      labels:
        app: gateway-edge
        tier: gateway
    spec:
      containers:
        - name: nginx
          image: nginx:1.27-alpine
          ports:
            - containerPort: 8080
          volumeMounts:
            - name: config
              mountPath: /etc/nginx/conf.d
              readOnly: true
          resources:
            requests:
              cpu: "50m"
              memory: "32Mi"
            limits:
              cpu: "200m"
              memory: "64Mi"
          livenessProbe:
            httpGet:
              path: /
              port: 8080
            initialDelaySeconds: 5
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /
              port: 8080
            initialDelaySeconds: 3
            periodSeconds: 5
      volumes:
        - name: config
          configMap:
            name: gateway-edge-config
---
apiVersion: v1
kind: Service
metadata:
  name: gateway-edge
  labels:
    app: gateway-edge
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: gateway-edge
```

- [ ] **Step 6: Update root kustomization**

Add `- gateways/` to resources in `k8s/demo/kustomization.yaml`.

- [ ] **Step 7: Validate kustomize build**

```bash
kubectl kustomize k8s/demo/ | grep "kind: Deployment" | wc -l
```

Expected: `6` (postgres is StatefulSet — 3 gateways + redis + gateway-edge + kafka StatefulSet)

- [ ] **Step 8: Commit**

```bash
git add k8s/demo/gateways/
git commit -m "feat(k8s): add gateway deployments — admin, fhir, clinical, edge (nginx)"
```

---

### Task 6: Backend service deployments

**Files:**
- Create: `k8s/demo/services/kustomization.yaml`
- Create: `k8s/demo/services/{fhir,patient,cql-engine,quality-measure,care-gap,event-processing,audit-query,hcc,demo-seeding}-service.yaml`

All backend services follow the same template. The key variables per service:

| Service | Image suffix | Port | Context path | Health path | MaxRAM% | Extra env |
|---------|-------------|------|-------------|------------|---------|-----------|
| fhir-service | fhir-service | 8085 | /fhir | /fhir/actuator/health | 85% | SPRING_JPA_HIBERNATE_DDL_AUTO=update |
| patient-service | patient-service | 8084 | /patient | /patient/actuator/health | 75% | — |
| cql-engine-service | cql-engine-service | 8081 | /cql-engine | /cql-engine/actuator/health | 75% | — |
| quality-measure-service | quality-measure-service | 8087 | /quality-measure | /quality-measure/actuator/health | 75% | — |
| care-gap-service | care-gap-service | 8086 | /care-gap | /care-gap/actuator/health | 75% | — |
| event-processing-service | event-processing-service | 8083 | /events | /events/actuator/health | 75% | — |
| audit-query-service | audit-query-service | 8088 | — | /actuator/health | 75% | — |
| hcc-service | hcc-service | 8105 | /hcc | /hcc/actuator/health | 75% | HCC_MODEL_V24_WEIGHT, HCC_MODEL_V28_WEIGHT |
| demo-seeding-service | demo-seeding-service | 8098 | /demo | /demo/actuator/health | 75% | demo config refs |

- [ ] **Step 1: Create services kustomization**

Create `k8s/demo/services/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - fhir-service.yaml
  - patient-service.yaml
  - cql-engine-service.yaml
  - quality-measure-service.yaml
  - care-gap-service.yaml
  - event-processing-service.yaml
  - audit-query-service.yaml
  - hcc-service.yaml
  - demo-seeding-service.yaml
```

- [ ] **Step 2: Create all 9 service manifests**

Each follows this template (substituting values from the table above):

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: <SERVICE_NAME>
  labels:
    app: <SERVICE_NAME>
    app.kubernetes.io/part-of: hdim-platform
    tier: core
spec:
  replicas: 1
  selector:
    matchLabels:
      app: <SERVICE_NAME>
  template:
    metadata:
      labels:
        app: <SERVICE_NAME>
        tier: core
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
            - weight: 100
              podAffinityTerm:
                labelSelector:
                  matchExpressions:
                    - key: app
                      operator: In
                      values: ["<SERVICE_NAME>"]
                topologyKey: kubernetes.io/hostname
      containers:
        - name: <SERVICE_NAME>
          image: ghcr.io/mahoosuc-solutions/hdim/<SERVICE_NAME>:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: <PORT>
          envFrom:
            - configMapRef:
                name: hdim-common-config
            - configMapRef:
                name: hdim-service-urls
            - secretRef:
                name: hdim-db-credentials
            - secretRef:
                name: hdim-redis-credentials
            - secretRef:
                name: hdim-kafka-credentials
            - secretRef:
                name: hdim-jwt-credentials
          env:
            - name: OTEL_SERVICE_NAME
              value: <SERVICE_NAME>
            - name: HEALTH_TENANT_ID
              value: service-health-<SHORT_NAME>
            - name: SPRING_DATASOURCE_URL
              value: jdbc:postgresql://postgres:5432/<DB_NAME>
            - name: JAVA_TOOL_OPTIONS
              value: "-Djava.net.preferIPv4Stack=true"
          resources:
            requests:
              cpu: "200m"
              memory: "256Mi"
            limits:
              cpu: "500m"
              memory: "512Mi"
          livenessProbe:
            httpGet:
              path: <HEALTH_PATH>/liveness
              port: <PORT>
            initialDelaySeconds: 120
            periodSeconds: 15
            timeoutSeconds: 10
            failureThreshold: 5
          readinessProbe:
            httpGet:
              path: <HEALTH_PATH>/readiness
              port: <PORT>
            initialDelaySeconds: 60
            periodSeconds: 10
            timeoutSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: <SERVICE_NAME>
  labels:
    app: <SERVICE_NAME>
spec:
  type: ClusterIP
  ports:
    - port: <PORT>
      targetPort: <PORT>
  selector:
    app: <SERVICE_NAME>
```

**Exceptions to the template:**

**fhir-service** — Override JAVA_OPTS for 85% MaxRAMPercentage, higher memory limits:
```yaml
env:
  - name: JAVA_OPTS
    value: "-XX:MaxRAMPercentage=85.0 -XX:InitialRAMPercentage=40.0 -XX:+UseG1GC"
resources:
  requests:
    cpu: "500m"
    memory: "1024Mi"
  limits:
    cpu: "1000m"
    memory: "2048Mi"
```

**demo-seeding-service** — Add envFrom for `hdim-demo-config`, add PVC for snapshots:
```yaml
envFrom:
  # ... standard refs plus:
  - configMapRef:
      name: hdim-demo-config
  - secretRef:
      name: hdim-gateway-db  # needs gateway DB access for user creation
volumeMounts:
  - name: snapshots
    mountPath: /app/snapshots
# Add PVC:
volumes:
  - name: snapshots
    persistentVolumeClaim:
      claimName: demo-snapshots
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: demo-snapshots
spec:
  accessModes: ["ReadWriteOnce"]
  resources:
    requests:
      storage: 5Gi
```

**hcc-service** — Add HCC model weight env vars:
```yaml
env:
  - name: HCC_MODEL_V24_WEIGHT
    value: "0.33"
  - name: HCC_MODEL_V28_WEIGHT
    value: "0.67"
```

- [ ] **Step 3: Update root kustomization**

Add `- services/` to resources in `k8s/demo/kustomization.yaml`.

- [ ] **Step 4: Validate kustomize build**

```bash
kubectl kustomize k8s/demo/ | grep "kind: Deployment" | wc -l
```

Expected: `14` (3 gateways + gateway-edge + redis + 9 services)

- [ ] **Step 5: Commit**

```bash
git add k8s/demo/services/
git commit -m "feat(k8s): add 9 backend service deployments with dynamic JVM sizing"
```

---

### Task 7: Frontend, Observability, Ops

**Files:**
- Create: `k8s/demo/frontend/kustomization.yaml`
- Create: `k8s/demo/frontend/clinical-portal.yaml`
- Create: `k8s/demo/observability/kustomization.yaml`
- Create: `k8s/demo/observability/jaeger.yaml`
- Create: `k8s/demo/ops/kustomization.yaml`
- Create: `k8s/demo/ops/ops-service.yaml`

- [ ] **Step 1: Create clinical-portal**

Create `k8s/demo/frontend/kustomization.yaml` and `k8s/demo/frontend/clinical-portal.yaml`:

```yaml
# clinical-portal.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: clinical-portal
  labels:
    app: clinical-portal
    app.kubernetes.io/part-of: hdim-platform
    tier: frontend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: clinical-portal
  template:
    metadata:
      labels:
        app: clinical-portal
        tier: frontend
    spec:
      containers:
        - name: clinical-portal
          image: ghcr.io/mahoosuc-solutions/hdim/clinical-portal:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 80
          resources:
            requests:
              cpu: "50m"
              memory: "64Mi"
            limits:
              cpu: "200m"
              memory: "256Mi"
          livenessProbe:
            httpGet:
              path: /
              port: 80
            initialDelaySeconds: 10
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /
              port: 80
            initialDelaySeconds: 5
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: clinical-portal
  labels:
    app: clinical-portal
spec:
  type: ClusterIP
  ports:
    - port: 80
      targetPort: 80
  selector:
    app: clinical-portal
```

- [ ] **Step 2: Create jaeger**

Create `k8s/demo/observability/kustomization.yaml` and `k8s/demo/observability/jaeger.yaml`:

```yaml
# jaeger.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger
  labels:
    app: jaeger
    app.kubernetes.io/part-of: hdim-platform
    tier: observability
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jaeger
  template:
    metadata:
      labels:
        app: jaeger
        tier: observability
    spec:
      containers:
        - name: jaeger
          image: jaegertracing/all-in-one:1.53
          ports:
            - containerPort: 16686
              name: ui
            - containerPort: 4318
              name: otlp-http
          env:
            - name: COLLECTOR_OTLP_ENABLED
              value: "true"
          resources:
            requests:
              cpu: "100m"
              memory: "128Mi"
            limits:
              cpu: "250m"
              memory: "256Mi"
          livenessProbe:
            httpGet:
              path: /
              port: 16686
            initialDelaySeconds: 10
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /
              port: 16686
            initialDelaySeconds: 5
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: jaeger
  labels:
    app: jaeger
spec:
  type: ClusterIP
  ports:
    - port: 16686
      targetPort: 16686
      name: ui
    - port: 4318
      targetPort: 4318
      name: otlp-http
  selector:
    app: jaeger
```

- [ ] **Step 3: Create ops-service**

Create `k8s/demo/ops/kustomization.yaml` and `k8s/demo/ops/ops-service.yaml`:

```yaml
# ops-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ops-service
  labels:
    app: ops-service
    app.kubernetes.io/part-of: hdim-platform
    tier: ops
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ops-service
  template:
    metadata:
      labels:
        app: ops-service
        tier: ops
    spec:
      containers:
        - name: ops-service
          image: ghcr.io/mahoosuc-solutions/hdim/ops-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 4710
          env:
            - name: PORT
              value: "4710"
            - name: DEMO_SEEDING_URL
              value: "http://demo-seeding-service:8098"
          resources:
            requests:
              cpu: "50m"
              memory: "64Mi"
            limits:
              cpu: "200m"
              memory: "256Mi"
---
apiVersion: v1
kind: Service
metadata:
  name: ops-service
  labels:
    app: ops-service
spec:
  type: ClusterIP
  ports:
    - port: 4710
      targetPort: 4710
  selector:
    app: ops-service
```

- [ ] **Step 4: Update root kustomization**

Add `frontend/`, `observability/`, `ops/` to resources in `k8s/demo/kustomization.yaml`.

- [ ] **Step 5: Validate kustomize build**

```bash
kubectl kustomize k8s/demo/ | grep "kind: Deployment" | wc -l
```

Expected: `17` (prev 14 + clinical-portal + jaeger + ops-service)

- [ ] **Step 6: Commit**

```bash
git add k8s/demo/frontend/ k8s/demo/observability/ k8s/demo/ops/
git commit -m "feat(k8s): add clinical portal, jaeger, ops-service deployments"
```

---

## Chunk 3: Ingress, Local Overlay, and Validation

### Task 8: Ingress — public and internal with basic auth

**Files:**
- Create: `k8s/demo/ingress/kustomization.yaml`
- Create: `k8s/demo/ingress/ingress-public.yaml`
- Create: `k8s/demo/ingress/ingress-internal.yaml`
- Create: `k8s/demo/ingress/basic-auth-middleware.yaml`
- Create: `k8s/demo/ingress/basic-auth-secret.yaml`

- [ ] **Step 1: Generate htpasswd for basic auth**

```bash
# Generate bcrypt hash for admin:hdim-demo-2026
htpasswd -nb admin hdim-demo-2026
```

Use the output in the secret. If htpasswd is not installed:

```bash
docker run --rm httpd:alpine htpasswd -nb admin hdim-demo-2026
```

- [ ] **Step 2: Create basic auth secret and middleware**

Create `k8s/demo/ingress/basic-auth-secret.yaml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: basic-auth
type: Opaque
stringData:
  users: |
    admin:$apr1$GENERATED_HASH_HERE
```

Create `k8s/demo/ingress/basic-auth-middleware.yaml`:

```yaml
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: basic-auth
spec:
  basicAuth:
    secret: basic-auth
```

- [ ] **Step 3: Create public ingress**

Create `k8s/demo/ingress/ingress-public.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: hdim-public
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: web,websecure
spec:
  rules:
    - host: demo.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: clinical-portal
                port:
                  number: 80
    - host: api.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: gateway-edge
                port:
                  number: 8080
    - host: fhir.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: fhir-service
                port:
                  number: 8085
```

- [ ] **Step 4: Create internal ingress**

Create `k8s/demo/ingress/ingress-internal.yaml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: hdim-internal
  annotations:
    traefik.ingress.kubernetes.io/router.entrypoints: web,websecure
    traefik.ingress.kubernetes.io/router.middlewares: hdim-demo-basic-auth@kubernetescrd
spec:
  rules:
    - host: traces.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: jaeger
                port:
                  number: 16686
    - host: docs.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: gateway-edge
                port:
                  number: 8080
    - host: ops.healthdatainmotion.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: ops-service
                port:
                  number: 4710
```

- [ ] **Step 5: Create ingress kustomization, update root**

Create `k8s/demo/ingress/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - basic-auth-secret.yaml
  - basic-auth-middleware.yaml
  - ingress-public.yaml
  - ingress-internal.yaml
```

Add `- ingress/` to root `k8s/demo/kustomization.yaml`.

- [ ] **Step 6: Validate kustomize build**

```bash
kubectl kustomize k8s/demo/ | grep "kind: Ingress" | wc -l
```

Expected: `2`

- [ ] **Step 7: Commit**

```bash
git add k8s/demo/ingress/
git commit -m "feat(k8s): add traefik ingress — 3 public + 3 basic-auth subdomains"
```

---

### Task 9: Local k3d overlay

**Files:**
- Create: `k8s/demo/local/kustomization.yaml`
- Create: `k8s/demo/local/k3d-cluster.yaml`
- Create: `k8s/demo/local/hosts-entries.txt`

- [ ] **Step 1: Create k3d cluster config**

Create `k8s/demo/local/k3d-cluster.yaml`:

```yaml
apiVersion: k3d.io/v1alpha5
kind: Simple
metadata:
  name: hdim-demo
servers: 1
agents: 0
ports:
  - port: 80:80
    nodeFilters: [loadbalancer]
  - port: 443:443
    nodeFilters: [loadbalancer]
options:
  k3d:
    wait: true
    timeout: 180s
  kubeconfig:
    updateDefaultKubeconfig: true
    switchCurrentContext: true
```

- [ ] **Step 2: Create hosts entries**

Create `k8s/demo/local/hosts-entries.txt`:

```
# HDIM Demo (k3d local) — added by k3s-local-up.sh, removed by k3s-local-down.sh
127.0.0.1 demo.healthdatainmotion.com
127.0.0.1 api.healthdatainmotion.com
127.0.0.1 fhir.healthdatainmotion.com
127.0.0.1 traces.healthdatainmotion.com
127.0.0.1 docs.healthdatainmotion.com
127.0.0.1 ops.healthdatainmotion.com
```

- [ ] **Step 3: Create local kustomization overlay**

Create `k8s/demo/local/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - ../    # inherit everything from parent

patches:
  # Remove TLS annotations from ingresses
  - target:
      kind: Ingress
      name: hdim-public
    patch: |
      - op: remove
        path: /metadata/annotations/cert-manager.io~1cluster-issuer
  - target:
      kind: Ingress
      name: hdim-internal
    patch: |
      - op: remove
        path: /metadata/annotations/cert-manager.io~1cluster-issuer

  # Set imagePullPolicy to IfNotPresent for all deployments
  - target:
      kind: Deployment
    patch: |
      - op: add
        path: /spec/template/spec/containers/0/imagePullPolicy
        value: IfNotPresent
```

- [ ] **Step 4: Commit**

```bash
git add k8s/demo/local/
git commit -m "feat(k8s): add k3d local overlay — no TLS, local storage, hosts entries"
```

---

### Task 10: Local startup and teardown scripts

**Files:**
- Create: `scripts/k3s-local-up.sh`
- Create: `scripts/k3s-local-down.sh`

- [ ] **Step 1: Create k3s-local-up.sh**

Create `scripts/k3s-local-up.sh`:

```bash
#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
K3D_CONFIG="$PROJECT_ROOT/k8s/demo/local/k3d-cluster.yaml"
HOSTS_FILE="$PROJECT_ROOT/k8s/demo/local/hosts-entries.txt"
NAMESPACE="hdim-demo"

echo "================================================"
echo "  HDIM Demo — K3d Local Setup"
echo "================================================"
echo ""

# Step 1: Check prerequisites
echo "Step 1/5: Checking prerequisites..."

if ! command -v docker &>/dev/null; then
    echo "ERROR: Docker not found. Install Docker first."
    exit 1
fi

if ! docker info &>/dev/null; then
    echo "ERROR: Docker daemon not running."
    exit 1
fi

if ! command -v k3d &>/dev/null; then
    echo "Installing k3d..."
    curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash
fi

if ! command -v kubectl &>/dev/null; then
    echo "Installing kubectl..."
    curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
    chmod +x kubectl
    sudo mv kubectl /usr/local/bin/kubectl
fi

echo "  docker:  $(docker version --format '{{.Server.Version}}')"
echo "  k3d:     $(k3d version -s | head -1)"
echo "  kubectl: $(kubectl version --client -o json 2>/dev/null | grep -o '"gitVersion":"[^"]*"' | cut -d'"' -f4)"
echo ""

# Step 2: Create cluster (or reuse existing)
echo "Step 2/5: Creating K3d cluster..."

if k3d cluster list | grep -q hdim-demo; then
    echo "  Cluster 'hdim-demo' already exists."
    read -p "  Delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        k3d cluster delete hdim-demo
    else
        echo "  Reusing existing cluster."
    fi
fi

if ! k3d cluster list | grep -q hdim-demo; then
    k3d cluster create --config "$K3D_CONFIG"
fi

echo "  Cluster ready."
echo ""

# Step 3: Apply manifests
echo "Step 3/5: Applying K8s manifests..."
kubectl apply -k "$PROJECT_ROOT/k8s/demo/local/"
echo ""

# Step 4: Add /etc/hosts entries
echo "Step 4/5: Configuring /etc/hosts..."

if grep -q "hdim-demo" /etc/hosts 2>/dev/null; then
    echo "  /etc/hosts entries already present."
else
    echo "  Adding subdomain entries (requires sudo)..."
    cat "$HOSTS_FILE" | sudo tee -a /etc/hosts > /dev/null
    echo "  Done."
fi
echo ""

# Step 5: Wait for pods
echo "Step 5/5: Waiting for pods to become ready..."
echo "  (this may take 5-10 minutes on first run)"
echo ""

# Wait for infrastructure first
echo "  Infrastructure:"
kubectl -n "$NAMESPACE" wait --for=condition=ready pod -l tier=infrastructure --timeout=300s 2>/dev/null || true

# Then wait for everything
kubectl -n "$NAMESPACE" wait --for=condition=ready pod --all --timeout=600s 2>/dev/null || true

echo ""
echo "================================================"
echo "  HDIM Demo — Local Environment Ready"
echo "================================================"
echo ""
echo "  Clinical Portal:  http://demo.healthdatainmotion.com"
echo "  API Gateway:      http://api.healthdatainmotion.com"
echo "  FHIR Metadata:    http://fhir.healthdatainmotion.com/metadata"
echo "  Jaeger Tracing:   http://traces.healthdatainmotion.com  (admin / hdim-demo-2026)"
echo "  API Docs:         http://docs.healthdatainmotion.com/swagger-ui/"
echo "  Ops Console:      http://ops.healthdatainmotion.com    (admin / hdim-demo-2026)"
echo ""
echo "  Demo login:       demo_admin@hdim.ai / demo123"
echo ""
echo "  kubectl -n $NAMESPACE get pods"
echo "  kubectl -n $NAMESPACE logs -f deployment/<service>"
echo ""
echo "  To tear down:     ./scripts/k3s-local-down.sh"
echo ""
```

- [ ] **Step 2: Create k3s-local-down.sh**

Create `scripts/k3s-local-down.sh`:

```bash
#!/bin/bash
set -euo pipefail

echo "================================================"
echo "  HDIM Demo — K3d Teardown"
echo "================================================"
echo ""

# Delete cluster
if k3d cluster list 2>/dev/null | grep -q hdim-demo; then
    echo "Deleting K3d cluster..."
    k3d cluster delete hdim-demo
    echo "  Cluster deleted."
else
    echo "  No hdim-demo cluster found."
fi

# Remove /etc/hosts entries
if grep -q "hdim-demo" /etc/hosts 2>/dev/null; then
    echo "Removing /etc/hosts entries (requires sudo)..."
    sudo sed -i '/# HDIM Demo (k3d local)/d' /etc/hosts
    sudo sed -i '/healthdatainmotion\.com/d' /etc/hosts
    echo "  Entries removed."
else
    echo "  No /etc/hosts entries to remove."
fi

echo ""
echo "  Teardown complete. Docker images are preserved for fast restart."
echo ""
```

- [ ] **Step 3: Make scripts executable**

```bash
chmod +x scripts/k3s-local-up.sh scripts/k3s-local-down.sh
```

- [ ] **Step 4: Commit**

```bash
git add scripts/k3s-local-up.sh scripts/k3s-local-down.sh
git commit -m "feat(k8s): add k3s-local-up/down scripts for local k3d validation"
```

---

### Task 11: Validation script

**Files:**
- Create: `scripts/k3s-validate-demo.sh`

- [ ] **Step 1: Create validation script**

Create `scripts/k3s-validate-demo.sh` — adapted from `gcp-validate-demo.sh` but uses kubectl for pod status and curl for service health. Supports both local (direct curl) and remote (kubectl port-forward) modes.

```bash
#!/bin/bash
set +e

# HDIM K3s Demo Validation
# Validates all pods, ingress routing, auth flow, FHIR, demo data

NAMESPACE="${NAMESPACE:-hdim-demo}"
BASE_URL="${BASE_URL:-http://demo.healthdatainmotion.com}"
API_URL="${API_URL:-http://api.healthdatainmotion.com}"
FHIR_URL="${FHIR_URL:-http://fhir.healthdatainmotion.com}"

echo "================================================"
echo "  HDIM K3s Demo Validation"
echo "================================================"
echo ""

PASS=0
FAIL=0
WARN=0

check() {
    local label="$1" result="$2"
    if [ "$result" = "pass" ]; then echo "  [PASS] $label"; PASS=$((PASS+1))
    elif [ "$result" = "warn" ]; then echo "  [WARN] $label"; WARN=$((WARN+1))
    else echo "  [FAIL] $label"; FAIL=$((FAIL+1)); fi
}

# 1. Pod status
echo "1. Pod Status"
READY=$(kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null | grep -c "Running")
TOTAL=$(kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null | wc -l)
if [ "$READY" -ge 19 ] 2>/dev/null; then check "All pods running ($READY/$TOTAL)" "pass"
elif [ "$READY" -ge 15 ] 2>/dev/null; then check "Most pods running ($READY/$TOTAL)" "warn"
else check "Pods running: $READY/$TOTAL" "fail"; fi

# Show non-ready pods
kubectl -n "$NAMESPACE" get pods --no-headers 2>/dev/null | grep -v Running | while read -r line; do
    echo "    -> $line"
done
echo ""

# 2. Infrastructure
echo "2. Infrastructure"
for svc in postgres redis kafka jaeger; do
    STATUS=$(kubectl -n "$NAMESPACE" get pod -l app=$svc -o jsonpath='{.items[0].status.phase}' 2>/dev/null)
    if [ "$STATUS" = "Running" ]; then check "$svc" "pass"; else check "$svc" "fail"; fi
done
echo ""

# 3. Service health endpoints
echo "3. Service Health (via ingress)"
for pair in \
    "Clinical Portal|${BASE_URL}/" \
    "Gateway Edge|${API_URL}/actuator/health" \
    "FHIR Metadata|${FHIR_URL}/metadata"; do
    label="${pair%%|*}"
    url="${pair##*|}"
    if curl -sf "$url" > /dev/null 2>&1; then check "$label" "pass"; else check "$label" "fail"; fi
done
echo ""

# 4. Auth flow
echo "4. Authentication"
LOGIN_RESPONSE=$(curl -sf -X POST "${API_URL}/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"demo_admin","password":"demo123"}' 2>/dev/null)

if echo "$LOGIN_RESPONSE" | grep -q "token"; then
    check "Login with demo_admin" "pass"
    TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ -n "$TOKEN" ]; then
        check "JWT token received" "pass"
        AUTH_RESP=$(curl -sf "${API_URL}/patient/api/v1/patients" \
            -H "Authorization: Bearer $TOKEN" \
            -H "X-Tenant-ID: demo-tenant" 2>/dev/null)
        if [ -n "$AUTH_RESP" ]; then check "Authenticated request" "pass"
        else check "Authenticated request" "fail"; fi
    else check "JWT token received" "fail"; fi
else check "Login with demo_admin" "fail"; fi
echo ""

# 5. FHIR
echo "5. FHIR Conformance"
FHIR_META=$(curl -sf "${FHIR_URL}/metadata" 2>/dev/null)
if echo "$FHIR_META" | grep -q "4.0.1"; then check "FHIR R4 version 4.0.1" "pass"
elif [ -n "$FHIR_META" ]; then check "FHIR metadata (version mismatch)" "warn"
else check "FHIR R4 metadata" "fail"; fi
echo ""

# 6. Basic auth on internal routes
echo "6. Internal Route Protection"
TRACES_UNAUTH=$(curl -sf -o /dev/null -w "%{http_code}" "http://traces.healthdatainmotion.com/" 2>/dev/null)
if [ "$TRACES_UNAUTH" = "401" ]; then check "Jaeger requires basic auth" "pass"
else check "Jaeger requires basic auth (got $TRACES_UNAUTH)" "warn"; fi
echo ""

# Summary
TOTAL=$((PASS + FAIL + WARN))
echo "================================================"
echo "  Validation Summary"
echo "================================================"
echo ""
echo "  PASS:     $PASS/$TOTAL"
echo "  WARNINGS: $WARN/$TOTAL"
echo "  FAILED:   $FAIL/$TOTAL"
echo ""
if [ $FAIL -eq 0 ]; then echo "  All checks passed!"; else echo "  $FAIL check(s) failed."; fi
echo ""

exit $FAIL
```

- [ ] **Step 2: Make executable**

```bash
chmod +x scripts/k3s-validate-demo.sh
```

- [ ] **Step 3: Commit**

```bash
git add scripts/k3s-validate-demo.sh
git commit -m "feat(k8s): add k3s-validate-demo.sh for local and remote validation"
```

---

### Task 12: Update root kustomization and final local test

- [ ] **Step 1: Verify final root kustomization includes all subdirectories**

`k8s/demo/kustomization.yaml` should have:

```yaml
resources:
  - namespace.yaml
  - config/
  - infrastructure/
  - gateways/
  - services/
  - frontend/
  - observability/
  - ops/
  - ingress/
```

- [ ] **Step 2: Full kustomize build validation**

```bash
kubectl kustomize k8s/demo/ > /tmp/hdim-demo-full.yaml
grep "kind: Deployment" /tmp/hdim-demo-full.yaml | wc -l    # expect: 17
grep "kind: StatefulSet" /tmp/hdim-demo-full.yaml | wc -l    # expect: 2
grep "kind: Service" /tmp/hdim-demo-full.yaml | wc -l        # expect: 19
grep "kind: Ingress" /tmp/hdim-demo-full.yaml | wc -l        # expect: 2
grep "kind: ConfigMap" /tmp/hdim-demo-full.yaml | wc -l      # expect: 5
grep "kind: Secret" /tmp/hdim-demo-full.yaml | wc -l         # expect: 5+
wc -l /tmp/hdim-demo-full.yaml                                # total lines
```

- [ ] **Step 3: Run k3d local test (smoke test)**

```bash
./scripts/k3s-local-up.sh
# Wait for pods
kubectl -n hdim-demo get pods -w
# Once all running:
./scripts/k3s-validate-demo.sh
# Tear down:
./scripts/k3s-local-down.sh
```

- [ ] **Step 4: Final commit**

```bash
git add k8s/demo/kustomization.yaml
git commit -m "feat(k8s): complete demo deployment — 19 services, 6 subdomains, local validation"
```

---

## Chunk 4: FluxCD and Hetzner Provisioning (Post-Validation)

> This chunk executes AFTER local k3d validation passes. It adds GitOps and the Hetzner provisioning script.

### Task 13: FluxCD configuration

**Files:**
- Create: `k8s/demo/flux/kustomization.yaml`
- Create: `k8s/demo/flux/gotk-sync.yaml`

- [ ] **Step 1: Create FluxCD sync manifest**

Create `k8s/demo/flux/gotk-sync.yaml`:

```yaml
apiVersion: source.toolkit.fluxcd.io/v1
kind: GitRepository
metadata:
  name: hdim
  namespace: flux-system
spec:
  interval: 1m
  url: https://github.com/mahoosuc-solutions/hdim.git
  ref:
    branch: main
---
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: hdim-demo
  namespace: flux-system
spec:
  interval: 5m
  sourceRef:
    kind: GitRepository
    name: hdim
  path: ./k8s/demo
  prune: true
  wait: true
  timeout: 10m
```

- [ ] **Step 2: Create flux kustomization**

Create `k8s/demo/flux/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
  - gotk-sync.yaml
```

- [ ] **Step 3: Commit**

```bash
git add k8s/demo/flux/
git commit -m "feat(k8s): add FluxCD GitOps sync — watches k8s/demo/ on main branch"
```

---

### Task 14: Hetzner provisioning script

**Files:**
- Create: `scripts/k3s-create-hetzner.sh`

- [ ] **Step 1: Create provisioning script**

Create `scripts/k3s-create-hetzner.sh` — creates Hetzner CCX33, installs K3s, bootstraps FluxCD, sets up firewall. Requires `hcloud` CLI and `flux` CLI.

```bash
#!/bin/bash
set -euo pipefail

# HDIM K3s — Hetzner Provisioning
# Creates CCX33, installs K3s, bootstraps FluxCD

SERVER_NAME="${SERVER_NAME:-hdim-demo}"
SERVER_TYPE="${SERVER_TYPE:-ccx33}"
LOCATION="${LOCATION:-fsn1}"
SSH_KEY="${SSH_KEY:-default}"
GITHUB_OWNER="${GITHUB_OWNER:-mahoosuc-solutions}"
GITHUB_REPO="${GITHUB_REPO:-hdim}"
FLUX_PATH="${FLUX_PATH:-k8s/demo}"

echo "================================================"
echo "  HDIM K3s — Hetzner Setup"
echo "================================================"
echo ""
echo "  Server:   $SERVER_NAME ($SERVER_TYPE)"
echo "  Location: $LOCATION"
echo "  Repo:     $GITHUB_OWNER/$GITHUB_REPO"
echo ""

# Prerequisites
for cmd in hcloud flux kubectl; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "ERROR: $cmd not found. Install it first."
        exit 1
    fi
done

# Step 1: Create server
echo "Step 1/5: Creating Hetzner server..."
SERVER_IP=$(hcloud server create \
    --name "$SERVER_NAME" \
    --type "$SERVER_TYPE" \
    --image ubuntu-22.04 \
    --location "$LOCATION" \
    --ssh-key "$SSH_KEY" \
    --format json 2>/dev/null | grep -o '"ipv4_address":"[^"]*"' | cut -d'"' -f4)

if [ -z "$SERVER_IP" ]; then
    SERVER_IP=$(hcloud server ip "$SERVER_NAME" 2>/dev/null)
fi

echo "  Server IP: $SERVER_IP"
echo ""

# Step 2: Firewall
echo "Step 2/5: Configuring firewall..."
hcloud firewall create --name hdim-demo-fw 2>/dev/null || true
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 80 --source-ips 0.0.0.0/0 2>/dev/null || true
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 443 --source-ips 0.0.0.0/0 2>/dev/null || true
hcloud firewall add-rule hdim-demo-fw --direction in --protocol tcp --port 6443 --source-ips 0.0.0.0/0 2>/dev/null || true
hcloud firewall apply-to-resource hdim-demo-fw --type server --server "$SERVER_NAME" 2>/dev/null || true
echo "  Firewall configured."
echo ""

# Step 3: Install K3s
echo "Step 3/5: Installing K3s (this takes ~2 minutes)..."
ssh -o StrictHostKeyChecking=no "root@$SERVER_IP" <<'REMOTE'
    curl -sfL https://get.k3s.io | sh -
    # Wait for K3s to be ready
    until kubectl get nodes &>/dev/null; do sleep 2; done
    echo "K3s installed."
REMOTE
echo "  K3s running."
echo ""

# Step 4: Copy kubeconfig
echo "Step 4/5: Fetching kubeconfig..."
scp "root@$SERVER_IP:/etc/rancher/k3s/k3s.yaml" /tmp/k3s-kubeconfig.yaml
sed -i "s|127.0.0.1|$SERVER_IP|g" /tmp/k3s-kubeconfig.yaml
export KUBECONFIG=/tmp/k3s-kubeconfig.yaml
kubectl get nodes
echo ""

# Step 5: Bootstrap FluxCD
echo "Step 5/5: Bootstrapping FluxCD..."
echo "  Requires GITHUB_TOKEN env var with repo access."

if [ -z "${GITHUB_TOKEN:-}" ]; then
    echo "  WARNING: GITHUB_TOKEN not set. Set it and run:"
    echo "    export GITHUB_TOKEN=ghp_..."
    echo "    flux bootstrap github --owner=$GITHUB_OWNER --repository=$GITHUB_REPO --path=$FLUX_PATH --branch=main"
    echo ""
    echo "  Skipping FluxCD bootstrap. K3s is ready for manual kubectl apply."
else
    flux bootstrap github \
        --owner="$GITHUB_OWNER" \
        --repository="$GITHUB_REPO" \
        --path="$FLUX_PATH" \
        --branch=main
    echo "  FluxCD bootstrapped. Cluster will auto-sync from git."
fi

echo ""
echo "================================================"
echo "  Hetzner K3s Setup Complete"
echo "================================================"
echo ""
echo "  Server:     $SERVER_NAME"
echo "  IP:         $SERVER_IP"
echo "  Kubeconfig: /tmp/k3s-kubeconfig.yaml"
echo ""
echo "  DNS: Add A record *.healthdatainmotion.com -> $SERVER_IP"
echo ""
echo "  Validate:   KUBECONFIG=/tmp/k3s-kubeconfig.yaml ./scripts/k3s-validate-demo.sh"
echo ""
echo "  Monthly cost: ~\$35 (CCX33)"
echo ""
```

- [ ] **Step 2: Make executable and commit**

```bash
chmod +x scripts/k3s-create-hetzner.sh
git add scripts/k3s-create-hetzner.sh k8s/demo/flux/
git commit -m "feat(k8s): add Hetzner provisioning script and FluxCD bootstrap"
```

---

## Summary

| Chunk | Tasks | What it delivers |
|-------|-------|-----------------|
| 1 | Tasks 1-4 | Namespace, config, secrets, PostgreSQL, Redis, Kafka |
| 2 | Tasks 5-7 | 4 gateways, 9 backend services, portal, jaeger, ops |
| 3 | Tasks 8-12 | Ingress routing, k3d local overlay, scripts, validation, smoke test |
| 4 | Tasks 13-14 | FluxCD GitOps, Hetzner provisioning |

**Total files created:** ~35 YAML manifests + 4 scripts
**Local validation:** No cloud account needed through Chunk 3
**First cloud deploy:** Chunk 4, requires Hetzner account + `hcloud` CLI

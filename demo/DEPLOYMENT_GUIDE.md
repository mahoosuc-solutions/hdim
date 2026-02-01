# HDIM Demo Deployment Guide

Multi-platform deployment guide for the HDIM Care Gap Demo.

---

## Quick Test & Deploy

```bash
# Test locally before deploying
./test-demo-deployment.sh

# Deploy to specific platform
./test-demo-deployment.sh --platform cloud
./test-demo-deployment.sh --platform swarm
./test-demo-deployment.sh --platform k8s
```

---

## Platform 1: Local Docker (Single VM)

**Best for**: Development, customer demos, proof of concept

### Requirements
- Docker 24.0+
- Docker Compose 2.20+
- 8GB RAM minimum (16GB recommended)
- 20GB disk space

### Deployment

```bash
# Test deployment
./test-demo-deployment.sh --platform local

# Start demo
./start-demo.sh

# Access
http://localhost:4200
```

### Verification

```bash
# Check all services
docker compose -f docker-compose.demo.yml ps

# View logs
docker compose -f docker-compose.demo.yml logs -f

# Test endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:8085/fhir/Patient?_count=1
```

---

## Platform 2: Cloud VM (AWS/Azure/GCP)

**Best for**: Production demos, customer trials, temporary deployments

### AWS EC2 Deployment

#### 1. Launch EC2 Instance

```bash
# Instance type: t3.large or larger
# AMI: Ubuntu 22.04 LTS
# Storage: 40GB GP3
# Security Group: Allow 22, 80, 443, 4200, 8080-8087

# Example AWS CLI
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.large \
  --key-name your-key \
  --security-group-ids sg-xxxxx \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":40}}]' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=hdim-demo}]'
```

#### 2. Install Docker

```bash
# SSH to instance
ssh -i your-key.pem ubuntu@<instance-ip>

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Log out and back in for group changes
exit
```

#### 3. Deploy Demo

```bash
# Clone repository or copy demo files
git clone <repo-url> hdim-demo
cd hdim-demo/demo

# Run comprehensive test
./test-demo-deployment.sh --platform cloud

# Start demo
./start-demo.sh

# Configure firewall (if needed)
sudo ufw allow 4200/tcp
sudo ufw allow 8080/tcp
```

#### 4. Access Demo

```
http://<instance-public-ip>:4200
```

### Azure VM Deployment

```bash
# Create resource group
az group create --name hdim-demo-rg --location eastus

# Create VM
az vm create \
  --resource-group hdim-demo-rg \
  --name hdim-demo-vm \
  --image Ubuntu2204 \
  --size Standard_D2s_v3 \
  --admin-username azureuser \
  --generate-ssh-keys

# Open ports
az vm open-port --port 4200 --resource-group hdim-demo-rg --name hdim-demo-vm
az vm open-port --port 8080 --resource-group hdim-demo-rg --name hdim-demo-vm

# Get public IP
az vm show --resource-group hdim-demo-rg --name hdim-demo-vm --show-details --query publicIps -o tsv

# SSH and deploy (follow steps from AWS section)
```

### GCP Compute Engine Deployment

```bash
# Create VM
gcloud compute instances create hdim-demo-vm \
  --zone=us-central1-a \
  --machine-type=n2-standard-2 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=40GB \
  --tags=hdim-demo

# Create firewall rules
gcloud compute firewall-rules create hdim-demo-allow \
  --allow tcp:4200,tcp:8080-8087 \
  --target-tags hdim-demo

# SSH and deploy (follow steps from AWS section)
gcloud compute ssh hdim-demo-vm --zone=us-central1-a
```

---

## Platform 3: Docker Swarm

**Best for**: Multi-node deployments, high availability

### Prerequisites

```bash
# Initialize swarm (on manager node)
docker swarm init

# Join worker nodes (run on worker nodes)
docker swarm join --token <token> <manager-ip>:2377
```

### Convert Compose to Stack

Create `docker-stack.demo.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: healthdata
      POSTGRES_PASSWORD: demo_password_2024
      POSTGRES_DB: healthdata_demo
    volumes:
      - demo_postgres_data:/var/lib/postgresql/data
    networks:
      - hdim-demo-network
    deploy:
      replicas: 1
      placement:
        constraints:
          - node.role == manager

  redis:
    image: redis:7-alpine
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    networks:
      - hdim-demo-network
    deploy:
      replicas: 1

  # ... (additional services with deploy: configs)

networks:
  hdim-demo-network:
    driver: overlay

volumes:
  demo_postgres_data:
```

### Deploy Stack

```bash
# Deploy stack
docker stack deploy -c docker-stack.demo.yml hdim-demo

# Check services
docker stack services hdim-demo

# Check logs
docker service logs hdim-demo_care-gap-service

# Scale service
docker service scale hdim-demo_care-gap-service=3

# Remove stack
docker stack rm hdim-demo
```

---

## Platform 4: Kubernetes

**Best for**: Production deployments, enterprise scale

### Generate Kubernetes Manifests

The test script generates K8s manifests. Manually create:

#### Namespace

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: hdim-demo
```

#### ConfigMap

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: hdim-demo-config
  namespace: hdim-demo
data:
  POSTGRES_HOST: postgres
  POSTGRES_PORT: "5432"
  POSTGRES_DB: healthdata_demo
  REDIS_HOST: redis
  REDIS_PORT: "6379"
```

#### Secret

```yaml
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: hdim-demo-secret
  namespace: hdim-demo
type: Opaque
stringData:
  POSTGRES_PASSWORD: demo_password_2024
  JWT_SECRET: DemoSecretKeyForJWTMustBeAtLeast256BitsLongForHS256Algorithm123456789
```

#### PostgreSQL Deployment

```yaml
# postgres-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: hdim-demo
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:16-alpine
        env:
        - name: POSTGRES_USER
          value: healthdata
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hdim-demo-secret
              key: POSTGRES_PASSWORD
        - name: POSTGRES_DB
          value: healthdata_demo
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-data
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-data
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: hdim-demo
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
```

#### Care Gap Service Deployment

```yaml
# care-gap-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: care-gap-service
  namespace: hdim-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: care-gap-service
  template:
    metadata:
      labels:
        app: care-gap-service
    spec:
      containers:
      - name: care-gap-service
        image: your-registry/hdim-care-gap-service:latest
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres:5432/caregap_db"
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: hdim-demo-secret
              key: POSTGRES_PASSWORD
        ports:
        - containerPort: 8086
        livenessProbe:
          httpGet:
            path: /care-gap/actuator/health/liveness
            port: 8086
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /care-gap/actuator/health/readiness
            port: 8086
          initialDelaySeconds: 30
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: care-gap-service
  namespace: hdim-demo
spec:
  selector:
    app: care-gap-service
  ports:
  - port: 8086
    targetPort: 8086
```

#### Ingress

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: hdim-demo-ingress
  namespace: hdim-demo
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - demo.healthdatainmotion.com
    secretName: hdim-demo-tls
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
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: gateway-service
            port:
              number: 8080
```

### Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f namespace.yaml

# Create config and secrets
kubectl apply -f configmap.yaml
kubectl apply -f secret.yaml

# Deploy infrastructure
kubectl apply -f postgres-deployment.yaml
kubectl apply -f redis-deployment.yaml

# Wait for infrastructure
kubectl wait --for=condition=ready pod -l app=postgres -n hdim-demo --timeout=120s

# Deploy services
kubectl apply -f care-gap-deployment.yaml
kubectl apply -f fhir-deployment.yaml
kubectl apply -f gateway-deployment.yaml
# ... (all services)

# Deploy ingress
kubectl apply -f ingress.yaml

# Check status
kubectl get all -n hdim-demo

# View logs
kubectl logs -f deployment/care-gap-service -n hdim-demo

# Scale deployment
kubectl scale deployment care-gap-service --replicas=5 -n hdim-demo
```

---

## Monitoring & Observability

### Health Checks

```bash
# All services
curl http://localhost:8080/actuator/health

# Specific service
curl http://localhost:8086/care-gap/actuator/health

# Kubernetes liveness
kubectl get pods -n hdim-demo -o json | jq '.items[] | {name: .metadata.name, ready: .status.conditions[] | select(.type=="Ready") | .status}'
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Service-specific metrics
curl http://localhost:8086/care-gap/actuator/metrics
```

### Logs

```bash
# Docker Compose
docker compose -f docker-compose.demo.yml logs -f care-gap-service

# Docker Swarm
docker service logs hdim-demo_care-gap-service

# Kubernetes
kubectl logs -f deployment/care-gap-service -n hdim-demo
```

---

## Performance Tuning

### Database

```sql
-- Increase connections
ALTER SYSTEM SET max_connections = 200;

-- Tune memory
ALTER SYSTEM SET shared_buffers = '2GB';
ALTER SYSTEM SET effective_cache_size = '6GB';

-- Apply changes
SELECT pg_reload_conf();
```

### Redis

```bash
# Increase memory
docker exec hdim-demo-redis redis-cli CONFIG SET maxmemory 512mb

# Persistence (if needed)
docker exec hdim-demo-redis redis-cli CONFIG SET save "900 1 300 10"
```

### JVM

```yaml
# Add to service environment
JAVA_OPTS: "-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

---

## Security Hardening

### Production Checklist

- [ ] Change all default passwords
- [ ] Generate unique JWT secret (64+ characters)
- [ ] Enable HTTPS/TLS
- [ ] Configure firewall (allow only necessary ports)
- [ ] Enable audit logging
- [ ] Set up monitoring alerts
- [ ] Configure backup retention
- [ ] Review HIPAA compliance settings
- [ ] Enable rate limiting
- [ ] Configure CORS properly

### SSL/TLS Setup

```bash
# Using Let's Encrypt with Nginx
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d demo.healthdatainmotion.com

# Auto-renewal
sudo systemctl enable certbot.timer
```

---

## Backup & Recovery

### Backup

```bash
# Database backup
docker exec hdim-demo-postgres pg_dump -U healthdata healthdata_demo > backup_$(date +%Y%m%d).sql

# Full backup (all volumes)
docker run --rm -v hdim-demo_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup_$(date +%Y%m%d).tar.gz /data
```

### Restore

```bash
# Database restore
docker exec -i hdim-demo-postgres psql -U healthdata healthdata_demo < backup_20250131.sql

# Volume restore
docker run --rm -v hdim-demo_postgres_data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres_backup_20250131.tar.gz -C /
```

---

## Troubleshooting

### Services Not Starting

```bash
# Check container logs
docker compose -f docker-compose.demo.yml logs service-name

# Check resource usage
docker stats

# Verify network
docker network inspect hdim-demo-network
```

### Database Connection Issues

```bash
# Test connection
docker exec hdim-demo-postgres psql -U healthdata -d healthdata_demo -c "SELECT 1"

# Check connections
docker exec hdim-demo-postgres psql -U healthdata -d healthdata_demo -c "SELECT count(*) FROM pg_stat_activity"
```

### Performance Issues

```bash
# Check slow queries
docker exec hdim-demo-postgres psql -U healthdata -d healthdata_demo -c "SELECT query, calls, total_time FROM pg_stat_statements ORDER BY total_time DESC LIMIT 10"

# Redis cache hit rate
docker exec hdim-demo-redis redis-cli INFO stats | grep keyspace
```

---

## Support & Documentation

- **Test Script**: `./test-demo-deployment.sh --help`
- **Demo Guide**: [DEMO_WALKTHROUGH.md](DEMO_WALKTHROUGH.md)
- **Architecture**: [docs/architecture/SYSTEM_ARCHITECTURE.md](../docs/architecture/SYSTEM_ARCHITECTURE.md)
- **Support**: support@healthdatainmotion.com

---

*Last Updated: December 31, 2025*

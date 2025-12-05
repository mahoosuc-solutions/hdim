# GCP Demo Deployment - Cost-Optimized & On-Demand

**Purpose**: Deploy HealthData In Motion for demos with minimal cost when offline
**Strategy**: Single VM with Docker Compose that can be stopped when not needed
**Cost**: ~$25/month when stopped, ~$5/hour when running

---

## Quick Start (TL;DR)

```bash
# 1. Create VM and deploy (one-time setup - 15 minutes)
./scripts/gcp-create-demo-vm.sh

# 2. Start demo (when you need it - 5 minutes)
./scripts/gcp-start-demo.sh

# 3. Stop demo (when finished - 2 minutes)
./scripts/gcp-stop-demo.sh

# Access at: http://YOUR-IP:4200
```

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│  Single GCP VM (e2-standard-4)                      │
│  4 vCPUs, 16GB RAM, 100GB SSD                       │
│  Ubuntu 22.04 LTS                                   │
│                                                     │
│  Docker Compose Services:                           │
│  ├─ PostgreSQL                                      │
│  ├─ Redis                                           │
│  ├─ Kafka + Zookeeper                               │
│  ├─ FHIR Service                                    │
│  ├─ CQL Engine Service                              │
│  ├─ Quality Measure Service                         │
│  └─ Clinical Portal (Angular)                       │
│                                                     │
│  Status: Can be STOPPED when not in use             │
│  Startup Time: ~5 minutes                           │
│  Cost When Stopped: $0/hour (only disk storage)    │
└─────────────────────────────────────────────────────┘
```

---

## Cost Breakdown

### Scenario: Demo 2 times per month, 4 hours each

| Resource | Cost When Running | Cost When Stopped | Monthly Cost |
|----------|------------------|-------------------|--------------|
| e2-standard-4 VM | $0.15/hour | $0/hour | $1.20 (8 hours) |
| 100GB SSD Persistent Disk | Always charged | Always charged | $17/month |
| Static IP (assigned) | $0/hour | $0/hour | $0/month |
| Static IP (unassigned) | $0/hour | $0.007/hour | $5/month |
| Network Egress | ~$0.12/GB | $0 | ~$1/month |
| **TOTAL** | **$0.15/hour** | **$17/month** | **~$23/month** |

**Running Cost**: $0.15/hour × 8 hours = $1.20/month
**Storage Cost**: $17/month (always charged)
**Total Monthly**: ~$18-20/month for 2 demos

### Comparison

| Scenario | Monthly Cost |
|----------|--------------|
| **Always Running 24/7** | ~$150/month |
| **Running 8 hours/week** | ~$40/month |
| **Running 8 hours/month (2 demos)** | ~$20/month |
| **Stopped + Snapshots Only** | ~$5/month |

---

## One-Time Setup (15 minutes)

### Prerequisites

1. **GCP Account** with billing enabled
2. **gcloud CLI** installed
3. **Project created** in GCP Console

### Step 1: Configure GCP

```bash
# Set your project details
export PROJECT_ID="healthdata-demo"
export REGION="us-central1"
export ZONE="us-central1-a"
export VM_NAME="healthdata-demo"

# Login and set project
gcloud auth login
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable compute.googleapis.com
gcloud services enable cloudscheduler.googleapis.com
```

### Step 2: Create Demo VM

Save this as `scripts/gcp-create-demo-vm.sh`:

```bash
#!/bin/bash
set -e

PROJECT_ID=${PROJECT_ID:-"healthdata-demo"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}

echo "Creating HealthData Demo VM..."

# Create VM
gcloud compute instances create $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --machine-type=e2-standard-4 \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=100GB \
  --boot-disk-type=pd-ssd \
  --tags=healthdata-demo,http-server \
  --metadata=startup-script='#!/bin/bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
usermod -aG docker $(logname)

# Install Docker Compose
DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep "tag_name" | cut -d\" -f4)
curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install utilities
apt-get update
apt-get install -y git curl wget jq

# Create app directory
mkdir -p /opt/healthdata
chown -R $(logname):$(logname) /opt/healthdata

echo "VM setup complete" > /opt/healthdata/setup-complete.txt
'

# Create firewall rule
gcloud compute firewall-rules create healthdata-allow-demo \
  --project=$PROJECT_ID \
  --allow=tcp:4200,tcp:8081,tcp:8083,tcp:8087 \
  --target-tags=healthdata-demo \
  --description="Allow HealthData demo ports"

echo "✅ VM created successfully!"
echo "Waiting for startup script to complete (this takes ~2 minutes)..."
sleep 120

# Get IP
VM_IP=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

echo ""
echo "VM IP Address: $VM_IP"
echo ""
echo "Next steps:"
echo "1. SSH into VM: gcloud compute ssh $VM_NAME --zone=$ZONE"
echo "2. Deploy application: Run deployment script"
```

### Step 3: Deploy Application

SSH into the VM and run:

```bash
# SSH into VM
gcloud compute ssh $VM_NAME --zone=$ZONE

# Clone repository
cd /opt/healthdata
git clone https://github.com/YOUR_ORG/healthdata-in-motion.git
cd healthdata-in-motion

# Build backend
cd backend
./gradlew build -x test

# Build Docker images
docker-compose -f docker-compose.yml build

# Start services
docker-compose up -d

# Load demo data
./load-demo-data.sh

# Verify
docker-compose ps
curl http://localhost:8083/fhir/actuator/health
```

---

## Daily Demo Usage

### Start Demo (5 minutes)

Save as `scripts/gcp-start-demo.sh`:

```bash
#!/bin/bash
set -e

PROJECT_ID=${PROJECT_ID:-"healthdata-demo"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}

echo "🚀 Starting HealthData Demo..."

# Start VM
echo "📡 Starting VM..."
gcloud compute instances start $VM_NAME --project=$PROJECT_ID --zone=$ZONE

# Wait for boot
echo "⏳ Waiting for VM to boot (60 seconds)..."
sleep 60

# Start Docker services
echo "🐳 Starting Docker containers..."
gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
cd /opt/healthdata/healthdata-in-motion
docker-compose up -d
sleep 30
docker-compose ps
'

# Get IP
VM_IP=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

echo ""
echo "✅ Demo is ready!"
echo ""
echo "🌐 Access URLs:"
echo "   Clinical Portal: http://$VM_IP:4200"
echo "   FHIR API:        http://$VM_IP:8083/fhir"
echo "   CQL Engine:      http://$VM_IP:8081/cql-engine"
echo "   Quality Measure: http://$VM_IP:8087/quality-measure"
echo "   Swagger Docs:    http://$VM_IP:8087/quality-measure/swagger-ui.html"
echo ""
echo "🔑 Demo Credentials:"
echo "   Username: demo@healthdata.com"
echo "   Password: Demo123!"
echo ""
echo "⏱️  Total startup time: ~5 minutes"
echo "💰 Running cost: $0.15/hour"
```

### Stop Demo (2 minutes)

Save as `scripts/gcp-stop-demo.sh`:

```bash
#!/bin/bash
set -e

PROJECT_ID=${PROJECT_ID:-"healthdata-demo"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}

echo "🛑 Stopping HealthData Demo..."

# Stop Docker containers (preserves data)
echo "🐳 Stopping Docker containers..."
gcloud compute ssh $VM_NAME --project=$PROJECT_ID --zone=$ZONE --command='
cd /opt/healthdata/healthdata-in-motion
docker-compose stop
'

# Stop VM
echo "📡 Stopping VM..."
gcloud compute instances stop $VM_NAME --project=$PROJECT_ID --zone=$ZONE

echo ""
echo "✅ Demo stopped successfully!"
echo "💾 All data preserved for next demo"
echo "💰 Compute cost: $0/hour (only storage charged)"
echo ""
echo "To restart: ./scripts/gcp-start-demo.sh"
```

### Check Status

```bash
#!/bin/bash
PROJECT_ID=${PROJECT_ID:-"healthdata-demo"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}

echo "Checking demo status..."

STATUS=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(status)")

echo "VM Status: $STATUS"

if [ "$STATUS" == "RUNNING" ]; then
    VM_IP=$(gcloud compute instances describe $VM_NAME \
      --project=$PROJECT_ID \
      --zone=$ZONE \
      --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

    echo "VM IP: $VM_IP"
    echo "Portal: http://$VM_IP:4200"
fi
```

---

## Automated Scheduling (Optional)

### Schedule Demo Days

If you demo every Monday 9 AM - 5 PM:

```bash
# Create service account for Cloud Scheduler
gcloud iam service-accounts create demo-scheduler \
  --display-name="Demo Scheduler Service Account"

# Grant permissions
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:demo-scheduler@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/compute.instanceAdmin.v1"

# Start VM every Monday at 8:30 AM EST
gcloud scheduler jobs create http start-demo \
  --schedule="30 8 * * 1" \
  --time-zone="America/New_York" \
  --uri="https://compute.googleapis.com/compute/v1/projects/$PROJECT_ID/zones/$ZONE/instances/$VM_NAME/start" \
  --http-method=POST \
  --oauth-service-account-email="demo-scheduler@$PROJECT_ID.iam.gserviceaccount.com"

# Stop VM every Monday at 5:30 PM EST
gcloud scheduler jobs create http stop-demo \
  --schedule="30 17 * * 1" \
  --time-zone="America/New_York" \
  --uri="https://compute.googleapis.com/compute/v1/projects/$PROJECT_ID/zones/$ZONE/instances/$VM_NAME/stop" \
  --http-method=POST \
  --oauth-service-account-email="demo-scheduler@$PROJECT_ID.iam.gserviceaccount.com"
```

---

## Advanced: Snapshot-Based Deployment

For even lower costs, delete VM between demos and recreate from snapshot:

### Create Golden Snapshot

```bash
# After configuring VM perfectly
gcloud compute disks snapshot $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --snapshot-names=healthdata-golden-snapshot

# Delete VM (keeps snapshot)
gcloud compute instances delete $VM_NAME --project=$PROJECT_ID --zone=$ZONE

# Cost: ~$2/month for snapshot storage
```

### Recreate from Snapshot

```bash
# Create disk from snapshot
gcloud compute disks create $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --source-snapshot=healthdata-golden-snapshot \
  --type=pd-ssd

# Create VM with restored disk
gcloud compute instances create $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --machine-type=e2-standard-4 \
  --disk=name=$VM_NAME,boot=yes,auto-delete=yes \
  --tags=healthdata-demo,http-server

# Start services (they auto-start on boot)
sleep 60
```

**Cost**: ~$2-5/month (snapshot storage only)
**Startup Time**: ~8 minutes (VM creation + boot + service startup)

---

## Budget Alerts

Set up cost alerts:

```bash
# Create budget with email alerts
gcloud billing budgets create \
  --billing-account=YOUR-BILLING-ACCOUNT-ID \
  --display-name="HealthData Demo Budget" \
  --budget-amount=50USD \
  --threshold-rule=percent=50 \
  --threshold-rule=percent=90 \
  --threshold-rule=percent=100 \
  --all-updates-rule-pubsub-topic=projects/$PROJECT_ID/topics/budget-alerts \
  --email-addresses=your-email@example.com
```

---

## Troubleshooting

### VM won't start
```bash
# Check status
gcloud compute instances describe $VM_NAME --zone=$ZONE

# View logs
gcloud compute instances get-serial-port-output $VM_NAME --zone=$ZONE
```

### Services not responding
```bash
# SSH into VM
gcloud compute ssh $VM_NAME --zone=$ZONE

# Check Docker
sudo systemctl status docker
docker ps

# Check logs
cd /opt/healthdata/healthdata-in-motion
docker-compose logs -f quality-measure-service
```

### Can't access from browser
```bash
# Check firewall
gcloud compute firewall-rules list --filter="name:healthdata"

# Test from VM
gcloud compute ssh $VM_NAME --zone=$ZONE --command="curl http://localhost:4200"

# Get VM IP
gcloud compute instances describe $VM_NAME --zone=$ZONE \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)"
```

---

## Demo Preparation Checklist

**Before Demo Day:**
- [ ] Start VM: `./scripts/gcp-start-demo.sh`
- [ ] Wait 5 minutes for full startup
- [ ] Test portal access in browser
- [ ] Login with demo credentials
- [ ] Verify WebSocket updates working
- [ ] Open Swagger docs tab
- [ ] Prepare talking points

**After Demo:**
- [ ] Stop VM: `./scripts/gcp-stop-demo.sh`
- [ ] Verify VM stopped (check GCP console)
- [ ] Confirm $0/hour compute cost

---

## Migration to Production

When ready to move beyond demos:

1. **Upgrade to Cloud SQL**: Replace Docker PostgreSQL
2. **Use Cloud Run**: Auto-scaling containers
3. **Add Load Balancer**: SSL termination, CDN
4. **Enable Cloud Armor**: DDoS protection
5. **Use Cloud Storage**: Static assets
6. **Implement CI/CD**: Automated deployments

See `GCP_DEPLOYMENT_GUIDE.md` for production architecture.

---

## Summary

This demo deployment provides:

✅ **Low Cost**: ~$20/month for occasional demos
✅ **Quick Start**: 5 minutes from stop to running
✅ **Full Featured**: Complete HIPAA-compliant system
✅ **Easy Management**: Simple scripts
✅ **Scalable**: Can upgrade to production anytime

**Recommended Usage Pattern**:
- Keep VM stopped when not demoing
- Start 30 minutes before demo
- Run demo (typically 1-2 hours)
- Stop immediately after

**Monthly Cost Example**:
- 4 demos per month, 2 hours each = 8 hours
- Running: 8 hours × $0.15 = $1.20
- Storage: $17/month
- **Total: ~$18-20/month**

---

**Next Steps**:
1. Run `scripts/gcp-create-demo-vm.sh` (one-time setup)
2. Test startup/shutdown cycle
3. Schedule your next demo!


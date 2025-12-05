# GCP Demo Deployment - Quick Start

**Purpose**: Cost-optimized demo deployment that's offline when not in use

**Cost**: ~$20/month for 2-4 demos
**Startup Time**: 5 minutes
**Shutdown Time**: 2 minutes

---

## Quick Start

### First Time Setup (15 minutes)

```bash
# 1. Set your GCP project
export PROJECT_ID="healthdata-demo"
export ZONE="us-central1-a"

# 2. Login to GCP
gcloud auth login
gcloud config set project $PROJECT_ID

# 3. Create the demo VM
./scripts/gcp-create-demo-vm.sh

# 4. SSH into VM and deploy application
gcloud compute ssh healthdata-demo --zone=us-central1-a

# Inside VM:
cd /opt/healthdata
git clone https://github.com/YOUR_ORG/healthdata-in-motion.git
cd healthdata-in-motion/backend
./gradlew build -x test
cd ..
docker-compose up -d
./load-demo-data.sh
```

### Daily Demo Usage

**Start Demo** (5 minutes):
```bash
./scripts/gcp-start-demo.sh
```

**Check Status**:
```bash
./scripts/gcp-demo-status.sh
```

**Stop Demo** (2 minutes):
```bash
./scripts/gcp-stop-demo.sh
```

---

## Cost Breakdown

### Monthly Cost Examples

| Usage Pattern | Hours/Month | Compute Cost | Storage | Total |
|--------------|-------------|--------------|---------|-------|
| **2 demos, 4 hours each** | 8 hours | $1.20 | $17 | **~$18** |
| **4 demos, 2 hours each** | 8 hours | $1.20 | $17 | **~$18** |
| **Weekly demos, 8 hours** | 32 hours | $4.80 | $17 | **~$22** |
| **Always running** | 720 hours | $108 | $17 | **~$125** |

**Savings**: Stopping when not in use saves ~$100/month!

---

## Access URLs

Once started, access at:
- **Clinical Portal**: `http://YOUR-IP:4200`
- **API Docs**: `http://YOUR-IP:8087/quality-measure/swagger-ui.html`
- **FHIR API**: `http://YOUR-IP:8083/fhir`

Demo credentials:
- **Username**: `demo@healthdata.com`
- **Password**: `Demo123!`

---

## Scripts Reference

### `gcp-create-demo-vm.sh`
Creates the GCP VM with all prerequisites installed.
- **Run**: Once during initial setup
- **Time**: ~15 minutes
- **Cost**: Starts charging immediately

### `gcp-start-demo.sh`
Starts the stopped VM and all Docker services.
- **Run**: Before each demo
- **Time**: ~5 minutes
- **Cost**: $0.15/hour while running

### `gcp-stop-demo.sh`
Stops all services and the VM (preserves data).
- **Run**: After each demo
- **Time**: ~2 minutes
- **Cost**: $0/hour when stopped (only storage)

### `gcp-demo-status.sh`
Checks VM and service status.
- **Run**: Anytime
- **Shows**: VM state, service health, access URLs

---

## Advanced Usage

### Scheduled Demos

Set up automatic start/stop for weekly demos:

```bash
# Start every Monday at 8:30 AM
gcloud scheduler jobs create http start-demo \
  --schedule="30 8 * * 1" \
  --time-zone="America/New_York" \
  --uri="https://compute.googleapis.com/compute/v1/projects/$PROJECT_ID/zones/$ZONE/instances/healthdata-demo/start" \
  --http-method=POST

# Stop every Monday at 5:00 PM
gcloud scheduler jobs create http stop-demo \
  --schedule="0 17 * * 1" \
  --time-zone="America/New_York" \
  --uri="https://compute.googleapis.com/compute/v1/projects/$PROJECT_ID/zones/$ZONE/instances/healthdata-demo/stop" \
  --http-method=POST
```

### Budget Alerts

Get email alerts when costs exceed thresholds:

```bash
gcloud billing budgets create \
  --billing-account=YOUR-BILLING-ACCOUNT-ID \
  --display-name="HealthData Demo Budget" \
  --budget-amount=50USD \
  --threshold-rule=percent=80 \
  --email-addresses=your-email@example.com
```

### SSH Access

```bash
# Direct SSH
gcloud compute ssh healthdata-demo --zone=us-central1-a

# Run commands remotely
gcloud compute ssh healthdata-demo --zone=us-central1-a --command="docker ps"

# Copy files
gcloud compute scp local-file.txt healthdata-demo:/opt/healthdata/ --zone=us-central1-a
```

---

## Troubleshooting

### VM Won't Start

```bash
# Check status
gcloud compute instances describe healthdata-demo --zone=us-central1-a

# View logs
gcloud compute instances get-serial-port-output healthdata-demo --zone=us-central1-a
```

### Services Not Responding

```bash
# SSH into VM
gcloud compute ssh healthdata-demo --zone=us-central1-a

# Check Docker
docker ps
docker-compose logs -f quality-measure-service

# Restart services
cd /opt/healthdata/healthdata-in-motion
docker-compose restart
```

### Can't Access from Browser

```bash
# Get VM IP
gcloud compute instances describe healthdata-demo \
  --zone=us-central1-a \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)"

# Check firewall
gcloud compute firewall-rules list --filter="name:healthdata"

# Test locally on VM
gcloud compute ssh healthdata-demo --zone=us-central1-a --command="curl http://localhost:4200"
```

---

## Best Practices

### Before Demo

1. ✅ Start VM 30 minutes before demo
2. ✅ Run status check to verify all services UP
3. ✅ Test portal access in browser
4. ✅ Login with demo credentials
5. ✅ Prepare talking points

### After Demo

1. ✅ Stop VM immediately (save costs)
2. ✅ Verify VM status is TERMINATED
3. ✅ Check GCP console for cost tracking

### Monthly Maintenance

1. ✅ Review billing dashboard
2. ✅ Check disk usage (shouldn't exceed 100GB)
3. ✅ Update Docker images if needed
4. ✅ Verify backups/snapshots

---

## Upgrading to Production

When ready for production deployment:

1. **Use Cloud SQL** - Replace Docker PostgreSQL
2. **Use Cloud Run** - Serverless containers
3. **Add Load Balancer** - SSL/TLS termination
4. **Enable Cloud Armor** - DDoS protection
5. **Use Cloud CDN** - Frontend optimization

See `GCP_DEPLOYMENT_GUIDE.md` for production architecture.

---

## FAQ

**Q: Can I use a smaller VM to save money?**
A: Yes! Try `e2-standard-2` (2 vCPU, 8GB RAM) for ~$0.07/hour. Performance may be slower.

**Q: What happens to data when I stop the VM?**
A: All data is preserved on the persistent disk. Services resume exactly where they left off.

**Q: Can I delete the VM entirely between demos?**
A: Yes! Take a snapshot first, then delete VM. Recreate from snapshot when needed. Saves more money but takes longer to start (~10 minutes).

**Q: How do I update the application code?**
A: SSH into VM, pull latest code, rebuild: `git pull && docker-compose build && docker-compose up -d`

**Q: Can I have multiple demo environments?**
A: Yes! Change `VM_NAME` environment variable and run create script again. Each VM costs separately.

**Q: How do I backup my data?**
A: Create snapshot: `gcloud compute disks snapshot healthdata-demo --zone=us-central1-a`

---

## Support

For issues:
1. Check `GCP_DEMO_DEPLOYMENT.md` for detailed documentation
2. Review logs: `docker-compose logs`
3. Check GCP Console for VM status
4. Verify firewall rules are active

---

**Summary**: This demo deployment provides a cost-effective way to maintain a full HealthData In Motion instance that you can start/stop on-demand, saving ~$100/month compared to always-running deployment.


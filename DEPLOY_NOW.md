# Deploy to GCP - Ready to Execute

**Project Updated**: `healthcare-data-in-motion`
**Status**: Scripts updated, authentication required

---

## ✅ Scripts Updated

All GCP scripts have been updated to use project `healthcare-data-in-motion`:
- ✅ `scripts/QUICK_START.sh`
- ✅ `scripts/gcp-create-demo-vm.sh`
- ✅ `scripts/gcp-start-demo.sh`
- ✅ `scripts/gcp-stop-demo.sh`
- ✅ `scripts/gcp-demo-status.sh`

---

## 🔐 Authentication Required

Your GCP authentication tokens have expired. Please authenticate:

```bash
# Step 1: Authenticate with GCP
gcloud auth login

# Step 2: Set the project
gcloud config set project healthcare-data-in-motion

# Step 3: Verify project access
gcloud projects describe healthcare-data-in-motion
```

---

## 🚀 Deploy Commands

After authentication, run ONE of these commands:

### Option 1: Quick Start (Recommended)
```bash
cd /home/webemo-aaron/projects/healthdata-in-motion
./scripts/QUICK_START.sh
```

### Option 2: Direct VM Creation
```bash
cd /home/webemo-aaron/projects/healthdata-in-motion
./scripts/gcp-create-demo-vm.sh
```

---

## 📋 What Will Happen

The deployment will:

1. **Create VM** (~15 minutes)
   - Name: `healthdata-demo`
   - Zone: `us-central1-a`
   - Machine: e2-standard-4 (4 vCPU, 16GB RAM)
   - Disk: 100GB SSD
   - Cost: $0.15/hour (~$110/month if left running)

2. **Install Prerequisites** (automatic)
   - Docker & Docker Compose
   - Java 21
   - Git, curl, unzip

3. **Configure Firewall** (automatic)
   - Ports: 4200, 8081, 8083, 8087
   - Tag: `healthdata-demo`

4. **Ready for Application Deployment**
   - SSH into VM
   - Clone repository
   - Build and start services

---

## 📝 After VM Creation

Once the VM is created, you'll need to:

```bash
# 1. SSH into the VM
gcloud compute ssh healthdata-demo \
  --zone=us-central1-a \
  --project=healthcare-data-in-motion

# 2. Inside VM, deploy application:
cd /opt/healthdata
git clone https://github.com/YOUR_ORG/healthdata-in-motion.git
cd healthdata-in-motion/backend
./gradlew build -x test
cd ..
docker-compose up -d
./load-demo-data.sh

# 3. Verify services
docker-compose ps
curl http://localhost:8087/quality-measure/actuator/health

# 4. Exit SSH
exit

# 5. Get VM IP and test
VM_IP=$(gcloud compute instances describe healthdata-demo \
  --zone=us-central1-a \
  --project=healthcare-data-in-motion \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

echo "Clinical Portal: http://$VM_IP:4200"

# 6. CRITICAL: Stop VM after demo
./scripts/gcp-stop-demo.sh
```

---

## ⚠️ Important Notes

1. **Repository URL**: Update the git clone URL with your actual repository location
2. **Cost Management**: Always stop VM after demos to save ~$100/month
3. **Demo Credentials**:
   - Username: `demo@healthdata.com`
   - Password: `Demo123!`
4. **HIPAA Features**: WebSocket security with 15-min timeout, audit logging, JWT auth

---

## 💰 Cost Summary

| Usage Pattern | VM Hours/Month | Monthly Cost |
|--------------|---------------|--------------|
| 2 demos (4hr each) | 8 hours | ~$20 |
| 4 demos (2hr each) | 8 hours | ~$20 |
| Always running | 720 hours | ~$135 |

**Key**: Stop VM when not demoing to save 85%!

---

## 🆘 Troubleshooting

### Authentication Issues
```bash
# Re-authenticate
gcloud auth login

# Verify account
gcloud auth list

# Check project access
gcloud projects describe healthcare-data-in-motion
```

### Project Not Found
If the project doesn't exist, create it:
```bash
# Create project
gcloud projects create healthcare-data-in-motion \
  --name="Healthcare Data in Motion"

# Link billing (required for VM creation)
gcloud billing accounts list
gcloud billing projects link healthcare-data-in-motion \
  --billing-account=YOUR-BILLING-ACCOUNT-ID

# Enable Compute Engine API
gcloud services enable compute.googleapis.com \
  --project=healthcare-data-in-motion
```

### VM Creation Fails
```bash
# Check quotas
gcloud compute project-info describe \
  --project=healthcare-data-in-motion

# Check available zones
gcloud compute zones list --filter="region:us-central1"
```

---

## ✅ Quick Checklist

Before running deployment:
- [ ] Run `gcloud auth login`
- [ ] Set project: `gcloud config set project healthcare-data-in-motion`
- [ ] Verify project exists: `gcloud projects describe healthcare-data-in-motion`
- [ ] Check billing enabled: `gcloud billing projects describe healthcare-data-in-motion`
- [ ] Run deployment: `./scripts/QUICK_START.sh`

---

## 📚 Documentation

- **This Guide**: `DEPLOY_NOW.md` - Quick deployment reference
- **Comprehensive Guide**: `DEPLOY_TO_GCP.md` - Detailed instructions
- **Technical Details**: `GCP_DEMO_DEPLOYMENT.md` - Architecture
- **Daily Operations**: `GCP_DEMO_README.md` - Start/stop guide
- **Security**: `WEBSOCKET_HIPAA_COMPLIANCE_COMPLETE.md` - HIPAA implementation

---

**Project**: healthcare-data-in-motion
**Status**: Ready to deploy (authentication required)
**Next Step**: Run `gcloud auth login`

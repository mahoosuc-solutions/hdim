#!/bin/bash
set -e

# HealthData Demo VM Creation Script
# Creates a GCP VM configured for demo use with easy start/stop

# Configuration
PROJECT_ID=${PROJECT_ID:-"healthcare-data-in-motion"}
REGION=${REGION:-"us-central1"}
ZONE=${ZONE:-"us-central1-a"}
VM_NAME=${VM_NAME:-"healthdata-demo"}
MACHINE_TYPE=${MACHINE_TYPE:-"e2-standard-4"}

echo "================================================"
echo "  Creating HealthData Demo VM"
echo "================================================"
echo ""
echo "Project:      $PROJECT_ID"
echo "Zone:         $ZONE"
echo "VM Name:      $VM_NAME"
echo "Machine Type: $MACHINE_TYPE (4 vCPU, 16 GB RAM)"
echo "Disk:         100 GB SSD"
echo ""

read -p "Continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 1
fi

echo ""
echo "Step 1/4: Checking GCP prerequisites..."

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo "❌ ERROR: gcloud CLI not found"
    echo "Install from: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Check if logged in
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" &> /dev/null; then
    echo "❌ ERROR: Not logged into gcloud"
    echo "Run: gcloud auth login"
    exit 1
fi

# Check if project exists
if ! gcloud projects describe $PROJECT_ID &> /dev/null; then
    echo "❌ ERROR: Project $PROJECT_ID not found"
    echo "Create project first or set PROJECT_ID environment variable"
    exit 1
fi

# Set project
gcloud config set project $PROJECT_ID

echo "✅ Prerequisites OK"
echo ""

echo "Step 2/4: Enabling required APIs..."
gcloud services enable compute.googleapis.com --quiet
gcloud services enable cloudscheduler.googleapis.com --quiet
echo "✅ APIs enabled"
echo ""

echo "Step 3/4: Creating VM instance..."

# Check if VM already exists
if gcloud compute instances describe $VM_NAME --zone=$ZONE &>/dev/null; then
    echo "⚠️  VM $VM_NAME already exists!"
    read -p "Delete and recreate? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Deleting existing VM..."
        gcloud compute instances delete $VM_NAME --zone=$ZONE --quiet
    else
        echo "Cancelled."
        exit 1
    fi
fi

# Create VM with startup script
gcloud compute instances create $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --machine-type=$MACHINE_TYPE \
  --image-family=ubuntu-2204-lts \
  --image-project=ubuntu-os-cloud \
  --boot-disk-size=100GB \
  --boot-disk-type=pd-ssd \
  --network-tier=STANDARD \
  --tags=healthdata-demo,http-server \
  --metadata=startup-script='#!/bin/bash
# Log startup
echo "Starting HealthData VM setup..." > /var/log/healthdata-setup.log
date >> /var/log/healthdata-setup.log

# Update system
apt-get update >> /var/log/healthdata-setup.log 2>&1

# Install Docker
echo "Installing Docker..." >> /var/log/healthdata-setup.log
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh >> /var/log/healthdata-setup.log 2>&1

# Add user to docker group
usermod -aG docker ubuntu
usermod -aG docker $SUDO_USER 2>/dev/null || true

# Install Docker Compose
echo "Installing Docker Compose..." >> /var/log/healthdata-setup.log
DOCKER_COMPOSE_VERSION=$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep "tag_name" | cut -d\" -f4)
curl -L "https://github.com/docker/compose/releases/download/${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose >> /var/log/healthdata-setup.log 2>&1
chmod +x /usr/local/bin/docker-compose

# Install utilities
echo "Installing utilities..." >> /var/log/healthdata-setup.log
apt-get install -y git curl wget jq openjdk-21-jdk-headless >> /var/log/healthdata-setup.log 2>&1

# Create app directory
mkdir -p /opt/healthdata
chown -R ubuntu:ubuntu /opt/healthdata

# Set JAVA_HOME
echo "export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64" >> /etc/environment
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> /etc/environment

# Mark setup complete
echo "Setup complete" > /opt/healthdata/setup-complete.txt
date >> /opt/healthdata/setup-complete.txt
echo "✅ VM setup completed successfully" >> /var/log/healthdata-setup.log
'

echo "✅ VM created"
echo ""

echo "Step 4/4: Configuring firewall rules..."

# Create or update firewall rule
if gcloud compute firewall-rules describe healthdata-allow-demo --project=$PROJECT_ID &>/dev/null; then
    echo "Firewall rule already exists, updating..."
    gcloud compute firewall-rules update healthdata-allow-demo \
      --project=$PROJECT_ID \
      --allow=tcp:4200,tcp:8081,tcp:8083,tcp:8087 \
      --target-tags=healthdata-demo
else
    echo "Creating firewall rule..."
    gcloud compute firewall-rules create healthdata-allow-demo \
      --project=$PROJECT_ID \
      --allow=tcp:4200,tcp:8081,tcp:8083,tcp:8087 \
      --target-tags=healthdata-demo \
      --description="Allow HealthData demo application ports"
fi

echo "✅ Firewall configured"
echo ""

# Wait for startup script to complete
echo "⏳ Waiting for VM startup script to complete (this takes ~3 minutes)..."
echo "   Installing Docker, Docker Compose, Java, and utilities..."
sleep 180

# Get VM IP
VM_IP=$(gcloud compute instances describe $VM_NAME \
  --project=$PROJECT_ID \
  --zone=$ZONE \
  --format="get(networkInterfaces[0].accessConfigs[0].natIP)")

echo ""
echo "================================================"
echo "  ✅ VM Created Successfully!"
echo "================================================"
echo ""
echo "VM Name:    $VM_NAME"
echo "VM IP:      $VM_IP"
echo "Zone:       $ZONE"
echo "Status:     RUNNING"
echo ""
echo "Next Steps:"
echo "1. SSH into VM:       gcloud compute ssh $VM_NAME --zone=$ZONE"
echo "2. Clone repository:  cd /opt/healthdata && git clone YOUR_REPO_URL"
echo "3. Deploy app:        Run docker-compose up -d"
echo "4. Load demo data:    ./load-demo-data.sh"
echo ""
echo "Or use the automated deployment script:"
echo "  ./scripts/gcp-deploy-app.sh"
echo ""
echo "💰 Current cost: $0.15/hour (~$110/month if left running)"
echo "   Stop when not needed: gcloud compute instances stop $VM_NAME --zone=$ZONE"
echo ""

#!/usr/bin/env bash

# ==============================================================================
# MEDSUPPLY ENTERPRISE PLATFORM - AWS CLOUD INFRASTRUCTURE DEPLOYMENT SCRIPT
# ==============================================================================
# This script automates production infrastructure provisioning and security
# hardening for AWS EC2, S3, RDS PostgreSQL, and CloudFront.
# ==============================================================================

set -eo pipefail

# Configuration Variables
REGION="us-east-1"
ENVIRONMENT="production"
PROJECT="medsupply"
BUCKET_NAME="medsupply-enterprise-storage"
PARAMETER_STORE_PREFIX="/medsupply/prod"

echo "======================================================================"
echo "🚀 Initializing AWS Infrastructure Provisioning for: MedSupply Platform"
echo "Environment: ${ENVIRONMENT} | Region: ${REGION}"
echo "======================================================================"

# 1. AWS S3 BUCKET PROVISIONING & SECURITY HARDENING
echo "📦 Step 1: Provisioning S3 Bucket: ${BUCKET_NAME}..."
if aws s3api head-bucket --bucket "${BUCKET_NAME}" 2>/dev/null; then
    echo "✔ S3 Bucket ${BUCKET_NAME} already exists."
else
    aws s3api create-bucket \
        --bucket "${BUCKET_NAME}" \
        --region "${REGION}" \
        --object-ownership BucketOwnerEnforced
    echo "✔ S3 Bucket created successfully."
fi

# Enable S3 Bucket Versioning for data integrity and recovery
echo "⚙ Enabling S3 Bucket Versioning..."
aws s3api put-bucket-versioning \
    --bucket "${BUCKET_NAME}" \
    --versioning-configuration Status=Enabled

# Enable Default Server-Side Encryption (KMS)
echo "🔒 Enabling AES256 Default Server-Side Encryption..."
aws s3api put-bucket-encryption \
    --bucket "${BUCKET_NAME}" \
    --server-side-encryption-configuration '{
        "Rules": [
            {
                "ApplyServerSideEncryptionByDefault": {
                    "SSEAlgorithm": "aes256"
                }
            }
        ]
    }'

# Block Public Access strictly
echo "🚫 Enforcing S3 Block Public Access..."
aws s3api put-public-access-block \
    --bucket "${BUCKET_NAME}" \
    --public-access-block-configuration '{
        "BlockPublicAcls": true,
        "IgnorePublicAcls": true,
        "BlockPublicPolicy": true,
        "RestrictPublicBuckets": true
    }'

# Set CORS Configuration for Pre-signed URLs on the frontend
echo "🌐 Applying CORS Rules for Secure Upload Handshakes..."
aws s3api put-bucket-cors \
    --bucket "${BUCKET_NAME}" \
    --cors-configuration '{
        "CORSRules": [
            {
                "AllowedHeaders": ["*"],
                "AllowedMethods": ["GET", "PUT", "POST", "HEAD"],
                "AllowedOrigins": ["https://medsupply.com", "https://*.medsupply.com"],
                "ExposeHeaders": ["ETag"],
                "MaxAgeSeconds": 3000
            }
        ]
    }'

# 2. PROVISION SECURE SYSTEMS MANAGER (SSM) PARAMETER STORE CREDENTIALS
echo "🔑 Step 2: Seeding SSM Parameter Store Credentials..."
seed_parameter() {
    local key="$1"
    local value="$2"
    local type="$3"
    echo "Setting Parameter: ${key}..."
    aws ssm put-parameter \
        --name "${PARAMETER_STORE_PREFIX}/${key}" \
        --value "${value}" \
        --type "${type}" \
        --overwrite > /dev/null
}

# Seed dummy production keys for secure parameter initialization (to be rotated in Secret Manager)
seed_parameter "database/url" "jdbc:postgresql://rds-medsupply.c123456789.us-east-1.rds.amazonaws.com:5432/medsupply" "String"
seed_parameter "database/username" "medsupply_admin" "String"
seed_parameter "database/password" "StrongRDSProductionSecurePassword123!" "SecureString"
seed_parameter "redis/host" "redis-cluster.medsupply.cache.amazonaws.com" "String"
seed_parameter "redis/password" "RedisStrongProductionSecurePassword123!" "SecureString"
seed_parameter "jwt/secret" "MedSupplySuperSecretProductionSigningKeyMustBeGreater32BytesKeyLength!" "SecureString"

echo "✔ Parameter Store seeded with infrastructure configuration."

# 3. BOOTSTRAPPING USER DATA FOR EC2 APPLIANCE HOSTS
echo "💻 Step 3: Generating EC2 Appliance User-Data Script..."
cat << 'EOF' > user-data.sh
#!/usr/bin/env bash
set -eo pipefail

echo "========================================"
echo "⚡ Starting System Bootstrap Sequence ⚡"
echo "========================================"

# Update Operating System Security Patches
apt-get update -y
apt-get upgrade -y

# Install Essential Platform Engines: Docker, Curl, AWS-CLI, jq
apt-get install -y apt-transport-https ca-certificates curl software-properties-common gnupg lsb-release jq awscli

# Install Docker CE
mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Create non-privileged service user
useradd -m -s /bin/bash medsupply-operator || true
usermod -aG docker medsupply-operator

# Configure AWS SSM Agent status check
systemctl enable amazon-ssm-agent || true
systemctl start amazon-ssm-agent || true

# Setup log rotation for system engines
cat << 'LOGROT' > /etc/logrotate.d/medsupply
/var/log/medsupply/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0640 medsupply-operator appgroup
}
LOGROT

echo "✔ EC2 Appliance Bootstrapped successfully."
EOF

echo "✔ user-data.sh script written to disk."

echo "======================================================================"
echo "🚀 AWS Infrastructure Deployment Script Execution Complete!"
echo "Ready for production EC2 Auto-Scaling Groups and RDS Master Instantiation."
echo "======================================================================"

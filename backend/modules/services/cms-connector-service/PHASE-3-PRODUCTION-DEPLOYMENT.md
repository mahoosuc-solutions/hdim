# Phase 3: Production Deployment

**Status**: Not Started  
**Duration**: 3 weeks  
**Priority**: 🔴 CRITICAL  
**Team**: DevOps, Backend Engineers  

## Overview

Phase 3 focuses on deploying the CMS Connector Service to production cloud infrastructure. This phase encompasses infrastructure planning, setup, application deployment, and data migration from legacy systems.

## Learning Outcomes

After completing Phase 3, you will:
- Understand cloud infrastructure design and best practices
- Be able to deploy containerized applications to production
- Implement zero-downtime deployment strategies
- Execute complex data migrations
- Set up production monitoring and alerting

## Prerequisites

- ✅ Phase 2 complete (CI/CD pipeline working)
- Cloud account created (AWS, GCP, or Azure)
- Domain name registered
- Team access to cloud console
- Understanding of networking concepts

## Architecture Decision

Before starting, decide on your cloud platform and deployment model:

### Cloud Provider Options

| Provider | Pros | Cons | Best For |
|----------|------|------|----------|
| **AWS** | Largest ecosystem, many services | Most expensive, complex | Enterprise applications |
| **GCP** | Best Kubernetes support, data analytics | Fewer third-party integrations | Data-intensive apps |
| **Azure** | Windows integration, enterprise support | Complex pricing | Microsoft shops |

### Recommended: AWS with ECS or Kubernetes

For this project, we recommend:
- **Compute**: ECS Fargate (simpler) or EKS (Kubernetes, more scalable)
- **Database**: Amazon RDS PostgreSQL
- **Cache**: Amazon ElastiCache Redis
- **Storage**: S3 for backups, CloudFront for CDN
- **Load Balancing**: Application Load Balancer (ALB)

---

## Week 1: Infrastructure Setup

### Day 1-2: Network & Security Foundation

#### Objectives
- Set up VPC with proper network segmentation
- Configure security groups and network ACLs
- Plan for high availability across availability zones

#### Deliverables

1. **Create VPC and Subnets**
```
Infrastructure:
├── VPC (10.0.0.0/16)
│   ├── Public Subnets (2 AZs for ALB)
│   │   ├── us-east-1a: 10.0.1.0/24
│   │   └── us-east-1b: 10.0.2.0/24
│   ├── Private Subnets (2 AZs for application)
│   │   ├── us-east-1a: 10.0.10.0/24
│   │   └── us-east-1b: 10.0.11.0/24
│   └── Database Subnets (2 AZs for RDS)
│       ├── us-east-1a: 10.0.20.0/24
│       └── us-east-1b: 10.0.21.0/24
```

2. **Create Terraform Configuration**
```bash
mkdir -p infrastructure/{terraform,scripts}
cd infrastructure/terraform
```

Create `variables.tf`:
```hcl
variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}
```

Create `vpc.tf`:
```hcl
# VPC
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "${var.environment}-vpc"
    Environment = var.environment
  }
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name        = "${var.environment}-igw"
    Environment = var.environment
  }
}

# Public Subnets
resource "aws_subnet" "public" {
  count                   = length(var.availability_zones)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.${count.index + 1}.0/24"
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name        = "${var.environment}-public-subnet-${count.index + 1}"
    Environment = var.environment
  }
}

# Private Subnets
resource "aws_subnet" "private" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 10}.0/24"
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name        = "${var.environment}-private-subnet-${count.index + 1}"
    Environment = var.environment
  }
}

# Database Subnets
resource "aws_subnet" "database" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.${count.index + 20}.0/24"
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name        = "${var.environment}-database-subnet-${count.index + 1}"
    Environment = var.environment
  }
}

# NAT Gateway (for private subnet internet access)
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name        = "${var.environment}-nat-eip"
    Environment = var.environment
  }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name        = "${var.environment}-nat"
    Environment = var.environment
  }

  depends_on = [aws_internet_gateway.main]
}

# Route Tables
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block      = "0.0.0.0/0"
    gateway_id      = aws_internet_gateway.main.id
  }

  tags = {
    Name        = "${var.environment}-public-rt"
    Environment = var.environment
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name        = "${var.environment}-private-rt"
    Environment = var.environment
  }
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}
```

Create `security.tf`:
```hcl
# Security Group for ALB
resource "aws_security_group" "alb" {
  name        = "${var.environment}-alb-sg"
  description = "Security group for ALB"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.environment}-alb-sg"
    Environment = var.environment
  }
}

# Security Group for ECS
resource "aws_security_group" "ecs" {
  name        = "${var.environment}-ecs-sg"
  description = "Security group for ECS tasks"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.environment}-ecs-sg"
    Environment = var.environment
  }
}

# Security Group for RDS
resource "aws_security_group" "rds" {
  name        = "${var.environment}-rds-sg"
  description = "Security group for RDS"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.environment}-rds-sg"
    Environment = var.environment
  }
}

# Security Group for Redis
resource "aws_security_group" "redis" {
  name        = "${var.environment}-redis-sg"
  description = "Security group for ElastiCache Redis"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.environment}-redis-sg"
    Environment = var.environment
  }
}
```

#### Time Estimate: 1-2 days
#### Checklist
- [ ] VPC created with proper CIDR blocks
- [ ] Subnets created across multiple AZs
- [ ] Internet Gateway created and attached
- [ ] NAT Gateway created
- [ ] Route tables created and associated
- [ ] Security groups created for ALB, ECS, RDS, Redis

---

### Day 3: Database Setup

#### Objectives
- Set up production PostgreSQL database with RDS
- Configure database backups and replication
- Set up database user accounts and permissions

#### Deliverables

Create `databases.tf`:
```hcl
# RDS Database Subnet Group
resource "aws_db_subnet_group" "main" {
  name       = "${var.environment}-db-subnet-group"
  subnet_ids = aws_subnet.database[*].id

  tags = {
    Name        = "${var.environment}-db-subnet-group"
    Environment = var.environment
  }
}

# RDS PostgreSQL Instance
resource "aws_db_instance" "main" {
  identifier     = "${var.environment}-cms-db"
  engine         = "postgres"
  engine_version = "15.3"
  instance_class = "db.t3.medium"  # Change based on size needs

  # Database configuration
  db_name  = "cms_production"
  username = "cms_admin"
  password = var.db_password  # Store in secrets manager!

  # Storage
  allocated_storage      = 100
  storage_type           = "gp3"
  storage_encrypted      = true
  iops                   = 3000
  storage_throughput     = 125

  # Availability
  multi_az               = true
  db_subnet_group_name   = aws_db_subnet_group.main.name
  publicly_accessible    = false
  vpc_security_group_ids = [aws_security_group.rds.id]

  # Backups
  backup_retention_period = 30
  backup_window           = "03:00-04:00"
  copy_tags_to_snapshot   = true

  # Maintenance
  maintenance_window      = "mon:04:00-mon:05:00"
  auto_minor_version_upgrade = true

  # Monitoring
  enabled_cloudwatch_logs_exports = ["postgresql"]
  monitoring_interval             = 60
  monitoring_role_arn            = aws_iam_role.rds_monitoring.arn

  # Deletion protection
  deletion_protection = true

  tags = {
    Name        = "${var.environment}-cms-db"
    Environment = var.environment
  }
}

# IAM role for RDS monitoring
resource "aws_iam_role" "rds_monitoring" {
  name = "${var.environment}-rds-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "monitoring.rds.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}
```

Create `variables-secrets.tf`:
```hcl
variable "db_password" {
  description = "RDS database password"
  type        = string
  sensitive   = true
}

# Usage: terraform apply -var="db_password=YourSecurePassword123!"
# Better: Use AWS Secrets Manager or terraform.tfvars (gitignored)
```

#### Time Estimate: 1 day
#### Checklist
- [ ] RDS instance created with Multi-AZ
- [ ] Database password stored securely
- [ ] Backups configured for 30-day retention
- [ ] Monitoring enabled
- [ ] Database accessible from ECS security group

---

### Day 4-5: Caching & CDN Setup

#### Objectives
- Set up Redis caching layer
- Configure CDN for static assets
- Set up SSL/TLS certificates

#### Deliverables

Create `caching.tf`:
```hcl
# ElastiCache Subnet Group
resource "aws_elasticache_subnet_group" "main" {
  name       = "${var.environment}-cache-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name        = "${var.environment}-cache-subnet-group"
    Environment = var.environment
  }
}

# ElastiCache Redis Cluster
resource "aws_elasticache_cluster" "main" {
  cluster_id           = "${var.environment}-redis"
  engine               = "redis"
  node_type            = "cache.t3.medium"
  num_cache_nodes      = 2
  parameter_group_name = "default.redis7"
  engine_version       = "7.0"
  port                 = 6379

  # High availability
  automatic_failover_enabled   = true
  multi_az_enabled             = true
  at_rest_encryption_enabled   = true
  transit_encryption_enabled   = true
  auth_token_enabled           = true
  auth_token                   = var.redis_auth_token

  # Backup & maintenance
  snapshot_retention_limit = 5
  snapshot_window          = "03:00-05:00"
  maintenance_window       = "sun:04:00-sun:05:00"

  # Networking
  subnet_group_name      = aws_elasticache_subnet_group.main.name
  security_group_ids     = [aws_security_group.redis.id]

  # Monitoring
  log_delivery_configuration {
    destination      = aws_cloudwatch_log_group.redis_slow_log.name
    destination_type = "cloudwatch-logs"
    log_format       = "json"
    log_type         = "slow-log"
  }

  tags = {
    Name        = "${var.environment}-redis"
    Environment = var.environment
  }
}

# CloudWatch Logs for Redis
resource "aws_cloudwatch_log_group" "redis_slow_log" {
  name              = "/aws/elasticache/redis/slow-log"
  retention_in_days = 7

  tags = {
    Name        = "${var.environment}-redis-logs"
    Environment = var.environment
  }
}
```

Create `cdn.tf`:
```hcl
# S3 bucket for static assets
resource "aws_s3_bucket" "assets" {
  bucket = "${var.environment}-cms-assets-${data.aws_caller_identity.current.account_id}"

  tags = {
    Name        = "${var.environment}-assets"
    Environment = var.environment
  }
}

# Block public access (CloudFront will handle it)
resource "aws_s3_bucket_public_access_block" "assets" {
  bucket = aws_s3_bucket.assets.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CloudFront Distribution
resource "aws_cloudfront_distribution" "assets" {
  origin {
    domain_name = aws_s3_bucket.assets.bucket_regional_domain_name
    origin_id   = "S3Assets"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.assets.cloudfront_access_identity_path
    }
  }

  enabled         = true
  is_ipv6_enabled = true
  default_root_object = "index.html"

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3Assets"

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }

  tags = {
    Name        = "${var.environment}-cdn"
    Environment = var.environment
  }
}

# CloudFront Origin Access Identity
resource "aws_cloudfront_origin_access_identity" "assets" {
  comment = "${var.environment} OAI"
}

# S3 bucket policy for CloudFront access
resource "aws_s3_bucket_policy" "assets" {
  bucket = aws_s3_bucket.assets.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid    = "CloudFrontAccess"
      Effect = "Allow"
      Principal = {
        AWS = aws_cloudfront_origin_access_identity.assets.iam_arn
      }
      Action   = "s3:GetObject"
      Resource = "${aws_s3_bucket.assets.arn}/*"
    }]
  })
}
```

#### Time Estimate: 1.5 days
#### Checklist
- [ ] Redis cluster created with Multi-AZ
- [ ] Redis encryption and auth token configured
- [ ] S3 bucket created for assets
- [ ] CloudFront distribution created
- [ ] S3 bucket policy configured for CloudFront access

---

### Day 5: SSL/TLS and Domain Configuration

#### Deliverables

Create `ssl.tf`:
```hcl
# ACM Certificate
resource "aws_acm_certificate" "main" {
  domain_name       = var.domain_name
  validation_method = "DNS"

  subject_alternative_names = [
    "*.${var.domain_name}"
  ]

  tags = {
    Name        = "${var.environment}-cert"
    Environment = var.environment
  }

  lifecycle {
    create_before_destroy = true
  }
}

# Route 53 DNS records for certificate validation
resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.main.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = aws_route53_zone.main.zone_id
}

# Wait for certificate validation
resource "aws_acm_certificate_validation" "main" {
  certificate_arn           = aws_acm_certificate.main.arn
  timeouts {
    create = "5m"
  }

  depends_on = [aws_route53_record.cert_validation]
}

# Route 53 Zone
resource "aws_route53_zone" "main" {
  name = var.domain_name

  tags = {
    Name        = "${var.environment}-zone"
    Environment = var.environment
  }
}
```

#### Time Estimate: 0.5 days
#### Checklist
- [ ] ACM certificate requested
- [ ] Route 53 zone created
- [ ] DNS validation records created
- [ ] Certificate validated

---

### Summary: Week 1

By end of Week 1, you should have:
- ✅ VPC with proper network segmentation
- ✅ Security groups for all components
- ✅ RDS PostgreSQL database
- ✅ ElastiCache Redis cluster
- ✅ S3 + CloudFront CDN
- ✅ ACM certificate and Route 53 zone

**Files Created**:
- `infrastructure/terraform/variables.tf`
- `infrastructure/terraform/vpc.tf`
- `infrastructure/terraform/security.tf`
- `infrastructure/terraform/databases.tf`
- `infrastructure/terraform/caching.tf`
- `infrastructure/terraform/cdn.tf`
- `infrastructure/terraform/ssl.tf`
- `infrastructure/terraform/terraform.tfvars` (gitignored)

**Cost this week**: ~$500-800 (just infrastructure, not fully optimized)

---

## Week 2: Application Deployment

[Full Week 2 content continues with ECS/Kubernetes setup, ALB configuration, CI/CD integration...]

## Week 3: Data Migration & Cutover

[Full Week 3 content continues with migration strategy, validation, go-live procedures...]

## Success Criteria

- [ ] Application running in production
- [ ] All data successfully migrated
- [ ] Zero-downtime deployments working
- [ ] Backups and recovery tested
- [ ] Monitoring and alerting in place
- [ ] SLA being met (99.9% uptime)

---

## Key Files to Create

```
infrastructure/
├── terraform/
│   ├── main.tf
│   ├── variables.tf
│   ├── vpc.tf
│   ├── security.tf
│   ├── databases.tf
│   ├── caching.tf
│   ├── cdn.tf
│   ├── ssl.tf
│   ├── compute.tf (Week 2)
│   ├── load-balancer.tf (Week 2)
│   ├── iam.tf
│   ├── outputs.tf
│   └── terraform.tfvars (gitignored)
├── scripts/
│   ├── deploy.sh
│   ├── rollback.sh
│   └── validate.sh
└── docs/
    ├── architecture.md
    ├── deployment-guide.md
    └── troubleshooting.md
```

## Next Steps

1. Choose cloud provider and create account
2. Register domain name
3. Set up Terraform backend (S3 + DynamoDB for state)
4. Begin Week 1 infrastructure setup
5. Schedule Phase 4 (Monitoring) planning

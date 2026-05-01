
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name prefix used for resource naming"
  type        = string
  default     = "all-in-market"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "Two public subnet CIDRs for ALB"
  type        = list(string)
  default     = ["10.0.0.0/20", "10.0.16.0/20"]
}

variable "ecs_private_subnet_cidrs" {
  description = "Two private subnet CIDRs for ECS/Fargate"
  type        = list(string)
  default     = ["10.0.128.0/20", "10.0.144.0/20"]
}

variable "data_private_subnet_cidrs" {
  description = "Two private subnet CIDRs for RDS/ElastiCache"
  type        = list(string)
  default     = ["10.0.160.0/20", "10.0.176.0/20"]
}

variable "availability_zones" {
  description = "Two AZs used for this deployment"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2b"]
}

variable "enable_s3_vpc_endpoint" {
  description = "Whether to create an S3 Gateway VPC endpoint. Assumption based on your note about endpoint creation."
  type        = bool
  default     = true
}

variable "db_name" {
  description = "Initial database name"
  type        = string
  default     = "allinmarket"
}

variable "db_username" {
  description = "Master username for RDS"
  type        = string
}

variable "db_password" {
  description = "Master password for RDS"
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB"
  type        = number
  default     = 20
}

variable "redis_node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t4g.micro"
}

variable "log_retention_in_days" {
  description = "CloudWatch log retention days"
  type        = number
  default     = 30
}

variable "task_role_policy_arns" {
  description = "Optional list of policy ARNs to attach to the ECS task role"
  type        = list(string)
  default     = []
}

variable "domain_name" {
  description = "Public domain name for the ALB, for example example.com. Required for dev/prod HTTPS."
  type        = string

  validation {
    condition     = length(trimspace(var.domain_name)) > 0
    error_message = "domain_name은 필수입니다. 예: example.com"
  }
}

variable "route53_zone_name" {
  description = "Public Route53 hosted zone name, for example example.com."
  type        = string

  validation {
    condition     = length(trimspace(var.route53_zone_name)) > 0
    error_message = "route53_zone_name은 필수입니다. 예: example.com"
  }
}

variable "github_owner" {
  description = "GitHub organization or user name"
  type        = string
}

variable "environment" {
  type    = string
  default = "dev"

  validation {
    condition     = contains(["dev", "prod"], var.environment)
    error_message = "environment는 dev 또는 prod만 가능합니다."
  }
}
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

variable "ecr_repository_name" {
  description = "ECR repository name. If null, project_name is used."
  type        = string
  default     = null
}

variable "image_tag" {
  description = "Docker image tag used by ECS task definition"
  type        = string
  default     = "latest"
}

variable "container_name" {
  description = "Container name inside ecs task definition"
  type        = string
  default     = "all-in-market"
}

variable "container_port" {
  description = "Application container port"
  type        = number
  default     = 8080
}

variable "ecs_task_cpu" {
  description = "Fargate task CPU"
  type        = number
  default     = 256
}

variable "ecs_task_memory" {
  description = "Fargate task memory"
  type        = number
  default     = 512
}

variable "ecs_desired_count" {
  description = "Desired ECS task count"
  type        = number
  default     = 1
}

variable "health_check_path" {
  description = "ALB target group health check path"
  type        = string
  default     = "/actuator/health"
}

variable "log_retention_in_days" {
  description = "CloudWatch log retention days"
  type        = number
  default     = 1
}

variable "environment_variables" {
  description = "Additional app environment variables besides DB/REDIS core values"
  type        = map(string)
  default     = {}
}

variable "task_role_policy_arns" {
  description = "Optional list of policy ARNs to attach to the ECS task role"
  type        = list(string)
  default     = []
}

variable "certificate_arn" {
  description = "Optional ACM certificate ARN for HTTPS listener. Leave null to create HTTP only."
  type        = string
  default     = null
}

variable "github_owner" {
  description = "GitHub organization or user name"
  type        = string
}

variable "github_repo" {
  description = "GitHub repository name"
  type        = string
}

variable "github_branch" {
  description = "GitHub branch allowed to deploy"
  type        = string
  default     = "dev"
}

variable "admin_enabled" {
  description = "Whether to create admin EC2 servers behind the ALB"
  type        = bool
  default     = true
}

variable "admin_container_name" {
  description = "Container name for admin ECS task"
  type        = string
  default     = "all-in-market-admin"
}

variable "admin_image_tag" {
  description = "Docker image tag used by admin ECS task"
  type        = string
  default     = "latest"
}

variable "admin_container_port" {
  description = "Admin server container/listening port"
  type        = number
  default     = 8081
}

variable "admin_ecs_task_cpu" {
  description = "CPU units for the admin ECS task running on EC2"
  type        = number
  default     = 512
}

variable "admin_ecs_task_memory" {
  description = "Memory in MiB for the admin ECS task running on EC2"
  type        = number
  default     = 1024
}

variable "admin_ecs_desired_count" {
  description = "Desired task count for the admin ECS service"
  type        = number
  default     = 0
}

variable "admin_ecr_repository_name" {
  description = "ECR repository name for admin server image. If null, project_name-admin is used."
  type        = string
  default     = null
}

variable "admin_health_check_path" {
  description = "ALB health check path for admin target group"
  type        = string
  default     = "/actuator/health"
}

variable "admin_github_repo" {
  description = "GitHub repository name"
  type        = string
}

variable "admin_github_branches" {
  description = "GitHub branch allowed to deploy"
  type        = list(string)
  default     = ["dev", "main"]
}

# CPU spike, 메모리 부족 시, 더 상위 버전으로 교체
variable "admin_instance_type" {
  description = "EC2 instance type for admin server"
  type        = string
  default     = "t4g.small"
}

variable "admin_root_volume_size" {
  description = "Root EBS volume size for admin EC2 in GB"
  type        = number
  default     = 40
}

variable "admin_path_patterns" {
  description = "Path patterns routed to the admin EC2 target group"
  type        = list(string)
  default     = ["/admin", "/admin/*"]
}

variable "admin_listener_rule_priority" {
  description = "HTTPS listener rule priority for admin path routing"
  type        = number
  default     = 10
}

variable "admin_environment_variables" {
  description = "Additional environment variables for the admin server"
  type        = map(string)
  default     = {}
}
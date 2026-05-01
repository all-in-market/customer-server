
variable "customer_container_name" {
  description = "Container name inside ecs task definition"
  type        = string
  default     = "all-in-market-customer"
}

variable "customer_image_tag" {
  description = "Docker image tag used by ECS task definition"
  type        = string
  default     = "latest"
}

variable "customer_container_port" {
  description = "Application container port"
  type        = number
  default     = 8080
}

variable "customer_ecs_task_cpu" {
  description = "Fargate task CPU"
  type        = number
  default     = 256
}

variable "customer_ecs_task_memory" {
  description = "Fargate task memory"
  type        = number
  default     = 512
}

variable "customer_ecs_desired_count" {
  description = "Desired ECS task count"
  type        = number
  default     = 0
}

variable "customer_ecr_repository_name" {
  description = "ECR repository name. If null, project_name is used."
  type        = string
  default     = null
}

variable "customer_health_check_path" {
  description = "ALB target group health check path"
  type        = string
  default     = "/actuator/health"
}

variable "customer_github_repo" {
  description = "GitHub repository name"
  type        = string
}

variable "customer_github_branches" {
  description = "GitHub branch allowed to deploy"
  type        = list(string)
  default     = ["dev", "main"]
}

variable "customer_environment_variables" {
  description = "Additional app environment variables besides DB/REDIS core values"
  type        = map(string)
  default     = {}
}


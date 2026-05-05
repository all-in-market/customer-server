variable "chat_container_name" {
  type    = string
  default = "all-in-market-chat"
}

variable "chat_container_port" {
  type    = number
  default = 8082
}

variable "chat_ecr_repository_name" {
  type    = string
  default = null
}

variable "chat_image_tag" {
  type    = string
  default = "latest"
}

variable "chat_ecs_task_cpu" {
  type    = number
  default = 512
}

variable "chat_ecs_task_memory" {
  type    = number
  default = 1024
}

variable "chat_ecs_desired_count" {
  type    = number
  default = 0
}

variable "chat_health_check_path" {
  type    = string
  default = "/actuator/health"
}

variable "chat_path_patterns" {
  type    = list(string)
  default = ["/chat", "/chat/*"]
}

variable "chat_listener_rule_priority" {
  type    = number
  default = 30
}

variable "chat_github_repo" {
  type = string
}

variable "chat_github_branches" {
  type    = list(string)
  default = ["dev", "main"]
}

variable "chat_environment_variables" {
  type    = map(string)
  default = {}
}

variable "deepseek_api_key" {
  type      = string
  sensitive = true
}

variable "openai_api_key" {
  type      = string
  sensitive = true
}
variable "alarm_container_name" {
  type    = string
  default = "all-in-market-alarm"
}

variable "alarm_container_port" {
  type    = number
  default = 8083
}

variable "alarm_ecr_repository_name" {
  type    = string
  default = null
}

variable "alarm_image_tag" {
  type    = string
  default = "latest"
}

variable "alarm_ecs_task_cpu" {
  type    = number
  default = 512
}

variable "alarm_ecs_task_memory" {
  type    = number
  default = 1024
}

variable "alarm_ecs_desired_count" {
  type    = number
  default = 0
}

variable "alarm_health_check_path" {
  type    = string
  default = "/actuator/health"
}

variable "alarm_path_patterns" {
  type    = list(string)
  default = ["/ws", "/ws/*", "/internal/notifications/*"]
}

variable "alarm_listener_rule_priority" {
  type    = number
  default = 20
}

variable "alarm_github_repo" {
  type = string
}

variable "alarm_github_branches" {
  type    = list(string)
  default = ["dev", "main"]
}

variable "alarm_environment_variables" {
  type    = map(string)
  default = {}
}
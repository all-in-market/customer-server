variable "grafana_admin_user_name" {
  type    = string
  default = "admin"
}

variable "grafana_admin_given_name" {
  type    = string
  default = "Grafana"
}

variable "grafana_admin_family_name" {
  type    = string
  default = "Admin"
}

variable "grafana_admin_email" {
  type      = string
  sensitive = true
}
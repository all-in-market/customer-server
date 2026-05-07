variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "project_name" {
  type    = string
  default = "all-in-market"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "infra_state_bucket" {
  type = string
}

variable "infra_state_key" {
  type = string
}

variable "infra_state_lock_table" {
  type = string
}

variable "infra_state_kms_key_id" {
  type = string
}
locals {
  common_tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }

  name_prefix = var.project_name

  log_group_name = "/ecs/${var.project_name}"

  ecr_repository_name = coalesce(var.ecr_repository_name, var.project_name)
  container_image     = "${aws_ecr_repository.app.repository_url}:${var.image_tag}"

  merged_environment_variables = merge(
    var.environment_variables,
    {
      SPRING_PROFILES_ACTIVE = "prod"

      DB_HOST    = aws_db_instance.this.address
      DB_PORT    = "5432"
      DB_NAME    = var.db_name
      DB_USERNAME = var.db_username
      DB_PASSWORD = var.db_password
      REDIS_HOST = aws_elasticache_replication_group.this.primary_endpoint_address
      REDIS_PORT = "6379"
    }
  )

  container_environment = [
    for key, value in local.merged_environment_variables : {
      name  = key
      value = value
    }
  ]
}

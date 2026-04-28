locals {
  common_tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }

  name_prefix = var.project_name

  log_group_name = "/ecs/${var.project_name}"

  ecr_repository_name = coalesce(var.ecr_repository_name, var.project_name)
  container_image     = "${aws_ecr_repository.app.repository_url}:${var.image_tag}"

  # 프로파일별 고가용성을 위한 정책 선택
  ecs = {
    desired_count = var.environment == "prod" ? 2 : 1
  }

  rds = {
    multi_az = var.environment == "prod"
    deletion_protection = var.environment == "prod"
    skip_final_snapshot = var.environment != "prod"
  }

  redis = {
    num_cache_clusters         = var.environment == "prod" ? 2 : 1
    automatic_failover_enabled = var.environment == "prod"
    multi_az_enabled           = var.environment == "prod"
  }


  merged_environment_variables = merge(
    var.environment_variables,
    {
      SPRING_PROFILES_ACTIVE = var.environment == "prod" ? "prod" : "dev"

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

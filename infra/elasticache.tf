resource "aws_elasticache_replication_group" "this" {
  replication_group_id       = lower(replace(local.name_prefix, "_", "-"))
  description                = "Redis for ${var.project_name}"
  engine                     = "redis"
  node_type                  = var.redis_node_type
  port                       = 6379
  parameter_group_name       = "default.redis7"
  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [aws_security_group.redis.id]
  num_cache_clusters         = 1
  automatic_failover_enabled = false
  multi_az_enabled           = false
  at_rest_encryption_enabled = false
  transit_encryption_enabled = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-redis"
  })
}

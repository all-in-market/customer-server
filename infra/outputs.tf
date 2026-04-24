output "vpc_id" {
  value = aws_vpc.this.id
}

output "public_subnet_ids" {
  value = [for subnet in aws_subnet.public : subnet.id]
}

output "ecs_private_subnet_ids" {
  value = [for subnet in aws_subnet.ecs_private : subnet.id]
}

output "data_private_subnet_ids" {
  value = [for subnet in aws_subnet.data_private : subnet.id]
}

output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "target_group_arn" {
  value = aws_lb_target_group.app.arn
}

output "rds_endpoint" {
  value = aws_db_instance.this.address
}

output "redis_host" {
  value = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "ecs_service_name" {
  value = aws_ecs_service.this.name
}

output "cloudwatch_log_group_name" {
  value = aws_cloudwatch_log_group.ecs.name
}

output "github_actions_role_arn" {
  value = aws_iam_role.github_actions.arn
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}
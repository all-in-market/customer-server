resource "aws_db_instance" "this" {
  identifier             = local.name_prefix
  allocated_storage      = var.db_allocated_storage
  db_name                = var.db_name
  engine                 = "postgres"
  instance_class         = var.db_instance_class
  username               = var.db_username
  password               = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  publicly_accessible    = false
  multi_az               = false
  storage_type           = "gp3"

  backup_retention_period = 7
  copy_tags_to_snapshot    = true

  storage_encrypted        = true

  performance_insights_enabled          = true
  performance_insights_retention_period = 7

  auto_minor_version_upgrade = true

  skip_final_snapshot    = true
  deletion_protection    = false

  monitoring_interval    = 0

  tags = merge(local.common_tags, {
    Name = local.name_prefix
  })
}

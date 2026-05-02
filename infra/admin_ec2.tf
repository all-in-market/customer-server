
# 관리자 EC2는 앱을 직접 docker run 하지 않고 ECS Container Instance 역할만 수행
data "aws_ssm_parameter" "admin_ecs_optimized_ami" {
  name = "/aws/service/ecs/optimized-ami/amazon-linux-2023/recommended/image_id"
}

# dev는 관리자 ECS용 EC2 1대, prod는 private subnet별 EC2 생성
# 이 EC2는 관리자 앱을 직접 실행하지 않고 ecs 클러스터에 등록되어 ecs task를 할당받아 앱을 실행한다
resource "aws_instance" "admin" {
  count = var.admin_enabled ? length(local.admin_ec2_subnet_ids) : 0

  ami                         = data.aws_ssm_parameter.admin_ecs_optimized_ami.value
  instance_type               = var.admin_instance_type
  subnet_id                   = local.admin_ec2_subnet_ids[count.index]
  vpc_security_group_ids      = [aws_security_group.admin_ec2.id]
  iam_instance_profile        = aws_iam_instance_profile.admin_ec2.name
  associate_public_ip_address = false

  # user_data 설정이 변경되면 EC2를 새로 만들어서 교체
  user_data_replace_on_change = true
  user_data = templatefile("${path.module}/admin_ec2_user_data.sh.tftpl", {
    ecs_cluster_name = aws_ecs_cluster.this.name
  })

  depends_on = [
    aws_iam_role_policy_attachment.admin_ec2_ecs_container_instance,
    aws_iam_role_policy_attachment.admin_ec2_ssm_managed,
    aws_internet_gateway.this,
    aws_route_table_association.public,
    aws_route_table_association.private,
    aws_nat_gateway.this
  ]

  # EC2 기본 저장소 설정
  root_block_device {
    volume_size           = var.admin_root_volume_size
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = true
  }

  # 메타데이터 접근 설정
  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 2
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-admin-ecs-ec2-${count.index + 1}"
    Role = "admin-ecs-container-instance"
  })
}


# EC2만 해당 Role을 사용할 수 있도록 지정
data "aws_iam_policy_document" "admin_ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "admin_ec2" {
  name               = "${local.name_prefix}-admin-ec2-role"
  assume_role_policy = data.aws_iam_policy_document.admin_ec2_assume_role.json

  tags = local.common_tags
}

# profile에 Role을 저장하여 EC2에 profile을 붙이는 구조
# EC2에 직접 Role을 붙일 수 없기 때문에 이러한 구조 사용
resource "aws_iam_instance_profile" "admin_ec2" {
  name = "${local.name_prefix}-admin-ec2-profile"
  role = aws_iam_role.admin_ec2.name

  tags = local.common_tags
}

# SSH 대신 AWS 콘솔/CLI의 Session Manager로 private EC2에 접속하기 위한 권한
resource "aws_iam_role_policy_attachment" "admin_ec2_ssm_managed" {
  role       = aws_iam_role.admin_ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# EC2가 ECS Container Instance로 클러스터에 등록되고 ECS Agent가 작업을 받을 수 있게 하는 권한
resource "aws_iam_role_policy_attachment" "admin_ec2_ecs_container_instance" {
  role       = aws_iam_role.admin_ec2.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2ContainerServiceforEC2Role"
}

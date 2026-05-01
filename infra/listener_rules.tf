# 예: https://hyu1335.cloud/admin/* 요청을 관리자 ECS Service Target Group으로 전달
# 관리자 서버 전체의 기본 URL prefix를 /admin으로 만들거나, 컨트롤러가 /admin prefix를 처리해야 한다.

# 관리자 서버용 리스너 Rule 생성
resource "aws_lb_listener_rule" "admin_path" {
  count = var.admin_enabled ? 1 : 0

  listener_arn = aws_lb_listener.https.arn
  priority     = var.admin_listener_rule_priority

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.admin[count.index].arn
  }

  condition {
    path_pattern {
      values = var.admin_path_patterns
    }
  }
}
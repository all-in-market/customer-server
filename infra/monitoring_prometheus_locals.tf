locals {
  # ADOT가 어디서 메트릭을 수집하고 어디로 전송할지 정의하는 설정 파일
  customer_adot_config = <<-YAML
receivers:
  prometheus:
    config:
      global:
        scrape_interval: 15s
        scrape_timeout: 10s

        # 모든 메트릭에 붙일 공용 태그 지정
        external_labels:
          environment: ${var.environment}
          service: customer

      scrape_configs:
        - job_name: customer

          # localhost:customer앱포트/actuator/prometheus 경로로 수집
          metrics_path: /actuator/prometheus
          static_configs:
            - targets:
                - 127.0.0.1:${var.customer_container_port}

processors:
  batch:

exporters:
  prometheusremotewrite:
    endpoint: ${aws_prometheus_workspace.app[0].prometheus_endpoint}api/v1/remote_write
    auth:
      authenticator: sigv4auth

# AMP remote_write 요청을 AWS SigV4 방식으로 서명
# 실제 권한은 ECS Task Role에 추가해둔 aps:RemoteWrite 권한을 사용
extensions:
  sigv4auth:
    region: ${var.aws_region}
    service: aps

service:
  extensions: [sigv4auth]
  pipelines:
    metrics:
      receivers: [prometheus]
      processors: [batch]
      exporters: [prometheusremotewrite]
YAML

  # ECS Task 안에서 실행될 ADOT Collector 컨테이너 정의
  customer_adot_container = {
    name      = "adot-collector"
    image     = var.adot_collector_image
    essential = false

    # ADOT가 자신의 설정 yaml을 환경변수(env)를 통해 받도록 설정
    command = [
      "--config=env:ADOT_CONFIG_CONTENT"
    ]

    environment = [
      {
        name  = "ADOT_CONFIG_CONTENT"
        value = local.customer_adot_config
      }
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.adot[0].name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "customer"
      }
    }
  }
}
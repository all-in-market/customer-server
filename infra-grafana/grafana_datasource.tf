# Grafana 내부에 Amazon Managed Prometheus datasource 생성
resource "grafana_data_source" "amp" {
  type       = "prometheus"
  name       = "${var.project_name}-${var.environment}-amp"
  url        = trimsuffix(data.terraform_remote_state.infra.outputs.amp_prometheus_endpoint, "/")
  is_default = true

  json_data_encoded = jsonencode({
    httpMethod = "POST"

    # Amazon Managed Prometheus 조회를 위한 SigV4 인증
    sigV4Auth     = true
    sigV4AuthType = "default"
    sigV4Region   = var.aws_region
  })
}
locals {
  customer_application_dashboard_json = jsondecode(
    replace(
      file("${path.module}/dashboards/customer-application-metrics.json"),
      "$${DS_ALL-IN-MARKET-DEV-AMP}",
      grafana_data_source.amp.uid
    )
  )

  customer_application_dashboard_config = merge(
    local.customer_application_dashboard_json,
    {
      id      = null
      uid     = "${var.project_name}-${var.environment}-customer-app"
      title   = "${var.project_name}-${var.environment} Customer Application Metrics"
      version = 0
    }
  )
}

resource "grafana_dashboard" "customer_application_metrics" {
  config_json = jsonencode(local.customer_application_dashboard_config)
  overwrite   = true

  depends_on = [
    grafana_data_source.amp
  ]
}
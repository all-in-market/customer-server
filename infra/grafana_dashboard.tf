locals {
  customer_application_dashboard_json = try(
    jsondecode(
      replace(
        file("${path.module}/grafana/dashboards/customer-application-metrics.json"),
        "$${DS_ALL-IN-MARKET-DEV-AMP}",
        one(grafana_data_source.amp).uid
      )
    ),
    {}
  )

  customer_application_dashboard_config = merge(
    local.customer_application_dashboard_json,
    {
      id      = null
      uid     = "${local.name_prefix}-${var.environment}-customer-app"
      title   = "${local.name_prefix}-${var.environment} Customer Application Metrics"
      version = 0
    }
  )
}

resource "grafana_dashboard" "customer_application_metrics" {
  count = var.managed_grafana_enabled && var.managed_prometheus_enabled ? 1 : 0

  config_json = jsonencode(local.customer_application_dashboard_config)

  overwrite = true

  depends_on = [
    grafana_data_source.amp
  ]
}
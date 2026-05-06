resource "aws_cloudwatch_dashboard" "main" {
  count = var.monitoring_enabled ? 1 : 0

  dashboard_name = "${local.name_prefix}-${var.environment}-dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        # 타이틀용 텍스트 박스 위젯
        type   = "text"
        x      = 0
        y      = 0
        width  = 24
        height = 2

        properties = {
          markdown = "# ${local.name_prefix} ${var.environment} Monitoring Dashboard\nALB / ECS / RDS / Redis / Application Logs"
        }
      },
      {
        # ALB 요청 횟수
        type   = "metric"
        x      = 0
        y      = 2
        width  = 12
        height = 6

        properties = {
          title   = "ALB Request Count"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            [
              "AWS/ApplicationELB",
              "RequestCount",
              "LoadBalancer",
              aws_lb.this.arn_suffix,
              {
                stat = "Sum"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 2
        width  = 12
        height = 6

        properties = {
          title   = "ALB 5XX / Target 5XX"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = concat(
            [
              [
                "AWS/ApplicationELB",
                "HTTPCode_ELB_5XX_Count",
                "LoadBalancer",
                aws_lb.this.arn_suffix,
                {
                  stat  = "Sum",
                  label = "ALB 5XX"
                }
              ]
            ],
            [
              for service_name, target_group_arn_suffix in local.target_groups : [
                "AWS/ApplicationELB",
                "HTTPCode_Target_5XX_Count",
                "LoadBalancer",
                aws_lb.this.arn_suffix,
                "TargetGroup",
                target_group_arn_suffix,
                {
                  stat  = "Sum",
                  label = "${service_name} Target 5XX"
                }
              ]
            ]
          )
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 8
        width  = 12
        height = 6

        properties = {
          title   = "Target Response Time p95"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            for service_name, target_group_arn_suffix in local.target_groups : [
              "AWS/ApplicationELB",
              "TargetResponseTime",
              "LoadBalancer",
              aws_lb.this.arn_suffix,
              "TargetGroup",
              target_group_arn_suffix,
              {
                stat  = "p95",
                label = "${service_name} p95"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 8
        width  = 12
        height = 6

        properties = {
          title   = "Unhealthy Target Count"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 60

          metrics = [
            for service_name, target_group_arn_suffix in local.target_groups : [
              "AWS/ApplicationELB",
              "UnHealthyHostCount",
              "LoadBalancer",
              aws_lb.this.arn_suffix,
              "TargetGroup",
              target_group_arn_suffix,
              {
                stat  = "Maximum",
                label = "${service_name} unhealthy"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 14
        width  = 12
        height = 6

        properties = {
          title   = "ECS CPU Utilization"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            for service_name, ecs_service_name in local.ecs_services : [
              "AWS/ECS",
              "CPUUtilization",
              "ClusterName",
              aws_ecs_cluster.this.name,
              "ServiceName",
              ecs_service_name,
              {
                stat  = "Average",
                label = "${service_name} CPU"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 14
        width  = 12
        height = 6

        properties = {
          title   = "ECS Memory Utilization"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            for service_name, ecs_service_name in local.ecs_services : [
              "AWS/ECS",
              "MemoryUtilization",
              "ClusterName",
              aws_ecs_cluster.this.name,
              "ServiceName",
              ecs_service_name,
              {
                stat  = "Average",
                label = "${service_name} Memory"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 20
        width  = 12
        height = 6

        properties = {
          title   = "RDS CPU / Connections"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            [
              "AWS/RDS",
              "CPUUtilization",
              "DBInstanceIdentifier",
              aws_db_instance.this.identifier,
              {
                stat  = "Average",
                label = "RDS CPU"
              }
            ],
            [
              ".",
              "DatabaseConnections",
              ".",
              ".",
              {
                stat  = "Average",
                label = "DB Connections"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 20
        width  = 12
        height = 6

        properties = {
          title   = "RDS Free Storage"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            [
              "AWS/RDS",
              "FreeStorageSpace",
              "DBInstanceIdentifier",
              aws_db_instance.this.identifier,
              {
                stat  = "Average",
                label = "Free Storage",
                yAxis = "left"
              }
            ]
          ]

          yAxis = {
            left = {
              label = "Bytes"
            }
          }
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 26
        width  = 12
        height = 6

        properties = {
          title   = "Redis CPU / Memory"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            [
              "AWS/ElastiCache",
              "CPUUtilization",
              "ReplicationGroupId",
              aws_elasticache_replication_group.this.replication_group_id,
              {
                stat  = "Average",
                label = "Redis CPU"
              }
            ],
            [
              ".",
              "DatabaseMemoryUsagePercentage",
              ".",
              ".",
              {
                stat  = "Average",
                label = "Redis Memory"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 26
        width  = 12
        height = 6

        properties = {
          title   = "Redis Evictions / Connections"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            [
              "AWS/ElastiCache",
              "Evictions",
              "ReplicationGroupId",
              aws_elasticache_replication_group.this.replication_group_id,
              {
                stat  = "Sum",
                label = "Evictions"
              }
            ],
            [
              ".",
              "CurrConnections",
              ".",
              ".",
              {
                stat  = "Average",
                label = "Current Connections"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 32
        width  = 24
        height = 6

        properties = {
          title   = "Application ERROR Logs"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 300

          metrics = [
            for service_name, log_group_name in local.log_groups : [
              "${local.name_prefix}/${var.environment}/ApplicationLogs",
              "${service_name}-ErrorLogCount",
              {
                stat  = "Sum",
                label = "${service_name} ERROR"
              }
            ]
          ]
        }
      },
      {
        type   = "text"
        x      = 0
        y      = 38
        width  = 24
        height = 2

        properties = {
          markdown = "## Application Metrics - Micrometer / Actuator"
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 40
        width  = 12
        height = 6

        properties = {
          title   = "Customer HTTP Requests"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 60

          metrics = [
            [
              {
                expression = "SEARCH('{${local.application_metrics_namespace},service} MetricName=\"http.server.requests.count\" service=\"customer\"', 'Sum', 60)"
                label      = "customer requests"
                id         = "e1"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 40
        width  = 12
        height = 6

        properties = {
          title   = "Customer HTTP 5xx"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 60

          metrics = [
            [
              {
                expression = "SEARCH('{${local.application_metrics_namespace},service,status} MetricName=\"http.server.requests.count\" service=\"customer\" status=\"500\"', 'Sum', 60)"
                label      = "customer 500"
                id         = "e1"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 46
        width  = 12
        height = 6

        properties = {
          title   = "Customer JVM Heap Used"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 60

          metrics = [
            [
              {
                expression = "SEARCH('{${local.application_metrics_namespace},service,area} MetricName=\"jvm.memory.used\" service=\"customer\" area=\"heap\"', 'Average', 60)"
                label      = "heap used"
                id         = "e1"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 46
        width  = 12
        height = 6

        properties = {
          title   = "Customer HikariCP Active Connections"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 60

          metrics = [
            [
              {
                expression = "SEARCH('{${local.application_metrics_namespace},service} MetricName=\"hikaricp.connections.active\" service=\"customer\"', 'Average', 60)"
                label      = "active connections"
                id         = "e1"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 52
        width  = 12
        height = 6

        properties = {
          title   = "Customer Tomcat Busy Threads"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 60

          metrics = [
            [
              {
                expression = "SEARCH('{${local.application_metrics_namespace},service} MetricName=\"tomcat.threads.busy\" service=\"customer\"', 'Average', 60)"
                label      = "busy threads"
                id         = "e1"
              }
            ]
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 52
        width  = 12
        height = 6

        properties = {
          title   = "Customer Tomcat Threads"
          region  = var.aws_region
          view    = "timeSeries"
          stacked = false
          period  = 60

          metrics = [
            [
              {
                expression = "SEARCH('{${local.application_metrics_namespace},service} MetricName=\"tomcat.threads.current\" service=\"customer\"', 'Average', 60)"
                label      = "current threads"
                id         = "e1"
              }
            ],
            [
              {
                expression = "SEARCH('{${local.application_metrics_namespace},service} MetricName=\"tomcat.threads.config.max\" service=\"customer\"', 'Maximum', 60)"
                label      = "max threads"
                id         = "e2"
              }
            ]
          ]
        }
      }
    ]
  })
}
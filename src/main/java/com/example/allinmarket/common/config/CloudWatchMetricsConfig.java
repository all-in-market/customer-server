package com.example.allinmarket.common.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.time.Duration;

@Configuration
public class CloudWatchMetricsConfig {

    @Value("${management.cloudwatch.metrics.namespace:all-in-market/ApplicationMetrics}")
    private String cloudWatchNamespace;

    @Bean
    @ConditionalOnProperty(
            name = "management.cloudwatch.metrics.export.enabled",
            havingValue = "true"
    )
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.create();
    }

    @Bean
    @ConditionalOnProperty(
            name = "management.cloudwatch.metrics.export.enabled",
            havingValue = "true"
    )
    public MeterRegistry cloudWatchMeterRegistry(
            CloudWatchAsyncClient cloudWatchAsyncClient
    ) {
        CloudWatchConfig config = new CloudWatchConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String namespace() {
                return cloudWatchNamespace;
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(60);
            }

            @Override
            public int batchSize() {
                return 20;
            }
        };

        return new CloudWatchMeterRegistry(
                config,
                Clock.SYSTEM,
                cloudWatchAsyncClient
        );
    }
}
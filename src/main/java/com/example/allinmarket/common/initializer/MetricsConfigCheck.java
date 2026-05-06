package com.example.allinmarket.common.initializer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsConfigCheck {

    private final Environment environment;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void check() {
        log.info("CLOUDWATCH_METRICS_ENABLED={}",
                environment.getProperty("CLOUDWATCH_METRICS_ENABLED"));

        log.info("CLOUDWATCH_METRICS_NAMESPACE={}",
                environment.getProperty("CLOUDWATCH_METRICS_NAMESPACE"));

        log.info("management.cloudwatch.metrics.export.namespace={}",
                environment.getProperty("management.cloudwatch.metrics.export.namespace"));

        log.info("management.metrics.export.cloudwatch.namespace={}",
                environment.getProperty("management.metrics.export.cloudwatch.namespace"));

        log.info("spring.cloud.aws.cloudwatch.enabled={}",
                environment.getProperty("spring.cloud.aws.cloudwatch.enabled"));
    }

    @PostConstruct
    public void checkRegistries() {
        log.info("MAIN REGISTRY = {}",
                meterRegistry.getClass().getName());

        if (meterRegistry instanceof CompositeMeterRegistry composite) {

            composite.getRegistries().forEach(registry ->
                    log.info("SUB REGISTRY = {}",
                            registry.getClass().getName()));
        }
    }

    @Bean
    ApplicationRunner runner(MeterRegistry registry) {
        return args -> {
            registry.counter("test.counter").increment();
        };
    }
}
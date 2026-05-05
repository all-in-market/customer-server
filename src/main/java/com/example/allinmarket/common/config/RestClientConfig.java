package com.example.allinmarket.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(
                (int) Duration.ofSeconds(3).toMillis()
        );
        factory.setReadTimeout(
                (int) Duration.ofSeconds(5).toMillis()
        );

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
package com.mic.smsmiddleware.config;

import com.mic.smsmiddleware.properties.AppProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate smsRestTemplate(RestTemplateBuilder builder, AppProperties appProperties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(appProperties.getProvider().getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(appProperties.getProvider().getReadTimeout()))
                .build();
    }
}

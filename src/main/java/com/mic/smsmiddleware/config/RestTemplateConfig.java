package com.mic.smsmiddleware.config;

import com.mic.smsmiddleware.properties.AppProperties;
import com.mic.smsmiddleware.properties.SmsProviderProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate smsRestTemplate(RestTemplateBuilder builder, AppProperties appProperties) {
        SmsProviderProperties provider = appProperties.getProvider();
        return builder
                .setConnectTimeout(Duration.ofMillis(provider.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(provider.getReadTimeout()))
                .basicAuthentication(provider.getUsername(), provider.getPassword())
                .build();
    }
}

package com.mic.smsmiddleware.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "mic")
@Getter
@Setter
public class AppProperties {

    private SmsProviderProperties provider = new SmsProviderProperties();
    private SchedulingProperties scheduling = new SchedulingProperties();
    private Map<String, BusinessTypeConfig> businessTypes = new LinkedHashMap<>();
}

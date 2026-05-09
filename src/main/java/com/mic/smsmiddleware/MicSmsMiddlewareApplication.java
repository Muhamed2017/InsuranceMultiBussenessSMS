package com.mic.smsmiddleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MicSmsMiddlewareApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicSmsMiddlewareApplication.class, args);
    }
}

package com.mic.smsmiddleware.scheduler;

import com.mic.smsmiddleware.properties.AppProperties;
import com.mic.smsmiddleware.service.BusinessProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessProcessingScheduler implements SchedulingConfigurer {

    private final AppProperties appProperties;
    private final BusinessProcessorService businessProcessorService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        appProperties.getBusinessTypes().forEach((type, config) -> {
            if (!StringUtils.hasText(config.getCron())) {
                log.warn("Business type '{}' has no cron defined — skipping scheduling", type);
                return;
            }
            log.info("Scheduling business type '{}' with cron: {}", type, config.getCron());
            registrar.addCronTask(() -> runType(type), config.getCron());
        });
    }

    private void runType(String businessType) {
        log.info("Scheduled run starting for '{}'", businessType);
        try {
            businessProcessorService.process(businessType);
        } catch (Exception ex) {
            log.error("Scheduled run failed for '{}'", businessType, ex);
        }
    }
}

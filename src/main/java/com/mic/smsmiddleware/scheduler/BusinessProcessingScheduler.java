package com.mic.smsmiddleware.scheduler;

import com.mic.smsmiddleware.properties.AppProperties;
import com.mic.smsmiddleware.service.BusinessProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BusinessProcessingScheduler {

    private final AppProperties appProperties;
    private final BusinessProcessorService businessProcessorService;

    @Scheduled(cron = "${mic.scheduling.processing-cron:0 0 10-23 * * *}")
    public void runScheduledProcessing() {
        List<String> activeTypes = appProperties.getScheduling().getActiveBusinessTypes();

        if (activeTypes.isEmpty()) {
            log.warn("No active business types configured — nothing to process");
            return;
        }

        log.info("Starting scheduled SMS processing for {} business type(s): {}", activeTypes.size(), activeTypes);

        for (String businessType : activeTypes) {
            try {
                businessProcessorService.process(businessType);
            } catch (Exception ex) {
                log.error("Error processing business type '{}' — skipping to next type", businessType, ex);
            }
        }

        log.info("Scheduled SMS processing completed");
    }
}

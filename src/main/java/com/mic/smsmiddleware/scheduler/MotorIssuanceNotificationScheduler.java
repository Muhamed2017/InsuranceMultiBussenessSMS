package com.mic.smsmiddleware.scheduler;

import com.mic.smsmiddleware.service.BusinessProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MotorIssuanceNotificationScheduler {

    private static final String BUSINESS_TYPE = "motor-issuance-notification";

    private final BusinessProcessorService businessProcessorService;

    @Scheduled(cron = "${mic.scheduling.motor-issuance-cron:0 0 12 * * *}")
    public void run() {
        log.info("Starting scheduled run for {}", BUSINESS_TYPE);
        try {
            businessProcessorService.process(BUSINESS_TYPE);
        } catch (Exception ex) {
            log.error("Scheduled run failed for {}", BUSINESS_TYPE, ex);
        }
        log.info("Scheduled run completed for {}", BUSINESS_TYPE);
    }
}

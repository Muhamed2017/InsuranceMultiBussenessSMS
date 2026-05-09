package com.mic.smsmiddleware.scheduler;

import com.mic.smsmiddleware.domain.entity.SmsLog;
import com.mic.smsmiddleware.domain.model.SmsDeliveryResult;
import com.mic.smsmiddleware.properties.AppProperties;
import com.mic.smsmiddleware.provider.SmsProvider;
import com.mic.smsmiddleware.repository.SmsLogRepository;
import com.mic.smsmiddleware.service.SmsLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private final AppProperties appProperties;
    private final SmsLogRepository smsLogRepository;
    private final SmsProvider smsProvider;
    private final SmsLogService smsLogService;

    @Scheduled(cron = "${mic.scheduling.retry-cron:0 */5 * * * *}")
    public void retryFailedMessages() {
        int maxRetryCount = appProperties.getScheduling().getMaxRetryCount();
        List<SmsLog> failedLogs = smsLogRepository.findRetryable(maxRetryCount);

        if (failedLogs.isEmpty()) {
            return;
        }

        log.info("Retry cycle — {} failed message(s) eligible for retry", failedLogs.size());

        int recovered = 0;
        int stillFailed = 0;

        for (SmsLog failedLog : failedLogs) {
            SmsDeliveryResult result = smsProvider.send(
                    failedLog.getNormalizedPhone(),
                    failedLog.getMessageContent()
            );

            if (result.isSuccessful()) {
                smsLogService.markSent(failedLog);
                recovered++;
                log.info("Retry succeeded — id={}, phone={}, businessType={}",
                        failedLog.getId(), failedLog.getNormalizedPhone(), failedLog.getBusinessType());
            } else {
                smsLogService.incrementRetry(failedLog, result.getFailureReason());
                stillFailed++;
                log.warn("Retry failed (attempt {}/{}) — id={}, phone={}, reason={}",
                        failedLog.getRetryCount() + 1, maxRetryCount,
                        failedLog.getId(), failedLog.getNormalizedPhone(), result.getFailureReason());
            }
        }

        log.info("Retry cycle complete — {} recovered, {} still failed", recovered, stillFailed);
    }
}

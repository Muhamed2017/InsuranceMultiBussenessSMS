package com.mic.smsmiddleware.service;

import com.mic.smsmiddleware.domain.entity.SmsLog;
import com.mic.smsmiddleware.domain.enums.SmsStatus;
import com.mic.smsmiddleware.domain.model.ContactRecord;
import com.mic.smsmiddleware.domain.model.SmsDeliveryResult;
import com.mic.smsmiddleware.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsLogService {

    private final SmsLogRepository smsLogRepository;

    public SmsLog record(ContactRecord contact, String messageContent, SmsDeliveryResult result) {
        SmsLog entry = new SmsLog();
        entry.setBusinessType(contact.getBusinessType());
        entry.setContactPhone(contact.getRawPhone());
        entry.setNormalizedPhone(contact.getNormalizedPhone());
        entry.setContactName(contact.getContactName());
        entry.setReferenceKey(contact.getReferenceKey());
        entry.setMessageContent(messageContent);
        entry.setRetryCount(0);

        if (result.isSuccessful()) {
            entry.setStatus(SmsStatus.SENT);
            entry.setSentAt(LocalDateTime.now());
            log.info("SMS sent — businessType={}, phone={}, ref={}",
                    contact.getBusinessType(), contact.getNormalizedPhone(), contact.getReferenceKey());
        } else {
            entry.setStatus(SmsStatus.FAILED);
            entry.setFailureReason(truncate(result.getFailureReason(), 500));
            log.warn("SMS failed — businessType={}, phone={}, ref={}, reason={}",
                    contact.getBusinessType(), contact.getNormalizedPhone(),
                    contact.getReferenceKey(), result.getFailureReason());
        }

        return smsLogRepository.save(entry);
    }

    public SmsLog markSent(SmsLog smsLog) {
        smsLog.setStatus(SmsStatus.SENT);
        smsLog.setSentAt(LocalDateTime.now());
        smsLog.setFailureReason(null);
        return smsLogRepository.save(smsLog);
    }

    public SmsLog incrementRetry(SmsLog smsLog, String failureReason) {
        smsLog.setRetryCount(smsLog.getRetryCount() + 1);
        smsLog.setFailureReason(truncate(failureReason, 500));
        return smsLogRepository.save(smsLog);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

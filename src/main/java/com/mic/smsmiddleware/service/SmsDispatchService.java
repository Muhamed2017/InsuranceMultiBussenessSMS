package com.mic.smsmiddleware.service;

import com.mic.smsmiddleware.domain.model.ContactRecord;
import com.mic.smsmiddleware.domain.model.SmsDeliveryResult;
import com.mic.smsmiddleware.provider.SmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsDispatchService {

    private final SmsProvider smsProvider;
    private final DeduplicationService deduplicationService;
    private final MessageTemplateService messageTemplateService;
    private final SmsLogService smsLogService;

    /**
     * Returns true if the SMS was dispatched (sent or failed and logged).
     * Returns false if the record was skipped because an identical SENT entry
     * already exists in SMS_LOG (deduplication).
     */
    public boolean dispatch(ContactRecord contact, String template) {
        if (deduplicationService.alreadySent(
                contact.getBusinessType(),
                contact.getReferenceKey(),
                contact.getNormalizedPhone())) {
            return false;
        }

        String message = messageTemplateService.compose(template, contact.getSemanticData());
        SmsDeliveryResult result = smsProvider.send(contact.getNormalizedPhone(), message);
        smsLogService.record(contact, message, result);
        return true;
    }
}

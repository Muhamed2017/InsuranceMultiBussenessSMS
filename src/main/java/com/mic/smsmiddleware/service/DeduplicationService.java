package com.mic.smsmiddleware.service;

import com.mic.smsmiddleware.domain.enums.SmsStatus;
import com.mic.smsmiddleware.repository.SmsLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private final SmsLogRepository smsLogRepository;

    public boolean alreadySent(String businessType, String referenceKey, String normalizedPhone) {
        boolean duplicate = smsLogRepository.existsByBusinessTypeAndReferenceKeyAndNormalizedPhoneAndStatus(
                businessType, referenceKey, normalizedPhone, SmsStatus.SENT
        );
        if (duplicate) {
            log.debug("Duplicate found — skipping: businessType={}, referenceKey={}, phone={}",
                    businessType, referenceKey, normalizedPhone);
        }
        return duplicate;
    }
}

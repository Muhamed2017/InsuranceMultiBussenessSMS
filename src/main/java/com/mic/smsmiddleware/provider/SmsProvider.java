package com.mic.smsmiddleware.provider;

import com.mic.smsmiddleware.domain.model.SmsDeliveryResult;

public interface SmsProvider {

    SmsDeliveryResult send(String normalizedPhone, String message);
}

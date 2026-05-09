package com.mic.smsmiddleware.domain.model;

import lombok.Getter;

@Getter
public final class SmsDeliveryResult {

    private final boolean successful;
    private final String providerReference;
    private final String failureReason;

    private SmsDeliveryResult(boolean successful, String providerReference, String failureReason) {
        this.successful = successful;
        this.providerReference = providerReference;
        this.failureReason = failureReason;
    }

    public static SmsDeliveryResult success(String providerReference) {
        return new SmsDeliveryResult(true, providerReference, null);
    }

    public static SmsDeliveryResult failure(String failureReason) {
        return new SmsDeliveryResult(false, null, failureReason);
    }
}

package com.mic.smsmiddleware.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public final class ContactRecord {

    private final String businessType;
    private final String rawPhone;
    private final String normalizedPhone;
    private final String contactName;
    private final String referenceKey;
    private final Map<String, String> semanticData;
}

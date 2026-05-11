package com.mic.smsmiddleware.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class ProcessingResult {

    private final String businessType;
    private final int totalFetched;
    private final int dispatched;
    private final int skippedDuplicate;
    private final int skippedInvalidPhone;
    private final long processingTimeMs;
}

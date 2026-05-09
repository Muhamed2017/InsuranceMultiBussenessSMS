package com.mic.smsmiddleware.service;

import com.mic.smsmiddleware.domain.model.ContactRecord;
import com.mic.smsmiddleware.exception.InvalidMobileNumberException;
import com.mic.smsmiddleware.properties.AppProperties;
import com.mic.smsmiddleware.properties.BusinessTypeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessProcessorService {

    private final AppProperties appProperties;
    private final StoredProcedureService storedProcedureService;
    private final MobileValidationService mobileValidationService;
    private final SmsDispatchService smsDispatchService;

    public void process(String businessType) {
        BusinessTypeConfig config = resolveConfig(businessType);

        log.info("Processing business type: {} using procedure: {}", businessType, config.getStoredProcedure());

        List<Map<String, String>> rawRecords = storedProcedureService.execute(
                config.getStoredProcedure(),
                config.getParameters()
        );

        log.info("Business type '{}' — {} record(s) fetched from SP", businessType, rawRecords.size());

        int dispatched = 0;
        int skippedInvalid = 0;

        for (Map<String, String> rawRecord : rawRecords) {
            boolean processed = processRecord(businessType, config, rawRecord);
            if (processed) {
                dispatched++;
            } else {
                skippedInvalid++;
            }
        }

        log.info("Business type '{}' — {} dispatched, {} skipped (invalid phone)",
                businessType, dispatched, skippedInvalid);
    }

    private boolean processRecord(String businessType, BusinessTypeConfig config, Map<String, String> rawRecord) {
        String rawPhone = rawRecord.getOrDefault(config.getPhoneField(), "");

        String normalizedPhone;
        try {
            normalizedPhone = mobileValidationService.normalize(rawPhone);
        } catch (InvalidMobileNumberException ex) {
            log.warn("Skipping record — invalid phone '{}' for businessType={}", rawPhone, businessType);
            return false;
        }

        Map<String, String> semanticData = applyFieldMappings(rawRecord, config.getFieldMappings());
        String referenceKey = buildReferenceKey(rawRecord, config.getReferenceFields());
        String contactName = rawRecord.getOrDefault(config.getNameField(), "");

        ContactRecord contact = ContactRecord.builder()
                .businessType(businessType)
                .rawPhone(rawPhone)
                .normalizedPhone(normalizedPhone)
                .contactName(contactName)
                .referenceKey(referenceKey)
                .semanticData(semanticData)
                .build();

        smsDispatchService.dispatch(contact, config.getTemplate());
        return true;
    }

    private Map<String, String> applyFieldMappings(Map<String, String> rawRecord, Map<String, String> fieldMappings) {
        Map<String, String> semantic = new LinkedHashMap<>();
        for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
            String infoKey = mapping.getKey();
            String semanticName = mapping.getValue();
            String value = rawRecord.getOrDefault(infoKey, "");
            semantic.put(semanticName, value);
        }
        return semantic;
    }

    private String buildReferenceKey(Map<String, String> rawRecord, List<String> referenceFields) {
        if (referenceFields == null || referenceFields.isEmpty()) {
            return "";
        }
        return referenceFields.stream()
                .map(field -> rawRecord.getOrDefault(field, ""))
                .collect(Collectors.joining("_"));
    }

    private BusinessTypeConfig resolveConfig(String businessType) {
        BusinessTypeConfig config = appProperties.getBusinessTypes().get(businessType);
        if (config == null) {
            throw new IllegalArgumentException("No configuration found for business type: " + businessType);
        }
        return config;
    }
}

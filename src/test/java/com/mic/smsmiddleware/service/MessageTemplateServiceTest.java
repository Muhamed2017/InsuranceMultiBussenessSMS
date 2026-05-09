package com.mic.smsmiddleware.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTemplateServiceTest {

    private MessageTemplateService service;

    @BeforeEach
    void setUp() {
        service = new MessageTemplateService();
    }

    @Test
    void compose_allPlaceholdersPresent_replacesAll() {
        String template = "Dear {InsuredName}, policy {PolicyNumber} issued on {IssuanceDate}.";
        Map<String, String> data = Map.of(
                "InsuredName", "Mohamed Ali",
                "PolicyNumber", "POL-2024-001",
                "IssuanceDate", "01/01/2024"
        );
        String result = service.compose(template, data);
        assertThat(result).isEqualTo("Dear Mohamed Ali, policy POL-2024-001 issued on 01/01/2024.");
    }

    @Test
    void compose_missingDataValue_replacesWithEmpty() {
        String template = "Dear {InsuredName}, amount: {Amount}.";
        Map<String, String> data = Map.of("InsuredName", "Ali");
        String result = service.compose(template, data);
        assertThat(result).isEqualTo("Dear Ali, amount: {Amount}.");
    }

    @Test
    void compose_nullDataValue_replacesWithEmpty() {
        String template = "Dear {InsuredName}.";
        Map<String, String> data = new java.util.HashMap<>();
        data.put("InsuredName", null);
        String result = service.compose(template, data);
        assertThat(result).isEqualTo("Dear .");
    }
}

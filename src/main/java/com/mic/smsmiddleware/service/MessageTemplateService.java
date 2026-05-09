package com.mic.smsmiddleware.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MessageTemplateService {

    public String compose(String template, Map<String, String> semanticData) {
        String result = template;
        for (Map.Entry<String, String> entry : semanticData.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }
}

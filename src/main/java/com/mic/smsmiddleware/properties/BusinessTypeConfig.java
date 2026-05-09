package com.mic.smsmiddleware.properties;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BusinessTypeConfig {

    private String storedProcedure;
    private List<SpParameterConfig> parameters = new ArrayList<>();
    private String phoneField;
    private String nameField;
    private List<String> referenceFields = new ArrayList<>();
    private String template;
    private Map<String, String> fieldMappings = new LinkedHashMap<>();
}

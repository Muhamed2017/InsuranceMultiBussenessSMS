package com.mic.smsmiddleware.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SpParameterConfig {

    private String name;
    private String value;
    private String type = "STRING";
}

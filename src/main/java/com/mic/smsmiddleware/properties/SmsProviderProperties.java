package com.mic.smsmiddleware.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsProviderProperties {

    private String baseUrl;
    private String username;
    private String password;
    private String senderId;
    private String language = "2";
    private String environment = "1";
    private String successResponseCode = "0000";
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}

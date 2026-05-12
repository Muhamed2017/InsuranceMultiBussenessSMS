package com.mic.smsmiddleware.properties;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SchedulingProperties {

    private int processingStartHour = 10;
    private int processingEndHour = 23;
    private int maxRetryCount = 3;
    private String processingCron = "0 0 10-23 * * *";
    private String retryCron = "0 */5 * * * *";
    private String motorIssuanceCron = "0 0 12 * * *";
    private List<String> activeBusinessTypes = new ArrayList<>();
}

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
    private String retryCron = "0 */5 * * * *";
}

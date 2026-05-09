package com.mic.smsmiddleware.provider;

import com.mic.smsmiddleware.domain.model.SmsDeliveryResult;
import com.mic.smsmiddleware.properties.AppProperties;
import com.mic.smsmiddleware.properties.SmsProviderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpSmsProvider implements SmsProvider {

    private final RestTemplate smsRestTemplate;
    private final AppProperties appProperties;

    @Override
    public SmsDeliveryResult send(String normalizedPhone, String message) {
        SmsProviderProperties config = appProperties.getProvider();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", config.getUsername());
        body.add("password", config.getPassword());
        body.add("sender", config.getSenderId());
        body.add("mobile", normalizedPhone);
        body.add("message", message);
        body.add("language", config.getLanguage());
        body.add("environment", config.getEnvironment());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = smsRestTemplate.postForEntity(
                    config.getBaseUrl(), request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                if (responseBody != null && responseBody.contains(config.getSuccessResponseCode())) {
                    log.debug("SMS sent successfully to {}. Provider ref: {}", normalizedPhone, responseBody);
                    return SmsDeliveryResult.success(responseBody);
                }
                String reason = "Provider returned non-success code. Response: " + responseBody;
                log.warn("SMS delivery failed for {}: {}", normalizedPhone, reason);
                return SmsDeliveryResult.failure(reason);
            }

            String reason = "HTTP " + response.getStatusCode().value() + " from provider";
            log.warn("SMS delivery failed for {}: {}", normalizedPhone, reason);
            return SmsDeliveryResult.failure(reason);

        } catch (RestClientException ex) {
            String reason = "Provider request error: " + ex.getMessage();
            log.error("SMS provider call failed for {}: {}", normalizedPhone, reason, ex);
            return SmsDeliveryResult.failure(reason);
        }
    }
}

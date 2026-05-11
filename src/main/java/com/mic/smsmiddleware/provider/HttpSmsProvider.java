package com.mic.smsmiddleware.provider;

import com.mic.smsmiddleware.domain.model.SmsDeliveryResult;
import com.mic.smsmiddleware.properties.AppProperties;
import com.mic.smsmiddleware.properties.SmsProviderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpSmsProvider implements SmsProvider {

    private final RestTemplate smsRestTemplate;
    private final AppProperties appProperties;

    @Override
    public SmsDeliveryResult send(String normalizedPhone, String message) {
        SmsProviderProperties config = appProperties.getProvider();

        String url = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl())
                .queryParam("phone", normalizedPhone)
                .queryParam("msg", message)
                .toUriString();

        try {
            ResponseEntity<String> response = smsRestTemplate.postForEntity(url, null, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("SMS sent successfully to {}. Provider response: {}", normalizedPhone, response.getBody());
                return SmsDeliveryResult.success(response.getBody());
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

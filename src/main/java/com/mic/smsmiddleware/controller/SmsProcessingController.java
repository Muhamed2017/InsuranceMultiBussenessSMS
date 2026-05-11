package com.mic.smsmiddleware.controller;

import com.mic.smsmiddleware.domain.model.ProcessingResult;
import com.mic.smsmiddleware.exception.StoredProcedureException;
import com.mic.smsmiddleware.properties.AppProperties;
import com.mic.smsmiddleware.service.BusinessProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsProcessingController {

    private final BusinessProcessorService businessProcessorService;
    private final AppProperties appProperties;

    /**
     * Lists all configured business types and which ones are active in the scheduler.
     *
     * GET /api/sms/business-types
     */
    @GetMapping("/business-types")
    public ResponseEntity<Map<String, Object>> listBusinessTypes() {
        return ResponseEntity.ok(Map.of(
                "configured", appProperties.getBusinessTypes().keySet(),
                "scheduledActive", appProperties.getScheduling().getActiveBusinessTypes()
        ));
    }

    /**
     * Manually triggers a full processing run for the given business type:
     * executes the configured stored procedure, validates phones, checks
     * deduplication, sends SMS messages, and logs results.
     *
     * POST /api/sms/process/{businessType}
     *
     * Returns 200 with a ProcessingResult summary on success.
     * Returns 404 if the business type is not defined in configuration.
     * Returns 502 if the stored procedure fails to execute.
     */
    @PostMapping("/process/{businessType}")
    public ResponseEntity<?> triggerProcessing(@PathVariable String businessType) {
        if (!appProperties.getBusinessTypes().containsKey(businessType)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Unknown business type: " + businessType,
                    "configured", appProperties.getBusinessTypes().keySet()
            ));
        }

        log.info("Manual trigger received — businessType={}", businessType);

        try {
            ProcessingResult result = businessProcessorService.process(businessType);
            return ResponseEntity.ok(result);
        } catch (StoredProcedureException ex) {
            log.error("Manual trigger failed — stored procedure error for businessType={}", businessType, ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "error", "Stored procedure execution failed",
                    "detail", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Manual trigger failed — unexpected error for businessType={}", businessType, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Unexpected error during processing",
                    "detail", ex.getMessage()
            ));
        }
    }
}

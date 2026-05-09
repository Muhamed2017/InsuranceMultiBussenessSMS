package com.mic.smsmiddleware.service;

import com.mic.smsmiddleware.exception.InvalidMobileNumberException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class MobileValidationService {

    private static final Set<String> VALID_PREFIXES = Set.of("010", "011", "012", "015");
    private static final int EXPECTED_LOCAL_LENGTH = 11;

    public String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            throw new InvalidMobileNumberException(rawPhone);
        }

        String digits = rawPhone.replaceAll("[^\\d+]", "").trim();

        if (digits.startsWith("+20")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("0020")) {
            digits = "0" + digits.substring(4);
        } else if (digits.startsWith("20") && digits.length() == 12) {
            digits = "0" + digits.substring(2);
        }

        if (digits.length() != EXPECTED_LOCAL_LENGTH) {
            throw new InvalidMobileNumberException(rawPhone);
        }

        String prefix = digits.substring(0, 3);
        if (!VALID_PREFIXES.contains(prefix)) {
            throw new InvalidMobileNumberException(rawPhone);
        }

        return digits;
    }

    public boolean isValid(String rawPhone) {
        try {
            normalize(rawPhone);
            return true;
        } catch (InvalidMobileNumberException e) {
            return false;
        }
    }
}

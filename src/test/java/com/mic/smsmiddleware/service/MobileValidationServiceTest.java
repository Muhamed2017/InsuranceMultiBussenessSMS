package com.mic.smsmiddleware.service;

import com.mic.smsmiddleware.exception.InvalidMobileNumberException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class MobileValidationServiceTest {

    private MobileValidationService service;

    @BeforeEach
    void setUp() {
        service = new MobileValidationService();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "01012345678",
            "01112345678",
            "01212345678",
            "01512345678"
    })
    void normalize_validLocalNumbers_returnsSameNumber(String input) {
        assertThat(service.normalize(input)).isEqualTo(input);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+201012345678",
            "00201012345678",
            "201012345678"
    })
    void normalize_numberWithCountryCode_stripsCodeAndReturnsLocalFormat(String input) {
        assertThat(service.normalize(input)).isEqualTo("01012345678");
    }

    @Test
    void normalize_numberWithSpaces_normalizesCorrectly() {
        assertThat(service.normalize("0101 234 5678")).isEqualTo("01012345678");
    }

    @Test
    void normalize_numberWithDashes_normalizesCorrectly() {
        assertThat(service.normalize("0101-234-5678")).isEqualTo("01012345678");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "01312345678",
            "01412345678",
            "01612345678",
            "09912345678",
            "12345",
            "",
            "   "
    })
    void normalize_invalidNumbers_throwsException(String input) {
        assertThatThrownBy(() -> service.normalize(input))
                .isInstanceOf(InvalidMobileNumberException.class);
    }

    @Test
    void isValid_validNumber_returnsTrue() {
        assertThat(service.isValid("01012345678")).isTrue();
    }

    @Test
    void isValid_invalidNumber_returnsFalse() {
        assertThat(service.isValid("13456789012")).isFalse();
    }
}

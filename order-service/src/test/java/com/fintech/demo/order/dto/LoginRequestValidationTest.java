package com.fintech.demo.order.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】單元測試 LoginRequest Bean Validation，與 AUTH-003 HTTP 成對。
 * 【技巧】純 Validator，不啟動 Spring。
 * 【概念】缺欄位應在進入 AuthService 前被擋下。
 */
@Tag("unit")
class LoginRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    /**
     * CASE AUTH-003：缺 username／password → 有違規。
     */
    @Test
    void AUTH_003_missingRequired_hasViolations() {
        LoginRequest request = new LoginRequest();
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("username", "password");
    }
}

package com.fintech.demo.order.dto;

import com.fintech.demo.order.domain.OrderSide;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 【職責】單元測試 CreateOrderRequest 的 Bean Validation，與 ORDER HTTP Case 成對。
 * 【技巧】純 Validator（無 Spring 容器）。
 * 【概念】進 MockMvc 前先鎖住 DTO 規則，失敗成本更低。
 */
@Tag("unit")
class CreateOrderRequestValidationTest {

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

    private CreateOrderRequest valid() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setClientOrderId("fixture-order-001");
        request.setSymbol("AAPL");
        request.setSide(OrderSide.BUY);
        request.setQuantity(1);
        request.setPrice(new BigDecimal("150.00"));
        return request;
    }

    /**
     * CASE ORDER-001：合法請求無違規。
     */
    @Test
    void ORDER_001_validRequest_hasNoViolations() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    /**
     * CASE ORDER-003：缺必填（clientOrderId）有違規。
     */
    @Test
    void ORDER_003_missingClientOrderId_hasViolation() {
        CreateOrderRequest request = valid();
        request.setClientOrderId(null);
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("clientOrderId"));
    }

    /**
     * CASE ORDER-004：quantity &lt; 1 有違規。
     */
    @Test
    void ORDER_004_invalidQuantity_hasViolation() {
        CreateOrderRequest request = valid();
        request.setQuantity(0);
        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("quantity"));
    }
}

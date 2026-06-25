package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

import de.hybris.bootstrap.annotations.UnitTest;

import java.math.BigDecimal;

import org.junit.Test;

@UnitTest
public class StripeValidationUtilsTest {

    @Test
    public void validatePositiveAmount_withPositiveAmount_doesNotThrow() {
        StripeValidationUtils.validatePositiveAmount(BigDecimal.ONE, "error");
    }

    @Test(expected = StripeIntegrationException.class)
    public void validatePositiveAmount_withZero_throwsException() {
        StripeValidationUtils.validatePositiveAmount(BigDecimal.ZERO, "error");
    }
}

package com.cci.hybris.stripe.services.util;

import com.stripe.model.PaymentIntent;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripePaymentIntentUtilsTest {

    @Test
    public void shouldReuse_withMatchingDataAndUpdatableStatus_returnsTrue() {
        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setAmount(Long.valueOf(1234L));
        paymentIntent.setCurrency("usd");
        paymentIntent.setStatus("requires_payment_method");

        assertTrue(StripePaymentIntentUtils.shouldReuse(paymentIntent, 1234L, "USD"));
    }

    @Test
    public void shouldReuse_withTerminalStatus_returnsFalse() {
        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setAmount(Long.valueOf(1234L));
        paymentIntent.setCurrency("usd");
        paymentIntent.setStatus("succeeded");

        assertFalse(StripePaymentIntentUtils.shouldReuse(paymentIntent, 1234L, "usd"));
    }

    @Test
    public void shouldUpdate_withRequiresConfirmationStatus_returnsTrue() {
        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setStatus("requires_confirmation");

        assertTrue(StripePaymentIntentUtils.shouldUpdate(paymentIntent));
    }

    @Test
    public void safeAmount_withNulls_returnsZero() {
        assertTrue(StripePaymentIntentUtils.safeAmount(null) == 0L);

        final PaymentIntent paymentIntent = new PaymentIntent();
        assertTrue(StripePaymentIntentUtils.safeAmount(paymentIntent) == 0L);
    }
}

package com.cci.hybris.stripe.facades.util;

import com.cci.hybris.stripe.facades.data.StripePaymentElementFacadeData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.CartModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripePaymentElementFacadeUtilsTest {

    @Test
    public void toFacadeData_mapsSourceAndFacadeFields() {
        final StripePaymentIntentData source = new StripePaymentIntentData();
        source.setId("pi_test_123");
        source.setClientSecret("pi_test_123_secret");
        source.setStatus("requires_payment_method");
        source.setAmount(Long.valueOf(1234L));
        source.setCurrency("usd");
        source.setClientReferenceId("cart-001");

        final StripePaymentElementFacadeData result = StripePaymentElementFacadeUtils.toFacadeData(
                source,
                "pk_test",
                "stripe-elements",
                "stripe-elements",
                "https://apparel-uk.local:4200/return",
                "$12.34");

        assertEquals("pi_test_123", result.getId());
        assertEquals("pi_test_123_secret", result.getClientSecret());
        assertEquals("requires_payment_method", result.getStatus());
        assertEquals(Long.valueOf(1234L), result.getAmount());
        assertEquals("usd", result.getCurrency());
        assertEquals("cart-001", result.getClientReferenceId());
        assertEquals("pk_test", result.getPublishableKey());
        assertEquals("stripe-elements", result.getPaymentOptionId());
        assertEquals("stripe-elements", result.getPaymentMethod());
        assertEquals("https://apparel-uk.local:4200/return", result.getReturnUrl());
        assertEquals("$12.34", result.getFormattedAmount());
    }

    @Test(expected = NullPointerException.class)
    public void toFacadeData_nullSource_throwsNullPointerException() {
        StripePaymentElementFacadeUtils.toFacadeData(
                null,
                "pk_test",
                "stripe-elements",
                "stripe-elements",
                "https://apparel-uk.local:4200/return",
                "$12.34");
    }

    @Test
    public void isFinalizablePaymentIntent_succeededAndRequiresCapture_returnTrue() {
        final StripePaymentIntentData succeeded = new StripePaymentIntentData();
        succeeded.setStatus("succeeded");
        final StripePaymentIntentData capture = new StripePaymentIntentData();
        capture.setStatus("REQUIRES_CAPTURE");

        assertTrue(StripePaymentElementFacadeUtils.isFinalizablePaymentIntent(succeeded));
        assertTrue(StripePaymentElementFacadeUtils.isFinalizablePaymentIntent(capture));
    }

    @Test
    public void isFinalizablePaymentIntent_otherStatusOrNull_returnFalse() {
        final StripePaymentIntentData source = new StripePaymentIntentData();
        source.setStatus("processing");

        assertFalse(StripePaymentElementFacadeUtils.isFinalizablePaymentIntent(source));
        assertFalse(StripePaymentElementFacadeUtils.isFinalizablePaymentIntent(null));
    }

    @Test
    public void matchesCheckoutContext_matchesCodeAndGuid() {
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setGuid("guid-001");

        assertTrue(StripePaymentElementFacadeUtils.matchesCheckoutContext(cart, "cart-001"));
        assertTrue(StripePaymentElementFacadeUtils.matchesCheckoutContext(cart, "guid-001"));
        assertFalse(StripePaymentElementFacadeUtils.matchesCheckoutContext(cart, "unknown"));
        assertFalse(StripePaymentElementFacadeUtils.matchesCheckoutContext(cart, ""));
        assertFalse(StripePaymentElementFacadeUtils.matchesCheckoutContext(null, "cart-001"));
    }
}

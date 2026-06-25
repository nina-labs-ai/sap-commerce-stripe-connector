package com.cci.hybris.stripe.facades.util;

import com.cci.hybris.stripe.facades.data.StripeCheckoutSessionFacadeData;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripeCheckoutFacadeUtilsTest {

    @Test
    public void toFacadeData_mapsSourceFields() {
        final StripeCheckoutSessionData source = new StripeCheckoutSessionData();
        source.setId("cs_test_123");
        source.setUrl("https://checkout.stripe.test/session");
        source.setStatus("complete");
        source.setPaymentStatus("paid");
        source.setClientReferenceId("cart-001");

        final StripeCheckoutSessionFacadeData result = StripeCheckoutFacadeUtils.toFacadeData(source);

        assertEquals("cs_test_123", result.getId());
        assertEquals("https://checkout.stripe.test/session", result.getUrl());
        assertEquals("complete", result.getStatus());
        assertEquals("paid", result.getPaymentStatus());
        assertEquals("cart-001", result.getClientReferenceId());
    }

    @Test(expected = NullPointerException.class)
    public void toFacadeData_nullSource_throwsNullPointerException() {
        StripeCheckoutFacadeUtils.toFacadeData(null);
    }

    @Test
    public void isFinalizableSession_paidOrComplete_returnsTrue() {
        final StripeCheckoutSessionData paid = new StripeCheckoutSessionData();
        paid.setPaymentStatus("PAID");
        final StripeCheckoutSessionData complete = new StripeCheckoutSessionData();
        complete.setStatus("complete");

        assertTrue(StripeCheckoutFacadeUtils.isFinalizableSession(paid));
        assertTrue(StripeCheckoutFacadeUtils.isFinalizableSession(complete));
    }

    @Test
    public void isFinalizableSession_unknownStatusOrNull_returnsFalse() {
        final StripeCheckoutSessionData pending = new StripeCheckoutSessionData();
        pending.setStatus("open");
        pending.setPaymentStatus("unpaid");

        assertFalse(StripeCheckoutFacadeUtils.isFinalizableSession(pending));
        assertFalse(StripeCheckoutFacadeUtils.isFinalizableSession(null));
    }

    @Test
    public void hasMatchingClientReference_validAndInvalidValues() {
        final StripeCheckoutSessionData source = new StripeCheckoutSessionData();
        source.setClientReferenceId("cart-001");

        assertTrue(StripeCheckoutFacadeUtils.hasMatchingClientReference("cart-001", source));
        assertFalse(StripeCheckoutFacadeUtils.hasMatchingClientReference("cart-002", source));
        assertFalse(StripeCheckoutFacadeUtils.hasMatchingClientReference("cart-001", null));
    }
}

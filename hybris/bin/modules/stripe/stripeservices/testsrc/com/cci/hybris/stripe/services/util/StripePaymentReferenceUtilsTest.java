package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripePaymentReferenceUtilsTest {

    @Test
    public void paymentReferenceTypeChecks_withKnownPrefixes_matchExpectedType() {
        assertTrue(StripePaymentReferenceUtils.isPaymentIntentReference("pi_test_123"));
        assertTrue(StripePaymentReferenceUtils.isCheckoutSessionReference("cs_test_123"));
    }

    @Test
    public void resolvePaymentFlow_withPaymentIntent_returnsElementsFlow() {
        assertEquals(StripeServicesConstants.PAYMENT_FLOW_ELEMENTS,
                StripePaymentReferenceUtils.resolvePaymentFlow("pi_test_123"));
    }

    @Test
    public void resolvePaymentFlow_withCheckoutSession_returnsCheckoutFlow() {
        assertEquals(StripeServicesConstants.PAYMENT_FLOW_CHECKOUT,
                StripePaymentReferenceUtils.resolvePaymentFlow("cs_test_123"));
    }
}

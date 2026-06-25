package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripeStatusUtilsTest {

    @Test
    public void mapRefundStatus_withKnownStatuses_mapsExpectedValues() {
        assertEquals(StripeServicesConstants.STATUS_ACCEPTED, StripeStatusUtils.mapRefundStatus("succeeded"));
        assertEquals(StripeServicesConstants.STATUS_PENDING, StripeStatusUtils.mapRefundStatus("pending"));
        assertEquals(StripeServicesConstants.STATUS_REJECTED, StripeStatusUtils.mapRefundStatus("failed"));
    }

    @Test
    public void eventPredicates_withMatchingTypesAndStatuses_returnTrue() {
        final Session session = new Session();
        session.setPaymentStatus("paid");
        session.setStatus("complete");

        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setStatus("succeeded");

        assertTrue(StripeStatusUtils.isCheckoutCompletedEvent("checkout.session.completed", session));
        assertTrue(StripeStatusUtils.isPaymentIntentSucceededEvent("payment_intent.succeeded", paymentIntent));
    }

    @Test
    public void eventPredicates_withMismatchedTypesOrStatuses_returnFalse() {
        final Session expiredSession = new Session();
        expiredSession.setPaymentStatus("unpaid");
        expiredSession.setStatus("expired");

        final PaymentIntent canceledIntent = new PaymentIntent();
        canceledIntent.setStatus("canceled");

        assertFalse(StripeStatusUtils.isCheckoutCompletedEvent("checkout.session.expired", expiredSession));
        assertFalse(StripeStatusUtils.isPaymentIntentSucceededEvent("payment_intent.payment_failed", canceledIntent));
        assertFalse(StripeStatusUtils.isPaymentIntentFailedEvent("payment_intent.created", canceledIntent));
    }
}

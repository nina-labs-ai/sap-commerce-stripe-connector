package com.cci.hybris.stripe.services.util;

import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import de.hybris.bootstrap.annotations.UnitTest;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UnitTest
public class StripeDataConversionUtilsTest {

    @Test
    public void resolveOrderCode_withSessionMetadata_usesMetadataValue() {
        final Session session = new Session();
        session.setClientReferenceId("fallback");
        session.setMetadata(Map.of("orderCode", "order-001"));

        assertEquals("order-001", StripeDataConversionUtils.resolveOrderCode(session));
    }

    @Test
    public void resolveOrderCode_withPaymentIntentMetadata_usesMetadataValue() {
        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setMetadata(Map.of("orderCode", "order-002"));

        assertEquals("order-002", StripeDataConversionUtils.resolveOrderCode(paymentIntent));
    }
}

package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.service.StripePaymentIntentService;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.model.order.OrderModel;

import org.junit.Test;

import jakarta.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Live Stripe PaymentIntent integration tests.
 */
@IntegrationTest
public class DefaultStripePaymentIntentServiceIntegrationTest extends AbstractStripeServicesIntegrationTest {

    @Resource
    private StripePaymentIntentService stripePaymentIntentService;

    @Test
    public void getPaymentIntent_existingLiveIntentForMatchingSite_returnsStripeData() {
        final String siteUid = uniqueCode("site-live-elements-get");
        prepareLiveStripeCheckoutConfiguration(siteUid);
        final OrderModel order = createOrder(uniqueCode("order-live-elements-get"), siteUid);
        final StripePaymentIntentData createdIntent = stripePaymentIntentService.createOrUpdatePaymentIntent(order);

        final StripePaymentIntentData retrievedIntent = stripePaymentIntentService.getPaymentIntent(order, createdIntent.getId(), siteUid);

        assertEquals(createdIntent.getId(), retrievedIntent.getId());
        assertEquals(order.getCode(), retrievedIntent.getClientReferenceId());
        assertNotNull(retrievedIntent.getStatus());
        assertTrue(retrievedIntent.getId().startsWith("pi_"));
    }

    @Test(expected = StripeIntegrationException.class)
    public void getPaymentIntent_wrongSite_rejectsLiveIntent() {
        final String siteUid = uniqueCode("site-live-elements-primary");
        final String wrongSiteUid = uniqueCode("site-live-elements-secondary");
        prepareLiveStripeCheckoutConfiguration(siteUid);
        prepareLiveStripeCheckoutConfiguration(wrongSiteUid);
        final OrderModel order = createOrder(uniqueCode("order-live-elements-wrong-site"), siteUid);
        final StripePaymentIntentData createdIntent = stripePaymentIntentService.createOrUpdatePaymentIntent(order);

        stripePaymentIntentService.getPaymentIntent(order, createdIntent.getId(), wrongSiteUid);
    }
}

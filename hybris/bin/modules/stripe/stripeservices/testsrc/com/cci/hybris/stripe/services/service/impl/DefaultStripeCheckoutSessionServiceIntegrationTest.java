package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.service.StripeCheckoutSessionService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.model.order.OrderModel;

import org.junit.Test;

import jakarta.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Live Stripe Checkout Session integration tests.
 */
@IntegrationTest
public class DefaultStripeCheckoutSessionServiceIntegrationTest extends AbstractStripeServicesIntegrationTest {

    @Resource
    private StripeCheckoutSessionService stripeCheckoutSessionService;
    @Resource
    private StripePaymentTransactionService stripePaymentTransactionService;

    @Test
    public void createCheckoutSession_validOrder_createsLiveSessionAndRegistersTransaction() {
        final String siteUid = uniqueCode("site-live-create");
        prepareLiveStripeCheckoutConfiguration(siteUid);
        final OrderModel order = createOrder(uniqueCode("order-live-create"), siteUid);

        final StripeCheckoutSessionData sessionData = stripeCheckoutSessionService.createCheckoutSession(order);

        modelService.refresh(order);
        assertNotNull(sessionData.getId());
        assertTrue(sessionData.getId().startsWith("cs_"));
        assertNotNull(sessionData.getUrl());
        assertFalse(sessionData.getUrl().isBlank());
        assertEquals(order.getCode(), sessionData.getClientReferenceId());
        assertTrue(stripePaymentTransactionService.hasPaymentTransactionEntry(order, sessionData.getId()));
    }

    @Test
    public void getCheckoutSession_existingLiveSession_returnsStripeData() {
        final String siteUid = uniqueCode("site-live-get");
        prepareLiveStripeCheckoutConfiguration(siteUid);
        final OrderModel order = createOrder(uniqueCode("order-live-get"), siteUid);
        final StripeCheckoutSessionData createdSession = stripeCheckoutSessionService.createCheckoutSession(order);

        final StripeCheckoutSessionData retrievedSession = stripeCheckoutSessionService
                .getCheckoutSession(createdSession.getId(), order.getSite().getUid(), order.getCode());

        assertEquals(createdSession.getId(), retrievedSession.getId());
        assertEquals(order.getCode(), retrievedSession.getClientReferenceId());
        assertNotNull(retrievedSession.getStatus());
    }

    @Test(expected = StripeIntegrationException.class)
    public void getCheckoutSession_wrongSite_rejectsLiveSession() {
        final String siteUid = uniqueCode("site-live-get-primary");
        final String wrongSiteUid = uniqueCode("site-live-get-secondary");
        prepareLiveStripeCheckoutConfiguration(siteUid);
        prepareLiveStripeCheckoutConfiguration(wrongSiteUid);
        final OrderModel order = createOrder(uniqueCode("order-live-get-wrong-site"), siteUid);
        final StripeCheckoutSessionData createdSession = stripeCheckoutSessionService.createCheckoutSession(order);

        stripeCheckoutSessionService.getCheckoutSession(createdSession.getId(), wrongSiteUid, order.getCode());
    }
}

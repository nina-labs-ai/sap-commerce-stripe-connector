package com.cci.hybris.stripe.events.controllers;

import com.cci.hybris.stripe.events.constants.StripeeventsConstants;
import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.webservicescommons.testsupport.client.WsRequestBuilder;
import de.hybris.platform.webservicescommons.testsupport.server.NeedsEmbeddedServer;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@NeedsEmbeddedServer(webExtensions = {StripeeventsConstants.EXTENSIONNAME})
@IntegrationTest
public class StripeWebhookControllerIntegrationTest extends AbstractStripeWebhookWebIntegrationSupport {

    private static final String WEBHOOK_PATH = "/webhooks/stripe";
    private static final String HEADER_SIGNATURE = "Stripe-Signature";

    @Test
    public void handleWebhook_completedCheckoutHttpRequest_marksOrderCaptured() throws Exception {
        final String siteUid = uniqueCode("site-http-complete");
        final OrderModel order = createOrder(uniqueCode("order-http-complete"), siteUid);
        final StripeCheckoutSessionData sessionData = createCheckoutSession(order, siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final String payload = buildCheckoutSessionEventPayload(
                uniqueCode("evt-http-complete"),
                "checkout.session.completed",
                sessionData.getId(),
                sessionData.getUrl(),
                order.getCode(),
                siteUid,
                "complete",
                "paid");

        final Response response = postWebhook(payload, siteUid);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertNotNull(findEntry(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()));
    }

    @Test
    public void handleWebhook_expiredCheckoutHttpRequest_marksOrderNotCaptured() throws Exception {
        final String siteUid = uniqueCode("site-http-expired");
        final OrderModel order = createOrder(uniqueCode("order-http-expired"), siteUid);
        final StripeCheckoutSessionData sessionData = createCheckoutSession(order, siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final String payload = buildCheckoutSessionEventPayload(
                uniqueCode("evt-http-expired"),
                "checkout.session.expired",
                sessionData.getId(),
                sessionData.getUrl(),
                order.getCode(),
                siteUid,
                "expired",
                "unpaid");

        final Response response = postWebhook(payload, siteUid);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_NOT_CAPTURED, order.getStatus());
        assertNull(findEntry(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()));
        assertEquals("REJECTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId()).getTransactionStatus());
    }

    protected Response postWebhook(final String payload, final String siteUid) throws Exception {
        final String webhookSecret = getRuntimeProperty(siteUid + "." + StripeServicesConstants.PROPERTY_WEBHOOK_SECRET);
        final Response response = new WsRequestBuilder()
                .extensionName(StripeeventsConstants.EXTENSIONNAME)
                .path(WEBHOOK_PATH)
                .build()
                .header(HEADER_SIGNATURE, createSignedStripeHeader(payload, webhookSecret))
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.entity(payload, MediaType.APPLICATION_JSON));
        response.bufferEntity();
        return response;
    }
}

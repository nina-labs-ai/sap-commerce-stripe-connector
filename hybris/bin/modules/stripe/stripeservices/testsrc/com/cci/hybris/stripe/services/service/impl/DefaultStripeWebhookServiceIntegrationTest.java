package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripeCheckoutSessionService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.cci.hybris.stripe.services.service.StripeWebhookService;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import org.junit.Test;

import jakarta.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Servicelayer integration tests for Stripe webhook processing.
 */
@IntegrationTest
public class DefaultStripeWebhookServiceIntegrationTest extends AbstractStripeServicesIntegrationTest {

    @Resource
    private StripeCheckoutSessionService stripeCheckoutSessionService;
    @Resource
    private StripeWebhookService stripeWebhookService;
    @Resource
    private StripeConfigurationService stripeConfigurationService;
    @Resource
    private StripePaymentTransactionService stripePaymentTransactionService;

    @Test
    public void handleWebhook_checkoutCompletedSignedPayload_marksOrderCaptured() throws Exception {
        final String siteUid = uniqueCode("site-webhook-paid");
        prepareLiveStripeWebhookConfiguration(siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final OrderModel order = createOrder(uniqueCode("order-webhook-paid"), siteUid);
        final StripeCheckoutSessionData sessionData = stripeCheckoutSessionService.createCheckoutSession(order);
        final String payload = buildCheckoutSessionEventPayload(
                uniqueCode("evt-paid"),
                "checkout.session.completed",
                sessionData.getId(),
                sessionData.getUrl(),
                order.getCode(),
                order.getSite().getUid(),
                "complete",
                "paid");

        stripeWebhookService.handleWebhook(payload,
                createSignedStripeHeader(payload, stripeConfigurationService.getWebhookSecret(order.getSite().getUid())),
                null);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertNotNull(findEntry(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()));
    }

    @Test
    public void handleWebhook_checkoutExpiredSignedPayload_marksOrderNotCaptured() throws Exception {
        final String siteUid = uniqueCode("site-webhook-expired");
        prepareLiveStripeWebhookConfiguration(siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final OrderModel order = createOrder(uniqueCode("order-webhook-expired"), siteUid);
        final StripeCheckoutSessionData sessionData = stripeCheckoutSessionService.createCheckoutSession(order);
        final String payload = buildCheckoutSessionEventPayload(
                uniqueCode("evt-expired"),
                "checkout.session.expired",
                sessionData.getId(),
                sessionData.getUrl(),
                order.getCode(),
                order.getSite().getUid(),
                "expired",
                "unpaid");

        stripeWebhookService.handleWebhook(payload,
                createSignedStripeHeader(payload, stripeConfigurationService.getWebhookSecret(order.getSite().getUid())),
                null);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_NOT_CAPTURED, order.getStatus());
        assertNull(findEntry(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()));
        assertEquals("REJECTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId()).getTransactionStatus());
    }

    @Test
    public void handleWebhook_duplicateCheckoutCompletedSignedPayload_keepsSingleCaptureEntry() throws Exception {
        final String siteUid = uniqueCode("site-webhook-paid-duplicate");
        prepareLiveStripeWebhookConfiguration(siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final OrderModel order = createOrder(uniqueCode("order-webhook-paid-duplicate"), siteUid);
        final StripeCheckoutSessionData sessionData = stripeCheckoutSessionService.createCheckoutSession(order);
        final String payload = buildCheckoutSessionEventPayload(
                uniqueCode("evt-paid-duplicate"),
                "checkout.session.completed",
                sessionData.getId(),
                sessionData.getUrl(),
                order.getCode(),
                order.getSite().getUid(),
                "complete",
                "paid");
        final String signature = createSignedStripeHeader(payload,
                stripeConfigurationService.getWebhookSecret(order.getSite().getUid()));

        stripeWebhookService.handleWebhook(payload, signature, null);
        stripeWebhookService.handleWebhook(payload, signature, null);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()));
    }

    @Test
    public void handleWebhook_checkoutExpiredAfterCompletionSignedPayload_doesNotDowngradeCapturedOrder() throws Exception {
        final String siteUid = uniqueCode("site-webhook-paid-then-expired");
        prepareLiveStripeWebhookConfiguration(siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final OrderModel order = createOrder(uniqueCode("order-webhook-paid-then-expired"), siteUid);
        final StripeCheckoutSessionData sessionData = stripeCheckoutSessionService.createCheckoutSession(order);
        final String completedPayload = buildCheckoutSessionEventPayload(
                uniqueCode("evt-paid-then-expired-completed"),
                "checkout.session.completed",
                sessionData.getId(),
                sessionData.getUrl(),
                order.getCode(),
                order.getSite().getUid(),
                "complete",
                "paid");
        final String completedSignature = createSignedStripeHeader(completedPayload,
                stripeConfigurationService.getWebhookSecret(order.getSite().getUid()));

        stripeWebhookService.handleWebhook(completedPayload, completedSignature, null);

        final String expiredPayload = buildCheckoutSessionEventPayload(
                uniqueCode("evt-paid-then-expired-expired"),
                "checkout.session.expired",
                sessionData.getId(),
                sessionData.getUrl(),
                order.getCode(),
                order.getSite().getUid(),
                "expired",
                "unpaid");
        final String expiredSignature = createSignedStripeHeader(expiredPayload,
                stripeConfigurationService.getWebhookSecret(order.getSite().getUid()));

        stripeWebhookService.handleWebhook(expiredPayload, expiredSignature, null);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()));
        assertEquals("ACCEPTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId()).getTransactionStatus());
    }

    @Test
    public void handleWebhook_paymentIntentCreatedSignedPayload_doesNotMutateOrder() throws Exception {
        final String siteUid = uniqueCode("site-webhook-pi-created");
        prepareLiveStripeWebhookConfiguration(siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final OrderModel order = createOrder(uniqueCode("order-webhook-pi-created"), siteUid);
        final StripePaymentIntentData paymentIntentData = createPaymentIntentData("pi_created_event");
        stripePaymentTransactionService.registerPaymentIntent(order, paymentIntentData);
        final String payload = buildPaymentIntentEventPayload(
                uniqueCode("evt-pi-created"),
                "payment_intent.created",
                paymentIntentData.getId(),
                order.getCode(),
                order.getSite().getUid(),
                "succeeded");
        final String signature = createSignedStripeHeader(payload,
                stripeConfigurationService.getWebhookSecret(order.getSite().getUid()));

        stripeWebhookService.handleWebhook(payload, signature, null);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertNull(findEntry(transaction, PaymentTransactionType.CAPTURE, paymentIntentData.getId()));
        assertEquals("PENDING",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, paymentIntentData.getId()).getTransactionStatus());
    }

    @Test
    public void handleWebhook_duplicatePaymentIntentSucceededSignedPayload_keepsSingleCaptureEntry() throws Exception {
        final String siteUid = uniqueCode("site-webhook-pi-succeeded-duplicate");
        prepareLiveStripeWebhookConfiguration(siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final OrderModel order = createOrder(uniqueCode("order-webhook-pi-succeeded-duplicate"), siteUid);
        final StripePaymentIntentData paymentIntentData = createPaymentIntentData("pi_succeeded_duplicate");
        stripePaymentTransactionService.registerPaymentIntent(order, paymentIntentData);
        final String payload = buildPaymentIntentEventPayload(
                uniqueCode("evt-pi-succeeded-duplicate"),
                "payment_intent.succeeded",
                paymentIntentData.getId(),
                order.getCode(),
                order.getSite().getUid(),
                "succeeded");
        final String signature = createSignedStripeHeader(payload,
                stripeConfigurationService.getWebhookSecret(order.getSite().getUid()));

        stripeWebhookService.handleWebhook(payload, signature, null);
        stripeWebhookService.handleWebhook(payload, signature, null);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.CAPTURE, paymentIntentData.getId()));
    }

    @Test
    public void handleWebhook_duplicatePaymentIntentFailedSignedPayload_keepsSingleRejectedAuthorization() throws Exception {
        final String siteUid = uniqueCode("site-webhook-pi-failed-duplicate");
        prepareLiveStripeWebhookConfiguration(siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final OrderModel order = createOrder(uniqueCode("order-webhook-pi-failed-duplicate"), siteUid);
        final StripePaymentIntentData paymentIntentData = createPaymentIntentData("pi_failed_duplicate");
        stripePaymentTransactionService.registerPaymentIntent(order, paymentIntentData);
        final String payload = buildPaymentIntentEventPayload(
                uniqueCode("evt-pi-failed-duplicate"),
                "payment_intent.payment_failed",
                paymentIntentData.getId(),
                order.getCode(),
                order.getSite().getUid(),
                "requires_payment_method");
        final String signature = createSignedStripeHeader(payload,
                stripeConfigurationService.getWebhookSecret(order.getSite().getUid()));

        stripeWebhookService.handleWebhook(payload, signature, null);
        stripeWebhookService.handleWebhook(payload, signature, null);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_NOT_CAPTURED, order.getStatus());
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.AUTHORIZATION, paymentIntentData.getId()));
        assertEquals("REJECTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, paymentIntentData.getId()).getTransactionStatus());
        assertNull(findEntry(transaction, PaymentTransactionType.CAPTURE, paymentIntentData.getId()));
    }

    @Test
    public void handleWebhook_paymentIntentCanceledAfterSuccessSignedPayload_doesNotDowngradeCapturedOrder() throws Exception {
        final String siteUid = uniqueCode("site-webhook-pi-canceled-after-success");
        prepareLiveStripeWebhookConfiguration(siteUid);
        overrideProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, null);
        final OrderModel order = createOrder(uniqueCode("order-webhook-pi-canceled-after-success"), siteUid);
        final StripePaymentIntentData paymentIntentData = createPaymentIntentData("pi_canceled_after_success");
        stripePaymentTransactionService.registerPaymentIntent(order, paymentIntentData);

        final String succeededPayload = buildPaymentIntentEventPayload(
                uniqueCode("evt-pi-canceled-after-success-succeeded"),
                "payment_intent.succeeded",
                paymentIntentData.getId(),
                order.getCode(),
                order.getSite().getUid(),
                "succeeded");
        final String succeededSignature = createSignedStripeHeader(succeededPayload,
                stripeConfigurationService.getWebhookSecret(order.getSite().getUid()));
        stripeWebhookService.handleWebhook(succeededPayload, succeededSignature, null);

        final String canceledPayload = buildPaymentIntentEventPayload(
                uniqueCode("evt-pi-canceled-after-success-canceled"),
                "payment_intent.canceled",
                paymentIntentData.getId(),
                order.getCode(),
                order.getSite().getUid(),
                "canceled");
        final String canceledSignature = createSignedStripeHeader(canceledPayload,
                stripeConfigurationService.getWebhookSecret(order.getSite().getUid()));
        stripeWebhookService.handleWebhook(canceledPayload, canceledSignature, null);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.CAPTURE, paymentIntentData.getId()));
        assertEquals("ACCEPTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, paymentIntentData.getId()).getTransactionStatus());
    }

    protected long countEntries(final PaymentTransactionModel transaction,
                                final PaymentTransactionType type,
                                final String requestId) {
        return transaction.getEntries().stream()
                .filter(entry -> type.equals(entry.getType()) && requestId.equals(entry.getRequestId()))
                .count();
    }

    protected StripePaymentIntentData createPaymentIntentData(final String paymentIntentId) {
        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId(paymentIntentId);
        paymentIntentData.setStatus("requires_payment_method");
        paymentIntentData.setCurrency("usd");
        paymentIntentData.setAmount(Long.valueOf(1000L));
        paymentIntentData.setClientReferenceId(paymentIntentId);
        return paymentIntentData;
    }
}

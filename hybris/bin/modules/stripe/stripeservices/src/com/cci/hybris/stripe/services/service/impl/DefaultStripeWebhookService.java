
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.cci.hybris.stripe.services.service.StripeWebhookService;
import com.cci.hybris.stripe.services.util.StripeDataConversionUtils;
import com.cci.hybris.stripe.services.util.StripeMetadataUtils;
import com.cci.hybris.stripe.services.util.StripeStatusUtils;
import com.cci.hybris.stripe.services.util.StripeWebhookPayloadUtils;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Default webhook verification and dispatch service.
 */
public class DefaultStripeWebhookService implements StripeWebhookService {

    private static final Set<String> CHECKOUT_SESSION_EVENT_TYPES = Set.of(
            StripeServicesConstants.EVENT_CHECKOUT_SESSION_COMPLETED,
            StripeServicesConstants.EVENT_CHECKOUT_SESSION_EXPIRED);
    private static final Set<String> PAYMENT_INTENT_EVENT_TYPES = Set.of(
            StripeServicesConstants.EVENT_PAYMENT_INTENT_SUCCEEDED,
            StripeServicesConstants.EVENT_PAYMENT_INTENT_PAYMENT_FAILED,
            StripeServicesConstants.EVENT_PAYMENT_INTENT_CANCELED);

    private final StripeClientFactory stripeClientFactory;
    private final StripeConfigurationService stripeConfigurationService;
    private final StripePaymentTransactionService stripePaymentTransactionService;

    /**
     * Creates the webhook service with the collaborators required for signature verification and event dispatch.
     *
     * @param stripeClientFactory Stripe client factory
     * @param stripeConfigurationService configuration service
     * @param stripePaymentTransactionService payment transaction service
     */
    public DefaultStripeWebhookService(final StripeClientFactory stripeClientFactory,
                                       final StripeConfigurationService stripeConfigurationService,
                                       final StripePaymentTransactionService stripePaymentTransactionService) {
        this.stripeClientFactory = stripeClientFactory;
        this.stripeConfigurationService = stripeConfigurationService;
        this.stripePaymentTransactionService = stripePaymentTransactionService;
    }

    /**
     * Verifies the Stripe webhook signature and dispatches the event to the supported payment-flow handlers.
     *
     * @param payload raw webhook payload
     * @param signature Stripe signature header value
     * @param siteId base-site identifier
     */
    @Override
    public void handleWebhook(final String payload, final String signature, final String siteId) {
        try {
            final String resolvedSiteId = StripeWebhookPayloadUtils.resolveVerificationSiteId(payload, siteId);
            final Event event = getStripeClientFactory().constructEvent(payload, signature,
                    getStripeConfigurationService().getWebhookSecret(resolvedSiteId));
            handleEvent(event, resolvedSiteId);
        } catch (final SignatureVerificationException exception) {
            throw new StripeIntegrationException("Stripe webhook signature verification failed.", exception);
        }
    }

    /**
     * Dispatches the webhook event to the supported Stripe resource handlers.
     *
     * @param event Stripe webhook event
     * @param siteId base-site identifier
     */
    protected void handleEvent(final Event event, final String siteId) {
        if (isCheckoutSessionEvent(event)) {
            handleCheckoutSessionEvent(event, extractRequiredSession(event), siteId);
            return;
        }

        if (isPaymentIntentEvent(event)) {
            handlePaymentIntentEvent(event, extractRequiredPaymentIntent(event), siteId);
        }
    }

    /**
     * Extracts a Stripe Checkout Session from the event payload.
     *
     * @param event Stripe webhook event
     * @return Checkout Session payload when present
     */
    protected Optional<Session> extractSession(final Event event) {
        return StripeWebhookPayloadUtils.extractSession(event);
    }

    /**
     * Extracts a Stripe PaymentIntent from the event payload.
     *
     * @param event Stripe webhook event
     * @return PaymentIntent payload when present
     */
    protected Optional<PaymentIntent> extractPaymentIntent(final Event event) {
        return StripeWebhookPayloadUtils.extractPaymentIntent(event);
    }

    /**
     * Returns whether this is a Checkout Session event handled by the connector.
     *
     * @param event Stripe webhook event
     * @return {@code true} when the event requires a Checkout Session payload
     */
    protected boolean isCheckoutSessionEvent(final Event event) {
        return event != null && CHECKOUT_SESSION_EVENT_TYPES.contains(event.getType());
    }

    /**
     * Returns whether this is a PaymentIntent event handled by the connector.
     *
     * @param event Stripe webhook event
     * @return {@code true} when the event requires a PaymentIntent payload
     */
    protected boolean isPaymentIntentEvent(final Event event) {
        return event != null && PAYMENT_INTENT_EVENT_TYPES.contains(event.getType());
    }

    /**
     * Extracts the required Checkout Session payload for supported Checkout Session events.
     *
     * @param event Stripe webhook event
     * @return Checkout Session payload
     */
    protected Session extractRequiredSession(final Event event) {
        return extractSession(event).orElseThrow(() -> createUnsupportedPayloadException(event, "Checkout Session"));
    }

    /**
     * Extracts the required PaymentIntent payload for supported PaymentIntent events.
     *
     * @param event Stripe webhook event
     * @return PaymentIntent payload
     */
    protected PaymentIntent extractRequiredPaymentIntent(final Event event) {
        return extractPaymentIntent(event).orElseThrow(() -> createUnsupportedPayloadException(event, "PaymentIntent"));
    }

    /**
     * Creates a visible failure for supported Stripe events whose object cannot be deserialized.
     *
     * @param event Stripe webhook event
     * @param expectedObjectType expected Stripe object type
     * @return integration exception
     */
    protected StripeIntegrationException createUnsupportedPayloadException(final Event event,
                                                                          final String expectedObjectType) {
        return new StripeIntegrationException("Stripe webhook event " + Objects.toString(event.getId(), "<unknown>")
                + " of type " + Objects.toString(event.getType(), "<unknown>")
                + " did not contain a deserializable " + expectedObjectType + " payload.");
    }

    /**
     * Handles Checkout Session webhook events for the supplied site.
     *
     * @param event Stripe webhook event
     * @param session Stripe Session payload
     * @param siteId base-site identifier
     */
    protected void handleCheckoutSessionEvent(final Event event, final Session session, final String siteId) {
        final String expectedSiteId = StripeMetadataUtils.resolveExpectedSiteId(siteId, session.getMetadata());
        if (!StripeStatusUtils.hasExpectedFlowAndSite(
                session.getMetadata(), StripeServicesConstants.PAYMENT_FLOW_CHECKOUT, expectedSiteId)) {
            return;
        }

        final Optional<AbstractOrderModel> orderOptional = resolveOrder(session);
        if (orderOptional.isEmpty()) {
            return;
        }

        final StripeCheckoutSessionData sessionData = StripeDataConversionUtils.toCheckoutSessionData(session);
        if (StripeStatusUtils.isCheckoutCompletedEvent(event.getType(), session)) {
            getStripePaymentTransactionService().markCheckoutSessionCompleted(orderOptional.get(), sessionData);
        } else if (StripeStatusUtils.isCheckoutExpiredEvent(event.getType(), session)) {
            getStripePaymentTransactionService().markCheckoutSessionExpired(orderOptional.get(), sessionData);
        }
    }

    /**
     * Handles PaymentIntent webhook events for the supplied site.
     *
     * @param event Stripe webhook event
     * @param paymentIntent Stripe PaymentIntent payload
     * @param siteId base-site identifier
     */
    protected void handlePaymentIntentEvent(final Event event, final PaymentIntent paymentIntent, final String siteId) {
        final String expectedSiteId = StripeMetadataUtils.resolveExpectedSiteId(siteId, paymentIntent.getMetadata());
        if (!StripeStatusUtils.hasExpectedFlowAndSite(
                paymentIntent.getMetadata(), StripeServicesConstants.PAYMENT_FLOW_ELEMENTS, expectedSiteId)) {
            return;
        }

        final Optional<AbstractOrderModel> orderOptional = resolveOrder(paymentIntent);
        if (orderOptional.isEmpty()) {
            return;
        }

        final StripePaymentIntentData paymentIntentData = StripeDataConversionUtils.toPaymentIntentData(paymentIntent);
        if (StripeStatusUtils.isPaymentIntentSucceededEvent(event.getType(), paymentIntent)) {
            getStripePaymentTransactionService().markPaymentIntentSucceeded(orderOptional.get(), paymentIntentData);
        } else if (StripeStatusUtils.isPaymentIntentFailedEvent(event.getType(), paymentIntent)) {
            getStripePaymentTransactionService().markPaymentIntentFailed(orderOptional.get(), paymentIntentData);
        }
    }

    /**
     * Resolves the owning order for a Checkout Session event.
     *
     * @param session Stripe Session payload
     * @return owning order when one is registered
     */
    protected Optional<AbstractOrderModel> resolveOrder(final Session session) {
        final String orderCode = StripeDataConversionUtils.resolveOrderCode(session);
        if (StringUtils.isBlank(orderCode)) {
            return Optional.empty();
        }
        return getStripePaymentTransactionService().findOrderByPaymentReference(orderCode, session.getId());
    }

    /**
     * Resolves the owning order for a PaymentIntent event.
     *
     * @param paymentIntent Stripe PaymentIntent payload
     * @return owning order when one is registered
     */
    protected Optional<AbstractOrderModel> resolveOrder(final PaymentIntent paymentIntent) {
        final String orderCode = StripeDataConversionUtils.resolveOrderCode(paymentIntent);
        if (StringUtils.isBlank(orderCode)) {
            return Optional.empty();
        }
        return getStripePaymentTransactionService().findOrderByPaymentReference(orderCode, paymentIntent.getId());
    }

    protected StripeClientFactory getStripeClientFactory() {
        return stripeClientFactory;
    }

    protected StripeConfigurationService getStripeConfigurationService() {
        return stripeConfigurationService;
    }

    protected StripePaymentTransactionService getStripePaymentTransactionService() {
        return stripePaymentTransactionService;
    }
}

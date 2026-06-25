
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.data.StripeRefundData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentLifecycleService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.cci.hybris.stripe.services.util.StripeAmountUtils;
import com.cci.hybris.stripe.services.util.StripeDataConversionUtils;
import com.cci.hybris.stripe.services.util.StripeMetadataUtils;
import com.cci.hybris.stripe.services.util.StripeOrderUtils;
import com.cci.hybris.stripe.services.util.StripePaymentReferenceUtils;
import com.cci.hybris.stripe.services.util.StripePaymentTransactionUtils;
import com.cci.hybris.stripe.services.util.StripeValidationUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionExpireParams;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.math.BigDecimal;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Default provider-side Stripe lifecycle service.
 */
public class DefaultStripePaymentLifecycleService implements StripePaymentLifecycleService {

    private static final String ORDER_CONTEXT_REQUIRED_MESSAGE =
            "Stripe lifecycle operations require a calculated order with site and currency.";

    private final StripeClientFactory stripeClientFactory;
    private final StripeConfigurationService stripeConfigurationService;
    private final StripePaymentTransactionService stripePaymentTransactionService;

    /**
     * Creates the service with the collaborators required for provider-side Stripe lifecycle operations.
     *
     * @param stripeClientFactory Stripe client factory
     * @param stripeConfigurationService Stripe configuration service
     * @param stripePaymentTransactionService transaction service
     */
    public DefaultStripePaymentLifecycleService(final StripeClientFactory stripeClientFactory,
                                                final StripeConfigurationService stripeConfigurationService,
                                                final StripePaymentTransactionService stripePaymentTransactionService) {
        this.stripeClientFactory = stripeClientFactory;
        this.stripeConfigurationService = stripeConfigurationService;
        this.stripePaymentTransactionService = stripePaymentTransactionService;
    }

    /**
     * Expires the Stripe Checkout Session for the supplied order after validating ownership metadata.
     *
     * @param order order that owns the Checkout Session
     * @param sessionId Stripe Checkout Session identifier
     * @return updated Checkout Session data
     */
    @Override
    public StripeCheckoutSessionData expireCheckoutSession(final AbstractOrderModel order, final String sessionId) {
        StripeOrderUtils.validateOrderWithContext(order, ORDER_CONTEXT_REQUIRED_MESSAGE);
        try {
            final String secretKey = getStripeConfigurationService().getSecretKey(order.getSite().getUid());
            final Session existingSession = getStripeClientFactory().getCheckoutSession(secretKey, sessionId);
            validateCheckoutSessionOwnership(order, existingSession);
            final Session session = getStripeClientFactory().expireCheckoutSession(secretKey, sessionId, SessionExpireParams.builder().build());
            final StripeCheckoutSessionData sessionData = StripeDataConversionUtils.toCheckoutSessionData(session);
            getStripePaymentTransactionService().markCheckoutSessionExpired(order, sessionData);
            return sessionData;
        } catch (final StripeException exception) {
            throw new StripeIntegrationException("Unable to expire Stripe Checkout Session " + sessionId, exception);
        }
    }

    /**
     * Cancels the Stripe PaymentIntent for the supplied order after validating ownership metadata.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @return updated PaymentIntent data
     */
    @Override
    public StripePaymentIntentData cancelPaymentIntent(final AbstractOrderModel order, final String paymentIntentId) {
        StripeOrderUtils.validateOrderWithContext(order, ORDER_CONTEXT_REQUIRED_MESSAGE);
        try {
            final String secretKey = getStripeConfigurationService().getSecretKey(order.getSite().getUid());
            final PaymentIntent existingPaymentIntent = getStripeClientFactory().getPaymentIntent(secretKey, paymentIntentId);
            validatePaymentIntentOwnership(order, existingPaymentIntent);
            final PaymentIntent paymentIntent = getStripeClientFactory().cancelPaymentIntent(secretKey, paymentIntentId,
                    PaymentIntentCancelParams.builder()
                            .setCancellationReason(PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER)
                            .build());
            final StripePaymentIntentData paymentIntentData = StripeDataConversionUtils.toPaymentIntentData(paymentIntent);
            getStripePaymentTransactionService().markPaymentIntentCancelled(order, paymentIntentData);
            return paymentIntentData;
        } catch (final StripeException exception) {
            throw new StripeIntegrationException("Unable to cancel Stripe PaymentIntent " + paymentIntentId, exception);
        }
    }

    /**
     * Creates a Stripe refund for the supplied payment reference and stores the refund state locally.
     *
     * @param order order that owns the payment reference
     * @param paymentReference Stripe Checkout Session or PaymentIntent identifier
     * @param amount refund amount in major units, when partial
     * @return Stripe refund data
     */
    @Override
    public StripeRefundData createRefund(final AbstractOrderModel order,
                                         final String paymentReference,
                                         final BigDecimal amount) {
        StripeOrderUtils.validateOrderWithContext(order, ORDER_CONTEXT_REQUIRED_MESSAGE);
        StripeValidationUtils.validatePositiveAmount(amount, "Stripe refund amount must be positive when provided.");

        try {
            final String secretKey = getStripeConfigurationService().getSecretKey(order.getSite().getUid());
            final String paymentIntentId = resolveRefundPaymentIntentId(secretKey, paymentReference);
            validateRefundOwnership(order, paymentReference, paymentIntentId, secretKey);
            final Refund refund = getStripeClientFactory().createRefund(secretKey,
                    buildRefundCreateParams(order, paymentIntentId, StripePaymentReferenceUtils.resolvePaymentFlow(paymentReference),
                            amount));
            final StripeRefundData refundData = StripeDataConversionUtils.toRefundData(refund);
            getStripePaymentTransactionService().registerRefund(order, paymentReference, refundData);
            return refundData;
        } catch (final StripeException exception) {
            throw new StripeIntegrationException("Unable to create Stripe refund for " + paymentReference, exception);
        }
    }

    /**
     * Builds the Stripe refund create payload for the supplied order and payment reference.
     *
     * @param order refunded order
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @param paymentFlow Stripe payment flow identifier
     * @param amount optional refund amount
     * @return Stripe refund create params
     */
    protected RefundCreateParams buildRefundCreateParams(final AbstractOrderModel order,
                                                         final String paymentIntentId,
                                                         final String paymentFlow,
                                                         final BigDecimal amount) {
        final RefundCreateParams.Builder builder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .putMetadata(StripeServicesConstants.METADATA_ORDER_CODE, order.getCode())
                .putMetadata(StripeServicesConstants.METADATA_SITE_UID, order.getSite().getUid())
                .putMetadata(StripeServicesConstants.METADATA_PAYMENT_FLOW, paymentFlow)
                .putMetadata(StripeServicesConstants.METADATA_ORDER_TYPE, order.getClass().getSimpleName());

        if (amount != null) {
            builder.setAmount(StripeAmountUtils.toMinorUnits(amount, order.getCurrency().getDigits()));
        }

        return builder.build();
    }

    /**
     * Resolves the PaymentIntent identifier to refund from the supplied Stripe payment reference.
     *
     * @param secretKey Stripe secret key
     * @param paymentReference Stripe Checkout Session or PaymentIntent identifier
     * @return Stripe PaymentIntent identifier
     * @throws StripeException when Stripe retrieval fails
     */
    protected String resolveRefundPaymentIntentId(final String secretKey, final String paymentReference) throws StripeException {
        if (StripePaymentReferenceUtils.isPaymentIntentReference(paymentReference)) {
            return paymentReference;
        }

        final Session session = getStripeClientFactory().getCheckoutSession(secretKey, paymentReference);
        if (StringUtils.isBlank(session.getPaymentIntent())) {
            throw new StripeIntegrationException("Stripe Checkout Session " + paymentReference + " has no PaymentIntent to refund.");
        }
        return session.getPaymentIntent();
    }

    /**
     * Validates that the refund payment reference belongs to the supplied order.
     *
     * @param order owning order
     * @param paymentReference Stripe Checkout Session or PaymentIntent identifier
     * @param paymentIntentId resolved PaymentIntent identifier
     * @param secretKey Stripe secret key
     * @throws StripeException when Stripe retrieval fails
     */
    protected void validateRefundOwnership(final AbstractOrderModel order,
                                           final String paymentReference,
                                           final String paymentIntentId,
                                           final String secretKey) throws StripeException {
        if (getStripePaymentTransactionService().hasPaymentTransactionEntry(order, paymentReference)
                || getStripePaymentTransactionService().hasPaymentTransactionEntry(order, paymentIntentId)
                || hasCheckoutSessionReference(order, secretKey, paymentIntentId)) {
            return;
        }

        throw new StripeIntegrationException("Stripe payment reference " + paymentReference
                + " is not registered for " + order.getCode());
    }

    /**
     * Validates that the Checkout Session belongs to the supplied order.
     *
     * @param order owning order
     * @param session Stripe Session payload
     */
    protected void validateCheckoutSessionOwnership(final AbstractOrderModel order, final Session session) {
        StripeOrderUtils.validateOrderWithContext(order, ORDER_CONTEXT_REQUIRED_MESSAGE);
        if (session == null) {
            throw new StripeIntegrationException("Stripe Checkout Session payload is required for " + order.getCode());
        }

        if ((getStripePaymentTransactionService().hasPaymentTransactionEntry(order, session.getId())
                && StripeMetadataUtils.hasExpectedSite(session.getMetadata(), order.getSite().getUid()))
                || StripeMetadataUtils.matchesCheckoutSessionOwnership(order, session)) {
            return;
        }

        throw new StripeIntegrationException("Stripe Checkout Session " + session.getId()
                + " is not registered for " + order.getCode());
    }

    /**
     * Validates that the PaymentIntent belongs to the supplied order.
     *
     * @param order owning order
     * @param paymentIntent Stripe PaymentIntent payload
     */
    protected void validatePaymentIntentOwnership(final AbstractOrderModel order, final PaymentIntent paymentIntent) {
        StripeOrderUtils.validateOrderWithContext(order, ORDER_CONTEXT_REQUIRED_MESSAGE);
        if (paymentIntent == null) {
            throw new StripeIntegrationException("Stripe PaymentIntent payload is required for " + order.getCode());
        }

        if ((getStripePaymentTransactionService().hasPaymentTransactionEntry(order, paymentIntent.getId())
                && StripeMetadataUtils.hasExpectedSite(paymentIntent.getMetadata(), order.getSite().getUid()))
                || StripeMetadataUtils.matchesPaymentIntentOwnership(order, paymentIntent)) {
            return;
        }

        throw new StripeIntegrationException("Stripe PaymentIntent " + paymentIntent.getId()
                + " is not registered for " + order.getCode());
    }

    /**
     * Returns whether any Checkout Session registered on the order points to the supplied PaymentIntent.
     *
     * @param order owning order
     * @param secretKey Stripe secret key
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @return {@code true} when a matching Checkout Session exists
     * @throws StripeException when Stripe retrieval fails
     */
    protected boolean hasCheckoutSessionReference(final AbstractOrderModel order,
                                                  final String secretKey,
                                                  final String paymentIntentId) throws StripeException {
        for (final String requestId : StripePaymentTransactionUtils.collectCheckoutSessionRequestIds(order)) {
            final Session session = getStripeClientFactory().getCheckoutSession(secretKey, requestId);
            if (Objects.equals(paymentIntentId, session.getPaymentIntent())) {
                return true;
            }
        }
        return false;
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

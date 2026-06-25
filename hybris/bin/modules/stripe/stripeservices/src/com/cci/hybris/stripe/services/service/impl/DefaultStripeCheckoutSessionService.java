
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeCheckoutSessionService;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.cci.hybris.stripe.services.util.StripeAmountUtils;
import com.cci.hybris.stripe.services.util.StripeDataConversionUtils;
import com.cci.hybris.stripe.services.util.StripeMetadataUtils;
import com.cci.hybris.stripe.services.util.StripeOrderUtils;
import com.cci.hybris.stripe.services.util.StripeUrlUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.math.BigDecimal;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * Default Stripe Checkout Session service.
 */
public class DefaultStripeCheckoutSessionService implements StripeCheckoutSessionService {

    private final StripeClientFactory stripeClientFactory;
    private final StripeConfigurationService stripeConfigurationService;
    private final StripePaymentTransactionService stripePaymentTransactionService;

    /**
     * Creates the service with the collaborators required to create and retrieve Stripe Checkout Sessions.
     *
     * @param stripeClientFactory Stripe client factory
     * @param stripeConfigurationService Stripe configuration service
     * @param stripePaymentTransactionService transaction service
     */
    public DefaultStripeCheckoutSessionService(final StripeClientFactory stripeClientFactory,
                                               final StripeConfigurationService stripeConfigurationService,
                                               final StripePaymentTransactionService stripePaymentTransactionService) {
        this.stripeClientFactory = stripeClientFactory;
        this.stripeConfigurationService = stripeConfigurationService;
        this.stripePaymentTransactionService = stripePaymentTransactionService;
    }

    /**
     * Creates a Stripe Checkout Session for the supplied order and registers its authorization state locally.
     *
     * @param order order to register at Stripe
     * @return created Checkout Session data
     */
    @Override
    public StripeCheckoutSessionData createCheckoutSession(final AbstractOrderModel order) {
        StripeOrderUtils.validateOrderWithContext(order,
                "Stripe Checkout Session requires a calculated order with site and currency.");

        final String siteId = order.getSite().getUid();
        final String secretKey = getStripeConfigurationService().getSecretKey(siteId);
        final SessionCreateParams params = buildSessionCreateParams(order, siteId);

        try {
            final Session session = getStripeClientFactory().createCheckoutSession(secretKey, params);
            final StripeCheckoutSessionData sessionData = StripeDataConversionUtils.toCheckoutSessionData(session);
            getStripePaymentTransactionService().registerCheckoutSession(order, sessionData);
            return sessionData;
        } catch (final StripeException exception) {
            throw new StripeIntegrationException("Unable to create Stripe Checkout Session for " + order.getCode(), exception);
        }
    }
    @Override
    public StripeCheckoutSessionData getCheckoutSession(final String sessionId, final String siteId, final String orderCode) {
        try {
            final Session session = getStripeClientFactory().getCheckoutSession(
                    getStripeConfigurationService().getSecretKey(siteId), sessionId);
            validateSessionOwnership(sessionId, session, siteId, orderCode);
            return StripeDataConversionUtils.toCheckoutSessionData(session);
        } catch (final StripeException exception) {
            throw new StripeIntegrationException("Unable to retrieve Stripe Checkout Session " + sessionId, exception);
        }
    }

    /**
     * Builds the Stripe Checkout Session create payload for the supplied order.
     *
     * @param order order to register at Stripe
     * @param siteId base-site identifier
     * @return Checkout Session create params
     */
    protected SessionCreateParams buildSessionCreateParams(final AbstractOrderModel order, final String siteId) {
        final BigDecimal totalWithTax = StripeAmountUtils.calculateOrderTotal(order);
        final long amountInMinor = StripeAmountUtils.toMinorUnits(totalWithTax, order.getCurrency().getDigits());

        final SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(StripeServicesConstants.ORDER_DESCRIPTION_PREFIX + order.getCode())
                        .build();

        final SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(order.getCurrency().getIsocode().toLowerCase(Locale.ROOT))
                        .setUnitAmount(amountInMinor)
                        .setProductData(productData)
                        .build();

        final SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(order.getCode())
                .setSuccessUrl(StripeUrlUtils.appendContextParameters(
                        getStripeConfigurationService().getSuccessUrl(siteId), order))
                .setCancelUrl(StripeUrlUtils.appendContextParameters(
                        getStripeConfigurationService().getCancelUrl(siteId), order))
                .putMetadata(StripeServicesConstants.METADATA_ORDER_CODE, order.getCode())
                .putMetadata(StripeServicesConstants.METADATA_SITE_UID, siteId)
                .putMetadata(StripeServicesConstants.METADATA_ORDER_TYPE, order.getClass().getSimpleName())
                .putMetadata(StripeServicesConstants.METADATA_PAYMENT_FLOW, StripeServicesConstants.PAYMENT_FLOW_CHECKOUT)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(priceData)
                        .build());

        final String customerEmail = StripeOrderUtils.extractCustomerEmail(order);
        if (StringUtils.isNotBlank(customerEmail)) {
            builder.setCustomerEmail(customerEmail);
        }

        return builder.build();
    }

    /**
     * Validates that the Checkout Session belongs to the expected site.
     *
     * @param sessionId Stripe Checkout Session identifier
     * @param session Stripe Session payload
     * @param siteId expected base-site identifier
     * @param orderCode expected order or cart code
     */
    protected void validateSessionOwnership(final String sessionId,
                                            final Session session,
                                            final String siteId,
                                            final String orderCode) {
        if (session == null) {
            throw new StripeIntegrationException("Stripe Checkout Session payload is required for " + sessionId);
        }

        if (!StripeMetadataUtils.hasExpectedSite(session.getMetadata(), siteId)) {
            throw new StripeIntegrationException("Stripe Checkout Session " + sessionId
                    + " is not registered for site " + siteId);
        }

        final String sessionOrderCode = StripeDataConversionUtils.resolveOrderCode(session);
        if (StringUtils.isBlank(sessionOrderCode)) {
            throw new StripeIntegrationException("Stripe Checkout Session " + sessionId
                    + " is missing the registered cart or order reference.");
        }

        if (StringUtils.isNotBlank(orderCode) && orderCode.equals(sessionOrderCode)) {
            return;
        }

        if (getStripePaymentTransactionService().findOrderByPaymentReference(sessionOrderCode, sessionId).isPresent()) {
            return;
        }

        throw new StripeIntegrationException("Stripe Checkout Session " + sessionId
                + " is not registered for cart or order " + sessionOrderCode);
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

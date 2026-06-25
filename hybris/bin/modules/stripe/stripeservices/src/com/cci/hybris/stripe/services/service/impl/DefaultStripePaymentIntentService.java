
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentIntentService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.cci.hybris.stripe.services.util.StripeAmountUtils;
import com.cci.hybris.stripe.services.util.StripeDataConversionUtils;
import com.cci.hybris.stripe.services.util.StripeMetadataUtils;
import com.cci.hybris.stripe.services.util.StripeOrderUtils;
import com.cci.hybris.stripe.services.util.StripePaymentIntentUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Default Stripe PaymentIntent service for Payment Elements.
 */
public class DefaultStripePaymentIntentService implements StripePaymentIntentService {

    private final StripeClientFactory stripeClientFactory;
    private final StripeConfigurationService stripeConfigurationService;
    private final StripePaymentTransactionService stripePaymentTransactionService;

    /**
     * Creates the service with the collaborators required to manage Stripe PaymentIntents.
     *
     * @param stripeClientFactory Stripe client factory
     * @param stripeConfigurationService Stripe configuration service
     * @param stripePaymentTransactionService payment transaction service
     */
    public DefaultStripePaymentIntentService(final StripeClientFactory stripeClientFactory,
                                             final StripeConfigurationService stripeConfigurationService,
                                             final StripePaymentTransactionService stripePaymentTransactionService) {
        this.stripeClientFactory = stripeClientFactory;
        this.stripeConfigurationService = stripeConfigurationService;
        this.stripePaymentTransactionService = stripePaymentTransactionService;
    }

    /**
     * Creates a new Stripe PaymentIntent or updates the reusable one that belongs to the supplied order.
     *
     * @param order order to register at Stripe
     * @return PaymentIntent data
     */
    @Override
    public StripePaymentIntentData createOrUpdatePaymentIntent(final AbstractOrderModel order) {
        StripeOrderUtils.validateOrderWithContext(order,
                "Stripe PaymentIntent requires a calculated order with site and currency.");

        final String siteId = order.getSite().getUid();
        final String secretKey = getStripeConfigurationService().getSecretKey(siteId);
        final long amountInMinor = StripeAmountUtils.toMinorUnits(
                StripeAmountUtils.calculateOrderTotal(order), order.getCurrency().getDigits());
        final String currency = order.getCurrency().getIsocode().toLowerCase(Locale.ROOT);

        try {
            final Optional<String> existingPaymentIntentId =
                    getStripePaymentTransactionService().findLatestOpenPaymentIntentId(order);
            if (existingPaymentIntentId.isPresent()) {
                final PaymentIntent existingPaymentIntent =
                        getStripeClientFactory().getPaymentIntent(secretKey, existingPaymentIntentId.get());
                if (StripePaymentIntentUtils.shouldReuse(existingPaymentIntent, amountInMinor, currency)) {
                    final StripePaymentIntentData paymentIntentData =
                            StripeDataConversionUtils.toPaymentIntentData(existingPaymentIntent);
                    getStripePaymentTransactionService().registerPaymentIntent(order, paymentIntentData);
                    return paymentIntentData;
                }

                if (StripePaymentIntentUtils.shouldUpdate(existingPaymentIntent)) {
                    final PaymentIntent updatedPaymentIntent = getStripeClientFactory().updatePaymentIntent(secretKey,
                            existingPaymentIntent.getId(), buildUpdateParams(order, amountInMinor, currency, siteId));
                    final StripePaymentIntentData paymentIntentData =
                            StripeDataConversionUtils.toPaymentIntentData(updatedPaymentIntent);
                    getStripePaymentTransactionService().registerPaymentIntent(order, paymentIntentData);
                    return paymentIntentData;
                }
            }

            final PaymentIntent paymentIntent = getStripeClientFactory().createPaymentIntent(secretKey,
                    buildCreateParams(order, amountInMinor, currency, siteId));
            final StripePaymentIntentData paymentIntentData = StripeDataConversionUtils.toPaymentIntentData(paymentIntent);
            getStripePaymentTransactionService().registerPaymentIntent(order, paymentIntentData);
            return paymentIntentData;
        } catch (final StripeException exception) {
            throw new StripeIntegrationException("Unable to create Stripe PaymentIntent for " + order.getCode(), exception);
        }
    }
    @Override
    public StripePaymentIntentData getPaymentIntent(final AbstractOrderModel order,
                                                    final String paymentIntentId,
                                                    final String siteId) {
        final PaymentIntent paymentIntent = retrievePaymentIntent(paymentIntentId, siteId);
        validateOwnership(order, paymentIntent, siteId);
        final StripePaymentIntentData paymentIntentData = StripeDataConversionUtils.toPaymentIntentData(paymentIntent);
        getStripePaymentTransactionService().registerPaymentIntent(order, paymentIntentData);
        return paymentIntentData;
    }
    @Override
    public StripePaymentIntentData getPaymentIntentForSite(final String paymentIntentId, final String siteId) {
        return StripeDataConversionUtils.toPaymentIntentData(retrievePaymentIntent(paymentIntentId, siteId));
    }

    /**
     * Retrieves a Stripe PaymentIntent for the supplied site.
     *
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @param siteId base-site identifier
     * @return Stripe PaymentIntent payload
     */
    protected PaymentIntent retrievePaymentIntent(final String paymentIntentId, final String siteId) {
        try {
            return getStripeClientFactory().getPaymentIntent(getStripeConfigurationService().getSecretKey(siteId), paymentIntentId);
        } catch (final StripeException exception) {
            throw new StripeIntegrationException("Unable to retrieve Stripe PaymentIntent " + paymentIntentId, exception);
        }
    }

    /**
     * Builds the Stripe PaymentIntent create payload for the supplied order.
     *
     * @param order order to register at Stripe
     * @param amountInMinor order total in minor units
     * @param currency Stripe currency code
     * @param siteId base-site identifier
     * @return PaymentIntent create params
     */
    protected PaymentIntentCreateParams buildCreateParams(final AbstractOrderModel order,
                                                          final long amountInMinor,
                                                          final String currency,
                                                          final String siteId) {
        final PaymentIntentCreateParams.Builder builder = PaymentIntentCreateParams.builder()
                .setAmount(amountInMinor)
                .setCurrency(currency)
                .setDescription(StripeServicesConstants.ORDER_DESCRIPTION_PREFIX + order.getCode())
                .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build())
                .putMetadata(StripeServicesConstants.METADATA_ORDER_CODE, order.getCode())
                .putMetadata(StripeServicesConstants.METADATA_SITE_UID, siteId)
                .putMetadata(StripeServicesConstants.METADATA_ORDER_TYPE, order.getClass().getSimpleName())
                .putMetadata(StripeServicesConstants.METADATA_PAYMENT_FLOW, StripeServicesConstants.PAYMENT_FLOW_ELEMENTS);

        final String customerEmail = StripeOrderUtils.extractCustomerEmail(order);
        if (StringUtils.isNotBlank(customerEmail)) {
            builder.setReceiptEmail(customerEmail);
        }

        return builder.build();
    }

    /**
     * Builds the Stripe PaymentIntent update payload for the supplied order.
     *
     * @param order order to register at Stripe
     * @param amountInMinor order total in minor units
     * @param currency Stripe currency code
     * @param siteId base-site identifier
     * @return PaymentIntent update params
     */
    protected PaymentIntentUpdateParams buildUpdateParams(final AbstractOrderModel order,
                                                          final long amountInMinor,
                                                          final String currency,
                                                          final String siteId) {
        final PaymentIntentUpdateParams.Builder builder = PaymentIntentUpdateParams.builder()
                .setAmount(amountInMinor)
                .setCurrency(currency)
                .setDescription(StripeServicesConstants.ORDER_DESCRIPTION_PREFIX + order.getCode())
                .putMetadata(StripeServicesConstants.METADATA_ORDER_CODE, order.getCode())
                .putMetadata(StripeServicesConstants.METADATA_SITE_UID, siteId)
                .putMetadata(StripeServicesConstants.METADATA_ORDER_TYPE, order.getClass().getSimpleName())
                .putMetadata(StripeServicesConstants.METADATA_PAYMENT_FLOW, StripeServicesConstants.PAYMENT_FLOW_ELEMENTS);

        final String customerEmail = StripeOrderUtils.extractCustomerEmail(order);
        if (StringUtils.isNotBlank(customerEmail)) {
            builder.setReceiptEmail(customerEmail);
        }

        return builder.build();
    }

    /**
     * Validates that the PaymentIntent belongs to the supplied order and site.
     *
     * @param order owning order
     * @param paymentIntent Stripe PaymentIntent payload
     * @param siteId expected base-site identifier
     */
    protected void validateOwnership(final AbstractOrderModel order,
                                     final PaymentIntent paymentIntent,
                                     final String siteId) {
        StripeOrderUtils.validateOrderWithContext(order,
                "Stripe PaymentIntent requires a calculated order with site and currency.");
        if (paymentIntent == null) {
            throw new StripeIntegrationException("Stripe PaymentIntent payload is required for " + order.getCode());
        }

        final String expectedSiteId = StripeMetadataUtils.resolveExpectedSiteId(siteId, order);
        if ((getStripePaymentTransactionService().hasPaymentTransactionEntry(order, paymentIntent.getId())
                && StripeMetadataUtils.hasExpectedSite(paymentIntent.getMetadata(), expectedSiteId))
                || StripeMetadataUtils.matchesOrderAndSite(paymentIntent.getMetadata(), order.getCode(), expectedSiteId)) {
            return;
        }

        throw new StripeIntegrationException("Stripe PaymentIntent " + paymentIntent.getId()
                + " is not registered for " + order.getCode());
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

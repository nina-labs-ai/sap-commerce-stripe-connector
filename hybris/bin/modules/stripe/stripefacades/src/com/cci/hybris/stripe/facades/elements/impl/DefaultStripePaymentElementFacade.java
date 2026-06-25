
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.elements.impl;

import com.cci.hybris.stripe.facades.constants.StripeFacadesConstants;
import com.cci.hybris.stripe.facades.data.StripePaymentElementFacadeData;
import com.cci.hybris.stripe.facades.elements.StripePaymentElementFacade;
import com.cci.hybris.stripe.facades.support.AbstractStripeCheckoutFacadeSupport;
import com.cci.hybris.stripe.facades.util.StripeFacadeAmountUtils;
import com.cci.hybris.stripe.facades.util.StripePaymentElementFacadeUtils;
import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentLifecycleService;
import com.cci.hybris.stripe.services.service.StripePaymentIntentService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.opffacades.calculators.CalculatorStrategy;
import de.hybris.platform.order.InvalidCartException;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Default facade for Stripe Payment Elements.
 */
public class DefaultStripePaymentElementFacade extends AbstractStripeCheckoutFacadeSupport
        implements StripePaymentElementFacade {

    private StripePaymentIntentService stripePaymentIntentService;
    private StripeConfigurationService stripeConfigurationService;
    private CalculatorStrategy calculatorStrategy;
    private StripePaymentLifecycleService stripePaymentLifecycleService;
    private StripePaymentTransactionService stripePaymentTransactionService;

    public void setStripePaymentIntentService(final StripePaymentIntentService stripePaymentIntentService) {
        this.stripePaymentIntentService = stripePaymentIntentService;
    }

    public void setStripeConfigurationService(final StripeConfigurationService stripeConfigurationService) {
        this.stripeConfigurationService = stripeConfigurationService;
    }

    public void setCalculatorStrategy(final CalculatorStrategy calculatorStrategy) {
        this.calculatorStrategy = calculatorStrategy;
    }

    public void setStripePaymentLifecycleService(final StripePaymentLifecycleService stripePaymentLifecycleService) {
        this.stripePaymentLifecycleService = stripePaymentLifecycleService;
    }

    public void setStripePaymentTransactionService(
            final StripePaymentTransactionService stripePaymentTransactionService) {
        this.stripePaymentTransactionService = stripePaymentTransactionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StripePaymentElementFacadeData createPaymentIntentForCart() {
        final CartModel cart = getSessionCart();
        return convert(cart, getStripePaymentIntentService().createOrUpdatePaymentIntent(cart));
    }
    @Override
    public StripePaymentElementFacadeData getPaymentIntent(final String paymentIntentId) {
        final CartModel cart = getSessionCart();
        return convert(cart, getStripePaymentIntentService().getPaymentIntent(cart, paymentIntentId, getCurrentSiteId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StripePaymentElementFacadeData cancelPaymentIntentForCart(final String paymentIntentId) {
        final CartModel cart = getSessionCart();
        return convert(cart, getStripePaymentLifecycleService().cancelPaymentIntent(cart, paymentIntentId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderData finalizePaymentIntentForContext(final String paymentIntentId,
                                                     final String orderCode) throws InvalidCartException {
        final AbstractOrderModel checkoutOwner = resolveCheckoutOwner(paymentIntentId, orderCode);
        if (checkoutOwner instanceof OrderModel orderModel) {
            return getOrderData(orderModel);
        }

        final StripePaymentIntentData paymentIntentData = getStripePaymentIntentService().getPaymentIntent(checkoutOwner,
                paymentIntentId, getCurrentSiteId());
        if (!StripePaymentElementFacadeUtils.isFinalizablePaymentIntent(paymentIntentData)) {
            throw new StripeIntegrationException("Stripe PaymentIntent is not ready for SAP Commerce order placement.");
        }

        getStripePaymentTransactionService().markPaymentIntentSucceeded(checkoutOwner, paymentIntentData);
        return placeOrder((CartModel) checkoutOwner);
    }

    /**
     * Converts Stripe PaymentIntent service data into facade data.
     *
     * @param order order used for formatting contextual values
     * @param source Stripe PaymentIntent service data
     * @return facade data
     */
    protected StripePaymentElementFacadeData convert(final AbstractOrderModel order,
                                                     final StripePaymentIntentData source) {
        final String siteId = getCurrentSiteId();
        final String formattedAmount = order == null ? null : formatAmount(order);
        return StripePaymentElementFacadeUtils.toFacadeData(
                source,
                getStripeConfigurationService().getPublishableKey(siteId),
                StripeServicesConstants.PAYMENT_OPTION_ID_ELEMENTS,
                StripeServicesConstants.PAYMENT_METHOD_ELEMENTS,
                getStripeConfigurationService().getElementsReturnUrl(siteId),
                formattedAmount);
    }

    /**
     * Formats the cart total for storefront presentation.
     *
     * @param order order to format
     * @return formatted amount string
     */
    protected String formatAmount(final AbstractOrderModel order) {
        final PriceData priceData = getCalculatorStrategy().createPrice(order,
                StripeFacadeAmountUtils.resolveDisplayAmount(order));
        return priceData == null ? null : priceData.getFormattedValue();
    }

    /**
     * Resolves the cart or order that owns the Stripe PaymentIntent.
     *
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @param orderCode expected cart or order code
     * @return owning cart or order
     */
    protected AbstractOrderModel resolveCheckoutOwner(final String paymentIntentId, final String orderCode) {
        final CartModel sessionCart = getSessionCartIfAvailable();
        if (sessionCart != null
                && (getStripePaymentTransactionService().hasPaymentTransactionEntry(sessionCart, paymentIntentId)
                || matchesCheckoutContext(sessionCart, orderCode))) {
            return sessionCart;
        }

        final Optional<AbstractOrderModel> requestIdMatch =
                getStripePaymentTransactionService().findOrderByPaymentReference(paymentIntentId);
        if (requestIdMatch.isPresent()) {
            return requestIdMatch.get();
        }

        final String contextCode = StringUtils.defaultIfBlank(orderCode, getSessionCartCode());
        if (StringUtils.isBlank(contextCode)) {
            throw new StripeIntegrationException("Stripe PaymentIntent " + paymentIntentId
                    + " requires the original cart or order reference.");
        }

        return getStripePaymentTransactionService().findOrderByPaymentReference(contextCode, paymentIntentId)
                .or(() -> sessionCart == null ? Optional.empty()
                        : getStripePaymentTransactionService().findOrderByPaymentReference(sessionCart.getCode(), paymentIntentId))
                .or(() -> getStripePaymentTransactionService().findOrderByCode(contextCode))
                .or(() -> findCheckoutOwnerFromStripeMetadata(paymentIntentId))
                .orElseThrow(
                        () -> new StripeIntegrationException("No SAP Commerce cart or order matches the Stripe PaymentIntent."));
    }

    /**
     * Returns whether the restored checkout cart matches the caller-provided cart reference.
     *
     * @param cart restored session cart
     * @param orderCode caller-provided cart or order reference
     * @return {@code true} when the reference matches the cart code or guid
     */
    protected boolean matchesCheckoutContext(final CartModel cart, final String orderCode) {
        return StripePaymentElementFacadeUtils.matchesCheckoutContext(cart, orderCode);
    }

    /**
     * Resolves the checkout owner from the Stripe PaymentIntent metadata when local request-id lookups are unavailable.
     *
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @return owning cart or order, when the metadata exposes a matching local code
     */
    protected Optional<AbstractOrderModel> findCheckoutOwnerFromStripeMetadata(final String paymentIntentId) {
        final StripePaymentIntentData paymentIntentData =
                getStripePaymentIntentService().getPaymentIntentForSite(paymentIntentId, getCurrentSiteId());
        return StringUtils.isBlank(paymentIntentData.getClientReferenceId())
                ? Optional.empty()
                : getStripePaymentTransactionService().findOrderByCode(paymentIntentData.getClientReferenceId());
    }

    protected StripePaymentIntentService getStripePaymentIntentService() {
        return stripePaymentIntentService;
    }

    protected StripeConfigurationService getStripeConfigurationService() {
        return stripeConfigurationService;
    }

    protected CalculatorStrategy getCalculatorStrategy() {
        return calculatorStrategy;
    }

    protected StripePaymentLifecycleService getStripePaymentLifecycleService() {
        return stripePaymentLifecycleService;
    }

    protected StripePaymentTransactionService getStripePaymentTransactionService() {
        return stripePaymentTransactionService;
    }

    @Override
    protected String getNoSessionCartMessage() {
        return StripeFacadesConstants.MESSAGE_NO_SESSION_CART_FOR_PAYMENT_ELEMENTS;
    }

    @Override
    protected String getNoCurrentBaseSiteMessage() {
        return StripeFacadesConstants.MESSAGE_NO_BASE_SITE_FOR_PAYMENT_ELEMENTS;
    }
}

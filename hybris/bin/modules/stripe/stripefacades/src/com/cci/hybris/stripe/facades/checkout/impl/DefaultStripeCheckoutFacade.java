
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.checkout.impl;

import com.cci.hybris.stripe.facades.checkout.StripeCheckoutFacade;
import com.cci.hybris.stripe.facades.constants.StripeFacadesConstants;
import com.cci.hybris.stripe.facades.data.StripeCheckoutSessionFacadeData;
import com.cci.hybris.stripe.facades.data.StripePublicConfigurationFacadeData;
import com.cci.hybris.stripe.facades.support.AbstractStripeCheckoutFacadeSupport;
import com.cci.hybris.stripe.facades.util.StripeCheckoutFacadeUtils;
import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.service.StripeCheckoutSessionService;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentLifecycleService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.InvalidCartException;

import org.apache.commons.lang3.StringUtils;

/**
 * Default Stripe checkout facade.
 */
public class DefaultStripeCheckoutFacade extends AbstractStripeCheckoutFacadeSupport implements StripeCheckoutFacade {

    private StripeCheckoutSessionService stripeCheckoutSessionService;
    private StripeConfigurationService stripeConfigurationService;
    private StripePaymentLifecycleService stripePaymentLifecycleService;
    private StripePaymentTransactionService stripePaymentTransactionService;

    public void setStripeCheckoutSessionService(final StripeCheckoutSessionService stripeCheckoutSessionService) {
        this.stripeCheckoutSessionService = stripeCheckoutSessionService;
    }

    public void setStripeConfigurationService(final StripeConfigurationService stripeConfigurationService) {
        this.stripeConfigurationService = stripeConfigurationService;
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
    public StripeCheckoutSessionFacadeData createCheckoutSessionForCart() {
        return StripeCheckoutFacadeUtils.toFacadeData(
                getStripeCheckoutSessionService().createCheckoutSession(getSessionCart()));
    }
    @Override
    public StripeCheckoutSessionFacadeData getCheckoutSessionForContext(final String sessionId, final String orderCode) {
        return StripeCheckoutFacadeUtils.toFacadeData(getStripeCheckoutSessionService().getCheckoutSession(sessionId, getCurrentSiteId(),
                StringUtils.defaultIfBlank(orderCode, getSessionCartCode())));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderData finalizeCheckoutSessionForContext(final String sessionId, final String orderCode) throws InvalidCartException {
        final String contextCode = requireContextCode(orderCode, sessionId);
        final StripeCheckoutSessionData sessionData = getStripeCheckoutSessionService().getCheckoutSession(sessionId,
                getCurrentSiteId(),
                contextCode);

        if (!StripeCheckoutFacadeUtils.isFinalizableSession(sessionData)) {
            throw new StripeIntegrationException("Stripe Checkout Session is not ready for SAP Commerce order placement.");
        }

        final AbstractOrderModel checkoutOwner = resolveCheckoutOwner(sessionId, contextCode, sessionData);
        if (checkoutOwner instanceof OrderModel orderModel) {
            return getOrderData(orderModel);
        }

        return placeOrder((CartModel) checkoutOwner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StripeCheckoutSessionFacadeData expireCheckoutSessionForCart(final String sessionId) {
        return StripeCheckoutFacadeUtils.toFacadeData(
                getStripePaymentLifecycleService().expireCheckoutSession(getSessionCart(), sessionId));
    }
    @Override
    public StripePublicConfigurationFacadeData getPublicConfiguration() {
        final StripePublicConfigurationFacadeData data = new StripePublicConfigurationFacadeData();
        data.setPublishableKey(getStripeConfigurationService().getPublishableKey(getCurrentSiteId()));
        data.setPaymentOptionId(StripeServicesConstants.PAYMENT_OPTION_ID);
        data.setPaymentMethod(StripeServicesConstants.PAYMENT_METHOD);
        return data;
    }

    /**
     * Resolves the cart or order that owns the Stripe Checkout Session.
     *
     * @param sessionId Stripe Checkout Session identifier
     * @param orderCode expected cart or order code
     * @param sessionData retrieved Stripe session data
     * @return owning cart or order
     */
    protected AbstractOrderModel resolveCheckoutOwner(final String sessionId,
                                                      final String orderCode,
                                                      final StripeCheckoutSessionData sessionData) {
        if (StringUtils.isBlank(orderCode) || !StripeCheckoutFacadeUtils.hasMatchingClientReference(orderCode, sessionData)) {
            throw new StripeIntegrationException("Stripe Checkout Session does not expose a cart or order reference.");
        }

        return getStripePaymentTransactionService().findOrderByPaymentReference(orderCode, sessionId)
                .orElseThrow(() -> new StripeIntegrationException("No SAP Commerce cart or order matches the Stripe Checkout Session."));
    }

    /**
     * Requires the caller to provide the original cart or order code alongside the Stripe session identifier.
     *
     * @param orderCode caller supplied cart or order code
     * @param sessionId Stripe Checkout Session identifier
     * @return validated context code
     */
    protected String requireContextCode(final String orderCode, final String sessionId) {
        final String contextCode = StringUtils.defaultIfBlank(orderCode, getSessionCartCode());
        if (StringUtils.isBlank(contextCode)) {
            throw new StripeIntegrationException("Stripe Checkout Session " + sessionId
                    + " requires the original cart or order reference.");
        }
        return contextCode;
    }

    protected StripeCheckoutSessionService getStripeCheckoutSessionService() {
        return stripeCheckoutSessionService;
    }

    protected StripeConfigurationService getStripeConfigurationService() {
        return stripeConfigurationService;
    }

    protected StripePaymentLifecycleService getStripePaymentLifecycleService() {
        return stripePaymentLifecycleService;
    }

    protected StripePaymentTransactionService getStripePaymentTransactionService() {
        return stripePaymentTransactionService;
    }

    @Override
    protected String getNoSessionCartMessage() {
        return StripeFacadesConstants.MESSAGE_NO_SESSION_CART_FOR_STRIPE_CHECKOUT;
    }

    @Override
    protected String getNoCurrentBaseSiteMessage() {
        return StripeFacadesConstants.MESSAGE_NO_BASE_SITE_FOR_STRIPE_CHECKOUT;
    }
}

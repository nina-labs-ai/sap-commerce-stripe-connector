
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.checkout;

import com.cci.hybris.stripe.facades.data.StripeCheckoutSessionFacadeData;
import com.cci.hybris.stripe.facades.data.StripePublicConfigurationFacadeData;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.order.InvalidCartException;

/**
 * Facade for Stripe Checkout Session orchestration.
 */
public interface StripeCheckoutFacade {

    /**
     * Creates a Stripe Checkout Session for the current session cart.
     *
     * @return checkout-session facade data for the current cart
     */
    StripeCheckoutSessionFacadeData createCheckoutSessionForCart();
    StripeCheckoutSessionFacadeData getCheckoutSessionForContext(String sessionId, String orderCode);

    /**
     * Finalizes a paid Stripe Checkout Session by returning the existing order or placing one for the resolved cart.
     *
     * @param sessionId Stripe Checkout Session identifier
     * @param orderCode expected cart or order code when the storefront can provide one
     * @return placed order data
     * @throws InvalidCartException when SAP Commerce rejects the cart during order placement
     */
    OrderData finalizeCheckoutSessionForContext(String sessionId, String orderCode) throws InvalidCartException;

    /**
     * Expires the current cart Checkout Session when Stripe still allows cancellation.
     *
     * @param sessionId Stripe Checkout Session identifier
     * @return updated checkout-session facade data
     */
    StripeCheckoutSessionFacadeData expireCheckoutSessionForCart(String sessionId);
    StripePublicConfigurationFacadeData getPublicConfiguration();
}

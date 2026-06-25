
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.elements;

import com.cci.hybris.stripe.facades.data.StripePaymentElementFacadeData;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.order.InvalidCartException;

/**
 * Facade for Stripe Payment Elements bootstrap and status retrieval.
 */
public interface StripePaymentElementFacade {

    /**
     * Creates or updates the PaymentIntent for the current session cart.
     *
     * @return Payment Elements bootstrap data for the current cart
     */
    StripePaymentElementFacadeData createPaymentIntentForCart();
    StripePaymentElementFacadeData getPaymentIntent(String paymentIntentId);

    /**
     * Cancels the current cart PaymentIntent at Stripe.
     *
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @return updated Payment Elements facade data
     */
    StripePaymentElementFacadeData cancelPaymentIntentForCart(String paymentIntentId);

    /**
     * Places or retrieves the SAP Commerce order that belongs to a paid Stripe PaymentIntent.
     *
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @param orderCode original cart or order code recorded on the PaymentIntent
     * @return placed or existing order data
     * @throws InvalidCartException when SAP Commerce rejects the paid cart
     */
    OrderData finalizePaymentIntentForContext(String paymentIntentId, String orderCode) throws InvalidCartException;
}

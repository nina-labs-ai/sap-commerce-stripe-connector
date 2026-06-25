
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service;

import com.cci.hybris.stripe.services.data.StripePaymentIntentData;

import de.hybris.platform.core.model.order.AbstractOrderModel;

/**
 * Owns Stripe PaymentIntent creation and retrieval for Payment Elements.
 */
public interface StripePaymentIntentService {

    /**
     * Creates or updates the PaymentIntent for the supplied order.
     *
     * @param order order to register at Stripe
     * @return PaymentIntent data
     */
    StripePaymentIntentData createOrUpdatePaymentIntent(AbstractOrderModel order);
    StripePaymentIntentData getPaymentIntent(AbstractOrderModel order, String paymentIntentId, String siteId);
    StripePaymentIntentData getPaymentIntentForSite(String paymentIntentId, String siteId);
}

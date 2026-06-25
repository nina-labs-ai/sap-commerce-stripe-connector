
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service;

import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;

import de.hybris.platform.core.model.order.AbstractOrderModel;

/**
 * Creates and retrieves Stripe Checkout Sessions.
 */
public interface StripeCheckoutSessionService {

    /**
     * Creates a Stripe Checkout Session for the supplied order.
     *
     * @param order order to register at Stripe
     * @return Checkout Session data
     */
    StripeCheckoutSessionData createCheckoutSession(AbstractOrderModel order);
    StripeCheckoutSessionData getCheckoutSession(String sessionId, String siteId, String orderCode);
}

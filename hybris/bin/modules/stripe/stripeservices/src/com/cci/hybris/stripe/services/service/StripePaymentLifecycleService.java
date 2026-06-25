
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service;

import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.data.StripeRefundData;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.math.BigDecimal;

/**
 * Owns provider-side Stripe lifecycle operations after a payment reference has been created.
 */
public interface StripePaymentLifecycleService {

    /**
     * Expires a Checkout Session that belongs to the supplied order.
     *
     * @param order order that owns the Checkout Session
     * @param sessionId Stripe Checkout Session identifier
     * @return updated Checkout Session data
     */
    StripeCheckoutSessionData expireCheckoutSession(AbstractOrderModel order, String sessionId);

    /**
     * Cancels a PaymentIntent that belongs to the supplied order.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentId Stripe PaymentIntent identifier
     * @return updated PaymentIntent data
     */
    StripePaymentIntentData cancelPaymentIntent(AbstractOrderModel order, String paymentIntentId);

    /**
     * Creates a refund for the supplied Stripe payment reference.
     *
     * @param order order that owns the payment reference
     * @param paymentReference Stripe Checkout Session or PaymentIntent identifier
     * @param amount optional refund amount in major units
     * @return Stripe refund data
     */
    StripeRefundData createRefund(AbstractOrderModel order, String paymentReference, BigDecimal amount);
}

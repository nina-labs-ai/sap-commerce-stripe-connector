
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.refund;

import com.cci.hybris.stripe.facades.data.StripeRefundFacadeData;

import java.math.BigDecimal;

/**
 * Facade for order-bound Stripe refund operations.
 */
public interface StripeRefundFacade {

    /**
     * Creates a Stripe refund for the specified order and payment reference.
     *
     * @param orderCode SAP Commerce order code
     * @param paymentReference Stripe Checkout Session or PaymentIntent identifier
     * @param amount optional refund amount in major units
     * @return refund facade data
     */
    StripeRefundFacadeData createRefundForOrder(String orderCode, String paymentReference, BigDecimal amount);
}


/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.stripe.model.PaymentIntent;

/**
 * PaymentIntent helper utilities for reuse/update checks.
 */
public final class StripePaymentIntentUtils {

    private StripePaymentIntentUtils() {
    }

    /**
     * Returns whether a PaymentIntent can be reused as-is.
     *
     * @param paymentIntent existing PaymentIntent
     * @param amountInMinor expected amount in minor units
     * @param currency expected currency
     * @return {@code true} when the PaymentIntent can be reused
     */
    public static boolean shouldReuse(final PaymentIntent paymentIntent, final long amountInMinor, final String currency) {
        return paymentIntent != null
                && amountInMinor == safeAmount(paymentIntent)
                && StripeStatusUtils.equalsIgnoreCase(currency, paymentIntent.getCurrency())
                && !StripeStatusUtils.isPaymentIntentTerminal(paymentIntent.getStatus());
    }

    /**
     * Returns whether Stripe still allows updates for the PaymentIntent status.
     *
     * @param paymentIntent existing PaymentIntent
     * @return {@code true} when updates are allowed
     */
    public static boolean shouldUpdate(final PaymentIntent paymentIntent) {
        return StripeStatusUtils.isPaymentIntentUpdatable(paymentIntent == null ? null : paymentIntent.getStatus());
    }

    /**
     * Returns a safe amount value for a PaymentIntent.
     *
     * @param paymentIntent existing PaymentIntent
     * @return amount in minor units or {@code 0}
     */
    public static long safeAmount(final PaymentIntent paymentIntent) {
        return paymentIntent == null || paymentIntent.getAmount() == null ? 0L : paymentIntent.getAmount();
    }
}

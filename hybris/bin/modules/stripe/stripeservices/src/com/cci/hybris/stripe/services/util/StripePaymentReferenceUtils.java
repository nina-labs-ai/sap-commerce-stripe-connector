
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;

/**
 * Stripe payment-reference helper utilities.
 */
public final class StripePaymentReferenceUtils {

    private StripePaymentReferenceUtils() {
    }

    /**
     * Returns whether the reference is a Stripe PaymentIntent identifier.
     *
     * @param paymentReference Stripe payment reference
     * @return {@code true} when the reference is a PaymentIntent
     */
    public static boolean isPaymentIntentReference(final String paymentReference) {
        return paymentReference != null && paymentReference.startsWith(StripeServicesConstants.PAYMENT_INTENT_PREFIX);
    }

    /**
     * Returns whether the reference is a Stripe Checkout Session identifier.
     *
     * @param paymentReference Stripe payment reference
     * @return {@code true} when the reference is a Checkout Session
     */
    public static boolean isCheckoutSessionReference(final String paymentReference) {
        return paymentReference != null && paymentReference.startsWith(StripeServicesConstants.CHECKOUT_SESSION_PREFIX);
    }

    /**
     * Resolves payment flow from a Stripe payment reference.
     *
     * @param paymentReference Stripe payment reference
     * @return resolved payment flow
     */
    public static String resolvePaymentFlow(final String paymentReference) {
        return isPaymentIntentReference(paymentReference)
                ? StripeServicesConstants.PAYMENT_FLOW_ELEMENTS
                : StripeServicesConstants.PAYMENT_FLOW_CHECKOUT;
    }
}

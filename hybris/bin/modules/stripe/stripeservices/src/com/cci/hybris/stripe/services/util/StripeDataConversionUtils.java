
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.data.StripeRefundData;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;

/**
 * Converters between Stripe SDK models and service data DTOs.
 */
public final class StripeDataConversionUtils {

    private StripeDataConversionUtils() {
    }

    /**
     * Converts Stripe Session into connector data.
     *
     * @param session Stripe Session
     * @return Checkout Session data
     */
    public static StripeCheckoutSessionData toCheckoutSessionData(final Session session) {
        final StripeCheckoutSessionData sessionData = new StripeCheckoutSessionData();
        sessionData.setId(session.getId());
        sessionData.setUrl(session.getUrl());
        sessionData.setStatus(session.getStatus());
        sessionData.setPaymentStatus(session.getPaymentStatus());
        sessionData.setClientReferenceId(session.getClientReferenceId());
        return sessionData;
    }

    /**
     * Converts Stripe PaymentIntent into connector data.
     *
     * @param paymentIntent Stripe PaymentIntent
     * @return PaymentIntent data
     */
    public static StripePaymentIntentData toPaymentIntentData(final PaymentIntent paymentIntent) {
        final StripePaymentIntentData data = new StripePaymentIntentData();
        data.setId(paymentIntent.getId());
        data.setClientSecret(paymentIntent.getClientSecret());
        data.setStatus(paymentIntent.getStatus());
        data.setAmount(paymentIntent.getAmount());
        data.setCurrency(paymentIntent.getCurrency());
        data.setClientReferenceId(
                StripeMetadataUtils.resolveOrderCode(paymentIntent.getMetadata(), null));
        return data;
    }

    /**
     * Converts Stripe Refund into connector data.
     *
     * @param refund Stripe Refund
     * @return refund data
     */
    public static StripeRefundData toRefundData(final Refund refund) {
        final StripeRefundData refundData = new StripeRefundData();
        refundData.setId(refund.getId());
        refundData.setPaymentIntentId(refund.getPaymentIntent());
        refundData.setStatus(refund.getStatus());
        refundData.setAmount(refund.getAmount());
        refundData.setCurrency(refund.getCurrency());
        return refundData;
    }

    /**
     * Resolves order code from session metadata with client-reference fallback.
     *
     * @param session Stripe session
     * @return resolved order code
     */
    public static String resolveOrderCode(final Session session) {
        return StripeMetadataUtils.resolveOrderCode(session.getMetadata(), session.getClientReferenceId());
    }

    /**
     * Resolves order code from payment-intent metadata.
     *
     * @param paymentIntent Stripe PaymentIntent
     * @return resolved order code
     */
    public static String resolveOrderCode(final PaymentIntent paymentIntent) {
        return StripeMetadataUtils.resolveOrderCode(paymentIntent.getMetadata(), null);
    }
}

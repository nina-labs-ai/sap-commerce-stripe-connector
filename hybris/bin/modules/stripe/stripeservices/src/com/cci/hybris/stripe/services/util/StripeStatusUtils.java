
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;

import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import java.util.Map;
import java.util.Objects;

/**
 * Stripe status and event helper utilities.
 */
public final class StripeStatusUtils {

    private StripeStatusUtils() {
    }

    /**
     * Returns whether both values are non-null and equal ignoring case.
     *
     * @param left first value
     * @param right second value
     * @return {@code true} when values match ignoring case
     */
    public static boolean equalsIgnoreCase(final String left, final String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    /**
     * Returns whether the PaymentIntent status can still be updated.
     *
     * @param status Stripe PaymentIntent status
     * @return {@code true} when status allows updates
     */
    public static boolean isPaymentIntentUpdatable(final String status) {
        return equalsIgnoreCase(status, StripeServicesConstants.STRIPE_STATUS_REQUIRES_PAYMENT_METHOD)
                || equalsIgnoreCase(status, StripeServicesConstants.STRIPE_STATUS_REQUIRES_CONFIRMATION);
    }

    /**
     * Returns whether the PaymentIntent status is terminal.
     *
     * @param status Stripe PaymentIntent status
     * @return {@code true} when status is terminal
     */
    public static boolean isPaymentIntentTerminal(final String status) {
        return equalsIgnoreCase(status, StripeServicesConstants.STRIPE_STATUS_SUCCEEDED)
                || equalsIgnoreCase(status, StripeServicesConstants.STRIPE_STATUS_CANCELED);
    }

    /**
     * Returns whether the checkout session is finalizable.
     *
     * @param paymentStatus session payment status
     * @param status session status
     * @return {@code true} when session can be finalized
     */
    public static boolean isCheckoutSessionFinalizable(final String paymentStatus, final String status) {
        return equalsIgnoreCase(paymentStatus, StripeServicesConstants.STRIPE_PAYMENT_STATUS_PAID)
                || equalsIgnoreCase(status, StripeServicesConstants.STRIPE_STATUS_COMPLETE);
    }

    /**
     * Returns whether the payment intent is finalizable.
     *
     * @param status PaymentIntent status
     * @return {@code true} when PaymentIntent can be finalized
     */
    public static boolean isPaymentIntentFinalizable(final String status) {
        return equalsIgnoreCase(status, StripeServicesConstants.STRIPE_STATUS_SUCCEEDED)
                || equalsIgnoreCase(status, StripeServicesConstants.STRIPE_STATUS_REQUIRES_CAPTURE);
    }

    /**
     * Returns whether event represents a paid checkout completion.
     *
     * @param eventType event type
     * @param session Stripe session
     * @return {@code true} when event marks completion
     */
    public static boolean isCheckoutCompletedEvent(final String eventType, final Session session) {
        return Objects.equals(eventType, StripeServicesConstants.EVENT_CHECKOUT_SESSION_COMPLETED)
                && equalsIgnoreCase(session.getPaymentStatus(), StripeServicesConstants.STRIPE_PAYMENT_STATUS_PAID);
    }

    /**
     * Returns whether event represents an expired/unpaid checkout session.
     *
     * @param eventType event type
     * @param session Stripe session
     * @return {@code true} when event marks expiry
     */
    public static boolean isCheckoutExpiredEvent(final String eventType, final Session session) {
        return Objects.equals(eventType, StripeServicesConstants.EVENT_CHECKOUT_SESSION_EXPIRED)
                && (equalsIgnoreCase(session.getStatus(), StripeServicesConstants.STRIPE_STATUS_EXPIRED)
                || equalsIgnoreCase(session.getPaymentStatus(), StripeServicesConstants.STRIPE_PAYMENT_STATUS_UNPAID));
    }

    /**
     * Returns whether event represents a successful PaymentIntent.
     *
     * @param eventType event type
     * @param paymentIntent Stripe PaymentIntent
     * @return {@code true} when event marks success
     */
    public static boolean isPaymentIntentSucceededEvent(final String eventType, final PaymentIntent paymentIntent) {
        return Objects.equals(eventType, StripeServicesConstants.EVENT_PAYMENT_INTENT_SUCCEEDED)
                && equalsIgnoreCase(paymentIntent.getStatus(), StripeServicesConstants.STRIPE_STATUS_SUCCEEDED);
    }

    /**
     * Returns whether event represents failed/canceled PaymentIntent.
     *
     * @param eventType event type
     * @param paymentIntent Stripe PaymentIntent
     * @return {@code true} when event marks failure
     */
    public static boolean isPaymentIntentFailedEvent(final String eventType, final PaymentIntent paymentIntent) {
        return (Objects.equals(eventType, StripeServicesConstants.EVENT_PAYMENT_INTENT_PAYMENT_FAILED)
                || Objects.equals(eventType, StripeServicesConstants.EVENT_PAYMENT_INTENT_CANCELED))
                && (equalsIgnoreCase(paymentIntent.getStatus(), StripeServicesConstants.STRIPE_STATUS_REQUIRES_PAYMENT_METHOD)
                || equalsIgnoreCase(paymentIntent.getStatus(), StripeServicesConstants.STRIPE_STATUS_CANCELED));
    }

    /**
     * Maps a Stripe refund status to SAP transaction status.
     *
     * @param status Stripe refund status
     * @return SAP transaction status
     */
    public static String mapRefundStatus(final String status) {
        if (equalsIgnoreCase(status, StripeServicesConstants.STRIPE_STATUS_SUCCEEDED)) {
            return StripeServicesConstants.STATUS_ACCEPTED;
        }
        if (equalsIgnoreCase(status, StripeServicesConstants.STATUS_PENDING_LOWER)) {
            return StripeServicesConstants.STATUS_PENDING;
        }
        return StripeServicesConstants.STATUS_REJECTED;
    }

    /**
     * Returns whether metadata matches expected payment flow and site.
     *
     * @param metadata Stripe metadata
     * @param expectedFlow expected payment flow
     * @param siteId expected site identifier
     * @return {@code true} when metadata matches
     */
    public static boolean hasExpectedFlowAndSite(final Map<String, String> metadata,
                                                 final String expectedFlow,
                                                 final String siteId) {
        return metadata != null
                && Objects.equals(metadata.get(StripeServicesConstants.METADATA_PAYMENT_FLOW), expectedFlow)
                && Objects.equals(metadata.get(StripeServicesConstants.METADATA_SITE_UID), siteId);
    }
}

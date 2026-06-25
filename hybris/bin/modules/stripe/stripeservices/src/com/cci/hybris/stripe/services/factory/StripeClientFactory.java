/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.factory;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionExpireParams;
import com.stripe.param.checkout.SessionCreateParams;

/**
 * Small wrapper around the Stripe Java SDK to keep service tests isolated from SDK static calls.
 */
public interface StripeClientFactory {

    /**
     * Creates a Stripe Checkout Session.
     *
     * @param secretKey Stripe secret key
     * @param params Checkout Session create params
     * @return created Checkout Session
     * @throws StripeException when Stripe API call fails
     */
    Session createCheckoutSession(String secretKey, SessionCreateParams params) throws StripeException;

    /**
     * Retrieves a Stripe Checkout Session.
     *
     * @param secretKey Stripe secret key
     * @param sessionId Checkout Session identifier
     * @return Checkout Session
     * @throws StripeException when Stripe API call fails
     */
    Session getCheckoutSession(String secretKey, String sessionId) throws StripeException;

    /**
     * Expires a Stripe Checkout Session.
     *
     * @param secretKey Stripe secret key
     * @param sessionId Checkout Session identifier
     * @param params session expire params
     * @return expired Checkout Session
     * @throws StripeException when Stripe API call fails
     */
    Session expireCheckoutSession(String secretKey, String sessionId, SessionExpireParams params) throws StripeException;

    /**
     * Creates a Stripe PaymentIntent.
     *
     * @param secretKey Stripe secret key
     * @param params PaymentIntent create params
     * @return created PaymentIntent
     * @throws StripeException when Stripe API call fails
     */
    PaymentIntent createPaymentIntent(String secretKey, PaymentIntentCreateParams params) throws StripeException;

    /**
     * Retrieves a Stripe PaymentIntent.
     *
     * @param secretKey Stripe secret key
     * @param paymentIntentId PaymentIntent identifier
     * @return PaymentIntent
     * @throws StripeException when Stripe API call fails
     */
    PaymentIntent getPaymentIntent(String secretKey, String paymentIntentId) throws StripeException;

    /**
     * Updates a Stripe PaymentIntent.
     *
     * @param secretKey Stripe secret key
     * @param paymentIntentId PaymentIntent identifier
     * @param params PaymentIntent update params
     * @return updated PaymentIntent
     * @throws StripeException when Stripe API call fails
     */
    PaymentIntent updatePaymentIntent(String secretKey, String paymentIntentId, PaymentIntentUpdateParams params)
            throws StripeException;

    /**
     * Cancels a Stripe PaymentIntent.
     *
     * @param secretKey Stripe secret key
     * @param paymentIntentId PaymentIntent identifier
     * @param params PaymentIntent cancel params
     * @return canceled PaymentIntent
     * @throws StripeException when Stripe API call fails
     */
    PaymentIntent cancelPaymentIntent(String secretKey, String paymentIntentId, PaymentIntentCancelParams params)
            throws StripeException;

    /**
     * Creates a Stripe Refund.
     *
     * @param secretKey Stripe secret key
     * @param params refund create params
     * @return created Refund
     * @throws StripeException when Stripe API call fails
     */
    Refund createRefund(String secretKey, RefundCreateParams params) throws StripeException;

    /**
     * Constructs and verifies a Stripe event from payload and signature.
     *
     * @param payload raw webhook payload
     * @param signature Stripe signature header value
     * @param webhookSecret Stripe webhook signing secret
     * @return verified Stripe event
     * @throws SignatureVerificationException when signature verification fails
     */
    Event constructEvent(String payload, String signature, String webhookSecret) throws SignatureVerificationException;

    /**
     * Creates a low-level Stripe SDK client.
     *
     * @param secretKey Stripe secret key
     * @return Stripe SDK client
     */
    StripeClient createClient(String secretKey);
}

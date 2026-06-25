/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.factory.impl;

import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionExpireParams;
import com.stripe.param.checkout.SessionCreateParams;

/**
 * Default Stripe SDK wrapper.
 */
public class DefaultStripeClientFactory implements StripeClientFactory {

    @Override
    public Session createCheckoutSession(final String secretKey, final SessionCreateParams params) throws StripeException {
        return createClient(secretKey).v1().checkout().sessions().create(params);
    }

    @Override
    public Session getCheckoutSession(final String secretKey, final String sessionId) throws StripeException {
        return createClient(secretKey).v1().checkout().sessions().retrieve(sessionId);
    }

    @Override
    public Session expireCheckoutSession(final String secretKey,
                                         final String sessionId,
                                         final SessionExpireParams params) throws StripeException {
        return createClient(secretKey).v1().checkout().sessions().expire(sessionId, params);
    }

    @Override
    public PaymentIntent createPaymentIntent(final String secretKey, final PaymentIntentCreateParams params)
            throws StripeException {
        return createClient(secretKey).v1().paymentIntents().create(params);
    }

    @Override
    public PaymentIntent getPaymentIntent(final String secretKey, final String paymentIntentId) throws StripeException {
        return createClient(secretKey).v1().paymentIntents().retrieve(paymentIntentId);
    }

    @Override
    public PaymentIntent updatePaymentIntent(final String secretKey,
                                             final String paymentIntentId,
                                             final PaymentIntentUpdateParams params) throws StripeException {
        return createClient(secretKey).v1().paymentIntents().update(paymentIntentId, params);
    }

    @Override
    public PaymentIntent cancelPaymentIntent(final String secretKey,
                                             final String paymentIntentId,
                                             final PaymentIntentCancelParams params) throws StripeException {
        return createClient(secretKey).v1().paymentIntents().cancel(paymentIntentId, params);
    }

    @Override
    public Refund createRefund(final String secretKey, final RefundCreateParams params) throws StripeException {
        return createClient(secretKey).v1().refunds().create(params);
    }

    @Override
    public Event constructEvent(final String payload, final String signature, final String webhookSecret)
            throws SignatureVerificationException {
        return Webhook.constructEvent(payload, signature, webhookSecret);
    }

    @Override
    public StripeClient createClient(final String secretKey) {
        return new StripeClient(secretKey);
    }
}

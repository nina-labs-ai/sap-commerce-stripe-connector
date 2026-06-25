
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service;

/**
 * Verifies and processes Stripe webhook payloads.
 */
public interface StripeWebhookService {

    /**
     * Verifies and processes a Stripe webhook payload.
     *
     * @param payload raw webhook payload
     * @param signature Stripe signature header value
     * @param siteId base-site identifier
     */
    void handleWebhook(String payload, String signature, String siteId);
}


/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.events.controllers;

import com.cci.hybris.stripe.services.service.StripeWebhookService;

import jakarta.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook endpoint for Stripe events.
 */
@RestController
public class StripeWebhookController {

    private static final String STRIPE_WEBHOOK_PATH = "/webhooks/stripe";
    private static final String STRIPE_SIGNATURE_HEADER = "Stripe-Signature";
    private static final String BASE_SITE_HEADER = "X-Base-Site-Id";

    @Resource(name = "stripeWebhookService")
    private StripeWebhookService stripeWebhookService;

    /**
     * Handles Stripe webhook callbacks and delegates signature verification and event processing to the service layer.
     *
     * @param payload raw webhook payload
     * @param signature Stripe webhook signature header
     * @param siteId optional base-site override header for local forwarding scenarios
     */
    @PostMapping(value = STRIPE_WEBHOOK_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public void handleWebhook(@RequestBody final String payload,
                              @RequestHeader(STRIPE_SIGNATURE_HEADER) final String signature,
                              @RequestHeader(name = BASE_SITE_HEADER, required = false) final String siteId) {
        getStripeWebhookService().handleWebhook(payload, signature, siteId);
    }

    /**
     * Exposes webhook service dependency for extensible subclasses.
     *
     * @return webhook service
     */
    protected StripeWebhookService getStripeWebhookService() {
        return stripeWebhookService;
    }
}
